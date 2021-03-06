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
import java.util.zip.*;

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
			if (fn.endsWith(".gz")) istr = new GZIPInputStream(istr);
			RDFDataMgr.read(inmodel, istr, fn.indexOf(".ttl") >= 0 ? Lang.TURTLE : Lang.RDFXML);
			istr.close();
			Util.writeln("Reading complete: " + inmodel.size() + " triples.");
			
			// these are the allowed outputs
			Property propClass = inmodel.createProperty(ModelSchema.PFX_RDFS + "subClassOf");
			Property propLabel = inmodel.createProperty(ModelSchema.PFX_RDFS + "label");
			Property propDescr = inmodel.createProperty(ModelSchema.PFX_OBO + "IAO_0000115");
			Property propAltLabel = inmodel.createProperty(ModelSchema.PFX_BAE + "altLabel");

			// these are inputs that get remapped
			Property propSynonym1 = inmodel.createProperty(ModelSchema.PFX_OBOINOWL + "hasExactSynonym");
			
			for (StmtIterator it = inmodel.listStatements(null, null, (RDFNode)null); it.hasNext();)
			{
				Statement st = it.next();
				Resource subj = st.getSubject();
				Property pred = st.getPredicate();
				RDFNode obj = st.getObject();

				if (pred.equals(propSynonym1)) pred = propAltLabel;
				else if (!pred.equals(propClass) && !pred.equals(propLabel) && 
					     !pred.equals(propDescr) && !pred.equals(propAltLabel)) continue; // don't want it
				
				outmodel.add(subj, pred, obj);
			}
		}
		catch (Exception ex) {throw new IOException("Failed to parse schema", ex);}
	}
	
	public void save(String fn) throws IOException
	{
		Map<String, String> prefixes = ModelSchema.getPrefixes();
		for (String pfx : prefixes.keySet()) outmodel.setNsPrefix(pfx.substring(0, pfx.length() - 1), prefixes.get(pfx));

		Util.writeln("Writing to: " + fn);

		try (OutputStream fstr = new FileOutputStream(fn))
		{
			OutputStream ostr = fn.endsWith(".gz") ? new GZIPOutputStream(fstr) : fstr;
			RDFDataMgr.write(ostr, outmodel, RDFFormat.TURTLE);
			ostr.close();
		}
		
		Util.writeln("Done: " + outmodel.size() + " triples");
	}
	
}
