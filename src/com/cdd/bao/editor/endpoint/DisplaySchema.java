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

import javafx.application.*;
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
	Displays the selected schema in a read-only fashion.
*/

public class DisplaySchema extends ScrollPane
{
	// ------------ private data ------------	

	private Schema schema = null;
	private Schema.Assay assay = null;
	
	private final int PADDING = 4;
	private VBox vbox = new VBox(PADDING);
	
/*	private TextField fieldPrefix = null;
	private TextField fieldName = null;
	private TextArea fieldDescr = null;
	private TextField fieldURI = null;
	private TextArea fieldPara = null;
	
	private final class ValueWidgets
	{
		Lineup line;
		Schema.Value sourceVal;
		TextField fieldURI, fieldName;
		TextArea fieldDescr;
	}
	private List<ValueWidgets> valueList = new ArrayList<>();
	
	private final class AnnotWidgets
	{
		Lineup line;
		Schema.Assignment sourceAssn;
		Schema.Annotation[] sourceAnnot;
		Button[] buttonShow;
		Button buttonAdd;
	}
	private List<AnnotWidgets> annotList = new ArrayList<>();
	
	private int focusIndex = -1; // which "group of widgets" has the focus*/

	// ------------ public methods ------------	

	public DisplaySchema()
	{
		super();
		
		setHbarPolicy(ScrollBarPolicy.NEVER);
		setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		
		setFitToWidth(true);
		vbox.setPadding(new Insets(4));
		setContent(vbox);
		vbox.setPrefWidth(Double.MAX_VALUE);
 	}

	// replaces the schema and redraws everything
	public void clearContent()
	{
		this.schema = null;
		this.assay = null;
		vbox.getChildren().clear();
	}
	public void setTemplate(Schema schema)
	{
		this.schema = schema;
		this.assay = null;
		recreateTemplate();
	}
	public void setAssay(Schema schema, Schema.Assay assay)
	{
		this.schema = schema;
		this.assay = assay;
		recreateAssay();
	}

	// ------------ private methods ------------	

	private void recreateTemplate()
	{
		vbox.getChildren().clear();

		Schema.Group root = schema.getRoot();
		
		Label heading = new Label("Template");
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #E0E0FF; -fx-padding: 0.1em 1em 0.1em 1em;");
		vbox.getChildren().add(heading);

		Lineup line = new Lineup(PADDING);
		line.setMaxWidth(Double.MAX_VALUE);
		line.add(fieldLabel(schema.getSchemaPrefix()), "Prefix:", 1, 0);
		line.add(fieldLabel(root.name), "Name:", 1, 0);

		vbox.getChildren().add(line);

		List<Schema.Group> stack = new ArrayList<>();
		stack.add(root);
		while (stack.size() > 0)
		{
			Schema.Group group = stack.remove(0);

			if (group.parent == null)
			{
				heading = new Label("Assignments");
        		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #E0E0FF; -fx-padding: 0.1em 1em 0.1em 1em;");
			}
			else
			{
    			String title = group.name;
    			for (Schema.Group look = group.parent; look.parent != null; look = look.parent) title = look.name + " \u25BA " + title;
        		heading = new Label(title);
        		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-underline: true;");
			}
    		vbox.getChildren().add(heading);

			for (Schema.Assignment assn : group.assignments)
			{
				Label label = new Label("    " + assn.name);
				label.setStyle("-fx-font-style: italic; -fx-text-fill: #404040;");
				vbox.getChildren().add(label);
			}

			stack.addAll(0, group.subGroups);
		}
	}
	
	private void recreateAssay()
	{
		vbox.getChildren().clear();

		final String STYLE_INFO = "-fx-text-fill: black; -fx-border-color: black; -fx-background-color: white; -fx-padding: 0.1em 0.1em 0.1em 0.1em;"; // looks kinda like an edit box

		Label heading = new Label("Assay");
		heading.setTextAlignment(TextAlignment.CENTER);
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0FFC0; -fx-padding: 0.1em 1em 0.1em 1em;");
		vbox.getChildren().add(heading);
		
		Lineup line = new Lineup(PADDING);
		line.setMaxWidth(Double.MAX_VALUE);
		line.add(fieldLabel(assay.name), "Name:", 1, 0);
		line.add(fieldLabel(assay.descr), "Description:", 1, 0);
		line.add(fieldLabel(assay.para), "Paragraph:", 1, 0);
		line.add(fieldLabel(assay.originURI), "Origin:", 1, 0);
		
		vbox.getChildren().add(line);

		heading = new Label("Annotations");
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0FFC0; -fx-padding: 0.1em 1em 0.1em 1em;");
		vbox.getChildren().add(heading);		
		
		if (assay.annotations.size() == 0) vbox.getChildren().add(new Label("(none)"));

		line = new Lineup(PADDING);
		line.setMaxWidth(Double.MAX_VALUE);

		// flatten & name the assignments first, to get the order		
		List<Schema.Group> stack = new ArrayList<>();
		stack.add(schema.getRoot());
		List<String> assnNames = new ArrayList<>();
		while (stack.size() > 0)
		{
			Schema.Group group = stack.remove(0);
			for (Schema.Assignment assn : group.assignments)
			{
				String name = assn.name;
    			for (Schema.Group look = assn.parent; look.parent != null; look = look.parent) name = look.name + " \u25BA " + name;
    			assnNames.add(name);
			}
			stack.addAll(0, group.subGroups);
		}
		Map<Schema.Annotation, String> annotName = new HashMap<>();
		for (Schema.Annotation annot : assay.annotations)
		{
			String name = annot.assn.name;
			for (Schema.Group look = annot.assn.parent; look.parent != null; look = look.parent) name = look.name + " \u25BA " + name;
			annotName.put(annot, name);
		}

		// order then emit the annotations
		List<Schema.Annotation> annotList = new ArrayList<>(assay.annotations);
		annotList.sort((v1, v2) ->
		{
			String name1 = annotName.get(v1), name2 = annotName.get(v2);
			int idx1 = assnNames.indexOf(name1), idx2 = assnNames.indexOf(name2);
			if (idx1 < 0) idx1 = Integer.MAX_VALUE;
			if (idx2 < 0) idx2 = Integer.MAX_VALUE;

			if (idx1 < idx2) return -1;
			if (idx1 > idx2) return 1;
			return name1.compareTo(name2);
		});

		for (Schema.Annotation annot : annotList)
		{
			Label label = annot.literal != null ? fieldLabel("\"" + annot.literal + "\"", "#FFFFC0") :
						  annot.value != null ? fieldLabel(annot.value.name, "#C0C0FF") : fieldLabel("?");
			line.add(label, annotName.get(annot) + ":", 1, 0);
		}

		vbox.getChildren().add(line);
	}
	
	// return a label that looks a bit like an editable field
	private Label fieldLabel(String txt) {return fieldLabel(txt, "white");}
	private Label fieldLabel(String txt, String bgcol)
	{
		Label label = new Label(txt);
		label.setStyle("-fx-text-fill: black; -fx-border-color: black; -fx-background-color: " + bgcol + "; -fx-padding: 0.1em 0.1em 0.1em 0.1em;");
		label.setWrapText(true);
		label.setMinWidth(50);
		label.setMaxWidth(Double.MAX_VALUE);
		return label;
	}
}

