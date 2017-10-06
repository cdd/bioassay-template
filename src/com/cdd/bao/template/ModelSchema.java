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

package com.cdd.bao.template;

import com.cdd.bao.*;
import com.cdd.bao.template.Schema.*;
import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;

/*
	Model Schema: serialisation and deserialisation of a Schema using RDF triples. It can be used either to wrap the
	datastructure in a local file (using Turtle format), or to push & pull between a SPARQL endpoint.
*/

public class ModelSchema
{
	public static final String PFX_BAO = "http://www.bioassayontology.org/bao#"; // BioAssay Ontology
	public static final String PFX_BAT = "http://www.bioassayontology.org/bat#"; // BioAssay Template
	public static final String PFX_BAS = "http://www.bioassayontology.org/bas#"; // BioAssay Schema (used as the default)
	public static final String PFX_BAE = "http://www.bioassayexpress.org/bae#"; // BioAssay Express (for provisional terms)
	
	public static final String PFX_OBO = "http://purl.obolibrary.org/obo/";
	public static final String PFX_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String PFX_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	public static final String PFX_XSD = "http://www.w3.org/2001/XMLSchema#";
	public static final String PFX_OWL = "http://www.w3.org/2002/07/owl#";
	public static final String PFX_UO = "http://purl.org/obo/owl/UO#";
	public static final String PFX_DTO = "http://www.drugtargetontology.org/dto/";
	public static final String PFX_GENEID = "http://www.bioassayontology.org/geneid#";
	public static final String PFX_TAXON = "http://www.bioassayontology.org/taxon#";
	public static final String PFX_PROTEIN = "http://www.bioassayontology.org/protein#";
	public static final String PFX_PROV = "http://www.w3.org/ns/prov#";
	public static final String PFX_ASTRAZENECA = "http://rdf.astrazeneca.com/bae#";

	public static final String BAT_ROOT = "BioAssayTemplate"; // root should be one of these, as well as a group
	public static final String BAT_ASSAY = "BioAssayDescription"; // there should be zero-or-more of these in the schema file
	
	// a group is made up of groups & assignments
	public static final String BAT_GROUP = "Group";
	public static final String HAS_GROUP = "hasGroup";
	public static final String BAT_ASSIGNMENT = "Assignment";
	public static final String HAS_ASSIGNMENT = "hasAssignment";

	public static final String HAS_DESCRIPTION = "hasDescription"; // longwinded version of rdf:label
	public static final String IN_ORDER = "inOrder"; // each group/assignment can have one of these
	
	public static final String HAS_GROUPURI = "hasGroupURI"; // maps to identifier property (one per assignment)
	public static final String HAS_PROPERTY = "hasProperty"; // maps to predicate (one per assignment)
	public static final String HAS_VALUE = "hasValue"; // contains a value option (many per assignment)	
	public static final String MAPS_TO = "mapsTo";

	public static final String HAS_PARAGRAPH = "hasParagraph"; // text description of the assay, if available
	public static final String HAS_ORIGIN = "hasOrigin"; // origin URI: where the assay came from
	public static final String USES_TEMPLATE = "usesTemplate"; // linking an assay description to a template
	
	public static final String HAS_ANNOTATION = "hasAnnotation"; // connecting an annotation to an assay
	public static final String IS_ASSIGNMENT = "isAssignment"; // connecting an annotation to an assignment
	public static final String HAS_LITERAL = "hasLiteral"; // used for annotations
	public static final String IS_EXCLUDE = "isExclude"; // indicates a value refers to something to exclude
	public static final String IS_WHOLEBRANCH = "isWholeBranch"; // indicates a value refers to an entire branch, not just one term
	public static final String IS_EXCLUDEBRANCH = "isExcludeBranch"; // indicates a value refers to an entire branch to exclude
	
