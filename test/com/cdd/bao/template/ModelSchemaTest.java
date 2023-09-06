/*
	BioAssay Ontology Annotator Tools

	Copyright 2016-2023 Collaborative Drug Discovery, Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
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
