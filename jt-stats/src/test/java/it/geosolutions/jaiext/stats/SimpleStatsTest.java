package it.geosolutions.jaiext.stats;

import static org.junit.Assert.*;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import it.geosolutions.jaiext.testclasses.TestBase;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test-class is used for evaluating if the SimpleStatsOpImage class computes correctly the requested statistics. For this purpose 6 methods are
 * created, each one testing a particular case:
 * <ul>
 * <li>Calculation without ROI and No Data.</li>
 * <li>Calculation with ROI but without No Data.</li>
 * <li>Calculation with ROI, using a RasterAccessor, but without No Data.</li>
 * <li>Calculation with NoData but without ROI.</li>
 * <li>Calculation with ROI and No Data.</li>
 * <li>Calculation with ROI, using a RasterAccessor, and No Data.</li>
 * </ul>
 * 
 * All of these cases are tested on all the JAI data types (Byte,Short, Unsigned Short, Integer, Float, Double), and on both 1 and 2 bands. The
 * correct statistics are calculated before the test executions, and then are compared inside all the tests. Every single test call the
 * testStatistics() method which calls the SimpleStatsDescriptor.create() method for returning a new instance of the SimpleStatsOpImage, then
 * calculates the statistics and finally compares them with the initial precalculated values.
 */
public class SimpleStatsTest extends TestBase {
    /** Tolerance value used for comparison between double */
    private final static double TOLERANCE = 0.1d;

    /** Horizontal subsampling parameter */
    private static int xPeriod;

    /** Vertical subsampling parameter */
    private static int yPeriod;

    /** ROI object used for selecting the active area of the image */
    private static ROI roi;

    /** No Data value for Byte */
    private static byte noDataB;

    /** No Data value for UShort */
    private static short noDataU;

    /** No Data value for Short */
    private static short noDataS;

    /** No Data value for Integer */
    private static int noDataI;

    /** No Data value for Float */
    private static float noDataF;

    /** No Data value for Double */
    private static double noDataD;

    /** No Data Range for Byte */
    private static Range noDataByte;

    /** No Data Range for UShort */
    private static Range noDataUShort;

    /** No Data Range for Short */
    private static Range noDataShort;

    /** No Data Range for Integer */
    private static Range noDataInt;

    /** No Data Range for Float */
    private static Range noDataFloat;

    /** No Data Range for Double */
    private static Range noDataDouble;

    /** Array indicating the statistics to calculate */
    private static StatsType[] stats;

    /** Array with only one band index */
    private static int[] band1;

    /** Array with 2 band indexes */
    private static int[] band2;

    /** Source Image array */
    private static RenderedImage[] sourceIMG;

    /**
     * 2-D array containing the results of all the selected statistics calculated manually, the second dimension takes in account if NO DATA or ROI
     * are used
     */
    private static double[][] calculations;

    /** Array containing the number of all samples, every entry indicates takes in account if ROI or No Data */
    private static long[] numSamples;

