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
import com.cdd.bao.util.Lineup;

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
				setStyle("-fx-text-fill: black;");
				setText(item.assn.name);
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
		SchemaVocab oldsv = SchemaVocab.deserialise(istr, new Schema[0]); // note: giving no schemata works for this purpose
		istr.close();

		// list of (tree1, tree2) tuples to be compared
		List<OldNewPair> comparables = new ArrayList<>();

		// note: only shows trees on both sides
		for (SchemaVocab.StoredTree tree1 : oldsv.getTrees()) for (SchemaVocab.StoredTree tree2 : schvoc.getTrees())
		{
			if (!tree1.schemaPrefix.equals(tree2.schemaPrefix) || !tree1.locator.equals(tree2.locator)) continue;

			String rootUri1 = (tree1.tree.getFlat().length <= 0) ? null : tree1.tree.getFlat()[0].uri;
			String rootUri2 = (tree2.tree.getFlat().length <= 0) ? null : tree2.tree.getFlat()[0].uri;
			if (!StringUtils.equals(rootUri1, rootUri2))
			{
				// reference tree (from disk file) is null here and will be identified in following loop
				comparables.add(new OldNewPair(null, tree2));
				continue;
			}
			comparables.add(new OldNewPair(tree1, tree2));
		}

		List<OldNewPair> reordered = OldNewPair.reordered(comparables);
		for (SchemaVocab.StoredTree tree1 : oldsv.getTrees()) for (OldNewPair onp : reordered)
		{
			SchemaVocab.StoredTree tree2 = onp.tree2;
			if (!tree1.schemaPrefix.equals(tree2.schemaPrefix) || tree1.locator.equals(tree2.locator)) continue;

			String rootUri1 = (tree1.tree.getFlat().length <= 0) ? null : tree1.tree.getFlat()[0].uri;
			String rootUri2 = (tree2.tree.getFlat().length <= 0) ? null : tree2.tree.getFlat()[0].uri;
			if (!StringUtils.equals(rootUri1, rootUri2)) continue;

			onp.tree1 = tree1;
		}

		populateDiffTree(oldsv, comparables);
	}

	private void populateDiffTree(SchemaVocab oldsv, List<OldNewPair> comparables)
	{
		for (OldNewPair onp : comparables)
		{
			SchemaVocab.StoredTree tree1 = onp.tree1;
			SchemaVocab.StoredTree tree2 = onp.tree2;

			Set<String> terms1 = new HashSet<>(), terms2 = new HashSet<>();
			for (SchemaTree.Node node : tree1.tree.getFlat()) terms1.add(node.uri);
			for (SchemaTree.Node node : tree2.tree.getFlat()) terms2.add(node.uri);

			Set<String> extra1 = new TreeSet<>(), extra2 = new TreeSet<>();
			for (String uri : terms1) if (!terms2.contains(uri)) extra1.add(uri);	// deletions
			for (String uri : terms2) if (!terms1.contains(uri)) extra2.add(uri);	// additions
			
			Item parent = new Item();
			TreeItem<Item> treeParent = new TreeItem<>(parent);
			parent.assn = tree2.assignment;
			treeRoot.getChildren().add(treeParent);

			for (String uri : extra1)
			{
				Item term = new Item();
				TreeItem<Item> treeTerm = new TreeItem<>(term);
				term.direction = -1;
				term.valueURI = uri;
				term.valueLabel = oldsv.getLabel(uri);
				treeParent.getChildren().add(treeTerm);
			}			

			for (String uri : extra2)
			{
				Item term = new Item();
				TreeItem<Item> treeTerm = new TreeItem<>(term);
				term.direction = 1;
				term.valueURI = uri;
				term.valueLabel = schvoc.getLabel(uri);
				treeParent.getChildren().add(treeTerm);
			}
		}
	}
	
	private void writeExport() throws IOException
	{
		OutputStream ostr = new FileOutputStream(file);
		schvoc.serialise(ostr);
		ostr.close();
	}
}
