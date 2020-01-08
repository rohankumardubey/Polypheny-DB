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

package ch.unibas.dmi.dbis.polyphenydb.tools;


import ch.unibas.dmi.dbis.polyphenydb.plan.Context;
import ch.unibas.dmi.dbis.polyphenydb.plan.RelOptCostFactory;
import ch.unibas.dmi.dbis.polyphenydb.plan.RelOptTable;
import ch.unibas.dmi.dbis.polyphenydb.plan.RelTraitDef;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataTypeSystem;
import ch.unibas.dmi.dbis.polyphenydb.rex.RexExecutor;
import ch.unibas.dmi.dbis.polyphenydb.schema.SchemaPlus;
import ch.unibas.dmi.dbis.polyphenydb.sql.SqlOperatorTable;
import ch.unibas.dmi.dbis.polyphenydb.sql.parser.SqlParser.SqlParserConfig;
import ch.unibas.dmi.dbis.polyphenydb.sql2rel.SqlRexConvertletTable;
import ch.unibas.dmi.dbis.polyphenydb.sql2rel.SqlToRelConverter;
import com.google.common.collect.ImmutableList;


/**
 * Interface that describes how to configure planning sessions generated using the Frameworks tools.
 *
 * @see Frameworks#newConfigBuilder()
 */
public interface FrameworkConfig {

    /**
     * The configuration of SQL parser.
     */
    SqlParserConfig getParserConfig();

    /**
     * The configuration of {@link SqlToRelConverter}.
     */
    SqlToRelConverter.Config getSqlToRelConverterConfig();

    /**
     * Returns the default schema that should be checked before looking at the root schema.  Returns null to only consult the root schema.
     */
    SchemaPlus getDefaultSchema();

    /**
     * Returns the executor used to evaluate constant expressions.
     */
    RexExecutor getExecutor();

    /**
     * Returns a list of one or more programs used during the course of query evaluation.
     *
     * The common use case is when there is a single program created using {@link Programs#of(RuleSet)} and {@link ch.unibas.dmi.dbis.polyphenydb.tools.Planner#transform} will only be called once.
     *
     * However, consumers may also create programs not based on rule sets, register multiple programs, and do multiple repetitions of {@link Planner#transform} planning cycles using different indices.
     *
     * The order of programs provided here determines the zero-based indices of programs elsewhere in this class.
     */
    ImmutableList<Program> getPrograms();

    /**
     * Returns operator table that should be used to resolve functions and operators during query validation.
     */
    SqlOperatorTable getOperatorTable();

    /**
     * Returns the cost factory that should be used when creating the planner.
     * If null, use the default cost factory for that planner.
     */
    RelOptCostFactory getCostFactory();

    /**
     * Returns a list of trait definitions.
     *
     * If the list is not null, the planner first de-registers any existing {@link RelTraitDef}s, then registers the {@code RelTraitDef}s in this list.
     *
     * The order of {@code RelTraitDef}s in the list matters if the planner is VolcanoPlanner. The planner calls {@link RelTraitDef#convert} in the order of this list. The most important trait comes first in the list,
     * followed by the second most important one, etc.
     */
    ImmutableList<RelTraitDef> getTraitDefs();

    /**
     * Returns the convertlet table that should be used when converting from SQL to row expressions
     */
    SqlRexConvertletTable getConvertletTable();

    /**
     * Returns the PlannerContext that should be made available during planning by calling {@link ch.unibas.dmi.dbis.polyphenydb.plan.RelOptPlanner#getContext()}.
     */
    Context getContext();

    /**
     * Returns the type system.
     */
    RelDataTypeSystem getTypeSystem();

    /**
     * Returns a view expander.
     */
    RelOptTable.ViewExpander getViewExpander();

    /**
     * Returns a prepare context.
     */
    ch.unibas.dmi.dbis.polyphenydb.jdbc.Context getPrepareContext();
}
