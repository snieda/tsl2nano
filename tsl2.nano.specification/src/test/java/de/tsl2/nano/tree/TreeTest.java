package de.tsl2.nano.tree;

import java.util.Scanner;

import org.junit.Assert;
import org.junit.Test;

import de.tsl2.nano.tree.Tree;

public class TreeTest {

    @Test
//    @Ignore
    public void testTree() throws Exception {
        String src = "";
//        digraph G {
//            2: size ="4,4";
//            3: main [shape=box]; /* this is a comment */
//            4: main -> parse [weight=8];
//            5: parse -> execute;
//            6: main -> init [style=dotted];
//            7: main -> cleanup;
//            8: execute -> { make_string; printf}
//            9: init -> make_string;
//            10: edge [color=red]; // so is this
//            11: main -> printf [style=bold,label="100 times"];
//            12: make_string [label="make a\nstring"];
//            13: node [shape=box,style=filled,color=".7 .3 1.0"];
//            14: execute -> compare;
//            15: } 

        Tree<String, String> graph = new Tree<String, String>("main", null);
        graph.add("label=nix", "child");
        graph.add("color=unsichtbar", "{leaf1, leaf2,leaf3}");
        Tree<String, String> graph2 = Tree.fromString(new Scanner(graph.toString()));
        String ln = System.lineSeparator();
        String formatGraph = "main -> leaf1[color=unsichtbar];" + ln + "main -> child[label=nix];"
                + ln + "main -> leaf2[leaf2];" + ln + "main -> leaf3[leaf3];" + ln;
        		
        Assert.assertEquals(formatGraph, graph2.toString());
    }

}
