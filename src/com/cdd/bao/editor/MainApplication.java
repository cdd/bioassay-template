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
