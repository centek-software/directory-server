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
package org.apache.directory.server.installers.deb;


import java.io.File;

import org.apache.directory.server.installers.Target;


/**
 * A Deb package for the Debian platform.
 * 
 * To create a Deb package we use the dpkg utility that is bundled in the 
 * Debian operating system.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DebTarget extends Target
{
    /** The dpkg utility executable */
    private File dpkgUtility = new File( "/usr/bin/dpkg" );


    /**
     * Creates a new instance of DebTarget.
     */
    public DebTarget()
    {
        setOsName( Target.OS_NAME_LINUX );
        setOsArch( Target.OS_ARCH_X86_64 );
    }


    /**
     * Gets the dpkg utility.
     *
     * @return
     *      the dpkg utility
     */
    public File getDpkgUtility()
    {
        return dpkgUtility;
    }


    /**
     * Sets the dpkg utility.
     *
     * @param dpkgUtility
     *      the dpkg utility
     */
    public void setDpkgUtility( File dpkgUtility )
    {
        this.dpkgUtility = dpkgUtility;
    }
}