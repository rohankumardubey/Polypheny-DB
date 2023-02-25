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

package org.polypheny.db.schema;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import lombok.Getter;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Queryable;
import org.polypheny.db.adapter.DataContext;
import org.polypheny.db.adapter.java.AbstractQueryableEntity;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.core.relational.RelModify;
import org.polypheny.db.algebra.core.common.Modify.Operation;
import org.polypheny.db.algebra.logical.relational.LogicalRelModify;
import org.polypheny.db.algebra.type.AlgDataType;
import org.polypheny.db.algebra.type.AlgDataTypeFactory;
import org.polypheny.db.algebra.type.AlgProtoDataType;
import org.polypheny.db.catalog.Catalog.NamespaceType;
import org.polypheny.db.plan.AlgOptCluster;
import org.polypheny.db.plan.AlgOptEntity;
import org.polypheny.db.plan.AlgOptEntity.ToAlgContext;
import org.polypheny.db.plan.AlgTraitSet;
import org.polypheny.db.plan.Convention;
import org.polypheny.db.prepare.Prepare.CatalogReader;
import org.polypheny.db.rex.RexNode;


public class LogicalEntity extends AbstractQueryableEntity implements TranslatableEntity, ScannableEntity, ModifiableEntity {

    private AlgProtoDataType protoRowType;

    @Getter
    private final NamespaceType namespaceType;

    @Getter
    private final String logicalSchemaName;
    @Getter
    private final String logicalTableName;

    @Getter
    private final Set<Long> constraintIds = new TreeSet<>();

    @Getter
    private final List<Long> columnIds;
    @Getter
    private final List<String> logicalColumnNames;


    public LogicalEntity(
            long tableId,
            String logicalSchemaName,
            String logicalTableName,
            List<Long> columnIds,
            List<String> logicalColumnNames,
            AlgProtoDataType protoRowType,
            NamespaceType namespaceType ) {
        super( Object[].class, tableId, null, null );
        this.logicalSchemaName = logicalSchemaName;
        this.logicalTableName = logicalTableName;
        this.columnIds = columnIds;
        this.logicalColumnNames = logicalColumnNames;
        this.protoRowType = protoRowType;
        this.namespaceType = namespaceType;
    }


    public String toString() {
        return "LogicTable {" + logicalSchemaName + "." + logicalTableName + "}";
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
    public AlgDataType getRowType( AlgDataTypeFactory typeFactory ) {
        return protoRowType.apply( typeFactory );
    }


    @Override
    public <T> Queryable<T> asQueryable( DataContext dataContext, SchemaPlus schema, String tableName ) {
        throw new RuntimeException( "asQueryable() is not implemented for Logical Tables!" );
    }


    @Override
    public Enumerable<Object[]> scan( DataContext root ) {
        throw new RuntimeException( "scan() is not implemented for Logical Tables!" );
    }


    @Override
    public AlgNode toAlg( ToAlgContext context, AlgOptEntity algOptEntity, AlgTraitSet traitSet ) {
        throw new RuntimeException( "toAlg() is not implemented for Logical Tables!" );
    }


}
