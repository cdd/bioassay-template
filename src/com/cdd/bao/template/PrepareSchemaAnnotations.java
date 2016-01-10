/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.template;

import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/*
	Miscellaneous functionality: loads a preexisting schema and adds in assay annotation content from elsewhere. Not a common workflow task, more of
	a convenience script.
*/

public class PrepareSchemaAnnotations
{
	private Schema schema;

	public PrepareSchemaAnnotations(String fn) throws IOException
	{
		schema = ModelSchema.deserialise(new File(fn));
	}
	
	public void appendPubChemFiles(String pubchemDir) throws IOException
	{
		final Pattern ptn = Pattern.compile("^pubchem_aid0*([1-9]\\d*).txt$");
		Map<Integer, String> aidText = new TreeMap<>();
		for (File f : new File(pubchemDir).listFiles())
		{
			Matcher m = ptn.matcher(f.getName());
			if (!m.matches()) continue;
			int aid = Integer.valueOf(m.group(1));
			
			BufferedReader rdr = new BufferedReader(new FileReader(f));
			StringBuffer buff = new StringBuffer();
			while (true)
			{
    			int ch = rdr.read();
    			if (ch < 0) break;
    			buff.append((char)ch);
			}
			rdr.close();
			
			aidText.put(aid, buff.toString());
		}
		
		//for (int n = schema.numAssays() - 1; n >= 0; n --) schema.deleteAssay(n);
		
		//Util.writeln(aidText.toString());
		skip: for (int aid : aidText.keySet())
		{
			String name = "PubChem Assay (ID " + aid + ")", originURI = "http://pubchem.ncbi.nlm.nih.gov/bioassay/" + aid;
			for (int n = 0; n < schema.numAssays(); n++) 
			{
				Schema.Assay assay = schema.getAssay(n);
				if (assay.name.equals(name) || assay.originURI.equals(originURI)) continue skip;
			}
		
			Schema.Assay assay = new Schema.Assay(name);
			assay.para = aidText.get(aid);
			assay.originURI = originURI;
			schema.appendAssay(assay);
		}
	}
	
	public void writeSchema(String fn) throws IOException
	{
		ModelSchema.serialise(schema, new File(fn));
	}
}