	public static final String SUGGESTIONS_FULL = "suggestionsFull"; // normal state: all suggestion modelling options enabled
	public static final String SUGGESTIONS_DISABLED = "suggestionsDisabled"; // do not use for suggestion models (neither input nor output)
	public static final String SUGGESTIONS_FIELD = "suggestionsField"; // suggestions based on auxiliary field names, not URIs
	public static final String SUGGESTIONS_URL = "suggestionsURL"; // use URLs to arbitrary resources
	public static final String SUGGESTIONS_ID = "suggestionsID"; // use ID codes for other assays
	public static final String SUGGESTIONS_STRING = "suggestionsString"; // use string literals
	public static final String SUGGESTIONS_NUMBER = "suggestionsNumber"; // use number-formatted literals
	public static final String SUGGESTIONS_INTEGER = "suggestionsInteger"; // use integer-formatted literals

	private Vocabulary vocab; // local instance of the BAO ontology: often initialised on demand/background thread
	private int watermark = 1; // autogenned next editable identifier

	private Property rdfLabel, rdfType;
	private Resource batRoot, batAssay;
	private Resource batGroup, batAssignment;
	private Property hasGroup, hasAssignment;
	private Property hasDescription, inOrder, hasParagraph, hasOrigin, usesTemplate;
	private Property hasGroupURI, hasProperty, hasValue;
	private Property mapsTo;
	private Property hasAnnotation, isAssignment, hasLiteral, isExclude, isWholeBranch, isExcludeBranch;
	private Property suggestionsFull, suggestionsDisabled, suggestionsField;
	private Property suggestionsURL, suggestionsID;
	private Property suggestionsString, suggestionsNumber, suggestionsInteger;

	// data used only during serialisation
	private Map<String, Integer> nameCounts; // ensures no name clashes
	private Map<Assignment, Resource> assignmentToResource; // stashes the model resource per assignment
	private Map<Resource, Assignment> resourceToAssignment; // or vice versa for loading

	private Schema schema;
	private Model model;
	private String rootURI = null; // set after a serialisation: this is the top level URI within the model

	// ------------ static methods ------------	
	
	private static final String[] PREFIX_MAP = new String[]
	{
		"bao:", PFX_BAO,
		"bat:", PFX_BAT,
		"bas:", PFX_BAS,
		"bae:", PFX_BAE,
		"obo:", PFX_OBO,
		"rdf:", PFX_RDF,
		"rdfs:", PFX_RDFS,
		"xsd:", PFX_XSD,
		"owl:", PFX_OWL,
		"uo:", PFX_UO,
		"dto:", PFX_DTO,
		"geneid:", PFX_GENEID,
		"taxon:", PFX_TAXON,
		"protein:", PFX_PROTEIN,
		"prov:", PFX_PROV,
		"az:", PFX_ASTRAZENECA,
	};
	
	// in case the caller needs to know what htye are
	public static Map<String, String> getPrefixes()
	{
		Map<String, String> map = new LinkedHashMap<>();
		for (int n = 0; n < PREFIX_MAP.length; n += 2) map.put(PREFIX_MAP[n], PREFIX_MAP[n + 1]);
		return map;
	}
	
	// if the given URI has one of the common prefixes, replace it with the abbreviated version; if none, returns same as input
	public static String collapsePrefix(String uri)
	{
		if (uri == null) return null;
		for (int n = 0; n < PREFIX_MAP.length; n += 2)
		{
			final String pfx = PREFIX_MAP[n], stem = PREFIX_MAP[n + 1];
			if (uri.startsWith(stem)) return pfx + uri.substring(stem.length());
		}
		return uri;
	}
	
	// if the given proto-URI starts with one of the common prefixes, replace it with the actual URI root stem; if none, returns same as input
	public static String expandPrefix(String uri)
	{
		if (uri == null) return null;
		for (int n = 0; n < PREFIX_MAP.length; n += 2)
		{
			final String pfx = PREFIX_MAP[n], stem = PREFIX_MAP[n + 1];
			if (uri.startsWith(pfx)) return stem + uri.substring(pfx.length());
		}
		return uri;
	}

	// ------------ private data: content ------------	

	private ModelSchema(Schema schema, Model model)
	{
		this.schema = schema;
		this.model = model;

		setupResources();
	}
	
	// access to the content
	public Schema getSchema() {return schema;}
	
