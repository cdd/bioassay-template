/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2018 Collaborative Drug Discovery Inc.
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

package com.cdd.bao;

import java.util.*;
import java.io.*;

import org.apache.commons.lang3.*;
import org.apache.jena.ontology.OntologyException;
import org.json.JSONException;

import com.cdd.bao.template.*;
import com.cdd.bao.template.Schema.*;
import com.cdd.bao.util.*;
import com.cdd.bao.axioms.AxiomCollector;
import com.cdd.bao.axioms.ScanAxioms;
import com.cdd.bao.validator.*;
import com.cdd.bao.editor.*;
import com.cdd.bao.importer.*;

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
			new MainApplication().exec(subset);
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
		else if (argv[0].equals("scanaxioms"))
		{
			try {new ScanAxioms().exec();}
			catch (Exception ex) {ex.printStackTrace();}
		}
		else
		{
			Util.writeln("Unknown option '" + argv[0] + "'");
			printHelp();
		}
		
		
		
		ScanAxioms s = new ScanAxioms();
		try {
			s.exec();
		} catch (OntologyException | JSONException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		AxiomCollector ac;
		ac = new AxiomCollector();
		
		try {
			AxiomCollector.serialiseAxiom();
			AxiomCollector.serialiseAxiomSome();
			//AxiomCollector.findAllAxiomsOfAssay();
			//AxiomCollector.minAllAxiomsOfAssay();
			//AxiomCollector.createAllAxiomsPerURI();
			//AxiomCollector.createMethodAxiomsPerURI();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ac.mergeAxiomMaps();
		
		
		
		
	}
	
	public static void printHelp()
	{
		Util.writeln("BioAssay Ontology Annotator Tools");
		Util.writeln("Options:");
		Util.writeln("    edit {files...}");
		Util.writeln("    geneont {infile} {outfile}");
		Util.writeln("    filter {infile.owl/ttl} {outfile.ttl}");
		Util.writeln("    compare {old.dump} {new.dump}");
		Util.writeln("    compile {schema*.ttl} {vocab.dump}");
		Util.writeln("    check {schema.ttl}");
		Util.writeln("    import {cfg.json}");
		Util.writeln("    scanaxioms");
		Util.writeln("    --onto {files...}");
		Util.writeln("    --excl {files...}");
	}

	private static void diffVocab(String[] options) throws Exception
	{
		String fn1 = options[0], fn2 = options[1], fn3 = options.length >= 3 ? options[2] : null;
		Util.writeln("Differences between vocab dumps...");
		Util.writeln("    OLD:" + fn1);
		Util.writeln("    NEW:" + fn2);

		InputStream istr = new FileInputStream(fn1);
		SchemaVocab sv1 = SchemaVocab.deserialise(istr, new Schema[0]); // note: giving no schemata works for this purpose
		istr.close();
		istr = new FileInputStream(fn2);
		SchemaVocab sv2 = SchemaVocab.deserialise(istr, new Schema[0]); // note: giving no schemata works for this purpose
		istr.close();
		
		Schema schema = null;
		if (fn3 != null) schema = SchemaUtil.deserialise(new File(fn3)).schema;

		Util.writeln("Term counts: [" + sv1.numTerms() + "] -> [" + sv2.numTerms() + "]");
		Util.writeln("Prefixes: [" + sv1.numPrefixes() + "] -> [" + sv2.numPrefixes() + "]");
		
		// note: only shows trees on both sides
		for (SchemaVocab.StoredTree tree1 : sv1.getTrees()) for (SchemaVocab.StoredTree tree2 : sv2.getTrees())
		{
			if (!tree1.schemaPrefix.equals(tree2.schemaPrefix)
				|| !tree1.propURI.equals(tree2.propURI)
				|| !Arrays.equals(tree1.groupNest, tree2.groupNest)) continue;

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
			public void vocabLoadingProgress(Vocabulary vocab, float progress) {Util.writeFlush(".");}
			public void vocabLoadingException(Exception ex) {ex.printStackTrace();}
		});
		vocab.load(null, null);
		Util.writeln();
		
		Schema[] schemata = new Schema[inputFiles.size()];
		for (int n = 0; n < schemata.length; n++) schemata[n] = SchemaUtil.deserialise(new File(inputFiles.get(n))).schema;
		SchemaVocab schvoc = new SchemaVocab(vocab, schemata);

		Util.writeln("Loaded: " + schvoc.numTerms() + " terms.");
		try (OutputStream ostr = new FileOutputStream(outputFile))
		{
			schvoc.serialise(ostr);
		}
		Util.writeln("Done.");
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
}
