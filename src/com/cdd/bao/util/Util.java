/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2018 Collaborative Drug Discovery Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2.0
 * as published by the Free Software Foundation:
 * 
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.cdd.bao.util;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.net.*;

/*
 * Static utilities to make life a little bit easier.
 */

public class Util
{
	/**
	 * Writes a string to the console, for debugging purposes.
	 * @param str string to write
	 */
	public static void write(String str) {System.out.print(str);}
	
	/**
	 * Writes a string to the console, and adds a carriage return.
	 * @param str string to write
	 */
	public static void writeln(String str) {System.out.println(str);}
	
	/**
	 * Writes a string representation of object to the console, and adds a carriage return.
	 * @param obj object to write
	 */
	public static void writeln(Object obj) {System.out.println(obj);}
	
	/**
	 * Writes a carriage return to the console.
	 */
	public static void writeln() {System.out.println();}
	
	/**
	 * Flushes the output console. This will display any lines that have not yet been terminated by a carriage return.
	 */
	public static void writeFlush() {System.out.flush();}
	
	/**
	 * Write text to output console, and flush immediately afterward.
	 */
	public static void writeFlush(String str) {System.out.print(str); System.out.flush();}
	
	/**
	 * Writes an error message to the console, with a formulated warning. In some environments, may later be swapped
	 * out to show a dialog box instead.
	 * @param msg message to show
	 */
	public static void errmsg(String msg) {System.out.println("*** ERROR: " + msg);}
	
	/**
	 * Displays an exception on the console, with an additional message. Similar to ex.printStackTrace(). In some
	 * environments, may later be swapped out to show a dialog box instead.
	 * @param msg message to show
	 * @param ex exception to format
	 */
	public static void errmsg(String msg, Exception ex)
	{
		System.out.println("*** ERROR: " + msg + "/" + ex.getMessage());
		ex.printStackTrace();
	}
	
