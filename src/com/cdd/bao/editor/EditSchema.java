/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.editor;

import com.cdd.bao.*;
import com.cdd.bao.template.*;

import java.io.*;
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
	Edit Schema: the primary window for BAO schema editing, which is responsible for taking care of a data instance.
*/

public class EditSchema
{
	// ------------ private data ------------	

	private File schemaFile = null;
	private StackSchema stack = new StackSchema();

    private Stage stage;
    private BorderPane root;
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

	// ------------ public methods ------------	

	public EditSchema(Stage stage)
	{
		this.stage = stage;

		menuBar = new MenuBar();
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
		treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<Branch>>()
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
		new Thread(() -> {try {Vocabulary.globalInstance();} catch (IOException ex) {}}).start();
 	}

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
	}

	// ------------ private methods ------------	

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
			TreeItem<Branch> branch = stack.remove(0);
			if (branch.getValue().locatorID.equals(locatorID)) return branch;
			for (TreeItem<Branch> item : branch.getChildren()) stack.add(item);
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
			// TODO
		}
	}

	// recreates all the widgets in the detail view, given that the indicated branch has been selected
	private void pushDetail(TreeItem<Branch> item)
	{
		if (currentlyRebuilding || item == null) return;
		Branch branch = item.getValue();

		if (branch.group != null) detail.setGroup(branch.group);
		else if (branch.assignment != null) detail.setAssignment(branch.assignment);
		else if (branch.assay != null) detail.setAssay(branch.assay);
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

	/*private void treeDoubleClick(MouseEvent event)
	{
        TreeItem<String> item = treeview.getSelectionModel().getSelectedItem();
        System.out.println("DOUBLE CLICK : " + item.getValue());
	}        
	private void treeRightClick(MouseEvent event)
	{
        MenuItem addMenuItem = new MenuItem("Fnord!");
        ContextMenu ctx = new ContextMenu();
        ctx.getItems().add(addMenuItem);
        addMenuItem.setOnAction(new EventHandler<ActionEvent>()
        {
            public void handle(ActionEvent t)
            {
            	Util.writeln("--> FNORD!");
            }
        });
        
        TreeItem<String> item = treeview.getSelectionModel().getSelectedItem();
		Util.writeln("RIGHT CLICK");
	}*/
	
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

	// ------------ action responses ------------	
	
	private void actionFileNew()
	{
		Stage stage = new Stage();
		EditSchema edit = new EditSchema(stage);
		stage.show();
	}
	private void actionFileSave(boolean promptNew)
	{
		pullDetail();
	
		// dialog in case filename is missing or requested as save-to-other
		if (promptNew || schemaFile == null)
		{
            FileChooser chooser = new FileChooser();
        	chooser.setTitle("Save Schema Template");
        	if (schemaFile != null) chooser.setInitialDirectory(schemaFile.getParentFile());
        	
        	File file = chooser.showSaveDialog(stage);
    		if (file == null) return;
    		
    		if (!file.getName().endsWith(".ttl")) file = new File(file.getAbsolutePath() + ".ttl");

			schemaFile = file;
		}
		
		// validity checking
		if (schemaFile == null) return;
		if (!schemaFile.getParentFile().canWrite() || (schemaFile.exists() && !schemaFile.canWrite()))
		{
			informMessage("Cannot Save", "Not able to write to file: " + schemaFile.getAbsolutePath());
            return;
		}
	
		// serialise-to-file
		Schema schema = stack.peekSchema();
		try 
		{
			//schema.serialise(System.out);
			
			OutputStream ostr = new FileOutputStream(schemaFile);
			schema.serialise(ostr);
			ostr.close();
			
			stack.setDirty(false);
		}
		catch (Exception ex) {ex.printStackTrace();}
	}
	private void actionFileOpen()
	{
        FileChooser chooser = new FileChooser();
    	chooser.setTitle("Open Schema Template");
    	if (schemaFile != null) chooser.setInitialDirectory(schemaFile.getParentFile());
    	
    	File file = chooser.showOpenDialog(stage);
		if (file == null) return;
		
		try
		{
			Schema schema = Schema.deserialise(file);

    		Stage stage = new Stage();
    		EditSchema edit = new EditSchema(stage);
			edit.loadFile(file, schema);
    		stage.show();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			informWarning("Open", "Failed to parse file: is it a valid schema?");
		}
	}
	private void actionFileClose()
	{
		if (!confirmClose()) return;
		stage.close();
	}
	private void actionFileQuit()
	{
		if (!confirmClose()) return;
		Platform.exit();
	}
    private void actionGroupAdd()
    {
    	TreeItem<Branch> item = currentBranch();
    	if (item == null || (item.getValue().group == null && item.getValue().assignment == null))
    	{
    		informMessage("Add Group", "Select a group to add to.");
    		return;
    	}

    	pullDetail();

    	Schema schema = stack.getSchema();
    	Schema.Group parent = schema.obtainGroup(item.getValue().locatorID);
    	Schema.Group newGroup = schema.appendGroup(parent, new Schema.Group(null, ""));
    	stack.changeSchema(schema);
    	rebuildTree();
    	setCurrentBranch(locateBranch(schema.locatorID(newGroup)));
    }
    private void actionAssignmentAdd()
    {
    	TreeItem<Branch> item = currentBranch();
    	if (item == null || (item.getValue().group == null && item.getValue().assignment == null))
    	{
    		informMessage("Add Assignment", "Select a group to add to.");
    		return;
    	}

    	pullDetail();

    	Schema schema = stack.getSchema();

    	Schema.Group parent = schema.obtainGroup(item.getValue().locatorID);
    	Schema.Assignment newAssn = schema.appendAssignment(parent, new Schema.Assignment(null, "", ""));
    	stack.changeSchema(schema);
    	rebuildTree();
    	setCurrentBranch(locateBranch(schema.locatorID(newAssn)));
    }
	private void actionAssayAdd()
	{
		pullDetail();
		
		Schema schema = stack.getSchema();
		schema.addAssay(new Schema.Assay(""));
		stack.changeSchema(schema);
		rebuildTree();
		// !! setcurrent... zog
	}
	private void actionEditCopy(boolean andCut)
	{
		if (!treeView.isFocused()) return; // punt to default action
		
		TreeItem<Branch> item = currentBranch();
		if (item == null) return;
		Branch branch = item.getValue();
		
		JSONObject json = null;
		if (branch.group != null) json = ClipboardSchema.composeGroup(branch.group);
		else if (branch.assignment != null) json = ClipboardSchema.composeAssignment(branch.assignment);
		else if (branch.assay != null) {} // TODO
		
		String serial = null;
		try {serial = json.toString(2);}
		catch (JSONException ex) {return;}
		
		ClipboardContent content = new ClipboardContent();
		content.putString(serial);
		if (!Clipboard.getSystemClipboard().setContent(content))
		{
			informWarning("Clipboard Copy", "Unable to copy to the clipboard.");
			return;
		}
		
		if (andCut) actionEditDelete();
	}
	private void actionEditPaste()
	{
		if (!treeView.isFocused()) return; // punt to default action
		
		TreeItem<Branch> item = currentBranch();
		if (item == null)
		{
			informMessage("Clipboard Paste", "Select a group to paste into.");
			return;
		}
		Branch branch = item.getValue();
		
		Clipboard clipboard = Clipboard.getSystemClipboard();
		String serial = clipboard.getString();
		if (serial == null)
		{
			informWarning("Clipboard Paste", "Content is not parseable.");
			return;
		}
		
		JSONObject json = null;
		try {json = new JSONObject(new JSONTokener(serial));}
		catch (JSONException ex)
		{
			informWarning("Clipboard Paste", "Content is not parseable: it should be a JSON-formatted string.");
			return;
		}
		
		Schema.Group group = ClipboardSchema.unpackGroup(json);
		Schema.Assignment assn = ClipboardSchema.unpackAssignment(json);
		if (group == null && assn == null)
		{
			informWarning("Clipboard Paste", "Content does not represent a group or assignment: cannot paste.");
			return;
		}
		
		pullDetail();
		Schema schema = stack.getSchema();
		
		if (group != null)
		{
			//Util.writeln("PASTEGROUP:"+group.name);
			//Util.writeln(ClipboardSchema.composeGroup(group).toString());
			schema.appendGroup(schema.obtainGroup(branch.locatorID), group);
		}
		else if (assn != null)
		{
			//Util.writeln("PASTEASSN:"+assn.name);
			//Util.writeln(ClipboardSchema.composeAssignment(assn).toString());
			schema.appendAssignment(schema.obtainGroup(branch.locatorID), assn);
		}
		
    	stack.changeSchema(schema);
    	rebuildTree();

		if (group != null) setCurrentBranch(locateBranch(schema.locatorID(group)));
		else if (assn != null) setCurrentBranch(locateBranch(schema.locatorID(assn)));
    	
	}
    private void actionEditDelete()
    {
    	TreeItem<Branch> item = currentBranch();
    	Branch branch = item == null ? null : item.getValue();
    	// TODO: assay..
    	if (branch == null || (branch.group == null && branch.assignment == null))
    	{
    		informMessage("Delete Branch", "Select a group or assignment to delete.");
    		return;
    	}
    	if (item == treeRoot)
    	{
    		informMessage("Delete Branch", "Can't delete the root branch.");
    		return;
    	}
    	
    	pullDetail();
    	
    	Schema schema = stack.getSchema();
    	Schema.Group parent = null;
    	if (branch.group != null)
    	{
    		Schema.Group group = schema.obtainGroup(branch.locatorID);
    		parent = group.parent;
    		schema.deleteGroup(group);
    	}
    	if (branch.assignment != null)
    	{
    		Schema.Assignment assn = schema.obtainAssignment(branch.locatorID);
    		parent = assn.parent;
    		schema.deleteAssignment(assn);
    	}
    	stack.changeSchema(schema);
    	rebuildTree();
    	setCurrentBranch(locateBranch(schema.locatorID(parent)));
    }
    private void actionEditUndo()
    {
    	if (!stack.canUndo())
    	{
    		informMessage("Undo", "Nothing to undo.");
    		return;
    	}
    	stack.performUndo();
    	rebuildTree();
    	clearSelection();
    }
    private void actionEditRedo()
    {
    	if (!stack.canRedo())
    	{
    		informMessage("Redo", "Nothing to redo.");
    		return;
    	}
    	stack.performRedo();
    	rebuildTree();
    	clearSelection();
    }
    private void actionEditMove(int dir)
    {
    	TreeItem<Branch> item = currentBranch();
    	Branch branch = item == null ? null : item.getValue();
    	// TODO: assay...
    	if (item == treeRoot || branch == null || (branch.group == null && branch.assignment == null)) return;
    	
    	pullDetail();
    	Schema schema = stack.getSchema();
    	String newLocator = "";
    	if (branch.group != null)
    	{
    		Schema.Group group = schema.obtainGroup(branch.locatorID);
    		schema.moveGroup(group, dir);
    		newLocator = schema.locatorID(group);
    	}
    	if (branch.assignment != null)
    	{
    		Schema.Assignment assn = schema.obtainAssignment(branch.locatorID);
    		schema.moveAssignment(assn, dir);
    		newLocator = schema.locatorID(assn);
    	}
    	stack.changeSchema(schema);
    	rebuildTree();
    	setCurrentBranch(locateBranch(newLocator));
    }
    private void actionViewTemplate()
    {
		treeTemplate.setExpanded(true);
		treeAssays.setExpanded(false);
		treeView.getSelectionModel().select(treeTemplate);
        treeView.getFocusModel().focus(treeView.getSelectionModel().getSelectedIndex());
        Platform.runLater(() -> treeView.getFocusModel().focus(treeView.getSelectionModel().getSelectedIndex()));
    }
	private void actionViewAssays()
	{
		treeTemplate.setExpanded(false);
		treeAssays.setExpanded(true);
		treeView.getSelectionModel().select(treeAssays);
        Platform.runLater(() -> treeView.getFocusModel().focus(treeView.getSelectionModel().getSelectedIndex()));
	}
}
