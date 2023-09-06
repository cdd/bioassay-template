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

package com.cdd.bao.importer;

import java.io.*;

import org.apache.jena.rdf.model.*;

import com.cdd.bao.template.ModelSchema;

/*
	Load Cell Line Ontology from OWL file and export the sub-tree rooted at CLO_0000001.
*/

public class ImportCellLineOntology extends OntologyImporter
{
	private Tree rootCLO;

	@Override
	public void buildTree() throws IOException
	{
		try 
		{
			rootCLO = new Tree(null, ModelSchema.PFX_OBO + "CLO_0000001");
			rootCLO.label = "cell line ontology";

			buildBranch(rootCLO);
			assertBranch(rootCLO);
		}
		catch (Exception ex) {throw new IOException("Failed to parse schema", ex);}
	}
}
