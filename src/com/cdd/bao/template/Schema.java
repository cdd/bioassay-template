/*
	BioAssay Ontology Annotator Tools

	Copyright 2016-2023 Collaborative Drug Discovery, Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package com.cdd.bao.template;

import com.cdd.bao.*;
import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.commons.lang3.*;
import org.json.*;

/*
	Schema: functionality for encapsulating a "BioAssay Template" document and some number of accompanying annotated
	assays. The datastructure matches a portion of the semantic triples that represent the same thing, but once it
	is deserialised, it is independent of the underlying triples. It works closely with ModelSchema.
*/

public class Schema
{
	// a "group" is a collection of assignments and subgroups; a BioAssayTemplate is basically a single root group, and its descendent
	// contents make up the definition
	public static final class Group
	{
		public Group parent;
		public String name, descr = "";
		public String groupURI = ""; // formal identity for the group
		public boolean canDuplicate = false; // if true, permit duplication of the group when used for annotation
		public List<Assignment> assignments = new ArrayList<>();
		public List<Group> subGroups = new ArrayList<>();
		
		public Group(Group parent, String name) 
		{
			this.parent = parent;
			this.name = name == null ? "" : name;
		}
		public Group(Group parent, String name, String groupURI) 
		{
			this.parent = parent;
			this.name = name == null ? "" : name;
			this.groupURI = groupURI == null ? "" : groupURI;
		}
		
		@Override
		public Group clone() {return clone(parent);}
		public Group clone(Group parent)
		{
			Group dup = new Group(parent, name, groupURI);
			dup.descr = descr;
			dup.groupURI = groupURI;
			dup.canDuplicate = canDuplicate;
			for (Assignment assn : assignments) dup.assignments.add(assn.clone(dup));
			for (Group grp : subGroups) dup.subGroups.add(grp.clone(dup));
			return dup;
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (o == null || getClass() != o.getClass()) return false;
			Group other = (Group)o;
			return Util.equals(name, other.name) && Util.equals(descr, other.descr) && Util.equals(groupURI, other.groupURI) &&
				   canDuplicate == other.canDuplicate && assignments.equals(other.assignments) && subGroups.equals(other.subGroups);
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hash(name, descr, groupURI, assignments, subGroups);
		}

		// returns a list of group URIs leading up to (but not including) this one, which can be used to disambiguate beyond just the propURI
		public String[] groupNest()
		{
			if (parent == null) return new String[0];
			List<String> nest = new ArrayList<>();
			for (Schema.Group look = parent; look.parent != null; look = look.parent) nest.add(look.groupURI == null ? "" : look.groupURI);
			while (nest.size() > 0 && nest.get(nest.size() - 1).equals("")) nest.remove(nest.size() - 1);
			return nest.toArray(new String[nest.size()]);
		}
		
		// as above, except compiles the labels rather than URIs
		public String[] groupLabel()
		{
			if (parent == null) return new String[0];
			List<String> nest = new ArrayList<>();
			for (Schema.Group look = parent; look.parent != null; look = look.parent) nest.add(look.name == null ? "" : look.name);
			return nest.toArray(new String[nest.size()]);
		}
		
		// render
		private void outputAsString(StringBuilder buff, int indent)
		{
			for (int n = 0; n < indent; n++) buff.append("  ");
			buff.append("[" + name + "] <" + groupURI + "> (" + descr + ")\n");
			for (Assignment assn : assignments) assn.outputAsString(buff, indent + 1);
			for (Group grp : subGroups) grp.outputAsString(buff, indent + 1);
		}

		// return a flattened list of subgroups (not including this one), arranged in tree-order
		public Group[] flattenedGroups()
		{
			List<Group> list = new ArrayList<>();
			List<Group> stack = new ArrayList<>();
			stack.addAll(subGroups);
			while (stack.size() > 0)
			{
				Group g = stack.remove(0);
				list.add(g);
				stack.addAll(g.subGroups);
			}
			return list.toArray(new Group[list.size()]);
		}
		
		// makes a list of all the assignments that occur within this group and its subgroups, in order of occurrence
		public Assignment[] flattenedAssignments()
		{
			List<Assignment> list = new ArrayList<>();
			List<Group> stack = new ArrayList<>();
			stack.add(this);
			while (stack.size() > 0)
			{
				Group g = stack.remove(0);
				list.addAll(g.assignments);
				stack.addAll(g.subGroups);
			}
			return list.toArray(new Assignment[list.size()]);
		}
	}

