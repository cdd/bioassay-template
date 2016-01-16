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

package com.cdd.bao.editor.fetch;

import com.cdd.bao.*;
import com.cdd.bao.template.*;
import com.cdd.bao.util.Lineup;
import com.cdd.bao.util.*;
import com.cdd.bao.editor.*;

import java.io.*;
import java.util.*;
import java.net.*;

import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.application.*;
import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.geometry.*;
import javafx.util.*;

import org.json.*;

/*
	PubChem Panel: its purpose in life is to discover a specific-or-random PubChem Assay ID, and allow the user to bring it in as a prototype
	annotation. The full description text 
*/

public class PubChemPanel extends Dialog<Schema.Assay>
{
	private Schema schema;
	private TextField fieldAID = new TextField();
	private Label labelStatus = new Label("Enter ID code, or ask for a Random one.");
	private TextField fieldName = new TextField();
	private TextField fieldURI = new TextField();
	private TextArea fieldPara = new TextArea();

	private Schema.Assay assay = null;
	
	private final String BASE_PUG = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/";
	
	private final Object mutex = new String("!");
	private final class Task
	{
		boolean cancelled = false;
	}
	private Task background = null;
	
	// ------------ public methods ------------

	public PubChemPanel(Schema schema)
	{
		super();
				
		this.schema = schema;
				
		setTitle("Lookup PubChem");

		setResizable(true);

        final int PADDING = 2;
        
		Lineup line = new Lineup(PADDING);
		
		line.add(fieldAID, "PubChem AID:", 1, 0);
 
 		labelStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: #000080;");
 		line.add(labelStatus, null, 1, 0, Lineup.NOINDENT);

		fieldName.setDisable(true);
 		fieldName.setPrefWidth(300);
		Tooltip.install(fieldName, new Tooltip("Very short name for the assay"));
		line.add(fieldName, "Name:", 1, 0);
		
		fieldURI.setDisable(true);
		fieldURI.setPrefWidth(300);
		Tooltip.install(fieldURI, new Tooltip("Source of origin in PubChem"));
		line.add(fieldURI, "Origin URI:", 1, 0);
		
		fieldPara.setDisable(true);
		fieldPara.setPrefRowCount(30);
		fieldPara.setPrefWidth(600);
		fieldPara.setWrapText(true);
		Tooltip.install(fieldPara, new Tooltip("Detailed description of assay"));
		line.add(fieldPara, "Paragraph:", 1, 0);

        BorderPane pane = new BorderPane();
        pane.setPrefSize(line.getPrefWidth(), line.getPrefHeight());
        pane.setMaxHeight(Double.MAX_VALUE);
        pane.setPadding(new Insets(10, 10, 10, 10));
        BorderPane.setMargin(line, new Insets(0, 0, 10, 0));
        pane.setCenter(line);
 		
		getDialogPane().setContent(pane);

		getDialogPane().getButtonTypes().add(new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE));

		ButtonType btnTypeRandom = new ButtonType("Random", ButtonBar.ButtonData.OTHER);
		getDialogPane().getButtonTypes().add(btnTypeRandom);
		
		ButtonType btnTypeDone = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
		getDialogPane().getButtonTypes().add(btnTypeDone);

		Button btnRandom = (Button)getDialogPane().lookupButton(btnTypeRandom), btnDone = (Button)getDialogPane().lookupButton(btnTypeDone);
		btnRandom.addEventFilter(ActionEvent.ACTION, event ->
		{
			stopBackground();
			findRandom();
			event.consume();
		});
		btnDone.addEventFilter(ActionEvent.ACTION, event ->
		{
			synchronized (mutex) 
			{
				if (background != null)
				{
					event.consume();
					return;
				}
			}
			if (assay == null)
			{
				findSpecific();
				event.consume();
			}
		});

		setResultConverter(buttonType ->
		{
			stopBackground();
			if (buttonType == btnTypeDone) 
			{
				if (assay != null) return assay;
			}
			return null;
		});
		