	// load in previously saved file
	public static Schema deserialise(File file) throws IOException
	{
		try
		{
			FileInputStream istr = new FileInputStream(file);
			Schema schema = deserialise(istr);
			istr.close();
			return schema;
		}
		catch (IOException ex) {throw new IOException("Loading schema file failed [" + file.getAbsolutePath() + "].", ex);}
	}
	public static Schema deserialise(InputStream istr) throws IOException
	{
		Model model = ModelFactory.createDefaultModel();
		try {RDFDataMgr.read(model, istr, Lang.TTL);}
		catch (Exception ex) {throw new IOException("Failed to parse schema", ex);}

		ModelSchema thing = new ModelSchema(new Schema(), model);
		thing.parseFromModel();
		return thing.getSchema();
	}
	
	// serialisation: writes the schema using RDF "turtle" format, using OWL classes
	public static void serialise(Schema schema, File file) throws IOException
	{
		BufferedOutputStream ostr = new BufferedOutputStream(new FileOutputStream(file));
		serialise(schema, ostr);
		ostr.close();
	}
	public static void serialise(Schema schema, OutputStream ostr) throws IOException
	{
		Model model = ModelFactory.createDefaultModel();
		ModelSchema thing = new ModelSchema(schema, model);
		try {thing.exportToModel();}
		catch (Exception ex) {throw new IOException(ex);}
		
		RDFDataMgr.write(ostr, model, RDFFormat.TURTLE);
	}
	
	// writes the schema into a Jena triple collection
	public static String serialiseToModel(Schema schema, Model model) throws IOException
	{
		ModelSchema thing = new ModelSchema(schema, model);
		try {thing.exportToModel();}
		catch (Exception ex) {throw new IOException(ex);}
		return thing.rootURI;
	}
	
	// ------------ private methods ------------	

	private void setupResources()
	{
		model.setNsPrefix("bao", PFX_BAO);
		model.setNsPrefix("bat", PFX_BAT);
		model.setNsPrefix("bas", schema.getSchemaPrefix());
		model.setNsPrefix("obo", PFX_OBO);
		model.setNsPrefix("rdfs", PFX_RDFS);
		model.setNsPrefix("xsd", PFX_XSD);
		model.setNsPrefix("rdf", PFX_RDF);
		model.setNsPrefix("dto", PFX_DTO);

		rdfLabel = model.createProperty(PFX_RDFS + "label");
		rdfType = model.createProperty(PFX_RDF + "type");

		batRoot = model.createResource(PFX_BAT + BAT_ROOT);
		batAssay = model.createResource(PFX_BAT + BAT_ASSAY);
		batGroup = model.createResource(PFX_BAT + BAT_GROUP);
		batAssignment = model.createResource(PFX_BAT + BAT_ASSIGNMENT);
		hasGroup = model.createProperty(PFX_BAT + HAS_GROUP);
		hasAssignment = model.createProperty(PFX_BAT + HAS_ASSIGNMENT);
		hasDescription = model.createProperty(PFX_BAT + HAS_DESCRIPTION);
		inOrder = model.createProperty(PFX_BAT + IN_ORDER);
		hasGroupURI = model.createProperty(PFX_BAT + HAS_GROUPURI);
		hasProperty = model.createProperty(PFX_BAT + HAS_PROPERTY);
		hasValue = model.createProperty(PFX_BAT + HAS_VALUE);
		mapsTo = model.createProperty(PFX_BAT + MAPS_TO);
		hasParagraph = model.createProperty(PFX_BAT + HAS_PARAGRAPH);
		hasOrigin = model.createProperty(PFX_BAT + HAS_ORIGIN);
		usesTemplate = model.createProperty(PFX_BAT + USES_TEMPLATE);
		hasAnnotation = model.createProperty(PFX_BAT + HAS_ANNOTATION);
		isAssignment = model.createProperty(PFX_BAT + IS_ASSIGNMENT);
		hasLiteral = model.createProperty(PFX_BAT + HAS_LITERAL);
		isExclude = model.createProperty(PFX_BAT + IS_EXCLUDE);
		isWholeBranch = model.createProperty(PFX_BAT + IS_WHOLEBRANCH);
		isExcludeBranch = model.createProperty(PFX_BAT + IS_EXCLUDEBRANCH);
		suggestionsFull = model.createProperty(PFX_BAT + SUGGESTIONS_FULL);
		suggestionsDisabled = model.createProperty(PFX_BAT + SUGGESTIONS_DISABLED);
		suggestionsField = model.createProperty(PFX_BAT + SUGGESTIONS_FIELD);
		suggestionsURL = model.createProperty(PFX_BAT + SUGGESTIONS_URL);
		suggestionsID = model.createProperty(PFX_BAT + SUGGESTIONS_ID);
		suggestionsString = model.createProperty(PFX_BAT + SUGGESTIONS_STRING);
		suggestionsNumber = model.createProperty(PFX_BAT + SUGGESTIONS_NUMBER);
		suggestionsInteger = model.createProperty(PFX_BAT + SUGGESTIONS_INTEGER);

		nameCounts = new HashMap<>();
		assignmentToResource = new HashMap<>();
	}

