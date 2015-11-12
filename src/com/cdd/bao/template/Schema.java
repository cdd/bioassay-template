/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.template;

import com.cdd.bao.*;

import java.io.*;
import java.util.*;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;

/*
	Schema: functionality for encapsulating a "BioAssay Template" document. The serialisation format is an OWL
	file consisting of RDF triples used to describe a schema that instructs on how to use the BioAssay Ontology
	(and related vocabulatory) to mark up an biological assay.
*/

public class Schema
{
	public static final String PFX_BAO = "http://www.bioassayontology.org/bao#"; // BioAssay Ontology
	public static final String PFX_BAT = "http://www.bioassayontology.org/bat#"; // BioAssay Template
	
	public static final String PFX_OBO = "http://purl.obolibrary.org/obo/";
	public static final String PFX_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String PFX_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	public static final String PFX_XSD = "http://www.w3.org/2001/XMLSchema#";

	public static final String BAT_ROOT = "BioAssayTemplate"; // root should be one of these, as well as a group
	
	// a group is made up of groups & assignments
	public static final String BAT_GROUP = "Group";
	public static final String HAS_GROUP = "hasGroup";
	public static final String BAT_ASSIGNMENT = "Assignment";
	public static final String HAS_ASSIGNMENT = "hasAssignment";

	public static final String HAS_DESCRIPTION = "hasDescription"; // longwinded version of rdf:label
	public static final String IN_ORDER = "inOrder"; // each group/assignment can have one of these
	
	public static final String HAS_PROPERTY = "hasProperty"; // maps to predicate (one per assignment)
	public static final String HAS_VALUE = "hasValue"; // contains a value option (many per assignment)	
	public static final String MAPS_TO = "mapsTo";
	

	// ------------ private data ------------	

	private Vocabulary vocab;

	public static final class Value
	{
		public String uri; // mapping to a URI in the BAO or related ontology; if null, is literal
		public String name; // short label for the value; if no URI, this is the literal to use
		public String descr = ""; // longer description
		
		public Value(String uri, String name)
		{
			this.uri = uri;
			this.name = name;
		}
	}

	public static final class Assignment
	{
		public String assnName, assnDescr = "";
		public String propURI;
		// !! exclusivity
		public List<Value> values = new ArrayList<>();
		
		public Assignment(String assnName, String propURI) 
		{
			this.assnName = assnName;
			this.propURI = propURI; 
		}
		
		private void outputAsString(StringBuffer buff, int indent)
		{
			for (int n = 0; n < indent; n++) buff.append("  ");
			buff.append("<" + assnName + "> " + propURI + "\n");
			for (Value val : values)
			{
				for (int n = 0; n <= indent; n++) buff.append("  ");
				buff.append(val.uri + " : " + val.name + " (" + val.descr + "\n");
			}
		}
	};
	private List<Assignment> rootAssignments = new ArrayList<Assignment>();

	public static final class Group
	{
		public String groupName, groupDescr = "";
		public List<Assignment> assignments = new ArrayList<>();
		public List<Group> subGroups = new ArrayList<>();
		
		public Group(String groupName) 
		{
			this.groupName = groupName;
		}
		
		private void outputAsString(StringBuffer buff, int indent)
		{
			for (int n = 0; n < indent; n++) buff.append("  ");
			buff.append("[" + groupName + "]\n");
			for (Assignment assn : assignments) assn.outputAsString(buff, indent + 1);
			for (Group grp : subGroups) grp.outputAsString(buff, indent + 1);
		}
	};
	private Group root = new Group("common assay template");

	// defined during [de]serialisation
	private Property rdfLabel, rdfType;
	private Resource batRoot;
	private Resource batGroup, batAssignment;
	private Property hasGroup, hasAssignment;
	private Property hasDescription, inOrder;
	private Property hasProperty, hasValue;
	private Property mapsTo;
	
	private Map<String, Integer> nameCounts; // restart this for each serialisation: ensures no name clashes

	// ------------ public methods ------------	

	public Schema(Vocabulary vocab)
	{
		this.vocab = vocab;
	}
	
	// returns the top level category: all of the assignments and subcategories are considered to be
	// connected to the primary assay description, and the root's category name is a description of this
	// particular assay schema template
	public Group getRoot() {return root;}
	
	public String toString()
	{
		StringBuffer buff = new StringBuffer();
		root.outputAsString(buff, 0);
		return buff.toString();
	}
	
