/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2017-2018 Collaborative Drug Discovery Inc.
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

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

/*
	Test for BAO utilities
 */

public class UtilTest
{
	@Test
	public void testLength()
	{
		assertEquals("handling of null", 0, Util.length(null));
		assertEquals("handling of null", 0, Util.length(new int[0]));
		assertEquals("handling of null", 3, Util.length(new int[]{1, 2, 3}));
	}

	@Test
	public void testPrimByte()
	{
		List<Byte> list = Arrays.asList((byte)1, (byte)2, (byte)3, (byte)4);
		byte[] primArray = Util.primByte(list);
		assertArrayEquals(new byte[]{1, 2, 3, 4}, primArray);

		Set<Byte> set = new TreeSet<>(list);
		primArray = Util.primByte(set);
		assertArrayEquals(new byte[]{1, 2, 3, 4}, primArray);
	}

	@Test
	public void testPrimInt()
	{
		List<Integer> list = Arrays.asList(1, 2, 3, 4);
		int[] primArray = Util.primInt(list);
		assertArrayEquals(new int[]{1, 2, 3, 4}, primArray);

		Set<Integer> set = new TreeSet<>(list);
		primArray = Util.primInt(set);
		assertArrayEquals(new int[]{1, 2, 3, 4}, primArray);
	}

	@Test
	public void testPrimLong()
	{
		List<Long> list = Arrays.asList(4L, 3L, 2L, 1L);
		long[] primArray = Util.primLong(list);
		assertArrayEquals(new long[]{4L, 3L, 2L, 1L}, primArray);

		Set<Long> set = new TreeSet<>(list);
		primArray = Util.primLong(set);
		assertArrayEquals(new long[]{1L, 2L, 3L, 4L}, primArray);
	}

	@Test
	public void testPrimFloat()
	{
		List<Float> list = Arrays.asList(4.0f, 3.0f, 2.0f, 1.0f);
		float[] primArray = Util.primFloat(list);
		assertArrayEquals(new float[]{4f, 3f, 2f, 1f}, primArray, 0.1f);

		Set<Float> set = new TreeSet<>(list);
		primArray = Util.primFloat(set);
		assertArrayEquals(new float[]{1f, 2f, 3f, 4f}, primArray, 0.1f);
	}

	@Test
	public void testPrimDouble()
	{
		List<Double> list = Arrays.asList(4.0d, 3.0, 2.0, 1.0);
		double[] primArray = Util.primDouble(list);
		assertArrayEquals(new double[]{4d, 3d, 2d, 1d}, primArray, 0.1);

		Set<Double> set = new TreeSet<>(list);
		primArray = Util.primDouble(set);
		assertArrayEquals(new double[]{1d, 2d, 3d, 4d}, primArray, 0.1);
	}

	@Test
	public void testPrimBoolean()
	{
		List<Boolean> list = Arrays.asList(false, true, false);
		boolean[] primArray = Util.primBoolean(list);
		assertArrayEquals(new boolean[]{false, true, false}, primArray);

		Set<Boolean> set = new TreeSet<>(list);
		primArray = Util.primBoolean(set);
		assertArrayEquals(new boolean[]{false, true}, primArray);
	}

	@Test
	public void testPrimChar()
	{
		List<Character> list = Arrays.asList('c', 'b', 'a', 'a', 'b', 'c');
		char[] primArray = Util.primCharacter(list);
		assertArrayEquals(new char[]{'c', 'b', 'a', 'a', 'b', 'c'}, primArray);

		Set<Character> set = new TreeSet<>(list);
		primArray = Util.primCharacter(set);
		assertArrayEquals(new char[]{'a', 'b', 'c'}, primArray);
	}

	@Test
	public void testPrimString()
	{
		List<String> list = Arrays.asList("c", "b", "a", "a", "b", "c");
		String[] primArray = Util.primString(list);
		assertArrayEquals(new String[]{"c", "b", "a", "a", "b", "c"}, primArray);

		Set<String> set = new TreeSet<>(list);
		primArray = Util.primString(set);
		assertArrayEquals(new String[]{"a", "b", "c"}, primArray);
	}

