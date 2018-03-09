/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2016 Collaborative Drug Discovery Inc.
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

import javafx.event.*;
import javafx.geometry.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.application.*;
import javafx.beans.value.*;
import javafx.util.*;

/*
	Display functionality for the schema tree shown on the left hand side of the main window (i.e. EditSchema).
*/

public final class HierarchyTreeCell extends TreeCell<EditSchema.Branch>
{
	private TextField textField;
 
	// ------------ private data ------------	

	public HierarchyTreeCell()
	{
	}
 
	public void updateItem(EditSchema.Branch branch, boolean empty)
	{
		super.updateItem(branch, empty);
 
		if (empty)
		{
			setText(null);
			setGraphic(null);
		}
		else 
		{
			if (isEditing()) 
			{
			    if (textField != null) textField.setText(getString());
			    setText(null);
			    setGraphic(textField);
			}
			else
			{
				String style = "-fx-font-family: arial;"; // note: using "Arial" because Mac system font (or sans) cannot match bold or
														 // italic for some reason; working around this bug

				String label = branch.heading != null ? branch.heading : 
							   branch.group != null ? branch.group.name : 
							   branch.assignment != null ? branch.assignment.name : 
							   branch.assay != null ? branch.assay.name : "?";
				if (label.length() == 0)
				{
					label = "unnamed";
					style += "-fx-text-fill: #800000;";
				}
	
				if (branch.heading != null || branch.group != null) 
				{
					style += "-fx-font-weight: bold;";
				}
				else /*if (branch.assignment != null)*/ 
				{
					style += "-fx-font-weight: normal;";
				}
	
				boolean grey = branch.assay != null && branch.assay.annotations.size() == 0;
				if (grey) style += " -fx-text-fill: #808080;"; else style += " -fx-text-fill: black;";
				
				setStyle(style);
				setText(label);
				setGraphic(getTreeItem().getGraphic());
				setupContextMenu(branch);
			}
		}
	}

	private void setupContextMenu(EditSchema.Branch branch)
	{
		ContextMenu ctx = new ContextMenu();

		if (branch.group != null) addMenu(ctx, "Add _Group").setOnAction(event -> branch.owner.actionGroupAdd());
		if (branch.group != null) addMenu(ctx, "Add _Assignment").setOnAction(event -> branch.owner.actionAssignmentAdd());
		if (branch.group == null && branch.assignment == null) addMenu(ctx, "Add Assa_y").setOnAction(event -> branch.owner.actionAssayAdd());
		if (branch.group != null || branch.assignment != null || branch.assay != null)
		{
			addMenu(ctx, "Cu_t").setOnAction(event -> branch.owner.actionEditCopy(true));
			addMenu(ctx, "_Copy").setOnAction(event -> branch.owner.actionEditCopy(false));
			addMenu(ctx, "_Paste").setOnAction(event -> branch.owner.actionEditPaste());
			addMenu(ctx, "_Delete").setOnAction(event -> branch.owner.actionEditDelete());
		}

		/* maybe add these: but need to check validity first
		addMenu(menuEdit, "Move _Up", new KeyCharacterCombination("[", cmd)).setOnAction(event -> actionEditMove(-1));
		addMenu(menuEdit, "Move _Down", new KeyCharacterCombination("]", cmd)).setOnAction(event -> actionEditMove(1));
		*/

		if (ctx.getItems().size() > 0) setContextMenu(ctx);
	}
	private MenuItem addMenu(ContextMenu parent, String title)
	{
		MenuItem item = new MenuItem(title);
		parent.getItems().add(item);
		return item;
	}

	private String getString() 
	{
		return getItem() == null ? "" : getItem().toString();
	}
}

