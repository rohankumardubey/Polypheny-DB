/*
 * Copyright 2019-2022 The Polypheny Project
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

package org.polypheny.db.adaptimizer.rndqueries;


import lombok.Getter;
import lombok.Setter;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.core.JoinAlgType;
import org.polypheny.db.http.model.SortState;


/**
 * Model for a {@link AlgNode} coming from the RelAlg-Builder in the AdaptiveOptimizer context.
 */
public class AdaptiveNode {

    public AdaptiveNode() {}

    public AdaptiveNode( int depth ) {
        this.depth = depth;
    }

    /**
     * Type of the AlgNode, e.g. TableScan
     */
    public String type;

    /**
     * Type of Table, e.g. Table, View
     */
    public String tableType;

    /**
     * Children of this node in the tree
     */
    public AdaptiveNode[] children;

    /**
     * Number of inputs of a node.
     * Required by the AlgBuilder
     */
    public int inputCount;

    //tableScan
    public String tableName;

    //join
    public JoinAlgType join;
    //join condition
    public String operator;
    public String col1;
    public String col2;

    //filter
    //(String operator)
    public String field;
    public String filter;

    //project
    public String[] fields;

    //aggregate
    public String groupBy;
    public String aggregation;


    public String alias;
    //(String field)

    //sort
    public SortState[] sortColumns;

    //union, minus
    public boolean all;

    @Getter
    @Setter
    private AdaptiveTableRecord adaptiveTableRecord;

    @Getter
    @Setter
    private int depth;

}