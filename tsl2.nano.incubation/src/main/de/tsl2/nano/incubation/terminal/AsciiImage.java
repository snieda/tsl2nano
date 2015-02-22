package de.tsl2.nano.incubation.terminal;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import de.tsl2.nano.core.util.FileUtil;

/**
 * transforms a picture of pixels into a picture of characters.
 * <p/>
 * Important Note: as ImageIO.read() returns java.awt.BufferedImage, this class is not usable on android systems!
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class AsciiImage {

//    /** scan rate, higher numbers will down-scale the image */
//    int rate = 10;
    /** RGB weights */
    double RGB[] = new double[] { 0.30, 0.11, 0.59 };

    /** the character mapping for the range 0-250 as indexes of 0-25. */
    char CHARS[] = {
        '@', '@', '@', '@', '@', '@',
        '#', '#',
        '8', '8', '8',
        '&',
        '^', '^', '^', '^', '^',
        '+', '+',
        '*', '*',
        '.', '.', '.',
        ' ', ' '
    };

    /** 4-gray shades with bars */
    char BARS[] = {
        0xDB, 0xDB, 0xDB, 0xDB, 0xDB, 0xDB,
        0xDB, 0xDB,
        0xB2, 0xB2, 0xB2,
        0xB2,
        0xB1, 0xB1, 0xB1, 0xB1, 0xB1,
        0xB1, 0xB1,
        0xB0, 0xB0,
        0xB0, 0xB0, 0xB0,
        ' ', ' '
    };

    /**
     * delegates to {@link #convertToAscii(String)} creating an own printwriter
     */
    public PrintWriter convertToAscii(String imgName) throws Exception {
        return convertToAscii(imgName, new PrintWriter(new FileWriter(FileUtil.getUniqueFileName(imgName) + ".txt",
            true)), -1, 25);
    }

    /**
     * convertToAscii
     * 
     * @param image image file name
     * @param printer print writer
     * @param width maximum count of horizontal characters. if width = -1 and height = -1, the same size as the source
     *            will be created. if only width = -1, the height size will be used with same aspect ratio as the
     *            source.
     * @param height maximum count of vertical characters. if width = -1 and height = -1, the same size as the source
     *            will be created. if only height = -1, the width size will be used with same aspect ratio as the
     *            source.
     * @return given print writer holding the result
     * @throws Exception
     */
    public PrintWriter convertToAscii(String image, PrintWriter printer, int width, int height) throws Exception {
        BufferedImage img = ImageIO.read(new File(image));
        double px;
        Color pxColor;
        StringBuilder buf = new StringBuilder(img.getWidth());

        int xrate = width == -1 ? height == -1 ? 1 : -1 : (int) Math.round(img.getWidth() / (double) width);
        int yrate = height == -1 ? width == -1 ? 1 : xrate : (int) Math.round(img.getHeight() / (double) height);
        if (xrate == -1)
            xrate = yrate;

        for (int y = 0; y < img.getHeight(); y += yrate) {
            for (int x = 0; x < img.getWidth(); x += xrate) {
                px = 0;
                //create the average of a pixel block 
                for (int ry = 0; ry < yrate; ry++) {
                    //on rounding errors...
                    if (y + ry >= img.getHeight())
                        continue;
                    for (int rx = 0; rx < xrate; rx++) {
                        //on rounding errors...
                        if (x + rx >= img.getWidth())
                            continue;
                        pxColor = new Color(img.getRGB(x + rx, y + ry));
                        px +=
                            (((pxColor.getRed() * RGB[0]) + (pxColor.getGreen() * RGB[1]) + (pxColor.getBlue() * RGB[2])));
                    }
                }
                px = px / (xrate * yrate);
                buf.append(ascii(px));
            }
            printer.println(buf.toString());
            buf.setLength(0);
        }
        return printer;
    }

    public char ascii(double g) {
        return BARS[((int) g / 10)];
    }

    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.out.println("syntax: asciiimage <image-file> [-out <width> <height>]");
                return;
            }
            long start = System.currentTimeMillis();
            PrintWriter writer;
            if (args.length > 1 && args[1].equals("-out"))
                writer =
                    new AsciiImage().convertToAscii(args[0], new PrintWriter(System.out), Integer.valueOf(args[2]),
                        Integer.valueOf(args[3]));
            else {
                writer = new AsciiImage().convertToAscii(args[0]);
            }
            writer.close();
            System.out.println(DateFormat.getTimeInstance().format(new Date(System.currentTimeMillis() - start)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}