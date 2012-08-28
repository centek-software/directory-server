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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.ParentIdAndRdn;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.name.Rdn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests the JdbmRdnIndex.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmRdnIndexIT
{
    private static File dbFileDir;
    Index<ParentIdAndRdn> idx;
    private static SchemaManager schemaManager;


    @BeforeClass
    public static void init() throws Exception
    {
        String workingDirectory = System.getProperty( "workingDirectory" );

        if ( workingDirectory == null )
        {
            String path = JdbmRdnIndexIT.class.getResource( "" ).getPath();
            int targetPos = path.indexOf( "target" );
            workingDirectory = path.substring( 0, targetPos + 6 );
        }

        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );
        LdifSchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors(schemaManager.getErrors()) );
        }
    }


    @Before
    public void setup() throws IOException
    {

        File tmpIndexFile = File.createTempFile( JdbmRdnIndexIT.class.getSimpleName(), "db" );
        tmpIndexFile.deleteOnExit();
        dbFileDir = new File( tmpIndexFile.getParentFile(), JdbmRdnIndexIT.class.getSimpleName() );

        dbFileDir.mkdirs();
    }


    @After
    public void teardown() throws Exception
    {
        destroyIndex();

        if ( ( dbFileDir != null ) && dbFileDir.exists() )
        {
            FileUtils.deleteDirectory( dbFileDir );
        }
    }


    void destroyIndex() throws Exception
    {
        if ( idx != null )
        {
            idx.sync();
            idx.close();

            // created by this test
            File dbFile = new File( idx.getWkDirPath().getPath(), idx.getAttribute().getOid() + ".db" );
            assertTrue( dbFile.delete() );

            // created by TransactionManager, if transactions are not disabled
            File logFile = new File( idx.getWkDirPath().getPath(), idx.getAttribute().getOid() + ".lg" );
            
            if ( logFile.exists() )
            {
                assertTrue( logFile.delete() );
            }
        }

        idx = null;
    }


    void initIndex() throws Exception
    {
        JdbmRdnIndex<Long> index = new JdbmRdnIndex<Long>();
        index.setWkDirPath( dbFileDir.toURI() );
        initIndex( index );
    }


    void initIndex( JdbmRdnIndex<Long> jdbmIdx ) throws Exception
    {
        if ( jdbmIdx == null )
        {
            jdbmIdx = new JdbmRdnIndex<Long>();
        }

        jdbmIdx.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_RDN_AT_OID ) );
        this.idx = jdbmIdx;
    }


    // -----------------------------------------------------------------------
    // Property Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testCacheSize() throws Exception
    {
        // uninitialized index
        JdbmRdnIndex<Object> JdbmRdnIndex = new JdbmRdnIndex<Object>();
        JdbmRdnIndex.setCacheSize( 337 );
        assertEquals( 337, JdbmRdnIndex.getCacheSize() );

        // initialized index
        initIndex();
        try
        {
            idx.setCacheSize( 30 );
            fail( "Should not be able to set cacheSize after initialization." );
        }
        catch ( Exception e )
        {
        }
        
        destroyIndex();
        initIndex();
        
        assertEquals( Index.DEFAULT_INDEX_CACHE_SIZE, idx.getCacheSize() );
    }


    @Test
    public void testWkDirPath() throws Exception
    {
        File wkdir = new File( dbFileDir, "foo" );

        // uninitialized index
        JdbmRdnIndex<Long> jdbmRdnIndex = new JdbmRdnIndex<Long>();
        jdbmRdnIndex.setWkDirPath( wkdir.toURI() );
        assertEquals( "foo", new File( jdbmRdnIndex.getWkDirPath() ).getName() );

        // initialized index
        initIndex();
        
        try
        {
            idx.setWkDirPath( wkdir.toURI() );
            fail( "Should not be able to set wkDirPath after initialization." );
        }
        catch ( Exception e )
        {
        }
        
        assertEquals( dbFileDir.toURI(), idx.getWkDirPath() );

        destroyIndex();
        
        jdbmRdnIndex = new JdbmRdnIndex<Long>();
        wkdir.mkdirs();
        jdbmRdnIndex.setWkDirPath( wkdir.toURI() );
        initIndex( jdbmRdnIndex );
        assertEquals( wkdir.toURI(), idx.getWkDirPath() );
    }


    @Test
    public void testGetAttribute() throws Exception
    {
        // uninitialized index
        JdbmRdnIndex<Object> rdnIndex = new JdbmRdnIndex<Object>();
        assertNull( rdnIndex.getAttribute() );

        initIndex();
        assertEquals( schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_RDN_AT ), idx.getAttribute() );
    }


    // -----------------------------------------------------------------------
    // Count Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testCount() throws Exception
    {
        initIndex();
        assertEquals( 0, idx.count() );

        ParentIdAndRdn key = new ParentIdAndRdn( getUUIDString( 0 ), new Rdn( "cn=key" ) );

        idx.add( key, getUUIDString( 0 ) );
        assertEquals( 1, idx.count() );

        // setting a different parentId should make this key a different key
        key = new ParentIdAndRdn( getUUIDString( 1 ), new Rdn( "cn=key" ) );
        
        idx.add( key, getUUIDString( 1 ) );
        assertEquals( 2, idx.count() );

        //count shouldn't get affected cause of inserting the same key
        idx.add( key, getUUIDString( 2 ) );
        assertEquals( 2, idx.count() );
        
        key = new ParentIdAndRdn( getUUIDString( 2 ), new Rdn( "cn=key" ) );
        idx.add( key, getUUIDString( 3 ) );
        assertEquals( 3, idx.count() );
    }


    @Test
    public void testCountOneArg() throws Exception
    {
        initIndex();
        
        ParentIdAndRdn key = new ParentIdAndRdn( getUUIDString( 0 ), new Rdn( "cn=key" ) );
        
        assertEquals( 0, idx.count( key ) );

        idx.add( key, getUUIDString( 0 ) );
        assertEquals( 1, idx.count( key ) );
    }


    // -----------------------------------------------------------------------
    // Add, Drop and Lookup Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testLookups() throws Exception
    {
        initIndex();
        
        ParentIdAndRdn key = new ParentIdAndRdn( getUUIDString( 0 ), new Rdn( schemaManager, "cn=key" ) );
        
        assertNull( idx.forwardLookup( key ) );

        idx.add( key, getUUIDString( 0 ) );
        assertEquals( getUUIDString( 0 ), idx.forwardLookup( key ) );
        assertEquals( key, idx.reverseLookup( getUUIDString( 0 ) ) );
        
        // check with the different case in UP name, this ensures that the custom
        // key comparator is used
        key = new ParentIdAndRdn( getUUIDString( 0 ), new Rdn( schemaManager, "cn=KEY" ) );
        assertEquals( getUUIDString( 0 ), idx.forwardLookup( key ) );
        assertEquals( key, idx.reverseLookup( getUUIDString( 0 ) ) );
    }


    @Test
    public void testAddDropById() throws Exception
    {
        initIndex();
        
        ParentIdAndRdn key = new ParentIdAndRdn( getUUIDString( 0 ), new Rdn( "cn=key" ) );
        
        assertNull( idx.forwardLookup( key ) );

        // test add/drop without adding any duplicates
        idx.add( key, getUUIDString( 0 ) );
        assertEquals( getUUIDString( 0 ), idx.forwardLookup( key ) );
        
        idx.drop( key, getUUIDString( 0 ) );
        assertNull( idx.forwardLookup( key ) );
        assertNull( idx.reverseLookup( getUUIDString( 0 ) ) );
    }


    // -----------------------------------------------------------------------
    // Miscellaneous Test Methods
    // -----------------------------------------------------------------------

    @Test
    public void testCursors() throws Exception
    {
        initIndex();
        
        ParentIdAndRdn key = new ParentIdAndRdn( getUUIDString( 0 ), new Rdn( "cn=key" ) );
        
        assertEquals( 0, idx.count() );

        idx.add( key, getUUIDString( 0 ) );
        assertEquals( 1, idx.count() );
        
        for( long i=1; i< 5; i++ )
        {
            key = new ParentIdAndRdn( getUUIDString( ( int )i ), new Rdn( "cn=key" + i ) );
            
            idx.add( key, getUUIDString( ( int )i ) );
        }

        assertEquals( 5, idx.count() );
        
        // use forward index's cursor
        Cursor<IndexEntry<ParentIdAndRdn>> cursor = idx.forwardCursor();
        cursor.beforeFirst();

        cursor.next();
        IndexEntry<ParentIdAndRdn> e1 = cursor.get();
        assertEquals( getUUIDString( 0 ), e1.getId() );
        assertEquals( "cn=key", e1.getValue().getRdns()[0].getName() );
        assertEquals( getUUIDString( 0 ), e1.getValue().getParentId() );

        cursor.next();
        IndexEntry<ParentIdAndRdn> e2 = cursor.get();
        assertEquals( getUUIDString( 1 ), e2.getId() );
        assertEquals( "cn=key1", e2.getValue().getRdns()[0].getName() );
        assertEquals( getUUIDString( 1 ), e2.getValue().getParentId() );
        
        cursor.next();
        IndexEntry<ParentIdAndRdn> e3 = cursor.get();
        assertEquals( getUUIDString( 2 ), e3.getId() );
        assertEquals( "cn=key2", e3.getValue().getRdns()[0].getName() );
        assertEquals( getUUIDString( 2 ), e3.getValue().getParentId() );
    }


//    @Test
//    public void testStoreRdnWithTwoATAVs() throws Exception
//    {
//        initIndex();
//        
//        Dn dn = new Dn( "dc=example,dc=com" );
//        dn.normalize( schemaManager.getNormalizerMapping() );
//        
//        Rdn rdn = new Rdn( dn.getName() );
//        rdn._setParentId( 1 );
//        idx.add( rdn, 0l );
//        
//        Rdn rdn2 = idx.reverseLookup( 0l );
//        System.out.println( rdn2 );
//        InternalRdnComparator rdnCom = new InternalRdnComparator( "" );
//        assertEquals( 0, rdnCom.compare( rdn, rdn2 ) );
//    }
    
    public static UUID getUUIDString( int idx )
    {
        /** UUID string */
        UUID baseUUID = UUID.fromString( "00000000-0000-0000-0000-000000000000" );
        
        long low = baseUUID.getLeastSignificantBits();
        long high = baseUUID.getMostSignificantBits();
        low = low + idx;
        
        return new UUID( high, low );
    }
}