	@Test
	public void testArrayStr()
	{
		int[] intArr = null;
		assertEquals("null handling", "{null}", Util.arrayStr(intArr));
		intArr = new int[]{1, 2, 3, 4};
		assertEquals("1,2,3,4", Util.arrayStr(intArr));

		long[] longArr = null;
		assertEquals("null handling", "{null}", Util.arrayStr(longArr));
		longArr = new long[]{2, 3, 4, 1};
		assertEquals("2,3,4,1", Util.arrayStr(longArr));

		float[] floatArr = null;
		assertEquals("null handling", "{null}", Util.arrayStr(floatArr));
		floatArr = new float[]{3, 4, 1, 2};
		assertEquals("3.0,4.0,1.0,2.0", Util.arrayStr(floatArr));
		assertEquals("6.0,8.0,2.0,4.0", Util.arrayStr(floatArr, 2));

		double[] doubleArr = null;
		assertEquals("null handling", "{null}", Util.arrayStr(doubleArr));
		doubleArr = new double[]{4, 1, 2, 3};
		assertEquals("4.0,1.0,2.0,3.0", Util.arrayStr(doubleArr));
		assertEquals("12.0,3.0,6.0,9.0", Util.arrayStr(doubleArr, 3));

		String[] stringArr = null;
		assertEquals("null handling", "{null}", Util.arrayStr(stringArr));
		stringArr = new String[]{"a", "b", "c", "d"};
		assertEquals("\"a\",\"b\",\"c\",\"d\"", Util.arrayStr(stringArr));
		stringArr = new String[]{null, "b", null, "d"};
		assertEquals("null,\"b\",null,\"d\"", Util.arrayStr(stringArr));

		boolean[] boolArr = null;
		assertEquals("null handling", "{null}", Util.arrayStr(boolArr));
		boolArr = new boolean[]{true, false, false, true};
		assertEquals("{1001}", Util.arrayStr(boolArr));

		int[][] intArr2D = null;
		assertEquals("null handling", "{null}", Util.arrayStr(intArr2D));
		intArr2D = new int[3][];
		intArr2D[0] = null;
		intArr2D[1] = new int[]{1};
		intArr2D[2] = new int[]{1, 2};
		assertEquals("{null}{1}{1,2}", Util.arrayStr(intArr2D));
	}

	@Test
	public void testJoin()
	{
		assertEquals(null, Util.join((String[])null, ","));
		assertEquals(null, Util.join(new String[0], ","));
		assertEquals("a", Util.join(new String[]{"a"}, ","));
		assertEquals("a,b", Util.join(new String[]{"a", "b"}, ","));
		assertEquals("a,b,c,d", Util.join(new String[]{"a", "b", "c", "d"}, ","));

		assertEquals(null, Util.join((int[])null, ","));
		assertEquals(null, Util.join(new int[0], ","));
		assertEquals("1", Util.join(new int[]{1}, ","));
		assertEquals("1,2", Util.join(new int[]{1, 2}, ","));
		assertEquals("1,2,3,4", Util.join(new int[]{1, 2, 3, 4}, ","));
	}

	@Test
	public void testIsNumeric()
	{
		assertTrue(Util.isNumeric("1.0"));
		assertTrue(Util.isNumeric("12"));
		assertTrue(Util.isNumeric("3.1415"));
		assertTrue(Util.isNumeric("-3.1415"));

		assertFalse(Util.isNumeric("abc"));
		assertFalse(Util.isNumeric(" 3.1415"));
		assertFalse(Util.isNumeric("+3.1415"));
		assertFalse(Util.isNumeric("3.1415 "));
	}

