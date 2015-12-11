/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.editor;

import com.cdd.bao.*;
import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;

import javafx.application.*;
import javafx.stage.*;
import javafx.scene.image.*;

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
		EditSchema edit = new EditSchema(primaryStage);
		
		for (String fn : getParameters().getUnnamed())
		{
			File f = new File(fn);
			if (!f.exists())
			{
				Util.writeln("File not found: " + f.getPath());
				return;
			}
			edit.loadFile(f);
			break;
		}
		
		final Stage stage = primaryStage;
        Platform.runLater(() -> stage.show());
	}

	// ------------ private methods ------------	

}
