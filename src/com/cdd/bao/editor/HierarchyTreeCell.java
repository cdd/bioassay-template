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

/*
	Display functionality for the schema tree shown on the left hand side.
*/

public final class HierarchyTreeCell extends TreeCell<EditSchema.Branch>
{
    private TextField textField;
 
	// ------------ private data ------------	

    public HierarchyTreeCell()
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
 
    public void updateItem(EditSchema.Branch branch, boolean empty)
    {
        super.updateItem(branch, empty);
 
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
				String style = "";

            	String label = branch.heading != null ? branch.heading : 
            				   branch.group != null ? branch.group.name : 
            				   branch.assignment != null ? branch.assignment.name : 
            				   branch.assay != null ? branch.assay.name : "?";

				//style += setStyle(label.length() == 0 ? "-fx-text-fill: red;" : "-fx-text-fill: black");
            	if (label.length() == 0)
            	{
            		label = "unnamed";
            		//style += "-fx-font-style: italic;"; (doesn't work, not sure why)
            		style += "-fx-text-fill: #800000;";
            	}
            	else
            	{
            		//style += "-fx-font-style: normal;";
            		//style += "-fx-text-fill: #404040;";
            	}

    			if (branch.heading != null || branch.group != null) style += "-fx-font-weight: bold;";
    			else if (branch.assignment != null) style += "-fx-font-weight: normal;";
				
				setStyle(style);
                setText(label);
                setGraphic(getTreeItem().getGraphic());
                setupContextMenu(branch);
            }
	    }
    }

    private void setupContextMenu(EditSchema.Branch branch)
    {   	
        ContextMenu ctx = new ContextMenu();

		if (branch.group != null) addMenu(ctx, "Add _Group").setOnAction(event -> branch.owner.actionGroupAdd());
		if (branch.group != null) addMenu(ctx, "Add _Assignment").setOnAction(event -> branch.owner.actionAssignmentAdd());
		if (branch.group == null && branch.assignment == null) addMenu(ctx, "Add Assa_y").setOnAction(event -> branch.owner.actionAssayAdd());
		if (branch.group != null || branch.assignment != null || branch.assay != null)
		{
			
    		addMenu(ctx, "Cu_t").setOnAction(event -> branch.owner.actionEditCopy(true));
    		addMenu(ctx, "_Copy").setOnAction(event -> branch.owner.actionEditCopy(false));
    		addMenu(ctx, "_Paste").setOnAction(event -> branch.owner.actionEditPaste());
        	addMenu(ctx, "_Delete").setOnAction(event -> branch.owner.actionEditDelete());
		}

		/* maybe add these: but need to check validity first
		addMenu(menuEdit, "Move _Up", new KeyCharacterCombination("[", cmd)).setOnAction(event -> actionEditMove(-1));
		addMenu(menuEdit, "Move _Down", new KeyCharacterCombination("]", cmd)).setOnAction(event -> actionEditMove(1));
		*/
        
        if (ctx.getItems().size() > 0) setContextMenu(ctx);
    }
    private MenuItem addMenu(ContextMenu parent, String title)
    {
    	MenuItem item = new MenuItem(title);
    	parent.getItems().add(item);
    	return item;
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

