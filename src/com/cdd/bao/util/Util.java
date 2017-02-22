/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2016 Collaborative Drug Discovery Inc.
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

import java.awt.Color;
import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.net.*;
import javafx.scene.control.*;

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
    public static void errmsg(String msg,Exception ex)
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
	public static int length(Object A) {return A == null ? 0 : Array.getLength(A);}

	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
	public static String arrayStr(int[] A)
	{
		if (A == null) return "{null}";
		String str = "";
		for (int n = 0; n < A.length; n++) str += (n > 0 ? "," : "") + A[n];
		return str;
	}

	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
	public static String arrayStr(long[] A)
	{
		if (A == null) return "{null}";
		String str = "";
		for (int n = 0; n < A.length; n++) str += (n > 0 ? "," : "") + A[n];
		return str;
	}

	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
	public static String arrayStr(float[] A)
	{
		if (A == null) return "{null}";
		String str = "";
		for (int n = 0; n < A.length; n++) str += (n > 0 ? "," : "") + A[n];
		return str;
	}

	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
	public static String arrayStr(double[] A)
	{
		if (A == null) return "{null}";
		String str = "";
		for (int n = 0; n < A.length; n++) str += (n > 0 ? "," : "") + A[n];
		return str;
	}

	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
	public static String arrayStr(String[] A)
	{
		if (A == null) return "{null}";
		String str = "";
		for (int n = 0; n < A.length; n++) str += (n > 0 ? "," : "") + "\"" + A[n] + "\"";
		return str;
	}

	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
	public static String arrayStr(boolean[] A)
	{
		if (A == null) return "{null}";
		String str = "{";
		for (int n = 0; n < A.length; n++) str += (A[n] ? "1" : "0");
		return str + "}";
	}
    
	/**
	 * Converts an array to a human-readable string, after having applied a multiplier.
	 * @param A array to format
	 * @param mul modifier to multiply by
	 */
	public static String arrayStr(float[] A, float mul)
	{
		String str = "";
		for (int n = 0; n < A.length; n++) str += (n > 0 ? "," : "") + (A[n] * mul);
		return str;
	}

	/**
	 * Converts an array to a human-readable string, after having applied a multiplier.
	 * @param A array to format
	 * @param mul modifier to multiply by
	 */
	public static String arrayStr(double[] A, double mul)
	{
		String str = "";
		for (int n = 0; n < A.length; n++) str += (n > 0 ? "," : "") + (A[n] * mul);
		return str;
	}
    
	/**
	 * Converts an array to a human-readable string, for debugging purposes.
	 */
    public static String arrayStr(int[][] A) 
    {
		if (A == null) return "{null}";
		String str = "";
		for (int n = 0; n < A.length; n++) str += A[n] == null ? "{null}" : "{" + arrayStr(A[n]) + "}";
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
		StringBuffer buff = new StringBuffer(list[0]);
		for (int n = 1; n < list.length; n++)
		{
			buff.append(sep);
			buff.append(list[n]);
		}
		return buff.toString();
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
		StringBuffer buff = new StringBuffer(String.valueOf(list[0]));
		for (int n = 1; n < list.length; n++)
		{
			buff.append(sep);
			buff.append(String.valueOf(list[n]));
		}
		return buff.toString();
	}

	/**
	 * Converts an array to a human-readable string, where each value is represented as an 8-digit padded hex string.
	 */
	public static String arrayStrHex(int[] A)
	{
		if (A == null) return "{null}";
		String str = "";
		for (int n = 0; n < A.length; n++)
		{
			long v = A[n] >= 0 ? A[n] : ((A[n] & 0x7FFFFFFF) | (1L << 31));
			String hex = Long.toString(v, 16);
			str += (n > 0 ? "," : "") + "0x" + padstr(hex, 8, '0');
		}
		return str;
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
    public static int safeInt(String str,int def) 
    {
    	if (str == null) return def;
    	try {return Integer.valueOf(str).intValue();} 
    	catch (NumberFormatException e) {return def;}
    }
    
    /**
     * Converts a string into an integer, or returns zero if it is invalid, rather than throwing an exception.
     * @param str string to convert
     */
    public static int safeInt(String S) {return safeInt(S, 0);}

    /**
     * Converts a string into a long integer, without the annoyance of having to trap an exception
     * @param str string to convert
     * @param def default value to return in case it is not a valid number
     */
    public static long safeLong(String S, long def) 
    {
    	if (S == null) return def;
    	try {return Long.valueOf(S).longValue();} 
    	catch (NumberFormatException e) {return def;}
    }

    /**
     * Converts a string into a long integer, or returns zero if it is invalid, rather than throwing an exception.
     * @param str string to convert
     */
    public static long safeLong(String S) {return safeLong(S, 0);}
    
    /**
     * Converts a string into a float, without the annoyance of having to trap an exception
     * @param str string to convert
     * @param def default value to return in case it is not a valid number
     */
    public static float safeFloat(String S, float def) 
    {
		if (S == null) return def;
    	try {return Float.valueOf(S).floatValue();} 
		catch (NumberFormatException e) {return def;}
    }

    /**
     * Converts a string into a float, or returns zero if it is invalid, rather than throwing an exception.
     * @param str string to convert
     */
    public static float safeFloat(String S) {return safeFloat(S, 0);}
    
    /**
     * Converts a string into a double, without the annoyance of having to trap an exception
     * @param str string to convert
     * @param def default value to return in case it is not a valid number
     */
    public static double safeDouble(String S, double def) 
    {
    	if ( S== null) return def;
    	try {return Double.valueOf(S).doubleValue();} 
		catch (NumberFormatException e) {return def;}
    }
    
    /**
     * Converts a string into a double, or returns zero if it is invalid, rather than throwing an exception.
     * @param str string to convert
     */
    public static double safeDouble(String S) {return safeDouble(S, 0);}
    
    /**
     * Returns the value of the string, or a blank string if the parameter is null.
     */
    public static String safeString(String S) {return S == null ? "" : S;}
    
    /**
     * Returns the value of the string, or null if the string was blank.
     */
    public static String nullOrString(String S) {return S == null || S.length() == 0 ? null : S;}
    
    /**
     * Returns true if the string is either blank or null.
     */
    public static boolean isBlank(String s) {return s == null || s.length() == 0;}
    
    /**
     * Returns true if the string is neither blank nor null.
     */
    public static boolean notBlank(String s) {return s != null && s.length() > 0;}
    
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
		return String.format("%.17g", val).replaceFirst("\\.?0+(e|$)","$1");
    }
    
    /**
     * Renders the given double floating point value into a string in decimal notation, with a maximum number of
     * significant figures shown. Trailing digits are truncated rather than rounded.
     * @param val value to render
     * @param maxSigFig maximum number of significant figures
     */
    public static String formatDouble(double val, int maxSigFig)
    {
		String str = String.format("%." + maxSigFig + "g", val);
		if (str.indexOf('.') < 0) return str;
		while (str.endsWith("0")) str = str.substring(0, str.length() - 1);
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
    public static int iround(float V) {return (int)Math.round(V);}
    
    /**
     * Rounds a floating point value and returns it as an integer.
     */
    public static int iround(double V) {return (int)Math.round(V);}
    
    /**
     * Rounds down a floating point value and returns it as an integer.
     */
    public static int ifloor(float V) {return (int)Math.floor(V);}

    /**
     * Rounds down a floating point value and returns it as an integer.
     */
    public static int ifloor(double V) {return (int)Math.floor(V);}

    /**
     * Rounds up a floating point value and returns it as an integer.
     */
    public static int iceil(float V) {return (int)Math.ceil(V);}

    /**
     * Rounds up a floating point value and returns it as an integer.
     */
    public static int iceil(double V) {return (int)Math.ceil(V);}
    
    /**
     * Rounds down a floating point value and returns it as a float.
     */
    public static float ffloor(float V) {return (float)Math.floor(V);}

    /**
     * Rounds up a floating point value and returns it as a float.
     */
    public static float fceil(float V) {return (float)Math.ceil(V);}
    
    /**
     * Returns the square of a number. This is useful when the parameter is calculated in an expression.
     */
    public static int sqr(int V) {return V * V;}

    /**
     * Returns the square of a number. This is useful when the parameter is calculated in an expression.
     */
    public static float sqr(float V) {return V * V;}

    /**
     * Returns the square of a number. This is useful when the parameter is calculated in an expression.
     */
    public static double sqr(double V) {return V * V;}

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
     * Returns the reciprocal of a number, or zero if undefined.
     */
    public static double divZ(double z) {return z == 0 ? 1 : 1 / z;}

    /**
     * Returns the reciprocal of a number, or zero if undefined.
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
		if (th == -180) return th = 180;
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
		if (th == -180) return th = 180;
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
	 * Merges two Color instances, to return a new Color with averaged values for red, green & blue.
	 */    
	public static Color mergeCols(Color col1, Color col2)
	{
		int r = col1.getRed() + col2.getRed(), g = col1.getGreen() + col2.getGreen(), b = col1.getBlue() + col2.getBlue();
		return new Color(r / 2,g / 2,b / 2);
	}
    
    /**
     * Converts an integer colour of the form 0xTTRRGGBB into the Color object.
     */
	public static Color intToCol(int trgb)
	{
		int t = (trgb >> 24) & 0xFF, r = (trgb >> 16) & 0xFF, g = (trgb >> 8) & 0xFF, b = trgb & 0xFF;
		return new Color(r,g,b,0xFF - t);
	}
    
    /**
     * Converts a Color object into an integer colour specifier of the form 0xTTRRGGBB.
     */
	public static int colToInt(Color col)
	{
		int t = 0xFF - col.getAlpha(), r = col.getRed(), g = col.getGreen(), b = col.getBlue();
		return (t << 24) | (r << 16) | (g << 8) | b;
	}
    
    /**
     * Takes a colour of the form 0xTTRRGGBB and applies the offset for each of the three colour channels, making sure
     * each is still in the single byte range.
     * @param col original colour in 0xTTRRGGBB format
     * @param dr value to add to the red channel
     * @param dg value to add to the green channel
     * @param db value to add to the blue channel
     */
	public static int tintCol(int col, int dr, int dg, int db)
	{
		float r = ((col >> 16) & 0xFF) + dr, g = ((col >> 8) & 0xFF) + dg, b = (col & 0xFF) + db;
    	if (r < 0) {g -= r; b -= r; r = 0;}
    	if (g < 0) {r -= g; b -= g; g = 0;}
    	if (b < 0) {r -= b; g -= b; b = 0;}
    	if (r < 0) r=0;
    	if (g < 0) g=0;
    	if (r > 255) {float m = 255.0f / r; r = 255; g *= m; b *= m;}
    	if (g > 255) {float m = 255.0f / g; g = 255; r *= m; b *= m;}
    	if (b > 255) {float m = 255.0f / b; b = 255; r *= m; g *= m;}
		return (col & 0xFF000000) | (Util.iround(r) << 16) | (Util.iround(r) << 8) | Util.iround(b);
    }
    
    /**
     * Converts the given integer into a string by adding spaces to the left.
     * @param val number to be formatted
     * @param len total length desired for the result
     * */
    public static String intrpad(int val, int len) {return intrpad(val, len, ' ');}

    /**
     * Converts the given integer into a string by adding the necessary number of characters to the left.
     * @param val number to be formatted
     * @param len total length desired for the result
     * @param ch character to be prepended
     */
    public static String intrpad(int val, int len, char ch)
    {
		String str = Integer.toString(val);
		str = rep(ch, len - str.length()) + str;
		if (str.length() > len) str = str.substring(0, len);
		return str;
    }
    
    /**
     * Pads the string to be right justified, by adding the given number of characters to the left.
     * */
	public static String strrpad(String str, int len, char ch)
	{
		str = rep(ch, len - str.length()) + str;
		if (str.length() > len) str = str.substring(0, len);
		return str;
    }

	/**
	 * Pads the string by adding a specified numbers of characters to the right.
	 */
	public static String padstr(String str, int len, char ch)
	{
		if (str.length() == len) return str;
		if (str.length() > len) return str.substring(0, len);
		return str + rep(ch, len - str.length());
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
		StringBuffer buff = new StringBuffer();
		for (int n = 0; n < len; n++) buff.append(str);
		return buff.toString();
    }
    
    /**
     * Splits up a string according to a supplied mask.
     * @param str string to be split
     * @param mask an array of size equal to length of string, for which true means that the character is to be used to split
     * @return an array of separated strings of size at least 1
     */
    public static String[] split(String str, final boolean[] sepmask)
    {
		int sz = 1;
		for (int n = 0; n < str.length(); n++) if (sepmask[n]) sz++;
		if (sz == 1) return new String[]{str};
    	
		String[] ret = new String[sz];
		sz = 0;
		for (int n = 0, last = 0; n <= str.length(); n++) if (n == str.length() || sepmask[n])
		{
			ret[sz++] = str.substring(last, n);
			last = n + 1;
		}
    	return ret;
    }

	/**
	 * Splits a string by exactly one possible character.
	 * @param str string to be split
	 * @param ch character to split by
	 */
	public static String[] split(String str, char sep)
	{
		final boolean[] sepmask = new boolean[str.length()];
		for (int n = 0; n < str.length(); n++) sepmask[n] = str.charAt(n) == sep;
		return split(str, sepmask);
	}
    
    /** 
     * Splits a string based on a list of possible splitting characters (literals, not a regular expression).
     * @param str string to be split
     * @param seplist list of possible split characters (literal)
     */
	public static String[] split(String str, String seplist)
	{
		boolean[] sepmask = new boolean[str.length()];
		for (int n = 0; n < str.length(); n++) sepmask[n] = seplist.indexOf(str.charAt(n)) >= 0;
		return split(str, sepmask);
	}

	/**
	 * Splits a string by linebreaks, being tolerant of Unix vs. Windows formats.
	 */    
    public static String[] splitLines(String str)
    {
    	return str.split("\\r?\\n");
    }

	/**
	 * Returns the "parent" directory of the given file; if this is already a directory (ends with "/"), rolls it back one further,
     * unless it is already down to "/", in which case it returns the input. If there is no directory, returns a null string. 
     * No checking is done for validity or file existence
     */
	public static String parentDir(String fn)
	{
		if (fn.equals("/")) return fn;
		if (fn.endsWith("/")) fn = fn.substring(0, fn.length() - 1);
		int i = fn.lastIndexOf('/');
		if (i < 0) return "/";
		return fn.substring(0, i + 1);
	}
    
    /**
	 * Returns the given file without any directory prefix.
	 */
	public static String fileName(String fn)
	{
		int i = fn.lastIndexOf('/');
		if (i >= 0) return fn.substring(i + 1);
		return fn;
	}
    
    /**
     * Returns the suffix of the file, if any; e.g. "foo.bar" will return "bar".
     */
	public static String fileSuffix(String fn)
	{
		fn = fileName(fn);
		int i = fn.lastIndexOf('.');
		if (i >= 0) return fn.substring(i + 1);
		return "";
	}

	/**
	 * If the filename has a suffix, returns the same without; e.g. "/wibble/foo.bar" will return "/wibble/foo".
	 */
	public static String fileWithoutSuffix(String fn)
	{
		int i = fn.lastIndexOf('.'), j = fn.lastIndexOf('/');
		if (i < 0 || i < j) return fn;
		return fn.substring(0, i);
	}
    
    /**
     * Adds something before the file suffix, e.g. adding "_001" to "foo.bar" will return "foo_001.bar".
     * @param fn original filename
     * @param psfx presuffix to insert before the file suffix
     */
	public static String insertPreSuffix(String fn, String psfx)
	{
		int i = fn.lastIndexOf('.');
		if (i < 0) i = fn.length();
		return fn.substring(0, i) + psfx + fn.substring(i);
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
		Reader rdr = new BufferedReader(new InputStreamReader(in,UTF8));
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
     * Closes a stream and ignores any exceptions. Useful for within catch blocks.
     */
    public static void silentClose(InputStream istr)
    {
    	try {if (istr != null) istr.close();} catch (IOException ex) {}
    }

    /**
     * Closes a stream and ignores any exceptions. Useful for within catch blocks.
     */
    public static void silentClose(OutputStream ostr)
    {
    	try {if (ostr != null) ostr.close();} catch (IOException ex) {}
    }
    
    /**
     * Closes a stream and ignores any exceptions. Useful for within catch blocks.
     */
    public static void silentClose(Reader rdr)
    {
    	try {if (rdr != null) rdr.close();} catch (IOException ex) {}
    }

    /**
     * Closes a stream and ignores any exceptions. Useful for within catch blocks.
     */
    public static void silentClose(Writer wtr)
    {
    	try {if (wtr != null) wtr.close();} catch (IOException ex) {}
    }

    /**
     * Convenience method for determining the number of days since the beginning of 1970, GMT.
     */
    public static int daysSince1970()
    {
		long time = new Date().getTime(); // in milliseconds
		final long DIVIDER = 1000 * 60 * 60 * 24;
		return (int)(time / DIVIDER);
	}
    
    /**
     * Returns the number of non-null objects in an array.
     */
    public static int packedLength(Object[] loose)
    {
		if (loose == null || loose.length == 0) return 0;
		int len = 0;
		for (int n = loose.length - 1; n >= 0; n--) if (loose[n] != null) len++;
		return len;
    }
    
    /**
     * Packs one array into another by removing nulls.
     * @param loose the array with the source content, may contain nulls
     * @param packed the destination array, which must be long enough to contain all the non-null objects
     */
	public static void packArray(Object[] loose, Object[] packed)
	{
		if (loose == null) return;
		int len = 0;
		for (int n = 0; n < loose.length; n++) if (loose[n] != null) packed[len++] = loose[n];
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
	 * Displays a helpful informational message.
	 */	
	public static void informMessage(String title, String msg)
	{
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
	}
	
	/**
	 * Displays a message with warning theme.
	 */
	public static void informWarning(String title, String msg)
	{
		Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
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
		for (int n = 0; n < rerequests - 1; n++) try {return makeRequest(url, post);} catch (Exception ex) {}
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
			BufferedWriter send = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(),Util.UTF8));
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
			try {b = istr.read();} catch (SocketTimeoutException ex) {throw new IOException(ex);}
			if (b < 0) break;
			if (buff.size() >= DOWNLOAD_LIMIT) 
				throw new IOException("Download size limit exceeded (max=" + DOWNLOAD_LIMIT + " bytes) for URL: " + url);
			buff.write(b);
		}
		istr.close();
		
		return new String(buff.toByteArray(), Util.UTF8);
	}
	
}
