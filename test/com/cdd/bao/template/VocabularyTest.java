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

import com.cdd.bao.util.*;

/*
	Test logic that loads ontologies
*/

public class VocabularyTest
{
	private File testDataDir;
	
	@Before
	public void setup()
	{
		testDataDir = new File(System.getProperty("user.dir") + "/build/test/testData");
	}

	@Test
	public void testLoadTestOntology() throws IOException
	{
		Vocabulary vocab = new Vocabulary();
		vocab.load(testDataDir.getCanonicalPath(), null);
		
		Vocabulary.Hierarchy hier = vocab.getValueHierarchy();
		for (Vocabulary.Branch branch : hier.rootBranches)
		{
			assertTrue("root label should contain \"Foo\"", (branch.label != null && branch.label.contains("Foo")));
			assertTrue("remapping should result in a single child branch", branch.children.size() == 1);

			for (Vocabulary.Branch child : branch.children)
			{
				assertTrue("root should have child branch", child.label != null);
				assertTrue("child label should contain \"Bar\"", (child.label.contains("Bar")));
			}
		}
		assertTrue("Test", true);
	}
}
