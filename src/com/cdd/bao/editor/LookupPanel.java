/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.editor;

import com.cdd.bao.template.*;

import java.io.*;
import java.util.*;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

/*
	Lookup panel: takes a partially specified schema value and opens up the vocabulary list, to make it easy to pick URI/label/description
	combinations.
*/

public class LookupPanel extends Dialog<Schema.Value>
{
	

	// ------------ public methods ------------

	public LookupPanel(Schema.Value value)
	{
		super();
		
		setTitle("Lookup URI");

/*		
ButtonType buttonTypeOk = new ButtonType("Okay", ButtonData.OK_DONE);
2
dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
*/
		getDialogPane().getButtonTypes().add(new ButtonType("Cancel", ButtonData.CANCEL_CLOSE));
		
		ButtonType btnUse = new ButtonType("Use", ButtonData.OK_DONE);
		getDialogPane().getButtonTypes().add(btnUse);
		setResultConverter((ButtonType b) ->
		{
			Schema.Value val = new Schema.Value("foo", "bar");
			//descr
			return val;
		});
	}

	
	// ------------ private methods ------------
	
}
