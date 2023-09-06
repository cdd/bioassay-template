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

package com.cdd.bao.editor;

import javafx.application.*;
import javafx.beans.*;
import javafx.geometry.*;
import javafx.scene.control.ButtonBar.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/*
	TextInputDialogWithCheckBox:  dialog like TextInputDialog, with optional checkbox. 
*/

public class TextInputDialogWithCheckBox extends Dialog<String>
{
	private final GridPane grid;
	private final Label label;
	private final TextField textField;
	private final CheckBox checkbox;
	private final String defaultValue;

	private Label createContentLabel(String text)
	{
		Label label = new Label(text);
		label.setMaxWidth(Double.MAX_VALUE);
		label.setMaxHeight(Double.MAX_VALUE);
		label.getStyleClass().add("content");
		label.setWrapText(true);
		label.setPrefWidth(360);
		return label;
	}

	public TextInputDialogWithCheckBox()
	{
		this("", null);
	}

	public TextInputDialogWithCheckBox(String defaultValue, String checkboxDesc)
	{
		final DialogPane dialogPane = getDialogPane();

		this.textField = new TextField(defaultValue);
		this.textField.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(textField, Priority.ALWAYS);
		GridPane.setFillWidth(textField, true);

		// -- label
		label = createContentLabel(dialogPane.getContentText());
		label.setPrefWidth(Region.USE_COMPUTED_SIZE);
		label.textProperty().bind(dialogPane.contentTextProperty());

		// -- checkbox
		if (checkboxDesc != null)
		{
			checkbox = new CheckBox(checkboxDesc);
			checkbox.setSelected(false); // off by default
		}
		else checkbox = null;

		this.defaultValue = defaultValue;

		this.grid = new GridPane();
		this.grid.setHgap(10);
		this.grid.setMaxWidth(Double.MAX_VALUE);
		this.grid.setAlignment(Pos.CENTER_LEFT);

		dialogPane.contentTextProperty().addListener(o -> updateGrid());

		setTitle("Enter text");
		dialogPane.setHeaderText("Enter text below:");
		dialogPane.getStyleClass().add("text-input-dialog");
		dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		updateGrid();

		setResultConverter(dialogButton ->
		{
			ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
			return data == ButtonData.OK_DONE ? textField.getText() : null;
		});
	}
	public final TextField getEditor()
	{
		return textField;
	}
	public final String getDefaultValue()
	{
		return defaultValue;
	}
	public final CheckBox getCheckbox()
	{
		return checkbox;
	}
	private void updateGrid()
	{
		grid.getChildren().clear();

		grid.add(label, 0, 0);
		grid.add(textField, 1, 0);
		if (checkbox != null) grid.add(checkbox, 2, 0); // checkbox appears below text input field
		getDialogPane().setContent(grid);

		Platform.runLater(() -> textField.requestFocus());
	}
}
