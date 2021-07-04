/*
 * Created on 25.08.2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.tsl2.nano.incubation.terminal;

import java.io.IOException;
import java.util.StringTokenizer;

import de.tsl2.nano.core.util.FileUtil;
import de.tsl2.nano.core.util.StringUtil;

/**
 * @author Tom
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TextTerminal {
    public static final char SPACE = ' ';
    public static final char NEWLINE = '\n';
    public static final int SCREEN_WIDTH = 79;
    public static final int SCREEN_HEIGHT = 25;
    // simple point
    public static final char GRAPH_POINT = 0xFE;
    // simple text line
    public static final char TEXT_HOR_LINE = '-';
    public static final char TEXT_VER_LINE = '|';
    public static final char TEXT_TOPLEFT_LINE = '/';
    public static final char TEXT_TOPRIGHT_LINE = '\\';
    public static final char TEXT_BOTTOMLEFT_LINE = '\\';
    public static final char TEXT_BOTTOMRIGHT_LINE = '/';

    // simple line
    public static final char GRAPH_HOR_LINE = 0xC4;
    public static final char GRAPH_VER_LINE = 0xB3;
    public static final char GRAPH_TOPLEFT_LINE = '/';
    public static final char GRAPH_TOPRIGHT_LINE = '\\';
    public static final char GRAPH_BOTTOMLEFT_LINE = '\\';
    public static final char GRAPH_BOTTOMRIGHT_LINE = '/';

    // Bars
    public static final char GRAPH_BIG_BAR = 0x9591;
    public static final char GRAPH_TOP_BAR = 0x9591;
    public static final char GRAPH_BOTTOM_BAR = 0x9591;

    public static final char GRAPH_LIGHTGRAY_BAR = 0x9591;
    public static final char GRAPH_MEDIUMGRAY_BAR = 0x9591;
    public static final char GRAPH_DARKGRAY_BAR = 0x9591;

    // double line for example: ÉÍÍÍÍÍÍÍÍ», º Halloo º, ÈÍÍÍÍÍÍÍÍ¼
    public static final char GRAPH_HOR_DBLLINE = '\u2550';
    public static final char GRAPH_VER_DBLLINE = '\u2551';
    public static final char GRAPH_TOPLEFT_DBLLINE = '\u2554';
    public static final char GRAPH_TOPRIGHT_DBLLINE = '\u2557';
    public static final char GRAPH_BOTTOMLEFT_DBLLINE = '\u255A';
    public static final char GRAPH_BOTTOMRIGHT_DBLLINE = '\u255D';

    // Lines
    public static final int LINE_HOR_BAR = 1;
    public static final int LINE_TOP_BAR = 2;
    public static final int LINE_BOTTOM_BAR = 3;
    public static final int LINE_LIGHTGRAY_BAR = 4;
    public static final int LINE_MEDIUMGRAY_BAR = 5;
    public static final int LINE_DARKGRAY_BAR = 6;
    public static final int LINE_HOR_SIMPLE = 7;
    public static final int LINE_TOP_SIMPLE = 8;
    public static final int LINE_BOTTOM_SIMPLE = 9;
    public static final int LINE_HOR_DOUBLE = 10;
    public static final int LINE_TOP_DOUBLE = 11;
    public static final int LINE_BOTTOM_DOUBLE = 12;

    // Block Styles
    public enum Frame {
        NONE, BAR, BAR_LIGHTGRAY, BAR_MEDIUMGRAY, BAR_DARKGRAY, LINE, DOUBLE_LINE, TEXT_LINE
    };

    /** horizontal alignments (used in getFormattedItem) */
    public static final int HORALIGN_NOTHING = 0;
    public static final int HORALIGN_LEFT = 1;
    public static final int HORALIGN_CENTER = 2;
    public static final int HORALIGN_RIGHT = 3;

    public static String getTextFrame(String text, Frame frameType, int width, boolean centered) {
    	assert width < 8000;
        text = getFormattedItem(text, width - 2, 0, null, "\n");
        switch (frameType) {
        case BAR:
            text = getLine(null, width, GRAPH_TOP_BAR, GRAPH_TOP_BAR, GRAPH_TOP_BAR, centered) + getLines(text,
                width,
                GRAPH_BIG_BAR,
                SPACE,
                GRAPH_BIG_BAR,
                centered) + getLine(null, width, GRAPH_BOTTOM_BAR, GRAPH_BOTTOM_BAR, GRAPH_BOTTOM_BAR, centered);
            break;
        case BAR_LIGHTGRAY:
        case BAR_MEDIUMGRAY:
        case BAR_DARKGRAY:
            char c = frameType == Frame.BAR_LIGHTGRAY ? GRAPH_LIGHTGRAY_BAR
                : frameType == Frame.BAR_LIGHTGRAY ? GRAPH_MEDIUMGRAY_BAR : GRAPH_DARKGRAY_BAR;
            text = getLine(null, width, c, c, c, centered) + getLines(text, width, c, SPACE, c, centered)
                + getLine(null, width, c, c, c, centered);
            break;
        case NONE:
        case TEXT_LINE:
            text =
                getLine(null, width, TEXT_TOPLEFT_LINE, TEXT_HOR_LINE, GRAPH_TOPRIGHT_LINE, centered) + getLines(text,
                    width,
                    TEXT_VER_LINE,
                    SPACE,
                    TEXT_VER_LINE,
                    centered)
                    + getLine(null, width, TEXT_BOTTOMLEFT_LINE, TEXT_HOR_LINE, GRAPH_BOTTOMRIGHT_LINE, centered);
            break;
        case LINE:
            text =
                getLine(null, width, GRAPH_TOPLEFT_LINE, GRAPH_HOR_LINE, GRAPH_TOPRIGHT_LINE, centered)
                    + getLines(text,
                        width,
                        GRAPH_VER_LINE,
                        SPACE,
                        GRAPH_VER_LINE,
                        centered)
                    + getLine(null, width, GRAPH_BOTTOMLEFT_LINE, GRAPH_HOR_LINE, GRAPH_BOTTOMRIGHT_LINE, centered);
            break;
        case DOUBLE_LINE:
            text =
                getLine(null, width, GRAPH_TOPLEFT_DBLLINE, GRAPH_HOR_DBLLINE, GRAPH_TOPRIGHT_DBLLINE, centered)
                    + getLines(text,
                        width,
                        GRAPH_VER_DBLLINE,
                        SPACE,
                        GRAPH_VER_DBLLINE,
                        centered)
                    + getLine(null, width, GRAPH_BOTTOMLEFT_DBLLINE, GRAPH_HOR_DBLLINE, GRAPH_BOTTOMRIGHT_DBLLINE,
                        centered);
            break;
        }
        // final String txt = text;
        // return Util.trY( () -> new String(txt.getBytes("iso8859-1"), "utf-8"));
        return text;
    }

    public static String getLines(String text, int width, char left, char mid, char right, boolean centered) {
        StringTokenizer strTokens = new StringTokenizer(text, String.valueOf(NEWLINE));
        text = "";
        while (strTokens.hasMoreTokens()) {
            text += getLine(strTokens.nextToken(), width, left, mid, right, centered);
        }
        return text;
    }

    public static String getLine(String text, int width, char left, char mid, char right, boolean centered) {
        return text = left + getLine(text, mid, width - 2, centered) + right + NEWLINE;
    }

    public static String getLine(String text, char filler, int width, boolean centered) {
    	assert width < 8000;
        text = text == null ? "" : text;
        int count = width - text.length();
        if (count < 1) {
            return text;
        }
        if (!centered) {
            return text + StringUtil.fixString(count, filler);
        } else {
            int spacelen = count / 2;
            text = StringUtil.fixString(spacelen, filler) + text + StringUtil.fixString(spacelen, filler);
            text += StringUtil.fixString(width - text.length(), filler);
            return text;
        }
    }

    /**
     * This method lets you format any substring of your text. If you give an alignment>0 (HORALIGN_NOTHING=0), your
     * substring will be filled with spaces to fit into the given width. For the TextPrinter formatting, you can give
     * concatenated styles (escape sequences), that will be inserted at the beginning of your substring. These styles
     * will be resetted by resets, that will be inserted at the end of your substring. For example:
     * getFormattedItem("hello", 10, HORALIGN_RIGHT, PRT_FONT_CURSIVE+PRT_FONT_BOLD, PRT_FONT_NCURSIVE+PRT_FONT_NORMAL)
     * returns: "(s1S(s3Bhello     (s0S(s0B"
     * 
     * @param item
     * @param width
     * @param alignment
     * @param styles
     * @param resets
     * @return
     */
    public static String getFormattedItem(String item, int width, int alignment, String styles, String resets) {
    	assert width < 8000;
        //if item text is to long
        item = StringUtil.format(item, width, resets);
        
        if (styles == null) {
            styles = "";
        }
        if (resets == null) {
            resets = "";
        }
        //tabs will be replaced with two spaces
        item = item.replace("\t", "  ");
        StringBuffer buf = new StringBuffer(styles);
        int space = width - item.length();
        switch (alignment) {
        case HORALIGN_NOTHING:
            buf.append(item);
            break;
        case HORALIGN_LEFT:
            buf.append(item);
            if (space > 0) {
                buf.append(StringUtil.fixString(space, ' '));
            }
            break;
        case HORALIGN_CENTER:
            if (space > 0) {
                buf.append(StringUtil.fixString(space / 2, ' '));
            }
            buf.append(item);
            if (space > 0) {
                buf.append(StringUtil.fixString(space / 2 + space % 2, ' '));
            }
            break;
        case HORALIGN_RIGHT:
            if (space > 0) {
                buf.append(StringUtil.fixString(space, ' '));
            }
            buf.append(item);
            break;
        }
        buf.append(resets);
        return buf.toString();
    }

