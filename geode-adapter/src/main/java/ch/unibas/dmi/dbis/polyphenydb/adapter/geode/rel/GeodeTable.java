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

package ch.unibas.dmi.dbis.polyphenydb.adapter.geode.rel;


import ch.unibas.dmi.dbis.polyphenydb.adapter.geode.util.GeodeUtils;
import ch.unibas.dmi.dbis.polyphenydb.adapter.geode.util.JavaTypeFactoryExtImpl;
import ch.unibas.dmi.dbis.polyphenydb.adapter.java.AbstractQueryableTable;
import ch.unibas.dmi.dbis.polyphenydb.plan.RelOptCluster;
import ch.unibas.dmi.dbis.polyphenydb.plan.RelOptTable;
import ch.unibas.dmi.dbis.polyphenydb.rel.RelNode;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataType;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataTypeFactory;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataTypeImpl;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelProtoDataType;
import ch.unibas.dmi.dbis.polyphenydb.runtime.Hook;
import ch.unibas.dmi.dbis.polyphenydb.schema.SchemaPlus;
import ch.unibas.dmi.dbis.polyphenydb.schema.TranslatableTable;
import ch.unibas.dmi.dbis.polyphenydb.schema.impl.AbstractTableQueryable;
import ch.unibas.dmi.dbis.polyphenydb.sql.type.SqlTypeName;
import ch.unibas.dmi.dbis.polyphenydb.util.Util;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.linq4j.Queryable;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.SelectResults;


/**
 * Table based on a Geode Region
 */
@Slf4j
public class GeodeTable extends AbstractQueryableTable implements TranslatableTable {

    private final String regionName;
    private final RelDataType rowType;


    GeodeTable( Region<?, ?> region ) {
        super( Object[].class );
        this.regionName = region.getName();
        this.rowType = GeodeUtils.autodetectRelTypeFromRegion( region );
    }


    public String toString() {
        return "GeodeTable {" + regionName + "}";
    }


