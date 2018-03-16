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

package com.cdd.bao.importer;

import static com.cdd.bao.importer.KeywordMapping.*;
import com.cdd.bao.util.*;
import com.cdd.bao.template.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.apache.commons.lang3.*;
import org.json.*;

/*
	Importing of controlled vocabulary: allows external content, of the keyword variety, to be imported into a schema. It presumes
	that the original import was a table with named columns, or a list of keyword/value pairs.

	The raw material needs to be preformatted into a JSON file with two object properties:
		.columns: a list of "columns" (aka "keywords") that will correspond to assignment categories
		.rows: an array of objects, each of which contains the {column:value} pairs that define the assay
		
	The mapping file is read as a starting point, and should ideally sufficient to perform all of the translations. Whenever it is not, 
	the import process gets interactive, offering to add a mapping category and write the modified file.
*/

public class ImportControlledVocab
{
	private String srcFN, mapFN, dstFN, schemaFN, vocabFN, hintFN;
	
	private JSONArray srcColumns, srcRows;
	private KeywordMapping map;
	private Schema schema;
	private Schema.Assignment[] assignments; // flattened
	private SchemaVocab schvoc;
	private Map<Schema.Assignment, SchemaTree> treeCache = new HashMap<>();
	
	private enum Directive
	{
		OK, // nothing changed
		MODIFIED, // content modified
		DELETE, // should be removed from list
	}

	private Scanner scanner = new Scanner(System.in);

	private Map<String, String> hints = new HashMap<>(); // keyword to URI
	private Map<String, String[]> invHints = new HashMap<>(); // URI to keyword(s)

	// ------------ public methods ------------

	public ImportControlledVocab(String srcFN, String mapFN, String dstFN, String schemaFN, String vocabFN, String hintFN)
	{
		this.srcFN = srcFN;
		this.mapFN = mapFN;
		this.dstFN = dstFN;
		this.schemaFN = schemaFN;
		this.vocabFN = vocabFN;
		this.hintFN = hintFN;
	}
	
	public void exec() throws IOException, JSONException
	{
		File f = new File(srcFN);
		if (!f.exists()) throw new IOException("Source file not found: " + f.getCanonicalPath());
		Reader rdr = new FileReader(srcFN);
		JSONObject json = new JSONObject(new JSONTokener(rdr));
		rdr.close();
		if (!json.has("columns") || !json.has("rows")) throw new IOException("Source JSON must have 'columns' and 'rows'.");
		srcColumns = json.getJSONArray("columns");
		srcRows = json.getJSONArray("rows");
		
		map = new KeywordMapping(mapFN);
		Util.writeln("Mapping file:");
		Util.writeln("    # identifiers = " + map.identifiers.size());
		Util.writeln("    # textblocks = " + map.textBlocks.size());
		Util.writeln("    # properties = " + map.properties.size());
		Util.writeln("    # values = " + map.values.size());
		Util.writeln("    # literals = " + map.literals.size());
		Util.writeln("    # references = " + map.references.size());
		Util.writeln("    # assertions = " + map.assertions.size());
		
		schema = ModelSchema.deserialise(new File(schemaFN));
		assignments = schema.getRoot().flattenedAssignments();
		
		InputStream istr = new FileInputStream(vocabFN);
		schvoc = SchemaVocab.deserialise(istr, new Schema[]{schema});
		istr.close();
		
		for (SchemaVocab.StoredTree stored : schvoc.getTrees()) treeCache.put(stored.assignment, stored.tree);
		
		if (hintFN != null)
		{
			rdr = new FileReader(hintFN);
			JSONObject jsonHints = new JSONObject(new JSONTokener(rdr));
			rdr.close();
			for (String key : jsonHints.keySet()) 
			{
				String uri = ModelSchema.expandPrefix(jsonHints.getString(key));
				hints.put(key, uri);
				invHints.put(uri, ArrayUtils.add(invHints.get(uri), key));
			}
			Util.writeln("    # hints = " + hints.size());
		}
		
		checkMappingProperties();
		for (int n = 0; n < srcColumns.length(); n++) matchColumn(srcColumns.getString(n));
		for (int n = 0; n < srcRows.length(); n++)
		{
			Util.writeln("---- Row#" + (n + 1) + " ----");
			JSONObject row = srcRows.getJSONObject(n);
			for (String key : row.keySet())
			{
				if (map.findIdentifier(key) != null) continue;
				Property prop = map.findProperty(key);
				if (prop == null || Util.isBlank(prop.propURI)) continue; // passed on the opportunity to map
				String val = row.optString(key, "");
				if (map.findValue(key, val) != null || map.findLiteral(key, val) != null || map.findReference(key, val) != null) continue; // already mapped
				matchValue(prop, key, val);
			}
		}
		
		Util.writeln("Saving mapping file...");
		map.save();
		
		writeMappedAssays();
		
		Util.writeln("Done.");
	}

