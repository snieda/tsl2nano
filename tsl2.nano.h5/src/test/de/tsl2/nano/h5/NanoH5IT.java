/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 03.11.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.h5;

import java.io.PipedOutputStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import de.tsl2.nano.core.execution.SystemUtil;
import de.tsl2.nano.core.util.ENVTestPreparation;
import de.tsl2.nano.core.util.Util;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class NanoH5IT extends NanoH5Unit {

    @BeforeClass
    public static void setUp() {
        System.setProperty("app.server.running", "true");
        NanoH5Unit.setUp();
    }
    
    @AfterClass
    public static void tearDown() {
//        NanoH5Unit.tearDown();
    }

    @Test
    public void testNano() throws Exception {
        Process process = null;
        HtmlPage page = null;
        PipedOutputStream myOut = SystemUtil.setPipedInput();
        try {
            System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "info");
            page = runWebClient();
            page = submit(page, BTN_LOGIN_OK);
            page = submit(page, BEANCOLLECTORLIST + BTN_SELECTALL);
            page = submit(page, BEANCOLLECTORLIST + BTN_OPEN);

            //-> all collectors now open - closing them in the testObjectCreation()
            int beanTypeCount = 1;//TODO: test it for all bean-types!
            for (int i = 0; i < beanTypeCount; i++) {
                //create and delete objects of all sample types
//                HtmlCheckBoxInput checkbox = page.getElementByName(String.valueOf(i));
//                page = submit(page, "beancollectorliste.open");
                page = crudBean(page);
            }

        } finally {
            if (page != null) {
                int i = 0;
                String id;
                //TODO: does not work yet!!!
                while (i ++ < 30) {
                    id = page.getBody().getId();
                    if (!Util.isEmpty(id) && id.toLowerCase().equals(BEANCOLLECTORLIST)) {
                        page = submit(page, listBtn(BTN_ADMINISTRATION));
                        page = submit(page, BEANCOLLECTORLIST + BTN_SHUTDOWN);
                        break;
                    }
                    page = back(page);
                }
            }
            if (process != null) {
                System.out.println("trying to shutdown nanoh5 server through ENTER...");
                if (myOut != null)
                    myOut.write("\n\n".getBytes());
//                process.destroy();
            }
        }
    }

    private String listBtn(String btnName) {
        return BEANCOLLECTORLIST + btnName;
    }

}
