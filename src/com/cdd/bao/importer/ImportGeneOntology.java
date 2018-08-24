/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2018 Collaborative Drug Discovery Inc.
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
