/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2016 Collaborative Drug Discovery Inc.
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
	Miscellaneous functionality: loads a given ontology, and spits out a limited subset, containing only the minimalistic relationships
	needed to recreate the tree. All the rest of the fluff is left out.
*/

public class OntologyFilter
{
	private Model inmodel, outmodel;
	
	public OntologyFilter() throws IOException
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
			
			Property propClass = inmodel.createProperty(ModelSchema.PFX_RDFS + "subClassOf");
			Property propLabel = inmodel.createProperty(ModelSchema.PFX_RDFS + "label");
			Property propDescr = inmodel.createProperty(ModelSchema.PFX_OBO + "IAO_0000115");

    		for (StmtIterator it = inmodel.listStatements(null, null, (RDFNode)null); it.hasNext();)
    		{
    			Statement st = it.next();
    			Property pred = st.getPredicate();
    			//if (pred != propClass && pred != propLabel && pred != propDescr) continue;
    			if (!pred.equals(propClass) && !pred.equals(propLabel) && !pred.equals(propDescr)) continue;
    			
    			outmodel.add(st);
    		}
		}
		catch (Exception ex) {throw new IOException("Failed to parse schema", ex);}
	}
	
	public void save(String fn) throws IOException
	{
		/*outmodel.setNsPrefix("bao", ModelSchema.PFX_BAO);
		outmodel.setNsPrefix("bat", ModelSchema.PFX_BAT);
		outmodel.setNsPrefix("obo", ModelSchema.PFX_OBO);
		outmodel.setNsPrefix("rdfs", ModelSchema.PFX_RDFS);
		outmodel.setNsPrefix("xsd", ModelSchema.PFX_XSD);
		outmodel.setNsPrefix("rdf", ModelSchema.PFX_RDF);*/
		
		Map<String, String> prefixes = ModelSchema.getPrefixes();
		for (String pfx : prefixes.keySet()) outmodel.setNsPrefix(pfx.substring(0, pfx.length() - 1), prefixes.get(pfx));

		Util.writeln("Writing to: " + fn);

		OutputStream ostr = new FileOutputStream(fn);
		RDFDataMgr.write(ostr, outmodel, RDFFormat.TURTLE);
		ostr.close();
		
		Util.writeln("Done: " + outmodel.size() + " triples");
	}
	
}
