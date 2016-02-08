package de.tsl2.nano.incubation.graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.knowm.xchart.ChartBuilder_Category;
import org.knowm.xchart.ChartBuilder_Pie;
import org.knowm.xchart.ChartBuilder_XY;
import org.knowm.xchart.Chart_Category;
import org.knowm.xchart.Chart_Pie;
import org.knowm.xchart.Chart_XY;
import org.knowm.xchart.Series_XY.ChartXYSeriesRenderStyle;
import org.knowm.xchart.VectorGraphicsEncoder;
import org.knowm.xchart.VectorGraphicsEncoder.VectorGraphicsFormat;
import org.knowm.xchart.internal.ChartBuilder;
import org.knowm.xchart.internal.chartpart.Chart;
import org.knowm.xchart.internal.style.Styler.ChartTheme;
import org.knowm.xchart.internal.style.markers.SeriesMarkers;

import de.tsl2.nano.core.ENV;
import de.tsl2.nano.core.ManagedException;
import de.tsl2.nano.core.util.FileUtil;

/**
 * creates simple charts and saves them to at least SVG files. this class has indirect dependencies to java.awt! so it
 * is not usable on android dalvik. while there exist some libraries like jfreechart, jfreesvg, gral, ... we decided to
 * use the small xchart library with dependency to VectorGraphics2D.
 * <p/>
 * the author of gral shows an overview of java plotters here: http://trac.erichseifert.de/gral/wiki/Comparison.<br/>
 * in wikipedia you get an overview of graph libraries here:
 * https://en.wikipedia.org/wiki/Wikipedia:How_to_create_charts_for_Wikipedia_articles
 * 
 * @author Tom, Thomas Schneider
 * @version $Revision$
 */
public class SVGChart {
    public enum Type {
        XY, BAR, PIE, SCATTER;
    }

    /**
     * uses xgraph to create a simple XY graph persisting it to an svg file.
     * 
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static <N extends Number> void createGraph(String title,
            String xTitle,
            String yTitle,
            int width,
            int height,
            N... yData) {
        Chart chart =
            createChart(Type.XY, title, xTitle, yTitle, width, height, false, null, serie("y(x)", Arrays.asList(yData)));
        String file = ENV.getTempPath() + FileUtil.getValidFileName(title);
        exportGraph(file, chart);
    }

    /**
     * exportGraph
     * 
     * @param title
     * @param chart
     */
    public static void exportGraph(String file, Chart chart) {
        try {
//            BitmapEncoder.saveBitmap(chart, file, BitmapFormat.PNG);
//            BitmapEncoder.saveBitmap(chart, file, BitmapFormat.JPG);
//            BitmapEncoder.saveJPGWithQuality(chart, file + "_With_Quality.jpg", 0.95f);
//            BitmapEncoder.saveBitmap(chart, file, BitmapFormat.BMP);
//            BitmapEncoder.saveBitmap(chart, file, BitmapFormat.GIF);
//
//            BitmapEncoder.saveBitmapWithDPI(chart, file + "_300_DPI", BitmapFormat.PNG, 300);
//            BitmapEncoder.saveBitmapWithDPI(chart, file + "_300_DPI", BitmapFormat.JPG, 300);
//            BitmapEncoder.saveBitmapWithDPI(chart, file + "_300_DPI", BitmapFormat.GIF, 300);
//
//            VectorGraphicsEncoder.saveVectorGraphic(chart, file, VectorGraphicsFormat.EPS);
//            VectorGraphicsEncoder.saveVectorGraphic(chart, file, VectorGraphicsFormat.PDF);
            VectorGraphicsEncoder.saveVectorGraphic(chart, file, VectorGraphicsFormat.SVG);
        } catch (Exception e) {
            ManagedException.forward(e);
        }
    }

    public static String createGraph(String title,
            String xTitle,
            String yTitle,
            int width,
            int height,
            List<Object> x,
            Serie... yn) {
        return createGraph(Type.BAR, title, xTitle, yTitle, width, height, false, x, yn);
    }
    
    public static String createGraph(Type type, String title,
            String xTitle,
            String yTitle,
            int width,
            int height,
            boolean yLogarithmic,
            List<Object> x,
            Serie... yn) {
        Chart chart = createChart(type, title, xTitle, yTitle, width, height, yLogarithmic, x, yn);
        String file = ENV.getTempPath() + FileUtil.getValidFileName(title);
        exportGraph(file, chart);
        return file;
    }

    public static Chart createChart(Type type, String title,
            String xTitle,
            String yTitle,
            int width,
            int height,
            boolean yLogarithmic,
            List<Object> x,
            Serie... yn) {

        // Create Chart
        ChartBuilder builder;
        switch (type) {
        case BAR:
            builder = new ChartBuilder_Category().xAxisTitle(xTitle).yAxisTitle(yTitle);
            break;
        case XY:
        case SCATTER:
            builder = new ChartBuilder_XY().xAxisTitle(xTitle).yAxisTitle(yTitle);
            break;
        case PIE:
            builder = new ChartBuilder_Pie();
            break;
        default:
            throw new IllegalArgumentException();
        }
        Chart chart = builder.width(width).height(height).theme(ChartTheme.GGPlot2).title(title).build();

        // Customize Chart
//        series.setMarker(SeriesMarkers.CIRCLE);
//      chart.getStyler().setPlotGridLinesVisible(false);
//        chart.getStyler().setXAxisTickMarkSpacingHint(100);

        if (type == Type.SCATTER)
            ((Chart_XY) chart).getStyler().setDefaultSeriesRenderStyle(ChartXYSeriesRenderStyle.Scatter);

        // Series
        if (chart instanceof Chart_Category) {
            Chart_Category c = (Chart_Category) chart;
            for (Serie s : yn) {
                c.addSeries(s.title, x, s.data);
            }
        } else if (chart instanceof Chart_XY) {
            Chart_XY c = (Chart_XY) chart;
            for (Serie s : yn) {
                c.addSeries(s.title, x, s.data);
            }
        } else if (chart instanceof Chart_Pie) {
            Chart_Pie c = (Chart_Pie) chart;
            for (int i = 0; i < yn[0].data.size(); i++) {
                c.addSeries(x.get(i).toString(), yn[0].data.get(i));
            }
        }
        return chart;
    }

    public static Chart createChart(String plotFile) {
        //TODO: implement
        try {
            Scanner sc = new Scanner(new File(plotFile));
        } catch (FileNotFoundException e) {
            ManagedException.forward(e);
        }
        return null;
    }
    
    /**
     * creates series like {@link #serie(String, List)} for titles and yx lists.
     * <p/>
     * WARNING: the first title will be ignored as it should be the x title!
     * 
     * @param titles series title
     * @param yx one or more data series
     * @return packed series
     */
    public static final Serie[] series(List<String> titles, List[] yx) {
        Serie yn[] = new Serie[yx.length];
        for (int i = 0; i < yx.length; i++) {
            yn[i] = new Serie(titles.get(i + 1), yx[i]);
        }
        return yn;
    }

    public static final Serie serie(String title, List<? extends Number> data) {
        return new Serie(title, data, null);
    }

    public static final Serie serie(String title, List<? extends Number> data, SeriesMarkers marker) {
        return new Serie(title, data, marker);
    }

}

class Serie {
    String title;
    List<? extends Number> data;
    SeriesMarkers marker;

    public Serie(String title, List<? extends Number> data) {
        this(title, data, null);
    }

    public Serie(String title, List<? extends Number> data, SeriesMarkers marker) {
        super();
        this.title = title;
        this.data = data;
        this.marker = marker;
    }

}
