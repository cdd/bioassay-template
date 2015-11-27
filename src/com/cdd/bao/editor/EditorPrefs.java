/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.editor;

import java.util.prefs.*;

/*
	Centralised
*/

public class EditorPrefs
{
	public static Preferences prefs = Preferences.userNodeForPackage(EditorPrefs.class);

	// SPARQL endpoint: to be used for querying; null or blank means none has been configured
	public static String getSparqlEndpoint() {return prefs.get("sparqlEndpoint", null);}
	public static void setSparqlEndpoint(String val) {prefs.put("sparqlEndpoint", val);}
}
