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
	Attempt to merge CLO and BRENDA terms.  One of three cases happens:

		1. CLO term matches a BRENDA term.  In that case, the CLO term description is rewritten
		   with the BRENDA term description, and the BRENDA term is deleted.
		2. CLO term does not match any BRENDA term.  In that case, the CLO term is flagged for
		   needing a description.
		3. BRENDA term does not match any CLO term.  In that case, we move the BRENDA term under
		   a contrived parent rooted at the BRENDA root.  For example, we might have contrived
		   parents that house cell- or tissue-related terms only.

	The result of this merging operation -- a set of semantic directives -- is written to a
	specified file meant to be curated at a later time by a human.
*/

public class CellLineFix 
{
	private Vocabulary vocab = null;
	
	private static class CellPair
	{
		String uri1, label1;
		String uri2, label2;
	}

	private static class Match
	{
		String uri, label;
		int score;
	}

	private final static String URI_CLO_ROOT = "obo:CLO_0000001";
	private final static String URI_BRENDA_ROOT = "http://dto.org/DTO/DTO_000";
	
	// XXX contrived branches rooted at CLO and used to house BRENDA cell and tissue terms
	// XXX note these closely resemble provisional terms in BAE
	private final static String URI_FOR_CELL_LINES = "obo:CLO_9999998";
	private final static String URI_FOR_TISSUES = "obo:CLO_9999999";

	private Map<String, String> cloMap = new HashMap<>(), brendaMap = new HashMap<>(); // URI-to-label
	///private Set<String> brendaMatched = new HashSet<>(); // set of BRENDA terms that matched at least one CLO term
	
	private List<CellPair> pairs = new ArrayList<>();
	private Map<String, String[]> alreadyMatched = new HashMap<>();
	private Map<String, String[]> doNotMatch = new HashMap<>();
	private int numAlready = 0, numDoNot = 0;

	// constructor parameters parsed from command-line
	private boolean readpairs;
	private PrintWriter out;
	private String curationFN;
	private int threshold;

	public static void main(String[] argv)
	{
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);

		if (argv.length > 0 && (argv[0].equals("-h") || argv[0].equals("--help")))
		{
			printHelp();
			return;
		}

		File outfile = null;
		String curationFN = null;
		boolean readpairs = false;
		int threshold = 0;
		for (int n = 0; n < argv.length; n++)
		{
			if (argv[n].equals("--outfile") && n < argv.length - 1) outfile = new File(argv[++n]);
			else if (argv[n].equals("--curate") && n < argv.length - 1) curationFN = argv[++n];
			else if (argv[n].equals("--readpairs")) readpairs = true;
			else if (argv[n].equals("--threshold")) threshold = Integer.parseInt(argv[++n]);
		}
		
		if (outfile != null && outfile.exists() && !outfile.isFile())
		{
			System.err.println("Please specify a valid file location for --outfile.");
			return;
		}

