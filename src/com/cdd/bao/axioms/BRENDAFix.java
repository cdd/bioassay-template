/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2017-2018 Collaborative Drug Discovery Inc.
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

package com.cdd.bao.axioms;

import com.cdd.bao.util.*;
import com.cdd.bao.template.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

import org.json.*;
import org.apache.commons.lang3.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;

/*
	Looks at the BRENDA hierarchy, which is initially flat, and generates a patch file that splits it into two parts -
	the cells and the non-cells (tissues).
*/

public class BRENDAFix 
{
	private Vocabulary vocab = null;
	
	private final static String URI_BRENDA_ROOT = "http://dto.org/DTO/DTO_000";
	
	private final static String URI_BRENDA_CELLLINES = "bae:BrendaCell";
	private final static String URI_BRENDA_TISSUES = "bae:BrendaTissues";

	// constructor parameters parsed from command-line
	private PrintWriter out;

	public static void main(String[] argv)
	{
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);

		if (argv.length > 0 && (argv[0].equals("-h") || argv[0].equals("--help")))
		{
			printHelp();
			return;
		}

		File outfile = null;
		for (int n = 0; n < argv.length; n++)
		{
			if (argv[n].equals("--outfile") && n < argv.length - 1) outfile = new File(argv[++n]);
		}
		
		if (outfile != null && outfile.exists() && !outfile.isFile())
		{
			System.err.println("Please specify a valid file location for --outfile.");
			return;
		}

		Util.writeln("BRENDA Fix");
		try {new BRENDAFix(outfile).exec();}
		catch (Exception ex) {ex.printStackTrace();}
		Util.writeln("Done.");
	}
	
	private static void printHelp()
	{
		Util.writeln("BioAssay Ontology Annotator Tools: BRENDA Cell/Tissue Fix");
		Util.writeln("Options:");
		Util.writeln("    --outfile {fn}         output file for delta results");
	}

	public BRENDAFix(File outfile) throws FileNotFoundException
	{
		this.out = new PrintWriter(outfile);
	}

	public void exec() throws Exception
	{
		writeHeader();

		Util.writeln("Loading vocab...");
		vocab = new Vocabulary("data/ontology", null);
		Util.writeln("... properties: " + vocab.numProperties() + ", values: " + vocab.numValues());		
		
		Vocabulary.Branch root = vocab.getValueHierarchy().uriToBranch.get(ModelSchema.expandPrefix(URI_BRENDA_ROOT));
		
		String skip1 = ModelSchema.expandPrefix(URI_BRENDA_CELLLINES), skip2 = ModelSchema.expandPrefix(URI_BRENDA_TISSUES); 
		Set<String> cells = new TreeSet<>(), tissues = new TreeSet<>();
		for (Vocabulary.Branch child : root.children)
		{
			if (child.uri.equals(skip1) || child.uri.equals(skip2)) continue;
			
			if (child.label.endsWith(" cell") || child.label.endsWith(" cell line"))
				cells.add(child.uri);
			else
				tissues.add(child.uri);
		}
		
		Util.writeln("Cells: " + cells.size());
		Util.writeln("Tissues: " + tissues.size());
		
		out.println("# reparenting cells\n");
		for (String uri : cells)
		{
			String abbrev = ModelSchema.collapsePrefix(uri), label = vocab.getLabel(uri);
			out.println(abbrev + " rdfs:subClassOf " + URI_BRENDA_CELLLINES + " . # " + label);
		}
		
		out.println("\n# reparenting tissues\n");
		for (String uri : tissues)
		{
			String abbrev = ModelSchema.collapsePrefix(uri), label = vocab.getLabel(uri);
			out.println(abbrev + " rdfs:subClassOf " + URI_BRENDA_TISSUES + " . # " + label);
		}

		out.close();
	}
	
	private void writeHeader()
	{
		out.println("# auto-generated file that divides BRENDA terms into cell vs. tissue branches\n");
		
		out.println("@prefix bao:   <http://www.bioassayontology.org/bao#> .");
		out.println("@prefix bat:   <http://www.bioassayontology.org/bat#> .");
		out.println("@prefix bae:   <http://www.bioassayexpress.org/bae#>  .");
		out.println("@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		out.println("@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .");
		out.println("@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .");
		out.println("@prefix owl:   <http://www.w3.org/2002/07/owl#> .");
		out.println("@prefix obo:   <http://purl.obolibrary.org/obo/> .");
		out.println();

		out.println(URI_BRENDA_CELLLINES);
		out.println("  rdfs:label \"cell lines\" ;");
		out.println("  rdfs:subClassOf <" + URI_BRENDA_ROOT + "> ;");
		out.println("  a bat:preferredParent ;");
		out.println("  .\n");

		out.println(URI_BRENDA_TISSUES);
		out.println("  rdfs:label \"tissues\" ;");
		out.println("  rdfs:subClassOf <" + URI_BRENDA_ROOT + "> ;");
		out.println("  a bat:preferredParent ;");
		out.println("  .\n");
	}
	
	// a trivial escaping transform that needs to be applied to any Turtle-formatted string
	private String escapeTurtleString(String str)
	{
		StringBuilder builder = new StringBuilder();
		for (char ch : str.toCharArray())
		{
			if (ch == '"') builder.append("\\\"");
			else if (ch == '\\') builder.append("\\\\");
			else if (ch == '\t') builder.append("\\t");
			else if (ch == '\r') {}
			else if (ch == '\n') builder.append("\\n");
			else builder.append(ch);
		}
		return builder.toString();
	}
}
