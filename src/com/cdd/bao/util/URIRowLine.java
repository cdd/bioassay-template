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

package com.cdd.bao.util;

import javafx.scene.*;
import javafx.scene.control.*;

public class URIRowLine extends RowLine
{
	public interface Delegate
	{
		public void observeFocus(Control field, final int idx);
		public void actionLookupName(int focusIndex);
	}

	private URITextFieldDelegate delegate;

	private TextField getURITextField()
	{
		for (Node n : getChildren())
			if (n instanceof TextField) return (TextField)n;

		return null;
	}

	public URIRowLine(String uri, String tooltip, int focusIndex, int padding, URITextFieldDelegate delegate)
	{
		super(padding);
		this.delegate = delegate;

		TextField tfURI = new TextField(uri == null ? "" : uri);
		tfURI.setPrefWidth(350);
		this.delegate.observeFocus(tfURI, focusIndex);
		Tooltip.install(tfURI, new Tooltip(tooltip));

		Button lookupUriBtn = new Button("Lookup");
		lookupUriBtn.setOnAction(event -> this.delegate.actionLookupName(focusIndex));

		this.add(tfURI, 1);
		this.add(lookupUriBtn, 0);
	}

	public String getText()
	{
		TextField tf = this.getURITextField();
		if (tf != null) return tf.getText();

		return null;
	}

	public void setText(String txt)
	{
		TextField tf = this.getURITextField();
		if (tf != null) tf.setText(txt);
	}
}
