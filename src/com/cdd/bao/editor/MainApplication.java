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

import com.cdd.bao.*;
import com.cdd.bao.util.*;
import com.cdd.bao.template.*;

import java.io.*;
import java.util.*;

import javafx.application.*;
import javafx.stage.*;
import javafx.scene.image.*;
import org.apache.commons.lang3.*;

/*
	BioAssay Ontology Tools: entrypoint with command line parameters.
*/

public class MainApplication extends Application
{
	public static Image icon = null;

	// ------------ public methods ------------	

	public MainApplication()
	{
		try
		{
			InputStream istr = Util.openResource(this, "/images/MainIcon.png");
			icon = new Image(istr);
			istr.close();
		}
		catch (Exception ex) {ex.printStackTrace();}
	}
	
	public void exec(String[] args)
	{
		Application.launch(MainApplication.class, args);
	}
	
	public void start(Stage primaryStage)
	{
		// open a main window: either a new schema or an existing one
		EditSchema edit = null;
		for (String fn : getParameters().getUnnamed())
		{
			File f = new File(fn);
			if (!f.exists())
			{
				Util.writeln("File not found: " + f.getPath());
				return;
			}
			edit = new EditSchema(primaryStage, f);
			break;
		}
		if (edit == null) edit = new EditSchema(primaryStage, null);
		
		final Stage stage = primaryStage;
		Platform.runLater(() -> stage.show());
	}

	// ------------ private methods ------------	

}
