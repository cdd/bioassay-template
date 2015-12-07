/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.template;

import com.cdd.bao.*;
import com.cdd.bao.template.*;

import java.io.*;
import java.util.*;
import org.json.*;

/*
	Serializing/deserializing parts of a schema using JSON, for purposes of cutting/pasting pieces of the schema
	for clipboard interoperability.
*/

public class ClipboardSchema
{
	//private Schema schema;

	// ------------ public methods ------------	

	/*public ClipboardSchema(Schema schema)
	{
		this.schema = schema;
	}*/
	
	// turns a group/assignment/assay into a JSON object which can be conveniently unpacked later
	public static JSONObject composeGroup(Schema.Group group)
	{
		JSONObject branch = new JSONObject();
		try {branch.put("group", formatGroup(group));}
		catch (JSONException ex) {}
		return branch;
	}
	public static JSONObject composeAssignment(Schema.Assignment assn)
	{
		JSONObject branch = new JSONObject();
		try {branch.put("assignment", formatAssignment(assn));} 
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
    		JSONObject jgroup = obj.optJSONObject("group");
    		if (jgroup == null) return null;
    		return parseGroup(jgroup, null);
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
	
	// ------------ private methods ------------	

	private static JSONObject formatGroup(Schema.Group group) throws JSONException
	{
		JSONObject json = new JSONObject();
		json.put("name", group.name);
		json.put("descr", group.descr);
		
		JSONArray jassignments = new JSONArray(), jsubgroups = new JSONArray();
		for (Schema.Assignment assn : group.assignments)
		{
			jassignments.put(formatAssignment(assn));
		}
		json.put("assignments", jassignments);
		for (Schema.Group subgrp : group.subGroups)
		{
			jsubgroups.put(formatGroup(subgrp));
		}
		json.put("subGroups", jsubgroups);
		
		return json;
	}
	private static JSONObject formatAssignment(Schema.Assignment assn) throws JSONException
	{
		JSONObject json = new JSONObject();
		json.put("name", assn.name);
		json.put("descr", assn.descr);
		json.put("propURI", assn.propURI);
		
		JSONArray jvalues = new JSONArray();
		for (Schema.Value val : assn.values)
		{
			JSONObject obj = new JSONObject();
			obj.put("uri", val.uri);
			obj.put("name", val.name);
			obj.put("descr", val.descr);
			jvalues.put(obj);
		}
		json.put("values", jvalues);
		
		return json;
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
		group.descr = json.getString("descr");
		
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
		assn.descr = json.getString("descr");
		
		JSONArray jvalues =  json.getJSONArray("values");
		for (int n = 0; n < jvalues.length(); n++)
		{
			JSONObject obj = jvalues.getJSONObject(n);
			Schema.Value val = new Schema.Value(obj.getString("uri"), obj.getString("name"));
			val.descr = obj.getString("descr");
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
}


