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
import org.apache.commons.cli.*;
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

	private final static String URI_BRENDA_ROOT = "http://dto.org/DTO/DTO_000";
	private final static String URI_CLO_ROOT = "obo:CLO_0000001";
	
	// XXX contrived branches rooted at CLO and used to house BRENDA cell and tissue terms
	// XXX note these closely resemble provisional terms in BAE
	private final static String URI_FOR_CELL_LINES = "obo:CLO_9999998";
	private final static String URI_FOR_TISSUES = "obo:CLO_9999999";

	private List<CellPair> pairs = new ArrayList<>();
	private Set<String> skipSet = new HashSet<>();
	private Map<String, String> brendaMap = new HashMap<>(), cloMap = new HashMap<>(); // uri-to-label
	private Set<String> brendaMatched = new HashSet<>(); // set of BRENDA terms that matched at least one CLO term

	// constructor parameters parsed from command-line
	private boolean readpairs;
	private boolean describe;
	private PrintWriter outWriter;
	private String curationFN;
	private int maxScore;

	public static void main(String[] args)
	{
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);

		Option readpairsOpt = Option.builder().longOpt("readpairs").desc("When specified, first read file with common pairs between BRENDA and CLO ontologies; by default, do not read pairs.").optionalArg(true).build();
		Option describeOpt = Option.builder().longOpt("describe").desc("When specified, output directives for CLO terms that lack descriptions; by default, do not do this.").optionalArg(true).build();
		Option curateOpt = Option.builder().longOpt("curate").desc("Path to zip file containing curated assays.").hasArg().build();
		Option scoreOpt = Option.builder().longOpt("score").desc("Scoring sensitivity. Roughly, ignore matches with Levenshtein distances in excess of this score.").hasArg().build();
		Option outfileOpt = Option.builder().longOpt("outfile").desc("Save semantic directives that make cell line corrections to the named file.").hasArg().required().build();

		Options options = new Options();
		options.addOption(readpairsOpt);
		options.addOption(describeOpt);
		options.addOption(curateOpt);
		options.addOption(scoreOpt);
		options.addOption(outfileOpt);

		CommandLineParser parser = new DefaultParser();
		CommandLine cmdLine = null;
		try {cmdLine = parser.parse(options, args);}
		catch (ParseException pe)
		{
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(getProgramName(), options);
			return;
		}

		boolean readpairs = cmdLine.hasOption("readpairs");
		boolean describe = cmdLine.hasOption("describe");

		int maxScore = 1;
		if (cmdLine.hasOption("score"))
		{
			maxScore = Integer.parseInt(cmdLine.getOptionValue("score"));
		}

		String curationFN = null;
		if (cmdLine.hasOption("curate"))
		{
			curationFN = cmdLine.getOptionValue("curate");
		}

		File outfile = null;
		if (cmdLine.hasOption("outfile"))
		{
			outfile = new File(cmdLine.getOptionValue("outfile"));
		}

		if (outfile != null && outfile.exists() && !outfile.isFile())
		{
			System.err.println("Please specify a valid file location for -outfile.");
			return;
		}

		Util.writeln("Cell Line Fix");
		try {new CellLineFix(readpairs, describe, outfile, curationFN, maxScore).exec();}
		catch (Exception ex) {ex.printStackTrace();}
		Util.writeln("Done.");
	}

	public CellLineFix(boolean readpairs, boolean describe, File outfile, String curationFN, int maxScore) throws FileNotFoundException
	{
		this.readpairs = readpairs;
		this.describe = describe;
		this.outWriter = new PrintWriter(outfile);
		this.curationFN = curationFN;
		this.maxScore = maxScore;
	}

	public void exec() throws Exception
	{
		writeHeader();

		if (readpairs)
		{
			loadCellPairs();
			Util.writeln("Predefined pairs: " + pairs.size() + ", skip: " + skipSet.size());
		}

		Util.writeln("Loading vocab...");
		vocab = new Vocabulary("data/ontology", null);
		Util.writeln("... properties: " + vocab.numProperties() + ", values: " + vocab.numValues());		
		
		Vocabulary.Hierarchy hier = vocab.getValueHierarchy();
		Vocabulary.Branch brendaRoot = hier.uriToBranch.get(ModelSchema.expandPrefix(URI_BRENDA_ROOT));
		Vocabulary.Branch cloRoot = hier.uriToBranch.get(ModelSchema.expandPrefix(URI_CLO_ROOT));
		
		for (Vocabulary.Branch br : brendaRoot.children) flattenBranch(brendaMap, br, true);
		for (Vocabulary.Branch br : cloRoot.children) flattenBranch(cloMap, br, false);
		
		Util.writeln("BRENDA terms: " + brendaMap.size());
		Util.writeln("CLO terms:    " + cloMap.size());
		
		for (CellPair cell : pairs)
		{
			brendaMap.remove(cell.uri1);
			brendaMap.remove(cell.uri2);
			cloMap.remove(cell.uri1);
			cloMap.remove(cell.uri2);
		}

		// house unmapped CLO terms here
		NavigableMap<String, String> unmatchedCLOTerms = new TreeMap<>();

		// iterate over remaining CLO pairs, look for possible BRENDA matches
		int count = 0;
		for (Map.Entry<String, String> entry : cloMap.entrySet())
		{
			String cloURI = entry.getKey(), cloLabel = entry.getValue();
			if (skipSet.contains(cloURI)) continue;
			if (++count > 64) {Util.writeln(""); count = 0;}
			Util.write(".");
			
			List<Match> matches = matchByLabel(cloURI, cloLabel, brendaMap);
			if (matches.size() <= 0)
			{
				// save unmatched CLO terms here and process them at the end
				unmatchedCLOTerms.put(cloURI, cloLabel);
			}
		}

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
		
		processCurated(new File(curationFN));

		outWriter.flush();
	}

	private void writeHeader()
	{
		outWriter.println("# auto-generated file of semantic directives that fold BRENDA terms into CLO ontology\n");
		outWriter.println("@prefix bao:   <http://www.bioassayontology.org/bao#> .");
		outWriter.println("@prefix bat:   <http://www.bioassayontology.org/bat#> .");
		outWriter.println("@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .");
		outWriter.println("@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .");
		outWriter.println("@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .");
		outWriter.println("@prefix owl:   <http://www.w3.org/2002/07/owl#> .");
		outWriter.println("@prefix obo:   <http://purl.obolibrary.org/obo/> .");
		outWriter.println("");
	}

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
			for (int n = 0; n < list.length(); n++)
			{
				JSONObject json = list.getJSONObject(n);
				
				String skipURI = json.optString("skip");
				if (Util.notBlank(skipURI)) 
				{
					skipSet.add(ModelSchema.expandPrefix(skipURI)); 
					continue;
				}

				CellPair cell = new CellPair();
				cell.uri1 = ModelSchema.expandPrefix(json.getString("uri1"));
				cell.label1 = json.getString("label1");
				cell.uri2 = ModelSchema.expandPrefix(json.optString("uri2"));
				cell.label2 = json.optString("label2");
				
				if (Util.isBlank(cell.uri2))
				{
					skipSet.add(ModelSchema.expandPrefix(cell.uri1)); 
					continue;
				}
				
				pairs.add(cell);
			}
		}
	}
	
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

	private List<Match> matchByLabel(String uri, String label, Map<String, String> map)
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

		if (possibleMatches.size() == 0) return new ArrayList<>();
		possibleMatches.sort((m1, m2) -> m1.score - m2.score);

		// list of actual matches logged 
		List<Match> actualMatches = new ArrayList<>();

		int bestScore = possibleMatches.get(0).score;
		if (bestScore <= 2) // only consider matches when the best match has 2 or fewer character tweaks
		{
			Util.writeln();
			int count = 0;
			for (Match m : possibleMatches)
			{
				if (m.score > maxScore) break;

				String uri1 = ModelSchema.collapsePrefix(uri);
				String uri2 = ModelSchema.collapsePrefix(m.uri);

				JSONObject json = new JSONObject();
				json.put("uri1", uri1);
				json.put("label1", label);
				json.put("uri2", uri2);
				json.put("label2", m.label);
				Util.writeln(json.toString() + " / score=" + m.score);

				handleMatchedTerms(uri1, label, uri2, m.label);
				actualMatches.add(m);

				if (++count >= 10) break;
			}
		}
		return actualMatches;
	}

	// uri1 and uri2 should already be collapsed
	private void handleMatchedTerms(String uri1Pfx, String label1, String uri2Pfx, String label2)
	{
		String uri1Exp = ModelSchema.expandPrefix(uri1Pfx);
		String uri2Exp = ModelSchema.expandPrefix(uri2Pfx);
		String cloDesc = vocab.getDescr(uri1Exp), brendaDesc = vocab.getDescr(uri2Exp);
		brendaMatched.add(uri2Exp);

		if (!StringUtils.isEmpty(brendaDesc) && !StringUtils.equals(cloDesc, brendaDesc))
		{
			String newDesc = !StringUtils.isEmpty(cloDesc) ? (cloDesc + " " + brendaDesc) : brendaDesc;
			String escDesc = Util.escapeStringBaseDoubleQuote(newDesc);
			outWriter.println("# incorporate description from BRENDA term into CLO term and then delete BRENDA term");
			outWriter.println(uri1Pfx + " obo:IAO_0000115 \"" + escDesc + "\" .");
		}
		else
		{
			outWriter.println("# delete redundant BRENDA term");
		}
		outWriter.println(uri2Pfx + " a bat:eliminated .\n");
		outWriter.flush();
	}

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

			outWriter.println("# create BRENDA cell branch and make its parent the CLO root");
			outWriter.println(URI_FOR_CELL_LINES + " rdfs:label \"" + branchLabel + "\" ;");
			outWriter.println("\trdfs:subClassOf " + URI_CLO_ROOT + " .");

			cloMap.put(URI_FOR_CELL_LINES, branchLabel);
		}
		else if (!mat.matches() && !cloMap.containsKey(URI_FOR_TISSUES))
		{
			String branchLabel = "BRENDA tissue lines";

			outWriter.println("# create BRENDA tissue branch and make its parent the CLO root");
			outWriter.println(URI_FOR_TISSUES + " rdfs:label \"" + branchLabel + "\" ;");
			outWriter.println("\trdfs:subClassOf " + URI_CLO_ROOT + " .");

			cloMap.put(URI_FOR_TISSUES, branchLabel);
		}

		String uriPfx = ModelSchema.collapsePrefix(uriExp);
		String brendaDesc = vocab.getDescr(uriExp);
		String end = !StringUtils.isEmpty(brendaDesc) ? ";" : ".";

		if (mat.matches())
		{
			outWriter.println("# re-parent BRENDA cell term to CLO branch");
			outWriter.println(uriPfx + " bat:notSubClass <" + URI_BRENDA_ROOT + "> ;");
			outWriter.println("\trdfs:subClassOf " + URI_FOR_CELL_LINES + " " + end);
		}
		else
		{
			outWriter.println("# re-parent BRENDA tissue term to CLO branch");
			outWriter.println(uriPfx + " bat:notSubClass <" + URI_BRENDA_ROOT + "> ;");
			outWriter.println("\trdfs:subClassOf " + URI_FOR_TISSUES + " " + end);
		}

		if (!StringUtils.isEmpty(brendaDesc))
			outWriter.println("\tobo:IAO_0000115 \"" + brendaDesc + "\" .");

		outWriter.println(""); // trailing blank line
		outWriter.flush();
	}

	// uri should be fully expanded
	private void handleUnmatchedCLOTerms(NavigableMap<String, String> unmatchedCLOTerms)
	{
		outWriter.println("\n################################################################################");
		outWriter.println("# following CLO terms are missing descriptions");
		outWriter.println("# please correct and uncomment as needs be");
		outWriter.println("################################################################################\n");

		for (String uriExp : unmatchedCLOTerms.keySet())
		{
			String cloDesc = vocab.getDescr(uriExp);
			if (StringUtils.isEmpty(cloDesc))
			{
				String cloLabel = unmatchedCLOTerms.get(uriExp);
				outWriter.println("\n# " + cloLabel); // even better if we had term hierarchy in comment

				String uriPfx = ModelSchema.collapsePrefix(uriExp);
				if (StringUtils.equals(uriPfx, uriExp))
				{
					// this obscure case happens when uri cannot be abbreviated
					outWriter.println("# <" + uriPfx + "> obo:IAO_0000115 \"\" .");
				}
				else
				{
					outWriter.println("# " + uriPfx + " obo:IAO_0000115 \"\" .");
				}
			}
		}
		outWriter.flush();
	}

	// returns a measure of string similarity, used to pair controlled vocabulary names with ontology terms; 0=perfect
	public int stringSimilarity(String str1, String str2)
	{
		char[] ch1 = str1.toLowerCase().toCharArray();
		char[] ch2 = str2.toLowerCase().toCharArray();
		int sz1 = ch1.length, sz2 = ch2.length;
		if (sz1 == 0) return sz2;
		if (sz2 == 0) return sz1;

		int cost = ch1[sz1 - 1] == ch2[sz2 - 1] ? 0 : 1;
		int lev1 = levenshteinDistance(ch1, sz1 - 1, ch2, sz2) + 1;
		int lev2 = levenshteinDistance(ch1, sz1, ch2, sz2 - 1) + 1;
		int lev3 = levenshteinDistance(ch1, sz1 - 1, ch2, sz2 - 1) + cost;
		
		return Math.min(Math.min(lev1, lev2), lev3);
	}
	private int levenshteinDistance(char[] ch1, int sz1, char[] ch2, int sz2)
	{
		int[][] d = new int[sz1 + 1][];
		for (int i = 0; i <= sz1; i++)
		{
			d[i] = new int[sz2 + 1];
			d[i][0] = i;
		}
		for (int j = 1; j <= sz2; j++) d[0][j] = j;

		for (int j = 1; j <= sz2; j++) for (int i = 1; i <= sz1; i++)
		{
			int cost = ch1[i - 1] == ch2[j - 1] ? 0 : 1;
			d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
		}
		return d[sz1][sz2];
	}	
	
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
}
