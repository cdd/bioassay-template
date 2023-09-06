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

import com.cdd.bao.template.Schema.*;

import static org.junit.Assert.*;

import org.junit.*;

/*
	Test for BAO Schema
*/

public class SchemaTest
{
	@Test
	public void testGroup()
	{
		Schema.Group obj1 = new Schema.Group(null, "name1", "groupURI1");
		Schema.Group obj2 = new Schema.Group(null, "name2", "groupURI2");
		expectEqual(obj1, obj1);
		expectDifferent(obj1, obj2);
		expectDifferent(obj1, null);
		assertClone(obj1, obj1.clone());

		obj1 = new Schema.Group(new Schema.Group(null, null), "name1");
		obj2 = new Schema.Group(new Schema.Group(null, null), "name1");
		expectEqual(obj1, obj2);
		assertClone(obj1, obj1.clone());

		obj1 = new Schema.Group(new Schema.Group(null, null), null, null);
		obj2 = new Schema.Group(new Schema.Group(null, null), null, null);
		expectEqual(obj1, obj2);
		assertClone(obj1, obj1.clone());

		obj1.name = "name";
		obj2.name = "name2";
		expectDifferent(obj1, obj2);
		obj2.name = "name";
		expectEqual(obj1, obj2);

		obj1.descr = "descr";
		obj2.descr = "descr2";
		expectDifferent(obj1, obj2);
		obj2.descr = "descr";
		expectEqual(obj1, obj2);

		obj1.groupURI = "groupURI";
		obj2.groupURI = "groupURI2";
		expectDifferent(obj1, obj2);
		obj2.groupURI = "groupURI";
		expectEqual(obj1, obj2);

		Assignment assn1 = new Assignment(null, "name", "propURI");
		Assignment assn2 = new Assignment(null, "name", "propURI");
		obj1.assignments.add(assn1);
		expectDifferent(obj1, obj2);
		obj2.assignments.add(assn2);
		expectEqual(obj1, obj2);

		Schema.Group subGroup = new Group(null, "subName");
		obj1.subGroups.add(subGroup);
		expectDifferent(obj1, obj2);
		obj2.subGroups.add(subGroup);
		expectEqual(obj1, obj2);

		assertClone(obj1, obj1.clone());
	}

	@Test
	public void testAssignment()
	{
		Schema.Group group = new Group(null, "parent");
		Schema.Assignment obj1 = new Schema.Assignment(group, "name1", "groupURI1");
		Schema.Assignment obj2 = new Schema.Assignment(group, null, null);
		expectEqual(obj1, obj1);
		expectEqual(obj2, obj2);
		expectDifferent(obj1, obj2);
		expectDifferent(obj1, null);
		assertClone(obj1, obj1.clone());

		obj1 = new Schema.Assignment(group, "name1", "groupURI1");
		obj2 = new Schema.Assignment(group, "name2", "groupURI2");
		expectEqual(obj1, obj1);
		expectDifferent(obj1, obj2);
		assertClone(obj1, obj1.clone());

		expectDifferent(obj1, new Schema.Assignment(group, "name1", "groupURI"));
		expectDifferent(obj1, new Schema.Assignment(group, "name", "groupURI1"));

		obj1 = new Schema.Assignment(group, "name1", "groupURI1");
		obj2 = new Schema.Assignment(group, "name1", "groupURI1");
		expectEqual(obj1, obj2);
		obj2.descr = "description";
		expectDifferent(obj1, obj2);

		obj2 = new Schema.Assignment(group, "name1", "groupURI1");
		obj2.suggestions = Schema.Suggestions.DISABLED;
		expectDifferent(obj1, obj2);

		obj2 = new Schema.Assignment(group, "name1", "groupURI1");
		obj2.values.add(new Schema.Value("uri", "name"));
		expectDifferent(obj1, obj2);
		obj1.values.add(new Schema.Value("uri", "name"));
		expectEqual(obj1, obj2);
		assertClone(obj1, obj1.clone());
	}

