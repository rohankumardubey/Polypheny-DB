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

package ch.unibas.dmi.dbis.polyphenydb.rel.core;


import ch.unibas.dmi.dbis.polyphenydb.rel.RelCollation;
import ch.unibas.dmi.dbis.polyphenydb.rel.RelCollations;
import ch.unibas.dmi.dbis.polyphenydb.rel.RelNode;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataType;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataTypeFactory;
import ch.unibas.dmi.dbis.polyphenydb.sql.SqlAggFunction;
import ch.unibas.dmi.dbis.polyphenydb.sql.type.SqlTypeUtil;
import ch.unibas.dmi.dbis.polyphenydb.util.mapping.Mapping;
import ch.unibas.dmi.dbis.polyphenydb.util.mapping.Mappings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;


/**
 * Call to an aggregate function within an {@link ch.unibas.dmi.dbis.polyphenydb.rel.core.Aggregate}.
 */
public class AggregateCall {

    private final SqlAggFunction aggFunction;

    private final boolean distinct;
    private final boolean approximate;
    public final RelDataType type;
    public final String name;

    // We considered using ImmutableIntList but we would not save much memory: since all values are small, ImmutableList uses cached Integer values.
    private final ImmutableList<Integer> argList;
    public final int filterArg;
    public final RelCollation collation;


    /**
     * Creates an AggregateCall.
     *
     * @param aggFunction Aggregate function
     * @param distinct Whether distinct
     * @param approximate Whether approximate
     * @param argList List of ordinals of arguments
     * @param filterArg Ordinal of filter argument (the {@code FILTER (WHERE ...)} clause in SQL), or -1
     * @param collation How to sort values before aggregation (the {@code WITHIN GROUP} clause in SQL)
     * @param type Result type
     * @param name Name (may be null)
     */
    private AggregateCall( SqlAggFunction aggFunction, boolean distinct, boolean approximate, List<Integer> argList, int filterArg, RelCollation collation, RelDataType type, String name ) {
        this.type = Objects.requireNonNull( type );
        this.name = name;
        this.aggFunction = Objects.requireNonNull( aggFunction );
        this.argList = ImmutableList.copyOf( argList );
        this.filterArg = filterArg;
        this.collation = Objects.requireNonNull( collation );
        this.distinct = distinct;
        this.approximate = approximate;
    }


    /**
     * Creates an AggregateCall, inferring its type if {@code type} is null.
     */
    public static AggregateCall create( SqlAggFunction aggFunction, boolean distinct, boolean approximate, List<Integer> argList, int filterArg, RelCollation collation,
            int groupCount, RelNode input, RelDataType type, String name ) {
        if ( type == null ) {
            final RelDataTypeFactory typeFactory = input.getCluster().getTypeFactory();
            final List<RelDataType> types = SqlTypeUtil.projectTypes( input.getRowType(), argList );
            final Aggregate.AggCallBinding callBinding = new Aggregate.AggCallBinding( typeFactory, aggFunction, types, groupCount, filterArg >= 0 );
            type = aggFunction.inferReturnType( callBinding );
        }
        return create( aggFunction, distinct, approximate, argList, filterArg, collation, type, name );
    }


    /**
     * Creates an AggregateCall.
     */
    public static AggregateCall create( SqlAggFunction aggFunction, boolean distinct, boolean approximate, List<Integer> argList, int filterArg, RelCollation collation, RelDataType type, String name ) {
        return new AggregateCall( aggFunction, distinct, approximate, argList, filterArg, collation, type, name );
    }


    /**
     * Returns whether this AggregateCall is distinct, as in <code>COUNT(DISTINCT empno)</code>.
     *
     * @return whether distinct
     */
    public final boolean isDistinct() {
        return distinct;
    }


    /**
     * Returns whether this AggregateCall is approximate, as in <code>APPROX_COUNT_DISTINCT(empno)</code>.
     *
     * @return whether approximate
     */
    public final boolean isApproximate() {
        return approximate;
    }


    /**
     * Returns the aggregate function.
     *
     * @return aggregate function
     */
    public final SqlAggFunction getAggregation() {
        return aggFunction;
    }


    /**
     * Returns the aggregate ordering definition (the {@code WITHIN GROUP} clause in SQL), or the empty list if not specified.
     *
     * @return ordering definition
     */
    public RelCollation getCollation() {
        return collation;
    }


    /**
     * Returns the ordinals of the arguments to this call.
     *
     * The list is immutable.
     *
     * @return list of argument ordinals
     */
    public final List<Integer> getArgList() {
        return argList;
    }


