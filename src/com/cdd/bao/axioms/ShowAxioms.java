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

package com.cdd.bao.axioms;

import com.cdd.bao.template.*;
import com.cdd.bao.util.*;

import static com.cdd.bao.axioms.AxiomVocab.*;

import java.io.*;
import java.util.*;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.json.*;

/*
	Loads up the axiom rule file and displays them, in context of the schema/tree.
*/

public class ShowAxioms
{
	private AxiomVocab axvoc;
	private Schema schema;
	private SchemaVocab schvoc;

	
	// ------------ public methods ------------

	public ShowAxioms(String fnAxioms, String fnTemplate, String fnVocab) throws IOException
	{
		Util.writeln("Loading...");
	
		axvoc = AxiomVocab.deserialise(new File(fnAxioms));
		schema = Schema.deserialise(new File(fnTemplate));
		
		try (InputStream istr = new FileInputStream(fnVocab))
		{
			schvoc = SchemaVocab.deserialise(istr, new Schema[]{schema});
		}
	}

	public void exec() throws OntologyException, JSONException, IOException
	{
		Util.writeln("Total rules: " + axvoc.numRules());
		
		for (int n = 0; n < axvoc.numRules(); n++)
		{
			Util.writeln("Rule (" + (n + 1) + "/" + axvoc.numRules() + ")");
			Rule rule = axvoc.getRule(n);
			Util.writeln("    type: " + rule.type);
			String label = schvoc.getLabel(rule.subject.valueURI);
			Util.writeln("    subject: <" + ModelSchema.collapsePrefix(rule.subject.valueURI) + "> '" + label + "' " + (rule.subject.wholeBranch ? "*" : ""));
			
			for (int i = 0; i < rule.impact.length; i++)
			{
				label = schvoc.getLabel(rule.impact[i].valueURI);
				Util.write("    " + (i == 0 ? "impact:" : "       ") + " <");
				Util.writeln(ModelSchema.collapsePrefix(rule.impact[i].valueURI) + "> '" + label + "' " + (rule.impact[i].wholeBranch ? "*" : ""));
			}
			
			// TODO: display the schema tree contents that this applies to, if any
		}
	}
	
	// ------------ private methods ------------

}
