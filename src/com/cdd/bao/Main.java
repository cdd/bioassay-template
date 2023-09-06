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

package com.cdd.bao;

import com.cdd.bao.axioms.*;
import com.cdd.bao.editor.*;
import com.cdd.bao.importer.*;
import com.cdd.bao.template.*;
import com.cdd.bao.template.Schema.*;
import com.cdd.bao.template.SchemaVocab.*;
import com.cdd.bao.util.*;
import com.cdd.bao.validator.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.apache.commons.lang3.*;
import org.apache.jena.ontology.*;
import org.json.*;

/*
	Entrypoint for all command line functionality: delegates to the appropriate corner.
*/

public class Main
{
	public static void main(String[] argv)
	{
		if (argv.length > 0 && (argv[0].equals("-h") || argv[0].equals("--help")))
		{
			printHelp();
			return;
		}
		
		// look for additional options that affect overall state
		String[] extraOnto = null, exclOnto = null;
		for (int n = 0; n < argv.length;)
		{
			if (argv[n].startsWith("--onto"))
			{
				argv = ArrayUtils.remove(argv, n);
				while (n < argv.length)
				{
					if (argv[n].startsWith("-")) break;
					extraOnto = ArrayUtils.add(extraOnto, argv[n]);
					argv = ArrayUtils.remove(argv, n);
				}
			}
			else if (argv[n].startsWith("--excl"))
			{
				argv = ArrayUtils.remove(argv, n);
				while (n < argv.length)
				{
					if (argv[n].startsWith("-")) break;
					exclOnto = ArrayUtils.add(exclOnto, argv[n]);
					argv = ArrayUtils.remove(argv, n);
				}
			}
			else n++;
		}
		Vocabulary.setExtraOntology(extraOnto);
		Vocabulary.setExclOntology(exclOnto);

		// main command-induced functionality
		if (argv.length == 0) new MainApplication().exec(new String[0]);
		else if (argv[0].equals("edit")) 
		{			
			String[] subset = Arrays.copyOfRange(argv, 1, argv.length);
			try {new MainApplication().exec(subset);}
			catch (RuntimeException ex) {ex.printStackTrace();}
		}
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
		else if (argv[0].equals("importclo")) // import cell line ontology
		{
			try
			{
				ImportCellLineOntology impclo = new ImportCellLineOntology();
				impclo.load(argv[1]);
				impclo.save(argv[2]);
			}
			catch (Exception ex) {ex.printStackTrace();}
		}
		else if (argv[0].equals("filter"))
		{
			try
			{
				OntologyFilter filt = new OntologyFilter();
				filt.load(argv[1]);
				filt.save(argv[2]);
			}
			catch (Exception ex) {ex.printStackTrace();}
		}
		else if (argv[0].equals("compare"))
		{
			try {diffVocab(ArrayUtils.remove(argv, 0));}
			catch (Exception ex) {ex.printStackTrace();}
		}
		else if (argv[0].equals("compile"))
		{
			try {compileSchema(ArrayUtils.remove(argv, 0));}
			catch (Exception ex) {ex.printStackTrace();}
		}
		else if (argv[0].equals("check"))
		{
			try {checkTemplate(ArrayUtils.remove(argv, 0));}
			catch (Exception ex) {ex.printStackTrace();}
		}
		else if (argv[0].equals("import"))
		{
			try {importKeywords(ArrayUtils.remove(argv, 0));}
			catch (Exception ex) {ex.printStackTrace();}
		}
		else if (argv[0].equals("valuetree"))
		{
			try {compileValueTree(ArrayUtils.remove(argv, 0));}
			catch (Exception ex) {ex.printStackTrace();}
		}
		/*else if (argv[0].equals("showaxioms"))
		{
			String fnAxioms = argv.length >= 2 ? argv[1] : "data/template/axioms.dump";
			String fnTemplate = argv.length >= 3 ? argv[2] : "data/template/schema.json";
			String fnVocab = argv.length >= 4 ? argv[3] : "data/template/vocab.dump";
			try
			{
				ShowAxioms show = new ShowAxioms(fnAxioms, fnTemplate, fnVocab);
				show.exec();
			}
			catch (Exception ex) {ex.printStackTrace();}
		}*/
		else
		{
			Util.writeln("Unknown option '" + argv[0] + "'");
			printHelp();
		}
		
		
		/* TODO: put this into a separate invocation block
		ScanAxioms s = new ScanAxioms();
		try
		{
			s.exec();
		} 
		catch (Exception ex) 
		{
			Util.writeln("Axiom scan failed:");
			ex.printStackTrace();
			return;
		}

		AxiomCollector ac;
		ac = new AxiomCollector();
		
		try
		{
			AxiomCollector.serialiseAxiom();
			AxiomCollector.serialiseAxiomSome();
			//AxiomCollector.findAllAxiomsOfAssay();
			//AxiomCollector.minAllAxiomsOfAssay();
			//AxiomCollector.createAllAxiomsPerURI();
			//AxiomCollector.createMethodAxiomsPerURI();
			
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try 
		{
			ac.mergeAxiomMaps();
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}
	
	public static void printHelp()
	{
		Util.writeln("BioAssay Ontology Annotator Tools");
		Util.writeln("Options:");
		Util.writeln("    edit {files...}");
		Util.writeln("    geneont {infile} {outfile}");
		Util.writeln("    importclo {infile} {outfile}");
		Util.writeln("    filter {infile.owl/ttl} {outfile.ttl}");
		Util.writeln("    compare {old.dump} {new.dump}");
		Util.writeln("    compile {schema*.json} {vocab.dump/json}");
		Util.writeln("    check {schema.json}");
		Util.writeln("    import {cfg.json}");
		Util.writeln("    scanaxioms");
		Util.writeln("    valuetree {schema*.json} {outfile.txt}");
		Util.writeln("    --onto {files...}");
		Util.writeln("    --excl {files...}");
		Util.writeln("User e.g. vocab.dump.gz to compress file");
	}

	private static void diffVocab(String[] options) throws Exception
	{
		String fn1 = options[0], fn2 = options[1], fn3 = options.length >= 3 ? options[2] : null;
		Util.writeln("Differences between vocab dumps...");
		Util.writeln("    OLD:" + fn1);
		Util.writeln("    NEW:" + fn2);

		// note: giving no schemata works for this purpose
		SchemaVocab sv1 = loadVocabDump(fn1, new Schema[0]);
		SchemaVocab sv2 = loadVocabDump(fn2, new Schema[0]);
		
		Schema schema = null;
		if (fn3 != null) schema = SchemaUtil.deserialise(new File(fn3)).schema;

		Util.writeln("Term counts: [" + sv1.numTerms() + "] -> [" + sv2.numTerms() + "]");
		Util.writeln("Prefixes: [" + sv1.numPrefixes() + "] -> [" + sv2.numPrefixes() + "]");
		
		// note: only shows trees on both sides
		for (SchemaVocab.StoredTree tree1 : sv1.getTrees()) for (SchemaVocab.StoredTree tree2 : sv2.getTrees())
		{
			if (!tree1.schemaPrefix.equals(tree2.schemaPrefix) ||
				!tree1.propURI.equals(tree2.propURI) ||
				!Arrays.equals(tree1.groupNest, tree2.groupNest)) continue;

			String info = "propURI: " + tree1.propURI;
			if (schema != null && tree1.schemaPrefix.equals(schema.getSchemaPrefix()))
			{
				Assignment[] asmtFound = schema.findAssignmentByProperty(tree1.propURI, tree1.groupNest);
				if (asmtFound.length > 0)
				{
					Schema.Assignment assn = asmtFound.length > 0 ? asmtFound[0] : null;
					info = "assignment: " + assn.name + " (propURI: " + tree1.propURI + ")";
				}
			}

			Util.writeln("Schema [" + tree1.schemaPrefix + "], " + info);
			Set<String> terms1 = new HashSet<>(), terms2 = new HashSet<>();
			for (SchemaTree.Node node : tree1.tree.getFlat()) terms1.add(node.uri);
			for (SchemaTree.Node node : tree2.tree.getFlat()) terms2.add(node.uri);

			Set<String> extra1 = new TreeSet<>(), extra2 = new TreeSet<>();
			for (String uri : terms1) if (!terms2.contains(uri)) extra1.add(uri);
			for (String uri : terms2) if (!terms1.contains(uri)) extra2.add(uri);

			Util.writeln("    terms removed: " + extra1.size());
			for (String uri : extra1) Util.writeln("        <" + uri + "> " + sv1.getLabel(uri));

			Util.writeln("    terms added: " + extra2.size());
			for (String uri : extra2) Util.writeln("        <" + uri + "> " + sv2.getLabel(uri));
		}
	}      

	// compiles one-or-more schema files into a single vocabulary dump
	private static void compileSchema(String[] options) throws Exception
	{
		List<String> inputFiles = new ArrayList<>();
		for (int n = 0; n < options.length - 1; n++) inputFiles.add(Util.expandFileHome(options[n]));
		String outputFile = Util.expandFileHome(options[options.length - 1]);
		Util.writeln("Compiling schema files:");
		for (int n = 0; n < inputFiles.size(); n++) Util.writeln("    " + inputFiles.get(n));
		Util.writeln("Output to:");
		Util.writeln("    " + outputFile);
		
		//loadupVocab();
		Vocabulary vocab = new Vocabulary();
		Util.writeFlush("Loading ontologies ");
		vocab.addListener(new Vocabulary.Listener()
		{
			public void vocabLoadingProgress(Vocabulary vocab, float progress) {}
			public void vocabLoadingException(Exception ex) {ex.printStackTrace();}
			public void vocabLoadingFile(String path) {Util.writeln(" [" + path + "]");}
		});
		vocab.load(null, null);
		
		Schema[] schemata = new Schema[inputFiles.size()];
		Set<String> allPrefixes = new HashSet<>();
		for (int n = 0; n < schemata.length; n++) 
		{
			schemata[n] = SchemaUtil.deserialise(new File(inputFiles.get(n))).schema;
			String pfx = schemata[n].getSchemaPrefix();
			if (Util.isBlank(pfx)) throw new IOException("Schema [" + inputFiles.get(n) + "] has no URI prefix.");
			if (allPrefixes.contains(pfx)) throw new IOException("Duplicate prefix [" + pfx + "] found in [" + inputFiles.get(n) + "].");
			allPrefixes.add(pfx);
		}
		SchemaVocab schvoc = new SchemaVocab(vocab, schemata);

		Util.writeln("Loaded: " + schvoc.numTerms() + " terms.");
		
		for (Schema schema : schemata) 
		{
			Util.writeln("Schema [" + schema.getRoot().name + "] <" + schema.getSchemaPrefix() + ">");
			summariseSchemaGroup(schema.getRoot(), 4, schvoc);
		}
		
		try (OutputStream outstr = new FileOutputStream(outputFile))
		{
			boolean isGzip = outputFile.endsWith(".gz");
			OutputStream ostr = isGzip ? new GZIPOutputStream(outstr) : outstr;
			if (outputFile.endsWith(".json"))
			{
				JSONArray templates = new JSONArray();
				for (Schema schema : schemata)
				{
					SchemaVocab subset = schvoc.singleTemplate(schema.getSchemaPrefix());
				
					JSONObject json = new JSONObject();
					json.put("schemaPrefix", schema.getSchemaPrefix());
					json.put("root", ClipboardSchema.composeGroup(schema.getRoot(), subset));
					templates.put(json);
				}
				Writer wtr = new BufferedWriter(new OutputStreamWriter(ostr));
				templates.write(wtr);
				wtr.flush();
			}
			else schvoc.serialise(ostr);
			ostr.close();
		}
		
		Util.writeln("Done.");
	}

	// assembles all the schema content, and then emits each unique {valueURI,propURI,groupNest...} to a text file	
	private static void compileValueTree(String[] options) throws Exception
	{
		List<String> inputFiles = new ArrayList<>();
		for (int n = 0; n < options.length - 1; n++) inputFiles.add(Util.expandFileHome(options[n]));
		String outputFile = Util.expandFileHome(options[options.length - 1]);
		Util.writeln("Compiling schema files:");
		for (int n = 0; n < inputFiles.size(); n++) Util.writeln("    " + inputFiles.get(n));
		Util.writeln("Output to:");
		Util.writeln("    " + outputFile);
		
		Vocabulary vocab = new Vocabulary();
		Util.writeFlush("Loading ontologies ");
		vocab.load(null, null);
		
		Schema[] schemata = new Schema[inputFiles.size()];
		for (int n = 0; n < schemata.length; n++) schemata[n] = SchemaUtil.deserialise(new File(inputFiles.get(n))).schema;
		SchemaVocab schvoc = new SchemaVocab(vocab, schemata);
		Util.writeln("Loaded: " + schvoc.numTerms() + " terms.");
		
		Set<String> valueLines = new TreeSet<>();
		for (SchemaVocab.StoredTree stored : schvoc.getTrees())
		{
			Schema.Assignment assn = stored.tree.getAssignment();
			for (SchemaTree.Node node : stored.tree.getFlat())
			{
				String line = node.uri + "\t" + assn.propURI;
				for (String gn : assn.groupNest()) line += "\t" + gn;
				valueLines.add(line);
			}
		}
		
		Util.writeln("Unique value/prop/group entries: " + valueLines.size());
		
		try (Writer wtr = new BufferedWriter(new FileWriter(outputFile)))
		{
			for (String line : valueLines) wtr.write(line + "\n");
		}
		
		Util.writeln("Done.");
	}

	// display overview of schema contents
	private static void summariseSchemaGroup(Group group, int indent, SchemaVocab schvoc)
	{
		for (Assignment assn : group.assignments)
		{
			Util.write(Util.rep(' ', indent) + "[" + assn.name + "] <" + ModelSchema.collapsePrefix(assn.propURI) + ">");
			int nterms = 0;
			for (StoredTree stored : schvoc.getTrees()) if (stored.assignment == assn) 
			{
				nterms = stored.tree.getTree().size();
				Util.write(" terms=" + nterms);
				break;
			}
			Util.writeln();
			
			if (nterms == 0 && (assn.suggestions == Suggestions.FULL || assn.suggestions == Suggestions.DISABLED))
			{
				Util.writeln(Util.rep(' ', indent) + " **** no terms available to select");
			}
		}
		for (Group subgrp : group.subGroups)
		{
			Util.writeln(Util.rep(' ', indent) + "{" + subgrp.name + "} <" + ModelSchema.collapsePrefix(subgrp.groupURI) + ">");
			summariseSchemaGroup(subgrp, indent + 4, schvoc);
		}
	}

	// evaluates a schema template, looking for obvious shortcomings
	private static void checkTemplate(String[] options) throws Exception
	{
		if (options.length == 0)
		{
			Util.writeln("Must provide the schema filename to check filename.");
			return;
		}
		String fn = options[0];
		TemplateChecker chk = new TemplateChecker(fn, diagnostics ->
		{
			Util.writeln("");
			String lastGroupName = null, lastAssn = null;
			for (TemplateChecker.Diagnostic d : diagnostics)
			{
				List<Schema.Group> groupNest = d.groupNest;
				int indent = 2 * groupNest.size();
				String indstr = Util.rep(' ', indent);

				int lastIdx = groupNest.size() > 0 ? (groupNest.size() - 1) : -1;
				String trailingGroupName = lastIdx < 0 ? null : (groupNest.get(lastIdx) != null ? groupNest.get(lastIdx).name : null);
				if (!StringUtils.equals(lastGroupName, trailingGroupName))
				{
					Util.writeln(indstr + "---- Group: [" + trailingGroupName + "] ----");
					lastGroupName = trailingGroupName;
				}
				if (d.propURI != null && !StringUtils.equals(lastAssn, d.propURI))
				{
					Util.writeln(indstr + "---- Assignment: [" + d.propURI + "] ----");
					lastAssn = d.propURI;
				}
				Util.writeln(indstr + "** " + d.issue);
			}
		});
		chk.perform();
	}
	
	// initiates the importing of keywords from controlled vocabulary
	private static void importKeywords(String[] options) throws Exception
	{
		if (options.length < 5)
		{
			Util.writeln("Importing syntax: {src} {map} {dst} {schema} {vocab} [{hints}]");
			Util.writeln("    where {src} is the JSON-formatted pre-import data");
			Util.writeln("          {map} contains the mapping instructions");
			Util.writeln("          {dst} is an import-ready ZIP file");
			Util.writeln("          {schema} is the template to conform to");
			Util.writeln("          {vocab} is the processed vocabulary dump");
			Util.writeln("          {hints} is an optional JSON file with putative term-to-URI options");
			return;
		}
		String hintsFN = options.length >= 6 ? options[5] : null;
		ImportControlledVocab imp = new ImportControlledVocab(options[0], options[1], options[2], options[3], options[4], hintsFN);
		imp.exec();
	}
	
	private static SchemaVocab loadVocabDump(String filename, Schema[] templates) throws IOException
	{
		try (InputStream istr = new FileInputStream(filename))
		{
			boolean isGzip = filename.endsWith(".gz");
			InputStream inp = isGzip ? new GZIPInputStream(istr) : istr;
			return SchemaVocab.deserialise(inp, templates);
		}
	}

}