//////////////////////////////////////////////////////////////////////////////
// Tests
//////////////////////////////////////////////////////////////////////////////
    static void testTerminalText() throws IOException {
        String testString = null;

        testString = getTextFrame("Hallo", Frame.BAR, SCREEN_WIDTH, false);
        System.out.println(testString);

        testString = getTextFrame("Option1\nOption2\nOption3", Frame.TEXT_LINE, SCREEN_WIDTH, true);
        System.out.println(testString);
        FileUtil.writeBytes(testString.getBytes(), "textprinter.txt", true);

        testString = getTextFrame("Option1\nOption2\nOption3", Frame.LINE, SCREEN_WIDTH, true);
        System.out.println(testString);
        FileUtil.writeBytes(testString.getBytes(), "textprinter.txt", true);

        testString = getTextFrame("Option1\nOption2\nOption3", Frame.BAR_LIGHTGRAY, SCREEN_WIDTH, true);
        System.out.println(testString);
        FileUtil.writeBytes(testString.getBytes(), "textprinter.txt", true);

        testString = getTextFrame("Option1\nOption2\nOption3", Frame.DOUBLE_LINE, SCREEN_WIDTH, true);
        System.out.println(testString);
        FileUtil.writeBytes(testString.getBytes(), "textprinter.txt", true);
        //TextPrinter printer = new TextPrinter();
        //printer.print(testString, "TextPrinter-Test", null);
    }

    static void testScreen() {
        //Zeichensatz Console: SIShell
        //einfache Text-Linie
        System.out.println("/--------\\"); //0xC4
        System.out.println("| Halloo |"); //0xB3
        System.out.println("\\--------/"); //0xC4
        //einfache Linie
        System.out.println("/ÄÄÄÄÄÄÄÄ\\"); //0xC4
        System.out.println("³ Halloo ³"); //0xB3
        System.out.println("\\ÄÄÄÄÄÄÄÄ/"); //0xC4
        //Balken
        System.out.println("ÜÜÜÜÜÜÜÜÜÜ"); //0xDC
        System.out.println("Û Halloo Û"); //0xDB
        System.out.println("ßßßßßßßßßß"); //0xDF
        //gepunkteter Balken
        System.out.println("²²²²²²²²²²"); //0xB0, 0xB1, 0xB2
        System.out.println("² Halloo ²"); //0xB2
        System.out.println("²²²²²²²²²²"); //0xB2
        //doppelte linie
        System.out.println("══════════"); //0xC9,CD,BB
        System.out.println("º Halloo º"); //0xBA
        System.out.println("\u2550"  + '\u2550'); //0xC8,CD,BC
    }

    public static void main(String[] args) {
        try {
            testTerminalText();
            testScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
