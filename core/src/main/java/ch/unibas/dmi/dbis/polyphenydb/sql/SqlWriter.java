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


import ch.unibas.dmi.dbis.polyphenydb.sql.util.SqlString;


/**
 * A <code>SqlWriter</code> is the target to construct a SQL statement from a parse tree. It deals with dialect differences; for example, Oracle quotes identifiers as <code>"scott"</code>,
 * while SQL Server quotes them as <code>[scott]</code>.
 */
public interface SqlWriter {


    /**
     * Style of formatting sub-queries.
     */
    enum SubQueryStyle {
        /**
         * Julian's style of sub-query nesting. Like this:
         *
         * <blockquote><pre>
         * SELECT *
         * FROM (
         *     SELECT *
         *     FROM t
         * )
         * WHERE condition
         * </pre></blockquote>
         */
        HYDE,

        /**
         * Damian's style of sub-query nesting. Like this:
         *
         * <blockquote><pre>
         * SELECT *
         * FROM
         * (   SELECT *
         *     FROM t
         * )
         * WHERE condition
         * </pre></blockquote>
         */
        BLACK
    }


    /**
     * Enumerates the types of frame.
     */
    enum FrameTypeEnum implements FrameType {
        /**
         * SELECT query (or UPDATE or DELETE). The items in the list are the clauses: FROM, WHERE, etc.
         */
        SELECT,

        /**
         * Simple list.
         */
        SIMPLE,

        /**
         * The SELECT clause of a SELECT statement.
         */
        SELECT_LIST,

        /**
         * The WINDOW clause of a SELECT statement.
         */
        WINDOW_DECL_LIST,

        /**
         * The SET clause of an UPDATE statement.
         */
        UPDATE_SET_LIST,

        /**
         * Function declaration.
         */
        FUN_DECL,

        /**
         * Function call or datatype declaration.
         *
         * Examples:
         * <ul>
         * <li><code>SUBSTRING('foobar' FROM 1 + 2 TO 4)</code></li>
         * <li><code>DECIMAL(10, 5)</code></li>
         * </ul>
         */
        FUN_CALL,

        /**
         * Window specification.
         *
         * Examples:
         * <ul>
         * <li><code>SUM(x) OVER (ORDER BY hireDate ROWS 3 PRECEDING)</code></li>
         * <li><code>WINDOW w1 AS (ORDER BY hireDate), w2 AS (w1 PARTITION BY gender RANGE BETWEEN INTERVAL '1' YEAR PRECEDING AND '2' MONTH PRECEDING)</code></li>
         * </ul>
         */
        WINDOW,

        /**
         * ORDER BY clause of a SELECT statement. The "list" has only two items: the query and the order by clause, with ORDER BY as the separator.
         */
        ORDER_BY,

        /**
         * ORDER BY list.
         *
         * Example:
         * <ul>
         * <li><code>ORDER BY x, y DESC, z</code></li>
         * </ul>
         */
        ORDER_BY_LIST,

        /**
         * WITH clause of a SELECT statement. The "list" has only two items: the WITH clause and the query, with AS as the separator.
         */
        WITH,

        /**
         * OFFSET clause.
         *
         * Example:
         * <ul>
         * <li><code>OFFSET 10 ROWS</code></li>
         * </ul>
         */
        OFFSET,

        /**
         * FETCH clause.
         *
         * Example:
         * <ul>
         * <li><code>FETCH FIRST 3 ROWS ONLY</code></li>
         * </ul>
         */
        FETCH,

        /**
         * GROUP BY list.
         *
         * Example:
         * <ul>
         * <li><code>GROUP BY x, FLOOR(y)</code></li>
         * </ul>
         */
        GROUP_BY_LIST,

        /**
         * Sub-query list. Encloses a SELECT, UNION, EXCEPT, INTERSECT query with optional ORDER BY.
         *
         * Example:
         * <ul>
         * <li><code>GROUP BY x, FLOOR(y)</code></li>
         * </ul>
         */
        SUB_QUERY,

        /**
         * Set operation.
         *
         * Example:
         * <ul>
         * <li><code>SELECT * FROM a UNION SELECT * FROM b</code></li>
         * </ul>
         */
        SETOP,

        /**
         * VALUES clause.
         *
         * Example:
         *
         * <blockquote><pre>VALUES (1, 'a'), (2, 'b')</pre></blockquote>
         */
        VALUES,

        /**
         * FROM clause (containing various kinds of JOIN).
         */
        FROM_LIST,

        /**
         * Pair-wise join.
         */
        JOIN( false ),

        /**
         * WHERE clause.
         */
        WHERE_LIST,

        /**
         * Compound identifier.
         *
         * Example:
         * <ul>
         * <li><code>"A"."B"."C"</code></li>
         * </ul>
         */
        IDENTIFIER( false );

        private final boolean needsIndent;


        /**
         * Creates a list type.
         */
        FrameTypeEnum() {
            this( true );
        }


        /**
         * Creates a list type.
         */
        FrameTypeEnum( boolean needsIndent ) {
            this.needsIndent = needsIndent;
        }


        @Override
        public boolean needsIndent() {
            return needsIndent;
        }


        /**
         * Creates a frame type.
         *
         * @param name Name
         * @return frame type
         */
        public static FrameType create( final String name ) {
            return new FrameType() {
                @Override
                public String getName() {
                    return name;
                }


                @Override
                public boolean needsIndent() {
                    return true;
                }
            };
        }


        @Override
        public String getName() {
            return name();
        }
    }


