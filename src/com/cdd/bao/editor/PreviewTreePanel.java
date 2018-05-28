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
import com.cdd.bao.util.Lineup;

import java.io.*;
import java.util.*;

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
	Preview: shows what the fully processed tree view will look like for an assignment.
*/

public class PreviewTreePanel
{
	private SchemaTree tree;
	private Schema.Assignment assn;
	private Vocabulary vocab = Vocabulary.globalInstance();
	private Set<String> containerNodes = new HashSet<>();

	private TreeItem<SchemaTree.Node> treeRoot = new TreeItem<>(new SchemaTree.Node());
	private TreeView<SchemaTree.Node> treeView = new TreeView<>(treeRoot);
	
	private final class HierarchyTreeCell extends TreeCell<SchemaTree.Node>
	{
		public void updateItem(SchemaTree.Node node, boolean empty)
		{
			super.updateItem(node, empty);
			
			if (node != null)
			{
				String text = "URI <" + node.uri + ">";
				String descr = vocab.getDescr(node.uri);
				if (descr != null && descr.length() > 0) text += "\n\n" + descr;
				Tooltip tip = new Tooltip(text);
				tip.setWrapText(true);
				tip.setMaxWidth(400);
				Tooltip.install(this, tip);
			}
			 
			if (empty)
			{
				setText(null);
				setGraphic(null);
			}
			else 
			{
				String label = node.label;
				String colour = containerNodes.contains(node.uri) ? "#808080" : "black";
				String style = "-fx-font-family: arial; -fx-text-fill: " + colour + "; -fx-font-weight: normal;";

				setText(label);
				setStyle(style);
				setGraphic(getTreeItem().getGraphic());
			}
		}
	}

	// ------------ public methods ------------

	public PreviewTreePanel(SchemaTree tree, Schema.Assignment assn)
	{
		this.tree = tree;
		this.assn = assn;
		
		for (Schema.Value value : assn.values) if (value.spec == Schema.Specify.CONTAINER) containerNodes.add(value.uri);
		
		treeView.setShowRoot(false);
		treeView.setCellFactory(p -> new HierarchyTreeCell());
		
		SchemaTree.Node[] flat = tree.getFlat();
		List<TreeItem<SchemaTree.Node>> items = new ArrayList<>();
		for (SchemaTree.Node node : flat) items.add(new TreeItem<>(node));
		for (int n = 0; n < flat.length; n++)
		{
			int idx = flat[n].parentIndex;
			TreeItem<SchemaTree.Node> parent = idx < 0 ? treeRoot : items.get(idx);
			TreeItem<SchemaTree.Node> item = items.get(n);
			parent.getChildren().add(item);
			if (flat[n].depth == 0) item.setExpanded(true);
		}
	}
	
	public void show()
	{
		Stage stage = new Stage();
		stage.setTitle("Tree: " + assn.name);

		BorderPane root = new BorderPane();
		root.setCenter(treeView);

		stage.setScene(new Scene(root, 700, 700));
		stage.show();
	}
	
	// ------------ private methods ------------

}
