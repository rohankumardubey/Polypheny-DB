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

package ch.unibas.dmi.dbis.polyphenydb.rex;


import ch.unibas.dmi.dbis.polyphenydb.sql.SqlOperator;
import java.util.HashMap;
import java.util.Map;


/**
 * Implementation of {@link RexSqlConvertletTable}.
 */
public class RexSqlReflectiveConvertletTable implements RexSqlConvertletTable {

    private final Map<Object, Object> map = new HashMap<>();


    public RexSqlReflectiveConvertletTable() {
    }


    @Override
    public RexSqlConvertlet get( RexCall call ) {
        RexSqlConvertlet convertlet;
        final SqlOperator op = call.getOperator();

        // Is there a convertlet for this operator (e.g. SqlStdOperatorTable.plusOperator)?
        convertlet = (RexSqlConvertlet) map.get( op );
        if ( convertlet != null ) {
            return convertlet;
        }

        // Is there a convertlet for this class of operator (e.g. SqlBinaryOperator)?
        Class<?> clazz = op.getClass();
        while ( clazz != null ) {
            convertlet = (RexSqlConvertlet) map.get( clazz );
            if ( convertlet != null ) {
                return convertlet;
            }
            clazz = clazz.getSuperclass();
        }

        // Is there a convertlet for this class of expression (e.g. SqlCall)?
        clazz = call.getClass();
        while ( clazz != null ) {
            convertlet = (RexSqlConvertlet) map.get( clazz );
            if ( convertlet != null ) {
                return convertlet;
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }


    /**
     * Registers a convertlet for a given operator instance
     *
     * @param op Operator instance, say {@link ch.unibas.dmi.dbis.polyphenydb.sql.fun.SqlStdOperatorTable#MINUS}
     * @param convertlet Convertlet
     */
    protected void registerOp( SqlOperator op, RexSqlConvertlet convertlet ) {
        map.put( op, convertlet );
    }
}
