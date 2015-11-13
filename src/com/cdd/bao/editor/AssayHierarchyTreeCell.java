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

public final class AssayHierarchyTreeCell extends TreeCell<String>
{
    private TextField textField;
 
	// ------------ private data ------------	

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
    	TreeItem<String> item = getTreeView().getSelectionModel().getSelectedItem();
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

