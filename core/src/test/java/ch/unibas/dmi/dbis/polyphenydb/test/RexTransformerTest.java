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
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2019 Databases and Information Systems Research Group, University of Basel, Switzerland
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package ch.unibas.dmi.dbis.polyphenydb.test;


import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ch.unibas.dmi.dbis.polyphenydb.jdbc.JavaTypeFactoryImpl;
import ch.unibas.dmi.dbis.polyphenydb.plan.RelOptUtil;
import ch.unibas.dmi.dbis.polyphenydb.plan.RelOptUtil.Logic;
import ch.unibas.dmi.dbis.polyphenydb.rel.RelNode;
import ch.unibas.dmi.dbis.polyphenydb.rel.logical.LogicalJoin;
import ch.unibas.dmi.dbis.polyphenydb.rel.logical.LogicalProject;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataType;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataTypeFactory;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataTypeField;
import ch.unibas.dmi.dbis.polyphenydb.rel.type.RelDataTypeSystem;
import ch.unibas.dmi.dbis.polyphenydb.rex.LogicVisitor;
import ch.unibas.dmi.dbis.polyphenydb.rex.RexBuilder;
import ch.unibas.dmi.dbis.polyphenydb.rex.RexInputRef;
import ch.unibas.dmi.dbis.polyphenydb.rex.RexLiteral;
import ch.unibas.dmi.dbis.polyphenydb.rex.RexNode;
import ch.unibas.dmi.dbis.polyphenydb.rex.RexTransformer;
import ch.unibas.dmi.dbis.polyphenydb.sql.fun.SqlStdOperatorTable;
import ch.unibas.dmi.dbis.polyphenydb.sql.type.SqlTypeName;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests transformations on rex nodes.
 */
public class RexTransformerTest {

    RexBuilder rexBuilder = null;
    RexNode x;
    RexNode y;
    RexNode z;
    RexNode trueRex;
    RexNode falseRex;
    RelDataType boolRelDataType;
    RelDataTypeFactory typeFactory;


    /**
     * Converts a SQL string to a relational expression using mock schema.
     */
    private static RelNode toRel( String sql ) {
        final SqlToRelTestBase test = new SqlToRelTestBase() {
        };
        return test.createTester().convertSqlToRel( sql ).rel;
    }


    @Before
    public void setUp() {
        typeFactory = new JavaTypeFactoryImpl( RelDataTypeSystem.DEFAULT );
        rexBuilder = new RexBuilder( typeFactory );
        boolRelDataType = typeFactory.createSqlType( SqlTypeName.BOOLEAN );

        x = new RexInputRef(
                0,
                typeFactory.createTypeWithNullability( boolRelDataType, true ) );
        y = new RexInputRef(
                1,
                typeFactory.createTypeWithNullability( boolRelDataType, true ) );
        z = new RexInputRef(
                2,
                typeFactory.createTypeWithNullability( boolRelDataType, true ) );
        trueRex = rexBuilder.makeLiteral( true );
        falseRex = rexBuilder.makeLiteral( false );
    }


    @After
    public void testDown() {
        typeFactory = null;
        rexBuilder = null;
        boolRelDataType = null;
        x = y = z = trueRex = falseRex = null;
    }


    void check( Boolean encapsulateType, RexNode node, String expected ) {
        RexNode root;
        if ( null == encapsulateType ) {
            root = node;
        } else if ( encapsulateType.equals( Boolean.TRUE ) ) {
            root = isTrue( node );
        } else {
            // encapsulateType.equals(Boolean.FALSE)
            root = isFalse( node );
        }

        RexTransformer transformer = new RexTransformer( root, rexBuilder );
        RexNode result = transformer.transformNullSemantics();
        String actual = result.toString();
        if ( !actual.equals( expected ) ) {
            String msg = "\nExpected=<" + expected + ">\n  Actual=<" + actual + ">";
            fail( msg );
        }
    }


    private RexNode lessThan( RexNode a0, RexNode a1 ) {
        return rexBuilder.makeCall( SqlStdOperatorTable.LESS_THAN, a0, a1 );
    }


