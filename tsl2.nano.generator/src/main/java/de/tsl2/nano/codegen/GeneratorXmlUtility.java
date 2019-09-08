package de.tsl2.nano.codegen;

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.tsl2.nano.core.util.FileUtil;

public class GeneratorXmlUtility extends GeneratorUtility{
    
    public Iterable<Node> iterableLeafs(NodeList nodeList) {
        return iterable(nodeList, true);
    }
    public Iterable<Node> iterable(NodeList nodeList, boolean onlyLeafs) {
        ArrayList<Node> list = new ArrayList<>(nodeList.getLength());
        Node n;
        for (int i = 0; i< nodeList.getLength(); i++) {
            n = nodeList.item(i);
            if (!onlyLeafs || (n.getNodeValue() != null && !n.getNodeValue().trim().isEmpty()) 
                || (n.getTextContent() != null && !n.getTextContent().trim().isEmpty()))
                list.add(nodeList.item(i));
        }
        return list;
    }

    public String  getNodeText(Node n) {
        return getNodeText(n, true);
    }
    protected String  getNodeText(Node n, boolean root) {
        String value =  n.getNodeValue() != null && !n.getNodeValue().trim().isEmpty() ? n.getNodeValue()
            : !n.hasChildNodes() && n.getNodeType() == Node.TEXT_NODE  && !n.getTextContent().trim().isEmpty() ? n.getTextContent().trim()
                : n.hasAttributes() ? getNodeText(n.getAttributes().item(0))
                    : n.hasChildNodes() ? getNodeText(n.getFirstChild(), false)
                        : !root && n.getNextSibling() != null ? getNodeText(n.getNextSibling(), root)
                            : n.getNodeName();
        return toFirstUpperCase(FileUtil.getValidFileName(value));
    }


    public String getName(Node n) {
        return toValidName(getNodeText(n));
    }
    public String getNameFL(Node n) {
        return toFirstLowerCase(getName(n));
    }
}
