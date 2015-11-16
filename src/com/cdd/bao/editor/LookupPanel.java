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

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
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
	Lookup panel: takes a partially specified schema value and opens up the vocabulary list, to make it easy to pick URI/label/description
	combinations.
*/

public class LookupPanel extends Dialog<Schema.Value>
{
	private static final class Resource
	{
		String uri, label, descr;
		Resource(String uri, String label, String descr)
		{
			this.uri = uri;
			this.label = label == null ? "" : label;
			this.descr = descr == null ? "" : descr;
		}
		String getURI() {return uri;}
		void setURI(String uri) {this.uri = uri;}
		String getLabel() {return label;}
		void setLabel(String label) {this.label = label;}
		String getDescr() {return descr;}
		void setDescr(String descr) {this.descr = descr;}
	};
	private List<Resource> resources = new ArrayList<>();

	private TextField search = new TextField();
	private TableView<Resource> table = new TableView<>();

	// ------------ public methods ------------

	public LookupPanel(Schema.Value value)
	{
		super();
		
		loadResources();
		
		setTitle("Lookup URI");

		setResizable(true);

        final int PADDING = 2;
        
		Lineup line = new Lineup(PADDING);
		line.add(search, "Search:", 1, 0);
 
        table.setEditable(false);
 
        TableColumn<Resource, String> colURI = new TableColumn<>("URI");
		colURI.setMinWidth(200);
        colURI.setCellValueFactory(resource -> {return new SimpleStringProperty(substitutePrefix(resource.getValue().uri));});
         
        TableColumn<Resource, String> colLabel = new TableColumn<>("Label");
		colLabel.setMinWidth(200);
        colLabel.setCellValueFactory(resource -> {return new SimpleStringProperty(resource.getValue().label);});
        
        TableColumn<Resource, String> colDescr = new TableColumn<>("Description");
		colDescr.setMinWidth(300);
        colDescr.setCellValueFactory(resource -> {return new SimpleStringProperty(cleanupDescription(resource.getValue().descr));});

		table.setMinHeight(450);        
        table.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        table.getColumns().addAll(colURI, colLabel, colDescr);
        table.setItems(FXCollections.observableArrayList(resources));
 
        BorderPane pane = new BorderPane();
        pane.setPrefSize(800, 500);
        pane.setMaxHeight(Double.MAX_VALUE);
        pane.setPadding(new Insets(PADDING, PADDING, PADDING, PADDING));
        BorderPane.setMargin(line, new Insets(0, 0, PADDING, 0));
        pane.setTop(line);
        pane.setCenter(table);

		getDialogPane().setContent(pane);

		getDialogPane().getButtonTypes().add(new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));
		
		ButtonType btnTypeUse = new ButtonType("Use", ButtonBar.ButtonData.OK_DONE);
		getDialogPane().getButtonTypes().add(btnTypeUse);
		setResultConverter(buttonType ->
		{
			if (buttonType == btnTypeUse) return composeCurrentValue();
			return null;
		});
		Button btnUse = (Button)getDialogPane().lookupButton(btnTypeUse);
		btnUse.addEventFilter(ActionEvent.ACTION, event ->
		{
			if (table.getSelectionModel().getSelectedIndex() < 0) event.consume();
		});
		table.setOnMousePressed(event ->
		{
			if (event.isPrimaryButtonDown() && event.getClickCount() == 2) btnUse.fire();
		});
		
		
		if (value.name.length() > 0) search.setText(value.name);
		else if (value.uri.length() > 0) search.setText(value.uri);
		
        Platform.runLater(() -> search.requestFocus());
	}

	
	// ------------ private methods ------------

	private void loadResources()
	{
		Vocabulary vocab = null;
		try {vocab = Vocabulary.globalInstance();}
		catch (IOException ex) {ex.printStackTrace(); return;}
		
		for (String uri : vocab.getAllURIs())
		{
			Resource res = new Resource(uri, vocab.getLabel(uri), vocab.getDescr(uri));
			resources.add(res);
		}
	}

	// manufactures a value from the selected item
	private Schema.Value composeCurrentValue()
	{
		Resource res = table.getSelectionModel().getSelectedItem();
		if (res == null) return null;
	
		Schema.Value val = new Schema.Value(res.uri, res.label);
		val.descr = res.descr;
		return val;
	}
	
	// switches shorter prefixes for display convenience
	private final String[] SUBST = 
    {
    	"obo:", "http://purl.obolibrary.org/obo/",
    	"bao:", "http://www.bioassayontology.org/bao#",
    	"uo:",	"http://purl.org/obo/owl/UO#"
    };
	private String substitutePrefix(String uri)
	{
		for (int n = 0; n < SUBST.length; n += 2)
		{
			if (uri.startsWith(SUBST[n + 1])) return SUBST[n] + uri.substring(SUBST[n + 1].length());
		}
		return uri;
	}
	
	private String cleanupDescription(String descr)
	{
		return descr.replaceAll("\n", " ");
	}
}
