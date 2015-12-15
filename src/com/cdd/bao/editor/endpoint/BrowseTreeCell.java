/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.editor.endpoint;

import com.cdd.bao.*;
import com.cdd.bao.template.*;
import com.cdd.bao.util.*;

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
	Display functionality for showing templates & assays fetched from the SPARQL endpoint.
*/

public final class BrowseTreeCell extends TreeCell<BrowseEndpoint.Branch>
{
	// ------------ private data ------------	

    public BrowseTreeCell()
    {
    }
 
    public void updateItem(BrowseEndpoint.Branch branch, boolean empty)
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
            }
            else
            {
				String label = "?", style = "";
				
				if (branch.assay == null)
				{
					label = branch.schema.getRoot().name;
					style = "-fx-font-weight: bold; -fx-text-fill: #000000;";
				}
				else
				{
					label = branch.assay.name;
					style = "-fx-font-weight: normal; -fx-text-fill: #404040;";
				}

				setStyle(style);
                setText(label);
                setGraphic(getTreeItem().getGraphic());
                setupContextMenu(branch);
            }
	    }
    }

    private void setupContextMenu(BrowseEndpoint.Branch branch)
    {
        ContextMenu ctx = new ContextMenu();

		if (branch.schema != null && branch.assay == null)
		{
    		addMenu(ctx, "_Open").setOnAction(event -> branch.owner.actionOpen());
		}
		else if (branch.assay != null)
		{
    		addMenu(ctx, "_Copy").setOnAction(event -> branch.owner.actionCopy());
		}

        if (ctx.getItems().size() > 0) setContextMenu(ctx);
    }
    private MenuItem addMenu(ContextMenu parent, String title)
    {
    	MenuItem item = new MenuItem(title);
    	parent.getItems().add(item);
    	return item;
    }

	private String getString() 
    {
        return getItem() == null ? "" : getItem().toString();
    }
}

