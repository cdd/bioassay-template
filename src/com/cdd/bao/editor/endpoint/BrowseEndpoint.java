/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2016 Collaborative Drug Discovery Inc.
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

package com.cdd.bao.editor.endpoint;

import com.cdd.bao.*;
import com.cdd.bao.template.*;
import com.cdd.bao.editor.*;
import com.cdd.bao.util.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javafx.event.*;
import javafx.geometry.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.application.*;
import javafx.beans.value.*;
import javafx.util.*;

import org.json.*;

/*
	Browse Endpoint: opens up a connection to a SPARQL endpoint, which is presumed to contain templates and possibly assays. Allows
	them to be viewed and fetched.
*/

public class BrowseEndpoint
{
	// ------------ private data ------------	

	private Schema[] schemaList = new Schema[0];

	private Stage stage;
	private BorderPane root;
	private SplitPane splitter;
	private TreeView<Branch> treeView;
	private TreeItem<Branch> treeRoot;
	private DisplaySchema display;
	
	private MenuBar menuBar;
	//private Menu menuFile, menuEdit, menuValue, menuView;
	
	private boolean currentlyRebuilding = false;
	
	// a "branch" encapsulates a tree item which is a generic heading, or one of the objects used within the schema
	public static final class Branch
	{
		public BrowseEndpoint owner;
		public Schema schema = null;
		public Schema.Assay assay = null;

		public Branch(BrowseEndpoint owner) 
		{
			this.owner = owner;
		}
		public Branch(BrowseEndpoint owner, Schema schema)
		{
			this.owner = owner;
			this.schema = schema;
		}
		public Branch(BrowseEndpoint owner, Schema schema, Schema.Assay assay)
		{
			this.owner = owner;
			this.schema = schema;
			this.assay = assay;
		}
	}

	// ------------ public methods ------------	

	public BrowseEndpoint(Stage stage)
	{
		this.stage = stage;

		stage.setTitle("BioAssay Schema Browser");

		treeRoot = new TreeItem<>(new Branch(this));
		treeView = new TreeView<>(treeRoot);
		treeView.setEditable(true);
		treeView.setCellFactory(p -> new BrowseTreeCell());
		treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldval, newval) -> {changeValue(newval.getValue());});

		display = new DisplaySchema();

		StackPane sp1 = new StackPane(), sp2 = new StackPane();
		sp1.getChildren().add(treeView);
		sp2.getChildren().add(display);
		
		splitter = new SplitPane();
		splitter.setOrientation(Orientation.HORIZONTAL);
		splitter.getItems().addAll(sp1, sp2);
		splitter.setDividerPositions(0.4, 1.0);

		root = new BorderPane();
		root.setTop(menuBar);
		root.setCenter(splitter);

		Scene scene = new Scene(root, 700, 600, Color.WHITE);

		stage.setScene(scene);
		
		treeView.setShowRoot(false);
		
		rebuildTree();

		Platform.runLater(() -> treeView.getFocusModel().focus(treeView.getSelectionModel().getSelectedIndex()));  // for some reason it defaults to not the first item
		
		new Thread(() -> backgroundLoadTemplates()).start();
	}

	public TreeView<Branch> getTreeView() {return treeView;}

	// ------------ private methods ------------	

	private void rebuildTree()
	{
		currentlyRebuilding = true;
	
		treeRoot.getChildren().clear();
		
		for (int n = 0; n < schemaList.length; n++)
		{
			TreeItem<Branch> item = new TreeItem<>(new Branch(this, schemaList[n]));
			item.setExpanded(true);
			treeRoot.getChildren().add(item);
			
			for (int i = 0; i < schemaList[n].numAssays(); i++)
			{
				Schema.Assay assay = schemaList[n].getAssay(i);
				item.getChildren().add(new TreeItem<Branch>(new Branch(this, schemaList[n], assay)));
			}
		}
		
		currentlyRebuilding = false;
	}

	public TreeItem<Branch> currentBranch() {return treeView.getSelectionModel().getSelectedItem();}
	public Branch currentBranchValue()
	{
		TreeItem<Branch> item = currentBranch();
		return item == null ? null : item.getValue();
	}
	public void setCurrentBranch(TreeItem<Branch> branch) 
	{
		treeView.getSelectionModel().select(branch);
		treeView.scrollTo(treeView.getRow(branch));
	}
	
	private void changeValue(Branch branch)
	{
		if (branch.assay == null) display.setTemplate(branch.schema); else display.setAssay(branch.schema, branch.assay);
	}

	// fires up a thread that pulls the list of assays from the SPARQL endpoint
	private void backgroundLoadTemplates()
	{
		try
		{
			EndpointSchema endpoint = new EndpointSchema(EditorPrefs.getSparqlEndpoint());
			String[] rootURI = endpoint.enumerateTemplates();
			Schema[] schemaList = new Schema[rootURI.length];
			
			for (int n = 0; n < rootURI.length; n++) 
			{
				schemaList[n] = endpoint.obtainTemplate(rootURI[n]);
				
				String[] assayURI = endpoint.enumerateAssays(rootURI[n]);
				for (String uri : assayURI) endpoint.obtainAssay(schemaList[n], uri);
			}
			Arrays.sort(schemaList, (v1, v2) -> v1.getRoot().name.compareTo(v2.getRoot().name));
			
			Platform.runLater(() -> 
			{
				this.schemaList = schemaList;
				rebuildTree();
			});
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			Platform.runLater(() -> UtilGUI.informWarning("SPARQL failure", "Unable to fetch a list of templates. See console output for detials."));
		}
	}
	
	// ------------ action responses ------------	

	public void actionOpen()
	{
		Branch branch = currentBranchValue();
		if (branch == null || branch.schema == null) return;

		Stage stage = new Stage();
		EditSchema edit = new EditSchema(stage, null);
		edit.loadFile(null, new SchemaUtil.SerialData(SchemaUtil.SerialFormat.JSON, branch.schema));
		stage.show();
	}
	
	public void actionCopy()
	{
		Branch branch = currentBranchValue();
		if (branch == null || branch.assay == null) return;
		
		JSONObject json = ClipboardSchema.composeAssay(branch.assay);
		
		String serial = null;
		try {serial = json.toString(2);}
		catch (JSONException ex) {return;}
		
		ClipboardContent content = new ClipboardContent();
		content.putString(serial);
		if (!Clipboard.getSystemClipboard().setContent(content))
		{
			UtilGUI.informWarning("Clipboard Copy", "Unable to copy to the clipboard.");
			return;
		}
	}
}
