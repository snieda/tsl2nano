/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Sep 16, 2011
 * 
 * Copyright: (c) Thomas Schneider 2011, all rights reserved
 */
package de.tsl2.nano.serviceaccess.test;

import java.io.IOException;
import java.nio.charset.MalformedInputException;

import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.service.util.BaseServiceTest;
import de.tsl2.nano.service.util.IFileService;
//import sun.io.MalformedInputException;

/**
 * Tests for FileService
 * 
 * TODO: solve nonserializable inputstream problem
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public class FileServiceTest extends BaseServiceTest {

    @BeforeClass
    public static void setUp() {
//        createTestData("akten");
    }

    @Test
    public void testStandardMode() throws Exception {
        final String testFile = "fileservice-testfile.txt";
        final String testFileContent = "Dies ist eine Testdatei\nmit drei Zeilen\n";

        doFileTest(testFile, testFileContent);
    }

//    @Test
    public void testAbsoluteMode() throws Exception {
        /*
         * test for absolute path mode! see FsConnectionFactory.MODE_ABSOLUTE_PATH.
         * create and write test file in home dir, read it and delete it.
         * this works only on a test system where test-client and app-server
         * are running on the same machine.
         */
        final String userHome = System.getProperty("user.home");
        final String testFile = userHome + "/fileservice-testfile.txt";
        final String testFileContent = "Dies ist eine Testdatei\nmit drei Zeilen\n";

        doFileTest(testFile, testFileContent);
    }

    /**
     * doFileTest
     * 
     * @param testFile
     * @param testFileContent
     * @param fileService
     * @throws MalformedInputException
     * @throws IOException
     */
    private void doFileTest(String testFile, String testFileContent) throws Exception {
        final IFileService fileService = getService(IFileService.class);
        //first: create it!
        assert !fileService.exists(testFile);
//        InputStream stream = new ByteArrayInputStream(CharToByteUTF8.getDefault().convertAll(data));
        final byte[] data = testFileContent.getBytes();
        fileService.writeFile(testFile, data, true);
        assert fileService.exists(testFile);
        //second: do it again - overwriting
        fileService.writeFile(testFile, data, true);
        //third: read it and compare it
        final byte[] content = fileService.getFileContent(testFile);
        
//        assert sun.io.ByteToCharConverter.getDefault().convertAll(content).equals(testFileContent.toCharArray());
        
//        BufferedReader fileReader = fileService.getFileContent(testFile);
//        String line = null;
//        StringBuilder buf = new StringBuilder();
//        while ((line = fileReader.readLine()) != null) {
//            buf.append(line);
//        }
//        assert buf.toString().equals(testFileContent);

        //finally: delete it and check deletion
        fileService.delete(testFile);
        assert !fileService.exists(testFile);
    }
}
