/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.xdbm.search.impl;


import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;


/**
 * A Cursor over entries satisfying one level scope constraints with alias
 * dereferencing considerations when enabled during search.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChildrenCursor<ID extends Comparable<ID>> extends AbstractIndexCursor<ID, Entry, ID>
{
    /** Error message for unsupported operations */
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_719 );

    /** A Cursor over the entries in the scope of the search base */
    private final IndexCursor<ParentIdAndRdn<ID>,Entry, ID> cursor;

    /** The entry database/store */
    private final Store<Entry, ID> db;
    
    /** The current position in the cursor */
    private int currentChild;
    
    /** The max number of childen */
    private int maxChildren;

    /**
     * Creates a Cursor over entries satisfying one level scope criteria.
     *
     * @param db the entry store
     * @param evaluator an IndexEntry (candidate) evaluator
     * @throws Exception on db access failures
     */
    //@SuppressWarnings("unchecked")
    public ChildrenCursor( Store<Entry, ID> db, ID id, IndexCursor<ParentIdAndRdn<ID>,Entry, ID> cursor )
        throws Exception
    {
        this.db = db;
        ParentIdAndRdn<ID> parent = db.getRdnIndex().reverseLookup( id );
        currentChild = 0;
        
        if ( parent == null )
        {
            maxChildren = 0;
        }
        else
        {
            maxChildren = parent.getNbChildren();
        }
        
        this.cursor = cursor;
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }


    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );
        setAvailable( false );
    }


    public void afterLast() throws Exception
    {
        throw new UnsupportedOperationException( getUnsupportedMessage() );
    }


    public boolean first() throws Exception
    {
        beforeFirst();
        currentChild = -1;
        
        return next();
    }


    public boolean last() throws Exception
    {
        throw new UnsupportedOperationException( getUnsupportedMessage() );
    }


    public boolean previous() throws Exception
    {
        throw new UnsupportedOperationException( getUnsupportedMessage() );
    }


    public boolean next() throws Exception
    {
        checkNotClosed( "next()" );
        
        if ( currentChild < maxChildren )
        {
            currentChild++;

            return cursor.next();
        }
        else
        {
            return false;
        }
    }

    public IndexEntry<ID, ID> get() throws Exception
    {
        checkNotClosed( "get()" );

        IndexEntry entry = cursor.get();
        
        return entry;
    }


    @Override
    public void close() throws Exception
    {
        cursor.close();

        super.close();
    }


    @Override
    public void close( Exception cause ) throws Exception
    {
        cursor.close( cause );

        super.close( cause );
    }
}