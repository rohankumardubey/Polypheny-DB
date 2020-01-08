/*
 * This file is based on code taken from the Apache Calcite project, which was released under the Apache License.
 * The changes are released under the MIT license.
 *
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Databases and Information Systems Research Group, University of Basel, Switzerland
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ch.unibas.dmi.dbis.polyphenydb.adapter.enumerable;


import ch.unibas.dmi.dbis.polyphenydb.adapter.enumerable.RexImpTable.CountImplementor;
import ch.unibas.dmi.dbis.polyphenydb.adapter.enumerable.RexImpTable.SumImplementor;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataType;
import ch.unibas.dmi.dbis.polyphenydb.rex.RexNode;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.calcite.linq4j.tree.BlockBuilder;
import org.apache.calcite.linq4j.tree.BlockStatement;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.linq4j.tree.ParameterExpression;
import org.apache.calcite.linq4j.tree.Primitive;
import org.apache.calcite.linq4j.tree.Types;


/**
 * The base implementation of strict aggregate function.
 *
 * @see CountImplementor
 * @see SumImplementor
 */
public abstract class StrictAggImplementor implements AggImplementor {

    private boolean needTrackEmptySet;
    private boolean trackNullsPerRow;
    private int stateSize;


    protected boolean nonDefaultOnEmptySet( AggContext info ) {
        return info.returnRelType().isNullable();
    }


    protected final int getStateSize() {
        return stateSize;
    }


    protected final void accAdvance( AggAddContext add, Expression acc, Expression next ) {
        add.currentBlock().add(
                Expressions.statement(
                        Expressions.assign(
                                acc,
                                Types.castIfNecessary( acc.type, next ) ) ) );
    }


    @Override
    public final List<Type> getStateType( AggContext info ) {
        List<Type> subState = getNotNullState( info );
        stateSize = subState.size();
        needTrackEmptySet = nonDefaultOnEmptySet( info );
        if ( !needTrackEmptySet ) {
            return subState;
        }
        final boolean hasNullableArgs = anyNullable( info.parameterRelTypes() );
        trackNullsPerRow = !(info instanceof WinAggContext) || hasNullableArgs;

        List<Type> res = new ArrayList<>( subState.size() + 1 );
        res.addAll( subState );
        res.add( boolean.class ); // has not nulls
        return res;
    }


    private boolean anyNullable( List<? extends RelDataType> types ) {
        for ( RelDataType type : types ) {
            if ( type.isNullable() ) {
                return true;
            }
        }
        return false;
    }


    public List<Type> getNotNullState( AggContext info ) {
        Type type = info.returnType();
        type = EnumUtils.fromInternal( type );
        type = Primitive.unbox( type );
        return Collections.singletonList( type );
    }


    @Override
    public final void implementReset( AggContext info, AggResetContext reset ) {
        if ( trackNullsPerRow ) {
            List<Expression> acc = reset.accumulator();
            Expression flag = acc.get( acc.size() - 1 );
            BlockBuilder block = reset.currentBlock();
            block.add(
                    Expressions.statement(
                            Expressions.assign(
                                    flag,
                                    RexImpTable.getDefaultValue( flag.getType() ) ) ) );
        }
        implementNotNullReset( info, reset );
    }


    protected void implementNotNullReset( AggContext info,
            AggResetContext reset ) {
        BlockBuilder block = reset.currentBlock();
        List<Expression> accumulator = reset.accumulator();
        for ( int i = 0; i < getStateSize(); i++ ) {
            Expression exp = accumulator.get( i );
            block.add(
                    Expressions.statement(
                            Expressions.assign( exp,
                                    RexImpTable.getDefaultValue( exp.getType() ) ) ) );
        }
    }


    @Override
    public final void implementAdd( AggContext info, final AggAddContext add ) {
        final List<RexNode> args = add.rexArguments();
        final RexToLixTranslator translator = add.rowTranslator();
        final List<Expression> conditions = new ArrayList<>();
        conditions.addAll( translator.translateList( args, RexImpTable.NullAs.IS_NOT_NULL ) );
        if ( add.rexFilterArgument() != null ) {
            conditions.add(
                    translator.translate( add.rexFilterArgument(),
                            RexImpTable.NullAs.FALSE ) );
        }
        Expression condition = Expressions.foldAnd( conditions );
        if ( Expressions.constant( false ).equals( condition ) ) {
            return;
        }

        boolean argsNotNull = Expressions.constant( true ).equals( condition );
        final BlockBuilder thenBlock =
                argsNotNull
                        ? add.currentBlock()
                        : new BlockBuilder( true, add.currentBlock() );
        if ( trackNullsPerRow ) {
            List<Expression> acc = add.accumulator();
            thenBlock.add(
                    Expressions.statement(
                            Expressions.assign(
                                    acc.get( acc.size() - 1 ),
                                    Expressions.constant( true ) ) ) );
        }
        if ( argsNotNull ) {
            implementNotNullAdd( info, add );
            return;
        }

        final Map<RexNode, Boolean> nullables = new HashMap<>();
        for ( RexNode arg : args ) {
            if ( translator.isNullable( arg ) ) {
                nullables.put( arg, false );
            }
        }
        add.nestBlock( thenBlock, nullables );
        implementNotNullAdd( info, add );
        add.exitBlock();
        add.currentBlock().add( Expressions.ifThen( condition, thenBlock.toBlock() ) );
    }


    protected abstract void implementNotNullAdd( AggContext info, AggAddContext add );


    @Override
    public final Expression implementResult( AggContext info, final AggResultContext result ) {
        if ( !needTrackEmptySet ) {
            return RexToLixTranslator.convert( implementNotNullResult( info, result ), info.returnType() );
        }
        String tmpName = result.accumulator().isEmpty()
                ? "ar"
                : (result.accumulator().get( 0 ) + "$Res");
        ParameterExpression res = Expressions.parameter( 0, info.returnType(), result.currentBlock().newName( tmpName ) );

        List<Expression> acc = result.accumulator();
        final BlockBuilder thenBlock = result.nestBlock();
        Expression nonNull = RexToLixTranslator.convert( implementNotNullResult( info, result ), info.returnType() );
        result.exitBlock();
        thenBlock.add( Expressions.statement( Expressions.assign( res, nonNull ) ) );
        BlockStatement thenBranch = thenBlock.toBlock();
        Expression seenNotNullRows =
                trackNullsPerRow
                        ? acc.get( acc.size() - 1 )
                        : ((WinAggResultContext) result).hasRows();

        if ( thenBranch.statements.size() == 1 ) {
            return Expressions.condition( seenNotNullRows, nonNull, RexImpTable.getDefaultValue( res.getType() ) );
        }
        result.currentBlock().add( Expressions.declare( 0, res, null ) );
        result.currentBlock().add(
                Expressions.ifThenElse( seenNotNullRows,
                        thenBranch,
                        Expressions.statement(
                                Expressions.assign(
                                        res,
                                        RexImpTable.getDefaultValue( res.getType() ) ) ) ) );
        return res;
    }


    protected Expression implementNotNullResult( AggContext info, AggResultContext result ) {
        return result.accumulator().get( 0 );
    }
}
