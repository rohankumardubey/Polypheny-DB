/*
 * Copyright 2019-2023 The Polypheny Project
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

package org.polypheny.db.adapter.jdbc;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import org.polypheny.db.adapter.java.JavaTypeFactory;
import org.polypheny.db.adapter.jdbc.rel2sql.AlgToSqlConverter;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.catalog.Catalog;
import org.polypheny.db.catalog.entity.CatalogPartitionPlacement;
import org.polypheny.db.catalog.entity.CatalogTable;
import org.polypheny.db.languages.ParserPos;
import org.polypheny.db.sql.language.SqlDialect;
import org.polypheny.db.sql.language.SqlIdentifier;
import org.polypheny.db.util.Util;


/**
 * State for generating a SQL statement.
 */
public class JdbcImplementor extends AlgToSqlConverter {

    private final JdbcSchema schema;


    public JdbcImplementor( SqlDialect dialect, JavaTypeFactory typeFactory, JdbcSchema schema ) {
        super( dialect );
        Util.discard( typeFactory );
        this.schema = schema;
    }


    /**
     * @see #dispatch
     */
    public Result visit( JdbcScan scan ) {
        return result( scan.jdbcTable.physicalTableName(), ImmutableList.of( Clause.FROM ), scan, null );
    }


    public Result implement( AlgNode node ) {
        return dispatch( node );
    }


    @Override
    public SqlIdentifier getPhysicalTableName( CatalogPartitionPlacement placement ) {
        return new SqlIdentifier( Arrays.asList( placement.physicalSchemaName, placement.physicalTableName ), ParserPos.ZERO );
    }


    @Override
    public SqlIdentifier getPhysicalColumnName( CatalogPartitionPlacement placement, String columnName ) {
        CatalogTable catalogTable = Catalog.getInstance().getTable( placement.tableId );
        JdbcEntity table = schema.getTableMap().get( catalogTable.name + "_" + placement.partitionId );
        if ( table.hasPhysicalColumnName( columnName ) ) {
            return table.physicalColumnName( columnName );
        } else {
            return new SqlIdentifier( "_" + columnName, ParserPos.ZERO );
        }
    }

}

