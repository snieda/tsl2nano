/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 02.08.2014
 * 
 * Copyright: (c) Thomas Schneider 2014, all rights reserved
 */
package de.tsl2.nano.h5.test;

import java.io.IOException;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.History;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class NanoH5Test {

    @Test
    public void testNano() throws Exception {
        runServer();

        WebClient webClient = new WebClient();
        HtmlPage page = webClient.getPage("http://localhost:8066");
        page = submit(page, "tsl2.nano.login.ok");
        page = submit(page, "beancollector.selectall");
        page = submit(page, "beancollector.open");

        for (int i = 0; i < 7; i++) {
            //create and delete objects of all sample types
            page = testObjectCreation(page);
        }
    }

    private HtmlPage submit(HtmlPage page, String buttonName) throws Exception {
        HtmlForm form = page.getFormByName("page.form");
        return form.getInputByName(buttonName).click();
    }

    private History back(HtmlPage page) throws Exception {
        return page.getWebClient().getWebWindows().get(0).getHistory().back();
    }

    private HtmlPage testObjectCreation(HtmlPage page) throws Exception {
        String beanName = page.getTitleText();
        page = submit(page, beanName + "." + "search");
        page = submit(page, beanName + "." + "forward");
        submit(page, beanName + "." + "print");
        back(page);
        submit(page, beanName + "." + "export");
        back(page);
        
        page = submit(page, beanName + "." + "new");
        page = submit(page, beanName + "." + "save");
        page = submit(page, beanName + "." + "delete");
        page = submit(page, beanName + "." + "reset");
        page = submit(page, beanName + "." + "cancel");
        return page;
    }

    private void runServer() {
        //TODO: start nano.h5 and hsqldb
    }
}
