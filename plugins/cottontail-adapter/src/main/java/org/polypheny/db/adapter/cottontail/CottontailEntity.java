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
 */

package org.polypheny.db.adapter.cottontail;

import java.util.List;
import lombok.Getter;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.linq4j.Queryable;
import org.polypheny.db.adapter.DataContext;
import org.polypheny.db.adapter.cottontail.algebra.CottontailScan;
import org.polypheny.db.adapter.cottontail.enumberable.CottontailQueryEnumerable;
import org.polypheny.db.adapter.java.AbstractQueryableEntity;
import org.polypheny.db.adapter.java.JavaTypeFactory;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.core.relational.RelModify;
import org.polypheny.db.algebra.core.common.Modify.Operation;
import org.polypheny.db.algebra.logical.relational.LogicalRelModify;
import org.polypheny.db.algebra.type.AlgDataType;
import org.polypheny.db.algebra.type.AlgDataTypeFactory;
import org.polypheny.db.algebra.type.AlgProtoDataType;
import org.polypheny.db.plan.AlgOptCluster;
import org.polypheny.db.plan.AlgOptEntity;
import org.polypheny.db.plan.AlgOptEntity.ToAlgContext;
import org.polypheny.db.plan.AlgTraitSet;
import org.polypheny.db.plan.Convention;
import org.polypheny.db.prepare.Prepare.CatalogReader;
import org.polypheny.db.rex.RexNode;
import org.polypheny.db.schema.ModifiableEntity;
import org.polypheny.db.schema.PolyphenyDbSchema;
import org.polypheny.db.schema.SchemaPlus;
import org.polypheny.db.schema.TranslatableEntity;
import org.polypheny.db.schema.impl.AbstractTableQueryable;
import org.vitrivr.cottontail.grpc.CottontailGrpc.EntityName;
import org.vitrivr.cottontail.grpc.CottontailGrpc.From;
import org.vitrivr.cottontail.grpc.CottontailGrpc.Metadata;
import org.vitrivr.cottontail.grpc.CottontailGrpc.Query;
import org.vitrivr.cottontail.grpc.CottontailGrpc.QueryMessage;
import org.vitrivr.cottontail.grpc.CottontailGrpc.Scan;
import org.vitrivr.cottontail.grpc.CottontailGrpc.SchemaName;


public class CottontailEntity extends AbstractQueryableEntity implements TranslatableEntity, ModifiableEntity {

    private AlgProtoDataType protoRowType;
    private CottontailSchema cottontailSchema;

    @Getter
    private EntityName entity;

    @Getter
    private final String physicalSchemaName;
    @Getter
    private final String physicalTableName;
    private final List<String> physicalColumnNames;

    private final List<String> logicalColumnNames;


    protected CottontailEntity(
            CottontailSchema cottontailSchema,
            String logicalSchemaName,
            String logicalTableName,
            List<String> logicalColumnNames,
            AlgProtoDataType protoRowType,
            String physicalSchemaName,
            String physicalTableName,
            List<String> physicalColumnNames,
            Long tableId,
            long partitionId, long adapterId ) {
        super( Object[].class, tableId, partitionId, adapterId );

        this.cottontailSchema = cottontailSchema;
        this.protoRowType = protoRowType;

        this.logicalColumnNames = logicalColumnNames;
        this.physicalSchemaName = physicalSchemaName;
        this.physicalTableName = physicalTableName;
        this.physicalColumnNames = physicalColumnNames;

        this.entity = EntityName.newBuilder()
                .setName( this.physicalTableName )
                .setSchema( SchemaName.newBuilder().setName( physicalSchemaName ).build() )
                .build();
    }


    public String getPhysicalColumnName( String logicalColumnName ) {
        return this.physicalColumnNames.get( this.logicalColumnNames.indexOf( logicalColumnName ) );
    }


    @Override
    public String toString() {
        return "CottontailTable {" + physicalSchemaName + "." + physicalTableName + "}";
    }


    @Override
    public RelModify toModificationAlg(
            AlgOptCluster cluster,
            AlgOptEntity table,
            CatalogReader catalogReader,
            AlgNode input,
            Operation operation,
            List<String> updateColumnList,
            List<RexNode> sourceExpressionList,
            boolean flattened ) {
        this.cottontailSchema.getConvention().register( cluster.getPlanner() );
        return new LogicalRelModify(
                cluster,
                cluster.traitSetOf( Convention.NONE ),
                table,
                catalogReader,
                input,
                operation,
                updateColumnList,
                sourceExpressionList,
                flattened );
    }


    @Override
    public Queryable<Object[]> asQueryable( DataContext dataContext, PolyphenyDbSchema schema, String tableName ) {
        return new CottontailTableQueryable( dataContext, schema, tableName );
    }


    @Override
    public AlgNode toAlg( ToAlgContext context, AlgOptEntity algOptEntity, AlgTraitSet traitSet ) {
        return new CottontailScan( context.getCluster(), algOptEntity, this, traitSet, this.cottontailSchema.getConvention() );
    }


    @Override
    public AlgDataType getRowType( AlgDataTypeFactory typeFactory ) {
        return protoRowType.apply( typeFactory );
    }


    public CottontailConvention getUnderlyingConvention() {
        return this.cottontailSchema.getConvention();
    }


    private class CottontailTableQueryable extends AbstractTableQueryable<Object[]> {

        public CottontailTableQueryable( DataContext dataContext, SchemaPlus schema, String tableName ) {
            super( dataContext, schema, CottontailEntity.this, tableName );
        }


        @Override
        public Enumerator enumerator() {
            final JavaTypeFactory typeFactory = dataContext.getTypeFactory();
            final CottontailEntity cottontailTable = (CottontailEntity) this.table;
            final long txId = cottontailTable.cottontailSchema.getWrapper().beginOrContinue( this.dataContext.getStatement().getTransaction() );
            final Query query = Query.newBuilder()
                    .setFrom( From.newBuilder().setScan( Scan.newBuilder().setEntity( cottontailTable.entity ) ).build() )
                    .build();
            final QueryMessage queryMessage = QueryMessage.newBuilder()
                    .setMetadata( Metadata.newBuilder().setTransactionId( txId ) )
                    .setQuery( query )
                    .build();
            return new CottontailQueryEnumerable(
                    cottontailTable.cottontailSchema.getWrapper().query( queryMessage ),
                    new CottontailQueryEnumerable.RowTypeParser( cottontailTable.getRowType( typeFactory ), cottontailTable.physicalColumnNames )
            ).enumerator();
        }

    }

}
