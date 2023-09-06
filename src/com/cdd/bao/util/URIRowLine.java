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

	private URIRowLine.Delegate delegate;

	private TextField getURITextField()
	{
		for (Node node : getChildren())
			if (node instanceof TextField) return (TextField)node;

		return null;
	}

	public URIRowLine(String uri, String tooltip, int focusIndex, int padding, URIRowLine.Delegate delegate)
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
