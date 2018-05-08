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
import com.cdd.bao.template.Schema.*;
import com.cdd.bao.template.Vocabulary.*;
import com.cdd.bao.util.*;

public class SchemaTreeTest extends OntologyReader
{
	private static class ProvTerm {
		public String parentURI;
		public String label;
		public String descr;
		public String uri;
	}

	private Vocabulary vocab;
	private Schema schema;
	private ProvTerm provTerm;

	@Before
	public void setUp() throws IOException
	{
		vocab = new Vocabulary();
		vocab.loadExplicit(getPathsForTests(new String[]{"bao_complete_merged.owl"}));

		String[] testSchema = getPathsForTests(new String[]{"schema.json"});
		schema = Schema.deserialise(new File(testSchema[0]));
		
		provTerm = new ProvTerm();
		provTerm.parentURI = "http://www.bioassayontology.org/bao#BAO_0000008";
		provTerm.label = "provisional_test";
		provTerm.descr = "Test logic that tacks on provisional term to existing schema tree.";
		provTerm.uri = "http://www.bioassayontology.org/bat#provisional_test";
	}
	
	@Test
	public void testAddNode() throws IOException
	{
		Schema.Group group = schema.getRoot();
		Assignment assn = group.assignments.get(1); // bioassay type

		SchemaTree schemaTree = new SchemaTree(assn, vocab);
		schemaTree.addNode(provTerm.parentURI, provTerm.label, provTerm.descr, provTerm.uri);

		// verify tree
		Map<String, SchemaTree.Node> tree = schemaTree.getTree();
		SchemaTree.Node provNode = tree.get(provTerm.uri);
		verifyNode(provNode, provTerm);

		// verify node in flat array
		for (SchemaTree.Node node : schemaTree.getFlat())
		{
			if (StringUtils.equals(node.uri, provTerm.uri))
			{
				verifyNode(node, provTerm);
				break;
			}
		}

		// verify node in list array
		for (SchemaTree.Node node : schemaTree.getList())
		{
			if (StringUtils.equals(node.uri, provTerm.uri))
			{
				verifyNode(node, provTerm);
				break;
			}
		}
	}
	
	@Test
	public void testAddNodeWithMissingParent()
	{
		Schema.Group group = schema.getRoot();
		Assignment assn = group.assignments.get(1); // bioassay type

		// send spurious parentURI to addNode
		SchemaTree schemaTree = new SchemaTree(assn, vocab);
		schemaTree.addNode("http://non.existent.com/uri", provTerm.label, provTerm.descr, provTerm.uri);
		
		Map<String, SchemaTree.Node> tree = schemaTree.getTree();
		SchemaTree.Node provNode = tree.get(provTerm.uri);
		assertTrue(provNode.parent == null);
	}
	
	private static void verifyNode(SchemaTree.Node provNode, ProvTerm provTerm)
	{
		assertTrue(provNode != null);
		assertTrue(StringUtils.equals(provNode.label, provTerm.label));
		assertTrue(StringUtils.equals(provNode.uri, provTerm.uri));
	}
}
