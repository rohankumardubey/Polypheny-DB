/*
 * Copyright 2019-2020 The Polypheny Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file incorporates code covered by the following terms:
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
 */

package org.polypheny.db.schema;


import java.lang.reflect.Type;
import org.apache.calcite.linq4j.Queryable;
import org.apache.calcite.linq4j.tree.Expression;
import org.polypheny.db.adapter.DataContext;


/**
 * Extension to {@link Entity} that can translate itself to a {@link Queryable}.
 */
public interface QueryableEntity extends Entity {

    /**
     * Converts this table into a {@link Queryable}.
     */
    <T> Queryable<T> asQueryable( DataContext dataContext, SchemaPlus schema, String tableName );

    /**
     * Returns the element type of the collection that will implement this table.
     */
    Type getElementType();

    /**
     * Generates an expression with which this table can be referenced in generated code.
     *
     * @param schema Schema
     * @param tableName Table name (unique within schema)
     * @param clazz The desired collection class; for example {@code Queryable}.
     */
    Expression getExpression( SchemaPlus schema, String tableName, Class<?> clazz );
}

