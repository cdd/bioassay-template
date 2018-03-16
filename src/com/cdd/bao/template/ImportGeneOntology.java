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

package com.cdd.bao.template;

import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;

/*
	Miscellaneous functionality: loads up the whole Gene Ontology OWL file (which is very large) and pulls out just the top level
	hierarchy that is of interest to template creation. Resubclasses the root nodes so that they are attached to the BAO hierarchy.
*/

public class ImportGeneOntology
{
	private Model inmodel, outmodel;
	
	private static final class Tree
	{
		Tree parent;
		String uri;
		String label = null, descr = null;
		List<Tree> children = new ArrayList<>();
		
		Tree(Tree parent, String uri)
		{
			this.parent = parent;
			this.uri = uri;
		}
	}
	private Tree rootBioProc, rootCellComp, rootMolFunc;

	public ImportGeneOntology() throws IOException
	{
	}
	
	public void load(String fn) throws IOException
	{
		inmodel = ModelFactory.createDefaultModel();
		outmodel = ModelFactory.createDefaultModel();
		try 
		{
			Util.writeln("Reading input model...");
			InputStream istr = new FileInputStream(fn);
			RDFDataMgr.read(inmodel, istr, Lang.RDFXML);
			istr.close();
			Util.writeln("Reading complete: " + inmodel.size() + " triples.");
			
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
	
	public void save(String fn) throws IOException
	{
		outmodel.setNsPrefix("bao", ModelSchema.PFX_BAO);
		outmodel.setNsPrefix("bat", ModelSchema.PFX_BAT);
		outmodel.setNsPrefix("obo", ModelSchema.PFX_OBO);
		outmodel.setNsPrefix("rdfs", ModelSchema.PFX_RDFS);
		outmodel.setNsPrefix("xsd", ModelSchema.PFX_XSD);
		outmodel.setNsPrefix("rdf", ModelSchema.PFX_RDF);

		Util.writeln("Writing to: " + fn);

		OutputStream ostr = new FileOutputStream(fn);
		RDFDataMgr.write(ostr, outmodel, RDFFormat.TURTLE);
		ostr.close();
		
		Util.writeln("Done: " + outmodel.size() + " triples");
	}
	
	private void buildBranch(Tree parent)
	{
		Property pred = inmodel.createProperty(ModelSchema.PFX_RDFS + "subClassOf");
		Resource obj = inmodel.createResource(parent.uri);
		Property propLabel = inmodel.createProperty(ModelSchema.PFX_RDFS + "label");
		Property propDescr = inmodel.createProperty(ModelSchema.PFX_OBO + "IAO_0000115");

		for (StmtIterator it = inmodel.listStatements(null, pred, obj); it.hasNext();)
		{
			Statement st = it.next();
			Resource subj = st.getSubject();

			Tree tree = new Tree(parent, subj.getURI());
			tree.label = findString(subj, propLabel);
			tree.descr = findString(subj, propDescr);
			parent.children.add(tree);
			
			buildBranch(tree);
		}
	}

	private void assertBranch(Tree tree)
	{
		Resource subj = outmodel.createResource(tree.uri);
		outmodel.add(subj, outmodel.createProperty(ModelSchema.PFX_RDFS + "subClassOf"), outmodel.createResource(tree.parent.uri));
		outmodel.add(subj, outmodel.createProperty(ModelSchema.PFX_RDFS + "label"), outmodel.createLiteral(tree.label));
		if (tree.descr != null && tree.descr.length() > 0)
			outmodel.add(subj, outmodel.createProperty(ModelSchema.PFX_OBO + "IAO_0000115"), outmodel.createLiteral(tree.descr));
		for (Tree child : tree.children) assertBranch(child);
	}

	private void showBranch(Tree tree, int level)
	{
		if (level == 0)
		{
			Util.writeln("---- " + tree.label + " ----");
		}
		else
		{
			for (int n = 0; n < level; n++) Util.write(" *");
			Util.writeln(" " + tree.label);
		}
		for (Tree child : tree.children) showBranch(child, level + 1);
	}

	private String findString(Resource subj, Property prop)
	{
		for (StmtIterator it = inmodel.listStatements(subj, prop, (RDFNode)null); it.hasNext();)
		{
			RDFNode obj = it.next().getObject();
			if (obj.isLiteral()) return obj.asLiteral().getString();
		}
		return "";
	}
}
