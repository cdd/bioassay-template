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

import static org.junit.Assert.assertTrue;

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
		StringBuilder sb = new StringBuilder();
		sb.append(System.getProperty("user.dir"));
		sb.append(File.separator);
		sb.append("test");
		sb.append(File.separator);
		sb.append("testData");
		testDataDir = new File(sb.toString());
	}

	@Test
	public void testLoadFooBarOntology()
	{
		Vocabulary vocab = new Vocabulary();
		try { vocab.load(testDataDir.getCanonicalPath(), null); }
		catch (IOException ioe) {}
		assertTrue("Test", true);
	}
}