	// load in previously saved file
	public static Schema deserialise(File file) throws IOException
	{
		FileInputStream istr = new FileInputStream(file);
		Schema schema = deserialise(istr);
		istr.close();
		return schema;
	}
	public static Schema deserialise(InputStream istr) throws IOException
	{
		Schema schema = new Schema(null);
		schema.parseFromStream(istr);
		return schema;
	}
	
	// serialisation: writes the schema using RDF "turtle" format, using OWL classes
	public void serialise(File file) throws IOException
	{
		BufferedOutputStream ostr = new BufferedOutputStream(new FileOutputStream(file));
		serialise(ostr);
		ostr.close();
	}
	public void serialise(OutputStream ostr) throws IOException
	{
		Model model = ModelFactory.createDefaultModel();
				
		setupResources(model);
		
		Resource objRoot = model.createResource(PFX_BAT + turnLabelIntoName(root.groupName));
		model.add(objRoot, rdfType, batRoot);
		model.add(objRoot, rdfType, batGroup);
		model.add(objRoot, rdfLabel, root.groupName);
		if (root.groupDescr.length() > 0) model.add(objRoot, hasDescription, root.groupDescr);
		
		formulateGroup(model, objRoot, root);
		
		RDFDataMgr.write(ostr, model, RDFFormat.TURTLE);
	}

	// ------------ private methods ------------	

	private void setupResources(Model model)
	{
		model.setNsPrefix("bao", Schema.PFX_BAO);
		model.setNsPrefix("bat", Schema.PFX_BAT);
		model.setNsPrefix("obo", Schema.PFX_OBO);
		model.setNsPrefix("rdfs", Schema.PFX_RDFS);
		model.setNsPrefix("xsd", Schema.PFX_XSD);
		model.setNsPrefix("rdf", Schema.PFX_RDF);

		rdfLabel = model.createProperty(PFX_RDFS + "label");
		rdfType = model.createProperty(PFX_RDF + "type");

		batRoot = model.createResource(PFX_BAT + BAT_ROOT);
		batGroup = model.createResource(PFX_BAT + BAT_GROUP);
		batAssignment = model.createResource(PFX_BAT + BAT_ASSIGNMENT);
		hasGroup = model.createProperty(PFX_BAT + HAS_GROUP);
		hasAssignment = model.createProperty(PFX_BAT + HAS_ASSIGNMENT);
		hasDescription = model.createProperty(PFX_BAT + HAS_DESCRIPTION);
		inOrder = model.createProperty(PFX_BAT + IN_ORDER);
		hasProperty = model.createProperty(PFX_BAT + HAS_PROPERTY);
		hasValue = model.createProperty(PFX_BAT + HAS_VALUE);
		mapsTo = model.createProperty(PFX_BAT + MAPS_TO);
		
		nameCounts = new HashMap<String, Integer>();
	}

	private void formulateGroup(Model model, Resource objParent, Group group)
	{
		int order = 0;
		
 		for (Assignment assn : group.assignments)
		{
			String name = turnLabelIntoName(assn.assnName);
						
			Resource objAssn = model.createResource(PFX_BAT + name);
			model.add(objParent, hasAssignment, objAssn);
			model.add(objAssn, rdfType, batAssignment);
			model.add(objAssn, rdfLabel, assn.assnName);
			if (assn.assnDescr.length() > 0) model.add(objAssn, hasDescription, assn.assnDescr);
			model.add(objAssn, inOrder, model.createTypedLiteral(++order));
			model.add(objAssn, hasProperty, model.createResource(assn.propURI));
			
			int vorder = 0;
			for (Value val : assn.values)
			{
				Resource objValue = val.uri == null ? null : model.createResource(val.uri);
				
				Resource blank = model.createResource();
				
				model.add(objAssn, hasValue, blank);
				
				if (objValue != null) model.add(blank, mapsTo, objValue);
				model.add(blank, rdfLabel, model.createLiteral(val.name));
				if (val.descr.length() > 0) model.add(blank, hasDescription, model.createLiteral(val.descr));
				model.add(blank, inOrder, model.createTypedLiteral(++vorder));
			}
		}
		
		// recursively emit any subcategories
		String parentName = turnLabelIntoName(group.groupName);
		for (Group subgrp : group.subGroups)
		{
    		Resource objGroup = model.createResource(PFX_BAT + turnLabelIntoName(subgrp.groupName));
    		model.add(objParent, hasGroup, objGroup);
    		model.add(objGroup, rdfType, batGroup);
    		model.add(objGroup, rdfLabel, subgrp.groupName);
    		if (subgrp.groupDescr.length() > 0) model.add(objGroup, hasDescription, subgrp.groupDescr);
			model.add(objGroup, inOrder, model.createTypedLiteral(++order));
    		
    		formulateGroup(model, objGroup, subgrp);
		}
	}

