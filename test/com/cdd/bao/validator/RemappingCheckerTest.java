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

package com.cdd.bao.validator;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.junit.*;

/*
	Test for logic in RemappingChecker that vets remapping chains
*/

public class RemappingCheckerTest
{
	@Test
	public void testValidOneLinkChain()
	{
		// a => b (valid)
		Map<String, String> chain = new HashMap<>();
		chain.put("a", "b");
		try {RemappingChecker.validateRemappings(chain);}
		catch (IOException ioe) {assertTrue("No exception should occur here!", false);}
		assertTrue("Invalid chain leads to cycle.", true);
	}

	@Test
	public void testValidTwoLinkChain()
	{
		// a => b => c (valid)
		Map<String, String> chain = new HashMap<>();
		chain.put("a", "b");
		chain.put("b", "c");
		try {RemappingChecker.validateRemappings(chain);}
		catch (IOException ioe) {assertTrue("No exception should occur here!", false);}
		assertTrue("Invalid chain leads to cycle.", true);
	}

	@Test(expected = IOException.class)
	public void testTwoChainsWithOneInvalid() throws IOException
	{
		// a => b => c (valid)
		// d => e => d (invalid)
		Map<String, String> chain = new HashMap<>();
		chain.put("a", "b");
		chain.put("b", "c");
		chain.put("d", "e");
		chain.put("e", "d");
		try {RemappingChecker.validateRemappings(chain);}
		catch (IOException ioe)
		{
			assertTrue("Should detect cycle in input chains and throw IOException.", true);
			throw ioe;
		}
		assertTrue("Should detect cycle in input chains and throw IOException.", false);
	}

	@Test(expected = IOException.class)
	public void testInvalidOneLinkChain() throws IOException
	{
		// a => a (invalid)
		Map<String, String> chain = new HashMap<>();
		chain.put("a", "a");
		try {RemappingChecker.validateRemappings(chain);}
		catch (IOException ioe)
		{
			assertTrue("Should detect cycle in input chain and throw IOException.", true);
			throw ioe;
		}
		assertTrue("Should detect cycle in input chain and throw IOException.", false);
	}

	@Test(expected = IOException.class)
	public void testChainWithNullTerminal() throws IOException
	{
		// a => null (invalid)
		Map<String, String> chain = new HashMap<>();
		chain.put("a", null);
		try {RemappingChecker.validateRemappings(chain);}
		catch (IOException ioe)
		{
			assertTrue("Null-terminated chain was not detected!", true);
			throw ioe;
		}
		assertTrue("Null-terminated chain was not detected!", false);
	}

	@Test(expected = IOException.class)
	public void testMultiLinkChainWithCycle() throws IOException
	{
		// a => b => c => d => e => c (invalid)
		Map<String, String> chain = new HashMap<>();
		chain.put("a", "b");
		chain.put("b", "c");
		chain.put("c", "d");
		chain.put("d", "e");
		chain.put("e", "c");
		try {RemappingChecker.validateRemappings(chain);}
		catch (IOException ioe)
		{
			assertTrue("Should detect cycle in input chains and throw IOException.", true);
			throw ioe;
		}
		assertTrue("Should detect cycle in input chains and throw IOException.", false);
	}
}
