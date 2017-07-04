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

import com.cdd.bao.util.*;
import com.cdd.bao.template.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.json.*;

/*
	Controlled vocabulary mapping: parses and manages a JSON-formatted file for storing translations between keywords and importing
	of semantic web terms.
*/

public class KeywordMapping
{
	private File file;
	
	public static final class Identity
	{
		public String regex; // there must be at least one group, e.g. "ACME(.*)"
		public String prefix; // must correspond to an identifier prefix for the output, e.g. "acmeID:"
	}
	
	public static class MapAssn
	{
		public String propURI; // URI of the assignment to match to (must be in template, or null)
		public String[] groupNest; // groupNest disambiguation
	}
	
	public static final class Property extends MapAssn
	{
		public String regex; // anything that matches this expression is included in this assignment
		
		public static Property create(String name, String propURI, String[] groupNest)
		{
			Property prop = new Property();
			prop.regex = Pattern.quote(name);
			prop.propURI = ModelSchema.collapsePrefix(propURI);
			prop.groupNest = collapsePrefixes(groupNest);
			return prop;
		}
	}
	
	public static final class Value extends MapAssn
	{
		public String regex; // anything that matches this expression is mapped to this value term
		public String valueURI; // URI of value to match to (must occur in hierarchy of corresponding assignment)

		public static Value create(String name, String valueURI, String propURI, String[] groupNest)
		{
			Value val = new Value();
			val.regex = Pattern.quote(name);
			val.valueURI = ModelSchema.collapsePrefix(valueURI);
			val.propURI = ModelSchema.collapsePrefix(propURI);
			val.groupNest = collapsePrefixes(groupNest);
			return val;
		}
	}
	
	public static final class Literal extends MapAssn
	{
		public String regex; // anything that matches this expression is passed through as a literal

		public static Literal create(String name, String propURI, String[] groupNest)
		{
			Literal lit = new Literal();
			lit.regex = Pattern.quote(name);
			lit.propURI = ModelSchema.collapsePrefix(propURI);
			lit.groupNest = collapsePrefixes(groupNest);
			return lit;
		}
	}
	
	public List<Identity> identities = new ArrayList<>();
	public List<Property> properties = new ArrayList<>();
	public List<Value> values = new ArrayList<>();
	public List<Literal> literals = new ArrayList<>();
	
	private Map<String, Pattern> regexes = new HashMap<>(); // avoid reparsing all the time

	// ------------ public methods ------------

	// instantiate with given filename; parses as much as possible, and fails gently if anything goes wrong
	public KeywordMapping(String mapFN)
	{
		file = new File(mapFN);
		
		// try to load the file, but it's OK if it fails
		JSONObject json = null;
		try
		{
			Reader rdr = new FileReader(file);
			json = new JSONObject(new JSONTokener(rdr));
			rdr.close();
		}
		catch (JSONException ex) {Util.writeln("NOTE: reading file " + file.getAbsolutePath() + " failed: " + ex.getMessage());}
		catch (IOException ex) {return;} // includes file not found, which is OK
		
		try
		{
			for (JSONObject obj : json.optJSONArrayEmpty("identities").toObjectArray())
			{
				Identity id = new Identity();
				id.regex = regexOrName(obj.optString("regex"), obj.optString("name"));
				id.prefix = obj.optString("prefix");
				identities.add(id);
			}
			for (JSONObject obj : json.optJSONArrayEmpty("properties").toObjectArray())
			{
				Property prop = new Property();
				prop.regex = regexOrName(obj.optString("regex"), obj.optString("name"));
				prop.propURI = obj.optString("propURI");
				prop.groupNest = obj.optJSONArrayEmpty("groupNest").toStringArray();
				properties.add(prop);
			}
			for (JSONObject obj : json.optJSONArrayEmpty("values").toObjectArray())
			{
				Value val = new Value();
				val.regex = regexOrName(obj.optString("regex"), obj.optString("name"));
				val.valueURI = obj.optString("valueURI");
				val.propURI = obj.optString("propURI");
				val.groupNest = obj.optJSONArrayEmpty("groupNest").toStringArray();
				values.add(val);
			}
			for (JSONObject obj : json.optJSONArrayEmpty("literals").toObjectArray())
			{
				Literal lit = new Literal();
				lit.regex = regexOrName(obj.optString("regex"), obj.optString("name"));
				lit.propURI = obj.optString("propURI");
				lit.groupNest = obj.optJSONArrayEmpty("groupNest").toStringArray();
				literals.add(lit);
			}
		}
		catch (JSONException ex) 
		{
			Util.writeln("NOTE: parsing error");
			ex.printStackTrace();
			Util.writeln("*** Execution will continue, but part of the mapping has not been loaded and may be overwritten.");
		}
	}
	
	// writes the current state of the mapping back to the original file
	public void save() throws IOException
	{
		JSONObject json = new JSONObject();
		JSONArray listID = new JSONArray(), listProp = new JSONArray(), listVal = new JSONArray(), listLit = new JSONArray();

		for (Identity id : identities)
		{
			JSONObject obj = new JSONObject();
			obj.put("regex", id.regex);
			obj.put("prefix", id.prefix);
			listID.put(obj);
		}
		for (Property prop : properties)
		{
			JSONObject obj = new JSONObject();
			obj.put("regex", prop.regex);
			obj.put("propURI", prop.propURI);
			obj.put("groupNest", prop.groupNest);
			listProp.put(obj);
		}
		for (Value val : values)
		{
			JSONObject obj = new JSONObject();
			obj.put("regex", val.regex);
			obj.put("valueURI", val.valueURI);
			obj.put("propURI", val.propURI);
			obj.put("groupNest", val.groupNest);
			listVal.put(obj);
		}
		for (Literal lit : literals)
		{
			JSONObject obj = new JSONObject();
			obj.put("regex", lit.regex);
			obj.put("propURI", lit.propURI);
			obj.put("groupNest", lit.groupNest);
			listLit.put(obj);
		}
		
		json.put("identities", listID);
		json.put("properties", listProp);
		json.put("values", listVal);
		json.put("literals", listLit);

		Writer wtr = new FileWriter(file);
		wtr.write(json.toString(2));
		wtr.close();
	}
	
	// searches for a property for which the name matches its regex
	public Property findProperty(String name)
	{
		for (Property prop : properties)
		{
			Pattern p = getPattern(prop.regex);
			if (p.matcher(name).matches()) return prop;
		}
		return null;
	}
	
	// collapses all the prefixes in the list
	public static String[] collapsePrefixes(String[] uriList)
	{
		if (uriList == null || uriList.length == 0) return null;
		String[] ret = new String[uriList.length];
		for (int n = 0; n < ret.length; n++) ret[n] = ModelSchema.collapsePrefix(uriList[n]);
		return ret;
	}

	// ------------ private methods ------------
	
	private Pattern getPattern(String regex) throws PatternSyntaxException
	{
		Pattern p = regexes.get(regex);
		if (p == null) regexes.put(regex, p = Pattern.compile(regex));
		return p;
	}
	
	private String regexOrName(String regex, String name)
	{
		if (Util.notBlank(name)) return Pattern.quote(name);
		return regex;
	}
}
