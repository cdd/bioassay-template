/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.template;

import com.cdd.bao.*;
import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;

/*
 * 	One off functionality: prepares a schema from externally organised data; used to prepare the starting point, but is no longer part of a workflow.
*/

public class PrepareTemplate
{
	private String cwd = System.getProperty("user.dir");
	private Map<String, String[]> categories = new TreeMap<String, String[]>(); // category URI -> list of corresponding values
	private Set<String> termsUsed = new HashSet<String>(); // URIs that are covered in priority set

	private Vocabulary vocab = null;
	private Schema schema = null;

	// ------------ public methods ------------

	public PrepareTemplate()
	{
	}
	
	public void exec()
	{
		try
		{
			Util.writeln("Loading vocabulary...");
			vocab = new Vocabulary();

			Util.writeln("Examining seed priorities...");
			examinePriority();
			Util.writeln(" ... loaded priorities: " + categories.size());
			
			Util.writeln("Building schema...");
			schema = new Schema();
			buildSchema();
			
			File f = new File(cwd + "/template/schema.ttl");
			Util.writeln("Serialising to: " + f.getPath());
			ModelSchema.serialise(schema, f);
			Util.writeln("Done.");
		}
		catch (Exception ex) {ex.printStackTrace();}
	}
	

	// ------------ private methods ------------

/*
	private void loadLabels()
	{
		// load all the BAO files into the model
		Model model = ModelFactory.createDefaultModel() ;
		for (File f : new File(cwd + "/bao").listFiles())
		{
			if (!f.getName().endsWith(".owl")) continue;
			//Util.writeln("Loading: " + f.getPath());
			RDFDataMgr.read(model, f.getPath());
		}
		
		// iterate over the list looking for label definitions
		StmtIterator iter = model.listStatements();
		while (iter.hasNext())
		{
			Statement stmt = iter.next();
			
			Resource subject = stmt.getSubject();
			Property predicate = stmt.getPredicate();
			RDFNode object = stmt.getObject();

			if (!object.isLiteral()) continue;

			String uri = subject.getURI();
			String label = object.asLiteral().getString();

			if (predicate.getLocalName().equals("label"))
			{
    			uriToLabel.put(uri, label);
    			String[] list = labelToURI.get(label);
    			if (list != null)
    			{
    				list = Arrays.copyOf(list, list.length + 1);
    				list[list.length - 1] = uri;
    				labelToURI.put(label, list);
    			}
    			else labelToURI.put(label, new String[]{uri});
			}
			else if (predicate.getLocalName().equals("IAO_0000115"))
			{
				uriToDescr.put(uri, label);
			}
		}

		// go over the label-to-URI list and whenever there are multiple cases, try to favour the BAO version first (expect a few to
		// slip through though)
		for (String label : labelToURI.keySet())
		{
			String[] list = labelToURI.get(label);
			if (list.length == 1) continue;
			Arrays.sort(list);
			int idx = -1;
			for (int n = 0; n < list.length; n++) if (list[n].startsWith(Schema.PFX_BAO)) {idx = n; break;}
			// NOTE: if there's more than one BAO-based label, that with the lowest sort order will be retained; there are a 
			// couple of these in the list
			if (idx >= 0) labelToURI.put(label, new String[]{list[idx]});
		}
	}*/
	
	private void examinePriority() throws IOException
	{
		File f = new File(cwd + "/template/priority.csv");
		BufferedReader rdr = new BufferedReader(new FileReader(f));
		
		rdr.readLine(); // skip header
		while (true)
		{
			String line = rdr.readLine();
			if (line == null) break;
			String[] bits = line.split("\t");
			if (bits.length < 2 || bits[0].length() == 0) continue;
			
			String propLabel = bits[0];
			String[] valueLabel = Arrays.copyOfRange(bits, 1, bits.length);
			
			String propURI = vocab.findURIForLabel(propLabel);
			String propDescr = propURI == null ? null : vocab.getDescr(propURI);
			if (propURI != null) termsUsed.add(propURI);
			
			final int nval = valueLabel.length;
			String[] valueURI = new String[nval], valueDescr = new String[nval];
			for (int n = 0; n < nval; n++)
			{
				valueURI[n] = vocab.findURIForLabel(valueLabel[n]);
				valueDescr[n] = valueURI[n] == null ? null : vocab.getDescr(valueURI[n]);
				if (valueURI[n] != null) termsUsed.add(valueURI[n]);
			}
			
			//Util.writeln("Property: '" + propLabel + "' " + propURI);
			
			List<String> fullValues = new ArrayList<String>();
			
			for (int n = 0; n < nval; n++)
			{
				//Util.writeln(String.format("Value %2d: '%s' %s", n + 1, valueLabel[n], valueURI[n]));
				//if (valueDescr[n] != null) Util.writeln("        : " + valueDescr[n]);
				if (valueURI[n] != null) fullValues.add(valueURI[n]);
			}
			//Util.writeln("  Values: " + bits[1]);
			
			if (propURI != null) categories.put(propURI, fullValues.toArray(new String[fullValues.size()]));
			
			Util.writeln();
		}
		
		rdr.close();
	}
	