        Platform.runLater(() -> fieldAID.requestFocus());
	}
	
	// ------------ private methods ------------
	
	// reports back on a looked-up assay
	private void applyFetchedAssay(Schema.Assay assay)
	{
		synchronized (mutex) {background = null;}

		this.assay = assay;
	
		labelStatus.setText("Edit to suit, then accept.");

		fieldName.setDisable(false);
		fieldURI.setDisable(false);
		fieldPara.setDisable(false);

		fieldAID.setText("");
		fieldName.setText(assay.name);
		fieldURI.setText(assay.originURI);
		fieldPara.setText(assay.para);
	}
	
	// tells the background thread (if any) to stop
	private void stopBackground()
	{
		labelStatus.setText("Stopped");
		synchronized (mutex)
		{
			if (background != null) background.cancelled = true;
			background = null;
		}
	}
	
	// starts a background thread to find the user-specified AID
	private void findSpecific()
	{
		int aid = 0;
		try {aid = Integer.parseInt(fieldAID.getText());}
		catch (NumberFormatException ex) {}
		
		if (aid <= 0)
		{
			Util.informWarning("Invalid AID", "Enter a valid PubChem AID number, which is counting number.");
            return;
		}
		
		stopBackground();
		labelStatus.setText("Searching PubChem...");
		background = new Task();
		
		final int useAID = aid;
		new Thread(() -> backgroundFindSpecific(background, useAID)).start();
	}
	private void backgroundFindSpecific(Task task, int aid)
	{
		Schema.Assay assay = findAID(aid);
		if (task.cancelled) return;

		synchronized (mutex)
		{
			background = null;
    		if (assay == null)
    		{
    			Util.informWarning("Not Found", "PubChem AID " + aid + " not found (or there's a network connectivity issue).");
    			return;
    		}
			Platform.runLater(() -> applyFetchedAssay(assay));
		}
	}
	
	// starts a background thread that searches for a random AID in PubChem
	private void findRandom()
	{
		stopBackground();		
		labelStatus.setText("Searching PubChem...");
		
		background = new Task();
		new Thread(() -> backgroundFindRandom(background)).start();
	}
	private void backgroundFindRandom(Task task)
	{
		// this is kludgey: PubChem PUG API doesn't provide any good way to list all the AID numbers, or to find out how many of them exist, or
		// what the highest one is; as of December 2015, assays IDs up to the number given below existed in the database, and they are mostly
		// consecutive, with a handful of entries missing; therefore repeatedly drunk-dialling until a valid one appears does work quite well
		final int MAX_AID = 1159500;
	
		Random rnd = new Random();
	
		while (!task.cancelled)
		{
			int aid = rnd.nextInt(MAX_AID) + 1;
			Schema.Assay assay = findAID(aid);
			
			// make sure we don't already have it
			if (assay != null) for (int n = 0; n < schema.numAssays(); n++)
			{
				Schema.Assay look = schema.getAssay(n);
				if (look.originURI.equals(assay.originURI)) {assay = null; break;}
			}
			
			if (assay == null) continue; // try again, until cancelled
			if (task.cancelled) break;
			
			final Schema.Assay useAssay = assay;
			Platform.runLater(() -> applyFetchedAssay(useAssay));
			break;
		}
	}

	// looks up an ID number in PubChem, and converts the result to a schema assay; gracefully returns null if anything goes wrong	
	private Schema.Assay findAID(int aid)
	{
		try
		{
			String url = BASE_PUG + "assay/aid/" + aid + "/summary/json";
			Util.writeln("URL: " + url);
			String str = makeRequest(url, null);
			JSONObject json = new JSONObject(new JSONTokener(str));
			JSONObject summary = json.getJSONObject("AssaySummaries").getJSONArray("AssaySummary").getJSONObject(0);
			
			if (!summary.has("Description")) return null; // skip it; useless without descriptions
			
			//Util.writeln("SUMMARY:"+summary);
			
			Schema.Assay assay = new Schema.Assay(summary.getString("Name"));
			assay.originURI = "http://rdf.ncbi.nlm.nih.gov/pubchem/bioassay/AID" + aid;
			
			String sourceName = summary.optString("SourceName");
			String sourceID = summary.optString("SourceID");
			String description = joinArray(summary.optJSONArray("Description"));
			String protocol = joinArray(summary.optJSONArray("Protocol"));
			String comment = joinArray(summary.optJSONArray("Comment"));
			String method = summary.optString("Method");
			JSONObject lastDataChange = summary.optJSONObject("LastDataChange");
			int countAll = summary.optInt("CIDCountAll", -1);
			int countActive = summary.optInt("CIDCountActive", -1);
			int countInactive = summary.optInt("CIDCountInactive", -1);
			int countInconclusive = summary.optInt("CIDCountInconclusive", -1);
			int countUnspecified = summary.optInt("CIDCountUnspecified", -1);
			int countProbe = summary.optInt("CIDCountProbe", -1);
			JSONObject target = summary.optJSONObject("Target");
			
			assay.descr = description;
			
			StringBuffer para =  new StringBuffer();
			if (sourceName != null && sourceID != null) para.append("Source: " + sourceName + " ID: " + sourceID + "\n");
			if (method != null) para.append("Method: " + method + "\n");
			if (target != null) para.append("Target: " + target.optString("Name") + ", GI: " + target.optString("GI") + "\n");
			if (lastDataChange != null)
			{
				int day = lastDataChange.optInt("Day", 0), month = lastDataChange.optInt("Month"), year = lastDataChange.optInt("Year");
				para.append("Last Data Change: " + year + "/" + month + "/" + day + "\n");
			}
			if (countAll > 0)
			{
				para.append("Total compounds: " + countAll + "\n");
				para.append("    Actives:" + countActive + "\n");
				para.append("    Inactives:" + countInactive + "\n");
				para.append("    Inconclusives:" + countInconclusive + "\n");
				para.append("    Unspecifieds:" + countUnspecified + "\n");
				para.append("    Probes:" + countProbe + "\n");
			}
			
			para.append("\n");

			if (description.length() > 0) para.append("-- Description --\n\n" + description + "\n\n");
			if (protocol.length() > 0) para.append("-- Protocol --\n\n" + protocol + "\n\n");
			if (comment.length() > 0) para.append("-- Comment --\n\n" + comment + "\n\n");
			assay.para = para.toString();
			
			return assay;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return null;
		}	
	}
	
	// convenience method for joining together an array of strings consistent with the PubChem style, whereby a blank
	// entry is equivalent to two newlines (assumes wordwrapping will be handled by the renderer)
	private String joinArray(JSONArray array)
	{
		if (array == null || array.length() == 0) return "";
		StringBuffer buff = new StringBuffer();
		for (int n = 0; n < array.length(); n++)
		{
			try 
			{
				String line = array.getString(n);
				if (line.length() == 0) buff.append("\n\n"); else buff.append(line);
			}
			catch (JSONException ex) {}
		}
		return buff.toString();
	}
	
	// issues an HTTP request, with an optional URL-encoded form post
	private String makeRequest(String url, String post) throws IOException
	{
		java.net.HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setDoOutput(true);
		if (post == null) conn.setRequestMethod("GET");
		else
		{
    		conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    	}
		int cutoff = 300000; // 5 minutes
		conn.setConnectTimeout(cutoff);
		conn.setReadTimeout(cutoff);
		conn.connect();
		
		if (post!=null)
		{
			BufferedWriter send = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(),Util.UTF8));
    		send.append(post);
    		send.flush();
    		send.close();
    	}
    	
		int respCode = conn.getResponseCode();
		if (respCode >= 400) return null; // this is OK, just means no molecule found
		if (respCode != 200) throw new IOException("HTTP response code " + respCode);
		
		// read the raw bytes into memory; abort if it's too long or too slow
		BufferedInputStream istr = new BufferedInputStream(conn.getInputStream());
		ByteArrayOutputStream buff = new ByteArrayOutputStream();
		final int DOWNLOAD_LIMIT = 10000000; // within reason
		while (true)
		{
			int b = -1;
			try {b = istr.read();} catch (SocketTimeoutException ex) {throw new IOException(ex);}
			if (b < 0) break;
			if (buff.size() >= DOWNLOAD_LIMIT) throw new IOException("Download size limit exceeded.");
			buff.write(b);
		}
		istr.close();
		
		return new String(buff.toByteArray(), Util.UTF8);
	}
}



