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
import java.util.stream.*;

import org.apache.commons.lang3.*;
import org.apache.commons.lang3.tuple.*;
import org.junit.*;

import com.cdd.bao.template.*;
import com.cdd.bao.template.Schema.*;
import com.cdd.bao.template.Vocabulary.*;
import com.cdd.bao.util.*;

public class SchemaTreeTest extends OntologyReader
{
	private static class ProvTerm
	{
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
		int flatLength = schemaTree.getFlat().length;
		int listLength = schemaTree.getList().length;

		// verify new node creation for provisional term
		SchemaTree.Node newNode = schemaTree.addNode(provTerm.parentURI, provTerm.label, provTerm.descr, provTerm.uri);
		assertTrue(newNode != null);

		// verify tree
		Map<String, SchemaTree.Node> tree = schemaTree.getTree();
		SchemaTree.Node provNode = tree.get(provTerm.uri);
		verifyNode(provNode, provTerm);

		// verify node in flat array
		assertTrue((flatLength + 1) == schemaTree.getFlat().length);
		verifyNodeInCollection(provTerm, schemaTree.getFlat());

		// verify node in list array
		assertTrue((listLength + 1) == schemaTree.getList().length);
		verifyNodeInCollection(provTerm, schemaTree.getList());
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
		assertTrue(provNode == null);
	}
	
	@Test
	public void testAddNodesWithCycle()
	{
		Schema.Group group = schema.getRoot();
		Assignment assn = group.assignments.get(1); // bioassay type
		SchemaTree schemaTree = new SchemaTree(assn, vocab);

		// list of provisional terms will contain a cycle
		ProvTerm provTerm2 = new ProvTerm();
		provTerm2.parentURI = "http://www.bioassayontology.org/bat#provisional_test";
		provTerm2.label = "provisional_test2";
		provTerm2.descr = "Test logic for cycle in list of provisional terms.";
		provTerm2.uri = "http://www.bioassayontology.org/bat#provisional_test2";
		
		// this create a loop
		provTerm.parentURI = "http://www.bioassayontology.org/bat#provisional_test2";

		List<Pair<String, SchemaTree.Node>> candidates = new ArrayList<>();
		List<SchemaTree.Node> nodes = new ArrayList<>();
		for (ProvTerm pt : new ProvTerm[]{provTerm, provTerm2})
		{
			SchemaTree.Node stNode = new SchemaTree.Node();
			stNode.uri = pt.uri;
			stNode.label = pt.label;
			stNode.descr = pt.descr;
			candidates.add(Pair.of(pt.parentURI, stNode));
		}
		
		List<SchemaTree.Node> added = schemaTree.addNodes(candidates);
		assertTrue(added.size() > 0);
	}
	
	private void verifyNode(SchemaTree.Node provNode, ProvTerm provTerm)
	{
		assertTrue(provNode != null);
		assertTrue(StringUtils.equals(provNode.label, provTerm.label));
		assertTrue(StringUtils.equals(provNode.uri, provTerm.uri));
	}

	private void verifyNodeInCollection(ProvTerm provTerm, SchemaTree.Node[] arrNodes)
	{
		List<SchemaTree.Node> collNodes = Arrays.asList(arrNodes);

		List<SchemaTree.Node> found = collNodes.stream()
			.filter(node -> StringUtils.equals(node.uri, provTerm.uri))
			.collect(Collectors.toList());
		assertEquals("Term should be found only once in the collection.", 1, found.size());

		verifyNode(found.get(0), provTerm);
	}
}
