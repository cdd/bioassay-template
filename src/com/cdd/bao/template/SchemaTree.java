/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2016-2018 Collaborative Drug Discovery Inc.
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

import com.cdd.bao.util.*;
import static com.cdd.bao.template.Schema.*;
import static com.cdd.bao.template.Vocabulary.*;

import java.util.*;

import org.apache.commons.lang3.tuple.Pair;

/*
	SchemaTree: for a given schema assignment, puts together a tree structure that can be used to present the hierarchy nicely to users.
*/

public class SchemaTree
{
	// per instance
	private Assignment assn;

	public static final class Node
	{
		public Branch source = null;
		public String uri = null, label = null, descr = null;
		
		public String[] altLabels = null;
		public String[] externalURLs = null;
		public String pubchemSource = null;
		public boolean pubchemImport = false;
		
		public Node parent = null;
		public List<Node> children = new ArrayList<>();
		public int depth = 0, parentIndex = -1;
		public int childCount = 0, schemaCount = 0;
		public boolean inSchema = false, isExplicit = false;
		
		public Node() {}
		public Node(Branch source, String descr)
		{
			this.source = source;
			if (source != null)
			{
				uri = source.uri;
				label = source.label;
			}
			this.descr = descr;
		}
		
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("uri=" + uri).append("; label=" + label).append("; descr=" + descr);
			return sb.toString();
		}
	}

	private Map<String, Node> tree = new HashMap<>(); // uri-to-node
	private List<Node> flat = new ArrayList<>(); // ordered by tree structure, i.e. root at the beginning, depth & parentIndex are meaningful
	private List<Node> list = new ArrayList<>(); // just nodes in the schema, sorted alphabetically (i.e. no tree structure)

	private static final String SEP = "::";

	// ------------ public methods ------------

	// direct constructor: when instantiating outside of the common cache framework
	public SchemaTree(Assignment assn, Vocabulary vocab)
	{
		this.assn = assn;
		buildTree(vocab);
	}
	
	// data constructor: when a flat layout is already present, the rest can be recreated
	public SchemaTree(Node[] flatsrc, Assignment assn)
	{
		this.assn = assn;
		for (Node node : flatsrc) 
		{
			tree.put(node.uri, node);
			flat.add(node);
			list.add(node);
		}
		list.sort((v1, v2) -> v1.label.compareToIgnoreCase(v2.label));
	}
	
	// returns the assignment with which it is affiliated
	public Schema.Assignment getAssignment() {return assn;}
	
	// grab just one node from the tree
	public Node getNode(String uri) {return tree.get(uri);}
	
	// access to the tree, in mapped form
	public Map<String, Node> getTree() {return tree;}
	
	// access to the tree, which is sorted and indented in a flat array, by hierarchy
	public Node[] getFlat() {return flat.toArray(new Node[flat.size()]);}
	
	// the list version of the tree, containing only items for which inSchema=true: and, sorted alphabetically, rather than in hierarchy order
	public Node[] getList() {return list.toArray(new Node[list.size()]);}
	
	// returns a text representation of the tree (for debugging purposes)
	public String toString()
	{
		StringBuilder buff = new StringBuilder();
		for (Node node : flat)
		{
			for (int n = 0; n < node.depth; n++) buff.append("* ");
			buff.append("<" + ModelSchema.collapsePrefix(node.uri) + "> '" + node.label + "'");
			if (node.inSchema) buff.append(" [schema]");
			buff.append(" (children=" + node.childCount + ", #schema=" + node.schemaCount + ")");
			buff.append("\n");
		}
		return buff.toString();
	}
	
	// takes a value term, which is hopefully within the tree, and returns a list that includes itself and all of its
	// ancestor nodes; only includes terms that are actually in the schema; will return an empty list if nothing qualifies
	public String[] expandAncestors(String valueURI)
	{
		Node node = tree.get(valueURI);
		if (node == null) return new String[0];
		List<String> ancestors = new ArrayList<>();
		while (node != null)
		{
			if (node.inSchema) ancestors.add(node.uri);
			node = node.parent;
		}
		return ancestors.toArray(new String[ancestors.size()]);
	}

	// nodes is a list of (parentURI, Node) pairs, where each node is a candidate for this SchemaTree, and should preset 
	// label, description, and uri fields return true if SchemaTree was changed, false otherwise
	public List<Node> addNodes(List<Pair<String, Node>> nodes)
	{
		List<Node> wereAdded = new ArrayList<>();
		List<Pair<String, Node>> candidates = new ArrayList<>(nodes); 

		while (!candidates.isEmpty())
		{
			boolean modified = false;
			for (Iterator<Pair<String, Node>> it = candidates.iterator(); it.hasNext();)
			{
				Pair<String, Node> pair = it.next();
				Node parent = tree.get(pair.getLeft());
				if (parent == null) continue;
				
				Node srcNode = pair.getRight();
				Node node = new Node(srcNode.source, srcNode.descr);
				node.uri = srcNode.uri;
				node.label = srcNode.label;
				node.altLabels = srcNode.altLabels;
				node.externalURLs = srcNode.externalURLs;
				node.pubchemSource = srcNode.pubchemSource;
				node.pubchemImport = srcNode.pubchemImport;
				node.parent = parent;
				node.depth = parent.depth + 1;
				parent.children.add(node);
				parent.childCount++;
				node.inSchema = srcNode.inSchema;
				node.isExplicit = srcNode.inSchema;
				wereAdded.add(node);

				tree.put(node.uri, node);
				list.add(node);
				
				it.remove();
				modified = true;
			}
			if (!modified) break;
		}
		
		if (wereAdded.size() > 0)
		{
			// both flat & list need to be updated
			flattenTree();
			list.sort((v1, v2) -> v1.label.compareToIgnoreCase(v2.label));
		}
		return wereAdded;
	}

	// tack on value node to this schema tree, leaving the underlying assignment untouched
	// return newly created node upon success, or null otherwise
	public Node addNode(String parentURI, String label, String descr, String uri)
	{
		Node node = new Node();
		node.label = label;
		node.descr = descr;
		node.uri = uri;
		
		Pair<String, Node> pair = Pair.of(parentURI, node);
		List<Pair<String, Node>> candidates = new ArrayList<>();
		candidates.add(pair);

		List<Node> added = addNodes(candidates);
		return added.size() > 0 ? node : null;
	}

	// ------------ private methods ------------

	// do the hard work of constructing the tree, and pruning as necessary for presentation purposes
	private void buildTree(Vocabulary vocab)
	{
		Hierarchy hier = vocab.getValueHierarchy();
		
		// a list of overrides for the Branch.parents list: during the construction of the tree it is essential to pick a single
		// parent for each node; this comes out naturally when building in the root-to-branch direction
		Map<String, String> oneParent = new HashMap<>();
	
		Set<String> includeURI = new HashSet<>(); // URIs that have been explicitly requested by the schema
		Set<String> includeBranch = new HashSet<>(); // URIs that have been implicitly requested by virtue of branch
		Set<String> excludeURI = new HashSet<>(); // URIs that have been explicitly excluded
		Set<String> excludeBranch = new HashSet<>(); // URIs that have been excluded as part of a branch
		Set<String> everything = new HashSet<>(); // the running tally of "everything" that has been included but not excluded

		// collect the simple excludes/includes first
		for (Value value : assn.values)
		{
			Branch branch = hier.uriToBranch.get(value.uri);
			if (branch == null) continue;
			
			if (value.spec == Specify.ITEM || value.spec == Specify.WHOLEBRANCH || value.spec == Specify.CONTAINER) 
			{
				includeURI.add(value.uri);
				updateOneParent(oneParent, branch);
			}
			else if (value.spec == Specify.EXCLUDE || value.spec == Specify.EXCLUDEBRANCH) 
			{
				excludeURI.add(value.uri);
			}
		}
		
		// go through the schema definition, and collect the extended collections of things to have or not have
		for (Value value : assn.values)
		{
			Branch branch = hier.uriToBranch.get(value.uri);
			if (branch == null) continue;

			if (value.spec == Specify.ITEM || value.spec == Specify.WHOLEBRANCH || value.spec == Specify.CONTAINER) 
			{
				includeBranch.add(value.uri);
				if (value.spec == Specify.WHOLEBRANCH || value.spec == Specify.CONTAINER)
					collectBranch(includeBranch, branch, excludeURI);
			}
			else if (value.spec == Specify.EXCLUDE || value.spec == Specify.EXCLUDEBRANCH) 
			{
				excludeBranch.add(value.uri);
				if (value.spec == Specify.EXCLUDEBRANCH) collectBranch(excludeBranch, branch, null);
			}
		}		
		
		// apply the exclusion: explicit exclusion is more powerful than exclusion by branch
		for (String uri : excludeURI) {includeURI.remove(uri); includeBranch.remove(uri);}
		for (String uri : excludeBranch) if (!includeURI.contains(uri)) includeBranch.remove(uri);
		
		// now that we have established what was asked for, minus what was asked not for, define a set of "everything": this is initially the
		// include set, but for individually specified items, there's more to do: want to add any of their siblings and descendents that are not
		// excluded; this is because we want the list to be more inclusive than just the schema
		everything.addAll(includeURI);
		everything.addAll(includeBranch);
		for (String uri : includeURI)
		{
			Branch branch = hier.uriToBranch.get(uri);
			collectBranch(everything, branch, excludeURI);
		}
		
		// now follow the parent lineage, and make sure these are included
		for (Branch branch : hier.uriToBranch.values()) if (includeBranch.contains(branch.uri))
		{
			Branch look = branch;
			while (look != null && look.parents.size() > 0)
			{
				String oneURI = oneParent.get(look.uri);
				if (oneURI == null)
				{
					// no upper trail established, so go with the first one
					oneParent.put(look.uri, look.parents.get(0).uri);
					look = look.parents.get(0);
					everything.add(look.uri);
				}
				else look = hier.uriToBranch.get(oneURI);
			}
		}

		// grab all of the branches from the original hierarchy: start by populating the new tree; then fill in the parents; then fill in the children
		for (Branch br : hier.uriToBranch.values()) if (everything.contains(br.uri)) 
		{
			Node node = new Node(br, vocab.getDescr(br.uri));
			node.altLabels = vocab.getAltLabels(br.uri);
			node.externalURLs = vocab.getExternalURLs(br.uri);
			node.pubchemSource = vocab.getPubChemSource(br.uri);
			node.pubchemImport = vocab.getPubChemImport(br.uri);
			tree.put(br.uri, node);
		}
		for (Node node : tree.values())
		{
			node.inSchema = includeBranch.contains(node.uri);
			node.isExplicit = includeURI.contains(node.uri);

			String oneURI = oneParent.get(node.uri);
			if (oneURI == null)
			{
				// preferably: first thing that's opted into the tree
				for (Branch br : node.source.parents) if (everything.contains(br.uri))
				{
					node.parent = tree.get(br.uri);
					if (node.parent != null) node.parent.children.add(node);
					break;
				}
				
				// second choice: the first item that isn't excluded
				if (node.parent == null) for (Branch br : node.source.parents) if (!excludeBranch.contains(br.uri))
				{
					node.parent = tree.get(br.uri);
					if (node.parent != null) node.parent.children.add(node);
					break;
				}			
			}
			else
			{
				node.parent = tree.get(oneURI);
				node.parent.children.add(node);
			}
		}

		// update the children counts: this involves running up the parent list for each node
		for (Node node : tree.values())
		{
			for (Node parent = node.parent; parent != null; parent = parent.parent)
			{
				parent.childCount++;
				if (node.inSchema) parent.schemaCount++;
			}
		}
		
		// look for root branches with only one child: these can be successively collapsed, to avoid needless indenting of the tree
		while (true)
		{
			boolean anything = false;
			for (Iterator<Node> it = tree.values().iterator(); it.hasNext();)
			{
				Node node = it.next();
				if (node.inSchema || node.parent != null) continue;
				int activeChildren = 0;
				for (Node child : node.children) if (child.schemaCount > 0 || child.inSchema) activeChildren++;
				if (activeChildren <= 1)
				{
					for (Node child : node.children) child.parent = null;
					it.remove();
					anything = true;
				}
			}
			if (!anything) break;
		}
		
		// perform the flattening: express the tree as a sequence of ordered branches
		flattenTree();
		
		// prepare the list version, which the same as flattened, except only in-schema items, and in alphabetical order
		for (Node node : flat) if (node.inSchema) list.add(node);
		list.sort((v1, v2) -> v1.label.compareToIgnoreCase(v2.label));
	}
	
	// takes the given branch and recursive updates all of the children, so that they are set to have "this" as their one and only parent
	private void updateOneParent(Map<String, String> oneParent, Branch branch)
	{
		for (Branch child : branch.children) if (!oneParent.containsKey(child.uri))
		{
			oneParent.put(child.uri, branch.uri);
			updateOneParent(oneParent, child);
		}
	}	
	
	// includes the URI for the given branch, and everything descended from it
	private void collectBranch(Set<String> urilist, Branch branch, Set<String> exclude)
	{
		List<Branch> stack = new ArrayList<>();
		stack.add(branch);
		while (stack.size() > 0)
		{
			Branch br = stack.remove(0);
			if (exclude != null && exclude.contains(br.uri)) continue;
			urilist.add(br.uri);
			stack.addAll(br.children);
		}
	}

	// recreates the "flat" version of the tree, which represents parent nodes by index position
	private void flattenTree()
	{
		flat.clear();

		List<Node> roots = new ArrayList<>();
		for (Node node : tree.values()) if (node.parent == null) roots.add(node);
		flattenBranch(roots, 0, -1);
	}

	// given that the branch nodes have been added into a hashmap, and their parent/child pointers updated, create a linearised version, where each 
	private void flattenBranch(List<Node> branch, int depth, int parentIndex)
	{
		branch.sort((v1, v2) -> v1.label.compareToIgnoreCase(v2.label));
		for (Node node : branch)
		{
			node.depth = depth;
			node.parentIndex = parentIndex;
			flat.add(node);
			flattenBranch(node.children, depth + 1, flat.size() - 1);
		}
	}
}
