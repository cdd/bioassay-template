/*
	BioAssay Ontology Annotator Tools

	Copyright 2016-2023 Collaborative Drug Discovery, Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package com.cdd.bao.editor.endpoint;

import com.cdd.bao.*;
import com.cdd.bao.template.*;
import com.cdd.bao.util.Lineup;
import com.cdd.bao.editor.*;

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
	Config panel: allows modification of settings, in particular those that pertain to SPARQL endpoints.
*/

public class ConfigPanel extends Dialog<Object>
{
	private TextField fieldEndpoint = new TextField();

	// ------------ public methods ------------

	public ConfigPanel()
	{
		super();
				
		setTitle("Configuration");

		setResizable(true);

		final int PADDING = 2;

		Lineup line = new Lineup(PADDING);
		
		Label notes = new Label("The SPARQL endpoint is a URL that contacts a triple store, which can serve and store schema documents.");
		notes.setWrapText(true);
		notes.setPrefWidth(300);
		notes.setMaxWidth(Double.MAX_VALUE);
		line.add(notes, null, 1, 0, Lineup.NOINDENT);
		
		line.add(fieldEndpoint, "Endpoint:", 1, 0);
 
		BorderPane pane = new BorderPane();
		pane.setPrefSize(line.getPrefWidth(), line.getPrefHeight());
		pane.setMaxHeight(Double.MAX_VALUE);
		pane.setPadding(new Insets(10, 10, 10, 10));
		BorderPane.setMargin(line, new Insets(0, 0, 10, 0));
		pane.setCenter(line);

		getDialogPane().setContent(pane);

		getDialogPane().getButtonTypes().add(new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));
		ButtonType btnTypeDone = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
		getDialogPane().getButtonTypes().add(btnTypeDone);
		setResultConverter(buttonType ->
		{
			if (buttonType == btnTypeDone) 
			{
				applyChanges();
				return "!";
			}
			return null;
		});
		
		fieldEndpoint.setText(EditorPrefs.getSparqlEndpoint());

		Platform.runLater(() -> fieldEndpoint.requestFocus());
	}
	
	// ------------ private methods ------------

	private void applyChanges()
	{
		EditorPrefs.setSparqlEndpoint(fieldEndpoint.getText());
	}
}
