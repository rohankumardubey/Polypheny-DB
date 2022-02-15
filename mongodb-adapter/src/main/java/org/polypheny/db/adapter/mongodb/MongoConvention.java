/*
 * Copyright 2019-2021 The Polypheny Project
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

package org.polypheny.db.adapter.mongodb;

import org.polypheny.db.plan.AlgOptPlanner;
import org.polypheny.db.plan.AlgOptRule;
import org.polypheny.db.plan.Convention;

public class MongoConvention extends Convention.Impl {

    public static final double COST_MULTIPLIER = 0.8d;

    public static final MongoConvention INSTANCE = new MongoConvention();

    public static final boolean mapsDocuments = true;


    public MongoConvention() {
        super( "MONGO", MongoAlg.class, MongoTypeDefinition.INSTANCE );
    }


    @Override
    public void register( AlgOptPlanner planner ) {
        for ( AlgOptRule rule : MongoRules.RULES ) {
            planner.addRule( rule );
        }
    }

}
