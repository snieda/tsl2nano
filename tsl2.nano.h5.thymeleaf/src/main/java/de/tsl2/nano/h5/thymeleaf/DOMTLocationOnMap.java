/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Tom, Thomas Schneider
 * created on: 22.11.2017
 * 
 * Copyright: (c) Thomas Schneider 2017, all rights reserved
 */
package de.tsl2.nano.h5.thymeleaf;

import static de.tsl2.nano.h5.HtmlUtil.ATTR_HEIGHT;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_TITLE;
import static de.tsl2.nano.h5.HtmlUtil.ATTR_WIDTH;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.tsl2.nano.bean.def.Bean;
import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ISession;
import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.h5.HtmlUtil;
import de.tsl2.nano.h5.NanoH5Session;
import de.tsl2.nano.h5.plugin.IDOMDecorator;

/**
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$ 
 */
public class DOMTLocationOnMap implements IDOMDecorator {

    public static final String NODE_FIELDPANEL = "field.panel";

    public DOMTLocationOnMap() {
		ENV.extractResource("location-on-googlemaps.html");
		ENV.extractResource("location-on-osm.html");
    }
    
	/**
     * {@inheritDoc}
     */
    @Override
    public void decorate(Document doc, ISession<?> session) {
    	decoratePanel(doc, session);
    }
    
    public static void decoratePanel(Document doc, ISession<?> session) {
    	decoratePanel(doc, session, NODE_FIELDPANEL, "location-on-osm.html", new HashMap<>());
    }
    
    public static void decoratePanel(Document doc, ISession<?> session, String previousTagID, String templateName, Map<String, Object> variables) {
        Node previousTag = HtmlUtil.getElementById(doc.getDocumentElement(), previousTagID);
        if (previousTag != null) {
	        TemplateEngine templateEngine = createThymeleafTemplateEngine();
	        IContext context = createThymeleafContext(session, variables, true);
	        
	        Element newParagraph = doc.createElement("iframe");
	        String file = ENV.getTempPath() + templateName;
	        FileUtil.printToFile(file, c->templateEngine.process(templateName, context, c));
	        newParagraph.setAttribute("src", ENV.getTempPathURL() + templateName);
	        newParagraph.setAttribute(ATTR_WIDTH, "100%");
	        newParagraph.setAttribute(ATTR_HEIGHT, "100%");
	        newParagraph.setAttribute("mediatype", "text/html");
	        newParagraph.setAttribute(ATTR_TITLE, templateName);
	        previousTag.appendChild(newParagraph);
        }
     }

	private static IContext createThymeleafContext(ISession<?> session, Map<String, Object> variables, boolean replaceDotWithUnderscore) {
		HashMap<String, Object> vars = new HashMap<>(ENV.getProperties());
		vars.putAll(((NanoH5Session)session).getContext());
		Bean bean = (Bean)session.getWorkingObject();
		vars.putAll(bean.toValueMap(null));
		vars.put(bean.getName(), bean.getInstance());
		//WORKAROUND for thymeleaf replacement-process
		if (replaceDotWithUnderscore) {
			HashMap<String, Object> vcopy = new HashMap<>(vars);
			Set<String> keys = vcopy.keySet();
			for (String key : keys) {
				vars.put(key.replace('.', '_'), vcopy.get(key));
			}
		}
		////////////////////////
        IContext context = new Context(Locale.getDefault(), vars);
		return context;
	}

	private static TemplateEngine createThymeleafTemplateEngine() {
		ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("./");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML5");

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
		return templateEngine;
	}

}
