/*
 * File: $HeadURL$
 * Id  : $Id$
 * 
 * created by: Thomas Schneider
 * created on: Mar 9, 2012
 * 
 * Copyright: (c) Thomas Schneider 2012, all rights reserved
 */
package de.tsl2.nano.bean.def;

import java.io.Serializable;

import de.tsl2.nano.action.IActivable;

/**
 * definitions to present a single value. extends and encapsulate the information of IAttributeDefinition.
 * 
 * @author Thomas Schneider
 * @version $Revision$
 */
public interface IPresentable extends Serializable {
    /** returns a component type */
    int getType();

    /** returns a component style */
    int getStyle();

    /** returns a dynamic enabler instance or null */
    IActivable getEnabler();

    /** returns true, if the field should be visible */
    boolean isVisible();

    /** returns the label or title */
    String getLabel();

    /** returns a description */
    String getDescription();

    /** returns optional layout. if it is used as descriptor only, it is recommended to fill maps of type 
     * {@link Map<String, ?} to be transformable into the graphical information object. */
    <L extends Serializable> L getLayout();

    /** returns optional layout constraintsif it is used as descriptor only, it is recommended to fill maps of type 
     * {@link Map<String, ?} to be transformable into the graphical information object. */
    <L extends Serializable> L getLayoutConstraints();

    /** optional fixed pixel with (valuable through layout constraints) */
    int getWidth();
    
    /** optional fixed pixel height (valuable through layout constraints) */
    int getHeight();
    
    /** file name for icon */
    String getIcon();

    /** foreground RGB color */
    int[] getForeground();

    /** background RGB color */
    int[] getBackground();

    /** sets standard presentable properties. for further informations, see docs of getters. */
    <L extends Serializable, T extends IPresentable> T setPresentation(String label,
            int type,
            int style,
            IActivable enabler,
            boolean visible,
            L layout,
            L layoutConstraints,
            String description);

    /** sets enhanced grafic properties. for further informations, see docs of getters. */
    <T extends IPresentable> T setPresentationDetails(int[] foreground, int[] background, String icon);

    /** sets type */
    <T extends IPresentable> T setType(int type);

    /** sets style */
    <T extends IPresentable> T setStyle(int style);

    /** sets optional layout */
    <L extends Serializable, T extends IPresentable> T setLayout(L layout);
    
    /** sets optional layout constraints */
    <L extends Serializable, T extends IPresentable> T setLayoutConstraints(L layoutConstraints);

    /** sets an enabler. use {@link IActivable#INACTIVE} to totally disable an item */
    public <T extends IPresentable> T setEnabler(IActivable enabler);
    
    /** sets a visible flag */
    public <T extends IPresentable> T setVisible(boolean visible);
    
    /** convenience to evaluate layout/layoutconstraint properties (perhaps, if layout or layoutconstraints are maps) */
    public String layout(String name);
    
    /** convenience to evaluate layout/layoutconstraint properties (perhaps, if layout or layoutconstraints are maps) */
    public <T> T layout(String name, T defaultValue);
    
    /** usable for int definitions that are not bitfields */
    public static final int UNDEFINED = IConstraint.UNDEFINED;

    /** usable for all int definitions like bitfiels type and style */
    public static final int UNSET = IConstraint.UNSET;

    /*
     * basic presentation types
     */
    /** to create text components (text and numbers) */
    public static final int TYPE_INPUT = 1;
    /** to create list components (combobox or list) */
    public static final int TYPE_SELECTION = 2;
    /** to create buttons (radio or checked) */
    public static final int TYPE_OPTION = 4;
    /** input type date */
    public static final int TYPE_DATE = 8;
    /** input type time */
    public static final int TYPE_TIME = 16;

