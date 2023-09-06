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

import java.io.*;
import java.util.*;

/*
	Tests that load ontologies defined in TTL or OWL files should inherit this class. 
 */

public class OntologyReader
{
	public String[] getPathsForTests(String[] fnames) throws IOException
	{
		// get list of test ontologies and vet their existence
		List<String> testOntologies = new ArrayList<>();
		for (String fn : fnames)
		{
			File testOntology = new File(getClass().getClassLoader().getResource("testData/" + fn).getFile());
			if (!testOntology.exists())
				throw new IOException("Test ontology \"" + testOntology.getCanonicalPath() + "\" does not exist");
			testOntologies.add(testOntology.getCanonicalPath());
		}

		return testOntologies.toArray(new String[0]);
	}
}
