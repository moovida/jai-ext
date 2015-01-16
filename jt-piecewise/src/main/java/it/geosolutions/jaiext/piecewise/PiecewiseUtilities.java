/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package it.geosolutions.jaiext.piecewise;

import it.geosolutions.jaiext.range.Range;

import java.text.ChoiceFormat;
import java.util.Arrays;

/**
 * Convenience class to group utilities methods for {@link DomainElement1D} and
 * {@link Domain1D} implmentations.
 * 
 * @author Simone Giannecchini, GeoSolutions.
 * 
 */
class PiecewiseUtilities {

	/**
	 * 
	 */
	private PiecewiseUtilities() {
	}

	/**
	 * Checks whether or not two DomainElement1Ds input range overlaps
	 * 
	 * @param domainElements
	 *            to be checked
	 * @param idx
	 *            index to start with
	 */
	static void domainElementsOverlap(DomainElement1D[] domainElements, int idx) {
		// Two domain elements have overlapping ranges;
		// Format an error message...............
		final Range range1 = domainElements[idx - 1].getRange();
		final Range range2 = domainElements[idx].getRange();
		final Number[] args = new Number[] { range1.getMin(), range1.getMax(),
				range2.getMin(), range2.getMax() };
		String[] results = new String[4];
		for (int j = 0; j < args.length; j++) {
			final double value = (args[j]).doubleValue();
			if (Double.isNaN(value)) {
				String hex = Long
						.toHexString(Double.doubleToRawLongBits(value));
				results[j] = "NaN(" + hex + ')';
			} else {
				results[j] = value + "";
			}

		}
		throw new IllegalArgumentException("Provided ranges are overlapping:"
				+ results[0] + " : " + results[1] + " / " + results[2] + " : "
				+ results[3]);
	}

	/**
	 * Makes sure that an argument is non-null.
	 * 
	 * @param name
	 *            Argument name.
	 * @param object
	 *            User argument.
	 * @throws IllegalArgumentException
	 *             if {@code object} is null.
	 */
	static void ensureNonNull(final String name, final Object object)
			throws IllegalArgumentException {
		if (object == null) {
			throw new IllegalArgumentException("Input object is null");
		}
	}

	/**
	 * Effectue une recherche bi-lin�aire de la valeur sp�cifi�e. Cette
	 * m�thode est semblable � {@link Arrays#binarySearch(double[],double)}
	 * , except� qu'elle peut distinguer diff�rentes valeurs de NaN.
	 * 
	 * Note: This method is not private in order to allows testing by {@link }.
	 */
	static int binarySearch(final double[] array, final double val) {
		int low = 0;
		int high = array.length - 1;
		final boolean keyIsNaN = Double.isNaN(val);
		while (low <= high) {
			final int mid = (low + high) >> 1;
			final double midVal = array[mid];
			if (midVal < val) { // Neither val is NaN, midVal is smaller
				low = mid + 1;
				continue;
			}
			if (midVal > val) { // Neither val is NaN, midVal is larger
				high = mid - 1;
				continue;
			}
			/*
			 * The following is an adaptation of evaluator's comments for bug
			 * #4471414
			 * (http://developer.java.sun.com/developer/bugParade/bugs/4471414.
			 * html). Extract from evaluator's comment:
			 * 
			 * [This] code is not guaranteed to give the desired results because
			 * of laxity in IEEE 754 regarding NaN values. There are actually
			 * two types of NaNs, signaling NaNs and quiet NaNs. Java doesn't
			 * support the features necessary to reliably distinguish the two.
			 * However, the relevant point is that copying a signaling NaN may
			 * (or may not, at the implementors discretion) yield a quiet NaN --
			 * a NaN with a different bit pattern (IEEE 754 6.2). Therefore, on
			 * IEEE 754 compliant platforms it may be impossible to find a
			 * signaling NaN stored in an array since a signaling NaN passed as
			 * an argument to binarySearch may get replaced by a quiet NaN.
			 */
			final long midRawBits = Double.doubleToRawLongBits(midVal);
			final long keyRawBits = Double.doubleToRawLongBits(val);
			if (midRawBits == keyRawBits) {
				return mid; // key found
			}
			final boolean midIsNaN = Double.isNaN(midVal);
			final boolean adjustLow;
			if (keyIsNaN) {
				// If (mid,key)==(!NaN, NaN): mid is lower.
				// If two NaN arguments, compare NaN bits.
				adjustLow = (!midIsNaN || midRawBits < keyRawBits);
			} else {
				// If (mid,key)==(NaN, !NaN): mid is greater.
				// Otherwise, case for (-0.0, 0.0) and (0.0, -0.0).
				adjustLow = (!midIsNaN && midRawBits < keyRawBits);
			}
			if (adjustLow)
				low = mid + 1;
			else
				high = mid - 1;
		}
		return -(low + 1); // key not found.
	}