    /**
     * Returns the result type.
     *
     * @return result type
     */
    public final RelDataType getType() {
        return type;
    }


    /**
     * Returns the name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }


    /**
     * Creates an equivalent AggregateCall that has a new name.
     *
     * @param name New name (may be null)
     */
    public AggregateCall rename( String name ) {
        if ( Objects.equals( this.name, name ) ) {
            return this;
        }
        return new AggregateCall( aggFunction, distinct, approximate, argList, filterArg, RelCollations.EMPTY, type, name );
    }


    public String toString() {
        StringBuilder buf = new StringBuilder( aggFunction.toString() );
        buf.append( "(" );
        if ( distinct ) {
            buf.append( (argList.size() == 0) ? "DISTINCT" : "DISTINCT " );
        }
        int i = -1;
        for ( Integer arg : argList ) {
            if ( ++i > 0 ) {
                buf.append( ", " );
            }
            buf.append( "$" );
            buf.append( arg );
        }
        buf.append( ")" );
        if ( !collation.equals( RelCollations.EMPTY ) ) {
            buf.append( " WITHIN GROUP (" );
            buf.append( collation );
            buf.append( ")" );
        }
        if ( hasFilter() ) {
            buf.append( " FILTER $" );
            buf.append( filterArg );
        }
        return buf.toString();
    }


    /**
     * Returns true if and only if this AggregateCall has a filter argument
     */
    public boolean hasFilter() {
        return filterArg >= 0;
    }


    @Override
    public boolean equals( Object o ) {
        if ( !(o instanceof AggregateCall) ) {
            return false;
        }
        AggregateCall other = (AggregateCall) o;
        return aggFunction.equals( other.aggFunction )
                && (distinct == other.distinct)
                && argList.equals( other.argList )
                && filterArg == other.filterArg
                && Objects.equals( collation, other.collation );
    }


    @Override
    public int hashCode() {
        return Objects.hash( aggFunction, distinct, argList, filterArg, collation );
    }


    /**
     * Creates a binding of this call in the context of an {@link ch.unibas.dmi.dbis.polyphenydb.rel.logical.LogicalAggregate}, which can then be used to infer the return type.
     */
    public Aggregate.AggCallBinding createBinding( Aggregate aggregateRelBase ) {
        final RelDataType rowType = aggregateRelBase.getInput().getRowType();

        return new Aggregate.AggCallBinding(
                aggregateRelBase.getCluster().getTypeFactory(),
                aggFunction,
                SqlTypeUtil.projectTypes( rowType, argList ),
                aggregateRelBase.getGroupCount(),
                hasFilter() );
    }


    /**
     * Creates an equivalent AggregateCall with new argument ordinals.
     *
     * @param args Arguments
     * @return AggregateCall that suits new inputs and GROUP BY columns
     * @see #transform(Mappings.TargetMapping)
     */
    public AggregateCall copy( List<Integer> args, int filterArg, RelCollation collation ) {
        return new AggregateCall( aggFunction, distinct, approximate, args, filterArg, collation, type, name );
    }


    /**
     * Creates equivalent AggregateCall that is adapted to a new input types and/or number of columns in GROUP BY.
     *
     * @param input relation that will be used as a child of aggregate
     * @param argList argument indices of the new call in the input
     * @param filterArg Index of the filter, or -1
     * @param oldGroupKeyCount number of columns in GROUP BY of old aggregate
     * @param newGroupKeyCount number of columns in GROUP BY of new aggregate
     * @return AggregateCall that suits new inputs and GROUP BY columns
     */
    public AggregateCall adaptTo( RelNode input, List<Integer> argList, int filterArg, int oldGroupKeyCount, int newGroupKeyCount ) {
        // The return type of aggregate call need to be recomputed. Since it might depend on the number of columns in GROUP BY.
        final RelDataType newType =
                oldGroupKeyCount == newGroupKeyCount
                        && argList.equals( this.argList )
                        && filterArg == this.filterArg
                        ? type
                        : null;
        return create( aggFunction, distinct, approximate, argList, filterArg, collation, newGroupKeyCount, input, newType, getName() );
    }


    /**
     * Creates a copy of this aggregate call, applying a mapping to its arguments.
     */
    public AggregateCall transform( Mappings.TargetMapping mapping ) {
        return copy(
                Mappings.apply2( (Mapping) mapping, argList ),
                hasFilter() ? Mappings.apply( mapping, filterArg ) : -1,
                RelCollations.permute( collation, mapping ) );
    }
}