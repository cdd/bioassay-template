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
import java.util.*;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;

import com.cdd.bao.template.ModelSchema;
import com.cdd.bao.util.*;

public abstract class OntologyImporter
{
	protected Model inmodel, outmodel;

	protected static final class Tree
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

	// sub-classes implement this method to build custom schema tree from the input model
	protected abstract void buildTree() throws IOException;

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

			this.buildTree();
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

	protected void buildBranch(Tree parent)
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

	protected void assertBranch(Tree tree)
	{
		Resource subj = outmodel.createResource(tree.uri);
		if (tree.parent != null)
			outmodel.add(subj, outmodel.createProperty(ModelSchema.PFX_RDFS + "subClassOf"), outmodel.createResource(tree.parent.uri));
		outmodel.add(subj, outmodel.createProperty(ModelSchema.PFX_RDFS + "label"), outmodel.createLiteral(tree.label));
		if (tree.descr != null && tree.descr.length() > 0)
			outmodel.add(subj, outmodel.createProperty(ModelSchema.PFX_OBO + "IAO_0000115"), outmodel.createLiteral(tree.descr));
		for (Tree child : tree.children) assertBranch(child);
	}

	protected void showBranch(Tree tree, int level)
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

	protected String findString(Resource subj, Property prop)
	{
		for (StmtIterator it = inmodel.listStatements(subj, prop, (RDFNode)null); it.hasNext();)
		{
			RDFNode obj = it.next().getObject();
			if (obj.isLiteral()) return obj.asLiteral().getString();
		}
		return "";
	}
}
