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

/*
	Test for BAO vocabularies (imported from the template project)
*/

public class AxiomVocabTest
{
	@Test
	public void testAxiomRules() throws Exception
	{
		AxiomVocab av1 = new AxiomVocab();
		
		AxiomVocab.Type[] types = AxiomVocab.Type.values();
		String[] uris =
		{
			ModelSchema.expandPrefix("bao:FNORD0000001"),
			ModelSchema.expandPrefix("bao:FNORD0000002"),
			ModelSchema.expandPrefix("bao:FNORD0000003"),
			ModelSchema.expandPrefix("obo:FNORD0000004"),
			ModelSchema.expandPrefix("rdf:FNORD0000005")
		};
		
		for (int n = 0, p = 0; n < 100; n++)
		{
			AxiomVocab.Rule r = new AxiomVocab.Rule();
			r.type = types[n % types.length];
			r.subject = new AxiomVocab.Term(uris[p++ % uris.length], n % 2 == 0);
			r.impact = new AxiomVocab.Term[n % 5 + 1];
			for (int i = 0; i < r.impact.length; i++) r.impact[i] = new AxiomVocab.Term(uris[p++ % uris.length], (n + i) % 2 == 0);
			
			assertEquals(r, r); // sanity test the equality operator
			
			av1.addRule(r);
		}
		
		assertEquals(100, av1.numRules());
		
		ByteArrayOutputStream ostr = new ByteArrayOutputStream();
		av1.serialise(ostr);
		ostr.flush();
		ByteArrayInputStream istr = new ByteArrayInputStream(ostr.toByteArray());
		AxiomVocab av2 = AxiomVocab.deserialise(istr);
		istr.close();
		
		// Util.writeln("# rules: input=" + av1.numRules() + ", output=" + av2.numRules());
		assert av1.numRules() == av2.numRules();
		for (int n = 0; n < av1.numRules(); n++)
		{
			AxiomVocab.Rule r1 = av1.getRule(n), r2 = av2.getRule(n);
			assertEquals(r1, r2);
			assertEquals(r2, r1);
		}
	}
}