	private void exportToModel()
	{
		Group root = schema.getRoot();
		String pfx = schema.getSchemaPrefix();
	
		rootURI = pfx + turnLabelIntoName(root.name);
		Resource objRoot = model.createResource(rootURI);
		model.add(objRoot, rdfType, batRoot);
		model.add(objRoot, rdfType, batGroup);
		model.add(objRoot, rdfLabel, root.name);
		if (root.descr.length() > 0) model.add(objRoot, hasDescription, root.descr);
		
		formulateGroup(objRoot, root);

		for (int n = 0; n < schema.numAssays(); n++)
		{
			Assay assay = schema.getAssay(n);
			Resource objAssay = model.createResource(pfx + turnLabelIntoName(assay.name));
			model.add(objAssay, rdfType, batAssay);
			model.add(objAssay, rdfLabel, assay.name);
			model.add(objAssay, usesTemplate, objRoot);
			if (assay.descr.length() > 0) model.add(objAssay, hasDescription, assay.descr);
			if (assay.para.length() > 0) model.add(objAssay, hasParagraph, assay.para);
			if (assay.originURI.length() > 0) model.add(objAssay, hasOrigin, assay.originURI.trim());
			model.add(objAssay, inOrder, model.createTypedLiteral(n + 1));
			
			for (int i = 0; i < assay.annotations.size(); i++)
			{
				Annotation annot = assay.annotations.get(i);
			
				Resource blank = model.createResource();
				model.add(objAssay, hasAnnotation, blank);

				// looks up the assignment in the overall hierarchy, to obtain the recently-created URI
				Assignment assn = schema.findAssignment(annot);
				if (assn != null) model.add(blank, isAssignment, assignmentToResource.get(assn));

				// emits either value or literal, with any accompanying decoration
				if (annot.value != null)
				{
					model.add(blank, hasProperty, model.createResource(annot.assn.propURI)); // note: using propURI stored in its own linear branch
					model.add(blank, hasValue, model.createResource(annot.value.uri));
					if (annot.value.name.length() > 0) model.add(blank, rdfLabel, model.createLiteral(annot.value.name));
					if (annot.value.descr.length() > 0) model.add(blank, hasDescription, model.createLiteral(annot.value.descr));
				}
				else
				{
					model.add(blank, hasLiteral, model.createLiteral(annot.literal));
				}
			}
		}
	}