	// used within assignments: used to indicate how building of models to make suggestions is handled
	public enum Suggestions
	{
		FULL, // (default) use all of the available methods for guestimating appropriate term suggestions for URIs
		DISABLED, // do not use the underlying terms as either inputs or outputs for suggestion models
		FIELD, // the assignment should be mapped to an auxiliary compound field rather than a URI
		URL, // preferred value type is a URL that directs to an external resource
		ID, // preferred value an identifier that refers to another assay
		STRING, // preferred value type is a string literal, of arbitrary format
		NUMBER, // preferred value type is a numeric iteral of arbitrary precision
		INTEGER, // preferred value type is a literal that evaluates to an integer
		DATE, // preferred value type is a date
	}

	// an "assignment" is an instruction to associate a bioassay (subject) with a value (object) via a property (predicate); the datastructure
	// has a unique property URI, and a list of applicable values
	public static final class Assignment
	{
		public Group parent;
		public String name, descr = "";
		public String propURI;
		public List<Value> values = new ArrayList<>();
		public Suggestions suggestions = Suggestions.FULL;
		public boolean mandatory = false;
		
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
			dup.suggestions = suggestions;
			dup.mandatory = mandatory;
			return dup;
		}
		
		// determines equality based on the immediate properties of the assignment and its values; does not compare its position in the branch hierarchy
		@Override
		public boolean equals(Object o)
		{
			if (o == null || getClass() != o.getClass()) return false;
			Assignment other = (Assignment)o;
			return Util.equals(name, other.name) && Util.equals(descr, other.descr) && Util.equals(propURI, other.propURI) &&
				   suggestions == other.suggestions && mandatory == other.mandatory && values.equals(other.values);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(name, descr, propURI, suggestions, mandatory, values);
		}
		
		// returns true if the other assignment has the same branch sequence, i.e. the name is the same, and likewise for the trail of parent groups
		public boolean sameBranch(Assignment other)
		{
			if (!name.equals(other.name)) return false;
			for (Group g1 = parent, g2 = other.parent; g1 != null || g2 != null; g1 = g1.parent, g2 = g2.parent)
			{
				if (g1 == null || g2 == null) return false;
				if (!g1.name.equals(g2.name)) return false;
			}
			return true;
		}

		// returns a list of group URIs leading up to this one, which can be used to disambiguate beyond just the propURI
		public String[] groupNest()
		{
			List<String> nest = new ArrayList<>();
			for (Schema.Group look = parent; look.parent != null; look = look.parent) nest.add(look.groupURI == null ? "" : look.groupURI);
			while (nest.size() > 1 && nest.get(nest.size() - 1).equals("")) nest.remove(nest.size() - 1);
			return nest.toArray(new String[nest.size()]);
		}
		
		// as above, except compiles the labels rather than URIs
		public String[] groupLabel()
		{
			List<String> nest = new ArrayList<>();
			for (Schema.Group look = parent; look.parent != null; look = look.parent) nest.add(look.name == null ? "" : look.name);
			return nest.toArray(new String[nest.size()]);
		}
		
		// convenient shortcuts for turning into a key
		public String keyPropGroup()
		{
			return Schema.keyPropGroup(propURI, groupNest());
		}
		public String keyPropGroupValue(String value)
		{
			value = value.replaceAll(SEP, "\\:\\:"); // this is commonly a URI, but can also be plain text in the case of labels
			return Schema.keyPropGroupValue(propURI, groupNest(), value);
		}
		
		private void outputAsString(StringBuilder buff, int indent)
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

	// used within values: indicates the scope of what's being specified by the URL (if given); note that the default state is "opt-in", so excluding is only
	// necessary when a large branch has been included, but some parts are not wanted
	public enum Specify
	{
		ITEM, // the term specified by the URL is explicitly whitelisted
		EXCLUDE, // explicitly blacklist the term (i.e. exclude it from a branch within which it was previously included)
		WHOLEBRANCH, // include the term specified and everything descended from it
		EXCLUDEBRANCH, // exclude a whole branch that had previously been included
		CONTAINER, // same as whole branch, except the term itself should not be explicitly selected
	}