    private RexNode lessThanOrEqual( RexNode a0, RexNode a1 ) {
        return rexBuilder.makeCall( SqlStdOperatorTable.LESS_THAN_OR_EQUAL, a0, a1 );
    }


    private RexNode greaterThan( RexNode a0, RexNode a1 ) {
        return rexBuilder.makeCall( SqlStdOperatorTable.GREATER_THAN, a0, a1 );
    }


    private RexNode greaterThanOrEqual( RexNode a0, RexNode a1 ) {
        return rexBuilder.makeCall( SqlStdOperatorTable.GREATER_THAN_OR_EQUAL, a0, a1 );
    }


    private RexNode equals( RexNode a0, RexNode a1 ) {
        return rexBuilder.makeCall( SqlStdOperatorTable.EQUALS, a0, a1 );
    }


    private RexNode notEquals( RexNode a0, RexNode a1 ) {
        return rexBuilder.makeCall( SqlStdOperatorTable.NOT_EQUALS, a0, a1 );
    }


    private RexNode and( RexNode a0, RexNode a1 ) {
        return rexBuilder.makeCall( SqlStdOperatorTable.AND, a0, a1 );
    }


    private RexNode or( RexNode a0, RexNode a1 ) {
        return rexBuilder.makeCall( SqlStdOperatorTable.OR, a0, a1 );
    }


    private RexNode not( RexNode a0 ) {
        return rexBuilder.makeCall( SqlStdOperatorTable.NOT, a0 );
    }


    private RexNode plus( RexNode a0, RexNode a1 ) {
        return rexBuilder.makeCall( SqlStdOperatorTable.PLUS, a0, a1 );
    }


    private RexNode isNotNull( RexNode a0 ) {
        return rexBuilder.makeCall( SqlStdOperatorTable.IS_NOT_NULL, a0 );
    }


    private RexNode isFalse( RexNode node ) {
        return rexBuilder.makeCall( SqlStdOperatorTable.IS_FALSE, node );
    }


    private RexNode isTrue( RexNode node ) {
        return rexBuilder.makeCall( SqlStdOperatorTable.IS_TRUE, node );
    }


    @Test
    public void testPreTests() {
        // can make variable nullable?
        RexNode node = new RexInputRef( 0, typeFactory.createTypeWithNullability( typeFactory.createSqlType( SqlTypeName.BOOLEAN ), true ) );
        assertTrue( node.getType().isNullable() );

        // can make variable not nullable?
        node = new RexInputRef( 0, typeFactory.createTypeWithNullability( typeFactory.createSqlType( SqlTypeName.BOOLEAN ), false ) );
        assertFalse( node.getType().isNullable() );
    }


    @Test
    public void testNonBooleans() {
        RexNode node = plus( x, y );
        String expected = node.toString();
        check( Boolean.TRUE, node, expected );
        check( Boolean.FALSE, node, expected );
        check( null, node, expected );
    }


    /**
     * the or operator should pass through unchanged since e.g. x OR y should return true if x=null and y=true if it was transformed into something like (x IS NOT NULL) AND (y IS NOT NULL) AND (x OR y) an incorrect result could be produced
     */
    @Test
    public void testOrUnchanged() {
        RexNode node = or( x, y );
        String expected = node.toString();
        check( Boolean.TRUE, node, expected );
        check( Boolean.FALSE, node, expected );
        check( null, node, expected );
    }


    @Test
    public void testSimpleAnd() {
        RexNode node = and( x, y );
        check( Boolean.FALSE, node, "AND(AND(IS NOT NULL($0), IS NOT NULL($1)), AND($0, $1))" );
    }


    @Test
    public void testSimpleEquals() {
        RexNode node = equals( x, y );
        check( Boolean.TRUE, node, "AND(AND(IS NOT NULL($0), IS NOT NULL($1)), =($0, $1))" );
    }


