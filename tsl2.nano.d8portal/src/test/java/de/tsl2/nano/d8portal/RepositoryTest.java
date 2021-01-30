package de.tsl2.nano.d8portal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;

public class RepositoryTest implements ENVTestPreparation {
    private static final String REPOSITORY = "testRep";

	@BeforeClass
	public static void setUp() {
		ENVTestPreparation.setUp("d8portal", false);
        FileUtil.deleteRecursive(FileUtil.userDirFile(REPOSITORY));
	}

	@AfterClass
	public static void tearDown() {
		ENVTestPreparation.tearDown();
	}

    @Test
    public void testRepositoryLifecycle() {
        Repository rep = new Repository(REPOSITORY, "http://example.myrep.nix");
        rep.setPublishParameter("--dry-run");

        rep.create();

        String docName = "test-document.txt";
        FileUtil.save(REPOSITORY + "/" + docName, "my test document title");
        rep.addFile(docName);

        //TODO: without a real remote origin, we can't call git diff
        // assertEquals(1, rep.newFiles().size());
        // assertEquals(docName, rep.newFiles().get(0));

        rep.publish();

        assertEquals(1, rep.lsFiles().size());
        assertEquals(docName, rep.lsFiles().get(0));
        assertTrue(FileUtil.userDirFile(REPOSITORY + "/.git/COMMIT_EDITMSG").exists());
        
    }
}
