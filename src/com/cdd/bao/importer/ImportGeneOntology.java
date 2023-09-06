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

import com.cdd.bao.template.ModelSchema;
import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;

/*
	Miscellaneous functionality: loads up the whole Gene Ontology OWL file (which is very large) and pulls out just the top level
	hierarchy that is of interest to template creation. Resubclasses the root nodes so that they are attached to the BAO hierarchy.
*/

public class ImportGeneOntology extends OntologyImporter
{
	private Tree rootBioProc, rootCellComp, rootMolFunc;

	@Override
	public void buildTree() throws IOException
	{
		try 
		{
			Tree root = new Tree(null, ModelSchema.PFX_OBO + "BFO_0000001");
			rootBioProc = new Tree(root, ModelSchema.PFX_OBO + "GO_0008150");
			rootCellComp = new Tree(root, ModelSchema.PFX_OBO + "GO_0008372");
			rootMolFunc = new Tree(root, ModelSchema.PFX_OBO + "GO_0005554");

			rootBioProc.label = "biological process";
			rootCellComp.label = "cellular component";
			rootMolFunc.label = "molecular function";

			buildBranch(rootBioProc);
			buildBranch(rootCellComp);
			buildBranch(rootMolFunc);
			
			//showBranch(rootBioProc, 0);
			//showBranch(rootCellComp, 0);
			//showBranch(rootMolFunc, 0);
			
			assertBranch(rootBioProc);
			assertBranch(rootCellComp);
			assertBranch(rootMolFunc);
		}
		catch (Exception ex) {throw new IOException("Failed to parse schema", ex);}
	}
}
