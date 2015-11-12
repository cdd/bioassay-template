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
import javafx.beans.value.*;
import javafx.util.*;

/*
	Edit Schema: the primary window for BAO schema editing, which is responsible for taking care of a data instance.
*/

public class EditSchema
{
	// ------------ private data ------------	

	private File schemaFile = null;
	private Schema schema = new Schema(null);

    private Stage stage;
    private BorderPane root;
    private SplitPane splitter;
    private TreeView<String> treeview;
    private SchemaBranch treeroot;
    private DetailPane detail;
    
    private MenuBar menuBar;
    private Menu menuFile, menuEdit, menuWindow;
    private MenuItem miFileNew;
    
    private final class SchemaBranch extends TreeItem<String>
    {
    	Schema.Group group = null;
    	Schema.Assignment assignment = null;

		SchemaBranch(String label)
		{
			super(label);
		}    	
    	SchemaBranch(Schema.Group category)
    	{
    		super(category.groupName);
    		this.group = category;
    	}
    	SchemaBranch(Schema.Assignment assignment)
    	{
    		super(assignment.assnName);
    		this.assignment = assignment;
    	}
    }

	// ------------ public methods ------------	

	public EditSchema(Stage stage)
	{
		this.stage = stage;

        stage.setTitle("BioAssay Schema Editor");        

		menuBar = new MenuBar();
		menuBar.setUseSystemMenuBar(true);
		menuBar.getMenus().add(menuFile = new Menu("_File"));
		menuBar.getMenus().add(menuWindow = new Menu("_Window"));
		createMenuItems();

		treeroot = new SchemaBranch("Assay");
		treeview = new TreeView<String>(treeroot);
		treeview.setEditable(true);
		treeview.setCellFactory(new Callback<TreeView<String>, TreeCell<String>>()
		{
            public TreeCell<String> call(TreeView<String> p) {return new AssayHierarchyTreeCell();}
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
		treeview.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<String>>()
		{
	        public void changed(ObservableValue<? extends TreeItem<String>> observable, TreeItem<String> oldVal, TreeItem<String> newVal) 
	        {
	        	SchemaBranch branch = (SchemaBranch)newVal;
	        	updateDetail(branch);
            }
		});		

		detail = new DetailPane();

		StackPane sp1 = new StackPane(), sp2 = new StackPane();
		sp1.getChildren().add(treeview);
		sp2.getChildren().add(detail);
		
		splitter = new SplitPane();
		splitter.setOrientation(Orientation.HORIZONTAL);
		splitter.getItems().addAll(sp1, sp2);
		splitter.setDividerPositions(0.4, 1.0);

		root = new BorderPane();
		root.setTop(menuBar);
		root.setCenter(splitter);

		Scene scene = new Scene(root, 800, 600, Color.WHITE);

		stage.setScene(scene);
		
		rebuildTree();
		
		// instantiate vocabulary in a background thread: we don't need it immediately, but prevent blocking later
		new Thread(new Runnable()
		{
			public void run() {try {Vocabulary.globalInstance();} catch (IOException ex) {}}
		}).start();
 	}

	public void loadFile(File f)
	{
		try
		{
			schema = Schema.deserialise(f);
			
			//Util.writeln("SCHEMA:\n" + schema.toString());
		}
		catch (Exception ex) 
		{
			ex.printStackTrace();
			return;
		}
		
		rebuildTree();
	}

	// ------------ private methods ------------	

	private void createMenuItems()
    {
		menuFile.getItems().add(miFileNew=new MenuItem("_New"));
		miFileNew.setMnemonicParsing(true);
		miFileNew.setAccelerator(new KeyCharacterCombination("N", KeyCombination.SHORTCUT_DOWN));
		miFileNew.setOnAction(new EventHandler<ActionEvent>()
    	{
            public void handle(ActionEvent ev)
            {
                Util.writeln("NEW!");
            }
    	});
    
    	//miWindowHub=new MenuItem("_Hub");
        //menuWindow.getItems().add(miWindowHub);
    }

	private void rebuildTree()
	{
		treeroot.getChildren().clear();
		treeroot.setExpanded(true);
		
		Schema.Group root = schema.getRoot();
		
		treeroot.setValue(root.groupName);
		treeroot.group = root;
		fillTreeGroup(root, treeroot);
	}
	
	private void fillTreeGroup(Schema.Group category, TreeItem<String> parent)
	{
		for (Schema.Assignment assn : category.assignments)
		{
			SchemaBranch item = new SchemaBranch(assn);
			parent.getChildren().add(item);
		}
		for (Schema.Group subcat : category.subGroups)
		{
			SchemaBranch item = new SchemaBranch(subcat);
			parent.getChildren().add(item);
		}
	}

	// recreates all the widgets in the detail view, given that the indicated branch has been selected
	private void updateDetail(SchemaBranch branch)
	{
		if (branch.group != null) detail.setGroup(branch.group);
		else if (branch.assignment != null) detail.setAssignment(branch.assignment);
		else detail.clearContent();
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
	
	// ------------ widget implementations ------------
	
	// !! useful for edit-in-place
	private final class AssayHierarchyTreeCell extends TreeCell<String>
	{
        private TextField textField;
 
        public AssayHierarchyTreeCell()
        {
        }
 
        /*public void startEdit() 
        {
        	Util.writeln("EDIT START!");
            super.startEdit();
            if (textField == null) createTextField();
            setText(null);
            setGraphic(textField);
            textField.selectAll();
        }
 
        public void cancelEdit()
        {
        	Util.writeln("EDIT STOP!");
            super.cancelEdit();
            setText((String) getItem());
            setGraphic(getTreeItem().getGraphic());
        }*/
 
        public void updateItem(String text, boolean empty)
        {
            super.updateItem(text, empty);
 
            if (empty)
            {
                setText(null);
                setGraphic(null);
            }
            else 
            {
                if (isEditing()) 
                {
                    if (textField != null) textField.setText(getString());
                    setText(null);
                    setGraphic(textField);
                }
                else
                {
                    setText(getString());
                    setGraphic(getTreeItem().getGraphic());
                    openContextMenu();
                    
                    
/*
		TreeItem<String> item = treeview.getSelectionModel().getSelectedItem();
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
            
            setContextMenu(ctx);
*/
                }
            }
        }
        
        private void openContextMenu()
        {
 			TreeItem<String> item = treeview.getSelectionModel().getSelectedItem();
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
            
            setContextMenu(ctx);
        }
        
        /*private void createTextField() 
        {
            textField = new TextField(getString());
            textField.setOnKeyReleased(new EventHandler<KeyEvent>()
            {
                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        commitEdit(textField.getText());
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                }
            });  
        }*/
 
        private String getString() 
        {
            return getItem() == null ? "" : getItem().toString();
        }
    }
}

