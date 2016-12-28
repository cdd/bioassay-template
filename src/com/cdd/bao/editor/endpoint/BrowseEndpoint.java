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

		/*menuBar = new MenuBar();
		menuBar.setUseSystemMenuBar(true);
		menuBar.getMenus().add(menuFile = new Menu("_File"));
		menuBar.getMenus().add(menuEdit = new Menu("_Edit"));
		menuBar.getMenus().add(menuValue = new Menu("_Value"));
		menuBar.getMenus().add(menuView = new Menu("Vie_w"));
		createMenuItems();*/

		treeRoot = new TreeItem<>(new Branch(this));
		treeView = new TreeView<>(treeRoot);
		treeView.setEditable(true);
		treeView.setCellFactory(new Callback<TreeView<Branch>, TreeCell<Branch>>()
		{
            public TreeCell<Branch> call(TreeView<Branch> p) {return new BrowseTreeCell();}
        });
        /*treeview.setOnMouseClicked(new EventHandler<MouseEvent>()
        {
            public void handle(MouseEvent event)
            {            
				if (event.getClickCount() == 2) treeDoubleClick(event);
            }
        });
        treeview.setOnMousePressed(new EventHandler<MouseEvent>()
		{
			public void handle(MouseEvent event)
			{
				if (event.getButton().equals(MouseButton.SECONDARY) || event.isControlDown()) treeRightClick(event);
			}
		});*/
		/*treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<Branch>>()
		{
	        public void changed(ObservableValue<? extends TreeItem<Branch>> observable, TreeItem<Branch> oldVal, TreeItem<Branch> newVal) 
	        {
	        	if (oldVal != null) pullDetail(oldVal);
	        	if (newVal != null) pushDetail(newVal);
            }
		});	
		treeView.focusedProperty().addListener((val, oldValue, newValue) -> Platform.runLater(() -> maybeUpdateTree()));
		*/
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
		
		/*stage.setOnCloseRequest(event -> 
		{
			if (!confirmClose()) event.consume();
		});*/
		
		new Thread(() -> backgroundLoadTemplates()).start();
 	}

	public TreeView<Branch> getTreeView() {return treeView;}
	//public DetailPane getDetailView() {return detail;}

	// ------------ private methods ------------	

