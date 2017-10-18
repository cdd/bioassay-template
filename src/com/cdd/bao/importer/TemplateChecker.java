/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2017 Collaborative Drug Discovery Inc.
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

import com.cdd.bao.template.*;
import static com.cdd.bao.template.Schema.*;
import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;
import org.json.*;

/*
	Loads up a schema and runs through the template content, looking for possible flaws that could cause trouble
	further down the line.
*/

public class TemplateChecker
{
	private String fn;
	private Schema schema;
	private Vocabulary vocab;

	// ------------ public methods ------------

	public TemplateChecker(String fn)
	{
		this.fn = fn;
	}
	
	public void perform() throws IOException, JSONException
	{
		schema = ModelSchema.deserialise(new File(fn));
		Util.writeln("Loaded schema: <" + schema.getSchemaPrefix() + ">");
		
		vocab = new Vocabulary();
		Util.writeFlush("Loading ontologies ");
		vocab.addListener(new Vocabulary.Listener()
		{
			public void vocabLoadingProgress(Vocabulary vocab, float progress) {Util.writeFlush(".");}
			public void vocabLoadingException(Exception ex) {ex.printStackTrace();}
		});
		vocab.load(null, null);
		Util.writeln();
		Util.writeln("Loaded " + vocab.numProperties() + " properties, " + vocab.numValues() + " values.");
		
		//SchemaVocab schvoc = new SchemaVocab(vocab, new Schema[]{schema});
		//Util.writeln("Loaded: " + schvoc.numTerms() + " terms.");
		
		checkGroup(0, schema.getRoot());
	}
	
	// ------------ private methods ------------
	
	private void checkGroup(int indent, Group group)
	{
		String indstr = Util.rep(' ', 2 * indent);
		
		Util.writeln(indstr + "---- Group: [" + group.name + "] ----");

		if (Util.isBlank(group.name)) Util.writeln(indstr + "** group name should not be blank");

		if (group.parent == null) {} // don't care
		else if (Util.isBlank(group.groupURI)) Util.writeln(indstr + "** group has no URI");
		else if (!vocab.hasPropertyURI(group.groupURI)) Util.writeln(indstr + "** group URI <" + group.groupURI + "> not a known property");

		if (Util.isBlank(group.descr)) Util.writeln(indstr + "** group has no description");
		
		Set<String> usedName = new HashSet<>(), usedURI = new HashSet<>();
		
		// go through all of the assignments and check them out
		for (int n = 0; n < group.assignments.size(); n++)
		{
			Assignment assn = group.assignments.get(n);
			Util.writeln(indstr + "assignment (" + (n + 1) + "/" + group.assignments.size() + "): [" + assn.name + "] + #values:" + assn.values.size());
			
			if (Util.isBlank(assn.name)) Util.writeln(indstr + "** assignment name should not be blank");
			else if (usedName.contains(assn.name)) Util.writeln(indstr + "** name has been used previously in this group");
			else usedName.add(assn.name);
			
			if (Util.isBlank(assn.propURI)) Util.writeln(indstr + "** assignment has no URI");
			else if (usedURI.contains(assn.propURI)) Util.writeln(indstr + "** URI <" + assn.propURI + "> has been used previously in this group");
			else 
			{
				if (!vocab.hasPropertyURI(assn.propURI)) Util.writeln(indstr + "** assignment property URI <" + assn.propURI + "> not a known property");
				usedURI.add(assn.propURI);
			}
			
			if (Util.isBlank(assn.descr)) Util.writeln(indstr + "** assignment has no description");
			
			Set<String> valueURI = new HashSet<>();
			for (int i = 0; i < assn.values.size(); i++)
			{
				Value val = assn.values.get(i);
				
				if (Util.isBlank(val.name)) Util.writeln(indstr + "** value#" + (i + 1) + " has no name (URI=<" + val.uri + ">)");
				
				if (Util.isBlank(val.uri)) Util.writeln(indstr + "** value#" + (i + 1) + " has no URI (name=[" + val.name + "])");
				else if (valueURI.contains(val.uri)) Util.writeln(indstr + "** value# " + (i + 1) + " URI <" + val.uri + "> is duplicated");
				else
				{
					if (!vocab.hasValueURI(val.uri)) Util.writeln(indstr + "** value# " + (i + 1) + " URI <" + val.uri + "> not a known value");
					valueURI.add(val.uri);
				}
			}
		}
		
		// run through subgroups and check for duplicate URIs, prior to recursively analyzing them individually
		for (int n = 0; n < group.subGroups.size(); n++)
		{
			Group subgrp = group.subGroups.get(n);
			if (Util.isBlank(subgrp.groupURI)) {} // will complain later
			if (usedURI.contains(subgrp.groupURI))
			{
				Util.writeln("** subgroup #" + (n + 1) + " [" + subgrp.name + "] has duplicate URI: <" + subgrp.groupURI + ">");
			}
			else usedURI.add(subgrp.groupURI);
		}
		
		for (Group subgrp : group.subGroups) checkGroup(indent + 1, subgrp);
	}
}