    // Initial static method for preparing all the test data
    @BeforeClass
    public static void initialSetup() {
        // Subsampling params definitions
        xPeriod = 1;

        yPeriod = 1;

        // ROI creation
        Rectangle roiBounds = new Rectangle(5, 5, DEFAULT_WIDTH / 4, DEFAULT_HEIGHT / 4);
        roi = new ROIShape(roiBounds);

        // Range creation

        // No Data values
        noDataB = 50;
        noDataU = 50;
        noDataS = 50;
        noDataI = 50;
        noDataF = 50;
        noDataD = 50;

        boolean minIncluded = true;
        boolean maxIncluded = true;

        noDataByte = RangeFactory.create(noDataB, minIncluded, noDataB, maxIncluded);
        noDataUShort = RangeFactory.createU(noDataU, minIncluded, noDataU, maxIncluded);
        noDataShort = RangeFactory.create(noDataS, minIncluded, noDataS, maxIncluded);
        noDataInt = RangeFactory.create(noDataI, minIncluded, noDataI, maxIncluded);
        noDataFloat = RangeFactory.create(noDataF, minIncluded, noDataF, maxIncluded, true);
        noDataDouble = RangeFactory.create(noDataD, minIncluded, noDataD, maxIncluded, true);

        // Statistics types
        stats = new StatsType[] { StatsType.MEAN, StatsType.SUM, StatsType.MAX, StatsType.MIN,
                StatsType.EXTREMA, StatsType.VARIANCE, StatsType.DEV_STD };

        // Band array creation
        int[] array1Band = { 0 };
        int[] array2Band = { 0, 2 };

        band1 = array1Band;
        band2 = array2Band;

        // Image creations
        sourceIMG = new RenderedImage[6];

        sourceIMG[0] = createTestImage(DataBuffer.TYPE_BYTE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataB, false);
        sourceIMG[1] = createTestImage(DataBuffer.TYPE_USHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataU, false);
        sourceIMG[2] = createTestImage(DataBuffer.TYPE_SHORT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataS, false);
        sourceIMG[3] = createTestImage(DataBuffer.TYPE_INT, DEFAULT_WIDTH, DEFAULT_HEIGHT, noDataI,
                false);
        sourceIMG[4] = createTestImage(DataBuffer.TYPE_FLOAT, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataF, false);
        sourceIMG[5] = createTestImage(DataBuffer.TYPE_DOUBLE, DEFAULT_WIDTH, DEFAULT_HEIGHT,
                noDataD, false);

        // Statistical calculation
        numSamples = new long[4];
        calculations = new double[7][4];

        int minTileX = sourceIMG[0].getMinTileX();
        int minTileY = sourceIMG[0].getMinTileY();
        int maxTileX = minTileX + sourceIMG[0].getNumXTiles();
        int maxTileY = minTileY + sourceIMG[0].getNumYTiles();

        calculations[2][0] = Double.NEGATIVE_INFINITY;
        calculations[2][1] = Double.NEGATIVE_INFINITY;
        calculations[2][2] = Double.NEGATIVE_INFINITY;
        calculations[2][3] = Double.NEGATIVE_INFINITY;

        calculations[3][0] = Double.POSITIVE_INFINITY;
        calculations[3][1] = Double.POSITIVE_INFINITY;
        calculations[3][2] = Double.POSITIVE_INFINITY;
        calculations[3][3] = Double.POSITIVE_INFINITY;

        // Cycle on all pixels of the 6 images
        for (int i = minTileX; i < maxTileX; i++) {
            for (int j = minTileY; j < maxTileY; j++) {
                // Selection of a Raster
                Raster arrayRas = sourceIMG[0].getTile(i, j);

                int minX = arrayRas.getMinX();
                int minY = arrayRas.getMinY();
                int maxX = minX + arrayRas.getWidth();
                int maxY = minY + arrayRas.getHeight();
                // Cycle on the Raster pixels
                for (int x = minX; x < maxX; x++) {
                    for (int y = minY; y < maxY; y++) {
                        byte value = (byte) arrayRas.getSample(x, y, 0);
                        // Cycle for all the 4 cases: No roi and No NoData, only roi, only NoData, both roi and NoData
                        for (int z = 0; z < 4; z++) {
                            switch (z) {
                            case 0:
                                // update of the number of samples
                                numSamples[z]++;
                                // update of the sum of samples
                                calculations[1][z] += value;
                                // update of the squared sum of samples
                                calculations[4][z] += value * value;
                                // update of the minimum and maximum values
                                if (value > calculations[2][z]) {
                                    calculations[2][z] = value;
                                }
                                if (value < calculations[3][z]) {
                                    calculations[3][z] = value;
                                }
                                break;
                            case 1:
                                if (roi.contains(x, y)) {
                                    numSamples[z]++;
                                    calculations[1][z] += value;
                                    calculations[4][z] += value * value;
                                    if (value > calculations[2][z]) {
                                        calculations[2][z] = value;
                                    }
                                    if (value < calculations[3][z]) {
                                        calculations[3][z] = value;
                                    }
                                }
                                break;
                            case 2:
                                if (!noDataByte.contains(value)) {
                                    numSamples[z]++;
                                    calculations[1][z] += value;
                                    calculations[4][z] += value * value;
                                    if (value > calculations[2][z]) {
                                        calculations[2][z] = value;
                                    }
                                    if (value < calculations[3][z]) {
                                        calculations[3][z] = value;
                                    }
                                }
                                break;
                            case 3:
                                if (!noDataByte.contains(value) && roi.contains(x, y)) {
                                    numSamples[z]++;
                                    calculations[1][z] += value;
                                    calculations[4][z] += value * value;
                                    if (value > calculations[2][z]) {
                                        calculations[2][z] = value;
                                    }
                                    if (value < calculations[3][z]) {
                                        calculations[3][z] = value;
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        // Final calculation
        for (int h = 0; h < 6; h++) {
            for (int z = 0; z < 4; z++) {
                // calculation of the mean
                calculations[0][z] = calculations[1][z] / (numSamples[z] - 1);
                // Calculation of the variance
                calculations[5][z] = (calculations[4][z] - (calculations[1][z] * calculations[1][z])
                        / (numSamples[z]))
                        / (numSamples[z] - 1);
                // Calculation of the standard deviation
                calculations[6][z] = Math.sqrt(calculations[5][z]);
            }
        }

    }

    // This test checks if the statistics are correct in absence of No Data and ROI
    @Test
    public void testNoRangeNoRoi() {
        boolean roiUsed = false;
        boolean noDataRangeUsed = false;
        boolean useROIAccessor = false;

        // Byte data Type
        testStatistics(sourceIMG[0], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Ushort data Type
        testStatistics(sourceIMG[1], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Short data Type
        testStatistics(sourceIMG[2], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Integer data Type
        testStatistics(sourceIMG[3], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Float data Type
        testStatistics(sourceIMG[4], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Double data Type
        testStatistics(sourceIMG[5], band1, roiUsed, noDataRangeUsed, useROIAccessor);

        // Byte data Type
        testStatistics(sourceIMG[0], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Ushort data Type
        testStatistics(sourceIMG[1], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Short data Type
        testStatistics(sourceIMG[2], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Integer data Type
        testStatistics(sourceIMG[3], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Float data Type
        testStatistics(sourceIMG[4], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Double data Type
        testStatistics(sourceIMG[5], band2, roiUsed, noDataRangeUsed, useROIAccessor);
    }

    // This test checks if the statistics are correct in absence of No Data but with ROI (ROI RasterAccessor not used)
    @Test
    public void testRoiBounds() {
        boolean roiUsed = true;
        boolean noDataRangeUsed = false;
        boolean useROIAccessor = false;

        // Byte data Type
        testStatistics(sourceIMG[0], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Ushort data Type
        testStatistics(sourceIMG[1], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Short data Type
        testStatistics(sourceIMG[2], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Integer data Type
        testStatistics(sourceIMG[3], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Float data Type
        testStatistics(sourceIMG[4], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Double data Type
        testStatistics(sourceIMG[5], band1, roiUsed, noDataRangeUsed, useROIAccessor);

        // Byte data Type
        testStatistics(sourceIMG[0], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Ushort data Type
        testStatistics(sourceIMG[1], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Short data Type
        testStatistics(sourceIMG[2], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Integer data Type
        testStatistics(sourceIMG[3], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Float data Type
        testStatistics(sourceIMG[4], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Double data Type
        testStatistics(sourceIMG[5], band2, roiUsed, noDataRangeUsed, useROIAccessor);
    }

    // This test checks if the statistics are correct in absence of No Data but with ROI (ROI RasterAccessor used)
    @Test
    public void testRoiAccessor() {
        boolean roiUsed = true;
        boolean noDataRangeUsed = false;
        boolean useROIAccessor = true;

        // Byte data Type
        testStatistics(sourceIMG[0], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Ushort data Type
        testStatistics(sourceIMG[1], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Short data Type
        testStatistics(sourceIMG[2], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Integer data Type
        testStatistics(sourceIMG[3], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Float data Type
        testStatistics(sourceIMG[4], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Double data Type
        testStatistics(sourceIMG[5], band1, roiUsed, noDataRangeUsed, useROIAccessor);

        // Byte data Type
        testStatistics(sourceIMG[0], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Ushort data Type
        testStatistics(sourceIMG[1], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Short data Type
        testStatistics(sourceIMG[2], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Integer data Type
        testStatistics(sourceIMG[3], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Float data Type
        testStatistics(sourceIMG[4], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Double data Type
        testStatistics(sourceIMG[5], band2, roiUsed, noDataRangeUsed, useROIAccessor);
    }

    // This test checks if the statistics are correct in absence of ROI but with No Data
    @Test
    public void testNoData() {
        boolean roiUsed = false;
        boolean noDataRangeUsed = true;
        boolean useROIAccessor = false;

        // Byte data Type
        testStatistics(sourceIMG[0], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Ushort data Type
        testStatistics(sourceIMG[1], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Short data Type
        testStatistics(sourceIMG[2], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Integer data Type
        testStatistics(sourceIMG[3], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Float data Type
        testStatistics(sourceIMG[4], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Double data Type
        testStatistics(sourceIMG[5], band1, roiUsed, noDataRangeUsed, useROIAccessor);

        // Byte data Type
        testStatistics(sourceIMG[0], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Ushort data Type
        testStatistics(sourceIMG[1], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Short data Type
        testStatistics(sourceIMG[2], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Integer data Type
        testStatistics(sourceIMG[3], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Float data Type
        testStatistics(sourceIMG[4], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Double data Type
        testStatistics(sourceIMG[5], band2, roiUsed, noDataRangeUsed, useROIAccessor);
    }

    // This test checks if the statistics are correct in presence of No Data and ROI (ROI RasterAccessor not used)
    @Test
    public void testRoiNoData() {
        boolean roiUsed = true;
        boolean noDataRangeUsed = true;
        boolean useROIAccessor = false;

        // Byte data Type
        testStatistics(sourceIMG[0], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Ushort data Type
        testStatistics(sourceIMG[1], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Short data Type
        testStatistics(sourceIMG[2], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Integer data Type
        testStatistics(sourceIMG[3], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Float data Type
        testStatistics(sourceIMG[4], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Double data Type
        testStatistics(sourceIMG[5], band1, roiUsed, noDataRangeUsed, useROIAccessor);

        // Byte data Type
        testStatistics(sourceIMG[0], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Ushort data Type
        testStatistics(sourceIMG[1], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Short data Type
        testStatistics(sourceIMG[2], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Integer data Type
        testStatistics(sourceIMG[3], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Float data Type
        testStatistics(sourceIMG[4], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Double data Type
        testStatistics(sourceIMG[5], band2, roiUsed, noDataRangeUsed, useROIAccessor);
    }

    // This test checks if the statistics are correct in presence of No Data and ROI (ROI RasterAccessor used)
    @Test
    public void testRoiAccessorNoData() {
        boolean roiUsed = true;
        boolean noDataRangeUsed = true;
        boolean useROIAccessor = true;

        // Byte data Type
        testStatistics(sourceIMG[0], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Ushort data Type
        testStatistics(sourceIMG[1], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Short data Type
        testStatistics(sourceIMG[2], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Integer data Type
        testStatistics(sourceIMG[3], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Float data Type
        testStatistics(sourceIMG[4], band1, roiUsed, noDataRangeUsed, useROIAccessor);
        // Double data Type
        testStatistics(sourceIMG[5], band1, roiUsed, noDataRangeUsed, useROIAccessor);

        // Byte data Type
        testStatistics(sourceIMG[0], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Ushort data Type
        testStatistics(sourceIMG[1], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Short data Type
        testStatistics(sourceIMG[2], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Integer data Type
        testStatistics(sourceIMG[3], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Float data Type
        testStatistics(sourceIMG[4], band2, roiUsed, noDataRangeUsed, useROIAccessor);
        // Double data Type
        testStatistics(sourceIMG[5], band2, roiUsed, noDataRangeUsed, useROIAccessor);
    }

    // This method calculates the statistics with the SimpleStatsOpImage and then compares them with the already calculated values.
    public void testStatistics(RenderedImage source, int[] bands, boolean roiUsed,
            boolean noDataRangeUsed, boolean useRoiAccessor) {
        // The precalculated roi is used, if selected by the related boolean.
        ROI roiData;

        if (roiUsed) {
            roiData = roi;
        } else {
            roiData = null;
        }

        // The precalculated NoData Range is used, if selected by the related boolean.
        Range noDataRange;

        if (noDataRangeUsed) {
            int dataType = source.getSampleModel().getDataType();
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                noDataRange = noDataByte;
                break;
            case DataBuffer.TYPE_USHORT:
                noDataRange = noDataUShort;
                break;
            case DataBuffer.TYPE_SHORT:
                noDataRange = noDataShort;
                break;
            case DataBuffer.TYPE_INT:
                noDataRange = noDataInt;
                break;
            case DataBuffer.TYPE_FLOAT:
                noDataRange = noDataFloat;
                break;
            case DataBuffer.TYPE_DOUBLE:
                noDataRange = noDataDouble;
                break;
            default:
                throw new IllegalArgumentException("Wrong data type");
            }
        } else {
            noDataRange = null;
        }
        // Index used for indicating which index of the "calculation" array must be taken for reading the precalculated statistic values
        int statsIndex = 0;

        if (roiUsed && noDataRangeUsed) {
            statsIndex = 3;
        } else if (roiUsed) {
            statsIndex = 1;
        } else if (noDataRangeUsed) {
            statsIndex = 2;
        }

        // Image creation
        RenderedImage destination = SimpleStatsDescriptor.create(source, xPeriod, yPeriod, roiData,
                noDataRange, useRoiAccessor, bands, stats, null);
        // Statistic calculation
        Statistics[][] result = (Statistics[][]) destination
                .getProperty(Statistics.SIMPLE_STATS_PROPERTY);

        // Control only band 0
        Statistics[] stats0 = result[0];

        // Test if the calculated values are equal with a tolerance value
        for (int i = 0; i < stats0.length; i++) {
            Statistics stat = stats0[i];
            switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 5:
            case 6:
                double value = (Double) stat.getResult();
                assertEquals(calculations[i][statsIndex], value, TOLERANCE);
                break;
            case 4:
                double[] extrema = (double[]) stat.getResult();
                double max = extrema[1];
                double min = extrema[0];
                assertEquals(calculations[2][statsIndex], max, TOLERANCE);
                assertEquals(calculations[3][statsIndex], min, TOLERANCE);
                break;
            }
        }

    }

    @Override
    protected void testGlobal(boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImage(int dataType,
            T noDataValue, boolean useROIAccessor, boolean isBinary, boolean bicubic2Disabled,
            boolean noDataRangeUsed, boolean roiPresent, InterpolationType interpType,
            TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

    @Override
    protected <T extends Number & Comparable<? super T>> void testImageAffine(
            RenderedImage sourceImage, int dataType, T noDataValue, boolean useROIAccessor,
            boolean isBinary, boolean bicubic2Disabled, boolean noDataRangeUsed,
            boolean roiPresent, boolean setDestinationNoData, TransformationType transformType,
            InterpolationType interpType, TestSelection testSelect, ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }

    @Override
    protected void testGlobalAffine(boolean useROIAccessor, boolean isBinary,
            boolean bicubic2Disabled, boolean noDataRangeUsed, boolean roiPresent,
            boolean setDestinationNoData, InterpolationType interpType, TestSelection testSelect,
            ScaleType scaleValue) {
        throw new UnsupportedOperationException("Operation not supported in this test class");
    }
}
