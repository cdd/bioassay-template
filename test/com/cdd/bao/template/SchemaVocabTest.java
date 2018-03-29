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
import static org.junit.Assume.*;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.*;
import org.junit.*;

import com.cdd.bao.template.*;
import com.cdd.bao.template.Vocabulary.*;
import com.cdd.bao.util.*;

/*
	Test SchemaVocab.
*/

public class SchemaVocabTest extends OntologyReader
{
	@Test
	public void testSerialiseWithRemappings() throws IOException
	{
		Vocabulary vocab = new Vocabulary();
		vocab.loadExplicit(getPathsForTests(new String[]{"remap-to.ttl", "remap-to.owl"}));

		SchemaVocab sv = new SchemaVocab(vocab, new Schema[]{});
		Map<String, SchemaVocab.StoredRemapTo> remappings = sv.getRemappings();
		assertTrue("Should find at least 1 remapping!", remappings.size() > 0);

		// serialise with remappings
		File tmpFile = File.createTempFile("serialiseRemappingsToFile", ".json");
		try (OutputStream ostr = new FileOutputStream(tmpFile))
		{
			sv.serialise(ostr);
		}

		// deserialise and vet presence of remappings
		boolean foundDeserialisedRemappings = false;
		try (InputStream istr = new FileInputStream(tmpFile))
		{
			SchemaVocab sv2 = SchemaVocab.deserialise(istr, new Schema[]{});
			Map<String, SchemaVocab.StoredRemapTo> remappings2 = sv2.getRemappings();
			for (String rmURI : remappings2.keySet())
			{
				String failMsg = "Did not find remapped URI \"" + rmURI + "\" in schema deserialised from file.";
				assertTrue(failMsg, remappings.keySet().contains(rmURI));

				String toURI2 = remappings2.get(rmURI).toURI;
				String toURI = remappings.get(rmURI).toURI;
				assertTrue("Deserialised toURI does not match original toURI.",
								toURI2 != null && toURI != null && toURI.equals(toURI2));

				foundDeserialisedRemappings = true;
			}
		}
		if (!foundDeserialisedRemappings)
			throw new AssertionError("Did not find remappings in SchemaVocab deserialised from file.");

		tmpFile.delete();
	}
}
