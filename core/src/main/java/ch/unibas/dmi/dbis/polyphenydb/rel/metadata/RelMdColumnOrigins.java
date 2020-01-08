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

package ch.unibas.dmi.dbis.polyphenydb.rel.metadata;


import ch.unibas.dmi.dbis.polyphenydb.plan.RelOptTable;
import ch.unibas.dmi.dbis.polyphenydb.rel.RelNode;
import ch.unibas.dmi.dbis.polyphenydb.rel.core.Aggregate;
import ch.unibas.dmi.dbis.polyphenydb.rel.core.AggregateCall;
import ch.unibas.dmi.dbis.polyphenydb.rel.core.Exchange;
import ch.unibas.dmi.dbis.polyphenydb.rel.core.Filter;
import ch.unibas.dmi.dbis.polyphenydb.rel.core.Join;
import ch.unibas.dmi.dbis.polyphenydb.rel.core.Project;
import ch.unibas.dmi.dbis.polyphenydb.rel.core.SetOp;
import ch.unibas.dmi.dbis.polyphenydb.rel.core.Sort;
import ch.unibas.dmi.dbis.polyphenydb.rel.core.TableFunctionScan;
import ch.unibas.dmi.dbis.polyphenydb.rex.RexInputRef;
import ch.unibas.dmi.dbis.polyphenydb.rex.RexNode;
import ch.unibas.dmi.dbis.polyphenydb.rex.RexVisitor;
import ch.unibas.dmi.dbis.polyphenydb.rex.RexVisitorImpl;
import ch.unibas.dmi.dbis.polyphenydb.util.BuiltInMethod;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;


/**
 * RelMdColumnOrigins supplies a default implementation of {@link RelMetadataQuery#getColumnOrigins} for the standard logical algebra.
 */
public class RelMdColumnOrigins implements MetadataHandler<BuiltInMetadata.ColumnOrigin> {

    public static final RelMetadataProvider SOURCE = ReflectiveRelMetadataProvider.reflectiveSource( BuiltInMethod.COLUMN_ORIGIN.method, new RelMdColumnOrigins() );


    private RelMdColumnOrigins() {
    }


    @Override
    public MetadataDef<BuiltInMetadata.ColumnOrigin> getDef() {
        return BuiltInMetadata.ColumnOrigin.DEF;
    }


    public Set<RelColumnOrigin> getColumnOrigins( Aggregate rel, RelMetadataQuery mq, int iOutputColumn ) {
        if ( iOutputColumn < rel.getGroupCount() ) {
            // Group columns pass through directly.
            return mq.getColumnOrigins( rel.getInput(), iOutputColumn );
        }

        if ( rel.indicator ) {
            if ( iOutputColumn < rel.getGroupCount() + rel.getIndicatorCount() ) {
                // The indicator column is originated here.
                return ImmutableSet.of();
            }
        }

        // Aggregate columns are derived from input columns
        AggregateCall call = rel.getAggCallList().get( iOutputColumn - rel.getGroupCount() - rel.getIndicatorCount() );

        final Set<RelColumnOrigin> set = new HashSet<>();
        for ( Integer iInput : call.getArgList() ) {
            Set<RelColumnOrigin> inputSet = mq.getColumnOrigins( rel.getInput(), iInput );
            inputSet = createDerivedColumnOrigins( inputSet );
            if ( inputSet != null ) {
                set.addAll( inputSet );
            }
        }
        return set;
    }


    public Set<RelColumnOrigin> getColumnOrigins( Join rel, RelMetadataQuery mq, int iOutputColumn ) {
        int nLeftColumns = rel.getLeft().getRowType().getFieldList().size();
        Set<RelColumnOrigin> set;
        boolean derived = false;
        if ( iOutputColumn < nLeftColumns ) {
            set = mq.getColumnOrigins( rel.getLeft(), iOutputColumn );
            if ( rel.getJoinType().generatesNullsOnLeft() ) {
                derived = true;
            }
        } else {
            set = mq.getColumnOrigins( rel.getRight(), iOutputColumn - nLeftColumns );
            if ( rel.getJoinType().generatesNullsOnRight() ) {
                derived = true;
            }
        }
        if ( derived ) {
            // nulls are generated due to outer join; that counts as derivation
            set = createDerivedColumnOrigins( set );
        }
        return set;
    }


    public Set<RelColumnOrigin> getColumnOrigins( SetOp rel, RelMetadataQuery mq, int iOutputColumn ) {
        final Set<RelColumnOrigin> set = new HashSet<>();
        for ( RelNode input : rel.getInputs() ) {
            Set<RelColumnOrigin> inputSet = mq.getColumnOrigins( input, iOutputColumn );
            if ( inputSet == null ) {
                return null;
            }
            set.addAll( inputSet );
        }
        return set;
    }