    /*
     * additional presenation types
     */
    /** to create a label component */
    public static final int TYPE_LABEL = 32;
    /** to create not only a list but a table component */
    public static final int TYPE_TABLE = 64;
    /** to create not only a list but a tree component */
    public static final int TYPE_TREE = 128;
    /** provides a button to open a file selection box */
    public static final int TYPE_ATTACHMENT = 256;
    /** to create sub panels. to have a multiline text, you may combine {@link #TYPE_INPUT} and TYPE_GROUP */
    public static final int TYPE_GROUP = 512;
    /**  information to create new nested form panel */
    public static final int TYPE_FORM = 1 << 10;
    /** extended type to be used e.g. for an html field */
    public static final int TYPE_PAINT = 1 << 11;
    /** text with carriage returns */
    public static final int TYPE_INPUT_MULTILINE = 1 << 12;
    /** e.g. for database blobs to be shown directly */
    public static final int TYPE_DATA = 1 << 13;

    /*
     * additional input types (see html5)
     */
    public static final int TYPE_INPUT_NUMBER = 1 << 15;
    public static final int TYPE_INPUT_TEL = 1 << 16;
    public static final int TYPE_INPUT_EMAIL = 1 << 17;
    public static final int TYPE_INPUT_URL = 1 << 18;
    public static final int TYPE_INPUT_PASSWORD = 1 << 19;
    public static final int TYPE_INPUT_SEARCH = 1 << 20;
    public static final int TYPE_OPTION_RADIO = 1 << 21;

    public static final int STYLE_SINGLE = 1;
    public static final int STYLE_MULTI = 2;
    
    /*
     * define some abstract alignments. on each gui system, you will need to convert the numbers
     */
    public static final int ALIGN_LEFT= 1;
    public static final int ALIGN_CENTER = 2;
    public static final int ALIGN_RIGHT = 4;
    public static final int ALIGN_TOP = 8;
    public static final int ALIGN_BOTTOM = 16;
    
    public static final int[] COLOR_WHITE = new int[]{255, 255, 255};
    public static final int[] COLOR_BLACK = new int[]{0, 0, 0};
    public static final int[] COLOR_GRAY = new int[]{128, 128, 128};
    public static final int[] COLOR_LGRAY = new int[]{192, 192, 192};
    public static final int[] COLOR_RED = new int[]{255, 0, 0};
    public static final int[] COLOR_GREEN = new int[]{0, 255, 0};
    public static final int[] COLOR_BLUE = new int[]{0, 0, 255};
    public static final int[] COLOR_LRED = new int[]{255, 64, 64};
    public static final int[] COLOR_LGREEN = new int[]{64, 255, 64};
    public static final int[] COLOR_LBLUE = new int[]{64, 64, 192};
    public static final int[] COLOR_YELLOW = new int[]{255, 255, 0};

    public static final String POSTFIX_SELECTOR = ".selector";
    
    public static final IPresentable DEFAULT = new IPresentable() {
        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        @Override
        public <T extends IPresentable> T  setPresentationDetails(int[] foreground, int[] background, String icon) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <L extends Serializable, T extends IPresentable> T  setPresentation(String label,
                int type,
                int style,
                IActivable enabler,
                boolean visible,
                L layout,
                L layoutConstraints,
                String description) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public int getType() {
            return UNSET;
        }

        @Override
        public int getStyle() {
            return UNSET;
        }

        @Override
        public Serializable getLayoutConstraints() {
            return null;
        }

        @Override
        public Serializable getLayout() {
            return null;
        }

        @Override
        public String getLabel() {
            return null;
        }

        @Override
        public String getIcon() {
            return null;
        }

        @Override
        public int[] getForeground() {
            return null;
        }

        @Override
        public IActivable getEnabler() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public int[] getBackground() {
            return null;
        }

        @Override
        public int getWidth() {
            return UNDEFINED;
        }

        @Override
        public int getHeight() {
            return UNDEFINED;
        }

        @Override
        public <T extends IPresentable> T setType(int type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends IPresentable> T setStyle(int style) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <L extends Serializable, T extends IPresentable> T setLayout(L layout) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <L extends Serializable, T extends IPresentable> T setLayoutConstraints(L layoutConstraints) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public <T extends IPresentable> T setEnabler(IActivable enabler) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public <T extends IPresentable> T setVisible(boolean visible) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String layout(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T layout(String name, T defaultValue) {
            throw new UnsupportedOperationException();
        }

    };
}