	// a "value" consists of a URI (in the case of references to a known resource), and descriptive text; an assignment typically has many of these
	public static final class Value
	{
		public String uri; // mapping to a URI in the BAO or related ontology; if blank, is literal
		public String name; // short label for the value; if no URI, this is the literal to use
		public String descr = ""; // longer description
		public String[] altLabels = null; // optional alternative labels (aka synonyms)
		public String[] externalURLs = null; // optional resource information
		public Specify spec = Specify.ITEM;
		public String parentURI = null; // if specified, indicates where the term goes in the hierarchy
		
		public Value(String uri, String name)
		{
			this.uri = uri == null ? "" : uri;
			this.name = name == null ? "" : name;
		}
		public Value(String uri, String name, Specify spec)
		{
			this(uri, name);
			this.spec = spec;
		}
		public Value clone()
		{
			Value dup = new Value(uri, name);
			dup.descr = descr;
			dup.altLabels = ArrayUtils.clone(altLabels);
			dup.externalURLs = ArrayUtils.clone(externalURLs);
			dup.spec = spec;
			dup.parentURI = parentURI;
			return dup;
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (o == null || getClass() != o.getClass()) return false;
			Value other = (Value)o;
			return Util.equals(uri, other.uri) && Util.equals(name, other.name) && Util.equals(descr, other.descr) && spec == other.spec;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(uri, name, descr, spec);
		}
	}

	// an "assay" is an actual instance of the bioassay template, i.e. filling in the assignments with values
	public static final class Assay
	{
		public String name; // short label for the bioassay
		public String descr = ""; // more descriptive label: used to complement the semantic assignments
		public String para = ""; // plain text description of the assay, if available; sometimes this is available prior semantic assignments
		public String originURI = ""; // optional: use to indicate the semantic resource that the assay originated from		
		public List<Annotation> annotations = new ArrayList<>();
		
		public Assay(String name)
		{
			this.name = name == null ? "" : name;
		}
		@Override
		public Assay clone()
		{
			Assay dup = new Assay(name);
			dup.descr = descr;
			dup.para = para;
			dup.originURI = originURI;
			for (Annotation a : annotations) dup.annotations.add(a.clone());
			return dup;
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (o == null || getClass() != o.getClass()) return false;
			Assay other = (Assay)o;
			if (!(name.equals(other.name) && descr.equals(other.descr) && para.equals(other.para) && originURI.equals(other.originURI))) return false;
			if (annotations.size() != other.annotations.size()) return false;

			// doesn't work: sort order is random 
			Set<String> akeys = new HashSet<>();
			for (Annotation annot : annotations) akeys.add(annot.keyString());
			for (Annotation annot : other.annotations) if (!akeys.contains(annot.keyString())) return false;
			
			return true;
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hash(name, descr, para, originURI, new HashSet<>(annotations));
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
			this.value = value == null ? null : value.clone();
		}
		public Annotation(Assignment assn, String literal)
		{
			this.assn = linearBranch(assn);
			this.literal = literal;
		}
		@Override
		public Annotation clone()
		{
			return value != null ? new Annotation(assn, value) : new Annotation(assn, literal);
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (o == null || getClass() != o.getClass()) return false;
			
			Annotation other = (Annotation)o;
			// handle the case where one of the assn is null
			if (assn == null || other.assn == null) return assn == other.assn;
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
			
			if (value == null)
			{
				if (other.value == null) return literal.equals(other.literal);
				return false;
			}
			if (other.value == null) return false;
			return value.equals(other.value);
		}
		
		@Override
		public int hashCode()
		{
			if (assn == null) return Objects.hash(null, null, null, value, literal);
			return Objects.hash(assn, assn.parent, assn.name, value, literal);
		}
		
		// returns a string that represents the entire content: can be used for uniqueness/sorting (more or less)
		public String keyString()
		{
			StringBuilder buff = new StringBuilder();
			buff.append(assn.name + "\n");
			for (Group g = assn.parent; g != null; g = g.parent) buff.append(g.name + "\n");
			if (value != null) buff.append(value.uri + "\n" + value.name + "\n" + value.descr + "\n");
			if (literal != null) buff.append(literal + "\n");
			return buff.toString();
		}
		
