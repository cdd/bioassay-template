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

import com.cdd.bao.template.Schema.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import org.apache.commons.lang3.*;
import org.apache.commons.lang3.tuple.*;
import org.junit.*;

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
	public void testAddNode()
	{
		Schema.Group group = schema.getRoot();
		Assignment assn = group.assignments.get(1); // bioassay type

		SchemaTree schemaTree = new SchemaTree(assn, vocab);
		int treeSize = schemaTree.getTree().size();
		verifyTreeStructure(schemaTree, treeSize);

		// verify new node creation for provisional term
		SchemaTree.Node newNode = schemaTree.addNode(provTerm.parentURI, provTerm.label, provTerm.descr, provTerm.uri);
		assertTrue(newNode != null);

		verifyTreeStructure(schemaTree, treeSize + 1);

		// verify tree
		SchemaTree.Node provNode = schemaTree.getTree().get(provTerm.uri);
		verifyNode(provNode, provTerm);

		// verify node in flat array
		verifyNodeInCollection(provTerm, schemaTree.getFlat());

		// verify node in list array
		verifyNodeInCollection(provTerm, schemaTree.getList());
	}

	@Test
	public void testAddNodeWithMissingParent()
	{
		Schema.Group group = schema.getRoot();
		Assignment assn = group.assignments.get(1); // bioassay type

		// send spurious parentURI to addNode
		SchemaTree schemaTree = new SchemaTree(assn, vocab);
		int treeSize = schemaTree.getTree().size();
		verifyTreeStructure(schemaTree, treeSize);

		schemaTree.addNode("http://non.existent.com/uri", provTerm.label, provTerm.descr, provTerm.uri);
		
		SchemaTree.Node provNode = schemaTree.getTree().get(provTerm.uri);
		assertTrue(provNode == null);
		
		// tree is unchanged
		verifyTreeStructure(schemaTree, treeSize);
	}
	
	@Test
	public void testAddNodesWithCycle()
	{
		Schema.Group group = schema.getRoot();
		Assignment assn = group.assignments.get(1); // bioassay type
		SchemaTree schemaTree = new SchemaTree(assn, vocab);
		int treeSize = schemaTree.getTree().size();
		verifyTreeStructure(schemaTree, treeSize);

		// list of provisional terms will contain a cycle
		ProvTerm provTerm2 = new ProvTerm();
		provTerm2.parentURI = "http://www.bioassayontology.org/bat#provisional_test";
		provTerm2.label = "provisional_test2";
		provTerm2.descr = "Test logic for cycle in list of provisional terms.";
		provTerm2.uri = "http://www.bioassayontology.org/bat#provisional_test2";
		
		// this create a loop
		provTerm.parentURI = "http://www.bioassayontology.org/bat#provisional_test2";

		List<Pair<String, SchemaTree.Node>> candidates = new ArrayList<>();
		for (ProvTerm pt : new ProvTerm[]{provTerm, provTerm2})
		{
			SchemaTree.Node stNode = new SchemaTree.Node();
			stNode.uri = pt.uri;
			stNode.label = pt.label;
			stNode.descr = pt.descr;
			candidates.add(Pair.of(pt.parentURI, stNode));
		}
		
		List<SchemaTree.Node> added = schemaTree.addNodes(candidates);
		assertTrue(added.isEmpty());

		// tree is unchanged
		verifyTreeStructure(schemaTree, treeSize);
	}
	
	@Test
	public void testChildCount()
	{
		// get tree for bioassay type
		Assignment assn = schema.getRoot().assignments.get(1);
		SchemaTree schemaTree = new SchemaTree(assn, vocab);
		int treeSize = schemaTree.getTree().size();
		verifyTreeStructure(schemaTree, treeSize);
		
		// get a parent node
		SchemaTree.Node parent = schemaTree.getNode(provTerm.parentURI);
		long parentChildCount = parent.childCount;

		// add branch with a few nodes 
		SchemaTree.Node node1 = schemaTree.addNode(parent.uri, "label1", "descr1", "uri1");
		verifyTreeStructure(schemaTree, treeSize + 1);
		assertEquals(parentChildCount + 1, schemaTree.getNode(parent.uri).childCount);
		assertEquals(0, node1.childCount);

		schemaTree.addNode(node1.uri, "label11", "descr11", "uri11");
		schemaTree.addNode(node1.uri, "label12", "descr12", "uri12");
		schemaTree.addNode("uri11", "label112", "descr112", "uri111");
		verifyTreeStructure(schemaTree, treeSize + 4);
		assertEquals(parentChildCount + 4, schemaTree.getNode(parent.uri).childCount);
		assertEquals(3, schemaTree.getNode("uri1").childCount);
		assertEquals(1, schemaTree.getNode("uri11").childCount);
		assertEquals(0, schemaTree.getNode("uri111").childCount);
		assertEquals(0, schemaTree.getNode("uri12").childCount);
		
		// remove one of the new terminal nodes
		schemaTree.removeNode("uri12");
		verifyTreeStructure(schemaTree, treeSize + 3);
		assertEquals(parentChildCount + 3, schemaTree.getNode(parent.uri).childCount);
		assertEquals(2, schemaTree.getNode("uri1").childCount);
		assertEquals(1, schemaTree.getNode("uri11").childCount);
		assertEquals(0, schemaTree.getNode("uri111").childCount);
		
		// remove branch at once
		schemaTree.removeNode("uri1");
		verifyTreeStructure(schemaTree, treeSize);
		assertEquals(parentChildCount, schemaTree.getNode(parent.uri).childCount);
	}
	
	//--- private methods -------------------
	
	private static void verifyTreeStructure(SchemaTree tree, int expectedSize)
	{
		assertThat(tree.getTree().size(), is(expectedSize));
		assertThat(tree.getFlat().length, is(expectedSize));
		assertThat(tree.getList().length, is(expectedSize));

		// assert that the child count is correct
		for (SchemaTree.Node node : tree.getFlat())
			assertThat(node.childCount, is(countNodes(node) - 1));
	}
	
	// return the number of nodes in branch starting at parent (incl. parent)
	private static int countNodes(SchemaTree.Node parent)
	{
		int count = 1;
		for (SchemaTree.Node child : parent.children) count += countNodes(child);
		return count;
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