	@Test
	public void testSafeConversion()
	{
		assertEquals(123, Util.safeInt("123"));
		assertEquals("Default for null values", 0, Util.safeInt(null));
		assertEquals("Default for non-numeric strings", 0, Util.safeInt("abc"));

		assertEquals(123, Util.safeInt("123", 11));
		assertEquals("Default for null values", 11, Util.safeInt(null, 11));
		assertEquals("Default for non-numeric strings", 11, Util.safeInt("abc", 11));

		assertEquals(123L, Util.safeLong("123"));
		assertEquals("Default for null values", 0, Util.safeLong(null));
		assertEquals("Default for non-numeric strings", 0, Util.safeLong("abc"));

		assertEquals(123L, Util.safeLong("123", 11));
		assertEquals("Default for null values", 11L, Util.safeLong(null, 11L));
		assertEquals("Default for non-numeric strings", 11L, Util.safeLong("abc", 11L));

		assertEquals(3.14f, Util.safeFloat("3.14"), 0.001);
		assertEquals("Default for null values", 0.0f, Util.safeFloat(null), 0.001);
		assertEquals("Default for non-numeric strings", 0.0f, Util.safeFloat("abc"), 0.001);

		assertEquals(3.14f, Util.safeFloat("3.14", 11.0f), 0.001);
		assertEquals("Default for null values", 11.0f, Util.safeFloat(null, 11.0f), 0.001);
		assertEquals("Default for non-numeric strings", 11.0f, Util.safeFloat("abc", 11.0f), 0.001);

		assertEquals(3.14, Util.safeDouble("3.14"), 0.001);
		assertEquals("Default for null values", 0.0, Util.safeDouble(null), 0.001);
		assertEquals("Default for non-numeric strings", 0.0, Util.safeDouble("abc"), 0.001);

		assertEquals(3.14, Util.safeDouble("3.14", 11.0), 0.001);
		assertEquals("Default for null values", 11.0, Util.safeDouble(null, 11.0), 0.001);
		assertEquals("Default for non-numeric strings", 11.0, Util.safeDouble("abc", 11.0), 0.001);
	}

	@Test
	public void testSafeStringHandling()
	{
		assertEquals("null handling", "", Util.safeString((String)null));
		assertEquals("", Util.safeString(""));
		assertEquals("abc", Util.safeString("abc"));

		assertEquals(null, Util.nullOrString((String)null));
		assertEquals(null, Util.nullOrString(""));
		assertEquals("abc", Util.nullOrString("abc"));

		assertTrue(null, Util.isBlank((String)null));
		assertTrue(null, Util.isBlank(""));
		assertFalse("abc", Util.isBlank("abc"));

		assertFalse(null, Util.notBlank((String)null));
		assertFalse(null, Util.notBlank(""));
		assertTrue("abc", Util.notBlank("abc"));

		assertTrue(Util.equals((String)null, (String)null));
		assertTrue(Util.equals((String)null, ""));
		assertFalse(Util.equals((String)null, "abc"));
		assertTrue(Util.equals("", (String)null));
		assertFalse(Util.equals("abc", (String)null));

		assertFalse(Util.equals("abc", "def"));
		assertTrue(Util.equals("abc", "abc"));
	}

	@Test
	public void testFormatDouble()
	{
		assertEquals("100000", Util.formatDouble(100000.0));
		assertEquals("100000.1", Util.formatDouble(100000.1));
		assertEquals("100000.12345", Util.formatDouble(100000.12345));
		assertEquals("1e+40", Util.formatDouble(1e40));
		assertEquals("0.001", Util.formatDouble(0.001));
		assertEquals("1e-11", Util.formatDouble(0.00000000001));

		assertEquals("1", Util.formatDouble(1, 3));
		assertEquals("0.123", Util.formatDouble(0.1234, 3));
		assertEquals("0.0123", Util.formatDouble(0.01234, 3));
		assertEquals("0.00123", Util.formatDouble(0.001234, 3));
		assertEquals("0.000123", Util.formatDouble(0.0001234, 3));
		assertEquals("12", Util.formatDouble(12, 3));
		assertEquals("123", Util.formatDouble(123, 3));
		assertEquals("1.23e+03", Util.formatDouble(1234, 3));
		assertEquals("1.24e+03", Util.formatDouble(1236, 3));
		assertEquals("1.00e+05", Util.formatDouble(100000.0, 3));
		assertEquals("1.00e+05", Util.formatDouble(100000.1, 3));
		assertEquals("1.01e+05", Util.formatDouble(100999, 3));
		assertEquals("1.00e+40", Util.formatDouble(1e40, 3));
		assertEquals("1.00e+100", Util.formatDouble(1e100, 3));
		assertEquals("1.00e-11", Util.formatDouble(0.00000000001, 3));
	}

