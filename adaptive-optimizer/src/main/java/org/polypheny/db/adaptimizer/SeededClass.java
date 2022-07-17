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

package org.polypheny.db.adaptimizer;

import java.util.Random;

public interface SeededClass {

    default long getSeed() {
        return SeedUtil.parseSeed( this.getRnd() );
    }

    default void setSeed( long seed ) {
        this.getRnd().setSeed( seed );
    }

    @SuppressWarnings( "UnusedReturnValue" )
    default long switchSeed() {
        long seed = this.getSeed();
        this.setSeed( this.getRnd().nextLong() );
        return seed;
    }

    Random getRnd();

}