    /**
     * Executes an OQL query on the underlying table.
     *
     * Called by the {@link GeodeQueryable} which in turn is called via the generated code.
     *
     * @param clientCache Geode client cache
     * @param fields List of fields to project
     * @param predicates A list of predicates which should be used in the query
     * @return Enumerator of results
     */
    public Enumerable<Object> query( final GemFireCache clientCache, final List<Map.Entry<String, Class>> fields, final List<Map.Entry<String, String>> selectFields, final List<Map.Entry<String, String>> aggregateFunctions,
            final List<String> groupByFields, List<String> predicates, List<String> orderByFields, Long limit ) {

        final RelDataTypeFactory typeFactory = new JavaTypeFactoryExtImpl();
        final RelDataTypeFactory.Builder fieldInfo = typeFactory.builder();

        for ( Map.Entry<String, Class> field : fields ) {
            SqlTypeName typeName = typeFactory.createJavaType( field.getValue() ).getSqlTypeName();
            fieldInfo.add( field.getKey(), typeFactory.createSqlType( typeName ) ).nullable( true );
        }

        final RelProtoDataType resultRowType = RelDataTypeImpl.proto( fieldInfo.build() );

        ImmutableMap<String, String> aggFuncMap = ImmutableMap.of();
        if ( !aggregateFunctions.isEmpty() ) {
            ImmutableMap.Builder<String, String> aggFuncMapBuilder = ImmutableMap.builder();
            for ( Map.Entry<String, String> e : aggregateFunctions ) {
                aggFuncMapBuilder.put( e.getKey(), e.getValue() );
            }
            aggFuncMap = aggFuncMapBuilder.build();
        }

        // Construct the list of fields to project
        Builder<String> selectBuilder = ImmutableList.builder();
        if ( !groupByFields.isEmpty() ) {
            // manually add GROUP BY to select clause (GeodeProjection was not visited)
            for ( String groupByField : groupByFields ) {
                selectBuilder.add( groupByField + " AS " + groupByField );
            }

            if ( !aggFuncMap.isEmpty() ) {
                for ( Map.Entry<String, String> e : aggFuncMap.entrySet() ) {
                    selectBuilder.add( e.getValue() + " AS " + e.getKey() );
                }
            }
        } else {
            if ( selectFields.isEmpty() ) {
                if ( !aggFuncMap.isEmpty() ) {
                    for ( Map.Entry<String, String> e : aggFuncMap.entrySet() ) {
                        selectBuilder.add( e.getValue() + " AS " + e.getKey() );
                    }
                } else {
                    selectBuilder.add( "*" );
                }
            } else {
                if ( !aggFuncMap.isEmpty() ) {
                    for ( Map.Entry<String, String> e : aggFuncMap.entrySet() ) {
                        selectBuilder.add( e.getValue() + " AS " + e.getKey() );
                    }
                } else {
                    for ( Map.Entry<String, String> field : selectFields ) {
                        selectBuilder.add( field.getKey() + " AS " + field.getValue() );
                    }
                }
            }
        }

        final String oqlSelectStatement = Util.toString( selectBuilder.build(), "", ", ", "" );

        // Combine all predicates conjunctively
        String whereClause = "";
        if ( !predicates.isEmpty() ) {
            whereClause = " WHERE ";
            whereClause += Util.toString( predicates, "", " AND ", "" );
        }

        // Build and issue the query and return an Enumerator over the results
        StringBuilder queryBuilder = new StringBuilder( "SELECT " );
        queryBuilder.append( oqlSelectStatement );
        queryBuilder.append( " FROM /" + regionName );
        queryBuilder.append( whereClause );

        if ( !groupByFields.isEmpty() ) {
            queryBuilder.append( Util.toString( groupByFields, " GROUP BY ", ", ", "" ) );
        }

        if ( !orderByFields.isEmpty() ) {
            queryBuilder.append( Util.toString( orderByFields, " ORDER BY ", ", ", "" ) );
        }
        if ( limit != null ) {
            queryBuilder.append( " LIMIT " + limit );
        }

        final String oqlQuery = queryBuilder.toString();

        Hook.QUERY_PLAN.run( oqlQuery );
        log.info( "OQL: {}", oqlQuery );

        return new AbstractEnumerable<Object>() {
            @Override
            public Enumerator<Object> enumerator() {
                final QueryService queryService = clientCache.getQueryService();
                try {
                    SelectResults results = (SelectResults) queryService.newQuery( oqlQuery ).execute();
                    return new GeodeEnumerator( results, resultRowType );
                } catch ( Exception e ) {
                    String message = String.format( Locale.ROOT, "Failed to execute query [%s] on %s", oqlQuery, clientCache.getName() );
                    throw new RuntimeException( message, e );
                }
            }
        };
    }


    @Override
    public <T> Queryable<T> asQueryable( QueryProvider queryProvider, SchemaPlus schema, String tableName ) {
        return new GeodeQueryable<>( queryProvider, schema, this, tableName );
    }


    @Override
    public RelNode toRel( RelOptTable.ToRelContext context, RelOptTable relOptTable ) {
        final RelOptCluster cluster = context.getCluster();
        return new GeodeTableScan( cluster, cluster.traitSetOf( GeodeRel.CONVENTION ), relOptTable, this, null );
    }


    @Override
    public RelDataType getRowType( RelDataTypeFactory typeFactory ) {
        return rowType;
    }


    /**
     * Implementation of {@link Queryable} based on a {@link GeodeTable}.
     *
     * @param <T> type
     */
    public static class GeodeQueryable<T> extends AbstractTableQueryable<T> {

        public GeodeQueryable( QueryProvider queryProvider, SchemaPlus schema, GeodeTable table, String tableName ) {
            super( queryProvider, schema, table, tableName );
        }


        // tzolov: this should never be called for queryable tables???
        @Override
        public Enumerator<T> enumerator() {
            throw new UnsupportedOperationException( "Enumerator on Queryable should never be called" );
        }


        private GeodeTable getTable() {
            return (GeodeTable) table;
        }


        private GemFireCache getClientCache() {
            return schema.unwrap( GeodeSchema.class ).cache;
        }


        /**
         * Called via code-generation.
         */
        @SuppressWarnings("UnusedDeclaration")
        public Enumerable<Object> query( List<Map.Entry<String, Class>> fields, List<Map.Entry<String, String>> selectFields, List<Map.Entry<String, String>> aggregateFunctions,
                List<String> groupByFields, List<String> predicates, List<String> order, Long limit ) {
            return getTable().query( getClientCache(), fields, selectFields, aggregateFunctions, groupByFields, predicates, order, limit );
        }
    }
}
