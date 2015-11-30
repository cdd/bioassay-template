/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.editor.endpoint;

import com.cdd.bao.*;
import com.cdd.bao.template.*;

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

	/*private File schemaFile = null;
	private StackSchema stack = new StackSchema();*/

    private Stage stage;
    /*private BorderPane root;
    private SplitPane splitter;
    private TreeView<Branch> treeView;
    private TreeItem<Branch> treeRoot, treeTemplate, treeAssays;
    private DetailPane detail;
    
    private MenuBar menuBar;
    private Menu menuFile, menuEdit, menuValue, menuView;
    
    private boolean currentlyRebuilding = false;
    
    // a "branch" encapsulates a tree item which is a generic heading, or one of the objects used within the schema
    public static final class Branch
    {
    	public String heading = null;
    	public Schema.Group group = null;
    	public Schema.Assignment assignment = null;
    	public Schema.Assay assay = null;
    	public String locatorID = null;

		public Branch() {}
		public Branch(String heading)
		{
			this.heading = heading;
		}
    	public Branch(Schema.Group group, String locatorID)
    	{
    		this.group = group.clone();
    		this.locatorID = locatorID;
    	}
    	public Branch(Schema.Assignment assignment, String locatorID)
    	{
    		this.assignment = assignment.clone();
    		this.locatorID = locatorID;
    	}
    	public Branch(Schema.Assay assay, String locatorID)
    	{
    		this.assay = assay;
    		this.locatorID = locatorID;
    	}
    }
*/
	// ------------ public methods ------------	

	public BrowseEndpoint(Stage stage)
	{
		this.stage = stage;

		/*menuBar = new MenuBar();
		menuBar.setUseSystemMenuBar(true);
		menuBar.getMenus().add(menuFile = new Menu("_File"));
		menuBar.getMenus().add(menuEdit = new Menu("_Edit"));
		menuBar.getMenus().add(menuValue = new Menu("_Value"));
		menuBar.getMenus().add(menuView = new Menu("Vie_w"));
		createMenuItems();

		treeRoot = new TreeItem<Branch>(new Branch());
		treeView = new TreeView<Branch>(treeRoot);
		treeView.setEditable(true);
		treeView.setCellFactory(new Callback<TreeView<Branch>, TreeCell<Branch>>()
		{
            public TreeCell<Branch> call(TreeView<Branch> p) {return new HierarchyTreeCell();}
        });*/
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

		detail = new DetailPane(this);

		StackPane sp1 = new StackPane(), sp2 = new StackPane();
		sp1.getChildren().add(treeView);
		sp2.getChildren().add(detail);
		
		splitter = new SplitPane();
		splitter.setOrientation(Orientation.HORIZONTAL);
		splitter.getItems().addAll(sp1, sp2);
		splitter.setDividerPositions(0.4, 1.0);

		root = new BorderPane();
		root.setTop(menuBar);
		root.setCenter(splitter);

		Scene scene = new Scene(root, 900, 800, Color.WHITE);

		stage.setScene(scene);
		
		treeView.setShowRoot(false);
		treeRoot.getChildren().add(treeTemplate = new TreeItem<Branch>(new Branch("Template")));
		treeRoot.getChildren().add(treeAssays = new TreeItem<Branch>(new Branch("Assays")));
		treeTemplate.setExpanded(true);
		treeAssays.setExpanded(true);
		
		rebuildTree();

        Platform.runLater(() -> treeView.getFocusModel().focus(treeView.getSelectionModel().getSelectedIndex()));  // for some reason it defaults to not the first item
		
		stage.setOnCloseRequest(event -> 
		{
			if (!confirmClose()) event.consume();
		});
		
		updateTitle();
		
		// instantiate vocabulary in a background thread: we don't need it immediately, but prevent blocking later
		new Thread(() -> {try {Vocabulary.globalInstance();} catch (IOException ex) {}}).start();*/
 	}

/*
	public TreeView<Branch> getTreeView() {return treeView;}
	public DetailPane getDetailView() {return detail;}

	// loads a file and parses the schema
	public void loadFile(File file)
	{
		try
		{
			Schema schema = Schema.deserialise(file);
			loadFile(file, schema);
		}
		catch (Exception ex) 
		{
			ex.printStackTrace();
			return;
		}
	}
	
	// loads a file with an already-parsed schema
	public void loadFile(File file, Schema schema)
	{
		schemaFile = file;
		stack.setSchema(schema);
		updateTitle();
		rebuildTree();
	}*/

	// ------------ private methods ------------	

