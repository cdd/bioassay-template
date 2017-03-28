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

import java.io.*;
import java.util.*;
import org.json.*;

/*
	Importing of controlled vocabulary: allows external content, of the keyword variety, to be imported into a schema.
	This involves constructing a mapping table for converting property/value tokens into URIs. The process of creating these
	mappings can be done iteratively, with intermediate output providing some guidance as to what remains to be done.
*/

public class ImportControlledVocab
{
	private String cwd;
	
	private String inputSubstrate; // the columns to be imported (.tsv, first line is title)
	private String inputSchema; // schema template to use (.ttl)
	private String inputVocab; // vocabulary complement (.dump)
	private String idPrefix; // a suitable prefix for assay IDs, e.g. "pubchemAID:"
	private int idColumn; // column containing the assay IDs (1-based)
	private int propKeyColumn; // column containing the property key from controlled vocabulary
	private int propDescrColumn; // optional column for the property description
	private int valueKeyColumn; // column containing the value key from controlled vocabulary
	private int valueDescrColumn; // optional column for the value description
	
	private Map<String, String> propMap = new HashMap<>(); // property key -> property URI
	private Map<String, Map<String, String>> valueMap = new HashMap<>(); // per property URI: value key -> value URI	

	// ------------ public methods ------------

	public ImportControlledVocab(String cfgFN) throws IOException, JSONException
	{
		File f = new File(cfgFN);
		if (!f.exists()) throw new IOException("File not found: " + f);
		cwd = f.getCanonicalFile().getParent();
		Reader rdr = new FileReader(f);
		JSONObject json = new JSONObject(new JSONTokener(rdr));
		rdr.close();
		
		inputSubstrate = json.getString("inputSubstrate");
		inputSchema = json.getString("inputSchema");
		inputVocab = json.getString("inputVocab");
		idPrefix = json.getString("idPrefix");
		idColumn = json.getInt("idColumn");
		propKeyColumn = json.getInt("propKeyColumn");
		propDescrColumn = json.getInt("propDescrColumn");
		valueKeyColumn = json.getInt("valueKeyColumn");
		valueDescrColumn = json.getInt("valueDescrColumn");
		
		JSONArray jsonProp = json.getJSONArray("propMap");
		for (int n = 0; n < jsonProp.length(); n++)
		{
			JSONArray pair = jsonProp.getJSONArray(n);
			propMap.put(pair.getString(0), pair.getString(1));
		}
		
		JSONObject jsonPropValue = json.getJSONObject("valueMap");
		for (Iterator<String> it = jsonPropValue.keys(); it.hasNext();)
		{
			String propURI = it.next();
			Map<String, String> map = new HashMap<>();
			JSONArray jsonValue = jsonPropValue.getJSONArray(propURI);
			for (int n = 0; n < jsonValue.length(); n++)
			{
				JSONArray pair = jsonValue.getJSONArray(n);
				map.put(pair.getString(0), pair.getString(1));
			}
			valueMap.put(propURI, map);
		}
	}
	
}
