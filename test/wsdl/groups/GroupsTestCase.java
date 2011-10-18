/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This tests the file generation of only the items that are referenced in WSDL
 * 
 * @author Steve Green (steve.green@epok.net)
 */ 
package test.wsdl.groups;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class GroupsTestCase extends junit.framework.TestCase {
    public GroupsTestCase(String name) {
        super(name);
    }

    /**
     * List of files which should be generated.
     */
    protected Set shouldExist() {
        HashSet set = new HashSet();
        set.add("GroupsTestCase.java");
        set.add("SomeType.java");
        return set;
    }
    
    /**
     * List of files which may or may not be generated.
     */
    protected Set shouldNotExist() {
        HashSet set = new HashSet();
        set.add("SomeGroup.java");
        return set;
    }

    /**
     * The directory containing the files that should exist.
     */
    protected String rootDir() {
        return "build" + File.separator + "work" + File.separator + 
                "test" + File.separator + "wsdl" + File.separator +
                "groups";
    }
    
    protected String getPrefix(String parent) {
        if (parent == null || parent.length() == 0) {
            return "";
        }
        else {
            return parent + File.separator;
        }
    }

    /** This method returns a array of String file paths, located within the
     * supplied root directory. The string values are created relative to the 
     * specified parent so that the names get returned in the form of 
     * "file.java", "dir/file.java", "dir/dir/file.java", etc. This feature 
     * asslows the various file specs to include files in sub-directories as
     * well as the root directory.
     */    
    protected String[] getPaths(File root, String parent) {
        File files[] = root.listFiles();
        if (files == null)
            fail("Unable to get a list of files from " + root.getPath());

        Set filePaths = new HashSet();
        for(int i=0; i<files.length; i++) {
            if (files[i].isDirectory()) {
                String children[] = getPaths(files[i],
                            getPrefix(parent) + files[i].getName());
                filePaths.addAll(Arrays.asList(children));
            }
            else {
                filePaths.add(getPrefix(parent) + files[i].getName());
            }
        }
        String paths[] = new String[filePaths.size()];
        return (String[]) filePaths.toArray(paths);
    }

    
    public void testGroups() throws IOException, ClassNotFoundException, SecurityException, NoSuchMethodException {

        // Test for the proper files

        String rootDir = rootDir();
        Set shouldExist = shouldExist();
        Set shouldNotExist = shouldNotExist();

        // open up the output directory and check what files exist.
        File outputDir = new File(rootDir);
        
        String[] files = getPaths(outputDir, null);

        for (int i = 0; i < files.length; ++i) {
            if (shouldExist.contains(files[i])) {
                shouldExist.remove(files[i]);
            } 
            else if (shouldNotExist.contains(files[i])) {
                fail("The following file should not exist in " + rootDir +
			", but does:  " + files[i]);
            }
        }

        if (shouldExist.size() > 0) {
            fail("The following files should exist in " + rootDir + 
                ", but do not:  " + shouldExist);
        }

    	// Test for the proper members
    
        Class ourClass = Class.forName("test.wsdl.groups.SomeType");
        ourClass.getDeclaredMethod("getA", null);
        ourClass.getDeclaredMethod("getB", null);
        ourClass.getDeclaredMethod("getZ", null);

        return;
    }
}