	@Test
	public void testGroupTreeStructure()
	{
		Schema.Group root = new Group(null, "root");
		Schema.Group g1 = new Group(root, "g1", "u1");
		Schema.Group g2 = new Group(g1, "g2", "u2");
		Schema.Group g3 = new Group(g2, "g3", "u3");
		Schema.Group g4 = new Group(g3, "g4", "u4");
		Schema.Assignment assn = new Schema.Assignment(g4, "assn", "propURI");
		Schema.Assignment assn1 = new Schema.Assignment(g1, "assn1", "propURI");

		assertArrayEquals(new String[]{"u3", "u2", "u1"}, g4.groupNest());
		assertArrayEquals(new String[]{"g3", "g2", "g1"}, g4.groupLabel());
		assertArrayEquals(new String[]{"u4", "u3", "u2", "u1"}, assn.groupNest());
		assertArrayEquals(new String[]{"g4", "g3", "g2", "g1"}, assn.groupLabel());
		assertArrayEquals(new String[]{"u1"}, assn1.groupNest());
		assertArrayEquals(new String[]{"g1"}, assn1.groupLabel());

		root = new Group(null, "root");
		g1 = new Group(root, "g1", "");
		g2 = new Group(g1, "g2", "u2");
		g3 = new Group(g2, "g3", "u3");
		g4 = new Group(g3, "g4", "u4");
		assn = new Schema.Assignment(g4, "assn", "propURI");
		assn1 = new Schema.Assignment(g1, "assn1", "propURI");

		assertArrayEquals("groups close to the root with missing URI are ignored", new String[]{"u3", "u2"}, g4.groupNest());
		assertArrayEquals("but not in the groupLabel", new String[]{"g3", "g2", "g1"}, g4.groupLabel());
		assertArrayEquals(new String[]{"u4", "u3", "u2"}, assn.groupNest());
		assertArrayEquals(new String[]{"g4", "g3", "g2", "g1"}, assn.groupLabel());
	}

	// @Test
	// currently not run
	public void testAssignmentGroupNest()
	{
		Schema.Group root = new Group(null, "root");

		Schema.Group group1 = new Group(root, "group1", "uriG1");
		Schema.Group group1a = new Group(group1, "subgroupA", "uriGA");
		Schema.Group group1b = new Group(group1, "subgroupB", "uriGB");

		Schema.Group group2 = new Group(root, "group2", "uriG2");
		Schema.Group group2a = new Group(group2, "subgroupA", "uriGA");
		Schema.Group group2b = new Group(group2, "subgroupB", "uriGB");

		Schema.Assignment obj1a = new Schema.Assignment(group1a, "name", "groupURI");
		Schema.Assignment obj1b = new Schema.Assignment(group1b, "name", "groupURI");
		Schema.Assignment obj2a = new Schema.Assignment(group2a, "name", "groupURI");
		Schema.Assignment obj2b = new Schema.Assignment(group2b, "name", "groupURI");

		System.out.println(obj1a.toString());
		System.out.println(obj1b.toString());
		System.out.println(obj2a.toString());
		System.out.println(obj2b.toString());

		expectEqual(obj1a, obj1a);
		expectEqual(obj1b, obj1b);
		expectEqual(obj2a, obj2a);
		expectEqual(obj2b, obj2b);

		System.out.println("fails here");
		expectDifferent("GA-G1 vs GA-G2", obj1a, obj2a);
		expectDifferent("GA-G1 vs GB-G1", obj1a, obj1b);
		expectDifferent("GA-G1 vs GB-G2", obj1a, obj2b);
	}

	@Test
	public void testValue()
	{
		Schema.Value obj1 = new Schema.Value("uri1", "name1");
		Schema.Value obj2 = new Schema.Value("uri2", "name2");
		expectEqual(obj1, obj1);
		expectDifferent(obj1, obj2);
		expectDifferent("Comparison with null", obj1, null);
		assertClone(obj1, obj1.clone());

		obj1 = new Schema.Value("uri1", "name1");
		obj2 = new Schema.Value("uri1", "name1");
		expectEqual(obj1, obj2);

		obj2.name = "name2";
		expectDifferent("Different name", obj1, obj2);

		obj2 = new Schema.Value("uri1", "name1");
		obj2.descr = "Longer description";
		expectDifferent("Different description", obj1, obj2);

		obj2 = new Schema.Value("uri1", "name1");
		obj2.spec = Schema.Specify.EXCLUDE;
		expectDifferent("Different specify", obj1, obj2);
	}