	// pull in an RDF-compatible file, and pull out the model information
	private void parseFromStream(InputStream istr) throws IOException
	{
		Model model = ModelFactory.createDefaultModel();
		RDFDataMgr.read(model, istr, Lang.TTL);
	
		setupResources(model);	

		//model.add(objRoot, rdfType, batRoot);
		Resource objRoot = null;
		for (StmtIterator it = model.listStatements(null, rdfType, batRoot); it.hasNext();)
		{
			objRoot = it.next().getSubject();
			break;
		}
		if (objRoot == null) throw new IOException("No template root found: this is probably not a bioassay template file.");

		root = new Group(findString(model, objRoot, rdfLabel));
		root.groupDescr = findString(model, objRoot, hasDescription);
		
		parseGroup(model, objRoot, root);
	}
	
	// for a given category node, pulls out and parses all of its assignments and subcategories
	private void parseGroup(Model model, Resource objParent, Group group) throws IOException
	{
		// look for assignments
		for (StmtIterator it = model.listStatements(objParent, hasAssignment, (RDFNode)null); it.hasNext();)
		{
			Statement st = it.next();
			Resource objAssn = (Resource)st.getObject();
			
			//Util.writeln("Cat:"+category.categoryName+ " prop:"+clsProp.toString());
			
			Assignment assn = parseAssignment(model, objAssn);
			// !! order
			group.assignments.add(assn);
		}
		
		// look for subcategories
		//List<Integer> sgOrder = new ArrayList<>(); hash?? dump assn and group...
		for (StmtIterator it = model.listStatements(objParent, hasGroup, (RDFNode)null); it.hasNext();)
		{
			Statement st = it.next();
			Resource objGroup = (Resource)st.getObject();
			
    		Group subgrp = new Group(findString(model, objGroup, rdfLabel));
    		subgrp.groupDescr = findString(model, objGroup, hasDescription);
    		
    		// !! order
    		group.subGroups.add(subgrp);
    		
    		parseGroup(model, objGroup, subgrp);
		}
	}
	
	private Assignment parseAssignment(Model model, Resource objAssn) throws IOException
	{
		Assignment assn = new Assignment(findString(model, objAssn, rdfLabel), findAsString(model, objAssn, hasProperty));
		assn.assnDescr = findString(model, objAssn, hasDescription);
		
		for (StmtIterator it = model.listStatements(objAssn, hasValue, (RDFNode)null); it.hasNext();)
		{
			Resource blank = (Resource)it.next().getObject();
					
			Value val = new Value(findAsString(model, blank, mapsTo), findString(model, blank, rdfLabel));
			val.descr = findString(model, blank, hasDescription);
			assn.values.add(val);
		}

		return assn;
	}
	
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
    	
    	// if the name was previously encountered, give it a number suffix to disambiguate
    	String name = buff.toString();
    	Integer count = nameCounts.get(name);
    	if (count != null)
    	{
    		count++;
    		name += count;
    		nameCounts.put(name, count);
    	}
    	else nameCounts.put(name, count);
    	
    	return name;    	
	}
	
	// looks for an assignment and returns it as a string regardless of what type it actually is; blank if not found
	private String findAsString(Model model, Resource subj, Property prop)
	{
		for (StmtIterator it = model.listStatements(subj, prop, (RDFNode)null); it.hasNext();) return it.next().getObject().toString();
		return "";
	}

	// looks up a specific string, typically a label or similar; returns blank string if not found
	private String findString(Model model, Resource subj, Property prop)
	{
		for (StmtIterator it = model.listStatements(subj, prop, (RDFNode)null); it.hasNext();)
		{
			RDFNode obj = it.next().getObject();
			if (obj.isLiteral()) return obj.toString();
		}
		return "";
	}
	
	// looks for an explicitly typed integer; returns 0 if not found
	private int findInteger(Model model, Resource subj, Property prop)
	{
		for (StmtIterator it = model.listStatements(subj, prop, (RDFNode)null); it.hasNext();)
		{
			RDFNode obj = it.next().getObject();
			if (obj.isLiteral())
			{
				Literal lit = obj.asLiteral();
				if (lit.getValue() instanceof Object) return lit.getInt();
			}
		}
		return 0;
	}
	
	// look for a URI node; returns null if none
	private Resource findResource(Model model, Resource subj, Property prop)
	{
		for (StmtIterator it = model.listStatements(subj, prop, (RDFNode)null); it.hasNext();)
		{
			RDFNode obj = it.next().getObject();
			if (obj.isResource()) return obj.asResource();
		}
		return null;
	}
}