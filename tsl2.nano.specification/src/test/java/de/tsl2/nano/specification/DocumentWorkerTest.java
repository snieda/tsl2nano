package de.tsl2.nano.specification;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.specification.DocumentWorker;
import de.tsl2.nano.specification.SpecificationExchange;
import de.tsl2.nano.util.FilePath;

public class DocumentWorkerTest implements ENVTestPreparation {
	boolean specCalled = false;
	@Before
	public void setUp() {
		ENVTestPreparation.super.setUp("specification");
		
		ENV.addService(SpecificationExchange.class, new SpecificationExchange() {
			@Override
			public int enrichFromSpecificationProperties() {
				System.out.println("Test " + SpecificationExchange.class.getSimpleName() + " successfully called");
				specCalled = true;
				return 0;
			}
		});
	}
	@Test
	public void testDocument() {
		String fileName = "testspecificationdocumentworker.md.html";
		InputStream stream = FileUtil.getResource(fileName);
		String fileData = String.valueOf(FileUtil.getFileData(stream, "utf8"));
		FilePath.write(ENV.getConfigPath() + fileName, fileData.getBytes());
		
		DocumentWorker worker = new DocumentWorker();
		worker.consume(ENV.getConfigPath() + fileName);
		
		assertTrue(specCalled);
	}
	
	public void myAction() {
		
	}

}
