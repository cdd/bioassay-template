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

import java.io.*;

import org.junit.*;

import com.cdd.bao.axioms.*;

/*
	Test for BAO vocabularies (imported from the template project)
*/

public class AxiomVocabTest
{
	private String uri1 = ModelSchema.expandPrefix("bao:FNORD0000001");
	private String uri2 = ModelSchema.expandPrefix("bao:FNORD0000002");
	private String uri3 = ModelSchema.expandPrefix("bao:FNORD0000003");
	private String uri4 = ModelSchema.expandPrefix("obo:FNORD0000004");
	private String uri5 = ModelSchema.expandPrefix("rdf:FNORD0000005");
	
	private AxiomVocab.Term term1 = new AxiomVocab.Term(uri1, true);
	private AxiomVocab.Term term2 = new AxiomVocab.Term(uri2, true);

	@Test
	public void testAxiomRules() throws Exception
	{
		AxiomVocab av1 = new AxiomVocab();

		AxiomVocab.Type[] types = AxiomVocab.Type.values();
		String[] uris = {uri1, uri2, uri3, uri4, uri5};

		for (int n = 0, p = 0; n < 100; n++)
		{
			AxiomVocab.Rule r = new AxiomVocab.Rule();
			r.type = types[n % types.length];
			r.subject = new AxiomVocab.Term(uris[p++ % uris.length], n % 2 == 0);
			r.impact = new AxiomVocab.Term[n % 5 + 1];
			for (int i = 0; i < r.impact.length; i++)
				r.impact[i] = new AxiomVocab.Term(uris[p++ % uris.length], (n + i) % 2 == 0);

			assertEquals(r, r); // sanity test the equality operator

			av1.addRule(r);
		}

		assertEquals(100, av1.numRules());

		byte[] serialisation;
		try (ByteArrayOutputStream ostr = new ByteArrayOutputStream())
		{
			av1.serialise(ostr);
			serialisation = ostr.toByteArray();
		}
		AxiomVocab av2;
		try (ByteArrayInputStream istr = new ByteArrayInputStream(serialisation))
		{
			av2 = AxiomVocab.deserialise(istr);
		}

		// Util.writeln("# rules: input=" + av1.numRules() + ", output=" + av2.numRules());
		assertEquals(av1.numRules(), av2.numRules());
		for (int n = 0; n < av1.numRules(); n++)
		{
			AxiomVocab.Rule r1 = av1.getRule(n), r2 = av2.getRule(n);
			assertEquals(r1, r2);
			assertEquals(r2, r1);
		}
	}

