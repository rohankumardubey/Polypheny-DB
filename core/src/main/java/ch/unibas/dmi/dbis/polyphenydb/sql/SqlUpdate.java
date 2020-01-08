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

package ch.unibas.dmi.dbis.polyphenydb.sql;


import ch.unibas.dmi.dbis.polyphenydb.sql.parser.SqlParserPos;
import ch.unibas.dmi.dbis.polyphenydb.sql.validate.SqlValidator;
import ch.unibas.dmi.dbis.polyphenydb.sql.validate.SqlValidatorScope;
import ch.unibas.dmi.dbis.polyphenydb.util.ImmutableNullableList;
import ch.unibas.dmi.dbis.polyphenydb.util.Pair;
import java.util.List;


/**
 * A <code>SqlUpdate</code> is a node of a parse tree which represents an UPDATE statement.
 */
public class SqlUpdate extends SqlCall {

    public static final SqlSpecialOperator OPERATOR = new SqlSpecialOperator( "UPDATE", SqlKind.UPDATE );

    SqlNode targetTable;
    SqlNodeList targetColumnList;
    SqlNodeList sourceExpressionList;
    SqlNode condition;
    SqlSelect sourceSelect;
    SqlIdentifier alias;


    public SqlUpdate( SqlParserPos pos, SqlNode targetTable, SqlNodeList targetColumnList, SqlNodeList sourceExpressionList, SqlNode condition, SqlSelect sourceSelect, SqlIdentifier alias ) {
        super( pos );
        this.targetTable = targetTable;
        this.targetColumnList = targetColumnList;
        this.sourceExpressionList = sourceExpressionList;
        this.condition = condition;
        this.sourceSelect = sourceSelect;
        assert sourceExpressionList.size() == targetColumnList.size();
        this.alias = alias;
    }


    @Override
    public SqlKind getKind() {
        return SqlKind.UPDATE;
    }


    @Override
    public SqlOperator getOperator() {
        return OPERATOR;
    }


    @Override
    public List<SqlNode> getOperandList() {
        return ImmutableNullableList.of( targetTable, targetColumnList, sourceExpressionList, condition, alias );
    }


    @Override
    public void setOperand( int i, SqlNode operand ) {
        switch ( i ) {
            case 0:
                assert operand instanceof SqlIdentifier;
                targetTable = operand;
                break;
            case 1:
                targetColumnList = (SqlNodeList) operand;
                break;
            case 2:
                sourceExpressionList = (SqlNodeList) operand;
                break;
            case 3:
                condition = operand;
                break;
            case 4:
                sourceExpressionList = (SqlNodeList) operand;
                break;
            case 5:
                alias = (SqlIdentifier) operand;
                break;
            default:
                throw new AssertionError( i );
        }
    }


    /**
     * @return the identifier for the target table of the update
     */
    public SqlNode getTargetTable() {
        return targetTable;
    }


    /**
     * @return the alias for the target table of the update
     */
    public SqlIdentifier getAlias() {
        return alias;
    }


    public void setAlias( SqlIdentifier alias ) {
        this.alias = alias;
    }


    /**
     * @return the list of target column names
     */
    public SqlNodeList getTargetColumnList() {
        return targetColumnList;
    }


    /**
     * @return the list of source expressions
     */
    public SqlNodeList getSourceExpressionList() {
        return sourceExpressionList;
    }


    /**
     * Gets the filter condition for rows to be updated.
     *
     * @return the condition expression for the data to be updated, or null for all rows in the table
     */
    public SqlNode getCondition() {
        return condition;
    }


    /**
     * Gets the source SELECT expression for the data to be updated. Returns null before the statement has been expanded by {@code SqlValidatorImpl#performUnconditionalRewrites(SqlNode, boolean)}.
     *
     * @return the source SELECT for the data to be updated
     */
    public SqlSelect getSourceSelect() {
        return sourceSelect;
    }


    public void setSourceSelect( SqlSelect sourceSelect ) {
        this.sourceSelect = sourceSelect;
    }


    @Override
    public void unparse( SqlWriter writer, int leftPrec, int rightPrec ) {
        final SqlWriter.Frame frame = writer.startList( SqlWriter.FrameTypeEnum.SELECT, "UPDATE", "" );
        final int opLeft = getOperator().getLeftPrec();
        final int opRight = getOperator().getRightPrec();
        targetTable.unparse( writer, opLeft, opRight );
        if ( alias != null ) {
            writer.keyword( "AS" );
            alias.unparse( writer, opLeft, opRight );
        }
        final SqlWriter.Frame setFrame = writer.startList( SqlWriter.FrameTypeEnum.UPDATE_SET_LIST, "SET", "" );
        for ( Pair<SqlNode, SqlNode> pair : Pair.zip( getTargetColumnList(), getSourceExpressionList() ) ) {
            writer.sep( "," );
            SqlIdentifier id = (SqlIdentifier) pair.left;
            id.unparse( writer, opLeft, opRight );
            writer.keyword( "=" );
            SqlNode sourceExp = pair.right;
            sourceExp.unparse( writer, opLeft, opRight );
        }
        writer.endList( setFrame );
        if ( condition != null ) {
            writer.sep( "WHERE" );
            condition.unparse( writer, opLeft, opRight );
        }
        writer.endList( frame );
    }


    @Override
    public void validate( SqlValidator validator, SqlValidatorScope scope ) {
        validator.validateUpdate( this );
    }
}
