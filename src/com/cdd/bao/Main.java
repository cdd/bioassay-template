/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao;

import java.util.*;

import org.apache.commons.logging.Log;

import com.cdd.bao.template.*;
import com.cdd.bao.util.*;
import com.cdd.bao.editor.*;

/*
	Entrypoint for all command line functionality: delegates to the appropriate corner.
*/

public class Main
{
	public static void main(String[] argv)
	{
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);	
	
		if (argv.length > 0 && (argv[0].equals("-h") || argv[0].equals("--help")))
		{
			printHelp();
			return;
		}
		
		if (argv.length == 0) new MainApplication().exec(new String[0]);
		else if (argv[0].equals("edit")) 
		{
			String[] subset = Arrays.copyOfRange(argv, 1, argv.length);
			new MainApplication().exec(subset);
		}
		else if (argv[0].equals("prepare")) new PrepareTemplate().exec();
		else if (argv[0].equals("geneont"))
		{
			try
			{
				ImportGeneOntology impgo = new ImportGeneOntology();
				impgo.load(argv[1]);
				impgo.save(argv[2]);
			}
			catch (Exception ex) {ex.printStackTrace();}
		}
		else if (argv[0].equals("pubchem")) 
		{
			try
			{
				PrepareSchemaAnnotations prep = new PrepareSchemaAnnotations(argv[1]);
				prep.appendPubChemFiles(argv[3]);
				prep.writeSchema(argv[2]);
			}
			catch (Exception ex) {ex.printStackTrace();}
		}
		else
		{
			Util.writeln("Unknown option '" + argv[0] + "'");
			printHelp();
		}
	}
	
	public static void printHelp()
	{
		Util.writeln("BioAssay Ontology Annotator Tools");
		Util.writeln("Options:");
		Util.writeln("    edit {files...}");
		Util.writeln("    prepare");
		Util.writeln("    geneont {infile} {outfile}");
		Util.writeln("    pubchem {inschema} {outschema} {pubchemdir}");
	}
}
