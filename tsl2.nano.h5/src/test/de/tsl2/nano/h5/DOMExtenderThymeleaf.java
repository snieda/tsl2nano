/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 22.11.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.h5;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.h5.inspect.IDOMDecorator;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class DOMExtenderThymeleaf implements IDOMDecorator {

    /**
     * {@inheritDoc}
     */
    @Override
    public void decorate(Document doc, ISession<?> session) {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("./");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML5");

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        Map<String, Object> variables = new HashMap<>();
        variables.put("message", "Hello, World from Thymeleaf!");
        IContext context = new Context(Locale.getDefault(), variables);

        
        Node body = doc.getElementsByTagName("body").item(0);
        Element newParagraph = doc.createElement("embed");
        String file = ENV.getTempPath() + "thymeoutput.html";
        FileUtil.printToFile(file, c->templateEngine.process("home", context, c));
        newParagraph.setAttribute("src", file);
        newParagraph.setAttribute("mediatype", "text/html");
        body.appendChild(newParagraph);
     }

}
