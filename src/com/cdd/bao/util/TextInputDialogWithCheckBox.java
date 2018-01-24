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

package com.cdd.bao.util;

import javafx.application.*;
import javafx.beans.*;
import javafx.geometry.*;
import javafx.scene.control.ButtonBar.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

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
