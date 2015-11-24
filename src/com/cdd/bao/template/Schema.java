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
	public static final String BAT_ASSAY = "BioAssayDescription"; // there should be zero-or-more of these in the schema file
	
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

	public static final String HAS_PARAGRAPH = "hasParagraph"; // text description of the assay, if available
	
	public static final String HAS_ANNOTATION = "hasAnnotation"; // connecting an annotation to an assay
	public static final String IS_ASSIGNMENT = "isAssignment"; // connecting an annotation to an assignment
	public static final String HAS_LITERAL = "hasLiteral"; // used for annotations

	// ------------ private data ------------	

	private Vocabulary vocab; // local instance of the BAO ontology: often initialised on demand/background thread
	private int watermark = 1; // autogenned next editable identifier

	// defined during [de]serialisation
	private Property rdfLabel, rdfType;
	private Resource batRoot, batAssay;
	private Resource batGroup, batAssignment;
	private Property hasGroup, hasAssignment;
	private Property hasDescription, inOrder, hasParagraph;
	private Property hasProperty, hasValue;
	private Property mapsTo;
	private Property hasAnnotation, isAssignment, hasLiteral;

	// a "group" is a collection of assignments and subgroups; a BioAssayTemplate is basically a single root group, and its descendent
	// contents make up the definition
	public static final class Group
	{
		public Group parent;
		public String name, descr = "";
		public List<Assignment> assignments = new ArrayList<>();
		public List<Group> subGroups = new ArrayList<>();
		
		public Group(Group parent, String name) 
		{
			this.parent = parent;
			this.name = name == null ? "" : name;
		}
		public Group clone() {return clone(parent);}
		public Group clone(Group parent)
		{
			Group dup = new Group(parent, name);
			dup.descr = descr;
			for (Assignment assn : assignments) dup.assignments.add(assn.clone(dup));
			for (Group grp : subGroups) dup.subGroups.add(grp.clone(dup));
			return dup;
		}
		public boolean equals(Group other)
		{
			if (!name.equals(other.name) || !descr.equals(other.descr)) return false;
			if (assignments.size() != other.assignments.size() || subGroups.size() != other.subGroups.size()) return false;
			for (int n = 0; n < assignments.size(); n++) if (!assignments.get(n).equals(other.assignments.get(n))) return false;
			for (int n = 0; n < subGroups.size(); n++) if (!subGroups.get(n).equals(other.subGroups.get(n))) return false;
			return true;
		}
		
		private void outputAsString(StringBuffer buff, int indent)
		{
			for (int n = 0; n < indent; n++) buff.append("  ");
			buff.append("[" + name + "] (" + descr + ")\n");
			for (Assignment assn : assignments) assn.outputAsString(buff, indent + 1);
			for (Group grp : subGroups) grp.outputAsString(buff, indent + 1);
		}
	};

	// an "assignment" is an instruction to associate a bioassay (subject) with a value (object) via a property (predicate); the datastructure
	// has a unique property URI, and a list of applicable values
	public static final class Assignment
	{
		public Group parent;
		public String name, descr = "";
		public String propURI;
		// !! exclusivity
		public List<Value> values = new ArrayList<>();
		
		public Assignment(Group parent, String name, String propURI) 
		{
			this.parent = parent;
			this.name = name == null ? "" : name;
			this.propURI = propURI == null ? "" : propURI; 
		}
		public Assignment clone() {return clone(parent);}
		public Assignment clone(Group parent)
		{
			Assignment dup = new Assignment(parent, name, propURI);
			dup.descr = descr;
			for (Value val : values) dup.values.add(val.clone());
			return dup;
		}
		public boolean equals(Assignment other)
		{
			if (!name.equals(other.name) || !descr.equals(other.descr) || !propURI.equals(other.propURI)) return false;
			if (values.size() != other.values.size()) return false;
			for (int n = 0; n < values.size(); n++) if (!values.get(n).equals(other.values.get(n))) return false;
			return true;
		}
		
		private void outputAsString(StringBuffer buff, int indent)
		{
			for (int n = 0; n < indent; n++) buff.append("  ");
			buff.append("<" + name + "> " + propURI + " (" + descr + ")\n");
			for (Value val : values)
			{
				for (int n = 0; n <= indent; n++) buff.append("  ");
				buff.append(val.uri + " : " + val.name + " (" + val.descr + ")\n");
			}
		}
	}

	// a "value" consists of a URI (in the case of references to a known resource), and descriptive text; an assignment typically has many of these
	public static final class Value
	{
		public String uri; // mapping to a URI in the BAO or related ontology; if null, is literal
		public String name; // short label for the value; if no URI, this is the literal to use
		public String descr = ""; // longer description
		
		public Value(String uri, String name)
		{
			this.uri = uri == null ? "" : uri;
			this.name = name == null ? "" : name;
		}
		public Value clone()
		{
			Value dup = new Value(uri, name);
			dup.descr = descr;
			return dup;
		}
		public boolean equals(Value other)
		{
			return uri.equals(other.uri) && name.equals(other.name) && descr.equals(other.descr);
		}
	}

	// template root: the schema definition resides within here
	private Group root = new Group(null, "common assay template");
	
	// an "assay" is an actual instance of the bioassay template, i.e. filling in the assignments with values
	public static final class Assay
	{
		public String name; // short label for the bioassay
		public String descr = ""; // more descriptive label: used to complement the semantic assignments
		public String para = ""; // plain text description of the assay, if available; sometimes this is available prior semantic assignments
		
		public List<Annotation> annotations = new ArrayList<>();
		
		public Assay(String name)
		{
			this.name = name == null ? "" : name;
		}
		public Assay clone()
		{
			Assay dup = new Assay(name);
			dup.descr = descr;
			dup.para = para;
			for (Annotation a : annotations) dup.annotations.add(a.clone());
			return dup;
		}
		public boolean equals(Assay other)
		{
			if (!name.equals(other.name) || !descr.equals(other.descr) || !para.equals(other.para)) return false;
			if (annotations.size() != other.annotations.size()) return false;
			for (int n = 0; n < annotations.size(); n++) if (!annotations.get(n).equals(other.annotations.get(n))) return false;
			
			return true;
		}
	}
	
	// an "annotation" is associated with an assay, linking it to an assignment and a selected value; note that the assignment/value objects are cloned
	// from the content in the template part of the schema, so they can be edited separately without doing too much damage - but this does mean that reconciliation
	// gets interesting, which is a necessary evil
	public static final class Annotation
	{
		public Assignment assn; // the assignment that the annotation corresponds to, as a linear-branched clone (never referenced to the schema assignment list)
		public Value value = null; // mutually exclusive with literal; instances are always cloned
		public String literal = null; // ditto/vv
		
		public Annotation() {assn = null;}
		public Annotation(Assignment assn, Value value)
		{
			this.assn = linearBranch(assn);
			this.value = value.clone();
		}
		public Annotation(Assignment assn, String literal)
		{
			this.assn = linearBranch(assn);
			this.literal = literal;
		}
		public Annotation clone()
		{
			Annotation dup = value != null ? new Annotation(assn, value) : new Annotation(assn, literal);
			return dup;
		}
		public boolean equals(Annotation other)
		{
			if (!assn.equals(other.assn)) return false;
			Group p1 = assn.parent, p2 = other.assn.parent;
			while (true)
			{
				if (p1 == null && p2 == null) break;
				if (p1 == null || p2 == null) return false;
				if (!p1.name.equals(p2.name)) return false;
				p1 = p1.parent;
				p2 = p2.parent;
			}
			
			if (value == null && other.value == null) return literal.equals(other.literal);
			else if (value != null && other.value == null) return false;
			else if (value == null && other.value != null) return false;
			return value.equals(other.value);
		}
		
		// clones the assignment, and re-manufactures the whole branch, except makes it linear: the parent group sequence will therefore check out, in terms of being able to 
		// recreate the tree with object names; this will enable comparison of annotations between two schema instances, with a significant amount of slack, i.e. if the schema is
		// being changed, or is temporarily invalid, then most annotations will still be able to correlate the assignments unambiguously, and those that cannot will still provide
		// a lot of clues for reconciliation; this is all in aid of the expectation that users will do some fairly radical schema refactorings, even after staying to annotate assays
		public static Assignment linearBranch(Assignment assn)
		{
			Group par = assn.parent;
			Group dup = new Group(null, par.name);
			dup.descr = par.descr;
			dup.assignments.add(assn);

			assn = assn.clone(dup);

			while (par.parent != null)
			{
				dup.parent = new Group(null, par.parent.name);
				dup.parent.descr = par.parent.descr;
				dup.parent.subGroups.add(dup);
				
				par = par.parent;
				dup = par.parent;
			}
			
			return assn;
		}
	}
	
	private List<Assay> assays = new ArrayList<>();
	
	// data used only during serialisation
	private Map<String, Integer> nameCounts; // ensures no name clashes
	private Map<Assignment, Resource> assignmentToResource; // stashes the model resource per assignment
	private Map<Resource, Assignment> resourceToAssignment; // or vice versa for loading

	// ------------ public methods ------------	

	public Schema(Vocabulary vocab)
	{
		this.vocab = vocab;
	}
	
	// returns true if the content is literally equivalent
	public boolean equals(Schema other)
	{
		if (!root.equals(other.root)) return false;
		if (assays.size() != other.assays.size()) return false;
		for (int n = 0; n < assays.size(); n++) if (!assays.get(n).equals(other.assays.get(n))) return false;
		return true;
	}
	
	// makes a deep copy of the schema content
	public Schema clone()
	{
		Schema dup = new Schema(vocab);
		dup.root = root.clone(null);
		for (Assay a : assays) dup.assays.add(a.clone());
		return dup;
	}
	
	// returns the top level group: all of the assignments and subgroups are considered to be
	// connected to the primary assay description, and the root's category name is a description of this
	// particular assay schema template
	public Group getRoot() {return root;}
	public void setRoot(Group root) {this.root = root;}
	
	// access to assays
	public int numAssays() {return assays.size();}
	public Assay getAssay(int idx) {return assays.get(idx);}
	public void setAssay(int idx, Assay assay) {assays.set(idx, assay);}
	public void addAssay(Assay assay) {assays.add(assay);}
	public void insertAssay(int idx, Assay assay) {assays.add(idx, assay);}
	public void deleteAssay(int idx) {assays.remove(idx);}
	public void swapAssays(int idx1, int idx2) {Assay a = assays.get(idx1); assays.set(idx1, assays.get(idx2)); assays.set(idx2, a);}
	
	// produces a human-readable definition of the schema, mainly for debugging
	public String toString()
	{
		StringBuffer buff = new StringBuffer();
		root.outputAsString(buff, 0);
		// !! and assays...
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
		
		Resource objRoot = model.createResource(PFX_BAT + turnLabelIntoName(root.name));
		model.add(objRoot, rdfType, batRoot);
		model.add(objRoot, rdfType, batGroup);
		model.add(objRoot, rdfLabel, root.name);
		if (root.descr.length() > 0) model.add(objRoot, hasDescription, root.descr);
		
		formulateGroup(model, objRoot, root);

		for (int n = 0; n < assays.size(); n++)
		{
			Assay assay = assays.get(n);
			Resource objAssay = model.createResource(PFX_BAT + turnLabelIntoName(assay.name));
			model.add(objAssay, rdfType, batAssay);
			model.add(objAssay, rdfLabel, assay.name);
			if (assay.descr.length() > 0) model.add(objAssay, hasDescription, assay.descr);
			if (assay.para.length() > 0) model.add(objAssay, hasParagraph, assay.para);
			model.add(objAssay, inOrder, model.createTypedLiteral(n + 1));
			
			for (int i = 0; i < assay.annotations.size(); i++)
			{
				Annotation annot = assay.annotations.get(i);
			
				Resource blank = model.createResource();
				model.add(objAssay, hasAnnotation, blank);

				// looks up the assignment in the overall hierarchy, to obtain the recently-created URI
				Assignment assn = findAssignment(annot);
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
		
		RDFDataMgr.write(ostr, model, RDFFormat.TURTLE);
	}
	
	// returns a string that identifies the object's position in the hierarchy; this can be used to apply between two schema instances with
	// the same layout (e.g. one that has been cloned)
	public String locatorID(Group group)
	{
		if (group == null) return null;
		List<Integer> seq = new ArrayList<>();
		while (group.parent != null)
		{
			seq.add(0, group.parent.subGroups.indexOf(group));
			group = group.parent;
		}
		
		StringBuffer buff = new StringBuffer();
		for (int n = 0; n < seq.size(); n++) buff.append(seq.get(n) + ":");
		return buff.toString();
	}
	public String locatorID(Assignment assn)
	{
		if (assn == null) return null;
		return locatorID(assn.parent) + assn.parent.assignments.indexOf(assn);
	}
	public String locatorID(Assay assay)
	{
		if (assay == null) return null;
		int idx = assays.indexOf(assay);
		if (idx < 0) return null;
		return "*" + idx;
	}
	
	// uses a locatorID to pull out the object from the schema hierarchy; returns null if couldn't find it for any reason; note that using an assignment locator
	// to obtain a group is valid: it will find the parent group
	public Group obtainGroup(String locatorID)
	{
		Group group = root;
		String[] bits = locatorID.split(":");
		int len = bits.length - (locatorID.endsWith(":") ? 0 : 1);
		for (int n = 0; n < len; n++)
		{
			int idx = Integer.parseInt(bits[n]);
			if (idx < 0 || idx >= group.subGroups.size()) return null;
			group = group.subGroups.get(idx);
		}
		return group;
	}
	public Assignment obtainAssignment(String locatorID)
	{
		Group group = obtainGroup(locatorID);
		if (group == null) return null;
		int idx = Integer.parseInt(locatorID.substring(locatorID.lastIndexOf(':') + 1));
		if (idx < 0 || idx >= group.assignments.size()) return null;
		return group.assignments.get(idx);
	}
	
	// locating assays: this is a bit simpler because it's a flat list
	public int indexOfAssay(String locatorID)
	{
		if (!locatorID.startsWith("*")) return -1;
		int idx = Integer.parseInt(locatorID.substring(1));
		return idx >= 0 && idx < assays.size() ? idx : -1;
	}
	public Assay obtainAssay(String locatorID)
	{
		int idx = indexOfAssay(locatorID);
		return idx < 0 ? null : assays.get(idx);
	}
	
	// given an annotation, which carries its own cloned "linear branch" as baggage, matches this sequence to the current group hierarchy
	public Assignment findAssignment(Annotation annot)
	{
		Assignment fakeAssn = annot.assn;
		List<Group> fakeGroups = new ArrayList<>();
		for (Group p = fakeAssn.parent; p != null && p.parent != null; p = p.parent) fakeGroups.add(0, p);

		// drill down the sequence of groups, until "look" is defined to be the matching group that should contain the assignment; in this way any
		// assignment that has an exact named hierarchy match is considered to be a hit
		Group look = root;
		descend: while (fakeGroups.size() > 0)
		{
			Group fake = fakeGroups.remove(0);
			for (Group g : look.subGroups) if (g.name.equals(fake.name))
			{
				look = g;
				continue descend;
			}
			look = null;
			break;
		}
		if (look != null)
		{
			for (Assignment assn : look.assignments)
			{
				if (assn.name.equals(fakeAssn.name) && assn.propURI.equals(fakeAssn.propURI)) return assn;
			}
		}
		
		// (try other approaches? match anything in the hierarchy?)
		// maybe: look for a partial match, looking for immediate parents; tolerant of renames, e.g. "foo -> bar -> thing" "fnord -> bar -> thing"
		
		return null;
	}
	
	// returns true if the annotation is considered to belong to the assignment, i.e. the assignment heading and group hierarchies both match
	public boolean matchAnnotation(Annotation annot, Assignment assn)
	{
		if (!annot.assn.name.equals(assn.name) || !annot.assn.propURI.equals(assn.propURI)) return false;
		
		// make sure the group trail has the same name at each step
		Group p1 = annot.assn.parent, p2 = assn.parent;
		while (true)
		{
			if (p1 == null || p2 == null) return false;
			if (p1.parent == null && p2.parent == null) break; // traced both back to the root: this is fine
			
			if (!p1.name.equals(p2.name)) return false;
			
			p1 = p1.parent;
			p2 = p2.parent;
		}
		
		return true;
	}
	
	// adding of content
	public Group appendGroup(Group parent, Group group)
	{
		group.parent = parent;
		parent.subGroups.add(group);
		return group;
	}
	public Assignment appendAssignment(Group parent, Assignment assn)
	{
		assn.parent = parent;
		parent.assignments.add(assn);
		return assn;
	}
	
	// removes the given items from the list
	public void deleteGroup(Group group)
	{
		group.parent.subGroups.remove(group);
	}
	public void deleteAssignment(Assignment assn)
	{
		assn.parent.assignments.remove(assn);
	}
	public void deleteAssay(Assay assay)
	{
		assays.remove(assay);
	}

	// shuffles the object up or down the parents' hierarchy
	public void moveGroup(Group group, int dir)
	{
		if (group.parent == null) return;
		List<Group> list = group.parent.subGroups;
		int idx = list.indexOf(group);
		if (dir < 0)
		{
			if (idx == 0) return;
			Group g1 = list.get(idx), g2 = list.get(idx - 1);
			list.set(idx, g2);
			list.set(idx - 1, g1);
		}
		else if (dir > 0)
		{
			if (idx >= list.size() - 1) return;
			Group g1 = list.get(idx), g2 = list.get(idx + 1);
			list.set(idx, g2);
			list.set(idx + 1, g1);
		}
	}
	public void moveAssignment(Assignment assn, int dir)
	{
		List<Assignment> list = assn.parent.assignments;
		int idx = list.indexOf(assn);
		if (dir < 0)
		{
			if (idx == 0) return;
			Assignment a1 = list.get(idx), a2 = list.get(idx - 1);
			list.set(idx, a2);
			list.set(idx - 1, a1);
		}
		else if (dir > 0)
		{
			if (idx >= list.size() - 1) return;
			Assignment a1 = list.get(idx), a2 = list.get(idx + 1);
			list.set(idx, a2);
			list.set(idx + 1, a1);
		}
	}
	
	// returns a set containing every URI mentioned in the template
	public Set<String> gatherAllURI()
	{
		Set<String> list = new HashSet<String>();
		List<Group> stack = new ArrayList<>();
		stack.add(root);
		while (stack.size() > 0)
		{
			Group group = stack.remove(0);
			for (Assignment assn : group.assignments)
			{
				if (assn.propURI.length() > 0) list.add(assn.propURI);
				for (Value val : assn.values) if (val.uri.length() > 0) list.add(val.uri);
			}
			stack.addAll(group.subGroups);
		}
		return list;
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
		batAssay = model.createResource(PFX_BAT + BAT_ASSAY);
		batGroup = model.createResource(PFX_BAT + BAT_GROUP);
		batAssignment = model.createResource(PFX_BAT + BAT_ASSIGNMENT);
		hasGroup = model.createProperty(PFX_BAT + HAS_GROUP);
		hasAssignment = model.createProperty(PFX_BAT + HAS_ASSIGNMENT);
		hasDescription = model.createProperty(PFX_BAT + HAS_DESCRIPTION);
		inOrder = model.createProperty(PFX_BAT + IN_ORDER);
		hasProperty = model.createProperty(PFX_BAT + HAS_PROPERTY);
		hasValue = model.createProperty(PFX_BAT + HAS_VALUE);
		mapsTo = model.createProperty(PFX_BAT + MAPS_TO);
		hasParagraph = model.createProperty(PFX_BAT + HAS_PARAGRAPH);
		hasAnnotation = model.createProperty(PFX_BAT + HAS_ANNOTATION);
		isAssignment = model.createProperty(PFX_BAT + IS_ASSIGNMENT);
		hasLiteral = model.createProperty(PFX_BAT + HAS_LITERAL);
		
		nameCounts = new HashMap<>();
		assignmentToResource = new HashMap<>();
	}

	private void formulateGroup(Model model, Resource objParent, Group group)
	{
		int order = 0;
		
 		for (Assignment assn : group.assignments)
		{
			String name = turnLabelIntoName(assn.name);
						
			Resource objAssn = model.createResource(PFX_BAT + name);
			model.add(objParent, hasAssignment, objAssn);
			model.add(objAssn, rdfType, batAssignment);
			model.add(objAssn, rdfLabel, assn.name);
			if (assn.descr.length() > 0) model.add(objAssn, hasDescription, assn.descr);
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
			
			assignmentToResource.put(assn,  objAssn); // for subsequent retrieval
		}
		
		// recursively emit any subcategories
		String parentName = turnLabelIntoName(group.name);
		for (Group subgrp : group.subGroups)
		{
    		Resource objGroup = model.createResource(PFX_BAT + turnLabelIntoName(subgrp.name));
    		model.add(objParent, hasGroup, objGroup);
    		model.add(objGroup, rdfType, batGroup);
    		model.add(objGroup, rdfLabel, subgrp.name);
    		if (subgrp.descr.length() > 0) model.add(objGroup, hasDescription, subgrp.descr);
			model.add(objGroup, inOrder, model.createTypedLiteral(++order));
    		
    		formulateGroup(model, objGroup, subgrp);
		}
	}

	// pull in an RDF-compatible file, and pull out the model information
	private void parseFromStream(InputStream istr) throws IOException
	{
		Model model = ModelFactory.createDefaultModel();

		try {RDFDataMgr.read(model, istr, Lang.TTL);}
		//catch (IOException ex) {throw ex;}
		catch (Exception ex) {throw new IOException("Failed to parse schema", ex);}
	
		setupResources(model);	

		// extract the template
		Resource objRoot = null;
		for (StmtIterator it = model.listStatements(null, rdfType, batRoot); it.hasNext();)
		{
			objRoot = it.next().getSubject();
			break;
		}
		if (objRoot == null) throw new IOException("No template root found: this is probably not a bioassay template file.");

		root = new Group(null, findString(model, objRoot, rdfLabel));
		root.descr = findString(model, objRoot, hasDescription);
		
		resourceToAssignment = new HashMap<>();
		
		parseGroup(model, objRoot, root);

		// extract each of the assays
		Map<Object, Integer> order = new HashMap<>();
		for (StmtIterator it = model.listStatements(null, rdfType, batAssay); it.hasNext();)
		{
			Resource objAssay = it.next().getSubject();
			
			Assay assay = parseAssay(model, objAssay);
			assays.add(assay);
			
			order.put(assay, findInteger(model, objAssay, inOrder));
		}
		assays.sort((a1, a2) -> order.get(a1).compareTo(order.get(a2)));
	}
	
	// for a given category node, pulls out and parses all of its assignments and subcategories
	private void parseGroup(Model model, Resource objParent, Group group) throws IOException
	{
		final Map<Object, Integer> order = new HashMap<>();
	
		// look for assignments
		for (StmtIterator it = model.listStatements(objParent, hasAssignment, (RDFNode)null); it.hasNext();)
		{
			Statement st = it.next();
			Resource objAssn = (Resource)st.getObject();
			
			//Util.writeln("Cat:"+category.categoryName+ " prop:"+clsProp.toString());
			
			Assignment assn = parseAssignment(model, group, objAssn);
			group.assignments.add(assn);
			order.put(assn, findInteger(model, objAssn, inOrder));
			
			resourceToAssignment.put(objAssn, assn);
		}
		group.assignments.sort((a1, a2) -> order.get(a1).compareTo(order.get(a2)));
		
		// look for subcategories
		order.clear();
		for (StmtIterator it = model.listStatements(objParent, hasGroup, (RDFNode)null); it.hasNext();)
		{
			Statement st = it.next();
			Resource objGroup = (Resource)st.getObject();
			
    		Group subgrp = new Group(group, findString(model, objGroup, rdfLabel));
    		subgrp.descr = findString(model, objGroup, hasDescription);
    		
    		group.subGroups.add(subgrp);
    		order.put(subgrp, findInteger(model, objGroup, inOrder));
    		
    		parseGroup(model, objGroup, subgrp);
		}
		group.subGroups.sort((a1, a2) -> order.get(a1).compareTo(order.get(a2)));
	}
	
	private Assignment parseAssignment(Model model, Group group, Resource objAssn) throws IOException
	{
		Assignment assn = new Assignment(group, findString(model, objAssn, rdfLabel), findAsString(model, objAssn, hasProperty));
		assn.descr = findString(model, objAssn, hasDescription);
		
		for (StmtIterator it = model.listStatements(objAssn, hasValue, (RDFNode)null); it.hasNext();)
		{
			Resource blank = (Resource)it.next().getObject();
					
			Value val = new Value(findAsString(model, blank, mapsTo), findString(model, blank, rdfLabel));
			val.descr = findString(model, blank, hasDescription);
			assn.values.add(val);
		}

		return assn;
	}
	
	private Assay parseAssay(Model model, Resource objAssay)
	{
		Assay assay = new Assay(findString(model, objAssay, rdfLabel));
		
		assay.descr = findString(model, objAssay, hasDescription);
		assay.para = findString(model, objAssay, hasParagraph);
		
		for (StmtIterator it = model.listStatements(objAssay, hasAnnotation, (RDFNode)null); it.hasNext();)
		{
			Resource blank = (Resource)it.next().getObject();

			Resource assnURI = findResource(model, blank, isAssignment);
			String label = findString(model, blank, rdfLabel);
			String descr = findString(model, blank, hasDescription);
			Resource propURI = findResource(model, blank, hasProperty);
			Resource valueURI = findResource(model, blank, hasValue);
			String valueLiteral = findString(model, blank, hasLiteral);

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
