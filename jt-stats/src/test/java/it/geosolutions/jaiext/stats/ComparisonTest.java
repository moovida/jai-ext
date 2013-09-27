package it.geosolutions.jaiext.stats;

import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.testclasses.TestBase;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.RenderedOp;

import org.junit.BeforeClass;
import org.junit.Test;

public class ComparisonTest extends TestBase {

    /** Number of benchmark iterations (Default 1) */
    private final static Integer BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.BenchmarkCycles", 1);

    /** Number of not benchmark iterations (Default 0) */
    private final static int NOT_BENCHMARK_ITERATION = Integer.getInteger(
            "JAI.Ext.NotBenchmarkCycles", 0);

    /** Boolean indicating if the old descriptor must be used */
    private final static boolean OLD_DESCRIPTOR = Boolean.getBoolean("JAI.Ext.OldDescriptor");

    /** Boolean indicating if the native acceleration must be used */
    private final static boolean NATIVE_ACCELERATION = Boolean.getBoolean("JAI.Ext.Acceleration");

    /** Boolean indicating if a No Data Range must be used */
    private final static boolean RANGE_USED = Boolean.getBoolean("JAI.Ext.RangeUsed");
    
    /** Boolean indicating if a ROI must be used */
    private final static boolean ROI_USED = Boolean.getBoolean("JAI.Ext.ROIUsed");

    /** Integer for selecting which statistic to compare */
    private final static int STATISTIC = Integer.getInteger("JAI.Ext.Statistic", 0);

    /** Source test image */
    private static RenderedImage testImage;
    /** No Data Range for Byte */
    private static Range rangeNDByte;
    /** No Data Range for UShort */
    private static Range rangeNDUSHort;
    /** No Data Range for Short */
    private static Range rangeNDShort;
    /** No Data Range for Integer */
    private static Range rangeNDInteger;
    /** No Data Range for Float */
    private static Range rangeNDFloat;
    /** No Data Range for Double */
    private static Range rangeNDDouble;
    /** Horizontal subsampling parameter */
    private static int xPeriod;
    /** Vertical subsampling parameter */
    private static int yPeriod;
    /** Array indicating the statistics to calculate */
    private static StatsType[] arrayStats;
    /** Array with band indexes */
    private static int[] bands;
    /** ROI object used for selecting the active area of the image */
    private static ROI roi; 

