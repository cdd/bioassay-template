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

import com.cdd.bao.*;
import com.cdd.bao.util.*;
import com.cdd.bao.template.*;

import java.io.*;
import java.util.*;
import org.json.*;

/*
	Miscellaneous utilities to do with schema instances.
*/

public class SchemaUtil
{
	// ------------ public methods ------------	

	// starts with schema1, and adds in content from schema2, which includes assignments, values and assays; makes a reasonable effort to avoid duplicating;
	// returns a new schema that contains the merged content; the log parameter contains a list of strings that describe each modification; if nothing was
	// added, then no items will be added to the list
	public static Schema mergeSchema(Schema schema, Schema extra, List<String> log)
	{
		schema = schema.clone();
		
		List<Schema.Group> stack = new ArrayList<>();
		stack.add(extra.getRoot());
		while (stack.size() > 0)
		{
			Schema.Group grp = stack.remove(0);
			for (int n = 0; n < grp.subGroups.size(); n++) stack.add(n, grp.subGroups.get(n));
			
			Schema.Group dstgrp = schema.findGroup(grp);
			if (dstgrp == null)
			{
				Schema.Group parent = schema.findGroup(grp.parent);
				dstgrp = new Schema.Group(parent, grp.name);
				dstgrp.descr = grp.descr;
				parent.subGroups.add(dstgrp);
				log.add("Added new group: [" + grp.name + "]");
			}
			
			for (Schema.Assignment assn : grp.assignments)
			{
				Schema.Assignment dstassn = schema.findAssignment(assn);
				if (dstassn == null)
				{
					dstassn = assn.clone();
					dstassn.parent = dstgrp;
					dstgrp.assignments.add(dstassn);
					log.add("Added new assignment: [" + assn.name + "]");
				}
				
				gotval: for (Schema.Value val : assn.values)
				{
					for (Schema.Value dstval : dstassn.values) if (val.equals(dstval)) continue gotval;
					dstassn.values.add(val);
					
					if (val.uri.length() > 0) 
						log.add("Assignment [" + assn.name + "] added new value <" + val.uri + ">: [" + val.name + "]");
					else
						log.add("Assignment [" + assn.name + "] added new literal \"" + val.name + "\"");
				}
			}
		}
		
		gotassay: for (int n = 0; n < extra.numAssays(); n++)
		{
			Schema.Assay assay = extra.getAssay(n);
			if (assay.annotations.size() == 0) continue;
			
			for (int i = 0; i < schema.numAssays(); i++)
			{
				Schema.Assay dstass = schema.getAssay(i);
				if (!assay.name.equals(dstass.name)) continue;
				if (assay.equals(dstass)) continue gotassay;
				
				if (dstass.annotations.size() == 0)
				{
					schema.setAssay(i, assay.clone());
					log.add("Assay [" + assay.name + "]: replaced blank entry");
					continue gotassay;
				}
				else if (!assay.equals(dstass))
				{
					schema.insertAssay(i + 1, assay.clone());
					log.add("Assay [" + assay.name + "]: inserted different version");
					continue gotassay;
				}
			}
			
			schema.appendAssay(assay.clone());
			log.add("Added new assay [" + assay.name + "]");
		}
		
		return schema;
	}
	
	// collects a bunch of stats about the assays gathered in the given schema, in relationship to the available assignments
	public static void gatherAssayStats(Schema schema, List<String> stats)
	{
		List<Integer> idxAssay = new ArrayList<>();
		for (int n = 0; n < schema.numAssays(); n++) if (schema.getAssay(n).annotations.size() > 0) idxAssay.add(n);
		final int nassay = idxAssay.size();
		stats.add("Assays with partial or complete annotations: " + nassay);
		
		List<Schema.Group> stack = new ArrayList<>();
		stack.add(schema.getRoot());
		List<Schema.Assignment> assignments = new ArrayList<>();
		while (stack.size() > 0)
		{
			Schema.Group grp = stack.remove(0);
			assignments.addAll(grp.assignments);
			for (int n = 0; n < grp.subGroups.size(); n++) stack.add(n, grp.subGroups.get(n));
		}

		List<Set<String>> assnValues = new ArrayList<>(), assnHits = new ArrayList<>();
		for (Schema.Assignment assn : assignments)
		{
    		Set<String> values = new HashSet<>();
    		for (Schema.Value val : assn.values) if (val.uri.length() > 0) values.add(val.uri);
    		assnValues.add(values);
    		assnHits.add(new HashSet<>());
		}

		final int nassn = assignments.size();
		if (nassn == 0) return;
		int[] assnCount = new int[nassn];
		
		for (int n : idxAssay)
		{
			Schema.Assay assay = schema.getAssay(n);
			boolean[] assnHit = new boolean[nassn];
			for (Schema.Annotation annot : assay.annotations)
			{
				for (int i = 0; i < nassn; i++) if (schema.matchAnnotation(annot, assignments.get(i))) 
				{
					assnHit[i] = true;
					if (annot.value != null && assnValues.get(i).contains(annot.value.uri)) assnHits.get(i).add(annot.value.uri);
				}
			}
			for (int i = 0; i < nassn; i++) if (assnHit[i]) assnCount[i]++;
		}
		
		for (int n = 0; n < nassn; n++) 
		{
			Schema.Assignment assn = assignments.get(n);
			String assnName = assn.name;
			for (Schema.Group p = assn.parent; p.parent != null; p = p.parent) assnName = p.name + " / " + assnName;
			
			stats.add("[" + assnName + "]");
			stats.add(String.format("        assigned: count=%d/%d (%.1f%%)", assnCount[n], nassay, assnCount[n] * 100.0f / nassay));
			
			int nhits = assnHits.get(n).size(), nvals = assnValues.get(n).size();
			
			stats.add(String.format("        values used: count=%d/%d (%.1f%%)", nhits, nvals, nhits * 100.0f / nvals));
		}
	}
	
	// ------------ private methods ------------	

}


