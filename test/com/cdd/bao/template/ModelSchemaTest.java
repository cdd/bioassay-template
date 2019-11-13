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

package com.cdd.bao.template;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

/*
	Test for BAO Schema
*/

public class ModelSchemaTest
{
	@Test
	public void testPrefixes()
	{
		Map<String, String> prefixes = ModelSchema.getPrefixes();
		assertTrue(prefixes.containsKey("bae:"));
		
		String longURI = "http://www.bioassayexpress.org/bae#BAE_000009";
		String shortURI = "bae:BAE_000009";
		assertEquals(shortURI, ModelSchema.collapsePrefix(longURI));
		assertEquals(longURI, ModelSchema.expandPrefix(shortURI));

		assertEquals(ModelSchema.collapsePrefix(null), null);
		assertEquals(ModelSchema.expandPrefix(null), null);

		assertEquals(ModelSchema.collapsePrefix("not matching"), "not matching");
		assertEquals(ModelSchema.expandPrefix("not matching"), "not matching");

		String[] longURIs = {longURI, longURI};
		String[] shortURIs = {shortURI, shortURI};
		assertArrayEquals(shortURIs, ModelSchema.collapsePrefixes(longURIs));
		assertArrayEquals(longURIs, ModelSchema.expandPrefixes(shortURIs));
		assertArrayEquals(ModelSchema.collapsePrefixes(null), null);
		assertArrayEquals(ModelSchema.expandPrefixes(null), null);
		
		// adding and removing
		assertFalse(prefixes.containsKey("newpfx:"));
		
		ModelSchema.addPrefix("newpfx:", "http://newpfx/pfx#");
		assertTrue(ModelSchema.getPrefixes().containsKey("newpfx:"));
		assertEquals("newpfx:NEW123", ModelSchema.collapsePrefix("http://newpfx/pfx#NEW123"));
		assertNotEquals(prefixes, ModelSchema.getPrefixes());

		ModelSchema.addPrefix("newpfx:", "http://changed/pfx#");
		assertEquals("newpfx:NEW123", ModelSchema.collapsePrefix("http://changed/pfx#NEW123"));
		
		ModelSchema.removePrefix("newpfx:");
		assertFalse(ModelSchema.getPrefixes().containsKey("newpfx:"));
		assertEquals(prefixes, ModelSchema.getPrefixes());
		
		ModelSchema.removePrefix("newpfx:");
		assertEquals(prefixes, ModelSchema.getPrefixes());
	}

	// ------------ private methods ------------

}