	@Test
	public void testAssay()
	{
		Schema.Assay obj1 = new Schema.Assay("name1");
		Schema.Assay obj2 = new Schema.Assay("uri2");
		expectEqual(obj1, obj1);
		expectDifferent(obj1, obj2);
		expectDifferent("Comparison with null", obj1, null);
		assertClone(obj1, obj1.clone());

		obj1 = new Schema.Assay("name1");
		obj2 = new Schema.Assay("name1");
		obj2.descr = "descr2";
		expectDifferent(obj1, obj2);

		obj2 = new Schema.Assay("name1");
		obj2.para = "para2";
		expectDifferent(obj1, obj2);

		obj2 = new Schema.Assay("name1");
		obj2.originURI = "originURI2";
		expectDifferent(obj1, obj2);

		Schema.Group group = new Schema.Group(null, "group");
		Schema.Assignment assn = new Schema.Assignment(group, "name1", "propURI");
		Schema.Annotation annot1 = new Schema.Annotation(assn, "annot1");
		Schema.Annotation annot2 = new Schema.Annotation(assn, "annot2");
		obj2 = new Schema.Assay("name1");
		obj2.annotations.add(annot1);
		expectDifferent(obj1, obj2);
		obj1.annotations.add(new Schema.Annotation(assn, "annot1"));
		expectEqual(obj1, obj2);
		obj2.annotations.clear();
		obj2.annotations.add(annot2);
		expectDifferent(obj1, obj2);

		// order of annotations is not relevant
		obj1.annotations.clear();
		obj1.annotations.add(annot1);
		obj1.annotations.add(annot2);
		obj2.annotations.clear();
		obj2.annotations.add(annot2);
		obj2.annotations.add(annot1);
		expectEqual(obj1, obj2);

		assertClone(obj1, obj1.clone());
	}

	@Test
	public void testAnnotation()
	{
		// empty annotation
		expectEqual(new Schema.Annotation(), new Schema.Annotation());

		// different assignments
		Schema.Assignment assn1 = new Schema.Assignment(new Schema.Group(null, "group"), "name1", "propURI");
		Schema.Assignment assn2 = new Schema.Assignment(new Schema.Group(null, "group"), "name2", "propURI");
		Schema.Annotation obj1 = new Schema.Annotation(assn1, "literal");
		expectDifferent(obj1, new Schema.Annotation());
		assertClone(obj1, obj1.clone());

		expectEqual(obj1, new Schema.Annotation(assn1, "literal"));
		expectDifferent(obj1, new Schema.Annotation(assn2, "literal"));

		Group g1a = new Schema.Group(null, "g1");
		Group g2a = new Schema.Group(g1a, "g2");
		Group g1b = new Schema.Group(null, "g1");
		Group g2b = new Schema.Group(g1b, "g2");

		obj1 = new Schema.Annotation(new Schema.Assignment(g2a, "name", "propURI"), "literal");
		Schema.Annotation obj2 = new Schema.Annotation(new Schema.Assignment(g1b, "name", "propURI"), "literal");
		expectDifferent(obj1, obj2);
		obj2 = new Schema.Annotation(new Schema.Assignment(g2b, "name", "propURI"), "literal");
		expectEqual(obj1, obj2);

		obj1 = new Schema.Annotation(new Schema.Assignment(g1a, "name", "propURI"), "literal");
		obj2 = new Schema.Annotation(new Schema.Assignment(g1b, "name", "propURI"), "literal");
		expectEqual(obj1, obj2);
		obj2 = new Schema.Annotation(new Schema.Assignment(g2b, "name", "propURI"), "literal");
		expectDifferent(obj1, obj2);

		assertClone(obj1, obj1.clone());

		obj1 = new Schema.Annotation(assn1, new Schema.Value("uri", "name"));
		obj2 = new Schema.Annotation(assn1, "literal");
		expectDifferent(obj1, obj2);

		obj1 = new Schema.Annotation(assn1, new Schema.Value("uri", "name"));
		obj2 = new Schema.Annotation(assn1, new Schema.Value("uri", "name2"));
		expectDifferent(obj1, obj2);

		obj1 = new Schema.Annotation(assn1, new Schema.Value("uri", "name"));
		obj2 = new Schema.Annotation(assn1, new Schema.Value("uri", "name"));
		expectEqual(obj1, obj2);

		assertClone(obj1, obj1.clone());
	}

