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

import java.io.*;
import java.util.*;

/*
	Compare two SchemaVocab trees and house the differences in a tree.
*/

public class CompareSchemaVocab
{
	// schema vocabs to be compared
	private SchemaVocab oldsv, newsv;
	
	public enum DiffType
	{
		ADDITION,
		DELETION,
		NONE
	}	

	public static final class DiffInfo
	{
		public Schema.Assignment assn;
		public DiffType direction;
		public String valueURI, valueLabel;
	}

	// tree of depth 2 will house diffs in this data structure
	public static final class TreeNode<T>
	{
		public T data = null;
		public List<TreeNode<T>> children = new ArrayList<>();
		public TreeNode<T> parent = null;
	}

	private TreeNode<DiffInfo> treeRoot = new TreeNode<>();

	public CompareSchemaVocab(SchemaVocab oldsv, SchemaVocab newsv)
	{
		this.oldsv = oldsv;
		this.newsv = newsv;
	}

	public TreeNode<DiffInfo> getDiffTree()
	{
		setupTree();
		return this.treeRoot;
	}

	private void setupTree()
	{
		// map key below: groupNest::propURI, where groupNest is a concatenation of all groups,
		// from outer-most group, to inner-most, and the join-pattern is "::".

		Map<String, SchemaVocab.StoredTree> keyToOldTree = new HashMap<>();
		for (SchemaVocab.StoredTree tree : oldsv.getTrees()) keyToOldTree.put(keyFromTree(tree), tree);

		// iterate through current trees, finding deletions and additions
		for (SchemaVocab.StoredTree curTree : newsv.getTrees())
		{
			String curKey = keyFromTree(curTree);
			SchemaVocab.StoredTree oldTree = keyToOldTree.get(curKey);
			
			DiffType diffType = (oldTree == null) ? DiffType.ADDITION : DiffType.NONE;
			TreeNode<DiffInfo> diffParent = attachToDiffTree(curTree, diffType);

			if (oldTree == null)
			{
				// only additions: new tree
				Set<String> additions = new HashSet<>();
				for (SchemaTree.Node node : curTree.tree.getFlat()) additions.add(node.uri);
				pinToDiffParent(additions, DiffType.ADDITION, diffParent, newsv);
			}
			else
			{
				// tree revisions only: additions, deletions, or both
				Set<String> terms1 = new HashSet<>(), terms2 = new HashSet<>();
				for (SchemaTree.Node node : oldTree.tree.getFlat()) terms1.add(node.uri);
				for (SchemaTree.Node node : curTree.tree.getFlat()) terms2.add(node.uri);

				Set<String> deletions = new TreeSet<>(), additions = new TreeSet<>();
				for (String uri : terms1) if (!terms2.contains(uri)) deletions.add(uri);
				for (String uri : terms2) if (!terms1.contains(uri)) additions.add(uri);

				pinToDiffParent(deletions, DiffType.DELETION, diffParent, oldsv);
				pinToDiffParent(additions, DiffType.ADDITION, diffParent, newsv);
			}
		}

		Map<String, SchemaVocab.StoredTree> keyToNewTree = new HashMap<>();
		for (SchemaVocab.StoredTree tree : newsv.getTrees()) keyToNewTree.put(keyFromTree(tree), tree);

		// detect tree deletions
		for (SchemaVocab.StoredTree oldTree : oldsv.getTrees())
		{
			String oldKey = keyFromTree(oldTree);
			SchemaVocab.StoredTree newTree = keyToNewTree.get(oldKey);
			if (newTree == null)
			{
				// only deletions: old tree
				TreeNode<DiffInfo> diffParent = attachToDiffTree(oldTree, DiffType.DELETION);
				Set<String> deletions = new HashSet<>();
				for (SchemaTree.Node node : oldTree.tree.getFlat()) deletions.add(node.uri);
				pinToDiffParent(deletions, DiffType.DELETION, diffParent, oldsv);
			}
		}
	}

	private String keyFromTree(SchemaVocab.StoredTree tree)
	{
		StringBuilder key = new StringBuilder();
		for (int k = 0; k < tree.groupNest.length; ++k) key.append(tree.groupNest[k]).append("::");
		key.append(tree.propURI);
		return key.toString();
	}

	// return handle to newly created TreeNode now attached to diff tree
	private TreeNode<DiffInfo> attachToDiffTree(SchemaVocab.StoredTree tree, DiffType diffType)
	{
		DiffInfo parent = new DiffInfo();
		TreeNode<DiffInfo> treeParent = new TreeNode<>();
		treeParent.data = parent;
		parent.direction = diffType;
		parent.assn = tree.assignment;
		parent.valueURI = tree.propURI;
		treeRoot.children.add(treeParent);
		return treeParent;
	}

	private void pinToDiffParent(Set<String> revisions, DiffType diffType, TreeNode<DiffInfo> parent, SchemaVocab sv)
	{
		for (String uri : revisions)
		{
			DiffInfo term = new DiffInfo();
			TreeNode<DiffInfo> treeTerm = new TreeNode<>();
			treeTerm.data = term;
			term.direction = diffType;
			term.valueURI = uri;
			term.valueLabel = sv.getLabel(uri);
			parent.children.add(treeTerm);
		}
	}
}