	// ------------ private methods ------------
	
	// go through the current list of definitions and make sure they are legit
	private void checkMappingProperties() throws IOException
	{
		for (int n = 0; n < map.properties.size(); n++)
		{
			Directive d = verifyMapAssn(map.properties.get(n), true);
			if (d == Directive.MODIFIED) map.save();
			else if (d == Directive.DELETE) 
			{
				Util.writeln("** deleting stale property: " + map.properties.get(n).regex);
				map.properties.remove(n); 
				n--;
				map.save();
			}
		}
		for (int n = 0; n < map.values.size(); n++)
		{
			Directive d = verifyMapAssn(map.values.get(n), false);
			if (d == Directive.MODIFIED) map.save();
			else if (d == Directive.DELETE) 
			{
				Util.writeln("** deleting stale value: " + map.values.get(n).regex);
				map.values.remove(n); 
				n--;
				map.save();
			}
		}
		for (int n = 0; n < map.literals.size(); n++)
		{
			Directive d = verifyMapAssn(map.literals.get(n), false);
			if (d == Directive.MODIFIED) map.save();
			else if (d == Directive.DELETE) 
			{
				Util.writeln("** deleting stale literal: " + map.literals.get(n).regex);
				map.literals.remove(n); 
				n--; 
				map.save();
			}
		}
	}
	
	// check to see that a mapping to an assignment property is legit, and do something if not
	private Directive verifyMapAssn(MapAssn mapAssn, boolean isProperty)
	{
		// if the value/literal does not match any columns that in turn match a property, then it is stale: delete it
		if (!isProperty)
		{
			boolean something = false;
			for (String col : srcColumns.toStringArray()) if (map.matchesName(mapAssn, col))
				if (map.findProperty(col) != null) {something = true; break;}
			if (!something) return Directive.DELETE;
		}
	
		return Directive.OK;
	}
	