		Util.writeln("Cell Line Fix");
		try {new CellLineFix(readpairs, outfile, curationFN, threshold).exec();}
		catch (Exception ex) {ex.printStackTrace();}
		Util.writeln("Done.");
	}
	
	private static void printHelp()
	{
		Util.writeln("BioAssay Ontology Annotator Tools: Cell Line Fix");
		Util.writeln("Options:");
		Util.writeln("    --outfile {fn}         output file for delta results");
		Util.writeln("    --curate {fn}          existing file to start from");
		Util.writeln("    --readpairs            bootstrap from original mapping");
		Util.writeln("    --threshold {thresh}   initial threshold (default 0)");
	}

	public CellLineFix(boolean readpairs, File outfile, String curationFN, int threshold) throws FileNotFoundException
	{
		this.readpairs = readpairs;
		this.out = new PrintWriter(outfile);
		this.curationFN = curationFN;
		this.threshold = threshold;
	}

	public void exec() throws Exception
	{
		//writeHeader();

		Util.writeln("Loading vocab...");
		vocab = new Vocabulary("data/ontology", null);
		Util.writeln("... properties: " + vocab.numProperties() + ", values: " + vocab.numValues());		
		
		Vocabulary.Hierarchy hier = vocab.getValueHierarchy();
		Vocabulary.Branch brendaRoot = hier.uriToBranch.get(ModelSchema.expandPrefix(URI_BRENDA_ROOT));
		Vocabulary.Branch cloRoot = hier.uriToBranch.get(ModelSchema.expandPrefix(URI_CLO_ROOT));
		
		for (Vocabulary.Branch br : brendaRoot.children) flattenBranch(brendaMap, br, true);
		for (Vocabulary.Branch br : cloRoot.children) flattenBranch(cloMap, br, false);

		if (readpairs)
		{
			loadCellPairs();
			Util.writeln("Predefined pairs: " + pairs.size());
		}
		else if (curationFN != null)
		{
			loadCuratedPairs();
			Util.writeln("Previously curated pairs: " + numAlready + ", not-pairs: " + numDoNot);
		}

		Util.writeln("BRENDA terms: " + brendaMap.size());
		Util.writeln("CLO terms:    " + cloMap.size());
		
		if (readpairs) 
			processPairs();
		else
			processIterations();

		out.close();
	}
	
	// load prescribed pairs (a legacy procedure, only needs to be done once)
	private void loadCellPairs() throws IOException
	{
		File f = new File("data/repair/axiom_cellpairs.json");
		if (!f.exists()) 
		{
			Util.writeln("** pair file [" + f.getAbsolutePath() + "] not found");
			return;
		}
		try (Reader rdr = new FileReader(f))
		{
			JSONArray list = new JSONArray(new JSONTokener(rdr));
			for (JSONObject json : list.toObjectArray()) 
			{
				CellPair cell = new CellPair();
				cell.uri1 = ModelSchema.expandPrefix(json.getString("uri1"));
				cell.label1 = json.getString("label1");
				cell.uri2 = ModelSchema.expandPrefix(json.optString("uri2"));
				cell.label2 = json.optString("label2");
				pairs.add(cell);
			}
		}
	}

	// load pairs that were established in a previous cycle
	private void loadCuratedPairs() throws IOException
	{
		Model model = ModelFactory.createDefaultModel();
		URL fileURL = new File(curationFN).toURI().toURL(); // changing file to a URL for passing into Jena's RDF reader			
		RDFDataMgr.read(model, fileURL.getPath(), curationFN.endsWith(".ttl") ? Lang.TURTLE : Lang.RDFXML);
		
		Property batPairedWith = model.createProperty(ModelSchema.PFX_BAT + "pairedWith");
		for (StmtIterator iter = model.listStatements(null, batPairedWith, (RDFNode)null); iter.hasNext();)
		{
			Statement stmt = iter.next();
			String uri1 = stmt.getSubject().getURI(), uri2 = stmt.getObject().asResource().getURI();
			alreadyMatched.put(uri1, ArrayUtils.add(alreadyMatched.get(uri1), uri2));
			alreadyMatched.put(uri2, ArrayUtils.add(alreadyMatched.get(uri2), uri1));
			numAlready++;
			
			// can happily deconsider both URIs from further processing
			cloMap.remove(uri1);
			cloMap.remove(uri2);
			brendaMap.remove(uri1);
			brendaMap.remove(uri2);
		}
		
		Property batNotPairedWith = model.createProperty(ModelSchema.PFX_BAT + "notPairedWith");
		for (StmtIterator iter = model.listStatements(null, batNotPairedWith, (RDFNode)null); iter.hasNext();)
		{
			Statement stmt = iter.next();
			String uri1 = stmt.getSubject().getURI(), uri2 = stmt.getObject().asResource().getURI();
			doNotMatch.put(uri1, ArrayUtils.add(doNotMatch.get(uri1), uri2));
			doNotMatch.put(uri2, ArrayUtils.add(doNotMatch.get(uri2), uri1));
			numDoNot++;
		}
	}
	
	// go through prescribed pair combinations
	private void processPairs() throws IOException
	{
		out.println("# generating content for predefined pairs only");
		
		for (CellPair cell : pairs)
		{
			brendaMap.remove(cell.uri1);
			brendaMap.remove(cell.uri2);
			cloMap.remove(cell.uri1);
			cloMap.remove(cell.uri2);
			handleMatchedTerms(cell.uri1, cell.label1, cell.uri2, cell.label2, null);
		}
		
		out.println("# end of predefined pairs");
	}
	
	// go hunting for terms to match, with best matches being claimed first
	private void processIterations() throws IOException
	{
		int totalMatches = 0;
		for (int th = threshold; th < 50; th++)
		{
			if (cloMap.isEmpty() || brendaMap.isEmpty()) break;
		
			Util.writeln("---- Score threshold: " + th + " ----");
			
			List<CellPair> pairs = new ArrayList<>();
			Map<String, Integer> dupCounts = new HashMap<>();
			for (String cloURI : cloMap.keySet()) 
			{
				String cloLabel = vocab.getLabel(cloURI);
				for (String brendaURI : brendaMap.keySet())
				{
					if (ArrayUtils.indexOf(doNotMatch.get(cloURI), brendaURI) >= 0) continue;
					String brendaLabel = vocab.getLabel(brendaURI);
					if (Util.stringSimilarity(cloLabel, brendaLabel) > th) continue;
					
					Util.writeln("    [" + cloLabel + "] / [" + brendaLabel + "]");
					
					CellPair pair = new CellPair();
					pair.uri1 = cloURI;
					pair.label1 = cloLabel;
					pair.uri2 = brendaURI;
					pair.label2 = brendaLabel;
					pairs.add(pair);
					
					dupCounts.put(cloURI, dupCounts.getOrDefault(cloURI, 0) + 1);
					dupCounts.put(brendaURI, dupCounts.getOrDefault(brendaURI, 0) + 1);
					
					//break;
				}
			}
			Util.writeln("     Matches found: " + pairs.size());
			
			pairs.sort((p1, p2) ->
			{
				int cmp = p1.label1.toLowerCase().compareTo(p2.label1.toLowerCase());
				if (cmp != 0) return cmp;
				return p1.label2.compareTo(p2.label2);
			});
			
			for (CellPair pair : pairs)
			{
				cloMap.remove(pair.uri1);
				brendaMap.remove(pair.uri2);
				handleMatchedTerms(pair.uri1, pair.label1, pair.uri2, pair.label2, dupCounts);
			}
			
			totalMatches += pairs.size();
			if (totalMatches > 100)
			{
				Util.writeln("==== Waypoint criteria met, stopping");
				break;
			}
		}
		
		Util.writeln("Total number of matches: " + totalMatches);
	
		//...
	
		/*

		// house unmapped CLO terms here
		NavigableMap<String, String> unmatchedCLOTerms = new TreeMap<>();

		// iterate over remaining CLO pairs, look for possible BRENDA matches
		int count = 0;

		Iterator<Map.Entry<String, String>> cloIter = cloMap.entrySet().iterator();
		while (cloIter.hasNext())
		{
			Map.Entry<String, String> entry = cloIter.next();

			String cloURI = entry.getKey(), cloLabel = entry.getValue();
			if (skipSet.contains(cloURI)) continue;

			if (++count > 64) {Util.writeln(""); count = 0;}
			Util.write(".");
			
			if (brendaMap.size() == 0) break; // abort loop when no more BRENDA terms to match
			
			Match bestMatch = matchByLabel(cloURI, cloLabel, brendaMap);
			if (bestMatch == null)
			{
				// save unmatched CLO terms here and process them at the end
				unmatchedCLOTerms.put(cloURI, cloLabel);
			}
			else cloIter.remove();
		}
		if (cloMap.size() > 0) unmatchedCLOTerms.putAll(cloMap);

		for (String brendaURI : brendaMap.keySet()) if (!brendaMatched.contains(brendaURI))
		{
			handleUnmatchedBRENDATerm(cloRoot, brendaURI, brendaMap.get(brendaURI));
		}

		// process unmatched CLO terms
		Util.writeln("unmatched CLO terms count is " + unmatchedCLOTerms.size());
		if (unmatchedCLOTerms.size() > 0 && describe) handleUnmatchedCLOTerms(unmatchedCLOTerms);

		if (curationFN == null)
		{
			Util.writeln("(skipping analysis of curated content; provide filename on command line to invoke)");
			return;
		}
		
		processCurated(new File(curationFN));*/
	}	

	/*private void writeHeader()
	{
		out.println("@prefix bao:   <http://www.bioassayontology.org/bao#> .");
		out.println("@prefix bat:   <http://www.bioassayontology.org/bat#> .");
		out.println("@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		out.println("@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .");
		out.println("@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .");
		out.println("@prefix owl:   <http://www.w3.org/2002/07/owl#> .");
		out.println("@prefix obo:   <http://purl.obolibrary.org/obo/> .");
		out.println();
		out.println("# auto-generated file of semantic directives that fold BRENDA terms into CLO ontology\n");
	}*/

	
	/*private void saveCellPairs() throws IOException
	{
		JSONArray list = new JSONArray();
		for (CellPair cell : pairs)
		{
			JSONObject json = new JSONObject();
			json.put("brendaURI", cell.brendaURI);
			json.put("brendaLabel", cell.brendaLabel);
			json.put("cloURI", cell.cloURI);
			json.put("cloLabel", cell.cloLabel);
			list.put(json);
		}
	
		try (Writer wtr = new FileWriter("cellpairs.json"))
		{
			wtr.write(list.toString(2));
		}
	}*/
	
	private void flattenBranch(Map<String, String> map, Vocabulary.Branch branch, boolean reqBrenda)
	{
		boolean isBrenda = ModelSchema.collapsePrefix(branch.uri).startsWith("obo:BTO_");
		if (isBrenda == reqBrenda) map.put(branch.uri, branch.label);
		for (Vocabulary.Branch child : branch.children) flattenBranch(map, child, reqBrenda);
	}

	// return best match if one exists or null otherwise
	// if a best match is found, then remove it from BRENDA map
	/*private Match matchByLabel(String uri, String label, Map<String, String> map)
	{
		Pattern pat = Pattern.compile("(.+)\\s+cell(\\s+line)?\\s*$");
		Matcher mat1 = pat.matcher(label);
		String label1 = mat1.matches() ? mat1.group(1) : label;

		List<Match> possibleMatches = new ArrayList<>();
		for (Map.Entry<String, String> entry : map.entrySet())
		{
			Matcher mat2 = pat.matcher(entry.getValue());
			String label2 = mat2.matches() ? mat2.group(1) : entry.getValue();

			Match m = new Match();
			m.uri = entry.getKey();
			m.label = entry.getValue(); // save original label
			m.score = stringSimilarity(label1, label2);

			possibleMatches.add(m);
		}

		if (possibleMatches.size() == 0) return null;
		possibleMatches.sort((m1, m2) -> m1.score - m2.score);

		Match bestMatch = possibleMatches.get(0);
		String uri1 = ModelSchema.collapsePrefix(uri);
		String uri2 = ModelSchema.collapsePrefix(bestMatch.uri);
		map.remove(bestMatch.uri);

		// list of actual matches logged 
		JSONObject json = new JSONObject();
		json.put("uri1", uri1);
		json.put("label1", label);
		json.put("uri2", uri2);
		json.put("label2", bestMatch.label);
		Util.writeln(json.toString() + " / score=" + bestMatch.score);

		handleMatchedTerms(uri1, label, uri2, bestMatch.label);

		return bestMatch;
	}*/

	// uri1 and uri2 should already be collapsed
	private void handleMatchedTerms(String cloURI, String cloLabel, String brendaURI, String brendaLabel, Map<String, Integer> dupCounts)
	{
		//if (!brendaMatched.add(uri2)) return; // greedily select 1st matching CLO term
		String cloPfx = ModelSchema.collapsePrefix(cloURI), brendaPfx = ModelSchema.collapsePrefix(brendaURI);

		String cloDesc = vocab.getDescr(cloURI), brendaDesc = vocab.getDescr(brendaURI);

		if (Util.notBlank(brendaDesc) && !StringUtils.equals(cloDesc, brendaDesc))
		{
			String newDesc = !StringUtils.isEmpty(cloDesc) ? (cloDesc + " " + brendaDesc) : brendaDesc;
			out.println("# CLO [" + cloLabel + "] paired with BRENDA [" + brendaLabel + "]");
			if (dupCounts != null)
			{
				int cloCount = dupCounts.get(cloURI), brendaCount = dupCounts.get(brendaURI);
				if (cloCount > 1) out.println("# **** CLO value pair count: " + cloCount);
				if (brendaCount > 1) out.println("# **** BRENDA value pair count: " + brendaCount);
			}
			out.println(cloPfx + " obo:IAO_0000115 \"" + escapeTurtleString(newDesc) + "\" .");
			out.println(cloPfx + " bat:pairedWith " + brendaPfx + " .");
		}
		out.println(brendaPfx + " a bat:eliminated .");
		out.println();
		out.flush();
	}
	
	/*
	// reparent BRENDA term to temporary location in CLO 
	private void handleUnmatchedBRENDATerm(Vocabulary.Branch cloRoot, String uriExp, String label)
	{
		// term with a label containing the keyword "cell" by defualt is re-parented to the "BRENDA cell parent" in CLO
		// otherwise, term is re-parented to the "BRENDA" tissue parent" in CLO 

		Pattern pat = Pattern.compile("\\s+cell\\s+");
		Matcher mat = pat.matcher(label);

		// create artificial branches on-demand for BRENDA cell and tissue terms
		if (mat.matches() && !cloMap.containsKey(URI_FOR_CELL_LINES))
		{
			String branchLabel = "BRENDA cell lines";

			out.println("# create BRENDA cell branch and make its parent the CLO root");
			out.println(URI_FOR_CELL_LINES + " rdfs:label \"" + escapeTurtleString(branchLabel) + "\" ;");
			out.println("\trdfs:subClassOf " + URI_CLO_ROOT + " .");

			cloMap.put(URI_FOR_CELL_LINES, branchLabel);
		}
		else if (!mat.matches() && !cloMap.containsKey(URI_FOR_TISSUES))
		{
			String branchLabel = "BRENDA tissue lines";

			out.println("# create BRENDA tissue branch and make its parent the CLO root");
			out.println(URI_FOR_TISSUES + " rdfs:label \"" + escapeTurtleString(branchLabel) + "\" ;");
			out.println("\trdfs:subClassOf " + URI_CLO_ROOT + " .");

			cloMap.put(URI_FOR_TISSUES, branchLabel);
		}

		String uriPfx = ModelSchema.collapsePrefix(uriExp);
		String brendaDesc = vocab.getDescr(uriExp);
		String end = !StringUtils.isEmpty(brendaDesc) ? ";" : ".";

		if (mat.matches())
		{
			out.println("# re-parent BRENDA cell term to CLO branch");
			out.println(uriPfx + " bat:notSubClass <" + URI_BRENDA_ROOT + "> ;");
			out.println("\trdfs:subClassOf " + URI_FOR_CELL_LINES + " " + end);
		}
		else
		{
			out.println("# re-parent BRENDA tissue term to CLO branch");
			out.println(uriPfx + " bat:notSubClass <" + URI_BRENDA_ROOT + "> ;");
			out.println("\trdfs:subClassOf " + URI_FOR_TISSUES + " " + end);
		}

		if (!StringUtils.isEmpty(brendaDesc))
			out.println("\tobo:IAO_0000115 \"" + escapeTurtleString(brendaDesc) + "\" .");

		out.println(""); // trailing blank line
		out.flush();
	}

	// uri should be fully expanded
	private void handleUnmatchedCLOTerms(NavigableMap<String, String> unmatchedCLOTerms)
	{
		out.println("\n################################################################################");
		out.println("# following CLO terms are missing descriptions");
		out.println("# please correct and uncomment as needs be");
		out.println("################################################################################\n");

		for (String uriExp : unmatchedCLOTerms.keySet())
		{
			String cloDesc = vocab.getDescr(uriExp);
			if (StringUtils.isEmpty(cloDesc))
			{
				String cloLabel = unmatchedCLOTerms.get(uriExp);
				out.println("\n# " + cloLabel); // even better if we had term hierarchy in comment

				String uriPfx = ModelSchema.collapsePrefix(uriExp);
				if (StringUtils.equals(uriPfx, uriExp))
				{
					// this obscure case happens when uri cannot be abbreviated
					out.println("# <" + uriPfx + "> obo:IAO_0000115 \"\" .");
				}
				else
				{
					out.println("# " + uriPfx + " obo:IAO_0000115 \"\" .");
				}
			}
		}
		out.flush();
	}*/
	
	private void processCurated(File f) throws IOException
	{
		Set<String> cellTerms = new HashSet<>();
		for (CellPair cell : pairs) {cellTerms.add(cell.uri1); cellTerms.add(cell.uri2);}
	
		int[] toFix = new int[1];
		List<String[]> lines = new ArrayList<>();
		lines.add(new String[]{"AID", "URI", "label"});
	
		try (ZipFile zipFile = new ZipFile(f))
		{
			zipFile.stream().forEach(ze ->
			{
				JSONObject json = null;
				try
				{
					 json = new JSONObject(new JSONTokener(new InputStreamReader(zipFile.getInputStream(ze))));
				}
				catch (IOException ex) {throw new UncheckedIOException(ex);}
			
				String uniqueID = json.optString("uniqueID");
				if (!uniqueID.startsWith("pubchemAID:")) return;
				String aid = uniqueID.substring(11);
				JSONObject[] annotList = json.optJSONArrayEmpty("annotations").toObjectArray();
				
				Set<String> terms = new HashSet<>();
				for (int n = 0; n < annotList.length; n++)
				{
					String valueURI = annotList[n].optString("valueURI");
					if (Util.isBlank(valueURI)) continue;
					if (cellTerms.contains(valueURI)) terms.add(valueURI);
				}
				
				int count = 0;
				for (CellPair cell : pairs)
				{
					boolean has1 = terms.contains(cell.uri1), has2 = terms.contains(cell.uri2);
					if ((!has1 && !has2) || (has1 && has2)) continue;
					
					if (count == 0) Util.writeln("[" + ze.getName() + "] {" + uniqueID + "} #annotations=" + annotList.length);
					count++;
					
					String oldURI = null, oldLabel = null, newURI = null, newLabel = null;
					if (has1 && !has2)
					{
						oldURI = cell.uri1;
						oldLabel = cell.label1;
						newURI = cell.uri2;
						newLabel = cell.label2;
					}					
					else
					{
						oldURI = cell.uri2;
						oldLabel = cell.label2;
						newURI = cell.uri1;
						newLabel = cell.label1;
					}					

					Util.writeln("    " + oldLabel + " <" + ModelSchema.collapsePrefix(oldURI) + "> " +
							     "--> " + newLabel + " <" + ModelSchema.collapsePrefix(newURI) + ">");
							     
					lines.add(new String[]{aid, newURI, newLabel});
				}
				if (count > 0) toFix[0]++;
			});
		}
		catch (UncheckedIOException e)
		{
			throw new IOException(e);
		}
		
		Util.writeln("\nAssays needing changes: " + toFix[0]);
		
		File of = new File("term_additions.tsv");
		Util.writeln("Writing these to: [" + of.getAbsolutePath() + "]");
		try (Writer wtr = new FileWriter(of))
		{
			for (String[] bits : lines) wtr.write(String.join("\t", bits) + "\n");
		}
	}

	private static String getProgramName()
	{
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StackTraceElement main = stack[stack.length - 1];
		String mainClass = main.getClassName();
		return mainClass;
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
