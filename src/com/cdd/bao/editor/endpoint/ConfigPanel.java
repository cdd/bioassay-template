/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2016 Collaborative Drug Discovery Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * All we ask is that proper credit is given for our work, which includes
 * - but is not limited to - adding the above copyright notice to the beginning
 * of your source code files, and to any copyright notice that you may distribute
 * with programs based on this work.
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

package com.cdd.bao.editor.endpoint;

import com.cdd.bao.*;
import com.cdd.bao.template.*;
import com.cdd.bao.util.Lineup;
import com.cdd.bao.editor.*;

import java.io.*;
import java.util.*;

import javafx.scene.Node;
import javafx.scene.control.*;
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