	// ensures that a column name gets a chance to be mapped to an assignment
	private void matchColumn(String colName) throws IOException
	{
		if (Util.isBlank(colName)) throw new IOException("Blank column name in source: this needs to be fixed.");
		
		// anything found, assume it's OK
		if (map.findIdentifier(colName) != null) return;
		if (map.findProperty(colName) != null) return; 
		
		// text blocks are not mutually exclusive
		//if (map.findTextBlock(colName) != null) return;
		
		int[] assnidx = mostSimilarAssignments(colName);

		// display column and some representative examples		
		Util.writeln("\nUnmapped column: [" + colName + "]");
		for (int n = 0, ncases = 0; n < srcRows.length() && ncases < 5; n++)
		{
			String val = srcRows.getJSONObject(n).optString(colName);
			if (Util.isBlank(val)) continue;
			Util.writeln("    value example #" + (++ncases) + ": [" + val + "]");
		}

		// require something be done with it
		Util.writeln("  [ENTER] = ignore temporarily");
		Util.writeln("  [0] = ignore permanently");
		for (int n = 0; n < 9 && n < assnidx.length; n++)
		{
			Schema.Assignment assn = assignments[assnidx[n]];
			Util.write("  [" + (n + 1) + "] = {" + assn.name + "} <" + ModelSchema.collapsePrefix(assn.propURI) + ">");
			String[] groups = assn.groupLabel();
			for (int i = 0; i < groups.length; i++) Util.write(" / " + groups[i]);
			Util.writeln();
		}
		Util.writeln("  or: enter a URI or abbreviation, or ;title to map to text");
		
		String choice = getChoice(Math.min(assnidx.length, 9), false);
		
		if (Util.isBlank(choice)) return; // temporary skip: do nothing
	
		int num = Util.safeInt(choice, -1);
		
		// create a skip-this-always
		if (num == 0)
		{
			Util.writeln("Permanently excluding property [" + colName + "] from mapping.");
			map.properties.add(Property.create(colName, null, null));
			map.save();
			return;
		}
		
		// map to a given property
		if (num > 0 && num <= assnidx.length)
		{
			Schema.Assignment assn = assignments[assnidx[num - 1]];
			Property prop = Property.create(colName, assn.propURI, assn.groupNest());
			Util.writeln("Mapping property to: [" + assn.name + "] <" + ModelSchema.collapsePrefix(assn.propURI) + ">");
			String[] groupLabel = assn.groupLabel();
			for (int n = 0; n < prop.groupNest.length; n++)
				Util.writeln("                    / " + groupLabel[n] + " <" + prop.groupNest[n] + ">");
			map.properties.add(prop);
			map.save();
			return;
		}
		
		// add a text mapping directive
		if (choice.startsWith(";"))
		{
			map.textBlocks.add(TextBlock.create(colName, choice.substring(1)));
			map.save();
			return;
		}
		
		// it's a URI: track it down to an actual assignment, or continue forever until it happens
		while (Util.notBlank(choice))
		{
			String abbrev = ModelSchema.collapsePrefix(choice), uri = ModelSchema.expandPrefix(choice);
			Util.writeln("Entered property URI: <" + abbrev + ">");
		
			List<Schema.Assignment> assnList = new ArrayList<>();
			for (Schema.Assignment assn : assignments) if (assn.propURI.equals(uri)) assnList.add(assn);
			
			if (assnList.size() == 1)
			{
				Schema.Assignment assn = assnList.get(0);
				Util.writeln("Matched exactly one assignment: [" + assn.name + "]");
				map.properties.add(Property.create(colName, assn.propURI, assn.groupNest()));
				map.save();
				return;
			}
			
			if (assnList.size() == 0)
			{
				Util.writeln("The URI does not match any of the assignments; try again");
				choice = getChoice(0, false);
			}
			else
			{
				Util.writeln("The URI matched more than one assignment:");
				for (int n = 0; n < assnList.size(); n++)
				{
					Schema.Assignment assn = assnList.get(n);
					Util.write("  [" + (n + 1) + "] = {" + assn.name + "} <" + ModelSchema.collapsePrefix(assn.propURI) + ">");
					String[] groups = assn.groupLabel();
					for (int i = 0; i < groups.length; i++) Util.write(" / " + groups[i]);
					Util.writeln();
				}
				choice = getChoice(assnList.size(), false);
				num = Util.safeInt(choice);
				if (num > 0 && num <= assnList.size())
				{
					Schema.Assignment assn = assnList.get(num - 1);
					Util.writeln("Adding assignment mapping.");
					map.properties.add(Property.create(colName, assn.propURI, assn.groupNest()));
					map.save();
					return;
				}
			}
		}
	}
	