	private void formulateGroup(Resource objParent, Group group)
	{
		int order = 0;
		String pfx = schema.getSchemaPrefix();
		
 		for (Assignment assn : group.assignments)
		{
			String name = turnLabelIntoName(assn.name);
						
			Resource objAssn = model.createResource(pfx + name);
			model.add(objParent, hasAssignment, objAssn);
			model.add(objAssn, rdfType, batAssignment);
			model.add(objAssn, rdfLabel, assn.name);
			if (assn.descr.length() > 0) model.add(objAssn, hasDescription, assn.descr);
			model.add(objAssn, inOrder, model.createTypedLiteral(++order));
			if (isURI(assn.propURI)) model.add(objAssn, hasProperty, model.createResource(assn.propURI.trim()));
			
			if (assn.suggestions == Schema.Suggestions.FULL) model.add(objAssn, suggestionsFull, model.createTypedLiteral(true));
			else if (assn.suggestions == Schema.Suggestions.DISABLED) model.add(objAssn, suggestionsDisabled, model.createTypedLiteral(true));
			else if (assn.suggestions == Schema.Suggestions.FIELD) model.add(objAssn, suggestionsField, model.createTypedLiteral(true));
			else if (assn.suggestions == Schema.Suggestions.URL) model.add(objAssn, suggestionsURL, model.createTypedLiteral(true));
			else if (assn.suggestions == Schema.Suggestions.ID) model.add(objAssn, suggestionsID, model.createTypedLiteral(true));
			else if (assn.suggestions == Schema.Suggestions.STRING) model.add(objAssn, suggestionsString, model.createTypedLiteral(true));
			else if (assn.suggestions == Schema.Suggestions.NUMBER) model.add(objAssn, suggestionsNumber, model.createTypedLiteral(true));
			else if (assn.suggestions == Schema.Suggestions.INTEGER) model.add(objAssn, suggestionsInteger, model.createTypedLiteral(true));
			
			int vorder = 0;
			for (Value val : assn.values)
			{
				Resource blank = model.createResource();		
				model.add(objAssn, hasValue, blank);
				
				Resource objValue = isURI(val.uri) ? model.createResource(val.uri.trim()) : null;
				if (objValue != null) model.add(blank, mapsTo, objValue);
				model.add(blank, rdfLabel, model.createLiteral(val.name));
				if (val.descr.length() > 0) model.add(blank, hasDescription, model.createLiteral(val.descr));

				if (val.spec == Schema.Specify.EXCLUDE) model.add(blank, isExclude, model.createTypedLiteral(true));
				else if (val.spec == Schema.Specify.WHOLEBRANCH) model.add(blank, isWholeBranch, model.createTypedLiteral(true));
				else if (val.spec == Schema.Specify.EXCLUDEBRANCH) model.add(blank, isExcludeBranch, model.createTypedLiteral(true));

				model.add(blank, inOrder, model.createTypedLiteral(++vorder));
			}
			
			assignmentToResource.put(assn, objAssn); // for subsequent retrieval
		}
		
		// recursively emit any subgroups
		String parentName = turnLabelIntoName(group.name);
		for (Group subgrp : group.subGroups)
		{
	    		Resource objGroup = model.createResource(pfx + turnLabelIntoName(subgrp.name));
	    		model.add(objParent, hasGroup, objGroup);
	    		model.add(objGroup, rdfType, batGroup);
	    		model.add(objGroup, rdfLabel, subgrp.name);
	    		if (subgrp.descr.length() > 0) model.add(objGroup, hasDescription, subgrp.descr);
			model.add(objGroup, inOrder, model.createTypedLiteral(++order));
			if (isURI(subgrp.groupURI)) model.add(objGroup, hasGroupURI, model.createResource(subgrp.groupURI.trim()));
    		
    			formulateGroup(objGroup, subgrp);
		}
	}

	// pull in an RDF-compatible file, and pull out the model information
	private void parseFromModel() throws IOException
	{
		// extract the template
		Resource objRoot = null;
		for (StmtIterator it = model.listStatements(null, rdfType, batRoot); it.hasNext();)
		{
			objRoot = it.next().getSubject();
			break;
		}
		if (objRoot == null) throw new IOException("No template root found: this is probably not a bioassay template file.");
		
		String rootURI = objRoot.toString();
		int pfxsz = rootURI.lastIndexOf('#');
		if (pfxsz > 0) schema.setSchemaPrefix(rootURI.substring(0, pfxsz + 1));

		Group root = new Group(null, findString(objRoot, rdfLabel));
		schema.setRoot(root);
		root.descr = findString(objRoot, hasDescription);
		
		resourceToAssignment = new HashMap<>();
		
		parseGroup(objRoot, root);

		// extract each of the assays
		Map<Object, Integer> order = new HashMap<>();
		List<Assay> assayList = new ArrayList<>();
		for (StmtIterator it = model.listStatements(null, rdfType, batAssay); it.hasNext();)
		{
			Resource objAssay = it.next().getSubject();
			
			Assay assay = parseAssay(objAssay);
			assayList.add(assay);
			order.put(assay, findInteger(objAssay, inOrder));
		}
		assayList.sort((a1, a2) -> order.get(a1).compareTo(order.get(a2)));
		for (Assay assay : assayList) schema.appendAssay(assay);
	}
	
