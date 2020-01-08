/*
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
 *
 */

package ch.unibas.dmi.dbis.polyphenydb.information;


import java.util.UUID;


/**
 * An information object containing a query plan. This class is mainly used for the debugger in the UI.
 */
public class InformationQueryPlan extends Information {

    private String queryPlan;


    /**
     * Constructor
     *
     * @param group The InformationGroup to which this information belongs
     */
    public InformationQueryPlan( final InformationGroup group, final String queryPlan ) {
        this( group.getId(), queryPlan );
    }


    /**
     * Constructor
     *
     * @param groupId The id of the InformationGroup to which this information belongs
     */
    public InformationQueryPlan( final String groupId, final String queryPlan ) {
        this( UUID.randomUUID().toString(), groupId, queryPlan );
    }


    /**
     * Constructor
     *
     * @param id Unique id for this information object
     * @param group The id of the InformationGroup to which this information belongs
     */
    public InformationQueryPlan( final String id, final String group, final String queryPlan ) {
        super( id, group );
        this.queryPlan = queryPlan;
    }


    public void updateQueryPlan( final String queryPlan ) {
        this.queryPlan = queryPlan;
        notifyManager();
    }

}