	// ensures that a value gets a chance to be mapped to a URI or marked as a literal
	private void matchValue(Property prop, String key, String value) throws IOException
	{
		String[] groupNest = KeywordMapping.expandPrefixes(prop.groupNest);
		Schema.Assignment[] assnList = schema.findAssignmentByProperty(ModelSchema.expandPrefix(prop.propURI), groupNest);
		if (assnList.length == 0) return;
		Schema.Assignment assn = assnList[0];
	
		Util.writeln();
		Util.writeln("Value: column [" + key + "]");
		Util.writeln("       value  [" + value + "]");
		Util.writeln("Assignment: [" + assn.name + "] <" + ModelSchema.collapsePrefix(assn.propURI) + ">");
		String[] groupLabel = assn.groupLabel();
		for (int n = 0; n < prop.groupNest.length; n++)
		Util.writeln("           / " + groupLabel[n] + " <" + prop.groupNest[n] + ">");

		SchemaTree.Node[] nodes = mostSimilarTerms(assn, value);

		// require something be done with it
		Util.writeln("  [ENTER] = ignore temporarily");
		Util.writeln("  [0] = ignore permanently");
		for (int n = 0; n < 9 && n < nodes.length; n++)
		{
			Util.writeln("  [" + (n + 1) + "] = {" + nodes[n].label + "} <" + ModelSchema.collapsePrefix(nodes[n].uri) + ">");
			for (SchemaTree.Node look = nodes[n].parent; look != null; look = look.parent)
				Util.writeln("       / " + look.label + " <" + ModelSchema.collapsePrefix(look.uri) + ">");
		}
		Util.writeln("  [;] = pass this value through as literal");
		Util.writeln("  [*] = pass the whole assignment as literal");
		Util.writeln("  [:pfx:] = map to a reference ID with given prefix");
		Util.writeln("  or: enter a URI or abbreviation");
		
		String choice = getChoice(Math.min(nodes.length, 9), true);

		if (Util.isBlank(choice)) return; // temporary skip: do nothing
		if (choice.equals(";"))
		{
			Util.writeln("Marking value [" + key + "]:[" + value + "] as a literal.");
			map.literals.add(Literal.create(key, value, prop.propURI, prop.groupNest));
			map.save();
			return;
		}
		if (choice.equals("*"))
		{
			Util.writeln("Marking value [" + key + "]:[" + value + "] as a literal.");
			map.literals.add(Literal.create(key, null, prop.propURI, prop.groupNest));
			map.save();
			return;
		}
		if (choice.length() >= 3 && choice.startsWith(":") && choice.endsWith(":"))
		{
			String pfx = choice.substring(1);
			int fluff = 0;
			for (; fluff < value.length(); fluff++)
			{
				char ch = value.charAt(fluff);
				if (ch >= '1' && ch <= '9') break;
			}
			String regex = "^" + value.substring(0, fluff) + "(.*)";
			Util.writeln("Marking value [" + key + "]:[" + value + "] as a reference ID, prefix '" + pfx + "'.");
			map.references.add(Reference.create(key, regex, pfx, prop.propURI, prop.groupNest));
			map.save();
			return;
		}

		int num = Util.safeInt(choice, -1);

		// create a skip-this-always
		if (num == 0)
		{
			Util.writeln("Permanently excluding value [" + key + "]:[" + value + "] from mapping.");
			map.values.add(Value.create(key, value, null, prop.propURI, prop.groupNest));
			map.save();
			return;
		}

		// map to a given term
		if (num > 0 && num <= nodes.length)
		{
			SchemaTree.Node node = nodes[num - 1];
			Util.writeln("Mapping value [" + key + "]:[" + value + "] to {" + node.label + "} <" + ModelSchema.collapsePrefix(node.uri) + ">");
			map.values.add(Value.create(key, value, node.uri, prop.propURI, prop.groupNest));
			map.save();
			return;
		}
		
		// it's a URI: track it down to an actual assignment, or continue forever until it happens
		while (Util.notBlank(choice))
		{
			String abbrev = ModelSchema.collapsePrefix(choice), uri = ModelSchema.expandPrefix(choice);
			Util.writeln("Entered property URI: <" + abbrev + ">");
			
			for (SchemaTree.Node node : nodes) if (node.uri.equals(uri))
			{
				Util.writeln("Mapping value [" + key + "]:[" + value + "] to {" + node.label + "} <" + ModelSchema.collapsePrefix(node.uri) + ">");
				map.values.add(Value.create(key, value, node.uri, prop.propURI, prop.groupNest));
				map.save();
				return;
			}
		
			Util.writeln("The URI does not match any of the terms in the tree; try again");
			choice = getChoice(0, false);
		}		
	}
	