	@Test
	public void testNumberHandling()
	{
		assertEquals(0L, Util.unsigned(0));
		assertEquals(1L, Util.unsigned(1));
		assertEquals(2L, Util.unsigned(2));
		assertEquals(4294967295L, Util.unsigned(-1));
		assertEquals(4294967294L, Util.unsigned(-2));

		assertEquals(0, Util.iround(0.1f));
		assertEquals(0, Util.iround(0.4f));
		assertEquals(1, Util.iround(0.5f));
		assertEquals(1, Util.iround(0.8f));
		assertEquals(0, Util.iround(-0.1f));
		assertEquals(0, Util.iround(-0.4f));
		assertEquals(0, Util.iround(-0.5f));
		assertEquals(-1, Util.iround(-0.8f));

		assertEquals(0, Util.iround(0.1));
		assertEquals(0, Util.iround(0.4));
		assertEquals(1, Util.iround(0.5));
		assertEquals(1, Util.iround(0.8));
		assertEquals(0, Util.iround(-0.1));
		assertEquals(0, Util.iround(-0.4));
		assertEquals(0, Util.iround(-0.5));
		assertEquals(-1, Util.iround(-0.8));

		assertEquals(0, Util.ifloor(0.1f));
		assertEquals(0, Util.ifloor(0.8f));
		assertEquals(1, Util.ifloor(1.1f));
		assertEquals(-1, Util.ifloor(-0.1f));
		assertEquals(-2, Util.ifloor(-1.1f));

		assertEquals(0, Util.ifloor(0.1));
		assertEquals(0, Util.ifloor(0.8));
		assertEquals(1, Util.ifloor(1.1));
		assertEquals(-1, Util.ifloor(-0.1));
		assertEquals(-2, Util.ifloor(-1.1));

		assertEquals(1, Util.iceil(0.1f));
		assertEquals(1, Util.iceil(0.8f));
		assertEquals(2, Util.iceil(1.1f));
		assertEquals(0, Util.iceil(-0.1f));
		assertEquals(-1, Util.iceil(-1.1f));

		assertEquals(1, Util.iceil(0.1));
		assertEquals(1, Util.iceil(0.8));
		assertEquals(2, Util.iceil(1.1));
		assertEquals(0, Util.iceil(-0.1));
		assertEquals(-1, Util.iceil(-1.1));

		assertEquals(0f, Util.ffloor(0.1f), 0.001);
		assertEquals(0f, Util.ffloor(0.8f), 0.001);
		assertEquals(1f, Util.ffloor(1.1f), 0.001);
		assertEquals(-1f, Util.ffloor(-0.1f), 0.001);
		assertEquals(-2f, Util.ffloor(-1.1f), 0.001);

		assertEquals(1f, Util.fceil(0.1f), 0.001);
		assertEquals(1f, Util.fceil(0.8f), 0.001);
		assertEquals(2f, Util.fceil(1.1f), 0.001);
		assertEquals(0f, Util.fceil(-0.1f), 0.001);
		assertEquals(-1f, Util.fceil(-1.1f), 0.001);

		assertEquals(9, Util.sqr(3));
		assertEquals(0, Util.sqr(0));
		assertEquals(9, Util.sqr(-3));

		assertEquals(9f, Util.sqr(3f), 0.001);
		assertEquals(0f, Util.sqr(0f), 0.001);
		assertEquals(9f, Util.sqr(-3f), 0.001);

		assertEquals(9.0, Util.sqr(3.0), 0.001);
		assertEquals(0.0, Util.sqr(0.0), 0.001);
		assertEquals(9.0, Util.sqr(-3.0), 0.001);

		assertEquals(25, Util.norm2(3, 4));
		assertEquals(25f, Util.norm2(3f, 4f), 0.001);
		assertEquals(29f, Util.norm2(3f, 4f, 2f), 0.001);
		assertEquals(25.0, Util.norm2(3.0, 4.0), 0.001);
		assertEquals(29.0, Util.norm2(3.0, 4.0, 2.0), 0.001);

		assertEquals(5.0, Util.norm(3.0, 4.0), 0.001);
		assertEquals(Math.sqrt(29.0), Util.norm(3.0, 4.0, 2.0), 0.001);

		assertEquals(5.0, Util.norm(-3.0f, -4.0f), 0.001);
		assertEquals(3.0, Util.norm(-3.0f, 0.0f), 0.001);
		assertEquals(4.0, Util.norm(0.0f, -4.0f), 0.001);
		assertEquals(Math.sqrt(29.0), Util.norm(3.0f, 4.0f, 2.0f), 0.001);

		assertEquals(1.0, Util.divZ(0.0f), 0.0001);
		assertEquals(0.5, Util.divZ(2.0f), 0.0001);
		assertEquals(Float.POSITIVE_INFINITY, Util.divZ(1e-40f), 0.0001);

		assertEquals(1.0, Util.divZ(0.0), 0.0001);
		assertEquals(0.5, Util.divZ(2.0), 0.0001);
		assertEquals(1e40, Util.divZ(1e-40), 0.0001);

		assertEquals(-1, Util.signum(-4));
		assertEquals(0, Util.signum(0));
		assertEquals(1, Util.signum(2));
		assertEquals(-1, Util.signum(-4.0f));
		assertEquals(0, Util.signum(0.0f));
		assertEquals(1, Util.signum(2.0f));
		assertEquals(-1, Util.signum(-4.0));
		assertEquals(0, Util.signum(0.0));
		assertEquals(1, Util.signum(2.0));
	}