	// for a given category node, pulls out and parses all of its assignments and subcategories
	private void parseGroup(Resource objParent, Group group) throws IOException
	{
		final Map<Object, Integer> order = new HashMap<>();
	
		// look for assignments
		for (StmtIterator it = model.listStatements(objParent, hasAssignment, (RDFNode)null); it.hasNext();)
		{
			Statement st = it.next();
			Resource objAssn = (Resource)st.getObject();
			
			//Util.writeln("Cat:"+category.categoryName+ " prop:"+clsProp.toString());
			
			Assignment assn = parseAssignment(group, objAssn);
			group.assignments.add(assn);
			order.put(assn, findInteger(objAssn, inOrder));
			
			resourceToAssignment.put(objAssn, assn);
		}
		group.assignments.sort((a1, a2) -> order.get(a1).compareTo(order.get(a2)));
		
		// look for subcategories
		order.clear();
		for (StmtIterator it = model.listStatements(objParent, hasGroup, (RDFNode)null); it.hasNext();)
		{
			Statement st = it.next();
			Resource objGroup = (Resource)st.getObject();
			
	    		Group subgrp = new Group(group, findString(objGroup, rdfLabel), findAsString(objGroup, hasGroupURI));
	    		subgrp.descr = findString(objGroup, hasDescription);
	    		parseGroup(objGroup, subgrp);

	    		group.subGroups.add(subgrp);
	    		order.put(subgrp, findInteger(objGroup, inOrder));
		}
		group.subGroups.sort((g1, g2) -> order.get(g1).compareTo(order.get(g2)));
	}
	
	private Assignment parseAssignment(Group group, Resource objAssn) throws IOException
	{
		Assignment assn = new Assignment(group, findString(objAssn, rdfLabel), findAsString(objAssn, hasProperty));
		if (assn.propURI.startsWith("file://")) assn.propURI = "";
		assn.descr = findString(objAssn, hasDescription);
		
		assn.suggestions = findBoolean(objAssn, suggestionsFull) ? Schema.Suggestions.FULL :
						   findBoolean(objAssn, suggestionsDisabled) ? Schema.Suggestions.DISABLED :
						   findBoolean(objAssn, suggestionsField) ? Schema.Suggestions.FIELD : 
						   findBoolean(objAssn, suggestionsURL) ? Schema.Suggestions.URL : 
						   findBoolean(objAssn, suggestionsID) ? Schema.Suggestions.ID : 
						   findBoolean(objAssn, suggestionsString) ? Schema.Suggestions.STRING : 
						   findBoolean(objAssn, suggestionsNumber) ? Schema.Suggestions.NUMBER : 
						   findBoolean(objAssn, suggestionsInteger) ? Schema.Suggestions.INTEGER : Schema.Suggestions.FULL;
		
		Map<Object, Integer> order = new HashMap<>();

		for (StmtIterator it = model.listStatements(objAssn, hasValue, (RDFNode)null); it.hasNext();)
		{
			Resource blank = (Resource)it.next().getObject();
					
			Value val = new Value(findAsString(blank, mapsTo), findString(blank, rdfLabel));
			if (val.uri.startsWith("file://")) val.uri = "";
			val.descr = findString(blank, hasDescription);
			val.spec = findBoolean(blank, isExclude) ? Schema.Specify.EXCLUDE :
					   findBoolean(blank, isWholeBranch) ? Schema.Specify.WHOLEBRANCH :
					   findBoolean(blank, isExcludeBranch) ? Schema.Specify.EXCLUDEBRANCH : Schema.Specify.ITEM;
			assn.values.add(val);
			order.put(val, findInteger(blank, inOrder));
		}

		assn.values.sort((a1, a2) -> order.get(a1).compareTo(order.get(a2)));

		return assn;
	}
	