	// requires that the user picks something, which is either blank, a number (from 0..highNum) or a URI
	private String getChoice(int highNum, boolean allowStar) throws IOException
	{
		String choice = null;
		while (true)
		{
			Util.write("Choose: ");
			choice = scanner.nextLine();
			int num = Util.safeInt(choice, -1);
			if (Util.isBlank(choice) || num >= 0 && num <= highNum) break;
			if (ModelSchema.expandPrefix(choice).startsWith("http://")) break;
			if (choice.startsWith(";")) break;
			if (choice.length() >= 3 && choice.startsWith(":") && choice.endsWith(":")) break;
			if (allowStar && choice.equals("*")) break;
			Util.write("Invalid; try again: ");
		}
		return choice;
	}
	
	// given an arbitrary name, looks for the assignments with the most similar label
	private int[] mostSimilarAssignments(String name)
	{
		int[] sim = new int[assignments.length];
		for (int n = 0; n < assignments.length; n++) sim[n] = stringSimilarity(name, assignments[n].name);
		
		Integer[] idx = new Integer[assignments.length];
		for (int n = 0; n < assignments.length; n++) idx[n] = n;
		Arrays.sort(idx, (i1, i2) -> sim[i1] - sim[i2]);
		
		/*Util.writeln("["+name+"]:");
		for (int n = 0; n < idx.length && n < 50; n++)
			Util.writeln("sim="+sim[idx[n]]+" : [" + assignments[idx[n]].name + "]");*/
		
		return ArrayUtils.toPrimitive(idx);
	}
	
	// given an assignment and a name, pull out the branch and rank the contents in terms of similarity to the name
	private SchemaTree.Node[] mostSimilarTerms(Schema.Assignment assn, String name)
	{
		if (assn.suggestions != Schema.Suggestions.FULL) return new SchemaTree.Node[0];
	
		SchemaTree.Node[] nodes = treeCache.get(assn).getFlat();
		
		int[] sim = new int[nodes.length];
		for (int n = 0; n < nodes.length; n++) 
		{
			sim[n] = stringSimilarity(name, nodes[n].label);

			// if there are hints, give each one a chance to 
			String[] hintKeys = invHints.get(nodes[n].uri);
			if (hintKeys != null) for (String key : hintKeys)
			{
				sim[n] = Math.min(sim[n], stringSimilarity(name, key));
			}
		}

		Integer[] idx = new Integer[nodes.length];
		for (int n = 0; n < nodes.length; n++) idx[n] = n;
		Arrays.sort(idx, (i1, i2) -> sim[i1] - sim[i2]);

		SchemaTree.Node[] ret = new SchemaTree.Node[nodes.length];
		for (int n = 0; n < nodes.length; n++) ret[n] = nodes[idx[n]];
		return ret;		
	}
	
	// returns a measure of string similarity, used to pair controlled vocabulary names with ontology terms; 0=perfect
	private int stringSimilarity(String str1, String str2)
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
	
	// write everything as a ZIP file (with JSON formatted assays, compatible with BioAssay Express)
	private void writeMappedAssays() throws IOException
	{
		File f = new File(dstFN);
		Util.writeln("Writing to: " + f.getCanonicalPath());
		
		ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(f));

		for (int n = 0; n < srcRows.length(); n++)
		{
			JSONObject json = null;
			try {json = map.createAssay(srcRows.getJSONObject(n), schema, treeCache);}
			catch (Exception ex) {zip.close(); throw new IOException("Failed to translate assay at row #" + (n + 1), ex);}
			
			if (!json.has("uniqueID"))
			{
				Util.writeln("** Row#" + (n + 1) + " is missing an identifier: cannot proceed.");
				break;
			}
			
			String fn = "assay_" + (n + 1) + "_";
			for (char ch : json.getString("uniqueID").toCharArray())
			{
				if (ch == ' ') fn += ' ';
				else if (ch == '_' || ch == ':' || (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) fn += ch;
				else fn += '-';
			}
			
			zip.putNextEntry(new ZipEntry(fn + ".json"));
			zip.write(json.toString(2).getBytes());
			zip.closeEntry();
		}

		zip.close();
	}
}
