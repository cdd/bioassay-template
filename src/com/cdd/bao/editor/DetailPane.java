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
	Detail: shows an object from the schema, and makes it editable.
*/

public class DetailPane extends ScrollPane
{
	// ------------ private data ------------	

	private EditSchema main;

	private Schema schema = null;
	private Schema.Group group = null;
	private Schema.Assignment assignment = null;
	private Schema.Assay assay = null;

	private final int PADDING = 4;
	private VBox vbox = new VBox(PADDING);
	
	private TextField fieldPrefix = null;
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
		Schema.Annotation sourceAnnot;
		Button buttonShow;
	}
	private List<AnnotWidgets> annotList = new ArrayList<>();
	
	private int focusIndex = -1; // which "group of widgets" has the focus

	// ------------ public methods ------------	

	public DetailPane(EditSchema main)
	{
		super();
		
		this.main = main;
		
		setHbarPolicy(ScrollBarPolicy.NEVER);
		setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		
		setFitToWidth(true);
		vbox.setPadding(new Insets(4));
		setContent(vbox);
		vbox.setPrefWidth(Double.MAX_VALUE);
 	}

	// clears out the contents of the pane; does so without regard for saving the content
	public void clearContent()
	{
		schema = null;
		group = null;
		assignment = null;
		assay = null;
		vbox.getChildren().clear();
	}
	
	// replaces the content with either type; any modifications to existing widgets will be zapped
	public void setGroup(Schema schema, Schema.Group group, boolean recreate)
	{
		this.schema = schema;
		this.group = group;
		assignment = null;
		assay = null;
		if (recreate) recreateGroup();
	}
	public void setAssignment(Schema schema, Schema.Assignment assignment, boolean recreate)
	{
		this.schema = schema;
		this.assignment = assignment;
		group = null;
		assay = null;
		if (recreate) recreateAssignment();
	}
	public void setAssay(Schema schema, Schema.Assay assay, boolean recreate)
	{
		this.schema = schema;
		this.assay = assay;
		assignment = null;
		group = null;
		if (recreate) recreateAssay();
	}
	
	// inquiries as to what kind of content is currently being represented
	public boolean isGroup() {return group != null;}
	public boolean isAssignment() {return assignment != null;}
	public boolean isAssay() {return assay != null;}
	
	// fetches the current version of the editing prefix (unlike below, does not check for changed)
	public String extractPrefix()
	{
		return group == null || group.parent != null ? null : fieldPrefix.getText();
	}

	// extracts a representation of the content from the current widgets; note that these return null if nothing has changed (or if it's not that content type)
	public Schema.Group extractGroup()
	{
		if (group == null) return null;
	
		if (group.name.equals(fieldName.getText()) && group.descr.equals(fieldDescr.getText())) return null;
		
		Schema.Group mod = group.clone(null); // duplicates all of the subordinate content (not currently editing this, so stays unchanged)
		mod.name = fieldName.getText();
		mod.descr = fieldDescr.getText();
		return mod;
	}
	public Schema.Assignment extractAssignment()
	{
		if (assignment == null) return null;
		
		Schema.Assignment mod = new Schema.Assignment(null, fieldName.getText(), fieldURI.getText());
		mod.descr = fieldDescr.getText();
		
		for (ValueWidgets vw : valueList)
		{
			Schema.Value val = new Schema.Value(vw.fieldURI.getText(), vw.fieldName.getText());
			val.descr = vw.fieldDescr.getText();
			mod.values.add(val);
		}
		
		if (assignment.equals(mod)) return null;
		return mod;
	}
	public Schema.Assay extractAssay()
	{
		if (assay == null) return null;
		
		Schema.Assay mod = new Schema.Assay(fieldName.getText());
		mod.descr = fieldDescr.getText();
		mod.para = fieldPara.getText();
		
		for (AnnotWidgets aw : annotList) if (aw.sourceAnnot != null) mod.annotations.add(aw.sourceAnnot.clone());

		if (assay.equals(mod)) return null;
		
		assay = mod; // (makes sure that the next call will claim nothing happened; is this valid?)
		
		return mod;
	}

	// menu-driven action events
    public void actionValueAdd()
    {
    	if (assignment == null) return;	
    	
    	Schema.Assignment modAssn = extractAssignment();
    	if (modAssn == null) modAssn = assignment;

    	modAssn.values.add(new Schema.Value("", ""));

    	main.updateBranchAssignment(modAssn);
    	recreateAssignment();

    	valueList.get(valueList.size() - 1).fieldURI.requestFocus();

		// scroll to end; have to pause first, though    	
        Platform.runLater(() -> setVvalue(getVmax()));
    }
    public void actionValueMultiAdd()
    {
    	if (assignment == null) return;

		LookupPanel lookup = new LookupPanel("", listValueURI(), true);
		Optional<LookupPanel.Resource[]> result = lookup.showAndWait();
		if (!result.isPresent()) return;
		LookupPanel.Resource[] resList = result.get();
		if (resList == null || resList.length == 0) return;

    	Schema.Assignment modAssn = extractAssignment();
    	if (modAssn == null) modAssn = assignment;

		int watermark = modAssn.values.size();
		for (LookupPanel.Resource res : resList)
		{
			Schema.Value val = new Schema.Value(res.uri, res.label);
			val.descr = res.descr;
    		modAssn.values.add(val);
		}
		
    	main.updateBranchAssignment(modAssn);
    	recreateAssignment();

    	valueList.get(watermark).fieldURI.requestFocus();

		// scroll to end; have to pause first, though    	
        Platform.runLater(() -> setVvalue(getVmax()));
    }
    
    public void actionValueDelete()
    {
    	if (assignment == null || focusIndex < 0) return;	

    	Schema.Assignment modAssn = extractAssignment();
    	if (modAssn == null) modAssn = assignment;

		int idx = focusIndex;
    	modAssn.values.remove(focusIndex);

    	main.updateBranchAssignment(modAssn);
    	recreateAssignment();
    	
    	if (valueList.size() > 0)
    		valueList.get(Math.min(idx, valueList.size() - 1)).fieldURI.requestFocus();
    	else
    		fieldName.requestFocus();
    }
    public void actionValueMove(int dir)
    {
    	if (assignment == null || focusIndex < 0) return;	

    	Schema.Assignment modAssn = extractAssignment();
    	if (modAssn == null) modAssn = assignment;

    	if (focusIndex + dir < 0 || focusIndex + dir >= modAssn.values.size()) return;

		int newIndex = focusIndex + dir;
		Schema.Value v1 = modAssn.values.get(focusIndex), v2 = modAssn.values.get(newIndex);
		modAssn.values.set(focusIndex, v2);
		modAssn.values.set(newIndex, v1);

    	main.updateBranchAssignment(modAssn);
    	recreateAssignment();
    	
   		valueList.get(newIndex).fieldURI.requestFocus();
    }
    public void actionLookupURI()
    {
    	if (focusIndex < 0) return;
    	ValueWidgets vw = valueList.get(focusIndex);
    	String uri = vw.fieldURI.getText();
    	if (uri.length() == 0) {actionLookupName(); return;}
    
		Vocabulary vocab = null;
		try {vocab = Vocabulary.globalInstance();}
		catch (IOException ex) {ex.printStackTrace(); return;}
		
		String label = vocab.getLabel(uri), descr = vocab.getDescr(uri);
		if (label == null) {actionLookupName(); return;}
		if (vw.fieldName.getText().length() == 0) vw.fieldName.setText(label);
		if (descr != null && vw.fieldDescr.getText().length() == 0) vw.fieldDescr.setText(descr);
    }
    public void actionLookupName()
    {
    	if (focusIndex >= 0)
    	{
    		ValueWidgets vw = valueList.get(focusIndex);
    		String searchText = vw.fieldName.getText().length() > 0 ? vw.fieldName.getText() : vw.fieldURI.getText();
    		LookupPanel lookup = new LookupPanel(searchText, listValueURI(), false);
    		Optional<LookupPanel.Resource[]> result = lookup.showAndWait();
    		if (result.isPresent())
    		{
    			LookupPanel.Resource[] res = result.get();
    			if (res != null && res.length > 0)
    			{
    				vw.fieldURI.setText(res[0].uri);
    				vw.fieldName.setText(res[0].label);
    				vw.fieldDescr.setText(res[0].descr);
				}
    		}
    	}
    	else if (assignment != null)
    	{
    		LookupPanel lookup = new LookupPanel(fieldName.getText(), new HashSet<>(), false);
    		Optional<LookupPanel.Resource[]> result = lookup.showAndWait();
    		if (result.isPresent())
    		{
    			LookupPanel.Resource[] res = result.get();
    			if (res != null && res.length >= 1)
    			{
    				fieldURI.setText(res[0].uri);
    				fieldName.setText(res[0].label);
    				fieldDescr.setText(res[0].descr);
				}
    		}
    	}
    }

	// ------------ private methods ------------	

	private void recreateGroup()
	{
		focusIndex = -1;
	
		vbox.getChildren().clear();
		
		if (group.parent == null)
		{
    		Label heading = new Label("Root");
    		heading.setTextAlignment(TextAlignment.CENTER);
    		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0C0FF; -fx-padding: 0.1em 1em 0.1em 1em;");
    		vbox.getChildren().add(heading);
    		
    		Lineup line = new Lineup(PADDING);
    		line.setMaxWidth(Double.MAX_VALUE);
    		
    		Label notes = new Label(
    			"The schema prefix is a URI stem, which is used as the basis for naming all of the objects that are used by the schema. For loading and saving files, " +
    			"its value does not matter, but for publishing on the semantic web, it is essential to select a unique namespace.");
    		notes.setWrapText(true);
    		notes.setPrefWidth(300);
    		notes.setMaxWidth(Double.MAX_VALUE);
    		line.add(notes, null, 1, 0, Lineup.NOINDENT);
    
    		fieldPrefix = new TextField(schema.getSchemaPrefix());
    		fieldPrefix.setPrefWidth(300);
    		line.add(fieldPrefix, "Prefix:", 1, 0);

    		vbox.getChildren().add(line);
		}
		
		Label heading = new Label("Group");
		heading.setTextAlignment(TextAlignment.CENTER);
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0A0FF; -fx-padding: 0.1em 1em 0.1em 1em;");
		vbox.getChildren().add(heading);
		
		Lineup line = new Lineup(PADDING);
		
		fieldName = new TextField(group.name);
		fieldName.setPrefWidth(300);
		observeFocus(fieldName, -1);
		Tooltip.install(fieldName, new Tooltip("Very short name for the group"));
		line.add(fieldName, "Name:", 1, 0);
		
		fieldDescr = new TextArea(group.descr);
		fieldDescr.setPrefRowCount(5);
		fieldDescr.setPrefWidth(300);
		fieldDescr.setWrapText(true);
		observeFocus(fieldDescr, -1);
		passthroughTab(fieldDescr);
		Tooltip.install(fieldDescr, new Tooltip("Concise paragraph explaining what the group represents"));
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
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0C0FF; -fx-padding: 0.1em 1em 0.1em 1em;");
		vbox.getChildren().add(heading);

		Lineup line = new Lineup(PADDING);
		
		fieldName = new TextField(assignment.name);
		fieldName.setPrefWidth(300);
		observeFocus(fieldName, -1);
		Tooltip.install(fieldName, new Tooltip("Very short name for the assignment"));
		line.add(fieldName, "Name:", 1, 0);
		
		fieldDescr = new TextArea(assignment.descr);
		fieldDescr.setPrefWidth(300);
		fieldDescr.setPrefRowCount(5);
		fieldDescr.setWrapText(true);
		observeFocus(fieldDescr, -1);
		passthroughTab(fieldDescr);
		Tooltip.install(fieldDescr, new Tooltip("Concise paragraph describing the assignment to the user"));
		line.add(fieldDescr, "Description:", 1, 0);

		fieldURI = new TextField(assignment.propURI);
		fieldURI.setPrefWidth(350);
		observeFocus(fieldURI, -1);
		Tooltip.install(fieldURI, new Tooltip("The property URI used to link the assay to the assignment"));
		line.add(fieldURI, "URI:", 1, 0);

		vbox.getChildren().add(line);
		
		heading = new Label("Values");
		heading.setTextAlignment(TextAlignment.CENTER);
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0FFC0; -fx-padding: 0.1em 1em 0.1em 1em;");
		vbox.getChildren().add(heading);		

		valueList.clear();
		for (int n = 0; n < assignment.values.size(); n++)
		{
			Schema.Value val = assignment.values.get(n);
		
			//final int idx = n;
			ValueWidgets vw = new ValueWidgets();
			vw.sourceVal = val;
			valueList.add(vw);
			
			vw.line = new Lineup(PADDING);
			
			vw.fieldURI = new TextField(val.uri == null ? "" : val.uri);
			vw.fieldURI.setPrefWidth(350);
			observeFocus(vw.fieldURI, n);
			Tooltip.install(vw.fieldURI, new Tooltip("The URI for this assignment value"));
			vw.line.add(vw.fieldURI, "URI:", 1, 0);
			
			vw.fieldName = new TextField(val.name);
			vw.fieldName.setPrefWidth(350);
			observeFocus(vw.fieldName, n);
			Tooltip.install(vw.fieldName, new Tooltip("Very short label for the assignment value"));
			vw.line.add(vw.fieldName, "Name:", 1, 0);
			
			vw.fieldDescr = new TextArea(val.descr);
			vw.fieldDescr.setPrefRowCount(5);
			vw.fieldDescr.setPrefWidth(350);
			vw.fieldDescr.setWrapText(true);
			observeFocus(vw.fieldDescr, n);
			passthroughTab(vw.fieldDescr);
			Tooltip.install(vw.fieldDescr, new Tooltip("Concise paragraph describing the value to the user"));
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

	private void recreateAssay()
	{
		focusIndex = -1;
	
		vbox.getChildren().clear();
		vbox.setFillWidth(true);
		vbox.setMaxWidth(Double.MAX_VALUE);
		
		Label heading = new Label("Assay");
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #B0E0E0; -fx-padding: 0.1em 1em 0.1em 1em;");
		vbox.getChildren().add(heading);

		Lineup line = new Lineup(PADDING);
		
		fieldName = new TextField(assay.name);
		fieldName.setPrefWidth(300);
		observeFocus(fieldName, -1);
		Tooltip.install(fieldName, new Tooltip("Very short label for the assay being annotated"));
		line.add(fieldName, "Name:", 1, 0);
		
		fieldDescr = new TextArea(assay.descr);
		fieldDescr.setPrefWidth(300);
		fieldDescr.setPrefRowCount(5);
		fieldDescr.setWrapText(true);
		observeFocus(fieldDescr, -1);
		passthroughTab(fieldDescr);
		Tooltip.install(fieldDescr, new Tooltip("Concise paragraph summarizing the assay"));
		line.add(fieldDescr, "Description:", 1, 0);

		fieldPara = new TextArea(assay.para);
		fieldPara.setPrefWidth(300);
		fieldPara.setPrefRowCount(5);
		fieldPara.setWrapText(true);
		observeFocus(fieldPara, -1);
		passthroughTab(fieldPara);
		Tooltip.install(fieldPara, new Tooltip("Optional detailed text for the assay, typically imported"));
		line.add(fieldPara, "Paragraph:", 1, 0);
		
		fieldURI = new TextField(assay.originURI);
		fieldURI.setPrefWidth(300);
		observeFocus(fieldURI, -1);
		Tooltip.install(fieldURI, new Tooltip("Optional URI referencing the origin of the assay"));
		line.add(fieldURI, "Origin URI:", 1, 0);

		vbox.getChildren().add(line);

		// annotations

		heading = new Label("Annotations");
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #FFFFD0; -fx-padding: 0.1em 1em 0.1em 1em;");
		vbox.getChildren().add(heading);

		List<Schema.Annotation> orphans = new ArrayList<>(assay.annotations);
		List<Schema.Group> groupList = new ArrayList<>();
		groupList.add(schema.getRoot());
		
		// recursively add all known assignments, in depth order of groups
		annotList.clear();
		while (groupList.size() > 0)
		{
			Schema.Group group = groupList.remove(0);
			int indent = 0;
			String indstr = "";
			for (Schema.Group p = group.parent; p != null; p = p.parent) {indent++; indstr += "  ";}

			if (group.parent != null) 
			{
				heading = new Label(indstr + group.name);
				heading.setStyle("-fx-font-weight: bold;");				
				vbox.getChildren().add(heading);
			}
			for (Schema.Assignment assn : group.assignments)
			{
				// insert any annotations that match this assignment; more than one is a possibility that will be reflected (though not necessarily valid); if none
				// were found, manufacture the unassigned state
				String title = indstr + assn.name + ":";
				boolean anything = false;
				for (int n = 0; n < orphans.size(); n++)
				{
					Schema.Annotation annot = orphans.get(n);
					if (schema.matchAnnotation(annot, assn))
					{
						anything = true;
						appendAnnotationWidget(title, assn, annot);
						title = "";
						orphans.remove(n);
						n--;
					}
				}
				if (!anything) appendAnnotationWidget(title, assn, null);
			}
			for (int n = group.subGroups.size() - 1; n >= 0; n--) groupList.add(0, group.subGroups.get(n));
		}
		
		// if there are any annotations left over, they are orphans
		if (orphans.size() > 0)
		{
    		heading = new Label("Orphans");
    		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: #800000;");
    		vbox.getChildren().add(heading);

			for (Schema.Annotation annot : orphans)
			{
				String title = annot.assn.name + ":";
				for (Schema.Group p = annot.assn.parent; p != null; p = p.parent) title = p.name + " / " + title;
				
				appendAnnotationWidget(title, null, annot);
			}
		}
	}

	// creates a single annotation entry line; the assignment or annotation may be blank, not not both
	private void appendAnnotationWidget(String title, Schema.Assignment assn, Schema.Annotation annot)
	{
		AnnotWidgets aw = new AnnotWidgets();
		aw.line = new Lineup(PADDING);
		aw.sourceAssn = assn;
		aw.sourceAnnot = annot;
		aw.buttonShow = new Button();
		
		updateAnnotationButton(aw);
		
		aw.buttonShow.setPrefWidth(300);
		aw.buttonShow.setMaxWidth(Double.MAX_VALUE);
		//observeFocus(fieldName, -1);
		aw.line.add(aw.buttonShow, title, 1, 0);
				
		vbox.getChildren().add(aw.line);
		
		aw.buttonShow.setOnAction(event -> pressedAnnotationButton(aw));
		
		annotList.add(aw);
	}
	
	// configures the button content and theme for an annotation
	private void updateAnnotationButton(AnnotWidgets aw)
	{
		Schema.Annotation annot = aw.sourceAnnot;
		if (annot == null)
		{
			// blank value: waiting for the user to pick something
			aw.buttonShow.setText("?");
			aw.buttonShow.setStyle("-fx-base: #F0F0FF;");
		}
		else if (annot.value != null)
		{
			aw.buttonShow.setText(annot.value.name);
			aw.buttonShow.setStyle("-fx-base: #000080; -fx-text-fill: white;");
		}
		else // annot.literal != null
		{
			aw.buttonShow.setText('"' + annot.literal + '"');
			aw.buttonShow.setStyle("-fx-base: #FFFFD0;");
		}

		// it's an orphan: mark it noticeably
		if (aw.sourceAssn == null)
		{
			// orphan
			aw.buttonShow.setStyle("-fx-base: #C00000; -fx-text-fill: white;");
		}
		
	}
	
	// responds to the pressing of an annotation button: typically brings up the edit panel
	private void pressedAnnotationButton(AnnotWidgets aw)
	{
		// orphan annotations: clicking deletes
		if (aw.sourceAssn == null)
		{
			vbox.getChildren().remove(aw.line);
			annotList.remove(aw);
			return;
		}

		// bring up panel for assignment selection
		AnnotatePanel lookup = new AnnotatePanel(aw.sourceAssn, aw.sourceAnnot);
		Optional<Schema.Annotation> result = lookup.showAndWait();
		if (result.isPresent())
		{
			Schema.Annotation res = result.get();
			if (res != null)
			{
				if (res.assn == null)
					aw.sourceAnnot = null;
				else
					aw.sourceAnnot = res;

				updateAnnotationButton(aw);
			}
		}
	}

	// respond to focus so that one of the blocks gets a highlight
	private void observeFocus(TextInputControl field, final int idx)
	{
		field.focusedProperty().addListener((val, oldValue, newValue) -> 
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
		});
	}
	
	// modify a TextArea so that it doesn't steal the Tab key; pretty ugly for a feature that should be just a switch, but it is what it is
	private void passthroughTab(final TextArea area)
	{
        area.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> 
        {
            if (event.getCode() == KeyCode.TAB) 
            {
            	if (event.isControlDown()) area.replaceSelection("\t");
            	else
            	{
            		List<Control> list = new ArrayList<>();
            		list.add(main.getTreeView());
            		recursiveControls(list, vbox);
            		int idx = list.indexOf(area);
            		if (idx >= 0)
            		{
            			if (event.isShiftDown()) idx--; else idx++;
            			if (idx < 0) idx = list.size() + idx;
            			if (idx >= list.size()) idx -= list.size();
            			list.get(idx).requestFocus();
            		}
                }
                event.consume();
            }  
        });  	
	}
	
	// returns a flattened list of all child nodes in the tree that are traversible
	private void recursiveControls(List<Control> list, Pane parent)
	{
		for (Node child : parent.getChildrenUnmodifiable())
		{
			if (child instanceof Control)
			{
				Control control = (Control)child;
				if (control.isFocusTraversable()) list.add(control);
			}
			if (child instanceof Pane) recursiveControls(list, (Pane)child);
		}
	}
	
	// make a list of the URIs currently being used for values
	private Set<String> listValueURI()
	{
		Set<String> used = new HashSet<>();
		for (ValueWidgets vw : valueList)
		{
			String uri = vw.fieldURI.getText();
			if (uri.length() > 0) used.add(uri);
		}
		return used;
	}
}

