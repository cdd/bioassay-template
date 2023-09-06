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
		catch (IOException ioe) {throw new AssertionError("Remappings are not valid!", ioe);}
		assertTrue("Remappings should be valid.", true);
	}

	@Test
	public void testValidTwoLinkChain()
	{
		// a => b => c (valid)
		Map<String, String> chain = new HashMap<>();
		chain.put("a", "b");
		chain.put("b", "c");
		try {RemappingChecker.validateRemappings(chain);}
		catch (IOException ioe) {throw new AssertionError("Remappings are not valid!", ioe);}
		assertTrue("Remappings should be valid.", true);
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
			assertTrue("Should detect cycle in remappings and throw IOException.", true);
			throw ioe;
		}
		throw new AssertionError("Remappings SHOULD NOT be valid!");
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
		throw new AssertionError("Remappings SHOULD NOT be valid!");
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
			assertTrue("Should detect null-terminated chain!", true);
			throw ioe;
		}
		throw new AssertionError("Remappings SHOULD NOT be valid!");
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
		throw new AssertionError("Remappings SHOULD NOT be valid!");
	}
}