	/**
	 * Compare deux valeurs de type {@code double}. Cette m�thode est
	 * similaire � {@link Double#compare(double,double)}, except� qu'elle
	 * ordonne aussi les diff�rentes valeurs NaN.
	 */
	static int compare(final double v1, final double v2) {
		if (Double.isNaN(v1) && Double.isNaN(v2)) {
			final long bits1 = Double.doubleToRawLongBits(v1);
			final long bits2 = Double.doubleToRawLongBits(v2);
			if (bits1 < bits2)
				return -1;
			if (bits1 > bits2)
				return +1;
		}
		return Double.compare(v1, v2);
	}

	/**
	 * V�rifie si le tableau de cat�gories sp�cifi� est bien en ordre
	 * croissant. La comparaison ne tient pas compte des valeurs {@code NaN}.
	 * Cette m�thode n'est utilis�e que pour les {@code assert}.
	 */
	static boolean isSorted(final DefaultDomainElement1D[] domains) {
		if (domains == null)
			return true;
		for (int i = 1; i < domains.length; i++) {
			final DefaultDomainElement1D d1 = domains[i];
			assert !(d1.getInputMinimum() > d1.getInputMaximum()) : d1;
			final DefaultDomainElement1D d0 = domains[i - 1];
			assert !(d0.getInputMinimum() > d0.getInputMaximum()) : d0;
			if (compare(d0.getInputMaximum(), d1.getInputMinimum()) > 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns a {@code double} value for the specified number. If
	 * {@code direction} is non-zero, then this method will returns the closest
	 * representable number of type {@code type} before or after the double
	 * value.
	 * 
	 * @param type
	 *            The range element class. {@code number} must be an instance of
	 *            this class (this will not be checked).
	 * @param number
	 *            The number to transform to a {@code double} value.
	 * @param direction
	 *            -1 to return the previous representable number, +1 to return
	 *            the next representable number, or 0 to return the number with
	 *            no change.
	 */
	static double doubleValue(final Class<? extends Number> type,
			final Number number, final int direction) {
		assert (direction >= -1) && (direction <= +1) : direction;
		return rool(type, number.doubleValue(), direction);
	}

	/**
	 * Returns a linear transform with the supplied scale and offset values.
	 * 
	 * @param scale
	 *            The scale factor. May be 0 for a constant transform.
	 * @param offset
	 *            The offset value. May be NaN.
	 */
	static MathTransformation createLinearTransform1D(final double scale,
			final double offset) {
		return SingleDimensionTransformation.create(scale, offset);
	}

	/**
	 * Create a linear transform mapping values from {@code sampleValueRange} to
	 * {@code geophysicsValueRange}.
	 */
	static MathTransformation createLinearTransform1D(final Range sourceRange,
			final Range destinationRange) {
		final Class<? extends Number> sType = sourceRange.getDataType()
				.getClassValue();
		final Class<? extends Number> dType = destinationRange.getDataType()
				.getClassValue();
		/*
		 * First, find the direction of the adjustment to apply to the ranges if
		 * we wanted all values to be inclusives. Then, check if the adjustment
		 * is really needed: if the values of both ranges are inclusive or
		 * exclusive, then there is no need for an adjustment before computing
		 * the coefficient of a linear relation.
		 */
		int sMinInc = sourceRange.isMinIncluded() ? 0 : +1;
		int sMaxInc = sourceRange.isMaxIncluded() ? 0 : -1;
		int dMinInc = destinationRange.isMinIncluded() ? 0 : +1;
		int dMaxInc = destinationRange.isMaxIncluded() ? 0 : -1;

		/*
		 * Now, extracts the minimal and maximal values and computes the linear
		 * coefficients.
		 */
		final double minSource = doubleValue(sType, sourceRange.getMin(),
				sMinInc);
		final double maxSource = doubleValue(sType, sourceRange.getMax(),
				sMaxInc);
		final double minDestination = doubleValue(dType,
				destinationRange.getMin(), dMinInc);
		final double maxDestination = doubleValue(dType,
				destinationRange.getMax(), dMaxInc);

		// /////////////////////////////////////////////////////////////////
		//
		// optimizations
		//
		// /////////////////////////////////////////////////////////////////
		// //
		//
		// If the output range is a single value let's create a constant
		// transform
		//
		// //
		if (PiecewiseUtilities.compare(minDestination, maxDestination) == 0)
			return SingleDimensionTransformation.create(0, minDestination);

		// //
		//
		// If the input range is a single value this transform ca be created
		// only if we map to another single value
		//
		// //
		if (PiecewiseUtilities.compare(minSource, maxSource) == 0)
			throw new IllegalArgumentException(
					"Impossible to map a single value to a range.");

		double scale = (maxDestination - minDestination)
				/ (maxSource - minSource);
		// /////////////////////////////////////////////////////////////////
		//
		// Take into account the fact that the maxSample and the minSample can
		// be
		// similar hence we have a constant transformation.
		//
		// /////////////////////////////////////////////////////////////////
		if (Double.isNaN(scale))
			scale = 0;
		final double offset = minDestination - scale * minSource;
		return createLinearTransform1D(scale, offset);
	}

	/**
	 * Returns a hash code for the specified object, which may be an array. This
	 * method returns one of the following values:
	 * <p>
	 * <ul>
	 * <li>If the supplied object is {@code null}, then this method returns 0.</li>
	 * <li>Otherwise if the object is an array of objects, then
	 * {@link Arrays#deepHashCode(Object[])} is invoked.</li>
	 * <li>Otherwise if the object is an array of primitive type, then the
	 * corresponding {@link Arrays#hashCode(double[]) Arrays.hashCode(...)}
	 * method is invoked.</li>
	 * <li>Otherwise {@link Object#hashCode()} is invoked.
	 * <li>
	 * </ul>
	 * <p>
	 * This method should be invoked <strong>only</strong> if the object type is
	 * declared exactly as {@code Object}, not as some subtype like
	 * {@code Object[]}, {@code String} or {@code float[]}. In the later cases,
	 * use the appropriate {@link Arrays} method instead.
	 * 
	 * @param object
	 *            The object to compute hash code. May be {@code null}.
	 * @return The hash code of the given object.
	 */
	public static int deepHashCode(final Object object) {
		if (object == null) {
			return 0;
		}
		if (object instanceof Object[]) {
			return Arrays.deepHashCode((Object[]) object);
		}
		if (object instanceof double[]) {
			return Arrays.hashCode((double[]) object);
		}
		if (object instanceof float[]) {
			return Arrays.hashCode((float[]) object);
		}
		if (object instanceof long[]) {
			return Arrays.hashCode((long[]) object);
		}
		if (object instanceof int[]) {
			return Arrays.hashCode((int[]) object);
		}
		if (object instanceof short[]) {
			return Arrays.hashCode((short[]) object);
		}
		if (object instanceof byte[]) {
			return Arrays.hashCode((byte[]) object);
		}
		if (object instanceof char[]) {
			return Arrays.hashCode((char[]) object);
		}
		if (object instanceof boolean[]) {
			return Arrays.hashCode((boolean[]) object);
		}
		return object.hashCode();
	}

	/**
	 * A prime number used for hash code computation.
	 */
	private static final int PRIME_NUMBER = 37;

	/**
	 * Alters the given seed with the hash code value computed from the given
	 * value. The givan object may be null. This method do <strong>not</strong>
	 * iterates recursively in array elements. If array needs to be hashed, use
	 * one of {@link Arrays} method or {@link #deepHashCode deepHashCode}
	 * instead.
	 * <p>
	 * <b>Note on assertions:</b> There is no way to ensure at compile time that
	 * this method is not invoked with an array argument, while doing so would
	 * usually be a program error. Performing a systematic argument check would
	 * impose a useless overhead for correctly implemented
	 * {@link Object#hashCode} methods. As a compromise we perform this check at
	 * runtime only if assertions are enabled. Using assertions for argument
	 * check in a public API is usually a deprecated practice, but we make an
	 * exception for this particular method.
	 * 
	 * @param value
	 *            The value whose hash code to compute, or {@code null}.
	 * @param seed
	 *            The hash code value computed so far. If this method is invoked
	 *            for the first field, then any arbitrary value (preferably
	 *            different for each class) is okay.
	 * @return An updated hash code value.
	 * @throws AssertionError
	 *             If assertions are enabled and the given value is an array.
	 */
	public static int hash(Object value, int seed) throws AssertionError {
		seed *= PRIME_NUMBER;
		if (value != null) {
			assert !value.getClass().isArray() : value;
			seed += value.hashCode();
		}
		return seed;
	}

	/**
	 * Returns {@code true} if the given doubles are equals. Positive and
	 * negative zero are considered different, while a NaN value is considered
	 * equal to other NaN values.
	 * 
	 * @param o1
	 *            The first value to compare.
	 * @param o2
	 *            The second value to compare.
	 * @return {@code true} if both values are equal.
	 * 
	 * @see Double#equals
	 */
	public static boolean equals(double o1, double o2) {
		if (Double.doubleToLongBits(o1) == Double.doubleToLongBits(o2))
			return true;

		double tol = getTolerance();
		final double min = o1 - Math.signum(o1) * o1 * tol;
		final double max = o1 + Math.signum(o1) * o1 * tol;
		return min <= o2 && o2 <= max;
	}

	public static boolean equals(
			it.geosolutions.jaiext.piecewise.MathTransformation inverse,
			it.geosolutions.jaiext.piecewise.MathTransformation inverse2) {
		return true;
	}

	/**
	 * Returns the next or previous representable number. If {@code amount} is
	 * equals to {@code 0}, then this method returns the {@code value}
	 * unchanged. Otherwise, The operation performed depends on the specified
	 * {@code type}:
	 * <ul>
	 * <li>
	 * <p>
	 * If the {@code type} is {@link Double}, then this method is equivalent to
	 * invoking {@link #previous(double)} if {@code amount} is equals to
	 * {@code -1}, or invoking {@link #next(double)} if {@code amount} is equals
	 * to {@code +1}. If {@code amount} is smaller than {@code -1} or greater
	 * than {@code +1}, then this method invokes {@link #previous(double)} or
	 * {@link #next(double)} in a loop for {@code abs(amount)} times.
	 * </p>
	 * </li>
	 * 
	 * <li>
	 * <p>
	 * If the {@code type} is {@link Float}, then this method is equivalent to
	 * invoking {@link #previous(float)} if {@code amount} is equals to
	 * {@code -1}, or invoking {@link #next(float)} if {@code amount} is equals
	 * to {@code +1}. If {@code amount} is smaller than {@code -1} or greater
	 * than {@code +1}, then this method invokes {@link #previous(float)} or
	 * {@link #next(float)} in a loop for {@code abs(amount)} times.
	 * </p>
	 * </li>
	 * 
	 * <li>
	 * <p>
	 * If the {@code type} is an {@linkplain #isInteger integer}, then invoking
	 * this method is equivalent to computing {@code value + amount}.
	 * </p>
	 * </li>
	 * </ul>
	 * 
	 * @param type
	 *            The type. Should be the class of {@link Double}, {@link Float}
	 *            , {@link Long}, {@link Integer}, {@link Short} or {@link Byte}
	 *            .
	 * @param value
	 *            The number to rool.
	 * @param amount
	 *            -1 to return the previous representable number, +1 to return
	 *            the next representable number, or 0 to return the number with
	 *            no change.
	 * @return One of previous or next representable number as a {@code double}.
	 * @throws IllegalArgumentException
	 *             if {@code type} is not one of supported types.
	 */
	public static double rool(final Class type, double value, int amount)
			throws IllegalArgumentException {
		if (Double.class.equals(type)) {
			if (amount < 0) {
				do {
					value = previous(value);
				} while (++amount != 0);
			} else if (amount != 0) {
				do {
					value = next(value);
				} while (--amount != 0);
			}
			return value;
		}
		if (Float.class.equals(type)) {
			float vf = (float) value;
			if (amount < 0) {
				do {
					vf = next(vf, false);
				} while (++amount != 0);
			} else if (amount != 0) {
				do {
					vf = next(vf, true);
				} while (--amount != 0);
			}
			return vf;
		}
		if (isInteger(type)) {
			return value + amount;
		}
		throw new IllegalArgumentException("Unsupported DataType: " + type);
	}

	/**
	 * Returns {@code true} if the specified {@code type} is one of integer
	 * types. Integer types includes {@link Long}, {@link Integer},
	 * {@link Short} and {@link Byte}.
	 * 
	 * @param type
	 *            The type to test (may be {@code null}).
	 * @return {@code true} if {@code type} is the class {@link Long},
	 *         {@link Integer}, {@link Short} or {@link Byte}.
	 * 
	 * @deprecated Moved to {@link Classes}.
	 */
	@Deprecated
	public static boolean isInteger(final Class<?> type) {
		return type != null && Long.class.equals(type)
				|| Integer.class.equals(type) || Short.class.equals(type)
				|| Byte.class.equals(type);
	}

	/**
	 * Finds the least double greater than <var>f</var>. If {@code NaN}, returns
	 * same value.
	 * 
	 * @see java.text.ChoiceFormat#nextDouble
	 * 
	 * @todo Remove this method when we will be allowed to use Java 6.
	 */
	public static double next(final double f) {
		return ChoiceFormat.nextDouble(f);
	}

	/**
	 * Finds the greatest double less than <var>f</var>. If {@code NaN}, returns
	 * same value.
	 * 
	 * @see java.text.ChoiceFormat#previousDouble
	 * 
	 * @todo Remove this method when we will be allowed to use Java 6.
	 */
	public static double previous(final double f) {
		return ChoiceFormat.previousDouble(f);
	}

	private static float next(final float f, final boolean positive) {
		final int SIGN = 0x80000000;
		final int POSITIVEINFINITY = 0x7F800000;

		// Filter out NaN's
		if (Float.isNaN(f)) {
			return f;
		}

		// Zero's are also a special case
		if (f == 0f) {
			final float smallestPositiveFloat = Float.intBitsToFloat(1);
			return (positive) ? smallestPositiveFloat : -smallestPositiveFloat;
		}

		// If entering here, d is a nonzero value.
		// Hold all bits in a int for later use.
		final int bits = Float.floatToIntBits(f);

		// Strip off the sign bit.
		int magnitude = bits & ~SIGN;

		// If next float away from zero, increase magnitude.
		// Else decrease magnitude
		if ((bits > 0) == positive) {
			if (magnitude != POSITIVEINFINITY) {
				magnitude++;
			}
		} else {
			magnitude--;
		}

		// Restore sign bit and return.
		final int signbit = bits & SIGN;
		return Float.intBitsToFloat(magnitude | signbit);
	}

	/**
	 * Gathers the tolerance for floating point comparisons
	 * 
	 * @return The tolerance set in the JVM properties, or its default value if
	 *         not set
	 */
	private static double getTolerance() {
		Double tol = Double.parseDouble(System
				.getProperty("jaiext.piecewise.tolerance"));
		if (tol == null) {
			return 0.0d;
		}
		return tol;
	}

	public static boolean equals(Range outputRange, Range outputRange2) {
		return outputRange.equals(outputRange2);
	}
}