	@Test
	public void testSchema()
	{
		Schema obj1 = new Schema();
		expectEqual(obj1, obj1);
		expectDifferent("Comparison with null", obj1, null);
		Schema obj2 = new Schema();
		expectEqual(obj1, obj2);

		// root
		obj1.setRoot(new Schema.Group(null, "group"));
		expectDifferent(obj1, obj2);
		obj2.setRoot(new Schema.Group(null, "group"));
		expectEqual(obj1, obj2);

		assertClone(obj1, obj1.clone());
	}

	@Test
	public void testDeepGroup()
	{
		// regression test for issue 19
		Group g1a = new Schema.Group(null, "g1", "uri1");
		Group g2a = new Schema.Group(g1a, "g2", "uri2");
		Group g3a = new Schema.Group(g2a, "g3", "uri3");

		Schema.Assignment assn2 = new Schema.Assignment(g2a, "name", "propURI");
		assertArrayEquals(new String[]{"uri2"}, assn2.groupNest());
		assertArrayEquals(new String[]{"g2"}, assn2.groupLabel());

		Schema.Assignment assn3 = new Schema.Assignment(g3a, "name", "propURI");
		assertArrayEquals(new String[]{"uri3", "uri2"}, assn3.groupNest());
		assertArrayEquals(new String[]{"g3", "g2"}, assn3.groupLabel());

		Schema.Annotation annot2 = new Schema.Annotation(assn2, "literal");
		assertEquals("name\ng2\ng1\nliteral\n", annot2.keyString());
		Schema.Annotation annot3 = new Schema.Annotation(assn3, "literal");
		assertEquals("name\ng3\ng2\ng1\nliteral\n", annot3.keyString());
	}
	
	@Test
	public void testGroupNestEqual()
	{
		String[] GROUPNEST0 = {"http://something/foo"};
		String[] GROUPNEST1 = {"http://something/foo@1"};
		String[] GROUPNEST2 = {"http://something/foo@2"};
	
		assertTrue(Schema.sameGroupNest(GROUPNEST0, GROUPNEST0));
		assertTrue(Schema.sameGroupNest(GROUPNEST0, GROUPNEST1));
		assertTrue(Schema.sameGroupNest(GROUPNEST1, GROUPNEST1));
		assertFalse(Schema.sameGroupNest(GROUPNEST1, GROUPNEST2));
		assertTrue(Schema.sameGroupNest(GROUPNEST2, GROUPNEST2));
	}

	// ------------ private methods ------------

	private void expectEqual(Object obj1, Object obj2)
	{
		assertTrue(obj1.equals(obj2));
		assertTrue(obj2.equals(obj1));
		assertEquals(obj1.hashCode(), obj2.hashCode());
	}

	private void expectDifferent(String msg, Object obj1, Object obj2)
	{
		assertFalse(msg, obj1.equals(obj2));
		if (obj2 != null)
		{
			assertFalse(msg, obj2.equals(obj1));
			assertNotEquals(msg, obj1.hashCode(), obj2.hashCode());
		}
	}

	private void expectDifferent(Object obj1, Object obj2)
	{
		assertFalse(obj1.equals(obj2));
		if (obj2 != null)
		{
			assertFalse(obj2.equals(obj1));
			assertNotEquals(obj1.hashCode(), obj2.hashCode());
		}
	}

	private void assertClone(Object obj, Object clone)
	{
		assertEquals(obj.getClass(), clone.getClass());
		assertTrue("Direction 1", obj.equals(clone));
		assertTrue("Direction 2", clone.equals(obj));
	}
}
