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
		this.schemaFromJSON = SchemaUtil.deserialise(new File("data/template/schema.json")).schema;
		assumeTrue(schemaFromJSON != null);

		this.schemaFromTTL = SchemaUtil.deserialise(new File("data/template/schema.ttl")).schema;
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
		try (OutputStream ostr = new FileOutputStream(tmpFile))
		{
			SchemaUtil.serialise(schemaFromJSON, SchemaUtil.SerialFormat.JSON, ostr);
		}

		// deserialise file to schema
		Schema schema2 = SchemaUtil.deserialise(tmpFile).schema;
		assertTrue("Schema does not match (destination file in JSON).", schemaFromJSON.equals(schema2));
		
		// deserialise without filename clue
		try (InputStream istr = new FileInputStream(tmpFile))
		{
			Schema schema3 = SchemaUtil.deserialise(istr).schema;
			assertTrue("Schema stream does not match (destination file in JSON).", schemaFromJSON.equals(schema2));
		}

		tmpFile.delete();
	}
	
	@Test
	public void testSerialisationToTTLFile() throws IOException
	{
		File tmpFile = File.createTempFile("testSerialisationToFile", ".ttl");
		try (OutputStream ostr = new FileOutputStream(tmpFile))
		{
			SchemaUtil.serialise(schemaFromJSON, SchemaUtil.SerialFormat.TTL, ostr);
		}

		// deserialise file to schema
		Schema schema2 = SchemaUtil.deserialise(tmpFile).schema;
		assertTrue("Schema does not match (destination file in Turtle).", schemaFromJSON.equals(schema2));

		// deserialise without filename clue
		try (InputStream istr = new FileInputStream(tmpFile))
		{
			Schema schema3 = SchemaUtil.deserialise(istr).schema;
			assertTrue("Schema stream does not match (destination file in Turtle).", schemaFromJSON.equals(schema2));
		}

		tmpFile.delete();
	}
}
