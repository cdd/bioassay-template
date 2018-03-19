/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2018 Collaborative Drug Discovery Inc.
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
import com.cdd.bao.template.CompareSchemaVocab.*;
import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;

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
import javafx.event.*;
import javafx.geometry.*;
import javafx.util.*;

/*
	Compare vocab tree: as a prelude to writing the whole tree, shows the before & after differences, in order to quickly
	check to see approximately what's going to happen.
*/

public class CompareVocabTree
{
	private File file;
	private SchemaVocab oldsv, newsv;
	
	private TreeItem<DiffInfo> treeRoot = new TreeItem<>(new DiffInfo());
	private TreeView<DiffInfo> treeView = new TreeView<>(treeRoot);

	private final class HierarchyTreeCell extends TreeCell<DiffInfo>
	{
		public void updateItem(DiffInfo item, boolean empty)
		{
			super.updateItem(item, empty);
			
			if (item == null) return;

			if (item.direction == DiffType.NONE)
			{
				setStyle("-fx-text-fill: black;");
				if (item.assn != null) setText(item.assn.name);
				else setText(item.valueURI);
			}
			else
			{
				String style = (item.direction == DiffType.DELETION) ? "-fx-text-fill: red;"
								: (item.direction == DiffType.ADDITION) ? "-fx-text-fill: green;" : "";
				style += "-fx-font-family: arial;";

				StringBuilder label = new StringBuilder((item.direction == DiffType.DELETION) ? "Removed: " : "Added: ");
				if (item.valueLabel != null) label.append(item.valueLabel + " ");
				label.append("<" + ModelSchema.collapsePrefix(item.valueURI) + ">"); 

				setStyle(style);
				setText(label.toString());
			}
		}
	}

	// ------------ public methods ------------

	public CompareVocabTree(File file, Schema schema)
	{
		this.treeView.setShowRoot(false);
		this.treeView.setCellFactory(p -> new HierarchyTreeCell());

		this.newsv = new SchemaVocab(Vocabulary.globalInstance(), new Schema[]{schema});
		this.file = file;

		try (InputStream istr = new FileInputStream(file))
		{
			SchemaVocab oldsv = SchemaVocab.deserialise(istr, new Schema[]{schema}); // use current schema to read in dump
			CompareSchemaVocab compareSchVocabs = new CompareSchemaVocab(oldsv, newsv);
			TreeNode<DiffInfo> diff = compareSchVocabs.getDiffTree();
			setupDiffTree(diff);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
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

	// assume diff-tree of depth 2 and populate javafx treeview accordingly 
	private void setupDiffTree(TreeNode<DiffInfo> diff)
	{
		for (TreeNode<DiffInfo> assn : diff.children) if (assn.data != null)
		{
			// add assignments
			TreeItem<DiffInfo> treeParent = new TreeItem<>(assn.data);
			treeRoot.getChildren().add(treeParent);

			// add terms
			for (TreeNode<DiffInfo> term : assn.children)
			{
				TreeItem<DiffInfo> treeTerm = new TreeItem<>(term.data);
				treeParent.getChildren().add(treeTerm);
			}
		}
	}

	private void writeExport() throws IOException
	{
		try (OutputStream ostr = new FileOutputStream(file))
		{
			if (newsv != null) newsv.serialise(ostr);
		}
	}
}