	@Test
	public void testAngleManipulations()
	{
		for (double angle : new double[]{0.0, Math.PI, 0.369})
		{
			String msg = "angle " + angle;
			assertEquals(msg, angle, Util.angleNorm(angle), 0.001);
			assertEquals(msg, angle, Util.angleNorm(angle + Util.TWOPI), 0.001);
			assertEquals(msg, angle, Util.angleNorm(angle + 2 * Util.TWOPI), 0.001);
			assertEquals(msg, angle, Util.angleNorm(angle - Util.TWOPI), 0.001);
		}
		assertEquals(Math.PI, Util.angleNorm(-Math.PI), 0.001);

		for (float angle : new float[]{0.0f, Util.PI_F, 0.369f})
		{
			String msg = "angle " + angle;
			assertEquals(msg, angle, Util.angleNorm(angle), 0.001);
			assertEquals(msg, angle, Util.angleNorm(angle + Util.TWOPI_F), 0.001);
			assertEquals(msg, angle, Util.angleNorm(angle + 2 * Util.TWOPI_F), 0.001);
			assertEquals(msg, angle, Util.angleNorm(angle - Util.TWOPI_F), 0.001);
		}
		assertEquals(Util.PI_F, Util.angleNorm(-Util.PI_F), 0.001);

		assertEquals(-0.5 * Math.PI, Util.angleDiff(0, 0.5 * Math.PI), 0.001);
		assertEquals(0.5 * Math.PI, Util.angleDiff(0, 1.5 * Math.PI), 0.001);
		assertEquals(0.5 * Math.PI, Util.angleDiff(0.5 * Math.PI, 0), 0.001);
		assertEquals(-0.5 * Math.PI, Util.angleDiff(1.5 * Math.PI, 0), 0.001);
		assertEquals(Math.PI, Util.angleDiff(1.5 * Math.PI, -1.5 * Math.PI), 0.001);
		assertEquals(Math.PI, Util.angleDiff(-1.5 * Math.PI, 1.5 * Math.PI), 0.001);
		assertEquals(Math.PI - 0.1, Util.angleDiff(1.5 * Math.PI, -1.5 * Math.PI + 0.1), 0.001);
		assertEquals(-Math.PI + 0.1, Util.angleDiff(-1.5 * Math.PI + 0.1, 1.5 * Math.PI), 0.001);

		assertEquals(-0.5 * Util.PI_F, Util.angleDiff(0f, 0.5f * Util.PI_F), 0.001);
		assertEquals(0.5 * Util.PI_F, Util.angleDiff(0f, 1.5f * Util.PI_F), 0.001);
		assertEquals(0.5 * Util.PI_F, Util.angleDiff(0.5f * Util.PI_F, 0f), 0.001);
		assertEquals(-0.5 * Util.PI_F, Util.angleDiff(1.5f * Util.PI_F, 0f), 0.001);
		assertEquals(Util.PI_F, Util.angleDiff(1.5f * Util.PI_F, -1.5f * Util.PI_F), 0.001);
		assertEquals(Util.PI_F - 0.1, Util.angleDiff(1.5f * Util.PI_F, -1.5f * Util.PI_F + 0.1f), 0.001);
		assertEquals(-Util.PI_F + 0.1, Util.angleDiff(-1.5f * Util.PI_F + 0.1f, 1.5f * Util.PI_F), 0.001);
	}