	private Assay parseAssay(Resource objAssay)
	{
		Assay assay = new Assay(findString(objAssay, rdfLabel));
		
		assay.descr = findString(objAssay, hasDescription);
		assay.para = findString(objAssay, hasParagraph);
		assay.originURI = findString(objAssay, hasOrigin);
		
		for (StmtIterator it = model.listStatements(objAssay, hasAnnotation, (RDFNode)null); it.hasNext();)
		{
			Resource blank = (Resource)it.next().getObject();

			Resource assnURI = findResource(blank, isAssignment);
			String label = findString(blank, rdfLabel);
			String descr = findString(blank, hasDescription);
			Resource propURI = findResource(blank, hasProperty);
			Resource valueURI = findResource(blank, hasValue);
			String valueLiteral = findString(blank, hasLiteral);

			// lookup the assignment in the template and if none, make a fake one (will be treated as an "orphan")
			Assignment assn = resourceToAssignment.get(assnURI);
			if (assn == null)
			{	
				// !! not saving enough information to recreate a dummy assignment
				continue;
			}

			// create the annotation
			if (valueURI != null)
			{
				Annotation annot = new Annotation(assn, new Value(valueURI.toString(), label));
				annot.value.descr = descr;
				assay.annotations.add(annot);
			}
			else if (valueLiteral.length() > 0)
			{
				Annotation annot = new Annotation(assn, valueLiteral);
				assay.annotations.add(annot);
			}
			// (else ignore)
		}
	
		return assay;
	}
	
	private String turnLabelIntoName(String label)
	{
		if (label == null) return null;
		if (label.length() == 0) label = "unnamed";
		
		StringBuffer buff = new StringBuffer();
		for (String bit : label.split(" "))
	    	{
	    		if (bit.length() == 0) continue;
	    		char[] chars = new char[bit.length()];
	    		bit.getChars(0, bit.length(), chars, 0);
	    		chars[0] = Character.toUpperCase(chars[0]);
	    		for (char ch : chars) if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) buff.append(ch);
	    	}
    	
	    	// if the name was previously encountered, give it a number suffix to disambiguate
	    	String name = buff.toString();
	    	Integer count = nameCounts.get(name);
	    	if (count != null)
	    	{
	    		count++;
	    		nameCounts.put(name, count);
	    		name += count;
	    	}
	    	else nameCounts.put(name, 1);
	    	
	    	return name;    	
	}
	
	// looks for an assignment and returns it as a string regardless of what type it actually is; blank if not found
	private String findAsString(Resource subj, Property prop)
	{
		for (StmtIterator it = model.listStatements(subj, prop, (RDFNode)null); it.hasNext();) 
		{
			RDFNode obj = it.next().getObject();
			if (obj.isLiteral()) return obj.asLiteral().getString();
			return obj.toString();
		}
		return "";
	}

	// looks up a specific string, typically a label or similar; returns blank string if not found
	private String findString(Resource subj, Property prop)
	{
		for (StmtIterator it = model.listStatements(subj, prop, (RDFNode)null); it.hasNext();)
		{
			RDFNode obj = it.next().getObject();
			if (obj.isLiteral()) return obj.asLiteral().getString();
		}
		return "";
	}
	
	// looks for an explicitly typed integer; returns 0 if not found
	private int findInteger(Resource subj, Property prop)
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

	// looks for an explicitly typed boolean; returns false if not found	
	private boolean findBoolean(Resource subj, Property prop)
	{
		for (StmtIterator it = model.listStatements(subj, prop, (RDFNode)null); it.hasNext();)
		{
			RDFNode obj = it.next().getObject();
			if (obj.isLiteral())
			{
				Literal lit = obj.asLiteral();
				if (lit.getValue() instanceof Object) return lit.getBoolean();
			}
		}
		return false;
	}

	// look for a URI node; returns null if none
	private Resource findResource(Resource subj, Property prop)
	{
		for (StmtIterator it = model.listStatements(subj, prop, (RDFNode)null); it.hasNext();)
		{
			RDFNode obj = it.next().getObject();
			if (obj.isResource()) return obj.asResource();
		}
		return null;
	}
	
	// returns true if the string can be reasonably interpreted as a URI
	private boolean isURI(String uri)
	{
		if (uri == null) return false;
		uri = uri.trim();
		return uri.startsWith("http://") || uri.startsWith("https://");
	}
}