    @Test
    public void testSimpleNotEquals() {
        RexNode node = notEquals( x, y );
        check( Boolean.FALSE, node, "AND(AND(IS NOT NULL($0), IS NOT NULL($1)), <>($0, $1))" );
    }


    @Test
    public void testSimpleGreaterThan() {
        RexNode node = greaterThan( x, y );
        check( Boolean.TRUE, node, "AND(AND(IS NOT NULL($0), IS NOT NULL($1)), >($0, $1))" );
    }


    @Test
    public void testSimpleGreaterEquals() {
        RexNode node = greaterThanOrEqual( x, y );
        check( Boolean.FALSE, node, "AND(AND(IS NOT NULL($0), IS NOT NULL($1)), >=($0, $1))" );
    }


    @Test
    public void testSimpleLessThan() {
        RexNode node = lessThan( x, y );
        check( Boolean.TRUE, node, "AND(AND(IS NOT NULL($0), IS NOT NULL($1)), <($0, $1))" );
    }


    @Test
    public void testSimpleLessEqual() {
        RexNode node = lessThanOrEqual( x, y );
        check( Boolean.FALSE, node, "AND(AND(IS NOT NULL($0), IS NOT NULL($1)), <=($0, $1))" );
    }


    @Test
    public void testOptimizeNonNullLiterals() {
        RexNode node = lessThanOrEqual( x, trueRex );
        check( Boolean.TRUE, node, "AND(IS NOT NULL($0), <=($0, true))" );
        node = lessThanOrEqual( trueRex, x );
        check( Boolean.FALSE, node, "AND(IS NOT NULL($0), <=(true, $0))" );
    }


    @Test
    public void testSimpleIdentifier() {
        RexNode node = rexBuilder.makeInputRef( boolRelDataType, 0 );
        check( Boolean.TRUE, node, "=(IS TRUE($0), true)" );
    }


    @Test
    public void testMixed1() {
        // x=true AND y
        RexNode op1 = equals( x, trueRex );
        RexNode and = and( op1, y );
        check( Boolean.FALSE, and, "AND(IS NOT NULL($1), AND(AND(IS NOT NULL($0), =($0, true)), $1))" );
    }


    @Test
    public void testMixed2() {
        // x!=true AND y>z
        RexNode op1 = notEquals( x, trueRex );
        RexNode op2 = greaterThan( y, z );
        RexNode and = and( op1, op2 );
        check( Boolean.FALSE, and, "AND(AND(IS NOT NULL($0), <>($0, true)), AND(AND(IS NOT NULL($1), IS NOT NULL($2)), >($1, $2)))" );
    }


    @Test
    public void testMixed3() {
        // x=y AND false>z
        RexNode op1 = equals( x, y );
        RexNode op2 = greaterThan( falseRex, z );
        RexNode and = and( op1, op2 );
        check( Boolean.TRUE, and, "AND(AND(AND(IS NOT NULL($0), IS NOT NULL($1)), =($0, $1)), AND(IS NOT NULL($2), >(false, $2)))" );
    }


    /**
     * Test case for "RexBuilder reverses precision and scale of DECIMAL literal" and "Incorrect inferred precision when BigDecimal value is less than 1".
     */
    @Test
    public void testExactLiteral() {
        final RexLiteral literal = rexBuilder.makeExactLiteral( new BigDecimal( "-1234.56" ) );
        assertThat( literal.getType().getFullTypeString(), is( "DECIMAL(6, 2) NOT NULL" ) );
        assertThat( literal.getValue().toString(), is( "-1234.56" ) );

        final RexLiteral literal2 = rexBuilder.makeExactLiteral( new BigDecimal( "1234.56" ) );
        assertThat( literal2.getType().getFullTypeString(), is( "DECIMAL(6, 2) NOT NULL" ) );
        assertThat( literal2.getValue().toString(), is( "1234.56" ) );

        final RexLiteral literal3 = rexBuilder.makeExactLiteral( new BigDecimal( "0.0123456" ) );
        assertThat( literal3.getType().getFullTypeString(), is( "DECIMAL(8, 7) NOT NULL" ) );
        assertThat( literal3.getValue().toString(), is( "0.0123456" ) );

        final RexLiteral literal4 = rexBuilder.makeExactLiteral( new BigDecimal( "0.01234560" ) );
        assertThat( literal4.getType().getFullTypeString(), is( "DECIMAL(9, 8) NOT NULL" ) );
        assertThat( literal4.getValue().toString(), is( "0.01234560" ) );
    }


