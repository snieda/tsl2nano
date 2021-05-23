package de.tsl2.nano.bean.def;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.tsl2.nano.autotest.TypeBean;
import de.tsl2.nano.autotest.ValueRandomizer;
import de.tsl2.nano.bean.BeanFileUtil;
import de.tsl2.nano.bean.BeanFileUtil.FileType;
import de.tsl2.nano.core.util.DateUtil;
import de.tsl2.nano.core.util.FileUtil;

@RunWith(Parameterized.class)
public class ParameterizedBeanTest {

	private FileType fileType;
	private Object[] typeBeans;
	private String filename = "testflatfile_";
	
	@Before
	public void setUp() throws Exception {
    	DateUtil.setUTCTimeZone();
	}

	@After
	public void tearDown() throws Exception {
    	FileUtil.delete(filename);
	}

	public ParameterizedBeanTest(FileType filetype, Object[] typeBeans) {
		this.fileType = filetype;
		this.typeBeans = typeBeans;
		filename = filename + filetype;
	}

	@Parameters(name="{0}")
	public static Collection<Object[]> parameters() {
		LinkedList<Object[]> list = new LinkedList<>();
		for (FileType fileType : FileType.values()) {
			list.add(new Object[] { fileType, ValueRandomizer.provideRandomizedObjects(2, TypeBean.class)});
		}
		return list;
	}
	
	@Test
	public void testFlatWriteAndReadBeans() {
    	checkFlatWriteAndReadBeans(fileType, typeBeans);
	}

	private void checkFlatWriteAndReadBeans(FileType fileType, Object...typeBeans) {
		BeanFileUtil.toFile(Arrays.asList(typeBeans), filename, fileType);
    	
    	Collection<TypeBean> myTypeBeans = null;
		try {
			myTypeBeans = BeanFileUtil.fromFile(filename, fileType, TypeBean.class);
		} catch (AssertionError e) {
			if (!fileType.equals(FileType.HTML))
				fail("only HTML should throw an assertion");
			else
				return;
		}
    	assertEquals(typeBeans.length, myTypeBeans.size());
    	
    	//zum manuellen Vergleich, Datei nach dem Auslesen nochmal schreiben
    	Iterator<TypeBean> it = myTypeBeans.iterator();
		BeanFileUtil.toFile(Arrays.asList(it.next(), it.next()), filename + "_", fileType);
    	it = myTypeBeans.iterator();

    	//TODO: equals() through serialization not working yet! 
//    	assertArrayEquals(typeBeans, myTypeBeans.toArray());
//		assertEquals(typeBean1, it.next());
//    	assertEquals(typeBean2, it.next());

    	//to see the comparable difference
//    	assertEquals(typeBean1.toString().replace(',', '\n'), it.next().toString().replace(',', '\n'));
//    	assertEquals(typeBean2.toString().replace(',', '\n'), it.next().toString().replace(',', '\n'));
    	
    	FileUtil.delete(filename + "_");
	}
}