/*
	private void createMenuItems()
    {
    	final KeyCombination.Modifier cmd = KeyCombination.SHORTCUT_DOWN, shift = KeyCombination.SHIFT_DOWN;
    
    	addMenu(menuFile, "_New", new KeyCharacterCombination("N", cmd)).setOnAction(event -> actionFileNew());
    	addMenu(menuFile, "_Open", new KeyCharacterCombination("O", cmd)).setOnAction(event -> actionFileOpen());
    	addMenu(menuFile, "_Save", new KeyCharacterCombination("S", cmd)).setOnAction(event -> actionFileSave(false));
    	addMenu(menuFile, "Save _As", new KeyCharacterCombination("S", cmd, shift)).setOnAction(event -> actionFileSave(true));
		menuFile.getItems().add(new SeparatorMenuItem());
		addMenu(menuFile, "Confi_gure", new KeyCharacterCombination(",", cmd)).setOnAction(event -> actionFileConfigure());
		addMenu(menuFile, "_Browse Triples", new KeyCharacterCombination("B", cmd, shift)).setOnAction(event -> actionFileBrowse());
		addMenu(menuFile, "_Upload Triples", new KeyCharacterCombination("U", cmd, shift)).setOnAction(event -> actionFileUpload());
		menuFile.getItems().add(new SeparatorMenuItem());
    	addMenu(menuFile, "_Close", new KeyCharacterCombination("W", cmd)).setOnAction(event -> actionFileClose());
    	addMenu(menuFile, "_Quit", new KeyCharacterCombination("Q", cmd)).setOnAction(event -> actionFileQuit());
    	
		addMenu(menuEdit, "Add _Group", new KeyCharacterCombination("G", cmd, shift)).setOnAction(event -> actionGroupAdd());
		addMenu(menuEdit, "Add _Assignment", new KeyCharacterCombination("A", cmd, shift)).setOnAction(event -> actionAssignmentAdd());
		addMenu(menuEdit, "Add Assa_y", new KeyCharacterCombination("Y", cmd, shift)).setOnAction(event -> actionAssayAdd());
		menuEdit.getItems().add(new SeparatorMenuItem());
		addMenu(menuEdit, "Cu_t", new KeyCharacterCombination("X", cmd)).setOnAction(event -> actionEditCopy(true));
		addMenu(menuEdit, "_Copy", new KeyCharacterCombination("C", cmd)).setOnAction(event -> actionEditCopy(false));
		addMenu(menuEdit, "_Paste", new KeyCharacterCombination("V", cmd)).setOnAction(event -> actionEditPaste());
		menuEdit.getItems().add(new SeparatorMenuItem());
    	addMenu(menuEdit, "_Delete", new KeyCodeCombination(KeyCode.DELETE, cmd, shift)).setOnAction(event -> actionEditDelete());
    	addMenu(menuEdit, "_Undo", new KeyCharacterCombination("Z", cmd)).setOnAction(event -> actionEditUndo());
    	addMenu(menuEdit, "_Redo", new KeyCharacterCombination("Z", cmd, shift)).setOnAction(event -> actionEditRedo());
		menuEdit.getItems().add(new SeparatorMenuItem());
		addMenu(menuEdit, "Move _Up", new KeyCharacterCombination("[", cmd)).setOnAction(event -> actionEditMove(-1));
		addMenu(menuEdit, "Move _Down", new KeyCharacterCombination("]", cmd)).setOnAction(event -> actionEditMove(1));

		addMenu(menuValue, "_Add Value", new KeyCharacterCombination("V", cmd, shift)).setOnAction(event -> detail.actionValueAdd());
		addMenu(menuValue, "_Delete Value", new KeyCodeCombination(KeyCode.DELETE, cmd)).setOnAction(event -> detail.actionValueDelete());
		addMenu(menuValue, "Move _Up", new KeyCodeCombination(KeyCode.UP, cmd)).setOnAction(event -> detail.actionValueMove(-1));
		addMenu(menuValue, "Move _Down", new KeyCodeCombination(KeyCode.DOWN, cmd)).setOnAction(event -> detail.actionValueMove(1));
		menuValue.getItems().add(new SeparatorMenuItem());
		addMenu(menuValue, "_Lookup URI", new KeyCharacterCombination("U", cmd)).setOnAction(event -> detail.actionLookupURI());
		addMenu(menuValue, "Lookup _Name", new KeyCharacterCombination("L", cmd)).setOnAction(event -> detail.actionLookupName());

    	addMenu(menuView, "_Template", new KeyCharacterCombination("1", cmd)).setOnAction(event -> actionViewTemplate());
    	addMenu(menuView, "_Assays", new KeyCharacterCombination("2", cmd)).setOnAction(event -> actionViewAssays());
    }
    
    private MenuItem addMenu(Menu parent, String title, KeyCombination accel)
    {
    	MenuItem item = new MenuItem(title);
    	parent.getItems().add(item);
    	if (accel != null) item.setAccelerator(accel);
    	return item;
    }*/

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
	        Platform.runLater(() -> Util.informWarning("SPARQL failure", "Unable to fetch a list of templates. See console output for detials."));
		}
	}
	
	// ------------ action responses ------------	

	public void actionOpen()
	{
		Branch branch = currentBranchValue();
		if (branch == null || branch.schema == null) return;

		Stage stage = new Stage();
		EditSchema edit = new EditSchema(stage);
		edit.loadFile(null, branch.schema);
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
			Util.informWarning("Clipboard Copy", "Unable to copy to the clipboard.");
			return;
		}
		
	}
}
