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

import org.apache.commons.lang3.*;
import org.junit.*;

import com.cdd.bao.template.Vocabulary.*;
import com.cdd.bao.util.*;

/*
	Test logic that loads ontologies.
*/

public class VocabularyTest
{
	@Test
	public void testRemap() throws IOException
	{
		File testRemapDir = new File(System.getProperty("user.dir") + "/build/test/testData/remapTo");
		Vocabulary vocab = new Vocabulary();
		vocab.load(testRemapDir.getCanonicalPath(), null);

		boolean foundClassHier = false;
		Vocabulary.Hierarchy hier = vocab.getValueHierarchy();
		for (Vocabulary.Branch branch : hier.rootBranches)
		{
			assertTrue("root label should contain \"Foo\"", (branch.label != null && branch.label.contains("Foo")));
			assertTrue("remapping should result in a single child branch", branch.children.size() == 1);

			Vocabulary.Branch child = branch.children.get(0);
			assertTrue("child label should contain \"Bar\"", (child.label != null && child.label.contains("Bar")));

			foundClassHier = true;
		}
		if (!foundClassHier) throw new AssertionError("Expected class hierarchy NOT FOUND!");
	}

	// XXX: this unit test does not yet work
	@Test
	public void testEquivalence() throws IOException
	{
		File testEquivDir = new File(System.getProperty("user.dir") + "/build/test/testData/equivalenceClass");
		Vocabulary vocab = new Vocabulary();
		vocab.load(testEquivDir.getCanonicalPath(), null);

		boolean foundClassHier = false;
		Vocabulary.Hierarchy hier = vocab.getValueHierarchy();
		for (Vocabulary.Branch branch : hier.rootBranches)
		{
			Util.writeln("branch.label=" + branch.label + "; branch.children.size=" + branch.children.size());
			if (branch.label != null && branch.label.equals("A1"))
			{
				// assertTrue("equivalence should result in a single parent", branch.children.size() == 2);
				Vocabulary.Branch c1 = branch.children.get(0);
				Util.writeln("c1.label=" + c1.label);
				// Vocabulary.Branch c2 = branch.children.get(1);
				//assertTrue("B1 and B2 should have same parent",
				//	(c1.label != null && c1.label.equals("B1") && c2.label != null && c2.label.equals("B2")));

				foundClassHier = true;
			}
		}
		if (!foundClassHier) throw new AssertionError("Expected class hierarchy NOT FOUND!");
	}

	@Test
	public void testNotSubClass() throws IOException
	{
		File testEquivDir = new File(System.getProperty("user.dir") + "/build/test/testData/notSubClass");
		Vocabulary vocab = new Vocabulary();
		vocab.load(testEquivDir.getCanonicalPath(), null);

		String[] allURIs = vocab.getAllURIs();
		assertTrue("In total, there should only be 2 toplevel, unrelated classes.", allURIs.length == 2);
		for (String uri : allURIs)
			assertTrue("Unrecognized uri \"" + uri + "\"!", StringUtils.endsWith(uri, "A1") || StringUtils.endsWith(uri, "B1"));

		Vocabulary.Hierarchy hier = vocab.getValueHierarchy();
		assertTrue("Found hierarchy!", hier.rootBranches.size() == 0);
	}

	@Test
	public void testFinalLabel() throws IOException
	{
		File testEquivDir = new File(System.getProperty("user.dir") + "/build/test/testData/finalLabel");
		Vocabulary vocab = new Vocabulary();
		vocab.load(testEquivDir.getCanonicalPath(), null);

		String[] allURIs = vocab.getAllURIs();
		assertTrue("In total, there should only be one toplevel class.", allURIs.length == 1);

		String label = vocab.getLabel(allURIs[0]);
		assertTrue("Expected final label to be \"Apple Bentley Charlie\"", label.equals("Apple Bentley Charlie"));
	}

	@Test
	public void testEliminated() throws IOException
	{
		File testEquivDir = new File(System.getProperty("user.dir") + "/build/test/testData/eliminated");
		Vocabulary vocab = new Vocabulary();
		vocab.load(testEquivDir.getCanonicalPath(), null);

		String[] allURIs = vocab.getAllURIs();
		assertTrue("Only one term should exist.", allURIs != null && allURIs.length == 1);
		
		String label = vocab.getLabel(allURIs[0]);
		assertTrue("Did not find the right label for the term.", label.equals("Not Eliminated"));
	}

	@Test
	public void testPubChemDirectives() throws IOException
	{
		File testEquivDir = new File(System.getProperty("user.dir") + "/build/test/testData/pubchem");
		Vocabulary vocab = new Vocabulary();
		vocab.load(testEquivDir.getCanonicalPath(), null);

		String[] allURIs = vocab.getAllURIs();
		assertTrue("Should find two URIs, one for pubchemImport and one for pubchemSource.", allURIs.length == 2);

		boolean foundImport = false, foundSource = false;
		for (String uri : allURIs)
		{
			String label = vocab.getLabel(uri);
			if (label.contains("Pubchem Import"))
			{
				assertTrue("pubchemImport boolean should be true!", vocab.getPubChemImport(uri) == true);
				foundImport = true;
			}
			else if (label.contains("Pubchem Source"))
			{
				assertTrue("pubchemSource should be \"JUnit\"!", vocab.getPubChemSource(uri).equals("JUnit"));
				foundSource = true;
			}
		}
		if (!foundImport || !foundSource)
			throw new AssertionError("Did not find terms for both pubchemImport and pubchemSource!");
	}

	@Test
	public void testSubPropertyOf() throws IOException
	{
		File testEquivDir = new File(System.getProperty("user.dir") + "/build/test/testData/subPropertyOf");
		Vocabulary vocab = new Vocabulary();
		vocab.load(testEquivDir.getCanonicalPath(), null);

		boolean foundClassHier = false;
		Vocabulary.Hierarchy hier = vocab.getPropertyHierarchy();
		assertTrue("Should find exactly one branch hierarchy.", hier.rootBranches.size() == 1);

		for (Vocabulary.Branch branch : hier.rootBranches)
		{
			assertTrue("root label should equal \"A1\"", branch.label != null && branch.label.startsWith("A1"));
			assertTrue("A1 hierarchy should contain single child.", branch.children.size() == 1);

			Vocabulary.Branch child = branch.children.get(0);
			assertTrue("child label should equal \"B1\"", child.label != null && child.label.startsWith("B1"));

			foundClassHier = true;
		}
		if (!foundClassHier) throw new AssertionError("Expected class hierarchy NOT FOUND!");
	}

	@Test
	public void testAltLabels() throws IOException
	{
		File testEquivDir = new File(System.getProperty("user.dir") + "/build/test/testData/altLabels");
		Vocabulary vocab = new Vocabulary();
		vocab.load(testEquivDir.getCanonicalPath(), null);

		String[] allURIs = vocab.getAllURIs();
		assertTrue("Should find exactly one URI.", allURIs != null && allURIs.length == 1);

		String[] altLabels = vocab.getAltLabels(allURIs[0]);
		assertTrue("Should find four alternative labels.", altLabels != null && altLabels.length == 4);

		for (String curLabel : new String[]{"AltLabel #1", "AltLabel #2", "AltLabel #3", "AltLabel #4"})
			assertTrue("Alternate label \"" + curLabel + "\" NOT FOUND!", ArrayUtils.contains(altLabels, curLabel));
	}
}
