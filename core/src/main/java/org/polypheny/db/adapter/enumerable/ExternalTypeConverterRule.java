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


import org.polypheny.db.adapter.enumerable.EnumerableConvention.ExternalConvention;
import org.polypheny.db.algebra.AlgNode;
import org.polypheny.db.algebra.convert.ConverterRule;
import org.polypheny.db.tools.AlgBuilderFactory;

public class ExternalTypeConverterRule extends ConverterRule {

    public ExternalTypeConverterRule( AlgBuilderFactory builder ) {
        super( AlgNode.class, r -> true, EnumerableConvention.INSTANCE, ExternalConvention.INSTANCE, builder, "ExternalTypeConverterRule" );
    }


    @Override
    public AlgNode convert( AlgNode alg ) {
        return new ExternalTypeConverter( alg.getCluster(), alg );
    }

}
