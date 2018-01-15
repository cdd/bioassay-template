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

package com.cdd.bao.template;

import com.cdd.bao.template.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.*;
import org.junit.*;

/*
	Test for (de)serialisation routines.
*/

public class SerialisationTest
{
	private Schema schemaFromJSON;
	private Schema schemaFromTTL;

	@Before
	public void setup() throws IOException
	{
		this.schemaFromJSON = SchemaUtil.deserialise(new File("data/template/schema.json"));
		assumeTrue(schemaFromJSON != null);

		this.schemaFromTTL = SchemaUtil.deserialise(new File("data/template/schema.ttl"));
		assumeTrue(schemaFromTTL != null);
	}

	@Test
	public void testSerialisationFromJSON() throws IOException
	{
		// serialise schema to string
		StringWriter sw = new StringWriter();
		schemaFromJSON.serialise(sw);

		// deserialise string to schema
		String srcSchema = sw.toString();
		Schema schema2 = Schema.deserialise(new StringReader(srcSchema));

		assertTrue("Schema do not match (source schema from JSON).", schemaFromJSON.equals(schema2));
	}

	@Test
	public void testSerialisationFromTTL() throws IOException
	{
		// serialise schema to string
		StringWriter sw = new StringWriter();
		schemaFromTTL.serialise(sw);

		// deserialise string to schema
		String srcSchema = sw.toString();
		Schema schema2 = Schema.deserialise(new StringReader(srcSchema));

		assertTrue("Schema do not match (source schema from TURTLE).", schemaFromTTL.equals(schema2));
	}
	
	@Test
	public void testSerialisationToJSONFile() throws IOException
	{
		File tmpFile = File.createTempFile("testSerialisationToFile", ".json");
		SchemaUtil.serialise(schemaFromJSON, tmpFile);

		// deserialise file to schema
		Schema schema2 = SchemaUtil.deserialise(tmpFile);
		tmpFile.delete();

		assertTrue("Schema do not match (destination file in JSON).", schemaFromJSON.equals(schema2));
	}
	
	@Test
	public void testSerialisationToTTLFile() throws IOException
	{
		File tmpFile = File.createTempFile("testSerialisationToFile", ".ttl");
		SchemaUtil.serialise(schemaFromJSON, tmpFile);

		// deserialise file to schema
		Schema schema2 = SchemaUtil.deserialise(tmpFile);
		tmpFile.delete();

		assertTrue("Schema do not match (destination file in TURTLE).", schemaFromJSON.equals(schema2));
	}
}
