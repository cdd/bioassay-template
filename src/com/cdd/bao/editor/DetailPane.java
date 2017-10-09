/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2017 Collaborative Drug Discovery Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2.0
 * as published by the Free Software Foundation:
 * 
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.cdd.bao.editor;

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

	private boolean isSummaryView;

	private final int PADDING = 4;
	private VBox vbox = new VBox(PADDING);
	
	private TextField fieldPrefix = null;
	private TextField fieldName = null;
	private TextArea fieldDescr = null;
	private TextField fieldURI = null;
	private TextArea fieldPara = null;
	private RadioButton suggestionsFull = null, suggestionsDisabled = null, suggestionsField = null;
	private RadioButton suggestionsURL = null, suggestionsID = null;
	private RadioButton suggestionsString = null, suggestionsNumber = null, suggestionsInteger = null;
	
	private final class ValueWidgets
	{
		Lineup line;
		Schema.Value sourceVal;
		TextField fieldURI, fieldName;
		ComboBox<String> dropSpec;
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
	
	// summary toggle: displays how views are displayed
	public boolean getSummaryView() {return isSummaryView;}
	public void setSummaryView(boolean flag)
	{
		isSummaryView = flag;
		if (isAssignment()) recreateAssignment();
	}
	
	// fetches the current version of the editing prefix (unlike below, does not check for changed)
	public String extractPrefix()
	{
		return group == null || group.parent != null ? null : fieldPrefix.getText();
	}

	// extracts a representation of the content from the current widgets; note that these return null if nothing has changed (or if it's not that content type)
	public Schema.Group extractGroup()
	{
		if (group == null) return null;
	
		if (group.name.equals(fieldName.getText()) && group.descr.equals(fieldDescr.getText()) && 
			group.groupURI.equals(fieldURI == null ? "" : ModelSchema.expandPrefix(fieldURI.getText()))) return null;
		
		Schema.Group mod = group.clone(null); // duplicates all of the subordinate content (not currently editing this, so stays unchanged)
		mod.name = fieldName.getText();
		mod.descr = fieldDescr.getText();
		mod.groupURI = ModelSchema.expandPrefix(fieldURI.getText());
		return mod;
	}
	public Schema.Assignment extractAssignment()
	{
		if (assignment == null) return null;
		
		Schema.Assignment mod = new Schema.Assignment(null, fieldName.getText(), ModelSchema.expandPrefix(fieldURI.getText()));
		mod.descr = fieldDescr.getText();
		if (suggestionsFull.isSelected()) mod.suggestions = Schema.Suggestions.FULL;
		else if (suggestionsDisabled.isSelected()) mod.suggestions = Schema.Suggestions.DISABLED;
		else if (suggestionsField.isSelected()) mod.suggestions = Schema.Suggestions.FIELD;
		else if (suggestionsURL.isSelected()) mod.suggestions = Schema.Suggestions.URL;
		else if (suggestionsID.isSelected()) mod.suggestions = Schema.Suggestions.ID;
		else if (suggestionsString.isSelected()) mod.suggestions = Schema.Suggestions.STRING;
		else if (suggestionsNumber.isSelected()) mod.suggestions = Schema.Suggestions.NUMBER;
		else if (suggestionsInteger.isSelected()) mod.suggestions = Schema.Suggestions.INTEGER;
		
		if (isSummaryView)
		{
			for (Schema.Value val : assignment.values) mod.values.add(val.clone());
		}
		else
		{
			for (ValueWidgets vw : valueList)
			{
				Schema.Value val = new Schema.Value(ModelSchema.expandPrefix(vw.fieldURI.getText()), vw.fieldName.getText());
				val.descr = vw.fieldDescr.getText();
				int sel = vw.dropSpec.getSelectionModel().getSelectedIndex();
				val.spec = sel == 1 ? Schema.Specify.WHOLEBRANCH : sel == 2 ? Schema.Specify.EXCLUDE : 
						   sel == 3 ? Schema.Specify.EXCLUDEBRANCH : Schema.Specify.ITEM;
				mod.values.add(val);
			}
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
		mod.originURI = ModelSchema.expandPrefix(fieldURI.getText());
		
		for (AnnotWidgets aw : annotList) if (aw.sourceAnnot != null) 
		{
			for (Schema.Annotation annot : aw.sourceAnnot) mod.annotations.add(annot.clone());
		}

		// this is rather awkward: keep the new annotation order the same as the original, otherwise it causes problems
		int watermark = assay.annotations.size();
		Map<Schema.Annotation, Integer> order = new HashMap<>();
		for (int i = 0; i < mod.annotations.size(); i++)
		{
			Schema.Annotation annot = mod.annotations.get(i);
			int idx = -1;
			for (int j = 0; j < assay.annotations.size(); j++) if (assay.annotations.get(j).equals(annot)) {idx = j; break;}
			order.put(annot, idx >= 0 ? idx : watermark++);
		}
		mod.annotations.sort((v1, v2) ->
		{
			int i1 = order.get(v1), i2 = order.get(v2);
			if (i1 < i2) return -1; else if (i1 > i2) return 1; else return 0;
		});

		if (assay.equals(mod)) return null;
		//assay = mod; // (makes sure that the next call will claim nothing happened; is this valid?)
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
		if (!Vocabulary.globalInstance().isLoaded()) return;

		LookupPanel lookup = new LookupPanel(false, "", listValueURI(), listExcludedURI(), true);
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
		String uri = ModelSchema.expandPrefix(vw.fieldURI.getText());
		if (uri.length() == 0) {actionLookupName(); return;}
	
		Vocabulary vocab = Vocabulary.globalInstance();
		
		String label = vocab.getLabel(uri), descr = vocab.getDescr(uri);
		if (label == null) {actionLookupName(); return;}
		if (vw.fieldName.getText().length() == 0) vw.fieldName.setText(label);
		if (descr != null && vw.fieldDescr.getText().length() == 0) vw.fieldDescr.setText(descr);
	}
	public void actionLookupName()
	{
		if (focusIndex >= 0)
		{
			if (!Vocabulary.globalInstance().isLoaded()) return;
			ValueWidgets vw = valueList.get(focusIndex);
			String searchText = vw.fieldName.getText().length() > 0 ? vw.fieldName.getText() : ModelSchema.expandPrefix(vw.fieldURI.getText());
			LookupPanel lookup = new LookupPanel(false, searchText, listValueURI(), listExcludedURI(), false);
			lookup.setInitialURI(ModelSchema.expandPrefix(vw.fieldURI.getText()));
			Optional<LookupPanel.Resource[]> result = lookup.showAndWait();
			if (result.isPresent())
			{
				LookupPanel.Resource[] res = result.get();
				if (res != null && res.length > 0)
				{
					vw.fieldURI.setText(res[0].uri);
					if (vw.fieldName.getText().length() == 0) vw.fieldName.setText(res[0].label);
					if (vw.fieldDescr.getText().length() == 0) vw.fieldDescr.setText(res[0].descr);
				}
			}
		}
		else if (assignment != null || group != null)
		{
			if (!Vocabulary.globalInstance().isLoaded()) return;
			LookupPanel lookup = new LookupPanel(true, fieldName.getText(), new HashSet<>(), new HashSet<>(), false);
			lookup.setInitialURI(ModelSchema.expandPrefix(fieldURI.getText()));
			Optional<LookupPanel.Resource[]> result = lookup.showAndWait();
			if (result.isPresent())
			{
				LookupPanel.Resource[] res = result.get();
				if (res != null && res.length >= 1)
				{
					fieldURI.setText(res[0].uri);
					if (fieldName.getText().length() == 0) fieldName.setText(res[0].label);
					if (fieldDescr.getText().length() == 0) fieldDescr.setText(res[0].descr);
				}
			}
		}
}	
	public void actionShowTree()
	{
		if (!Vocabulary.globalInstance().isLoaded()) return;
	
		if (assignment == null) return;
		
		Schema.Assignment modAssn = extractAssignment();
		if (modAssn == null) modAssn = assignment;

		SchemaTree tree = new SchemaTree(modAssn, Vocabulary.globalInstance());
		new PreviewTreePanel(tree, modAssn).show();
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

		if (group.parent != null)
		{
			fieldURI = new TextField(group.groupURI);
			fieldURI.setPrefWidth(350);
			observeFocus(fieldURI, -1);
			Tooltip.install(fieldURI, new Tooltip("The group URI used to disambiguate this group"));
			line.add(fieldURI, "URI:", 1, 0);
		}

		vbox.getChildren().add(line);
	}
	
	private void recreateAssignment()
	{
		focusIndex = -1;
	
		vbox.getChildren().clear();
		vbox.setFillWidth(true);
		vbox.setMaxWidth(Double.MAX_VALUE);

		// assignment properties

		HBox titleLine = new HBox();
		titleLine.setSpacing(5);
		Label heading = new Label("Assignment");
		heading.setTextAlignment(TextAlignment.CENTER);
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0C0FF; -fx-padding: 0.1em 1em 0.1em 1em;");
		titleLine.getChildren().add(heading);
		Node padding = new Pane();
		titleLine.getChildren().add(padding);
		HBox.setHgrow(padding, Priority.ALWAYS);
		Button btnAdd = new Button("Add Values"), btnTree = new Button("Show Tree");
		btnAdd.setOnAction(event -> actionValueMultiAdd());
		btnTree.setOnAction(event -> actionShowTree());
		titleLine.getChildren().addAll(btnAdd, btnTree);
		vbox.getChildren().add(titleLine);

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

		ToggleGroup fieldSuggestions = new ToggleGroup();
		FlowPane suggestionsLine = new FlowPane();
		suggestionsLine.setPadding(new Insets(5, 0, 5, 0));
		suggestionsLine.setHgap(10);		
		suggestionsLine.setVgap(4);
		suggestionsFull = new RadioButton("Full");
		suggestionsDisabled = new RadioButton("Disabled");
		suggestionsField = new RadioButton("Field");
		suggestionsURL = new RadioButton("URL");
		suggestionsID = new RadioButton("ID");
		suggestionsString = new RadioButton("String");
		suggestionsNumber = new RadioButton("Number");
		suggestionsInteger = new RadioButton("Integer");
		suggestionsFull.setToggleGroup(fieldSuggestions);
		suggestionsDisabled.setToggleGroup(fieldSuggestions);
		suggestionsField.setToggleGroup(fieldSuggestions);
		suggestionsString.setToggleGroup(fieldSuggestions);
		suggestionsNumber.setToggleGroup(fieldSuggestions);
		suggestionsInteger.setToggleGroup(fieldSuggestions);
		suggestionsURL.setToggleGroup(fieldSuggestions);
		suggestionsID.setToggleGroup(fieldSuggestions);
		Tooltip.install(suggestionsFull, new Tooltip("Use suggestion models for the assignment"));
		Tooltip.install(suggestionsDisabled, new Tooltip("Don't use suggestion models for the assignment"));
		Tooltip.install(suggestionsField, new Tooltip("Connect assignment value to structure-activity fields"));
		Tooltip.install(suggestionsURL, new Tooltip("Assignment should be a URL to an external resource"));
		Tooltip.install(suggestionsID, new Tooltip("Assignment should be an ID code for another assay"));
		Tooltip.install(suggestionsString, new Tooltip("Assignment should be free text"));
		Tooltip.install(suggestionsNumber, new Tooltip("Assignment should be numeric (any precision)"));
		Tooltip.install(suggestionsInteger, new Tooltip("Assignment should be an integer"));
		suggestionsFull.setSelected(assignment.suggestions == Schema.Suggestions.FULL);
		suggestionsDisabled.setSelected(assignment.suggestions == Schema.Suggestions.DISABLED);
		suggestionsField.setSelected(assignment.suggestions == Schema.Suggestions.FIELD);
		suggestionsURL.setSelected(assignment.suggestions == Schema.Suggestions.URL);
		suggestionsID.setSelected(assignment.suggestions == Schema.Suggestions.ID);
		suggestionsString.setSelected(assignment.suggestions == Schema.Suggestions.STRING);
		suggestionsNumber.setSelected(assignment.suggestions == Schema.Suggestions.NUMBER);
		suggestionsInteger.setSelected(assignment.suggestions == Schema.Suggestions.INTEGER);
		suggestionsLine.getChildren().addAll(suggestionsFull, suggestionsDisabled, suggestionsField, suggestionsURL, suggestionsID, 
											 suggestionsString, suggestionsNumber, suggestionsInteger);
		line.add(suggestionsLine, "Suggestions:", 1, 0);

		vbox.getChildren().add(line);
	
		// constituent values
		
		heading = new Label("Values");
		heading.setTextAlignment(TextAlignment.CENTER);
		heading.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-border-color: black; -fx-background-color: #C0FFC0; -fx-padding: 0.1em 1em 0.1em 1em;");
		vbox.getChildren().add(heading);		

		valueList.clear();
		if (isSummaryView) recreateAssignmentSummary(); else recreateAssignmentFull();
	}
	private void recreateAssignmentSummary()
	{
		for (int n = 0; n < assignment.values.size(); n++)
		{
			Schema.Value val = assignment.values.get(n);
			
			Label label = new Label(val.name);
			label.setStyle("-fx-font-style: italic; -fx-padding: 0.1em 0.1em 0.1em 0.1em; -fx-text-fill: black; -fx-border-color: #808080; -fx-background-color: #F0F0F0;");

			String descr = val.uri;
			if (val.descr.length() > 0) descr += "\n\n" + val.descr;
			Tooltip tip = new Tooltip(descr);
			tip.setWrapText(true);
			tip.setMaxWidth(400);
			Tooltip.install(label, tip);

			vbox.getChildren().add(label);
		}
	}
	private void recreateAssignmentFull()	
	{
		for (int n = 0; n < assignment.values.size(); n++)
		{
			Schema.Value val = assignment.values.get(n);
		
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
			
			vw.dropSpec = new ComboBox<>();
			vw.dropSpec.getItems().addAll("Include", "Include Branch", "Exclude", "Exclude Branch");
			int sel = val.spec == Schema.Specify.WHOLEBRANCH ? 1 : val.spec == Schema.Specify.EXCLUDE ? 2 : val.spec == Schema.Specify.EXCLUDEBRANCH ? 3 : 0;
			vw.dropSpec.getSelectionModel().select(sel);
			observeFocus(vw.dropSpec, n);
			Tooltip.install(vw.dropSpec, new Tooltip("How to interpret the selected term: include or exclude, singleton or branch."));
			
			RowLine rowNameSpec = new RowLine(PADDING);
			rowNameSpec.add(vw.fieldName, 1);
			rowNameSpec.add(new Label("Specify:"), 0);
			rowNameSpec.add(vw.dropSpec, 0);
			vw.line.add(rowNameSpec, "Name:", 1, 0);
			
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
		Button btnPara = new Button("View");
		btnPara.setOnAction((ev) -> popupViewPara());
		line.add(RowLine.pair(PADDING, 0, fieldPara, 1, RowLine.TOP, btnPara, 0, RowLine.BOTTOM), "Paragraph:", 1, 0);
		
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
		List<Integer> orphidx = new ArrayList<>();
		for (int n = 0; n < orphans.size(); n++) orphidx.add(n);
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
				List<Schema.Annotation> matches = new ArrayList<>();
				List<Integer> indices = new ArrayList<>();
				for (int n = 0; n < orphans.size(); n++)
				{
					Schema.Annotation annot = orphans.get(n);
					if (schema.matchAnnotation(annot, assn))
					{
						matches.add(annot);
						indices.add(orphidx.get(n));
						
						orphans.remove(n);
						orphidx.remove(n);
						n--;
					}
				}
				String title = indstr + assn.name + ":";
				appendAnnotationWidgets(title, assn, matches.toArray(new Schema.Annotation[matches.size()]), indices.toArray(new Integer[indices.size()]), false);
			}
			for (int n = group.subGroups.size() - 1; n >= 0; n--) groupList.add(0, group.subGroups.get(n));
		}
		
		// if there are any annotations left over, they are orphans
		if (orphans.size() > 0)
		{
			heading = new Label("Orphans");
			heading.setStyle("-fx-font-weight: bold; -fx-text-fill: #800000;");
			vbox.getChildren().add(heading);

			for (int n = 0; n < orphans.size(); n++)
			{
				Schema.Annotation annot = orphans.get(n);
				String title = annot.assn.name + ":";
				for (Schema.Group p = annot.assn.parent; p != null; p = p.parent) title = p.name + " / " + title;
				
				appendAnnotationWidgets(title, null, new Schema.Annotation[]{annot}, new Integer[]{orphidx.get(n)}, true);
			}
		}
	}

	// creates a single annotation entry line; the assignment or annotation may be blank, not not both
	private void appendAnnotationWidgets(String title, Schema.Assignment assn, Schema.Annotation[] annots, Integer[] indices, boolean orphan)
	{
		AnnotWidgets aw = new AnnotWidgets();
		aw.line = new Lineup(PADDING);
		aw.sourceAssn = assn;
		aw.sourceAnnot = annots;
		final int nbtn = Math.max(1, annots.length);
		aw.buttonShow = new Button[nbtn];
		for (int n = 0; n < nbtn; n++) aw.buttonShow[n] = new Button();
		if (annots.length > 0 && !orphan) 
		{
			aw.buttonAdd = new Button("+");
			aw.buttonAdd.setOnAction(event -> pressedAnnotationButton(aw.sourceAssn, -1));
		}
		
		updateAnnotationButtons(aw);
		
		for (int n = 0; n < nbtn; n++)
		{
			aw.buttonShow[n].setPrefWidth(300);
			aw.buttonShow[n].setMaxWidth(Double.MAX_VALUE);
			Region row = orphan || annots.length == 0 || n < nbtn - 1 ? aw.buttonShow[n] : RowLine.pair(PADDING, aw.buttonShow[n], 1, aw.buttonAdd, 0);
			aw.line.add(row, n == 0 ? title : null, 1, 0);

			final int idx = n < indices.length ? indices[n] : -1;
			aw.buttonShow[n].setOnAction(event -> pressedAnnotationButton(aw.sourceAssn, idx));
		}
				
		vbox.getChildren().add(aw.line);
		
		annotList.add(aw);
	}
	
	// configures the button content and theme for an annotation
	private void updateAnnotationButtons(AnnotWidgets aw)
	{
		for (int n = 0; n < aw.buttonShow.length; n++)
		{
			Button btn = aw.buttonShow[n];
			Schema.Annotation annot = n < aw.sourceAnnot.length ? aw.sourceAnnot[n] : null;
			
			if (annot == null)
			{
				// blank value: waiting for the user to pick something
				btn.setText("?");
				btn.setStyle("-fx-base: #F0F0FF;");
			}
			else if (annot.value != null)
			{
				btn.setText(annot.value.name);
				btn.setStyle("-fx-base: #000080; -fx-text-fill: white;");
			}
			else // annot.literal != null
			{
				btn.setText('"' + annot.literal + '"');
				btn.setStyle("-fx-base: #FFFFD0;");
			}
	
			// it's an orphan: mark it noticeably
			if (aw.sourceAssn == null)
			{
				// orphan
				btn.setStyle("-fx-base: #C00000; -fx-text-fill: white;");
			}
		}
	}
	
	// responds to the pressing of an annotation button: typically brings up the edit panel
	private void pressedAnnotationButton(Schema.Assignment assn, int idx)
	{
		Schema.Assay modAssay = extractAssay();
		
		if (modAssay == null) modAssay = assay.clone();

		// orphan annotations: clicking deletes
		if (assn == null)
		{
			modAssay.annotations.remove(idx);
			main.updateBranchAssay(modAssay);
			recreateAssay();
			return;
		}

		// bring up panel for assignment selection
		Schema.Annotation annot = idx >= 0 ? modAssay.annotations.get(idx) : null;
		AnnotatePanel lookup = new AnnotatePanel(assn, annot);
		Optional<Schema.Annotation> result = lookup.showAndWait();
		if (result.isPresent())
		{
			Schema.Annotation res = result.get();
			if (res == null) return;
			
			if (res.assn == null) // clear
			{
				if (idx < 0) return; // nop
				modAssay.annotations.remove(idx);
			}
			else
			{
				if (idx < 0)
					modAssay.annotations.add(res);
				else
					modAssay.annotations.set(idx, res);
			}
		
			main.updateBranchAssay(modAssay);
			recreateAssay();
		}
	}

	// respond to focus so that one of the blocks gets a highlight
	private void observeFocus(Control field, final int idx)
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
					Background bg = new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.05), CornerRadii.EMPTY, new Insets(-4, -4, -4, -4)));
					valueList.get(focusIndex).line.setBackground(bg);
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
	
	// make a list of the URIs currently being used for values (of the inclusionary variety)
	private Set<String> listValueURI()
	{
		Set<String> used = new HashSet<>();
		for (ValueWidgets vw : valueList)
		{
			String uri = ModelSchema.expandPrefix(vw.fieldURI.getText());
			if (uri.length() > 0 && vw.dropSpec.getSelectionModel().getSelectedIndex() <= 1) used.add(uri);
		}
		return used;
	}
	
	// make a list of URIs currently, which are set to exclude
	private Set<String> listExcludedURI()
	{
		Set<String> excl = new HashSet<>();
		for (ValueWidgets vw : valueList)
		{
			String uri = ModelSchema.expandPrefix(vw.fieldURI.getText());
			if (uri.length() > 0 && vw.dropSpec.getSelectionModel().getSelectedIndex() >= 2) excl.add(uri);
		}
		return excl;
	}
	
	
	// brings up a popup window to show the paragraph text, which is handy for annotating
	private void popupViewPara()
	{
		String text = fieldPara.getText();
		if (text.length() == 0)
		{
			Util.informMessage("Paragraph", "Text is blank: nothing to show.");
			return;
		}
	
		Stage stage = new Stage();
		
		String title = fieldName.getText();
		if (title.length() == 0) title = "Paragraph";
		stage.setTitle(title);

		TextArea area = new TextArea(text);
		area.setWrapText(true);

		BorderPane root = new BorderPane();
		root.setCenter(area);

		Scene scene = new Scene(root, 500, 500, Color.WHITE);
		stage.setScene(scene);
		stage.show();
	}
}

