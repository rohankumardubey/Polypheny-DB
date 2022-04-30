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

package org.polypheny.db.cypher;

import org.junit.Before;
import org.junit.Test;
import org.polypheny.db.webui.models.Result;

public class FilterTest extends CypherTestTemplate {

    @Before
    public void reset() {
        tearDown();
        createSchema();
    }

    ///////////////////////////////////////////////
    ///////// FILTER
    ///////////////////////////////////////////////


    @Test
    public void simplePropertyFilter() {
        execute( SINGLE_NODE_PERSON_1 );
        execute( SINGLE_NODE_ANIMAL );
        Result res = execute( "MATCH (p) WHERE p.age > 3 RETURN p" );
        assertNode( res, 0 );

        assert containsRows( res, true, false );

        res = execute( "MATCH (p) WHERE p.age >= 3 RETURN p" );
        assertNode( res, 0 );

        assert containsRows( res, true, false, Row.of( KIRA ) );
    }

}