	@Test
	public void testRule()
	{
		AxiomVocab.Rule rule1 = new AxiomVocab.Rule(AxiomVocab.Type.LIMIT, term1);
		AxiomVocab.Rule rule2 = new AxiomVocab.Rule(AxiomVocab.Type.EXCLUDE, term1);
		/*AxiomVocab.Rule rule3 = new AxiomVocab.Rule(AxiomVocab.Type.REQUIRED, new AxiomVocab.Term(uri1, false),
				new AxiomVocab.Term[]{new AxiomVocab.Term(uri2, true), new AxiomVocab.Term(uri3, true)});
		AxiomVocab.Rule rule4 = new AxiomVocab.Rule(AxiomVocab.Type.BLANK, new AxiomVocab.Term(uri5, true));*/

		assertEquals("LIMIT type axiom; subject: [bao:FNORD0000001/true]impacts: [])", rule1.toString());
		assertEquals("EXCLUDE type axiom; subject: [bao:FNORD0000001/true]impacts: [])", rule2.toString());
		/*assertEquals("REQUIRED type axiom; subject: [bao:FNORD0000001/false]impacts: [bao:FNORD0000002/true,bao:FNORD0000003/true])", rule3.toString());
		assertEquals("BLANK type axiom; subject: [rdf:FNORD0000005/true]impacts: [])", rule4.toString());*/

		assertEquals("LIMIT type axiom; ", rule1.rulesFormatString());
		/*assertEquals("REQUIRED type axiom; " +
				"[bao:FNORD0000001/false]=>[bao:FNORD0000002/true]\n" +
				"[bao:FNORD0000001/false]=>[bao:FNORD0000003/true]\n", rule3.rulesFormatString());*/
		
		// test equality
		rule1 = new AxiomVocab.Rule(AxiomVocab.Type.LIMIT, term1);
		rule2 = new AxiomVocab.Rule(AxiomVocab.Type.LIMIT, term1);
		assertTrue(rule1.equals(rule2));
		assertEquals(rule1.hashCode(), rule2.hashCode());
		/*rule2.type = AxiomVocab.Type.BLANK;
		assertFalse(rule1.equals(rule2));
		assertNotEquals(rule1.hashCode(), rule2.hashCode());*/
		
		rule2 = new AxiomVocab.Rule(AxiomVocab.Type.LIMIT, null);
		assertFalse(rule1.equals(rule2));
		assertFalse(rule2.equals(rule1));
		assertNotEquals(rule1.hashCode(), rule2.hashCode());

		rule2 = new AxiomVocab.Rule(AxiomVocab.Type.LIMIT, term2);
		assertFalse(rule1.equals(rule2));
		assertFalse(rule2.equals(rule1));
		assertNotEquals(rule1.hashCode(), rule2.hashCode());

		rule1 = new AxiomVocab.Rule(AxiomVocab.Type.LIMIT, null);
		rule2 = new AxiomVocab.Rule(AxiomVocab.Type.LIMIT, null);
		assertTrue(rule1.equals(rule2));
		assertEquals(rule1.hashCode(), rule2.hashCode());
		
		/*rule1.impact = rule3.impact;
		assertFalse(rule1.equals(rule2));
		assertFalse(rule2.equals(rule1));
		assertNotEquals(rule1.hashCode(), rule2.hashCode());*/
		
		rule2.impact = new AxiomVocab.Term[]{new AxiomVocab.Term(uri1, true), new AxiomVocab.Term(uri3, true)};
		assertFalse(rule1.equals(rule2));
		assertFalse(rule2.equals(rule1));
		assertNotEquals(rule1.hashCode(), rule2.hashCode());
		
		rule1.impact = new AxiomVocab.Term[]{new AxiomVocab.Term(uri1, true), new AxiomVocab.Term(uri3, true)};
		assertTrue(rule1.equals(rule2));
		assertTrue(rule2.equals(rule1));
		assertEquals(rule1.hashCode(), rule2.hashCode());
	}

	@Test
	public void testType()
	{
		assertEquals(1, AxiomVocab.Type.LIMIT.raw());
		assertEquals(2, AxiomVocab.Type.EXCLUDE.raw());
		/*assertEquals(3, AxiomVocab.Type.BLANK.raw());
		assertEquals(4, AxiomVocab.Type.REQUIRED.raw());*/

		assertEquals(AxiomVocab.Type.LIMIT, AxiomVocab.Type.valueOf(1));
		assertEquals(AxiomVocab.Type.EXCLUDE, AxiomVocab.Type.valueOf(2));
		/*assertEquals(AxiomVocab.Type.BLANK, AxiomVocab.Type.valueOf(3));
		assertEquals(AxiomVocab.Type.REQUIRED, AxiomVocab.Type.valueOf(4));*/

		assertEquals(AxiomVocab.Type.LIMIT, AxiomVocab.Type.valueOf(0));
		assertEquals(AxiomVocab.Type.LIMIT, AxiomVocab.Type.valueOf(5));
	}

	@Test
	public void testTerm()
	{
		String valueURI = "http://www.bioassayontology.org/bao#BAO_0000008";
		AxiomVocab.Term term1 = new AxiomVocab.Term(valueURI, false);
		assertEquals("bao:BAO_0000008/false", term1.toString());

		AxiomVocab.Term term2 = new AxiomVocab.Term(valueURI, true);
		assertEquals("bao:BAO_0000008/true", term2.toString());

		// test equality 
		assertNotEquals(term1, term2);
		assertNotEquals(term2, term1);
		assertNotEquals(term1.hashCode(), term2.hashCode());

		term2.wholeBranch = false;
		assertEquals(term1, term2);
		assertEquals(term2, term1);
		assertEquals(term1.hashCode(), term2.hashCode());

		AxiomVocab.Term term3 = new AxiomVocab.Term(null, false);
		assertNotEquals(term1, term3);
		assertNotEquals(term3, term1);
		assertNotEquals(term1.hashCode(), term3.hashCode());
		term1.valueURI = null;
		assertEquals(term1.hashCode(), term3.hashCode());
	}
}
