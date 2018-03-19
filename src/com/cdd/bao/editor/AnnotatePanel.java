/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2018 Collaborative Drug Discovery Inc.
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

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.geometry.*;
import javafx.util.*;

/*
	Panel for looking up or otherwise defining an annotation.
*/

public class AnnotatePanel extends Dialog<Schema.Annotation>
{
	private Vocabulary vocab;
	private Vocabulary.Hierarchy hier;
	
	private Schema.Assignment assn;
	private List<Schema.Value> options = new ArrayList<>();

	private Button btnUse, btnClear;

	private static final int PADDING = 2;

	private TextField fieldSearch = new TextField();
	private CheckBox chkHierarchy = new CheckBox("Show ontology hierarchy");

	private TabPane tabber = new TabPane();
	private Tab tabValue = new Tab("Values"), tabLiteral = new Tab("Literal"), tabCustom = new Tab("Custom");
	
	private TextField fieldLiteral = new TextField();
	private TextField fieldCustomURI = new TextField(), fieldCustomName = new TextField();
	private TextArea fieldCustomDescr = new TextArea();

	private TableView<Schema.Value> table = new TableView<>();

	// ------------ public methods ------------

	public AnnotatePanel(Schema.Assignment assn, Schema.Annotation annot)
	{
		super();
		
		try 
		{
			vocab = Vocabulary.globalInstance();
			hier = vocab.getValueHierarchy();
		} 
		catch (Exception ex) {}
		
		this.assn = assn;
		options.addAll(assn.values);
		
		setTitle("Annotation: " + assn.name);

		setResizable(true);
		for (Tab tab : new Tab[]{tabValue, tabLiteral, tabCustom}) {tab.setClosable(false);}

		setupValue();
		setupLiteral();
		setupCustom();

		tabber.getTabs().addAll(tabValue, tabLiteral, tabCustom);

		getDialogPane().setContent(tabber);

		getDialogPane().getButtonTypes().add(new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));
		
		ButtonType btnTypeUse = new ButtonType("Use", ButtonBar.ButtonData.OK_DONE);
		ButtonType btnTypeClear = new ButtonType("Clear", ButtonBar.ButtonData.NO);
		getDialogPane().getButtonTypes().add(btnTypeClear);
		getDialogPane().getButtonTypes().add(btnTypeUse);
		setResultConverter(buttonType ->
		{
			if (buttonType == btnTypeUse) return composeCurrentValue();
			if (buttonType == btnTypeClear) return composeBlank();
			return null;
		});
		btnUse = (Button)getDialogPane().lookupButton(btnTypeUse);
		btnClear = (Button)getDialogPane().lookupButton(btnTypeClear);
		btnUse.addEventFilter(ActionEvent.ACTION, event ->
		{
			if (composeCurrentValue() == null) event.consume();
		});
		
