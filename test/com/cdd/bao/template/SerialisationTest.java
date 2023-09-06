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
import static org.junit.Assume.*;

import java.io.*;

import org.junit.*;

/*
	Test for (de)serialisation routines.
*/

public class SerialisationTest extends OntologyReader
{
	private Schema schemaFromJSON;
	private Schema schemaFromTTL;

	@Before
	public void setup() throws IOException
	{
		String[] testSchema = getPathsForTests(new String[]{"schema.json", "schema.ttl"});
		this.schemaFromJSON = SchemaUtil.deserialise(new File(testSchema[0])).schema;
		assumeTrue(schemaFromJSON != null);

		this.schemaFromTTL = SchemaUtil.deserialise(new File(testSchema[1])).schema;
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
			assertTrue("Schema stream does not match (destination file in JSON).", schemaFromJSON.equals(schema3));
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
			assertTrue("Schema stream does not match (destination file in Turtle).", schemaFromJSON.equals(schema3));
		}

		tmpFile.delete();
	}
}
