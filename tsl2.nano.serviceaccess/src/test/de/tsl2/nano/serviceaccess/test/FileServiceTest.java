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

import javax.security.auth.Subject;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.tsl2.nano.core.util.MapUtil;
import de.tsl2.nano.resource.fs.FsConnectionFactory;
import de.tsl2.nano.resource.fs.impl.FsConnectionRequestInfo;
import de.tsl2.nano.resource.fs.impl.FsManagedConnection;
import de.tsl2.nano.service.util.BaseServiceTest;
import de.tsl2.nano.service.util.FileServiceBean;
import de.tsl2.nano.service.util.IFileService;
//import sun.io.MalformedInputException;
import de.tsl2.nano.serviceaccess.ServiceFactory;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit4.JMockit;

/**
 * Tests for FileService
 * 
 * TODO: solve nonserializable inputstream problem
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
@RunWith(JMockit.class)
public class FileServiceTest extends BaseServiceTest {
	@Tested FileServiceBean fileServiceBean;
	@Injectable FsConnectionFactory fsConnectionFactory;
	
	@Injectable Subject subject;
	
	@Tested(fullyInitialized = true) 
	@Injectable FsConnectionRequestInfo info;
	
	@Tested(fullyInitialized = true) 
	@Injectable FsManagedConnection fsManagedConnection;
	
    @BeforeClass
    public static void setUp() {
        System.setProperty(ServiceFactory.NO_JNDI, Boolean.toString(true));
    }

	private void createMockUps() {
		new MockUp<FsManagedConnection>() {
        	@Mock public void $init(@Mocked Subject subject, @Tested FsConnectionRequestInfo info) {
        		info.setRootDirPath("./");
        	}
        	@Mock
        	public boolean isUseAbsoluteFilePath(String rootDirPath) {
        		return false;
        	}
        	@Mock
        	public boolean isUseAbsoluteFilePath() {
        		return false;
        	}
		};
	}

    @Ignore
    @Test
    public void testStandardMode() throws Exception {
    	info.setRootDirPath("./");
		new Expectations() {{
			info.getRootDirPath(); result = "./";
			fsManagedConnection.isUseAbsoluteFilePath(); result = false;
		}};
        createMockUps();
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
        ServiceFactory.createInstance(getClass().getClassLoader());
        ServiceFactory.instance().setInitialServices(MapUtil.asMap(IFileService.class.getName(), fileServiceBean));
        final IFileService fileService = getService(IFileService.class);
        //first: create it!
        assert !fileService.exists(testFile);
//        InputStream stream = new ByteArrayInputStream(CharToByteUTF8.getDefault().convertAll(data));
        final byte[] data = testFileContent.getBytes();
        fileService.writeFile(testFile, data, true);
        assert fileService.exists(testFile);
        //second: do it again - overwriting
        fileService.writeFile(testFile, data, true);
        fileService.getFileContent(testFile);
        
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