	@Test
	public void testAngleManipulationsDeg()
	{
		for (double angle : new double[]{0.0, 180, 45})
		{
			String msg = "angle " + angle;
			assertEquals(msg, angle, Util.angleNormDeg(angle), 0.001);
			assertEquals(msg, angle, Util.angleNormDeg(angle + 360), 0.001);
			assertEquals(msg, angle, Util.angleNormDeg(angle + 720), 0.001);
			assertEquals(msg, angle, Util.angleNormDeg(angle - 360), 0.001);
		}
		assertEquals(180, Util.angleNormDeg(-180), 0.001);

		for (float angle : new float[]{0.0f, Util.PI_F, 0.369f})
		{
			String msg = "angle " + angle;
			assertEquals(msg, angle, Util.angleNormDeg(angle), 0.001);
			assertEquals(msg, angle, Util.angleNormDeg(angle + 360f), 0.001);
			assertEquals(msg, angle, Util.angleNormDeg(angle + 720f), 0.001);
			assertEquals(msg, angle, Util.angleNormDeg(angle - 360f), 0.001);
		}
		assertEquals(180f, Util.angleNormDeg(-180f), 0.001);

		assertEquals(-90, Util.angleDiffDeg(0.0, 90.0), 0.001);
		assertEquals(90, Util.angleDiffDeg(0.0, 270.0), 0.001);
		assertEquals(90, Util.angleDiffDeg(90.0, 0.0), 0.001);
		assertEquals(-90, Util.angleDiffDeg(270.0, 0.0), 0.001);
		assertEquals(170, Util.angleDiffDeg(270.0, -260.0), 0.001);
		assertEquals(-170, Util.angleDiffDeg(-260.0, 270.0), 0.001);

		assertEquals(-90, Util.angleDiffDeg(0.0f, 90.0f), 0.001);
		assertEquals(90, Util.angleDiffDeg(0.0f, 270.0f), 0.001);
		assertEquals(90, Util.angleDiffDeg(90.0f, 0.0f), 0.001);
		assertEquals(-90, Util.angleDiffDeg(270.0f, 0.0f), 0.001);
		assertEquals(170, Util.angleDiffDeg(270.0f, -260.0f), 0.001);
		assertEquals(-170, Util.angleDiffDeg(-260.0f, 270.0f), 0.001);
	}

	@Test
	public void testColorHandling()
	{
		assertEquals("#000001", Util.colourHTML(0x1));
		assertEquals("#000101", Util.colourHTML(0x101));
		assertEquals("#010101", Util.colourHTML(0x10101));
		assertEquals("#ffffff", Util.colourHTML(0xffffff));
	}

	@Test
	public void testStringHandling()
	{
		assertEquals("", Util.rep("ab", 0));
		assertEquals("ab", Util.rep("ab", 1));
		assertEquals("abab", Util.rep("ab", 2));
		assertEquals("ababab", Util.rep("ab", 3));
	}

	@Test
	public void testMiscellaneous()
	{
		Map<String, Integer> counts = new HashMap<>();
		assertEquals(1, Util.incr(counts, "a"));
		assertEquals(2, Util.incr(counts, "a"));
		assertEquals(1, Util.incr(counts, "b"));

		List<Integer> list = Arrays.asList(1, 2, 3);
		Util.swap(list, 0, 1);
		assertEquals(Arrays.asList(2, 1, 3), list);
	}
	
	@Test
	public void testStringSim()
	{
		Object[][] testCases = new Object[][]
		{
			{"same", "same", 0, 0},
			{"a", "b", 1, 5},
			{"uh", "huh", 1, 3},
			{"rat", "cat", 1, 3},
			{"flip", "flop", 1, 2},
			{"fnord", "dronf", 4, 4},
			{"Ni!", "Ekke ekke ekke ekke ptang zoo boing!", 33, 33},
		};
		for (Object[] test : testCases)
		{
			String str1 = (String)test[0], str2 = (String)test[1];
			Integer sim = (Integer)test[2], cal = (Integer)test[3];
			String msg = "strings: [" + str1 + "], [" + str2 + "]";
			assertEquals("raw similarity; " + msg, sim, Integer.valueOf(Util.stringSimilarity(str1, str2)));
			assertEquals("raw similarity; " + msg, sim, Integer.valueOf(Util.stringSimilarity(str2, str1)));
			assertEquals("calibrated similarity; " + msg, cal, Integer.valueOf(Util.calibratedSimilarity(str1, str2)));
			assertEquals("calibrated similarity; " + msg, cal, Integer.valueOf(Util.calibratedSimilarity(str2, str1)));
			assertEquals(0, Util.stringSimilarity(str1, str1));
			assertEquals(0, Util.stringSimilarity(str2, str2));
			assertEquals(0, Util.calibratedSimilarity(str1, str1));
			assertEquals(0, Util.calibratedSimilarity(str2, str2));
		}
	}
}
