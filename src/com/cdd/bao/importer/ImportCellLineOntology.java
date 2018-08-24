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