    /**
     * Resets this writer so that it can format another expression. Does not affect formatting preferences (see {@link #resetSettings()}
     */
    void reset();

    /**
     * Resets all properties to their default values.
     */
    void resetSettings();

    /**
     * Returns the dialect of SQL.
     *
     * @return SQL dialect
     */
    SqlDialect getDialect();

    /**
     * Returns the contents of this writer as a 'certified kocher' SQL string.
     *
     * @return SQL string
     */
    SqlString toSqlString();

    /**
     * Prints a literal, exactly as provided. Does not attempt to indent or convert to upper or lower case. Does not add quotation marks. Adds preceding whitespace if necessary.
     */
    void literal( String s );

    /**
     * Prints a sequence of keywords. Must not start or end with space, but may contain a space. For example, <code>keyword("SELECT")</code>, <code>keyword("CHARACTER SET")</code>.
     */
    void keyword( String s );

    /**
     * Prints a string, preceded by whitespace if necessary.
     */
    void print( String s );

    /**
     * Prints an integer.
     *
     * @param x Integer
     */
    void print( int x );

    /**
     * Prints an identifier, quoting as necessary.
     */
    void identifier( String name );

    /**
     * Prints a dynamic parameter (e.g. {@code ?} for default JDBC)
     */
    void dynamicParam( int index );

    /**
     * Prints the OFFSET/FETCH clause.
     */
    void fetchOffset( SqlNode fetch, SqlNode offset );

    /**
     * Prints a new line, and indents.
     */
    void newlineAndIndent();

    /**
     * Returns whether this writer should quote all identifiers, even those that do not contain mixed-case identifiers or punctuation.
     *
     * @return whether to quote all identifiers
     */
    boolean isQuoteAllIdentifiers();

    /**
     * Returns whether this writer should start each clause (e.g. GROUP BY) on a new line.
     *
     * @return whether to start each clause on a new line
     */
    boolean isClauseStartsLine();

    /**
     * Returns whether the items in the SELECT clause should each be on a separate line.
     *
     * @return whether to put each SELECT clause item on a new line
     */
    boolean isSelectListItemsOnSeparateLines();

    /**
     * Returns whether to output all keywords (e.g. SELECT, GROUP BY) in lower case.
     *
     * @return whether to output SQL keywords in lower case
     */
    boolean isKeywordsLowerCase();

    /**
     * Starts a list which is a call to a function.
     *
     * @see #endFunCall(Frame)
     */
    Frame startFunCall( String funName );

    /**
     * Ends a list which is a call to a function.
     *
     * @param frame Frame
     * @see #startFunCall(String)
     */
    void endFunCall( Frame frame );

    /**
     * Starts a list.
     */
    Frame startList( String open, String close );

    /**
     * Starts a list with no opening string.
     *
     * @param frameType Type of list. For example, a SELECT list will be
     */
    Frame startList( FrameTypeEnum frameType );

    /**
     * Starts a list.
     *
     * @param frameType Type of list. For example, a SELECT list will be governed according to SELECT-list formatting preferences.
     * @param open String to start the list; typically "(" or the empty string.
     * @param close String to close the list
     */
    Frame startList( FrameType frameType, String open, String close );

    /**
     * Ends a list.
     *
     * @param frame The frame which was created by {@link #startList}.
     */
    void endList( Frame frame );

    /**
     * Writes a list separator, unless the separator is "," and this is the first occurrence in the list.
     *
     * @param sep List separator, typically ",".
     */
    void sep( String sep );

    /**
     * Writes a list separator.
     *
     * @param sep List separator, typically ","
     * @param printFirst Whether to print the first occurrence of the separator
     */
    void sep( String sep, boolean printFirst );

    /**
     * Sets whether whitespace is needed before the next token.
     */
    void setNeedWhitespace( boolean needWhitespace );

    /**
     * Returns the offset for each level of indentation. Default 4.
     */
    int getIndentation();

    /**
     * Returns whether to enclose all expressions in parentheses, even if the operator has high enough precedence that the parentheses are not required.
     *
     * For example, the parentheses are required in the expression <code>(a + b) * c</code> because the '*' operator has higher precedence than the '+' operator, and so without the parentheses, the expression would be equivalent
     * to <code>a + (b * c)</code>. The fully-parenthesized expression, <code>((a + b) * c)</code> is unambiguous even if you don't know the precedence of every operator.
     */
    boolean isAlwaysUseParentheses();

    /**
     * Returns whether we are currently in a query context (SELECT, INSERT, UNION, INTERSECT, EXCEPT, and the ORDER BY operator).
     */
    boolean inQuery();


    /**
     * A Frame is a piece of generated text which shares a common indentation level.
     *
     * Every frame has a beginning, a series of clauses and separators, and an end. A typical frame is a comma-separated list. It begins with a "(", consists of expressions separated by ",", and ends with a ")".
     *
     * A select statement is also a kind of frame. The beginning and end are are empty strings, but it consists of a sequence of clauses. "SELECT", "FROM", "WHERE" are separators.
     *
     * A frame is current between a call to one of the {@link SqlWriter#startList} methods and the call to {@link SqlWriter#endList(Frame)}. If other code starts a frame in the mean time, the sub-frame is put onto a stack.
     */
    interface Frame {

    }


    /**
     * Frame type.
     */
    interface FrameType {

        /**
         * Returns the name of this frame type.
         *
         * @return name
         */
        String getName();

        /**
         * Returns whether this frame type should cause the code be further indented.
         *
         * @return whether to further indent code within a frame of this type
         */
        boolean needsIndent();
    }
}