	/**
	 * Convenience method for turning an exception into a formatted multiline string that includes the description
	 * and the entire stack trace.
	 * @param ex exception to formulate
	 * @return multiline string with full description
	 */
	public static String exToString(Exception ex)
	{
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
	/**
	 * Length of array, protected against nulls.
	 */
	public static int length(Object arr) {return arr == null ? 0 : Array.getLength(arr);}
	
	/**
	 * Converts a collection of Byte objects directly to a primitive array.
	 */
	public static byte[] primByte(Collection<Byte> coll) 
	{
		byte[] arr = new byte[coll.size()]; 
		int n = 0;
		for (byte v : coll) arr[n++] = v;
		return arr;
	}

	/**
	 * Converts a collection of Integer objects directly to a primitive array.
	 */
	public static int[] primInt(Collection<Integer> coll) 
	{
		int[] arr = new int[coll.size()];
		int n = 0;
		for (int v : coll) arr[n++] = v;
		return arr;
	}

	/**
	 * Converts a collection of Long objects directly to a primitive array.
	 */
	public static long[] primLong(Collection<Long> coll) 
	{
		long[] arr = new long[coll.size()];
		int n = 0;
		for (long v : coll) arr[n++] = v;
		return arr;
	}

	/**
	 * Converts a collection of Float objects directly to a primitive array.
	 */	
	public static float[] primFloat(Collection<Float> coll)
	{
		float[] arr = new float[coll.size()];
		int n = 0;
		for (float v : coll) arr[n++] = v;
		return arr;
	}

	/**
	 * Converts a collection of Double objects directly to a primitive array.
	 */
	public static double[] primDouble(Collection<Double> coll) 
	{
		double[] arr = new double[coll.size()];
		int n = 0;
		for (double v : coll) arr[n++] = v;
		return arr;
	}

	/**
	 * Converts a collection of Boolean objects directly to a primitive array.
	 */
	public static boolean[] primBoolean(Collection<Boolean> coll)
	{
		boolean[] arr = new boolean[coll.size()];
		int n = 0;
		for (boolean v : coll) arr[n++] = v;
		return arr;
	}

	/**
	 * Converts a collection of Character objects directly to a primitive array.
	 */
	public static char[] primCharacter(Collection<Character> coll) 
	{
		char[] arr = new char[coll.size()];
		int n = 0;
		for (char v : coll) arr[n++] = v;
		return arr;
	}

	/**
	 * Converts a collection of String objects directly to a primitive array.
	 */
	public static String[] primString(Collection<String> coll)
	{
		return coll.toArray(new String[0]);
	}

	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
	public static String arrayStr(int[] arr)
	{
		if (arr == null) return "{null}";
		String str = "";
		for (int n = 0; n < arr.length; n++) str += (n > 0 ? "," : "") + arr[n];
		return str;
	}
	
	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
	public static String arrayStr(long[] arr)
	{
		if (arr == null) return "{null}";
		String str = "";
		for (int n = 0; n < arr.length; n++) str += (n > 0 ? "," : "") + arr[n];
		return str;
	}
	
	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
	public static String arrayStr(float[] arr)
	{
		if (arr == null) return "{null}";
		String str = "";
		for (int n = 0; n < arr.length; n++) str += (n > 0 ? "," : "") + arr[n];
		return str;
	}
	
	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
	public static String arrayStr(double[] arr)
	{
		if (arr == null) return "{null}";
		String str = "";
		for (int n = 0; n < arr.length; n++) str += (n > 0 ? "," : "") + arr[n];
		return str;
	}
	
	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
	public static String arrayStr(String[] arr)
	{
		if (arr == null) return "{null}";
		StringJoiner sj = new StringJoiner("\",\"", "\"", "\"");
		for (String s : arr) sj.add(s);
		return sj.toString();
	}
	
	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
	public static String arrayStr(boolean[] arr)
	{
		if (arr == null) return "{null}";
		StringJoiner sj = new StringJoiner("", "{", "}");
		for (boolean b : arr) sj.add(b ? "1" : "0");
		return sj.toString();
	}
	
	/**
	 * Converts an array to a human-readable string, after having applied a multiplier.
	 * @param A array to format
	 * @param mul modifier to multiply by
	 */
	public static String arrayStr(float[] arr, float mul)
	{
		String str = "";
		for (int n = 0; n < arr.length; n++) str += (n > 0 ? "," : "") + (arr[n] * mul);
		return str;
	}
	
	/**
	 * Converts an array to a human-readable string, after having applied a multiplier.
	 * @param A array to format
	 * @param mul modifier to multiply by
	 */
	public static String arrayStr(double[] arr, double mul)
	{
		String str = "";
		for (int n = 0; n < arr.length; n++) str += (n > 0 ? "," : "") + (arr[n] * mul);
		return str;
	}
	
	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
	public static String arrayStr(int[][] arr) 
	{
		if (arr == null) return "{null}";
		String str = "";
		for (int n = 0; n < arr.length; n++) str += arr[n] == null ? "{null}" : "{" + arrayStr(arr[n]) + "}";
		return str;
	}
	
	/**
	 * Joins an array of strings together, in a reasonably performant way.
	 * @param list strings to join
	 * @param sep separator to insert between each two strings
	 * @return joined string, or null if the input list is null/0-length
	 */
	public static String join(String[] list, String sep)
	{
		if (list == null || list.length == 0) return null;
		if (list.length == 1) return list[0];
		if (list.length == 2) return list[0] + sep + list[1];
		StringJoiner sj = new StringJoiner(sep);
		for (String s : list) sj.add(s);
		return sj.toString();
	}
	
	/**
	 * Joins an array of integers together to make a string, in a reasonably performant way.
	 * @param list integers to join
	 * @param sep separator to insert between each two strings
	 * @return joined string, or null if the input list is null/0-length
	 */
	public static String join(int[] list, String sep)
	{
		if (list == null || list.length == 0) return null;
		if (list.length == 1) return String.valueOf(list[0]);
		if (list.length == 2) return list[0] + sep + list[1];
		StringBuilder buff = new StringBuilder(String.valueOf(list[0]));
		for (int n = 1; n < list.length; n++)
		{
			buff.append(sep);
			buff.append(String.valueOf(list[n]));
		}
		return buff.toString();
	}
	
	/**
	 * Returns true if the given string represents a parseable numeric value, either integer or floating point.
	 */
	public static boolean isNumeric(String str)
	{
		NumberFormat fmt = NumberFormat.getInstance();
		ParsePosition pos = new ParsePosition(0);
		fmt.parse(str, pos);
		return str.length() == pos.getIndex();
	}
	
	/**
	 * Converts a string into an integer, without the annoyance of having to trap an exception
	 * @param str string to convert
	 * @param def default value to return in case it is not a valid number
	 */
	public static int safeInt(String str, int def) 
	{
		if (str == null) return def;
		try {return Integer.parseInt(str);} 
		catch (NumberFormatException e) {return def;}
	}
	
	/**
	 * Converts a string into an integer, or returns zero if it is invalid, rather than throwing an exception.
	 * @param str string to convert
	 */
	public static int safeInt(String str) {return safeInt(str, 0);}
	
	/**
	 * Converts a string into a long integer, without the annoyance of having to trap an exception
	 * @param str string to convert
	 * @param def default value to return in case it is not a valid number
	 */
	public static long safeLong(String str, long def) 
	{
		if (str == null) return def;
		try {return Long.parseLong(str);}
		catch (NumberFormatException e) {return def;}
	}
	
	/**
	 * Converts a string into a long integer, or returns zero if it is invalid, rather than throwing an exception.
	 * @param str string to convert
	 */
	public static long safeLong(String str) {return safeLong(str, 0);}
	
	/**
	 * Converts a string into a float, without the annoyance of having to trap an exception
	 * @param str string to convert
	 * @param def default value to return in case it is not a valid number
	 */
	public static float safeFloat(String str, float def) 
	{
		if (str == null) return def;
		try {return Float.parseFloat(str);} 
		catch (NumberFormatException e) {return def;}
	}
	
	/**
	 * Converts a string into a float, or returns zero if it is invalid, rather than throwing an exception.
	 * @param str string to convert
	 */
	public static float safeFloat(String str) {return safeFloat(str, 0);}
	
	/**
	 * Converts a string into a double, without the annoyance of having to trap an exception
	 * @param str string to convert
	 * @param def default value to return in case it is not a valid number
	 */
	public static double safeDouble(String str, double def) 
	{
		if (str == null) return def;
		try {return Double.parseDouble(str);} 
		catch (NumberFormatException e) {return def;}
	}
	
	/**
	 * Converts a string into a double, or returns zero if it is invalid, rather than throwing an exception.
	 * @param str string to convert
	 */
	public static double safeDouble(String str) {return safeDouble(str, 0);}
	
	/**
	 * Returns the value of the string, or a blank string if the parameter is null.
	 */
	public static String safeString(String str) {return str == null ? "" : str;}
	
	/**
	 * Returns the value of the string, or null if the string was blank.
	 */
	public static String nullOrString(String str) {return str == null || str.length() == 0 ? null : str;}
	
	/**
	 * Returns true if the string is either blank or null.
	 */
	public static boolean isBlank(String str) {return str == null || str.length() == 0;}
	
	/**
	 * Returns true if the string is neither blank nor null.
	 */
	public static boolean notBlank(String str) {return str != null && str.length() > 0;}
	
	/**
	 * Returns true if the two strings are equal, whereby null strings and blank strings are considered the same.
	 */
	public static boolean equals(String s1, String s2) {return safeString(s1).equals(safeString(s2));}
	
	/**
	 * Renders the given double floating point value into a string, in a way that is similar to the C function
	 * sprintf("%g",val), which avoids use of scientific notation.
	 */
	public static String formatDouble(double val)
	{
		return String.format("%.17g", val).replaceFirst("\\.?0+(e|$)", "$1");
	}
	
	/**
	 * Renders the given double floating point value into a string in decimal notation, with a maximum number of
	 * significant figures shown. Trailing digits are truncated rather than rounded.
	 * @param val value to render
	 * @param maxSigFig maximum number of significant figures
	 */
	public static String formatDouble(double val, int maxSigFig)
	{
		String fmt = "%." + maxSigFig + "g";
		String str = String.format(fmt, val);
		if (str.indexOf('.') < 0) return str;
		if (str.indexOf('e') < 0) while (str.endsWith("0")) str = str.substring(0, str.length() - 1);
		if (str.endsWith(".")) str = str.substring(0, str.length() - 1);
		return str;
	}
	
	/**
	 * Considers the bits of a signed 32-bit integer, and converts it into a long integer as if it were unsigned, i.e.
	 * negative values are converted into results above ~2 billion.
	 */
	public static long unsigned(int val)
	{
		return val >= 0 ? val : ((val & 0x7FFFFFFF) | (1L << 31));
	}
	
	/**
	 * Rounds a floating point value and returns it as an integer.
	 */
	public static int iround(float val) {return (int)Math.round(val);}
	
	/**
	 * Rounds a floating point value and returns it as an integer.
	 */
	public static int iround(double val) {return (int)Math.round(val);}
	
	/**
	 * Rounds down a floating point value and returns it as an integer.
	 */
	public static int ifloor(float val) {return (int)Math.floor(val);}
	
	/**
	 * Rounds down a floating point value and returns it as an integer.
	 */
	public static int ifloor(double val) {return (int)Math.floor(val);}
	
	/**
	 * Rounds up a floating point value and returns it as an integer.
	 */
	public static int iceil(float val) {return (int)Math.ceil(val);}
	
	/**
	 * Rounds up a floating point value and returns it as an integer.
	 */
	public static int iceil(double val) {return (int)Math.ceil(val);}
	
	/**
	 * Rounds down a floating point value and returns it as a float.
	 */
	public static float ffloor(float val) {return (float)Math.floor(val);}
	
	/**
	 * Rounds up a floating point value and returns it as a float.
	 */
	public static float fceil(float val) {return (float)Math.ceil(val);}
	
	/**
	 * Returns the square of a number. This is useful when the parameter is calculated in an expression.
	 */
	public static int sqr(int val) {return val * val;}
	
	/**
	 * Returns the square of a number. This is useful when the parameter is calculated in an expression.
	 */
	public static float sqr(float val) {return val * val;}
	
	/**
	 * Returns the square of a number. This is useful when the parameter is calculated in an expression.
	 */
	public static double sqr(double val) {return val * val;}
	
	/**
	 * Returns the square of the magnitude of a 2D vector.
	 */
	public static int norm2(int x, int y) {return x * x + y * y;}
	
	/**
	 * Returns the square of the magnitude of a 2D vector.
	 */
	public static double norm2(double x, double y) {return x * x + y * y;}
	
	/**
	 * Returns the square of the magnitude of a 3D vector.
	 */
	public static double norm2(double x, double y, double z) {return x * x + y * y + z * z;}
	
	/**
	 * Returns the square of the magnitude of a 2D vector.
	 */
	public static float norm2(float x, float y) {return x * x + y * y;}
	
	/**
	 * Returns the square of the magnitude of a 3D vector.
	 */
	public static float norm2(float x, float y, float z) {return x * x + y * y + z * z;}
	
	/**
	 * Returns the magnitude of a 2D vector.
	 */
	public static double norm(double x, double y) {return Math.sqrt(x * x + y * y);}
	
	/**
	 * Returns the magnitude of a 3D vector.
	 */
	public static double norm(double x, double y, double z) {return Math.sqrt(x * x + y * y + z * z);}
	
	/**
	 * Returns the magnitude of a 2D vector.
	 */
	public static float norm(float x, float y)
	{
		float ax = Math.abs(x), ay = Math.abs(y);
		if (ax < ay * 1E-7f) return ay;
		if (ay < ax * 1E-7f) return ax;
		return (float) Math.sqrt(x * x + y * y);
	}
	
	/**
	 * Returns the magnitude of a 3D vector.
	 */
	public static float norm(float x, float y, float z) {return (float)Math.sqrt(x * x + y * y + z * z);}
	
	/**
	 * Returns the reciprocal of a number, or one if undefined.
	 */
	public static double divZ(double z) {return z == 0 ? 1 : 1 / z;}
	
	/**
	 * Returns the reciprocal of a number, or one if undefined.
	 */
	public static float divZ(float z) {return z == 0 ? 1 : 1 / z;}
	
	/**
	 * Returns the sign of a number: either -1, 0 or 1.
	 */
	public static int signum(int v) {return v < 0 ? -1 : v > 0 ? 1 : 0;}
	
	/**
	 * Returns the sign of a number: either -1, 0 or 1.
	 */
	public static int signum(float v) {return v < 0 ? -1 : v > 0 ? 1 : 0;}
	
	/**
	 * Returns the sign of a number: either -1, 0 or 1.
	 */
	public static int signum(double v) {return v < 0 ? -1 : v > 0 ? 1 : 0;}
	
	public static final double TWOPI = Math.PI * 2;
	public static final double INV_TWOPI = 1 / TWOPI;
	public static final double DEGRAD = Math.PI / 180;
	public static final double RADDEG = 180 / Math.PI;
	
	public static final float PI_F = (float) Math.PI;
	public static final float TWOPI_F = (float) Math.PI * 2;
	public static final float INV_TWOPI_F = 1 / TWOPI_F;
	public static final float DEGRAD_F = (float) Math.PI / 180;
	public static final float RADDEG_F = 180 / (float) Math.PI;
	
	/**
	 * Calculates the difference between two angles, i.e. <b>th2</b> - <b>th1</b>, corrected so that the returned
	 * value is the interior of the two and lies between -PI and +PI, i.e. -PI < <b>th</b> <= PI.
	 * @param th1 angle in radians
	 * @param th2 angle in radians
	 */
	public static double angleDiff(double th1, double th2)
	{
		double theta = angleNorm(th1) - angleNorm(th2);
		return theta - (theta > Math.PI ? TWOPI : 0) + (theta <= -Math.PI ? TWOPI : 0);
	}
	
	/**
	 * Calculates the difference between two angles, i.e. <b>th2</b> - <b>th1</b>, corrected so that the returned
	 * value is the interior of the two and lies between -PI and +PI, i.e. -PI < <b>th</b> <= PI.
	 * @param th1 angle in radians
	 * @param th2 angle in radians
	 */
	public static float angleDiff(float th1, float th2)
	{
		float theta = angleNorm(th1) - angleNorm(th2);
		return theta - (theta > PI_F ? TWOPI_F : 0) + (theta < -PI_F ? TWOPI_F : 0);
	}
	
	/**
	 * Calculates the difference between two angles, i.e. <b>th2</b> - <b>th1</b>, corrected so that the returned
	 * value is the interior of the two and lies between -180 and +180, i.e. -180 < <b>th</b> <= 180.
	 * @param th1 angle in degrees
	 * @param th2 angle in degrees
	 */
	public static double angleDiffDeg(double th1, double th2)
	{
		double theta = angleNormDeg(th1) - angleNormDeg(th2);
		return theta - (theta > 180 ? 360 : 0) + (theta < -180 ? 360 : 0);
	}
	
	/**
	 * Calculates the difference between two angles, i.e. <b>th2</b> - <b>th1</b>, corrected so that the returned
	 * value is the interior of the two and lies between -180 and +180, i.e. -180 < <b>th</b> <= 180.
	 * @param th1 angle in degrees
	 * @param th2 angle in degrees
	 */
	public static float angleDiffDeg(float th1, float th2)
	{
		float theta = angleNormDeg(th1) - angleNormDeg(th2);
		return theta - (theta > 180 ? 360 : 0) + (theta < -180 ? 360 : 0);
	}
	
	/**
	 * Normalises an angle so that it lies within the range -PI < <b>th</b> <= PI.
	 */
	public static double angleNorm(double th)
	{
		if (th == -Math.PI) return Math.PI;
		if (th < -Math.PI)
		{
			double mod = Math.ceil((-th - Math.PI) * INV_TWOPI);
			return th + mod * TWOPI;
		}
		if (th > Math.PI)
		{
			double mod = Math.ceil((th - Math.PI) * INV_TWOPI);
			return th - mod * TWOPI;
		}
		return th;
	}
	
	/**
	 * Normalises an angle so that it lies within the range -PI < <b>th</b> <= PI.
	 */
	public static float angleNorm(float th)
	{
		if (th == -PI_F) return PI_F;
		if (th < -PI_F)
		{
			int mod = Util.iceil((-th - PI_F) * INV_TWOPI_F);
			return th + mod * TWOPI_F;
		}
		if (th >= PI_F)
		{
			int mod = Util.iceil((th - PI_F) * INV_TWOPI_F);
			return th - mod * TWOPI_F;
		}
		return th;
	}
	
	/**
	 * Normalises an angle so that it lies within the range -180 < <b>th</b> <= 180.
	 */
	public static double angleNormDeg(double th)
	{
		if (th == -180) return 180;
		if (th < -180)
		{
			int mod = Util.iceil((-th - 180) * (1.0 / 360));
			return th + mod * 360;
		}
		if (th > 180)
		{
			int mod = Util.iceil((th - 180) * (1.0 / 360));
			return th - mod * 360;
		}
		return th;
	}
	
	/**
	 * Normalises an angle so that it lies within the range -180 < <b>th</b> <= 180.
	 */
	public static float angleNormDeg(float th)
	{
		if (th == -180) return 180;
		if (th < -180)
		{
			int mod = Util.iceil((-th - 180) * (1f / 360));
			return th + mod * 360;
		}
		if (th > 180)
		{
			int mod = Util.iceil((th - 180) * (1f / 360));
			return th - mod * 360;
		}
		return th;
	}
	
	/**
	 * Single precision version of <b>atan2</b>.
	 */
	public static float atan2(float y, float x) {return (float)Math.atan2(y, x);}
	
	/**
	 * Single precision version of <b>cos</b>.
	 */
	public static float cos(float th) {return (float)Math.cos(th);}
	
	/**
	 * Single precision version of <b>sin</b>.
	 */
	public static float sin(float th) {return (float)Math.sin(th);}
	
	/**
	 * Comparison of single precision floating point values, within reasonable precision: tolerates epsilon rounding errors.
	 */
	public static boolean fltEqual(float v1, float v2) {return v1 == v2 || Math.abs(v1 - v2) <= 1E-6f * Math.max(v1, v2);}
	
	/**
	 * Comparison of double precision floating point values, within reasonable precision: tolerates epsilon rounding errors.
	 */
	public static boolean dblEqual(double v1, double v2) {return v1 == v2 || Math.abs(v1 - v2) <= 1E-14 * Math.max(v1, v2);}
	
	/**
	 * Converts an integer colour value, of the format 0xRRGGBB, into the web-style "#RRGGBB" format.
	 */
	public static String colourHTML(int col)
	{
		String str = Integer.toHexString(col);
		return "#" + rep('0', 6 - str.length()) + str;
	}
		
	/**
	 * Repeats the given character a certain number of times.
	 * @param ch character to repeat
	 * @param len number of times to repeat it
	 */
	public static String rep(char ch, int len)
	{
		if (len <= 0) return "";
		String str = String.valueOf(ch);
		while (str.length() < len) str = str + ch;
		return str;
	}
	
	/**
	 * Repeats the given string a certain number of times.
	 * @param str string to repeat
	 * @param len number of times to repeat it
	 */
	public static String rep(String str, int len)
	{
		if (len == 0) return "";
		if (len == 1) return str;
		StringBuilder buff = new StringBuilder();
		for (int n = 0; n < len; n++) buff.append(str);
		return buff.toString();
	}
			
	public static final String UTF8 = "UTF-8";
	
	/**
	 * Convenience method for reading the entire contents of a stream into a string, assuming that it is UTF-8 formatted.
	 * Returns null on exception. This should only be used if it is known for certain that the stream is short.
	 */
	public static String streamToString(InputStream in) throws IOException
	{
		Writer wtr = new StringWriter();
		char[] buff = new char[1024];
		Reader rdr = new BufferedReader(new InputStreamReader(in, UTF8));
		int sz;
		while ((sz = rdr.read(buff)) >= 0) wtr.write(buff, 0, sz);
		return wtr.toString();
	}
	
	/**
	 * Wrapper for ensuring that an input stream is buffered: will either wrap or typecast.
	 */
	public static BufferedInputStream bufferedInputStream(InputStream istr)
	{
		if (istr instanceof BufferedInputStream) return (BufferedInputStream)istr;
		return new BufferedInputStream(istr);
	}
	
	/**
	 * Wrapper for ensuring that a reader is buffered: will either wrap or typecast.
	 */
	public static BufferedReader bufferedReader(Reader rdr)
	{
		if (rdr instanceof BufferedReader) return (BufferedReader)rdr;
		return new BufferedReader(rdr);
	}	
	
	/**
	 * Opens a file resource which may be incorporated into the current .jar file, or it may be a file relative to the current
	 * working directory, which is the case when running with a folder of .class files.
	 * @param obj the object associated with the appropriate .jar file, usually 'this'
	 * @param fn full file path within .jar file, e.g. /foo/bar/fnord.txt
	 */
	public static InputStream openResource(Object obj, String fn) throws IOException
	{
		InputStream istr = obj.getClass().getResourceAsStream(fn);
		if (istr == null)
		{
			String ufn = System.getProperty("user.dir") + fn;
			if (new File(ufn).exists()) istr = new FileInputStream(ufn);
		}
		if (istr == null)
		{
			String ufn = System.getProperty("user.dir") + "/.." + fn;
			if (new File(ufn).exists()) istr = new FileInputStream(ufn);
		}
		if (istr == null) throw new IOException("Missing resource: " + fn);
		return istr;
	}
	
	/**
	 * Increments the value corresponding to a mapped key: if the value does not currently exist or is null, will set it to 1.
	 */	
	public static <T> int incr(Map<T, Integer> counts, T key)
	{
		Integer v = counts.get(key);
		if (v == null) {counts.put(key, 1); return 1;}
		counts.put(key, v + 1);
		return v + 1;
	}	
	
	/**
	 * Swaps two entries in a list.
	 */
	public static <T> void swap(List<T> list, int i1, int i2)
	{
		T v1 = list.get(i1), v2 = list.get(i2);
		list.set(i1, v2);
		list.set(i2, v1);
	}
	
	/**
	* Issues an HTTP request, with an optional URL-encoded form post; makes a optional number of re-requests before
	* ultimately failing.
	*/
	public static String makeRequest(String url, String post, int rerequests) throws IOException
	{
		for (int n = 0; n < rerequests - 1; n++) 
		{
			try {return makeRequest(url, post);} 
			catch (Exception ex) {}
		}
		return makeRequest(url, post);
	}
	
	/**
	* Issues an HTTP request, with an optional URL-encoded form post. A return value of null implies a relatively graceful
	* not found error (usually 404).
	*/
	public static String makeRequest(String url, String post) throws IOException
	{
		HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
		conn.setDoOutput(true);
		if (post == null) conn.setRequestMethod("GET");
		else
		{
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		}
		int cutoff = 300000; // 5 minutes
		conn.setConnectTimeout(cutoff);
		conn.setReadTimeout(cutoff);
		conn.connect();
		
		if (post != null)
		{
			BufferedWriter send = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), Util.UTF8));
			send.append(post);
			send.flush();
			send.close();
		}
		
		int respCode = conn.getResponseCode();
		if (respCode >= 400) return null; // this is relatively graceful
		if (respCode != 200) throw new IOException("HTTP response code " + respCode + " for URL [" + url + "]");
	
		// read the raw bytes into memory; abort if it's too long or too slow
		BufferedInputStream istr = new BufferedInputStream(conn.getInputStream());
		ByteArrayOutputStream buff = new ByteArrayOutputStream();
		final int DOWNLOAD_LIMIT = 100 * 1024 * 1024; // within reason
		while (true)
		{
			int b = -1;
			try {b = istr.read();} 
			catch (SocketTimeoutException ex) {throw new IOException(ex);}
			if (b < 0) break;
			if (buff.size() >= DOWNLOAD_LIMIT) 
				throw new IOException("Download size limit exceeded (max=" + DOWNLOAD_LIMIT + " bytes) for URL: " + url);
			buff.write(b);
		}
		istr.close();
		
		return new String(buff.toByteArray(), Util.UTF8);
	}
	
	/**
	 * Returns the filename that has the "~" prefix expanded out to the full path of the home directory. Valid prefixes are
	 * of the form "~/something" or "~". Other users' directories are not expanded out, i.e. "~username" is not handled.
	 */
	public static String expandFileHome(String fn)
	{
		if (isBlank(fn) || fn.charAt(0) != '~') return fn;
		String home = System.getProperty("user.home");
		return home + fn.substring(1);
	}	
}
