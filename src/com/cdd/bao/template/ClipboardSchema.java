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

import com.cdd.bao.*;
import com.cdd.bao.template.Schema.*;
import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.*;
import org.json.*;

/*
	Serializing/deserializing parts of a schema using JSON, for purposes of cutting/pasting pieces of the schema
	for clipboard interoperability.
*/

public class ClipboardSchema
{
	// ------------ public methods ------------	

	// turns a group/assignment/assay into a JSON object which can be conveniently unpacked later; note that the optional
	// SchemaVocab instance is should be non-null if a full readout of the schema tree is desired (and note also that it should
	// contain just a single schema)
	public static JSONObject composeGroup(Schema.Group group) {return composeGroup(group, null);}
	public static JSONObject composeGroup(Schema.Group group, SchemaVocab schvoc)
	{
		return formatGroup(group, schvoc);
	}
	public static JSONObject composeAssignment(Schema.Assignment assn) {return composeAssignment(assn, null);}
	public static JSONObject composeAssignment(Schema.Assignment assn, SchemaVocab schvoc)
	{
		JSONObject branch = new JSONObject();
		try {branch.put("assignment", formatAssignment(assn, schvoc));} 
		catch (JSONException ex) {}
		return branch;
	}
	public static JSONObject composeAssay(Schema.Assay assay)
	{
		JSONObject branch = new JSONObject();
		try {branch.put("assay", formatAssay(assay));} 
		catch (JSONException ex) {}
		return branch;
	}
	
