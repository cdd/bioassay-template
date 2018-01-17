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

package com.cdd.bao.editor;

import com.cdd.bao.*;
import com.cdd.bao.template.*;
import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.geometry.*;
import javafx.util.*;

/*
	Compare vocab tree: as a prelude to writing the whole tree, shows the before & after differences, in order to quickly
	check to see approximately what's going to happen.
*/

public class CompareVocabTree
{
	private File file;
	private Schema schema;
	private SchemaVocab schvoc;
	
	private static final class Item
	{
		Schema.Assignment assn;
		int direction;
		String valueURI, valueLabel;
	}
	
	private TreeItem<Item> treeRoot = new TreeItem<>(new Item());
	private TreeView<Item> treeView = new TreeView<>(treeRoot);
	
	private final class HierarchyTreeCell extends TreeCell<Item>
	{
		public void updateItem(Item item, boolean empty)
		{
			super.updateItem(item, empty);
			
			if (item == null) return;

			if (item.direction == 0)
			{
				if (item.assn != null)
				{
					setStyle("-fx-text-fill: black;");
					setText(item.assn.name);
				}
				else
				{
					setStyle("-fx-text-fill: red; -fx-font-family: arial;");
					setText("Removed: " + item.valueURI);
				}
			}
			else
			{
				String style = item.direction < 0 ? "-fx-text-fill: red;" : item.direction > 0 ? "-fx-text-fill: green;" : "";
				style += "-fx-font-family: arial;";
				String label = item.direction < 0 ? "Removed: " : "Added: ";
				label += item.valueLabel + " <" + ModelSchema.collapsePrefix(item.valueURI) + ">"; 

				setStyle(style);
				setText(label);
			}
		}
	}

	private enum DiffType
	{
		DIFF_ADDITION,
		DIFF_DELETION
	}
	
	// ------------ public methods ------------

	public CompareVocabTree(File file, Schema schema)
	{
		this.file = file;
		this.schema = schema;
		
		schvoc = new SchemaVocab(Vocabulary.globalInstance(), new Schema[]{schema});
	
		treeView.setShowRoot(false);
		treeView.setCellFactory(p -> new HierarchyTreeCell());
		
		try {setupTree();}
		catch (IOException ex) {ex.printStackTrace();}
	}
	
	public void show()
	{
		Stage stage = new Stage();
		stage.setTitle("Differences: " + file.getName());

		HBox buttons = new HBox();
		buttons.setSpacing(5);
		buttons.setPadding(new Insets(2, 2, 2, 2));
		Node padding = new Pane();
		Button btnCancel = new Button("Cancel"), btnExport = new Button("Export");
		btnCancel.setOnAction(event -> stage.close());
		btnExport.setOnAction(event -> 
		{
			try {writeExport(); stage.close();}
			catch (Exception ex) {ex.printStackTrace();}
		});
		buttons.getChildren().addAll(padding, btnCancel, btnExport);
		HBox.setHgrow(padding, Priority.ALWAYS);

		BorderPane root = new BorderPane();
		root.setCenter(treeView);
		root.setBottom(buttons);
		
		stage.setScene(new Scene(root, 700, 700));
		stage.show();
	}
	
	// ------------ private methods ------------
	
	private static final class OldNewPair
	{
		SchemaVocab.StoredTree tree1;	// old
		SchemaVocab.StoredTree tree2;	// new
		
		public OldNewPair(SchemaVocab.StoredTree tree1, SchemaVocab.StoredTree tree2)
		{
			this.tree1 = tree1;
			this.tree2 = tree2;
		}

		public static List<OldNewPair> reordered(List<OldNewPair> comparables)
		{
			List<OldNewPair> reordered = new ArrayList<>();
			for (OldNewPair onp : comparables) if (onp.tree1 == null) reordered.add(onp);
			return reordered;
		}
	}

	private void setupTree() throws IOException
	{
		InputStream istr = new FileInputStream(file);
		SchemaVocab oldsv = SchemaVocab.deserialise(istr, new Schema[]{this.schema}); // use current schema to read in dump  
		istr.close();

		// map key below: groupNest::propURI, where groupNest is a concatenation of all groups,
		// from outer-most group, to inner-most, and the join-pattern is "::".

		Map<String, SchemaVocab.StoredTree> keyToOldTree = new HashMap<>();
		for (SchemaVocab.StoredTree tree : oldsv.getTrees()) keyToOldTree.put(keyFromTree(tree), tree);

		// iterate through current trees, finding deletions and additions
		for (SchemaVocab.StoredTree curTree : schvoc.getTrees())
		{
			String curKey = keyFromTree(curTree);
			TreeItem<Item> diffParent = attachToDiffTree(curTree);

			SchemaVocab.StoredTree oldTree = keyToOldTree.get(curKey);
			if (oldTree == null)
			{
				// only additions: new tree
				Set<String> additions = new HashSet<>();
				for (SchemaTree.Node node : curTree.tree.getFlat()) additions.add(node.uri);
				pinToDiffParent(additions, DiffType.DIFF_ADDITION, diffParent, schvoc);
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

				pinToDiffParent(deletions, DiffType.DIFF_DELETION, diffParent, oldsv);
				pinToDiffParent(additions, DiffType.DIFF_ADDITION, diffParent, schvoc);
			}
		}

		Map<String, SchemaVocab.StoredTree> keyToNewTree = new HashMap<>();
		for (SchemaVocab.StoredTree tree : schvoc.getTrees()) keyToNewTree.put(keyFromTree(tree), tree);

		// detect tree deletions
		for (SchemaVocab.StoredTree oldTree : oldsv.getTrees())
		{
			String oldKey = keyFromTree(oldTree);
			SchemaVocab.StoredTree newTree = keyToNewTree.get(oldKey);
			if (newTree == null)
			{
				// only deletions: old tree
				TreeItem<Item> diffParent = attachToDiffTree(oldTree);
				Set<String> deletions = new HashSet<>();
				for (SchemaTree.Node node : oldTree.tree.getFlat()) deletions.add(node.uri);
				pinToDiffParent(deletions, DiffType.DIFF_DELETION, diffParent, oldsv);
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

	// return handle to newly created TreeItem now attached to diff tree
	private TreeItem<Item> attachToDiffTree(SchemaVocab.StoredTree tree)
	{
		Item parent = new Item();
		TreeItem<Item> treeParent = new TreeItem<>(parent);
		parent.assn = tree.assignment;
		parent.valueURI = tree.propURI;
		treeRoot.getChildren().add(treeParent);
		return treeParent;
	}

	private void pinToDiffParent(Set<String> revisions, DiffType diffType, TreeItem<Item> parent, SchemaVocab sv)
	{
		for (String uri : revisions)
		{
			Item term = new Item();
			TreeItem<Item> treeTerm = new TreeItem<>(term);
			term.direction = (diffType == DiffType.DIFF_DELETION) ? -1 : 1;
			term.valueURI = uri;
			term.valueLabel = sv.getLabel(uri);
			parent.getChildren().add(treeTerm);
		}
	}

	private void writeExport() throws IOException
	{
		OutputStream ostr = new FileOutputStream(file);
		schvoc.serialise(ostr);
		ostr.close();
	}
}
