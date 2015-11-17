/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.editor;

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
	
	// turns a group/assignment into a JSON object which can be conveniently unpacked later
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
			JSONObject jv = jvalues.getJSONObject(n);
			Schema.Value val = new Schema.Value(jv.getString("uri"), jv.getString("name"));
			val.descr = jv.getString("descr");
			assn.values.add(val);
		}
		
		return assn;
	}
}


