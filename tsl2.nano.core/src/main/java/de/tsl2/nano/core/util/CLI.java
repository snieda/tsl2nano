package de.tsl2.nano.core.util;

/**
 * Command Line Interface for Java with ANSI escape Sequences
 * </p>
 * ANSI escape sequences are a standard for in-band signaling to control the
 * cursor location, color, and other options on video text terminals and
 * terminal emulators. Certain sequences of bytes, most starting with Esc and
 * '[', are embedded into the text, which the terminal looks for and interprets
 * as commands, not as character codes.
 * 
 * @see https://en.wikipedia.org/wiki/ANSI_escape_code#Example_of_use_in_shell_scripting
 */

public class CLI {
    /* Control Sequence Introducer */
    static final String CSI = "\033[";
    static final int RESET = 0;

    static final int COLOR_BASE = 30;
    static final int BRIGHT_FOREGROUND = 90;
    static final int BRIGHT_BACKGROUND = 100;

    static final String TAG_COLOR = "\033[%d;%d;%dm";
    public static final String NC = "\033[0m";


    public enum Style {
        NORMAL, BOLD, DARK, ITALIC, UNDERLINED
    }

    /*
     * SGR Parameters
     */
    public enum Font {
        Reset, Bold, Faint, Italic, Underline, BlinkSlow, BlickRapid, InverseColor, Default, A1, A2, A3, A4, A5, A6, A7,
        A8, A9, Fraktur, DoubleUnderline, Normal, NotItalicNotFraktur, UnderlineOFF, Blink, InverseOFF, Reveal,
        NotCrossedOut
    };

    public enum Color {
        BLACK, RED, GREEN, ORANGE, BLUE, PURPLE, CYAN, LIGHT_GRAY, SET_FOREGROUND/* 38 */, DEFAULT_FOREGROUND, /* 39 */
        DARK_GRAY, LIGHT_RED, LIGHT_GREEN, YELLOW, LIGHT_BLUE, LIGHT_PURPLE, LIGHT_CYAN, WHITE, SET_BACKGROUND/* 48 */,
        DEFAULT_BACKGROUND
        }

    public enum Cursor {
        Up('A'), Down('B'), Forward('C'), Back('D'), NextLine('E'), PreviousLine('F'), HorAbsolute('G'), Position('H'),
        EraseInDisplay('J'), EraseInLine('K'), ScrollUp('S'), ScrollDown('T');

        char key;

        Cursor(char key) {
            this.key = key;
        }
    }

    enum Aux {
        PortOn("5i"), PortOff("4i"), DeviceStatusReport("6" /* ESC[n;mR, where n is the row and m is the column. */ ),
        SaveCursorPosition("s"), RestoreCursorPosition("u");

        String key;

        Aux(String key) {
            this.key = key;
        }
    }

    public static final String setFont(Font f) {
        return CSI + f.ordinal() + "m";
    }

    public static final String setColor(Color c) {
        return CSI + c.ordinal() + "m";
    }

    public static final String moveCursor(Cursor direction, int steps) {
        return CSI + steps + direction.key;
    }

    /**
     * Moves the cursor to row n, column m. The values are 1-based, and default to 1
     * (top left corner) if omitted. A sequence such as CSI ;5H is a synonym for CSI
     * 1;5H as well as CSI 17;H is the same as CSI 17H and CSI 17;1H
     */
    public static final String moveCursor(String rowN, String colM) {
        return CSI + rowN + ";" + colM + Cursor.Position.key;
    }

    public static String reset(String s) {
        return CSI + s + RESET;
    }

    public static String tag(Object txt, Color color) {
        return tag(txt, color, null);
    }

    public static String tag(Object txt, Color color, Color background) {
        return tag(txt, color, background, null);
    }

    public static String tag(Object txt, Color color, Color background, Style style) {
        return tag(color, background, style) + txt + NC;
    }

    public static String tag(Color color) {
        return tag(color, null);
    }

    public static String tag(Color color, Color background) {
        return tag(color, background, (Style) null);
    }

    public static String tag(Color color, Color background, Style style) {
        if (Boolean.getBoolean("dont.use.ansi.escape"))
            return ""; //on legacy systems like windows older than windows 10, ansi escape codes are unavailable
        color = color != null ? color : Color.LIGHT_GRAY;
        int lightOrStyle = style != null ? style.ordinal() : color.ordinal() > 7 ? 1 : 0;
        int c = (color.ordinal() % 10) + COLOR_BASE;
        int b = background != null ? background.ordinal() + COLOR_BASE + 10 : 1;
        return String.format(TAG_COLOR, lightOrStyle, c, b);
    }

}
