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

package ch.unibas.dmi.dbis.polyphenydb.test;


import ch.unibas.dmi.dbis.polyphenydb.util.Bug;
import ch.unibas.dmi.dbis.polyphenydb.util.Sources;
import ch.unibas.dmi.dbis.polyphenydb.util.TestUtil;
import ch.unibas.dmi.dbis.polyphenydb.util.Util;
import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;


/**
 * Tests for the {@code ch.unibas.dmi.dbis.polyphenydb.adapter.cassandra} package.
 *
 * Will start embedded cassandra cluster and populate it from local {@code twissandra.cql} file. All configuration files are located in test classpath.
 *
 * Note that tests will be skipped if running on JDK11 and JDK12 (which is not yet supported by cassandra) see
 * <a href="https://issues.apache.org/jira/browse/CASSANDRA-9608">CASSANDRA-9608</a>.
 */
@Ignore
public class CassandraAdapterTest {
    // TODO MV: enable
    /*
    @ClassRule
    public static final ExternalResource RULE = initCassandraIfEnabled();
*/
    /**
     * Connection factory based on the "mongo-zips" model.
     */
    private static final ImmutableMap<String, String> TWISSANDRA = ImmutableMap.of( "model", Sources.of( CassandraAdapterTest.class.getResource( "/model.json" ) ).file().getAbsolutePath() );


    /**
     * Whether to run this test.
     * Enabled by default, unless explicitly disabled from command line ({@code -Dpolyphenydb.test.cassandra=false}) or running on incompatible JDK version (see below).
     *
     * As of this wiring Cassandra 4.x is not yet released and we're using 3.x (which fails on JDK11 and JDK12). All cassandra tests will be skipped if running on JDK11 and JDK12.
     *
     * @return {@code true} if test is compatible with current environment, {@code false} otherwise
     * @see <a href="https://issues.apache.org/jira/browse/CASSANDRA-9608">CASSANDRA-9608</a>
     */
    private static boolean enabled() {
        final boolean enabled = Util.getBooleanProperty( "polyphenydb.test.cassandra", true );
        Bug.upgrade( "remove JDK version check once current adapter supports Cassandra 4.x" );
        final boolean compatibleJdk = TestUtil.getJavaMajorVersion() != 11 && TestUtil.getJavaMajorVersion() != 12;
        return enabled && compatibleJdk;
    }

