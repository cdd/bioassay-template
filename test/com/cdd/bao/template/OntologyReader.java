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
