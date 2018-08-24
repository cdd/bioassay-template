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

package com.cdd.bao.axioms;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

/*
	Test for BAO utilities
 */


public class ScanAxiomsTest
{
	@Test
	public void testPutAdd()
	{
		Map<String, Set<String>> map = new HashMap<>();
		Set<String> expected = new HashSet<>();
		assertTrue(ScanAxioms.putAdd(map, "k1", "b"));
		expected.add("b");
		assertEquals(expected, map.get("k1"));
		
		assertFalse(ScanAxioms.putAdd(map, "k1", "b"));
		assertEquals(expected, map.get("k1"));
		
		assertTrue(ScanAxioms.putAdd(map, "k1", "c"));
		expected.add("c");
		assertEquals(expected, map.get("k1"));

		assertTrue(ScanAxioms.putAdd(map, "k2", "b"));
		assertEquals(expected, map.get("k1"));

	}

	@Test
	public void testPutAddList()
	{
		Map<String, List<String>> map = new HashMap<>();
		List<String> expected = new ArrayList<>();
		ScanAxioms.putAdd(map, "k1", "b");
		expected.add("b");
		assertEquals(expected, map.get("k1"));
		
		ScanAxioms.putAdd(map, "k1", "b");
		expected.add("b");
		assertEquals(expected, map.get("k1"));
		
		ScanAxioms.putAdd(map, "k1", "c");
		expected.add("c");
		assertEquals(expected, map.get("k1"));

		ScanAxioms.putAdd(map, "k2", "b");
		assertEquals(expected, map.get("k1"));

	}
}
