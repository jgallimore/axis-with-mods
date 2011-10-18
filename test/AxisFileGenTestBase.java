/*
 * Copyright 2002-2004 The Apache Software Foundation.
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


package test;

import org.custommonkey.xmlunit.XMLTestCase;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Vector;

/**
 *  base class for Axis FileGen test cases.
 */
public abstract class AxisFileGenTestBase extends AxisTestBase {

    public AxisFileGenTestBase(String s) {
        super(s);
    }

    protected String getPrefix(String parent) {
        if (parent == null || parent.length() == 0) {
            return "";
        }
        else {
            return parent + File.separator;
        }
    }

    abstract protected Set mayExist();
    abstract protected String rootDir();
    abstract protected Set shouldExist();

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


    public void testFileGen() throws IOException {
        String rootDir = rootDir();
        Set shouldExist = shouldExist();
        Set mayExist = mayExist();

        // open up the output directory and check what files exist.
        File outputDir = new File(rootDir);

        String[] files = getPaths(outputDir, null);

        Vector shouldNotExist = new Vector();

        for (int i = 0; i < files.length; ++i) {
            if (shouldExist.contains(files[i])) {
                shouldExist.remove(files[i]);
            }
            else if (mayExist.contains(files[i])) {
                mayExist.remove(files[i]);
            }
            else {
                shouldNotExist.add(files[i]);
            }
        }

        if (shouldExist.size() > 0) {
            fail("The following files should exist in " + rootDir +
                ", but do not:  " + shouldExist);
        }

        if (shouldNotExist.size() > 0) {
            fail("The following files should NOT exist in " + rootDir +
                ", but do:  " + shouldNotExist);
        }
    }
}