	/*private void showAbsences() throws IOException
	{
		Util.writeln("\nBAO terms not found in priority...");
		BufferedWriter wtr = new BufferedWriter(new FileWriter(new File(cwd + "/template/unusedterms.txt")));
		int count = 0;
		for (String uri : uriToLabel.keySet())
		{
			if (!uri.startsWith(Schema.PFX_BAO)) continue;
			wtr.write(uri + " : '" + uriToLabel.get(uri) + "'\n");
			String descr = uriToDescr.get(uri);
			if (descr != null) wtr.write("    " + descr + "\n");
			count++;
		}
		Util.writeln("... total number: " + count);
		wtr.close();
	}*/

	private void buildSchema() throws IOException
	{
		/*schema = ModelFactory.createDefaultModel();
		
		schema.setNsPrefix("bao", Schema.PFX_BAO);
		schema.setNsPrefix("bat", Schema.PFX_BAT);
		schema.setNsPrefix("obo", Schema.PFX_OBO);
		schema.setNsPrefix("rdfs", Schema.PFX_RDFS);
		schema.setNsPrefix("owl", Schema.PFX_OWL);
		schema.setNsPrefix("xsd", Schema.PFX_XSD);
		schema.setNsPrefix("rdf", Schema.PFX_RDF);
		
		Property rdfLabel = schema.createProperty(Schema.PFX_RDFS + "label");
		Property rdfType = schema.createProperty(Schema.PFX_RDF + "type");
		Property subClass = schema.createProperty(Schema.PFX_RDFS + "subClassOf");
		Property subProp = schema.createProperty(Schema.PFX_RDFS + "subPropertyOf");
		Property subDomain = schema.createProperty(Schema.PFX_RDFS + "domain");
		Property subRange = schema.createProperty(Schema.PFX_RDFS + "range");
		Resource owlClass = schema.createProperty(Schema.PFX_OWL + "Class");
		Resource owlProp = schema.createProperty(Schema.PFX_OWL + "ObjectProperty");
		
		Resource clsAssay = schema.createResource(Schema.PFX_BAT + Schema.CLASS_ASSAY);
		schema.add(clsAssay, rdfType, owlClass);
		
		Resource clsCategory = schema.createResource(Schema.PFX_BAT + Schema.CLASS_CATEGORY);
		schema.add(clsCategory, rdfType, owlClass);
		
		Resource hasCategory = schema.createResource(Schema.PFX_BAT + Schema.PROP_CATEGORY);
		schema.add(hasCategory, rdfType, owlProp);
		schema.add(hasCategory, subDomain, clsAssay);
		schema.add(hasCategory, subRange, clsCategory);
		
		Resource hasAssign = schema.createResource(Schema.PFX_BAT + Schema.PROP_ASSIGN);
		schema.add(hasAssign, rdfType, owlProp);
		
 		for (String propURI : categories.keySet())
		{
			// create the object class definition and the property to link to it

			String name = turnLabelIntoName(uriToLabel.get(propURI));
			if (name == null) continue;
			
			Resource clsProp = schema.createResource(Schema.PFX_BAT + name);
			schema.add(clsProp, rdfType, owlClass);
			schema.add(clsProp, subClass, clsCategory);
			
			Resource hasProp = schema.createResource(Schema.PFX_BAT + "has" + name);
			schema.add(hasProp, rdfType, owlProp);
			schema.add(hasProp, subProp, hasCategory);
			schema.add(hasProp, subDomain, clsAssay); // (inferred?)
			schema.add(hasProp, subRange, clsProp);
			// [constraints: maybe insist that there be exactly 0 or 1?]
			
			Resource hasGroup = schema.createResource(Schema.PFX_BAT + "assign" + name);
			schema.add(hasGroup, rdfType, owlProp);
			schema.add(hasGroup, subProp, hasAssign);
			schema.add(hasGroup, subDomain, clsProp);
			
			// go through the value URI list; each one of them gets its own property
			for (String valueURI : categories.get(propURI))
			{
				if (valueURI == null) continue;
				name = turnLabelIntoName(uriToLabel.get(valueURI));
				if (name == null) continue;
				
				Resource hasValue = schema.createResource(Schema.PFX_BAT + "assign" + name);
				schema.add(hasValue, rdfType, owlProp);
				schema.add(hasValue, subProp, hasGroup);
				schema.add(hasValue, subDomain, clsProp);
				schema.add(hasValue, subRange, valueURI);
			}			
		}
		
		FileOutputStream ostr = new FileOutputStream(new File(cwd + "/template/schema.ttl"));
		RDFDataMgr.write(ostr, schema, RDFFormat.TURTLE);
		ostr.close();*/
		
		Schema.Group root = schema.getRoot();
		for (String propURI : categories.keySet())
		{
			String label = vocab.getLabel(propURI);
			//String propName = turnLabelIntoName(label);
			Schema.Assignment assn = new Schema.Assignment(root, label, propURI);
			
			for (String valueURI : categories.get(propURI))
			{
				if (valueURI == null)
				{
					// (some facility for adding literals? maybe later)
				}
				else
				{
					Schema.Value val = new Schema.Value(valueURI, vocab.getLabel(valueURI));
					val.descr = vocab.getDescr(valueURI);
					if (val.name == null) val.name = "";
					if (val.descr == null) val.descr = "";
					assn.values.add(val);
				}
			}

			root.assignments.add(assn);
		}
		
		/*
 		for (String propURI : categories.keySet())
		{
			// create the object class definition and the property to link to it

			String name = turnLabelIntoName(uriToLabel.get(propURI));
			if (name == null) continue;
			
			Resource clsProp = schema.createResource(Schema.PFX_BAT + name);
			schema.add(clsProp, rdfType, owlClass);
			schema.add(clsProp, subClass, clsCategory);
			
			Resource hasProp = schema.createResource(Schema.PFX_BAT + "has" + name);
			schema.add(hasProp, rdfType, owlProp);
			schema.add(hasProp, subProp, hasCategory);
			schema.add(hasProp, subDomain, clsAssay); // (inferred?)
			schema.add(hasProp, subRange, clsProp);
			// [constraints: maybe insist that there be exactly 0 or 1?]
			
			Resource hasGroup = schema.createResource(Schema.PFX_BAT + "assign" + name);
			schema.add(hasGroup, rdfType, owlProp);
			schema.add(hasGroup, subProp, hasAssign);
			schema.add(hasGroup, subDomain, clsProp);
			
			// go through the value URI list; each one of them gets its own property
			for (String valueURI : categories.get(propURI))
			{
				if (valueURI == null) continue;
				name = turnLabelIntoName(uriToLabel.get(valueURI));
				if (name == null) continue;
				
				Resource hasValue = schema.createResource(Schema.PFX_BAT + "assign" + name);
				schema.add(hasValue, rdfType, owlProp);
				schema.add(hasValue, subProp, hasGroup);
				schema.add(hasValue, subDomain, clsProp);
				schema.add(hasValue, subRange, valueURI);
			}			
		}*/
	}
	
	/*private String findURIForLabel(String label)
	{
		String[] list = labelToURI.get(label);
		if (list == null) return null; //return "**!notfound!**";
		return list[0];
	}*/
	
	private String turnLabelIntoName(String label)
	{
		if (label == null) return null;
		StringBuffer buff = new StringBuffer();
		for (String bit : label.split(" "))
    	{
    		if (bit.length() == 0) continue;
    		char[] chars = new char[bit.length()];
    		bit.getChars(0, bit.length(), chars, 0);
    		chars[0] = Character.toUpperCase(chars[0]);
    		for (char ch : chars) if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9'))
    			buff.append(ch);
    	}
    	return buff.toString();
	}
}
