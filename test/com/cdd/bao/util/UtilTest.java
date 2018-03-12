/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2017 Collaborative Drug Discovery Inc.
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
}
