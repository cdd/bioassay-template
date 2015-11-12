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
import javafx.scene.text.TextAlignment;
import javafx.beans.value.*;
import javafx.util.*;

/*
	Detail: shows an object from the schema, and makes it editable.
*/

public class DetailPane extends ScrollPane
{
	// ------------ private data ------------	

	private Schema.Group group = null;
	private Schema.Assignment assignment = null;

	private final int PADDING = 4;
	private VBox vbox = new VBox(PADDING);
	
	private TextField fieldName = null, fieldDescr = null, fieldURI = null;
	
	private final class ValueWidgets
	{
		TextField fieldURI;
	}
	private List<ValueWidgets> valueList = new ArrayList<>();

	// ------------ public methods ------------	

	public DetailPane()
	{
		setHbarPolicy(ScrollBarPolicy.NEVER);
		setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		
		vbox.setPadding(new Insets(4));
		setContent(vbox);
		//vbox.setPrefWidth(Double.MAX_VALUE);
 	}

	public void clearContent()
	{
		group = null;
		assignment = null;
		vbox.getChildren().clear();
	}
	public void setGroup(Schema.Group group)
	{
		this.group = group;
		assignment = null;
		recreateCategory();
	}
	public void setAssignment(Schema.Assignment assignment)
	{
		this.assignment = assignment;
		group = null;
		recreateAssignment();
	}

	// ------------ private methods ------------	

	private void recreateCategory()
	{
		vbox.getChildren().clear();
		
		Label heading = new Label("Category");
		heading.setTextAlignment(TextAlignment.CENTER);
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0FFC0; -fx-padding: 0.1em;");
		vbox.getChildren().add(heading);
		
		Lineup line = new Lineup(PADDING);
		
		fieldName = new TextField(group.groupName);
		fieldName.setPrefWidth(300);
		line.add(fieldName, "Name:", 1, 0);
		
		fieldDescr = new TextField(group.groupDescr);
		fieldDescr.setPrefWidth(300);
		line.add(fieldDescr, "Description:", 1, 0);

		vbox.getChildren().add(line);
	}
	
	private void recreateAssignment()
	{
		vbox.getChildren().clear();

		Label heading = new Label("Assignment");
		heading.setTextAlignment(TextAlignment.CENTER);
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0C0FF; -fx-padding: 0.1em;");
		vbox.getChildren().add(heading);

		Lineup line = new Lineup(PADDING);
		
		fieldName = new TextField(assignment.assnName);
		fieldName.setPrefWidth(300);
		line.add(fieldName, "Name:", 1, 0);
		
		fieldDescr = new TextField(assignment.assnDescr);
		fieldDescr.setPrefWidth(300);
		line.add(fieldDescr, "Description:", 1, 0);

		fieldURI = new TextField(assignment.propURI);
		fieldURI.setPrefWidth(350);
		line.add(fieldURI, "URI:", 1, 0);

		vbox.getChildren().add(line);
		
		heading = new Label("Values");
		heading.setTextAlignment(TextAlignment.CENTER);
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0FFC0; -fx-padding: 0.1em;");
		vbox.getChildren().add(heading);		

		/*Vocabulary vocab = null;
		try {vocab = Vocabulary.globalInstance();}
		catch (IOException ex) {ex.printStackTrace(); return;}*/
		
		valueList.clear();
		for (int n = 0; n < assignment.values.size(); n++)
		{
			Schema.Value val = assignment.values.get(n);
		
			//final int idx = n;
			ValueWidgets vw = new ValueWidgets();
			valueList.add(vw);
			
			vw.fieldURI = new TextField(val.uri == null ? "" : val.uri);
			vw.fieldURI.setPrefWidth(350);
			
			Label label = new Label(val.name);
			// (doesn't work) label.setStyle("-fx-font-style: italic;");

			FlowPane flow = new FlowPane(Orientation.HORIZONTAL);
			flow.getChildren().addAll
			(
				new Label("URI: "),
				vw.fieldURI,
				label
			);
			vbox.getChildren().add(flow);
			
			// !! DESCRIPTION
			// !! LINEUP
		}
	}
}

