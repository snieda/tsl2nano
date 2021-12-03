package de.tsl2.nano.core.secure;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import de.tsl2.nano.core.util.FileUtil;

public class HashTest {

	@Test
	public void testHashCheck() throws IllegalAccessException {
		try {
			Hash.checkHash(new File("xyz.xyz"), "workflow");
			fail("file should not exist");
		} catch (IllegalStateException e) {
			//ok
		}
		File file = FileUtil.userDirFile("feature-workflow.hash");
		FileUtil.writeBytes("irgendwas".getBytes(), file.getAbsolutePath(), false);
		try {
			Hash.checkHash(file, "workflow");
			fail("should be wrong hash");
		} catch (IllegalAccessException e) {
			//ok
		}
		
		String hash = "cLea6gPYpB+VhWuRMyWixj4FA7BWuFpiM1N9VhA7BJaFyBMm9bY6rY9jhvDiLKyYb30y1hM2RDHQ4ol6l7YutA==";
		FileUtil.writeBytes(hash.getBytes(), file.getAbsolutePath(), false);
		try {
			Hash.checkHash(file, "workflow");
		} catch (IllegalAccessException e) {
			// BEI JEDER AENDERUNG an dieser Testklasse oder irgendetwas im callstack muss der code neu ermittelt werden!		
		}
	}
}
