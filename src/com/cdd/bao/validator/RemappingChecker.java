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

import java.io.*;
import java.util.*;

/*
	Use logic in this class to detect cycles in remapping directives. 
*/

public class RemappingChecker
{
	public static void validateRemappings(Map<String, String> remappings) throws IOException
	{
		// prohibit null-terminated chains
		if (remappings.values().contains(null)) throw new IOException("Null-terminated remappings are not allowed.");

		List<String> dependencies = new ArrayList<>();
		for (String uri : remappings.keySet())
		{
			dependencies.clear();
			while (remappings.containsKey(uri))
			{
				dependencies.add(uri); // add URI to dependency chain
				if ((uri = remappings.get(uri)) == null) break; 
				if (dependencies.contains(uri)) throw new IOException(buildMessage(dependencies));
			}
		}
	}

	private static String buildMessage(List<String> terms)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Cannot load vocabulary because the following remapped terms lead to a cycle: ");

		boolean appendCommand = false;
		for (String t : terms)
		{
			if (appendCommand) sb.append(" => ");
			sb.append(t);
			appendCommand = true;
		}
		return sb.toString();
	}
}
