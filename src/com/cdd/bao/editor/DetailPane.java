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
import javafx.scene.shape.Line;
import javafx.scene.text.*;
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
	
	private TextField fieldName = null;
	private TextArea fieldDescr = null;
	private TextField fieldURI = null;
	
	private final class ValueWidgets
	{
		Lineup line;
		TextField fieldURI, fieldName, fieldDescr;
	}
	private List<ValueWidgets> valueList = new ArrayList<>();
	
	private int focusIndex = -1; // which "group of widgets" has the focus

	// ------------ public methods ------------	

	public DetailPane()
	{
		setHbarPolicy(ScrollBarPolicy.NEVER);
		setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		
		setFitToWidth(true);
		vbox.setPadding(new Insets(4));
		setContent(vbox);
		vbox.setPrefWidth(Double.MAX_VALUE);
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
		recreateGroup();
	}
	public void setAssignment(Schema.Assignment assignment)
	{
		this.assignment = assignment;
		group = null;
		recreateAssignment();
	}

	// ------------ private methods ------------	

	private void recreateGroup()
	{
		focusIndex = -1;
	
		vbox.getChildren().clear();
		
		Label heading = new Label("Category");
		heading.setTextAlignment(TextAlignment.CENTER);
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0FFC0; -fx-padding: 0.1em;");
		vbox.getChildren().add(heading);
		
		Lineup line = new Lineup(PADDING);
		
		fieldName = new TextField(group.groupName);
		fieldName.setPrefWidth(300);
		observeFocus(fieldName, -1);
		line.add(fieldName, "Name:", 1, 0);
		
		fieldDescr = new TextArea(group.groupDescr);
		fieldDescr.setPrefRowCount(5);
		fieldDescr.setPrefWidth(300);
		observeFocus(fieldDescr, -1);
		line.add(fieldDescr, "Description:", 1, 0);

		vbox.getChildren().add(line);
	}
	
	private void recreateAssignment()
	{
		focusIndex = -1;
	
		vbox.getChildren().clear();
		vbox.setFillWidth(true);
		vbox.setMaxWidth(Double.MAX_VALUE);

		Label heading = new Label("Assignment");
		heading.setTextAlignment(TextAlignment.CENTER);
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0C0FF; -fx-padding: 0.1em;");
		vbox.getChildren().add(heading);

		Lineup line = new Lineup(PADDING);
		
		fieldName = new TextField(assignment.assnName);
		fieldName.setPrefWidth(300);
		observeFocus(fieldName, -1);
		line.add(fieldName, "Name:", 1, 0);
		
		fieldDescr = new TextArea(assignment.assnDescr);
		fieldDescr.setPrefWidth(300);
		fieldDescr.setPrefRowCount(5);
		observeFocus(fieldDescr, -1);
		line.add(fieldDescr, "Description:", 1, 0);

		fieldURI = new TextField(assignment.propURI);
		fieldURI.setPrefWidth(350);
		observeFocus(fieldURI, -1);
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
			
			vw.line = new Lineup(PADDING);
			
			vw.fieldURI = new TextField(val.uri == null ? "" : val.uri);
			vw.fieldURI.setPrefWidth(350);
			observeFocus(vw.fieldURI, n);
			vw.line.add(vw.fieldURI, "URI:", 1, 0);
			
			vw.fieldName = new TextField(val.name);
			vw.fieldName.setPrefWidth(350);
			observeFocus(vw.fieldName, n);
			vw.line.add(vw.fieldName, "Name:", 1, 0);
			
			vw.fieldDescr = new TextField(val.descr);
			vw.fieldDescr.setPrefWidth(350);
			observeFocus(vw.fieldDescr, n);
			vw.line.add(vw.fieldDescr, "Description:", 1, 0);
			
			vbox.getChildren().add(vw.line);
			
			if (n < assignment.values.size() - 1)
			{
    			Line hr = new Line(0, 0, 300, 0);
    			hr.setStroke(Color.rgb(0, 0, 0, 0.1));
    			vbox.getChildren().add(hr);
			}
		}
	}

	private void observeFocus(TextInputControl field, final int idx)
	{
		field.focusedProperty().addListener(new ChangeListener<Boolean>()
		{
            public void changed(ObservableValue<? extends Boolean> val, Boolean oldValue, Boolean newValue)
            {
                if (newValue)
                {
                	if (focusIndex >= 0)
                	{
                		valueList.get(focusIndex).line.setBackground(null);
                	}
                	focusIndex = idx;
                	if (focusIndex >= 0)
                	{
                		valueList.get(focusIndex).line.setBackground(new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.05), CornerRadii.EMPTY, new Insets(-4, -4, -4, -4))));
                	}
                }
            }		
		});
	}
}

