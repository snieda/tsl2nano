package de.tsl2.nano.d8portal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;

public class RepositoryPortalTest implements ENVTestPreparation {
    private static String orgaName = "testOrga";
    private static String idClient = "testClient1";

	@BeforeClass
	public static void setUp() {
		ENVTestPreparation.setUp("d8portal", false);
        FileUtil.deleteRecursive(FileUtil.userDirFile(orgaName));
        FileUtil.deleteRecursive(FileUtil.userDirFile(idClient));
	}

	@AfterClass
	public static void tearDown() {
		ENVTestPreparation.tearDown();
	}

    @Test
    public void testPortal() {
        RepositoryPortal portal = new RepositoryPortal();

        portal.setPublishOptions("--dry-run");
        portal.setMailEnabled(false);

        portal.createOrganisation(orgaName, "info@gar.nix", "my.git.nix", "smtp.mail.gar.nix");

        String passwd = "mypassword";
        portal.createRepository(idClient, idClient, "client@gar.nix", passwd);

        String content = "tolle neuigkeiten...";
        String newFile = "newdoc.txt";
        FileUtil.writeBytes(content.getBytes(), newFile, false);
        String newGitFile = portal.upload(idClient, newFile, newFile, "news", null, null);
        assertTrue(newGitFile, newGitFile.contains("news-" + newFile));

        portal.find(idClient, "news");
        // String privateKey = null;
        // portal.synchronize(idClient);
        // byte[]  downloaded = portal.download(idClient, privateKey, newGitFile);
        InputStream decrypted = portal.decrypt(newGitFile, idClient, passwd);

        assertEquals(content, StringUtil.fromInputStream(decrypted));
    }
}
