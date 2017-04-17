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
import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;

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
		public String groupURI = "";
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
		public Group clone() {return clone(parent);}
		public Group clone(Group parent)
		{
			Group dup = new Group(parent, name, groupURI);
			dup.descr = descr;
			for (Assignment assn : assignments) dup.assignments.add(assn.clone(dup));
			for (Group grp : subGroups) dup.subGroups.add(grp.clone(dup));
			return dup;
		}
		public boolean equals(Group other)
		{
			if (!name.equals(other.name) || !descr.equals(other.descr) || !groupURI.equals(other.groupURI)) return false;
			if (assignments.size() != other.assignments.size() || subGroups.size() != other.subGroups.size()) return false;
			for (int n = 0; n < assignments.size(); n++) if (!assignments.get(n).equals(other.assignments.get(n))) return false;
			for (int n = 0; n < subGroups.size(); n++) if (!subGroups.get(n).equals(other.subGroups.get(n))) return false;
			return true;
		}
		
		private void outputAsString(StringBuffer buff, int indent)
		{
			for (int n = 0; n < indent; n++) buff.append("  ");
			buff.append("[" + name + "] <" + groupURI + "> (" + descr + ")\n");
			for (Assignment assn : assignments) assn.outputAsString(buff, indent + 1);
			for (Group grp : subGroups) grp.outputAsString(buff, indent + 1);
		}
	};

	// used within assignments: used to indicate how building of models to make suggestions is handled
	public enum Suggestions
	{
		FULL, // (default) use all of the available methods for guestimating appropriate term suggestions
		DISABLED, // do not use the underlying terms as either inputs or outputs for suggestion models
		FIELD, // the assignment should be mapped to an auxiliary compound field rather than a URI
		STRING, // preferred value type is string literals, of arbitrary format
		NUMBER, // preferred value type is numeric iterals of arbitrary precision
		INTEGER // preferred value type is literals that evaluate to an integer
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
			return dup;
		}
		
		// determines equality based on the immediate properties of the assignment and its values; does not compare its position in the branch hierarchy
		public boolean equals(Assignment other)
		{
			if (!name.equals(other.name) || !descr.equals(other.descr) || !propURI.equals(other.propURI)) return false;
			if (suggestions != other.suggestions) return false;
			if (values.size() != other.values.size()) return false;
			for (int n = 0; n < values.size(); n++) if (!values.get(n).equals(other.values.get(n))) return false;
			return true;
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

	// used within values: indicates the scope of what's being specified by the URL (if given); note that the default state is "opt-in", so excluding is only
	// necessary when a large branch has been included, but some parts are not wanted
	public enum Specify
	{
		ITEM, // the term specified by the URL is explicitly whitelisted
		EXCLUDE, // explicitly blacklist the term (i.e. exclude it from a branch within which it was previously included)
		WHOLEBRANCH, // incline the term specified and everything descended from it
		EXCLUDEBRANCH // exclude a whole branch that had previously been included
	}

	// a "value" consists of a URI (in the case of references to a known resource), and descriptive text; an assignment typically has many of these
	public static final class Value
	{
		public String uri; // mapping to a URI in the BAO or related ontology; if blank, is literal
		public String name; // short label for the value; if no URI, this is the literal to use
		public String descr = ""; // longer description
		public Specify spec = Specify.ITEM;
		
		public Value(String uri, String name)
		{
			this.uri = uri == null ? "" : uri;
			this.name = name == null ? "" : name;
		}
		public Value clone()
		{
			Value dup = new Value(uri, name);
			dup.descr = descr;
			dup.spec = spec;
			return dup;
		}
		public boolean equals(Value other)
		{
			return uri.equals(other.uri) && name.equals(other.name) && descr.equals(other.descr) && spec == other.spec;
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
		public Assay clone()
		{
			Assay dup = new Assay(name);
			dup.descr = descr;
			dup.para = para;
			dup.originURI = originURI;
			for (Annotation a : annotations) dup.annotations.add(a.clone());
			return dup;
		}
		public boolean equals(Assay other)
		{
			if (!name.equals(other.name) || !descr.equals(other.descr) || !para.equals(other.para) || !originURI.equals(other.originURI)) return false;
			if (annotations.size() != other.annotations.size()) return false;

			// doesn't work: sort order is random 
			//for (int n = 0; n < annotations.size(); n++) if (!annotations.get(n).equals(other.annotations.get(n))) return false;
			Set<String> akeys = new HashSet<>();
			for (Annotation annot : annotations) akeys.add(annot.keyString());
			for (Annotation annot : other.annotations) if (!akeys.contains(annot.keyString())) return false;
			
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
			this.value = value == null ? null : value.clone();
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
		
		// returns a string that represents the entire content: can be used for uniqueness/sorting (more or less)
		public String keyString()
		{
			StringBuffer buff = new StringBuffer();
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
				dup = par.parent;
			}
			
			return assn;
		}
	}
	
	// ------------ private methods ------------	
	
	// template root: the schema definition resides within here
	private Group root = new Group(null, "common assay template");
	
	// accompanying assays
	private List<Assay> assays = new ArrayList<>();
	
	// the URI prefix used for all non-hardwired resources: can be used to separate namespaces
	private String schemaPrefix = ModelSchema.PFX_BAS;
	
	// ------------ public methods ------------	

	public Schema()
	{
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
		Schema dup = new Schema();
		dup.root = root.clone(null);
		for (Assay a : assays) dup.assays.add(a.clone());
		return dup;
	}
	
	// access to the schema prefix, which serves as the namespace
	public String getSchemaPrefix() {return schemaPrefix;}
	public void setSchemaPrefix(String prefix) {schemaPrefix = prefix;}
	
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
		StringBuffer buff = new StringBuffer();
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
		/*Assignment fakeAssn = annot.assn;
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
		}*/
		
		Assignment assn = findAssignment(annot.assn);
		if (assn != null) return assn;
		
		// (try other approaches? match anything in the hierarchy?)
		// maybe: look for a partial match, looking for immediate parents; tolerant of renames, e.g. "foo -> bar -> thing" "fnord -> bar -> thing"
		
		return null;
	}
	
	// returns all of the assignments that match the given property URI, or empty list if none
	public Assignment[] findAssignmentByProperty(String propURI)
	{
		List<Assignment> matches = new ArrayList<>();
		List<Group> stack = new ArrayList<>();
		stack.add(root);
		while (stack.size() > 0)
		{
			Group grp = stack.remove(0);
			for (Assignment assn : grp.assignments) if (assn.propURI.equals(propURI)) matches.add(assn);
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
	
	// ------------ private methods ------------	

}