    // Initial static method for preparing all the test data
    @BeforeClass
    public static void initialSetup() {
        // Setting of the image filler parameter to true for a better image creation
        IMAGE_FILLER = true;
        // Images initialization
        byte noDataB = 100;
        short noDataUS = 100;
        short noDataS = 100;
        int noDataI = 100;
        float noDataF = 100;
        double noDataD = 100;
        // Image creations
        switch (TEST_SELECTOR) {
        case DataBuffer.TYPE_BYTE:
            testImage = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                    noDataB, false);
            break;
        case DataBuffer.TYPE_USHORT:
            testImage = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                    noDataUS, false);
            break;
        case DataBuffer.TYPE_SHORT:
            testImage = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                    noDataS, false);
            break;
        case DataBuffer.TYPE_INT:
            testImage = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataI,
                    false);
            break;
        case DataBuffer.TYPE_FLOAT:
            testImage = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                    noDataF, false);
            break;
        case DataBuffer.TYPE_DOUBLE:
            testImage = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                    noDataD, false);
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }
        // Image filler must be reset
        IMAGE_FILLER = false;
        // Range creation if selected
        if (RANGE_USED && !OLD_DESCRIPTOR) {
            switch (TEST_SELECTOR) {
            case DataBuffer.TYPE_BYTE:
                rangeNDByte = RangeFactory.create(noDataB, true, noDataB, true);
                break;
            case DataBuffer.TYPE_USHORT:
                rangeNDUSHort = RangeFactory.createU(noDataUS, true, noDataUS, true);
                break;
            case DataBuffer.TYPE_SHORT:
                rangeNDShort = RangeFactory.create(noDataS, true, noDataS, true);
                break;
            case DataBuffer.TYPE_INT:
                rangeNDInteger = RangeFactory.create(noDataI, true, noDataI, true);
                break;
            case DataBuffer.TYPE_FLOAT:
                rangeNDFloat = RangeFactory.create(noDataF, true, noDataF, true, true);
                break;
            case DataBuffer.TYPE_DOUBLE:
                rangeNDDouble = RangeFactory.create(noDataD, true, noDataD, true, true);
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        }
        
        //ROI creation
        if(ROI_USED){
            Rectangle rect = new Rectangle(0, 0, DEFAULT_WIDTH/4, DEFAULT_HEIGHT/4);
            roi = new ROIShape(rect);
        }else{
            roi = null;
        }
        
        // Statistic types definition

        if (STATISTIC == 0) {
            arrayStats = new StatsType[] { StatsType.MEAN };
        } else if (STATISTIC == 1) {
            arrayStats = new StatsType[] { StatsType.EXTREMA };
        }

        // Band definition
        bands = new int[] { 0, 1, 2 };

        // Period Definitions
        xPeriod = 1;
        yPeriod = 1;

    }

    // General method for showing calculation time of the 2 StatisticDescriptors
    @Test
    public void testStatsDescriptor() {

        // Statistic string
        String stat = "";
        if (STATISTIC == 0) {
            stat += "Mean";
        } else if (STATISTIC == 1) {
            stat += "Extrema";
        }

        Range rangeND = null;

        int dataType = TEST_SELECTOR;

        // Descriptor string
        String description = "\n ";
        String propertyName = "";

        // Control if the acceleration should be used for the old descriptor
        if (OLD_DESCRIPTOR) {
            propertyName += stat;
            description = "Old " + stat;
            if (NATIVE_ACCELERATION) {
                description += " accelerated ";
                System.setProperty("com.sun.media.jai.disableMediaLib", "false");
            } else {
                System.setProperty("com.sun.media.jai.disableMediaLib", "true");
            }
            // Control if the Range should be used for the new descriptor
        } else {
            propertyName += Statistics.SIMPLE_STATS_PROPERTY;
            description = "New " + stat;
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");

            if (RANGE_USED) {
                switch (dataType) {
                case DataBuffer.TYPE_BYTE:
                    rangeND = rangeNDByte;
                    break;
                case DataBuffer.TYPE_USHORT:
                    rangeND = rangeNDUSHort;
                    break;
                case DataBuffer.TYPE_SHORT:
                    rangeND = rangeNDShort;
                    break;
                case DataBuffer.TYPE_INT:
                    rangeND = rangeNDInteger;
                    break;
                case DataBuffer.TYPE_FLOAT:
                    rangeND = rangeNDFloat;
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    rangeND = rangeNDDouble;
                    break;
                default:
                    throw new IllegalArgumentException("Wrong data type");
                }
            }
        }
        // Data type string
        String dataTypeString = "";

        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            dataTypeString += "Byte";
            break;
        case DataBuffer.TYPE_USHORT:
            dataTypeString += "UShort";
            break;
        case DataBuffer.TYPE_SHORT:
            dataTypeString += "Short";
            break;
        case DataBuffer.TYPE_INT:
            dataTypeString += "Integer";
            break;
        case DataBuffer.TYPE_FLOAT:
            dataTypeString += "Float";
            break;
        case DataBuffer.TYPE_DOUBLE:
            dataTypeString += "Double";
            break;
        default:
            throw new IllegalArgumentException("Wrong data type");
        }

        // Total cycles number
        int totalCycles = BENCHMARK_ITERATION + NOT_BENCHMARK_ITERATION;
        // PlanarImage
        PlanarImage imageStats = null;
        // Initialization of the statistics
        long mean = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;

        // Cycle for calculating the mean, maximum and minimum calculation time
        for (int i = 0; i < totalCycles; i++) {

            // creation of the image with the selected descriptor

            if (OLD_DESCRIPTOR) {
                if (STATISTIC == 0) {
                    imageStats = javax.media.jai.operator.MeanDescriptor.create(testImage, roi,
                            xPeriod, yPeriod, null);
                } else if (STATISTIC == 1) {
                    imageStats = javax.media.jai.operator.ExtremaDescriptor.create(testImage, roi,
                            xPeriod, yPeriod, false, 1, null);
                }
            } else {
                imageStats = SimpleStatsDescriptor.create(testImage, xPeriod, yPeriod, roi, rangeND,
                        false, bands, arrayStats, null);
            }

            // Total statistic calculation time
            long start = System.nanoTime();
            imageStats.getProperty(propertyName);
            long end = System.nanoTime() - start;

            // If the the first NOT_BENCHMARK_ITERATION cycles has been done, then the mean, maximum and minimum values are stored
            if (i > NOT_BENCHMARK_ITERATION - 1) {
                if (i == NOT_BENCHMARK_ITERATION) {
                    mean = end;
                } else {
                    mean = mean + end;
                }

                if (end > max) {
                    max = end;
                }

                if (end < min) {
                    min = end;
                }
            }
            // For every cycle the cache is flushed such that all the tiles must be recalculated
            JAI.getDefaultInstance().getTileCache().flush();
        }
        // Mean values
        double meanValue = mean / BENCHMARK_ITERATION * 1E-6;

        // Max and Min values stored as double
        double maxD = max * 1E-6;
        double minD = min * 1E-6;
        // Comparison between the mean times
        System.out.println(dataTypeString);
        // Output print
        System.out.println("\nMean value for " + description + "Descriptor : " + meanValue
                + " msec.");
        System.out.println("Maximum value for " + description + "Descriptor : " + maxD + " msec.");
        System.out.println("Minimum value for " + description + "Descriptor : " + minD + " msec.");
        // Final Image disposal
        if (imageStats instanceof RenderedOp) {
            ((RenderedOp) imageStats).dispose();
        }

    }

    // UNSUPPORTED OPERATIONS
    @Override
    protected void testGlobal(boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImage(int dataType,
            T noDataValue, boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImageAffine(
            RenderedImage sourceImage, int dataType, T noDataValue, boolean useROIAccessor,
            boolean isBinary, boolean bicubic2Disabled, boolean noDataRangeUsed,
            boolean roiPresent, boolean setDestinationNoData, TransformationType transformType,
            InterpolationType interpType, TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    protected void testGlobalAffine(boolean useROIAccessor, boolean isBinary,
            boolean bicubic2Disabled, boolean noDataRangeUsed, boolean roiPresent,
            boolean setDestinationNoData, InterpolationType interpType, TestSelection testSelect,
            ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported");
    }

}