    // TODO MV: enable
/*
    private static ExternalResource initCassandraIfEnabled() {
        if ( !enabled() ) {
            // Return NOP resource (to avoid nulls)
            return new ExternalResource() {
                @Override
                public Statement apply( final Statement base, final Description description ) {
                    return super.apply( base, description );
                }
            };
        }

        String configurationFileName = "cassandra.yaml"; // use default one
        // Apache Jenkins often fails with "CassandraAdapterTest Cassandra daemon did not start within timeout (20 sec by default)"
        long startUpTimeoutMillis = TimeUnit.SECONDS.toMillis( 60 );

        CassandraCQLUnit rule = new CassandraCQLUnit( new ClassPathCQLDataSet( "twissandra.cql" ), configurationFileName, startUpTimeoutMillis );

        // This static init is necessary otherwise tests fail with CassandraUnit in IntelliJ (jdk10) should be called right after constructor
        // NullPointerException for DatabaseDescriptor.getDiskFailurePolicy
        // for more info see
        // https://github.com/jsevellec/cassandra-unit/issues/249
        // https://github.com/jsevellec/cassandra-unit/issues/221
        DatabaseDescriptor.daemonInitialization();

        return rule;
    }


    @BeforeClass
    public static void setUp() {
        // run tests only if explicitly enabled
        assumeTrue( "test explicitly disabled", enabled() );
    }


    @Test
    public void testSelect() {
        PolyphenyDbAssert.that()
                .with( TWISSANDRA )
                .query( "select * from \"users\"" )
                .returnsCount( 10 );
    }


    @Test
    public void testFilter() {
        PolyphenyDbAssert.that()
                .with( TWISSANDRA )
                .query( "select * from \"userline\" where \"username\"='!PUBLIC!'" )
                .limit( 1 )
                .returns( "username=!PUBLIC!; time=e8754000-80b8-1fe9-8e73-e3698c967ddd; " + "tweet_id=f3c329de-d05b-11e5-b58b-90e2ba530b12\n" )
                .explainContains( "PLAN=CassandraToEnumerableConverter\n" + "  CassandraFilter(condition=[=($0, '!PUBLIC!')])\n" + "    CassandraTableScan(table=[[twissandra, userline]]" );
    }


    @Test
    public void testFilterUUID() {
        PolyphenyDbAssert.that()
                .with( TWISSANDRA )
                .query( "select * from \"tweets\" where \"tweet_id\"='f3cd759c-d05b-11e5-b58b-90e2ba530b12'" )
                .limit( 1 )
                .returns( "tweet_id=f3cd759c-d05b-11e5-b58b-90e2ba530b12; " + "body=Lacus augue pede posuere.; username=JmuhsAaMdw\n" )
                .explainContains( "PLAN=CassandraToEnumerableConverter\n" + "  CassandraFilter(condition=[=(CAST($0):CHAR(36), 'f3cd759c-d05b-11e5-b58b-90e2ba530b12')])\n" + "    CassandraTableScan(table=[[twissandra, tweets]]" );
    }


    @Test
    public void testSort() {
        PolyphenyDbAssert.that()
                .with( TWISSANDRA )
                .query( "select * from \"userline\" where \"username\" = '!PUBLIC!' order by \"time\" desc" )
                .returnsCount( 146 )
                .explainContains( "PLAN=CassandraToEnumerableConverter\n" + "  CassandraSort(sort0=[$1], dir0=[DESC])\n" + "    CassandraFilter(condition=[=($0, '!PUBLIC!')])\n" );
    }


    @Test
    public void testProject() {
        PolyphenyDbAssert.that()
                .with( TWISSANDRA )
                .query( "select \"tweet_id\" from \"userline\" where \"username\" = '!PUBLIC!' limit 2" )
                .returns( "tweet_id=f3c329de-d05b-11e5-b58b-90e2ba530b12\n" + "tweet_id=f3dbb03a-d05b-11e5-b58b-90e2ba530b12\n" )
                .explainContains( "PLAN=CassandraToEnumerableConverter\n" + "  CassandraLimit(fetch=[2])\n" + "    CassandraProject(tweet_id=[$2])\n" + "      CassandraFilter(condition=[=($0, '!PUBLIC!')])\n" );
    }


    @Test
    public void testProjectAlias() {
        PolyphenyDbAssert.that()
                .with( TWISSANDRA )
                .query( "select \"tweet_id\" as \"foo\" from \"userline\" " + "where \"username\" = '!PUBLIC!' limit 1" )
                .returns( "foo=f3c329de-d05b-11e5-b58b-90e2ba530b12\n" );
    }


    @Test
    public void testProjectConstant() {
        PolyphenyDbAssert.that()
                .with( TWISSANDRA )
                .query( "select 'foo' as \"bar\" from \"userline\" limit 1" )
                .returns( "bar=foo\n" );
    }


    @Test
    public void testLimit() {
        PolyphenyDbAssert.that()
                .with( TWISSANDRA )
                .query( "select \"tweet_id\" from \"userline\" where \"username\" = '!PUBLIC!' limit 8" )
                .explainContains( "CassandraLimit(fetch=[8])\n" );
    }


    @Test
    public void testSortLimit() {
        PolyphenyDbAssert.that()
                .with( TWISSANDRA )
                .query( "select * from \"userline\" where \"username\"='!PUBLIC!' " + "order by \"time\" desc limit 10" )
                .explainContains( "  CassandraLimit(fetch=[10])\n" + "    CassandraSort(sort0=[$1], dir0=[DESC])" );
    }


    @Test
    public void testSortOffset() {
        PolyphenyDbAssert.that()
                .with( TWISSANDRA )
                .query( "select \"tweet_id\" from \"userline\" where " + "\"username\"='!PUBLIC!' limit 2 offset 1" )
                .explainContains( "CassandraLimit(offset=[1], fetch=[2])" )
                .returns( "tweet_id=f3dbb03a-d05b-11e5-b58b-90e2ba530b12\n" + "tweet_id=f3e4182e-d05b-11e5-b58b-90e2ba530b12\n" );
    }
*/
}