		if (annot != null) fillCurrent(annot);
	}
		
	// ------------ private methods ------------

	private void setupValue()
	{
		table.setEditable(false);
 
		TableColumn<Schema.Value, String> colURI = new TableColumn<>("URI");
		colURI.setMinWidth(150);
		colURI.setCellValueFactory(value -> {return new SimpleStringProperty(ModelSchema.collapsePrefix(value.getValue().uri));});
		 
		TableColumn<Schema.Value, String> colLabel = new TableColumn<>("Label");
		colLabel.setMinWidth(200);
		colLabel.setCellValueFactory(value -> {return new SimpleStringProperty(valueName(value.getValue()));});
		
		TableColumn<Schema.Value, String> colDescr = new TableColumn<>("Description");
		colDescr.setMinWidth(400);
		colDescr.setCellValueFactory(value -> {return new SimpleStringProperty(cleanupDescription(value.getValue().descr));});
		
		table.setMinHeight(450);        
		table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		table.getColumns().add(colURI);
		table.getColumns().add(colLabel);
		table.getColumns().add(colDescr);
		table.setItems(FXCollections.observableArrayList(options));
 
		Lineup line = new Lineup(PADDING);
		fieldSearch.setPrefWidth(500);
		fieldSearch.setMaxWidth(Double.MAX_VALUE);
		line.add(RowLine.pair(PADDING, fieldSearch, 1, chkHierarchy, 0), "Search:", 1, 0);

		BorderPane pane = new BorderPane();
		pane.setPrefSize(800, 500);
		pane.setMaxHeight(Double.MAX_VALUE);
		pane.setPadding(new Insets(PADDING, PADDING, PADDING, PADDING));
		BorderPane.setMargin(line, new Insets(0, 0, PADDING, 0));
		pane.setTop(line);
		pane.setCenter(table);
		
		tabValue.setContent(pane);

		fieldSearch.textProperty().addListener((observable, oldValue, newValue) -> 
		{
			List<Schema.Value> selected = table.getSelectionModel().getSelectedItems();
			List<Schema.Value> newSelection = searchedSubset(newValue);
			table.setItems(FXCollections.observableArrayList(newSelection));
			for (Schema.Value sel : selected)
			{
				int idx = newSelection.indexOf(sel);
				if (idx >= 0) table.getSelectionModel().select(idx);
			}
		});
		chkHierarchy.selectedProperty().addListener((observable, oldValue, newValue) ->
		{
			colLabel.setVisible(false);
			colLabel.setVisible(true);
		});
		
		table.setOnMousePressed(event ->
		{
			if (event.isPrimaryButtonDown() && event.getClickCount() == 2) btnUse.fire();
		});
		table.setOnKeyPressed(event ->
		{
			if (event.getCode() == KeyCode.ENTER) btnUse.fire();
		});
	}
	private void setupLiteral()
	{
		Lineup content = new Lineup(PADDING, PADDING * 2);
		content.setMaxWidth(Double.MAX_VALUE);
		
		Label notes = new Label(
			"Enter a literal value for this annotation. This is free text, which means that there are no constraints on " +
			"what can be entered, but note that it has no semantic value, and will not be understandable by software.");
		notes.setWrapText(true);
		notes.setPrefWidth(300);
		notes.setMaxWidth(Double.MAX_VALUE);
		content.add(notes, null, 1, 0, Lineup.NOINDENT);

		fieldLiteral.setPrefWidth(300);
		content.add(fieldLiteral, "Value:", 1, 0);

		tabLiteral.setContent(content);
	}
	private void setupCustom()
	{
		Lineup content = new Lineup(PADDING, PADDING * 2);
		content.setMaxWidth(Double.MAX_VALUE);
		
		Label notes = new Label(
			"A custom value needs you to supply a semantically meaningful URI, and ideally a label to go with it. " +
			"The value is fully machine readable, but may be a singleton. The most common use case is for proposing " +
			"a new assignment option that should be added to the standard list.");
		notes.setWrapText(true);
		notes.setPrefWidth(300);
		notes.setMaxWidth(Double.MAX_VALUE);
		content.add(notes, null, 1, 0, Lineup.NOINDENT);

		fieldCustomURI.setPrefWidth(300);
		content.add(fieldCustomURI, "URI:", 1, 0);

		fieldCustomName.setPrefWidth(300);
		content.add(fieldCustomName, "Name:", 1, 0);

		fieldCustomDescr.setPrefRowCount(5);
		fieldCustomDescr.setPrefWidth(300);
		fieldCustomDescr.setWrapText(true);
		content.add(fieldCustomDescr, "Description:", 1, 0);

		tabCustom.setContent(content);
	}

	// fills in widget values from an existing annotation
	private void fillCurrent(Schema.Annotation annot)
	{
		if (annot.literal != null)
		{
			tabber.getSelectionModel().select(tabLiteral);
			fieldLiteral.setText(annot.literal);
			Platform.runLater(() -> fieldLiteral.requestFocus());
			return;
		}
		
		for (int n = 0; n < assn.values.size(); n++) if (assn.values.get(n).uri.equals(annot.value.uri))
		{
			tabber.getSelectionModel().select(tabValue);
			table.getSelectionModel().select(n);
			Platform.runLater(() -> table.requestFocus());
			return;
		}
		
		tabber.getSelectionModel().select(tabCustom);
		fieldCustomURI.setText(annot.value.uri);
		fieldCustomName.setText(annot.value.name);
		fieldCustomDescr.setText(annot.value.descr);
		Platform.runLater(() -> fieldCustomURI.requestFocus());
	}	

	// produces a value based on the current tab's contents, or returns null if not valid 
	private Schema.Annotation composeCurrentValue()
	{
		Tab seltab = tabber.selectionModelProperty().get().getSelectedItem();
		if (seltab == tabValue)
		{
			int idx = table.getSelectionModel().getSelectedIndex();
			if (idx < 0) return null;
			Schema.Value val = table.getSelectionModel().getSelectedItem();
			return new Schema.Annotation(assn, val);
		}
		else if (seltab == tabLiteral)
		{
			String literal = fieldLiteral.getText();
			if (literal.length() == 0) return null;
			return new Schema.Annotation(assn, literal);
		}
		else if (seltab == tabCustom)
		{
			Schema.Value val = new Schema.Value(fieldCustomURI.getText(), fieldCustomName.getText());
			if (val.uri.length() == 0) return null;
			val.descr = fieldCustomDescr.getText();
			return new Schema.Annotation(assn, val);
		}
		return null;
	}
	
	// creates a "blank" value, which signifies to the caller that the annotation should be zapped
	private Schema.Annotation composeBlank()
	{
		return new Schema.Annotation();
	}
	
	private String cleanupDescription(String descr)
	{
		return descr.replaceAll("\n", " ");
	}
	
	// returns a subset of the resources which matches the search text (or all if blank)
	private List<Schema.Value> searchedSubset(String searchText)
	{
		if (searchText.length() == 0) return options;
		
		String searchLC = searchText.toLowerCase();
		
		List<Schema.Value> subset = new ArrayList<>();
		for (Schema.Value val : options)
		{
			if (val.name.toLowerCase().contains(searchLC) || val.uri.toLowerCase().contains(searchLC) ||
				val.descr.toLowerCase().contains(searchLC)) subset.add(val);
		}
		return subset;
	}	
	
	// returns the "name" of a value: whether this is just the name itself, or the hierarchy sequence
	private String valueName(Schema.Value val)
	{
		if (!chkHierarchy.isSelected()) return val.name;

		String prefix = "";
		Vocabulary.Branch branch = hier.uriToBranch.get(val.uri);
		if (branch != null)
		{
			while (branch.parents.size() > 0)
			{
				branch = branch.parents.get(0);
				String label = vocab.getLabel(branch.uri);
				if (label == null) label = branch.uri;
				prefix = label + " \u25BA " + prefix;
			}
		}
		
		return prefix + val.name;
	}
}