    public Set<RelColumnOrigin> getColumnOrigins( Project rel, final RelMetadataQuery mq, int iOutputColumn ) {
        final RelNode input = rel.getInput();
        RexNode rexNode = rel.getProjects().get( iOutputColumn );

        if ( rexNode instanceof RexInputRef ) {
            // Direct reference:  no derivation added.
            RexInputRef inputRef = (RexInputRef) rexNode;
            return mq.getColumnOrigins( input, inputRef.getIndex() );
        }

        // Anything else is a derivation, possibly from multiple columns.
        final Set<RelColumnOrigin> set = new HashSet<>();
        RexVisitor visitor =
                new RexVisitorImpl<Void>( true ) {
                    @Override
                    public Void visitInputRef( RexInputRef inputRef ) {
                        Set<RelColumnOrigin> inputSet = mq.getColumnOrigins( input, inputRef.getIndex() );
                        if ( inputSet != null ) {
                            set.addAll( inputSet );
                        }
                        return null;
                    }
                };
        rexNode.accept( visitor );

        return createDerivedColumnOrigins( set );
    }


    public Set<RelColumnOrigin> getColumnOrigins( Filter rel, RelMetadataQuery mq, int iOutputColumn ) {
        return mq.getColumnOrigins( rel.getInput(), iOutputColumn );
    }


    public Set<RelColumnOrigin> getColumnOrigins( Sort rel, RelMetadataQuery mq, int iOutputColumn ) {
        return mq.getColumnOrigins( rel.getInput(), iOutputColumn );
    }


    public Set<RelColumnOrigin> getColumnOrigins( Exchange rel, RelMetadataQuery mq, int iOutputColumn ) {
        return mq.getColumnOrigins( rel.getInput(), iOutputColumn );
    }


    public Set<RelColumnOrigin> getColumnOrigins( TableFunctionScan rel, RelMetadataQuery mq, int iOutputColumn ) {
        final Set<RelColumnOrigin> set = new HashSet<>();
        Set<RelColumnMapping> mappings = rel.getColumnMappings();
        if ( mappings == null ) {
            if ( rel.getInputs().size() > 0 ) {
                // This is a non-leaf transformation:  say we don't know about origins, because there are probably columns below.
                return null;
            } else {
                // This is a leaf transformation: say there are fer sure no column origins.
                return set;
            }
        }
        for ( RelColumnMapping mapping : mappings ) {
            if ( mapping.iOutputColumn != iOutputColumn ) {
                continue;
            }
            final RelNode input = rel.getInputs().get( mapping.iInputRel );
            final int column = mapping.iInputColumn;
            Set<RelColumnOrigin> origins = mq.getColumnOrigins( input, column );
            if ( origins == null ) {
                return null;
            }
            if ( mapping.derived ) {
                origins = createDerivedColumnOrigins( origins );
            }
            set.addAll( origins );
        }
        return set;
    }


    // Catch-all rule when none of the others apply.
    public Set<RelColumnOrigin> getColumnOrigins( RelNode rel, RelMetadataQuery mq, int iOutputColumn ) {
        // NOTE jvs 28-Mar-2006: We may get this wrong for a physical table expression which supports projections.  In that case, it's up to the plugin writer to override with the correct information.
        if ( rel.getInputs().size() > 0 ) {
            // No generic logic available for non-leaf rels.
            return null;
        }

        final Set<RelColumnOrigin> set = new HashSet<>();

        RelOptTable table = rel.getTable();
        if ( table == null ) {
            // Somebody is making column values up out of thin air, like a VALUES clause, so we return an empty set.
            return set;
        }

        // Detect the case where a physical table expression is performing projection, and say we don't know instead of making any assumptions.
        // (Theoretically we could try to map the projection using column names.)  This detection assumes the table expression doesn't handle rename as well.
        if ( table.getRowType() != rel.getRowType() ) {
            return null;
        }

        set.add( new RelColumnOrigin( table, iOutputColumn, false ) );
        return set;
    }


    private Set<RelColumnOrigin> createDerivedColumnOrigins( Set<RelColumnOrigin> inputSet ) {
        if ( inputSet == null ) {
            return null;
        }
        final Set<RelColumnOrigin> set = new HashSet<>();
        for ( RelColumnOrigin rco : inputSet ) {
            RelColumnOrigin derived =
                    new RelColumnOrigin(
                            rco.getOriginTable(),
                            rco.getOriginColumnOrdinal(),
                            true );
            set.add( derived );
        }
        return set;
    }
}
