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
import java.util.List;

import de.tsl2.nano.action.IActivable;
import de.tsl2.nano.action.IConstraint;

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

    /** returns true, if this instance should be presented in a search mask */
    boolean isSearchable();
    
    /** returns true, if this instance should be presented in a nesting detail panel */
    boolean isNesting();
    
    /** returns the label or title */
    String getLabel();

    /** returns a description */
    String getDescription();

    /**
     * @returns an optional item list to be usable as input assist with auto-completion.
     */
    List<?> getItemList();
    
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

    void setIcon(String icon);
    
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

    /** add layout constraint element to the current layout constraints */
    <T extends IPresentable> T addLayoutConstraints(String name, Object value);
    
    /** sets an enabler. use {@link IActivable#INACTIVE} to totally disable an item */
    public <T extends IPresentable> T setEnabler(IActivable enabler);
    
    /** sets a visible flag */
    public <T extends IPresentable> T setVisible(boolean visible);
    
    /** convenience to evaluate layout/layoutconstraint properties (perhaps, if layout or layoutconstraints are maps) */
    public String layout(String name);
    
    /** convenience to evaluate layout/layoutconstraint properties (perhaps, if layout or layoutconstraints are maps) */
    public <T> T layout(String name, T defaultValue);
    
    /** don't use that value - don't set any default values on it */
    public static final int UNUSABLE = -2;

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
    /** type will be defined by value/data on runtime */
    public static final int TYPE_DEPEND = UNDEFINED;

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

    /** standard style */
    public static final int STYLE_SINGLE = 1;
    /** to be used for e.g. multiline text (textarea), multiple selection, etc. */
    public static final int STYLE_MULTI = 2;
    
    /*
     * define some abstract alignments. on each gui system, you will need to convert the numbers
     */
    public static final int STYLE_ALIGN_LEFT = 4;
    public static final int STYLE_ALIGN_CENTER = 8;
    public static final int STYLE_ALIGN_RIGHT = 16;
    public static final int STYLE_ALIGN_TOP = 32;
    public static final int STYLE_ALIGN_BOTTOM = 64;
    
    /*
     * this styles are defined for html5 media tags.
     * NOTE: STYLE_DATA_IMG has to be the lowest one of this bock - 
     *       STYLE_DATA_FRAME the highest one! there doesn't has to be any leak.
     */
    public static final int STYLE_DATA_IMG = 128;
    public static final int STYLE_DATA_EMBED = 256;
    public static final int STYLE_DATA_OBJECT = 512;
    public static final int STYLE_DATA_CANVAS = 1 << 10;
    public static final int STYLE_DATA_AUDIO = 1 << 11;
    public static final int STYLE_DATA_VIDEO = 1 << 12;
    public static final int STYLE_DATA_DEVICE = 1 << 13;
    public static final int STYLE_DATA_SVG = 1 << 14;
    public static final int STYLE_DATA_FRAME = 1 << 15;

    /*
     * standard colors
     */
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
    
    @SuppressWarnings("unchecked")
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
        public boolean isSearchable() {
            return false;
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

        /**
         * {@inheritDoc}
         */
        @Override
        public void setIcon(String icon) {
            throw new UnsupportedOperationException();
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

        
        /**
         * {@inheritDoc}
         */
        @Override
        public <T extends IPresentable> T addLayoutConstraints(String name, Object value) {
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
        @Override
        public java.util.List<?> getItemList() {
            return null;
        }
        
        @Override
        public boolean isNesting() {
            return false;
        }
    };
}