		// clones the assignment, and re-manufactures the whole branch, except makes it linear: the parent group sequence will therefore check out, in terms of being able to 
		// recreate the tree with object names; this will enable comparison of annotations between two schema instances, with a significant amount of slack, i.e. if the schema is
		// being changed, or is temporarily invalid, then most annotations will still be able to correlate the assignments unambiguously, and those that cannot will still provide
		// a lot of clues for reconciliation; this is all in aid of the expectation that users will do some fairly radical schema refactorings, even after staying to annotate assays
		public static Assignment linearBranch(Assignment assn)
		{
			Group par = assn.parent;
			Group dup = new Group(null, par.name, par.groupURI);
			dup.descr = par.descr;
			dup.assignments.add(assn);

			assn = assn.clone(dup);

			while (par.parent != null)
			{
				dup.parent = new Group(null, par.parent.name, par.parent.groupURI);
				dup.parent.descr = par.parent.descr;
				dup.parent.subGroups.add(dup);
				
				par = par.parent;
				dup = dup.parent;
			}
			
			return assn;
		}
	}
	
	// ------------ private members ------------	
	
	// template root: the schema definition resides within here
	private Group root = new Group(null, "common assay template");
	
	// accompanying assays
	private List<Assay> assays = new ArrayList<>();
	
	// the URI prefix used for all non-hardwired resources: can be used to separate namespaces
	private String schemaPrefix = ModelSchema.PFX_BAS;
	
	// for normal templates, branch groups is null; if defined, it refers to a list of eligible groupURI values that this template
	// may be nested underneath; an empty array means root only; any member of the array that is null/blank also refers to the root
	private String[] branchGroups = null;
	
	// ------------ public methods ------------	

