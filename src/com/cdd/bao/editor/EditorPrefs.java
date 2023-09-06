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
