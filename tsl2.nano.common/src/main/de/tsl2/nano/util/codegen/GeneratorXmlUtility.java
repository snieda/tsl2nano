package de.tsl2.nano.util.codegen;

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GeneratorXmlUtility extends GeneratorUtility{
    
    Iterable<Node> iterable(NodeList nodeList) {
        ArrayList<Node> list = new ArrayList<>(nodeList.getLength());
        for (int i = 0; i< nodeList.getLength(); i++) {
            list.add(nodeList.item(0));
        }
        return list;
    }    
}