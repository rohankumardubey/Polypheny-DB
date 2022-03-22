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

package org.polypheny.db.adapter.enumerable;

import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.convert.ConverterRule;
import org.polypheny.db.algebra.logical.graph.LogicalGraphUnwind;
import org.polypheny.db.plan.AlgOptRule;
import org.polypheny.db.plan.AlgTraitSet;
import org.polypheny.db.plan.Convention;

public class EnumerableUnwindRule extends ConverterRule {


    public EnumerableUnwindRule() {
        super( LogicalGraphUnwind.class, Convention.NONE, EnumerableConvention.INSTANCE, "EnumerableUnwind" );
    }


    @Override
    public AlgNode convert( AlgNode alg ) {
        LogicalGraphUnwind unwind = (LogicalGraphUnwind) alg;
        AlgTraitSet out = unwind.getTraitSet().replace( EnumerableConvention.INSTANCE );

        return new EnumerableUnwind( unwind.getCluster(), out, AlgOptRule.convert( unwind.getInput(), out ), unwind.target, unwind.alias );
    }

}
