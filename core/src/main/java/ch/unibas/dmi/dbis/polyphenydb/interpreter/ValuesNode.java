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

package ch.unibas.dmi.dbis.polyphenydb.interpreter;


import ch.unibas.dmi.dbis.polyphenydb.rel.core.Values;
import ch.unibas.dmi.dbis.polyphenydb.rex.RexLiteral;
import ch.unibas.dmi.dbis.polyphenydb.rex.RexNode;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;


/**
 * Interpreter node that implements a {@link ch.unibas.dmi.dbis.polyphenydb.rel.core.Values}.
 */
public class ValuesNode implements Node {

    private final Sink sink;
    private final int fieldCount;
    private final ImmutableList<Row> rows;


    public ValuesNode( Compiler compiler, Values rel ) {
        this.sink = compiler.sink( rel );
        this.fieldCount = rel.getRowType().getFieldCount();
        this.rows = createRows( compiler, rel.getTuples() );
    }


    private ImmutableList<Row> createRows( Compiler compiler, ImmutableList<ImmutableList<RexLiteral>> tuples ) {
        final List<RexNode> nodes = new ArrayList<>();
        for ( ImmutableList<RexLiteral> tuple : tuples ) {
            nodes.addAll( tuple );
        }
        final Scalar scalar = compiler.compile( nodes, null );
        final Object[] values = new Object[nodes.size()];
        final Context context = compiler.createContext();
        scalar.execute( context, values );
        final ImmutableList.Builder<Row> rows = ImmutableList.builder();
        Object[] subValues = new Object[fieldCount];
        for ( int i = 0; i < values.length; i += fieldCount ) {
            System.arraycopy( values, i, subValues, 0, fieldCount );
            rows.add( Row.asCopy( subValues ) );
        }
        return rows.build();
    }


    @Override
    public void run() throws InterruptedException {
        for ( Row row : rows ) {
            sink.send( row );
        }
        sink.end();
    }
}