	public Schema()
	{
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == null || getClass() != o.getClass()) return false;
		Schema other = (Schema)o;
		if (!root.equals(other.root)) return false;
		if (assays.size() != other.assays.size()) return false;
		for (int n = 0; n < assays.size(); n++) if (!assays.get(n).equals(other.assays.get(n))) return false;
		return true;
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(root, assays);
	}
	
	// makes a deep copy of the schema content
	@Override
	public Schema clone()
	{
		Schema dup = new Schema();
		dup.schemaPrefix = schemaPrefix;
		dup.branchGroups = branchGroups == null ? null : Arrays.copyOf(branchGroups, branchGroups.length);
		dup.root = root.clone(null);
		for (Assay a : assays) dup.assays.add(a.clone());
		return dup;
	}
	
	// access to the schema prefix, which serves as the namespace
	public String getSchemaPrefix() {return schemaPrefix;}
	public void setSchemaPrefix(String prefix) {schemaPrefix = prefix;}
	
	// access to branch groups (null = regular template, empty array = root only)
	public String[] getBranchGroups() {return branchGroups;}
	public void setBranchGroups(String[] groups) {branchGroups = groups;}
	
	// returns the top level group: all of the assignments and subgroups are considered to be
	// connected to the primary assay description, and the root's category name is a description of this
	// particular assay schema template
	public Group getRoot() {return root;}
	public void setRoot(Group root) {this.root = root;}
	
	// access to assays
	public int numAssays() {return assays.size();}
	public Assay getAssay(int idx) {return assays.get(idx);}
	public void setAssay(int idx, Assay assay) {assays.set(idx, assay);}
	public void appendAssay(Assay assay) {assays.add(assay);}
	public void insertAssay(int idx, Assay assay) {assays.add(idx, assay);}
	public void deleteAssay(int idx) {assays.remove(idx);}
	public void swapAssays(int idx1, int idx2) {Assay a = assays.get(idx1); assays.set(idx1, assays.get(idx2)); assays.set(idx2, a);}
	
	// produces a human-readable definition of the schema, mainly for debugging
	public String toString()
	{
		StringBuilder buff = new StringBuilder();
		root.outputAsString(buff, 0);
		// !! and assays...
		return buff.toString();
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
		
		StringBuilder buff = new StringBuilder();
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
	
	// given information about the [group -> group -> assignment] sequence, using objects that are taken from some other schema, tries to match
	// the hierarchy in this schema and returns the matching object, if any
	public Group findGroup(Group grp)
	{
		if (grp.parent == null) return root;
		
		List<Group> fakeGroups = new ArrayList<>();
		for (Group p = grp; p != null && p.parent != null; p = p.parent) fakeGroups.add(0, p);

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
		
		return look;
	}
	public Assignment findAssignment(Assignment assn)
	{
		Group grp = findGroup(assn.parent);
		if (grp == null) return null;
		for (Assignment look : grp.assignments)
		{
			if (assn.name.equals(look.name) && assn.propURI.equals(look.propURI)) return look;
		}
		return null;
	}
	
	public Assignment findAssignment(Annotation annot)
	{
		Assignment assn = findAssignment(annot.assn);
		if (assn != null) return assn;
		
		// (try other approaches? match anything in the hierarchy?)
		// maybe: look for a partial match, looking for immediate parents; tolerant of renames, e.g. "foo -> bar -> thing" "fnord -> bar -> thing"
		
		return null;
	}
	
	// returns the first group that matches the sequence of nesting URIs, or null if none
	public Group findGroupByNest(String[] groupNest)
	{
		if (Util.length(groupNest) == 0) return root;

		Group grp = root;
		found: for (int n = groupNest.length - 1; n >= 0; n--)
		{
			for (Group look : grp.subGroups) if (groupNest[n].equals(look.groupURI)) {grp = look; continue found;}
			return null;
		}
		return grp;
	}
	
	// returns all of the assignments that match the given property URI, or empty list if none; if the groupNest parameter is given, it will
	// make sure that the nested hierarchy of groupURIs match the parameter (otherwise it will be ignored)
	public Assignment[] findAssignmentByProperty(String propURI) {return findAssignmentByProperty(propURI, null);}
	public Assignment[] findAssignmentByProperty(String propURI, String[] groupNest)
	{
		int gsz = Util.length(groupNest);
		List<Assignment> matches = new ArrayList<>();
		List<Group> stack = new ArrayList<>();
		stack.add(root);
		while (stack.size() > 0)
		{
			Group grp = stack.remove(0);
			skip: for (Assignment assn : grp.assignments) if (assn.propURI.equals(propURI)) 
			{
				Group look = assn.parent;
				for (int n = 0; n < gsz && look != null; n++, look = look.parent)
					if (!groupNest[n].equals(look.groupURI)) continue skip;
				
				matches.add(assn);
			}
			stack.addAll(grp.subGroups);
		}
		return matches.toArray(new Assignment[matches.size()]);
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
	
	// convenience methods for comparing property/groupNest; for "same", the group nesting has to be equivalent (usually the right
	// call for matching within the same template); "compatible" means that only the defined parts of the group nesting are compared,
	// which is often useful for inter-template annotation comparisons; note that all of these methods treat groupNests of null & empty
	// array as the same
	public static boolean sameGroupNest(String[] groupNest1, String[] groupNest2)
	{
		int sz1 = Util.length(groupNest1), sz2 = Util.length(groupNest2);
		if (sz1 != sz2) return false;
		for (int n = 0; n < sz1; n++) if (!compareGroupURI(groupNest1[n], groupNest2[n])) return false;
		return true;
	}
	public static boolean compatibleGroupNest(String[] groupNest1, String[] groupNest2)
	{
		int sz = Math.min(Util.length(groupNest1), Util.length(groupNest2));
		for (int n = 0; n < sz; n++) if (!compareGroupURI(groupNest1[n], groupNest2[n])) return false;
		return true;
	}
	public static boolean samePropGroupNest(String propURI1, String[] groupNest1, String propURI2, String[] groupNest2)
	{
		return propURI1.equals(propURI2) && sameGroupNest(groupNest1, groupNest2);
	}
	public static boolean compatiblePropGroupNest(String propURI1, String[] groupNest1, String propURI2, String[] groupNest2)
	{
		return propURI1.equals(propURI2) && compatibleGroupNest(groupNest1, groupNest2);
	}
	
	// comparison/manipulation of group URIs, with or without suffixes
	private static final Pattern PTN_GROUPINDEXED = Pattern.compile("(.*)@\\d+$");
	public static boolean compareGroupURI(String uri1, String uri2)
	{
		if (uri1.equals(uri2)) return true; // quick out: avoid regex
		boolean m1 = PTN_GROUPINDEXED.matcher(uri1).matches(), m2 = PTN_GROUPINDEXED.matcher(uri2).matches();
		if (m1 && m2) return false; // both have indices and they're different
		if (!m1) uri1 += "@1";
		if (!m2) uri2 += "@1";
		return uri1.equals(uri2);
	}
	public static String removeSuffixGroupURI(String uri)
	{
		if (uri == null) return null;
		Matcher m = PTN_GROUPINDEXED.matcher(uri);
		return m.matches() ? m.group(1) : uri;
	}
	
	// convenience methods for combining parts of assignments/annotations to make string identifiers
	private final static String SEP = "::";
	public static String keyPropGroup(String propURI, String[] groupNest)
	{
		return propURI + SEP + (groupNest == null ? "" : String.join(SEP, groupNest));
	}
	public static String keyPropGroupValue(String propURI, String[] groupNest, String value)
	{
		return propURI + SEP + (groupNest == null ? "" : String.join(SEP, groupNest)) + SEP + value;
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
	public void moveAssay(Assay assay, int dir)
	{
		int idx = assays.indexOf(assay);
		if (dir < 0)
		{
			if (idx == 0) return;
			Assay a1 = assays.get(idx), a2 = assays.get(idx - 1);
			assays.set(idx, a2);
			assays.set(idx - 1, a1);
		}
		else if (dir > 0)
		{
			if (idx >= assays.size() - 1) return;
			Assay a1 = assays.get(idx), a2 = assays.get(idx + 1);
			assays.set(idx, a2);
			assays.set(idx + 1, a1);
		}
	}
	
	// when an assignment is renamed, it would normally put any referring annotations out of sync
	public void syncAnnotations(Assignment oldAssn, Assignment newAssn)
	{
		if (oldAssn.name.equals(newAssn.name)) return;
		
		for (Assay assay : assays) 
		{
			for (int n = 0; n < assay.annotations.size(); n++)
			{
				Annotation annot = assay.annotations.get(n);
				if (annot.assn.sameBranch(oldAssn)) 
				{
					annot = annot.clone();
					annot.assn = newAssn;
					assay.annotations.set(n, annot);
				}
			}
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
	
	// ------------ serialisation ------------
	
	// the default serialisation format is JSON, which is highly convenient; there are other options though (such as RDF triples), provided
	// by other classes
	
	public static Schema deserialise(File file) throws IOException
	{
		try (Reader rdr = new FileReader(file))
		{
			Schema schema = deserialise(rdr);
			return schema;
		}
		catch (IOException ex) {throw new IOException("Loading schema file failed [" + file.getAbsolutePath() + "].", ex);}
	}
	public static Schema deserialise(InputStream istr) throws IOException
	{
		return deserialise(new InputStreamReader(istr));
	}
	public static Schema deserialise(Reader rdr) throws IOException
	{
		try
		{
			JSONObject json = new JSONObject(new JSONTokener(rdr));
			var schema = deserialise(json);
			return schema;
		}
		catch (JSONException ex)
		{
			// all errors percolate up to here, from invalid JSON to missing parts of the JSON hierarchy; one size fits all
			// error message will suffice
			throw new IOException("Not a valid JSON-formatted schema.");
		}
	}
	public static Schema deserialise(JSONObject json)
	{
		Schema schema = new Schema();

		schema.setSchemaPrefix(json.getString("schemaPrefix"));
		
		JSONArray branchGroups = json.optJSONArray("branchGroups");
		if (branchGroups != null) schema.setBranchGroups(branchGroups.toStringArray());
		
		JSONObject jsonRoot = json.getJSONObject("root");
		schema.setRoot(ClipboardSchema.unpackGroup(jsonRoot));
		return schema;
	}
	
	// serialisation: writes the schema using RDF "turtle" format, using OWL classes
	public void serialise(File file) throws IOException
	{
		try (Writer wtr = new BufferedWriter(new FileWriter(file)))
		{
			serialise(wtr);
		}
	}
	public void serialise(OutputStream ostr) throws IOException
	{
		serialise(new OutputStreamWriter(ostr));
	}
	public void serialise(Writer wtr) throws IOException
	{
		var json = serialiseJSON();
		wtr.write(json.toString(4));
		wtr.flush();
	}
	
	// returns the JSONObject with serialised content
	public JSONObject serialiseJSON()
	{
		var json = new JSONObject();
		json.put("schemaPrefix", schemaPrefix);
		if (branchGroups != null) json.put("branchGroups", branchGroups);
		json.put("root", ClipboardSchema.composeGroup(root));
		return json;
	}	
	
	// ------------ private methods ------------	
}