	// pulling a group/assignment out of a JSON object; returns null if invalid for any reason
	public static Schema.Group unpackGroup(JSONObject obj)
	{
		try 
		{
			return parseGroup(obj, null);
		}
		catch (JSONException ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
	public static Schema.Assignment unpackAssignment(JSONObject obj)
	{
		try
		{
			JSONObject jassn = obj.optJSONObject("assignment");
			if (jassn == null) return null;
			return parseAssignment(jassn, null);
		}
		catch (JSONException ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
	
	// pulls an assay from a JSON object; note that it needs the schema as context, in order to try to match up the assignments
	public static Schema.Assay unpackAssay(JSONObject obj)
	{
		try
		{
			JSONObject jassay = obj.optJSONObject("assay");
			if (jassay == null) return null;
			return parseAssay(jassay);
		}
		catch (JSONException ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
	
	// turns a group or assignment into a tab-separated list, which is convenient for spreadsheet-style wrangling
	public static String composeGroupTSV(Schema.Group group)
	{
		List<String> lines = new ArrayList<>();
		lines.add("name\tdescription\tproperty URI\tgroup...");
		formatGroupTSV(lines, group);
		return String.join("\n", lines);
	}
	public static String composeAssignmentTSV(Schema.Assignment assn)
	{
		List<String> lines = new ArrayList<>();
		lines.add("name\t" + assn.name);
		lines.add("description\t" + assn.descr.replace("\n", " "));
		lines.add("property URI\t" + assn.propURI);
		
		List<String> nest = new ArrayList<>();
		for (Position pos : assn.groupNest()) {nest.add(pos.uri); nest.add(String.valueOf(pos.idx));}
		lines.add("group nest\t" + String.join("\t", nest));
		lines.add("");
		lines.add("value hierarchy");
		lines.add("");

		SchemaTree tree = new SchemaTree(assn, Vocabulary.globalInstance());
		if (tree != null) for (SchemaTree.Node node : tree.getFlat())
		{
			List<String> cols = new ArrayList<>();
			cols.add(node.label);
			cols.add(Util.safeString(node.descr).replace("\n", " "));
			for (SchemaTree.Node look = node.parent; look != null; look = look.parent) cols.add("");
			cols.add(node.uri);
			lines.add(String.join("\t", cols));
		}

		return String.join("\n", lines);
	}
	
	// ------------ private methods ------------	

	private static JSONObject formatGroup(Schema.Group group, SchemaVocab schvoc) throws JSONException
	{
		JSONObject json = new JSONObject();
		json.put("name", group.name);
		json.put("descr", group.descr);
		json.put("groupURI", group.groupURI);
		if (group.canDuplicate) json.put("canDuplicate", true);
		
		JSONArray jassignments = new JSONArray(), jsubgroups = new JSONArray();
		for (Schema.Assignment assn : group.assignments)
		{
			jassignments.put(formatAssignment(assn, schvoc));
		}
		json.put("assignments", jassignments);
		for (Schema.Group subgrp : group.subGroups)
		{
			jsubgroups.put(formatGroup(subgrp, schvoc));
		}
		json.put("subGroups", jsubgroups);
		
		return json;
	}
	private static JSONObject formatAssignment(Schema.Assignment assn, SchemaVocab schvoc) throws JSONException
	{
		JSONObject json = new JSONObject();
		json.put("name", assn.name);
		json.put("descr", assn.descr);
		json.put("propURI", assn.propURI);
		json.put("suggestions", assn.suggestions.toString().toLowerCase());

		JSONArray jvalues = new JSONArray();
		for (Schema.Value val : assn.values)
		{
			JSONObject obj = new JSONObject();
			obj.put("uri", val.uri);
			obj.put("name", val.name);
			obj.put("descr", val.descr);
			obj.put("spec", val.spec.toString().toLowerCase());
			jvalues.put(obj);
		}
		json.put("values", jvalues);
		
		if (schvoc != null) 
		{
			Position[] groupNest = assn.groupNest();
			for (SchemaVocab.StoredTree tree : schvoc.getTrees()) if (assn.propURI.equals(tree.propURI) && Objects.deepEquals(groupNest, tree.groupNest))
			{
				json.put("tree", formatTree(tree.tree));
				break;
			}
		}
		
		return json;
	}
	private static JSONArray formatTree(SchemaTree tree)
	{
		JSONArray jsonList = new JSONArray();
		for (SchemaTree.Node node : tree.getFlat())
		{
			JSONObject json = new JSONObject();
			json.put("parent", node.parentIndex);
			json.put("uri", node.uri);
			json.put("label", node.label);
			//json.put("descr", node.descr); (too much)
			if (node.altLabels != null) json.put("altLabels", node.altLabels);
			jsonList.put(json);
		}
		return jsonList;
	}
	private static JSONObject formatAssay(Schema.Assay assay) throws JSONException
	{
		JSONObject json = new JSONObject();
		json.put("name", assay.name);
		json.put("descr", assay.descr);
		json.put("para", assay.para);
		json.put("originURI", assay.originURI);
		
		JSONArray jannots = new JSONArray();
		for (Schema.Annotation annot : assay.annotations)
		{
			List<String> seq = new ArrayList<>();
			seq.add(annot.assn.name);
			for (Schema.Group grp = annot.assn.parent; grp != null; grp = grp.parent) seq.add(0, grp.name);
			JSONArray jassn = new JSONArray(seq);
		
			JSONObject obj = new JSONObject();
			obj.put("assnSequence", jassn);
			obj.put("assnPropURI", annot.assn.propURI);
			
			if (annot.value != null)
			{
				obj.put("valURI", annot.value.uri);
				obj.put("valName", annot.value.name);
				obj.put("valDescr", annot.value.descr);
			}
			else if (annot.literal != null)
			{
				obj.put("literal", annot.literal);
			}
			else continue; // (shouldn't happen)
			
			jannots.put(obj);
		}
		
		json.put("annotations", jannots);
		
		return json;
	}
	
	private static Schema.Group parseGroup(JSONObject json, Schema.Group parent) throws JSONException
	{
		Schema.Group group = new Schema.Group(parent, json.getString("name"));
		group.descr = json.optString("descr", "");
		group.groupURI = json.optString("groupURI", "");
		group.canDuplicate = json.optBoolean("canDuplicate", false);
		
		JSONArray jassignments = json.getJSONArray("assignments"), jsubgroups = json.getJSONArray("subGroups");
		for (int n = 0; n < jassignments.length(); n++)
		{
			JSONObject obj = jassignments.getJSONObject(n);
			group.assignments.add(parseAssignment(obj, group));
		}
		for (int n = 0; n < jsubgroups.length(); n++)
		{
			JSONObject obj = jsubgroups.getJSONObject(n);
			group.subGroups.add(parseGroup(obj, group));
		}
		
		return group;
	}
	private static Schema.Assignment parseAssignment(JSONObject json, Schema.Group parent) throws JSONException
	{
		Schema.Assignment assn = new Schema.Assignment(parent, json.getString("name"), json.getString("propURI"));
		assn.descr = json.optString("descr", "");
		
		String suggestValue = json.optString("suggestions");
		if (Util.notBlank(suggestValue))
		{
			try {assn.suggestions = Schema.Suggestions.valueOf(suggestValue.toUpperCase());}
			catch (IllegalArgumentException ex) {} // silent fail: leave it as default (see class definition)
		}
		
		JSONArray jvalues = json.getJSONArray("values");
		for (int n = 0; n < jvalues.length(); n++)
		{
			JSONObject obj = jvalues.getJSONObject(n);
			Schema.Value val = new Schema.Value(obj.optString("uri", ""), obj.optString("name", ""));
			val.descr = obj.optString("descr", "");
			
			String strSpec = obj.optString("spec");
			try {if (Util.notBlank(strSpec)) val.spec = Schema.Specify.valueOf(strSpec.toUpperCase());}
			catch (IllegalArgumentException ex) {} // (note that constructor defaults to ITEM)

			// deprecated format: still reads the old version
			if (obj.optBoolean("exclude", false)) val.spec = Schema.Specify.EXCLUDE;
			else if (obj.optBoolean("wholeBranch", false)) val.spec = Schema.Specify.WHOLEBRANCH;
			else if (obj.optBoolean("excludeBranch", false)) val.spec = Schema.Specify.EXCLUDEBRANCH;

			assn.values.add(val);
		}
		
		return assn;
	}
	private static Schema.Assay parseAssay(JSONObject json) throws JSONException
	{
		Schema.Assay assay = new Schema.Assay(json.getString("name"));
		assay.descr = json.getString("descr");
		assay.para = json.optString("para", "");
		assay.originURI = json.optString("originURI", "");
		
		JSONArray jannots = json.getJSONArray("annotations");
		for (int n = 0; n < jannots.length(); n++)
		{
			JSONObject obj = jannots.getJSONObject(n);
			
			String propURI = obj.getString("assnPropURI");
			JSONArray seq = obj.getJSONArray("assnSequence");
			Schema.Assignment assn = new Schema.Assignment(null, seq.getString(seq.length() - 1), propURI);
			assn.parent = new Schema.Group(null, seq.getString(seq.length() - 2));
			Schema.Group grp = assn.parent;
			for (int i = seq.length() - 3; i >= 0; i--)
			{
				grp.parent = new Schema.Group(null, seq.getString(i));
				grp = grp.parent;
			}
			
			String valName = obj.optString("valName", null), valDescr = obj.optString("valDescr", null), valURI = obj.optString("valURI", null);
			String literal = obj.optString("literal", null);
			if (valName != null && valDescr != null && valURI != null)
			{
				Schema.Value val = new Schema.Value(valURI, valName);
				val.descr = valDescr;
				assay.annotations.add(new Schema.Annotation(assn, val));
			}
			else if (literal != null)
			{
				assay.annotations.add(new Schema.Annotation(assn, literal));
			}
		}
		
		return assay;
	}
	
	private static void formatGroupTSV(List<String> lines, Schema.Group group)
	{
		List<String> cols = new ArrayList<>();
		cols.add(group.name);
		cols.add(group.descr.replace("\n", " "));
		cols.add("");
		for (Position pos : group.groupNest()) 
		{
			cols.add(pos.uri);
			cols.add(String.valueOf(pos.idx));
		}
		cols.add(Util.safeString(group.groupURI));
		lines.add(String.join("\t", cols));
		
		for (Schema.Assignment assn : group.assignments)
		{
			cols.clear();
			cols.add(assn.name);
			cols.add(assn.descr.replace("\n", " "));
			cols.add(assn.propURI);
			for (Position pos : assn.groupNest()) 
			{
				cols.add(pos.uri);
				cols.add(String.valueOf(pos.idx));
			}
			lines.add(String.join("\t", cols));
		}
		for (Schema.Group subgrp : group.subGroups) formatGroupTSV(lines, subgrp);
	}
}