/*
	private void updateTitle()
	{
		String title = "BioAssay Schema Editor";
		if (schemaFile != null) title += " - " + schemaFile.getName();
        stage.setTitle(title);
	}

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
    }

	private void rebuildTree()
	{
		currentlyRebuilding = true;
	
		treeTemplate.getChildren().clear();
		treeAssays.getChildren().clear();
		
		Schema schema = stack.getSchema();
		Schema.Group root = schema.getRoot();
		
		treeTemplate.setValue(new Branch(root, schema.locatorID(root)));
		fillTreeGroup(schema, root, treeTemplate);
		
		for (int n = 0; n < schema.numAssays(); n++)
		{
			Schema.Assay assay = schema.getAssay(n);
			TreeItem<Branch> item = new TreeItem<>(new Branch(assay, schema.locatorID(assay)));
			treeAssays.getChildren().add(item);
		}
		
		currentlyRebuilding = false;
	}
	
	private void fillTreeGroup(Schema schema, Schema.Group group, TreeItem<Branch> parent)
	{
		for (Schema.Assignment assn : group.assignments)
		{
			TreeItem<Branch> item = new TreeItem<>(new Branch(assn, schema.locatorID(assn)));
			parent.getChildren().add(item);
		}
		for (Schema.Group subgrp : group.subGroups)
		{
			TreeItem<Branch> item = new TreeItem<>(new Branch(subgrp, schema.locatorID(subgrp)));
			item.setExpanded(true);
			parent.getChildren().add(item);
			fillTreeGroup(schema, subgrp, item);
		}
	}

	// convenience for tree selection
	public TreeItem<Branch> currentBranch() {return treeView.getSelectionModel().getSelectedItem();}
	public void setCurrentBranch(TreeItem<Branch> branch) 
	{
		treeView.getSelectionModel().select(branch);
		treeView.scrollTo(treeView.getRow(branch));
	}
	public String currentLocatorID()
	{
		TreeItem<Branch> branch = currentBranch();
		return branch == null ? null : branch.getValue().locatorID;
	}
	public TreeItem<Branch> locateBranch(String locatorID)
	{
		List<TreeItem<Branch>> stack = new ArrayList<>();
		stack.add(treeRoot);
		while (stack.size() > 0)
		{
			TreeItem<Branch> item = stack.remove(0);
			String lookID = item.getValue().locatorID;
			if (lookID != null && lookID.equals(locatorID)) return item;
			for (TreeItem<Branch> sub : item.getChildren()) stack.add(sub);
		}
		return null;
	}
	public void clearSelection()
	{
		detail.clearContent();
		treeView.getSelectionModel().clearSelection();
	}

	// for the given branch, pulls out the content: if any changes have been made, pushes the modified schema onto the stack
	private void pullDetail() {pullDetail(currentBranch());}
	private void pullDetail(TreeItem<Branch> item)
	{
		if (currentlyRebuilding || item == null) return;
		Branch branch = item.getValue();

		if (branch.group != null)
		{
			// if root, check prefix first
			String prefix = detail.extractPrefix();
			if (prefix != null)
			{
				prefix = prefix.trim();
				if (!prefix.endsWith("#")) prefix += "#";
				if (!stack.peekSchema().getSchemaPrefix().equals(prefix))
				{
    				try {new URI(prefix);}
    				catch (Exception ex)
    				{
    					//informMessage("Invalid URI", "Prefix is not a valid URI: " + prefix);
    					return;
    				}
    				Schema schema = stack.getSchema();
    				schema.setSchemaPrefix(prefix);
    				stack.setSchema(schema);
				}
			}
			
			// then handle the group content
			Schema.Group modGroup = detail.extractGroup();
			if (modGroup == null) return;

			Schema schema = stack.getSchema();
			if (branch.group.parent != null)
			{
				// reparent the modified group, then swap it out in the parent's child list
				Schema.Group replGroup = schema.obtainGroup(branch.locatorID);
				modGroup.parent = replGroup.parent;
				int idx = replGroup.parent.subGroups.indexOf(replGroup);
				replGroup.parent.subGroups.set(idx, modGroup);
			}
			else schema.setRoot(modGroup);
			
			branch.group = modGroup;
			stack.changeSchema(schema, true);
			
			item.setValue(new Branch());
			item.setValue(branch); // triggers redraw
		}
		else if (branch.assignment != null)
		{
			Schema.Assignment modAssn = detail.extractAssignment();
			if (modAssn == null) return;
			
			Schema schema = stack.getSchema();
			
			// reparent the modified assignment, then swap it out in the parent's child list
			Schema.Assignment replAssn = schema.obtainAssignment(branch.locatorID);
			modAssn.parent = replAssn.parent;
			int idx = replAssn.parent.assignments.indexOf(replAssn);
			replAssn.parent.assignments.set(idx, modAssn);
			
			branch.assignment = modAssn;
			stack.changeSchema(schema, true);
			
			item.setValue(new Branch());
			item.setValue(branch); // triggers redraw
		}
		else if (branch.assay != null)
		{
			Schema.Assay modAssay = detail.extractAssay();
			if (modAssay == null) return;
			
			Schema schema = stack.getSchema();

			int idx = schema.indexOfAssay(branch.locatorID);
			schema.setAssay(idx, modAssay);
			branch.assay = modAssay;
			stack.changeSchema(schema, true);
			
			item.setValue(new Branch());
			item.setValue(branch); // triggers redraw
		}
	}

	// recreates all the widgets in the detail view, given that the indicated branch has been selected
	private void pushDetail(TreeItem<Branch> item)
	{
		if (currentlyRebuilding || item == null) return;
		Branch branch = item.getValue();

		if (branch.group != null) detail.setGroup(stack.peekSchema(), branch.group);
		else if (branch.assignment != null) detail.setAssignment(stack.peekSchema(), branch.assignment);
		else if (branch.assay != null) detail.setAssay(stack.peekSchema(), branch.assay);
		else detail.clearContent();
	}

	// in case the detail has changed, update the main part of the tree
	private void maybeUpdateTree()
	{
		pullDetail();
	}

	// returns true if the data is already saved, or the user agrees to abandon it
	private boolean confirmClose()
	{
		pullDetail();
	
		if (!stack.isDirty()) return true;
		
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Close Window");
        alert.setHeaderText("Abandon changes");
        alert.setContentText("Closing this window will cause modifications to be lost.");
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.get() == ButtonType.OK;
	}

	private void informMessage(String title, String msg)
	{
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
	}
	private void informWarning(String title, String msg)
	{
		Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
	}
*/

	// ------------ action responses ------------	
	

}