    /**
     * Test case for "RelOptUtil.splitJoinCondition attempts to split a Join-Condition which has a remaining condition".
     */
    @Test
    public void testSplitJoinCondition() {
        final String sql = "select * \n"
                + "from emp a \n"
                + "INNER JOIN dept b \n"
                + "ON CAST(a.empno AS int) <> b.deptno";

        final RelNode relNode = toRel( sql );
        final LogicalProject project = (LogicalProject) relNode;
        final LogicalJoin join = (LogicalJoin) project.getInput( 0 );
        final List<RexNode> leftJoinKeys = new ArrayList<>();
        final List<RexNode> rightJoinKeys = new ArrayList<>();
        final ArrayList<RelDataTypeField> sysFieldList = new ArrayList<>();
        final RexNode remaining = RelOptUtil.splitJoinCondition(
                sysFieldList,
                join.getInputs().get( 0 ),
                join.getInputs().get( 1 ),
                join.getCondition(),
                leftJoinKeys,
                rightJoinKeys,
                null,
                null );

        assertThat( remaining.toString(), is( "<>(CAST($0):INTEGER NOT NULL, $9)" ) );
        assertThat( leftJoinKeys.isEmpty(), is( true ) );
        assertThat( rightJoinKeys.isEmpty(), is( true ) );
    }


    /**
     * Test case for {@link LogicVisitor}.
     */
    @Test
    public void testLogic() {
        // x > FALSE AND ((y = z) IS NOT NULL)
        final RexNode node = and( greaterThan( x, falseRex ), isNotNull( equals( y, z ) ) );
        assertThat( deduceLogic( node, x, Logic.TRUE_FALSE ), is( Logic.TRUE_FALSE ) );
        assertThat( deduceLogic( node, y, Logic.TRUE_FALSE ), is( Logic.TRUE_FALSE_UNKNOWN ) );
        assertThat( deduceLogic( node, z, Logic.TRUE_FALSE ), is( Logic.TRUE_FALSE_UNKNOWN ) );

        // TRUE means that a value of FALSE or UNKNOWN will kill the row
        // (therefore we can safely use a semijoin)
        assertThat( deduceLogic( and( x, y ), x, Logic.TRUE ), is( Logic.TRUE ) );
        assertThat( deduceLogic( and( x, y ), y, Logic.TRUE ), is( Logic.TRUE ) );
        assertThat( deduceLogic( and( x, and( y, z ) ), z, Logic.TRUE ), is( Logic.TRUE ) );
        assertThat( deduceLogic( and( x, not( y ) ), x, Logic.TRUE ), is( Logic.TRUE ) );
        assertThat( deduceLogic( and( x, not( y ) ), y, Logic.TRUE ), is( Logic.UNKNOWN_AS_TRUE ) );
        assertThat( deduceLogic( and( x, not( not( y ) ) ), y, Logic.TRUE ), is( Logic.TRUE_FALSE_UNKNOWN ) ); // TRUE_FALSE would be better
        assertThat( deduceLogic( and( x, not( and( y, z ) ) ), z, Logic.TRUE ), is( Logic.UNKNOWN_AS_TRUE ) );
        assertThat( deduceLogic( or( x, y ), x, Logic.TRUE ), is( Logic.TRUE_FALSE_UNKNOWN ) );
    }


    private Logic deduceLogic( RexNode root, RexNode seek, Logic logic ) {
        final List<Logic> list = new ArrayList<>();
        LogicVisitor.collect( root, seek, logic, list );
        assertThat( list.size(), is( 1 ) );
        return list.get( 0 );
    }
}
