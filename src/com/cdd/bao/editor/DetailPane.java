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

	private Schema.Group group = null;
	private Schema.Assignment assignment = null;
	private Schema.Assay assay = null;

	private final int PADDING = 4;
	private VBox vbox = new VBox(PADDING);
	
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
		group = null;
		assignment = null;
		assay = null;
		vbox.getChildren().clear();
	}
	
	// replaces the content with either type; any modifications to existing widgets will be zapped
	public void setGroup(Schema.Group group)
	{
		this.group = group;
		assignment = null;
		assay = null;
		recreateGroup();
	}
	public void setAssignment(Schema.Assignment assignment)
	{
		this.assignment = assignment;
		group = null;
		assay = null;
		recreateAssignment();
	}
	public void setAssay(Schema.Assay assay)
	{
		this.assay = assay;
		assignment = null;
		group = null;
		recreateAssay();
	}
	
	// inquiries as to what kind of content is currently being represented
	public boolean isGroup() {return group != null;}
	public boolean isAssignment() {return assignment != null;}
	
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
		
		// !! TODO: pull in the annotations
		
		if (assay.equals(mod)) return null;
		return mod;
	}

	// menu-driven action events
    public void actionValueAdd()
    {
    	if (assignment == null) return;	
    	
    	Schema.Assignment modified = extractAssignment();
    	if (modified != null) assignment = modified;

    	assignment.values.add(new Schema.Value("", ""));
    	recreateAssignment();

    	valueList.get(valueList.size() - 1).fieldURI.requestFocus();

		// scroll to end; have to pause first, though    	
        Platform.runLater(() -> setVvalue(getVmax()));
    }
    public void actionValueDelete()
    {
    	if (assignment == null || focusIndex < 0) return;	

    	Schema.Assignment modified = extractAssignment();
    	if (modified != null) assignment = modified;

		int idx = focusIndex;
    	assignment.values.remove(focusIndex);
    	recreateAssignment();
    	
    	if (valueList.size() > 0)
    		valueList.get(Math.min(idx, valueList.size() - 1)).fieldURI.requestFocus();
    	else
    		fieldName.requestFocus();
    }
    public void actionValueMove(int dir)
    {
    	if (assignment == null || focusIndex < 0) return;	

    	Schema.Assignment modified = extractAssignment();
    	if (modified != null) assignment = modified;
    	if (focusIndex + dir < 0 || focusIndex + dir >= assignment.values.size()) return;

		int newIndex = focusIndex + dir;
		Schema.Value v1 = assignment.values.get(focusIndex), v2 = assignment.values.get(newIndex);
		assignment.values.set(focusIndex, v2);
		assignment.values.set(newIndex, v1);
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
    		LookupPanel lookup = new LookupPanel(searchText);
    		Optional<LookupPanel.Resource> result = lookup.showAndWait();
    		if (result.isPresent())
    		{
    			LookupPanel.Resource res = result.get();
    			if (res != null)
    			{
    				vw.fieldURI.setText(res.uri);
    				vw.fieldName.setText(res.label);
    				vw.fieldDescr.setText(res.descr);
				}
    		}
    	}
    	else if (assignment != null)
    	{
    		LookupPanel lookup = new LookupPanel(fieldName.getText());
    		Optional<LookupPanel.Resource> result = lookup.showAndWait();
    		if (result.isPresent())
    		{
    			LookupPanel.Resource res = result.get();
    			if (res != null)
    			{
    				fieldURI.setText(res.uri);
    				fieldName.setText(res.label);
    				fieldDescr.setText(res.descr);
				}
    		}
    	}
    }

	// ------------ private methods ------------	

	private void recreateGroup()
	{
		focusIndex = -1;
	
		vbox.getChildren().clear();
		
		Label heading = new Label("Group");
		heading.setTextAlignment(TextAlignment.CENTER);
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0A0FF; -fx-padding: 0.1em;");
		vbox.getChildren().add(heading);
		
		Lineup line = new Lineup(PADDING);
		
		fieldName = new TextField(group.name);
		fieldName.setPrefWidth(300);
		observeFocus(fieldName, -1);
		line.add(fieldName, "Name:", 1, 0);
		
		fieldDescr = new TextArea(group.descr);
		fieldDescr.setPrefRowCount(5);
		fieldDescr.setPrefWidth(300);
		fieldDescr.setWrapText(true);
		observeFocus(fieldDescr, -1);
		passthroughTab(fieldDescr);
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
		
		fieldName = new TextField(assignment.name);
		fieldName.setPrefWidth(300);
		observeFocus(fieldName, -1);
		line.add(fieldName, "Name:", 1, 0);
		
		fieldDescr = new TextArea(assignment.descr);
		fieldDescr.setPrefWidth(300);
		fieldDescr.setPrefRowCount(5);
		fieldDescr.setWrapText(true);
		observeFocus(fieldDescr, -1);
		passthroughTab(fieldDescr);
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
			vw.line.add(vw.fieldURI, "URI:", 1, 0);
			
			vw.fieldName = new TextField(val.name);
			vw.fieldName.setPrefWidth(350);
			observeFocus(vw.fieldName, n);
			vw.line.add(vw.fieldName, "Name:", 1, 0);
			
			vw.fieldDescr = new TextArea(val.descr);
			vw.fieldDescr.setPrefRowCount(5);
			vw.fieldDescr.setPrefWidth(350);
			vw.fieldDescr.setWrapText(true);
			observeFocus(vw.fieldDescr, n);
			passthroughTab(vw.fieldDescr);
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
		heading.setTextAlignment(TextAlignment.CENTER);
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #B0E0E0; -fx-padding: 0.1em;");
		vbox.getChildren().add(heading);

		Lineup line = new Lineup(PADDING);
		
		fieldName = new TextField(assay.name);
		fieldName.setPrefWidth(300);
		observeFocus(fieldName, -1);
		line.add(fieldName, "Name:", 1, 0);
		
		fieldDescr = new TextArea(assay.descr);
		fieldDescr.setPrefWidth(300);
		fieldDescr.setPrefRowCount(5);
		fieldDescr.setWrapText(true);
		observeFocus(fieldDescr, -1);
		passthroughTab(fieldDescr);
		line.add(fieldDescr, "Description:", 1, 0);

		fieldPara = new TextArea(assay.para);
		fieldPara.setPrefWidth(300);
		fieldPara.setPrefRowCount(5);
		fieldPara.setWrapText(true);
		observeFocus(fieldPara, -1);
		passthroughTab(fieldPara);
		line.add(fieldPara, "Paragraph:", 1, 0);

		vbox.getChildren().add(line);

		// and the annotations
		//TODO
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
}

