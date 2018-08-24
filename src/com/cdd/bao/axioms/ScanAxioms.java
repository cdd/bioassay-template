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

package com.cdd.bao.axioms;

import com.cdd.bao.template.*;
import com.cdd.bao.util.*;

import static com.cdd.bao.axioms.AxiomVocab.*;

import java.io.*;
import java.util.*;

import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.json.*;

/*
	Looks through a collection of underlying ontology files and looks for axioms to pull out. Whenever
	possible, these are reformulated as "axiom rules", which is a boiled down format: this is quick to
	parse, and convenient for assisting other machine learning tools for annotating protocols.
	
	This is intended to be only run occasionally, from the command line, when the axioms from the underlying
	ontologies are thought to have changed since the last scan.
*/

public class ScanAxioms
{
	private OntModel ontology;
	private Schema schema;
	private SchemaVocab schvoc;

	private Map<String, String> uriToLabel = new HashMap<>();
	private Map<String, String> labelToURI = new HashMap<>();
	private Map<String, List<Statement>> anonStatements = new HashMap<>();

	private Map<String, Set<String>> onlyAxioms = new TreeMap<>();
	private Map<String, Set<String>> someAxioms = new TreeMap<>();
	private Map<String, Set<String>> inverseProperties = new TreeMap<>();

	private AxiomVocab axvoc = new AxiomVocab();

	/* not using this at the moment
	private static String[] redundantURIs = 
	{
		"http://www.bioassayontology.org/bao#BAO_0000035", "http://www.bioassayontology.org/bao#BAO_0000179",
		"http://www.bioassayontology.org/bao#BAO_0002202", "http://www.bioassayontology.org/bao#BAO_0000015",
		"http://www.bioassayontology.org/bao#BAO_0000026", "http://www.bioassayontology.org/bao#BAO_0000019",
		"http://www.bioassayontology.org/bao#BAO_0000248", "http://www.bioassayontology.org/bao#BAO_0000015",
		"http://www.bioassayontology.org/bao#BAO_0000264", "http://www.bioassayontology.org/bao#BAO_0000074",
		"http://www.bioassayontology.org/bao#BAO_0002202", "http://www.bioassayontology.org/bao#BAO_0003075"
	};
	public Set<String> redundantURISet = new HashSet<>(Arrays.asList(redundantURIs));*/

	private Map<String, Set<String>> axioms = new TreeMap<>();

	private Map<String, Integer> propTypeCount = new TreeMap<>();

	// ------------ public methods ------------

	public ScanAxioms()
	{
	}

	public void exec() throws OntologyException, JSONException, IOException
	{
		try
		{
			Util.writeln("Loading common assay template...");
			schema = Schema.deserialise(new File("data/template/schema.json"));

			Util.writeln("Loading vocabulary dump...");
			try (InputStream idump = new FileInputStream("data/template/vocab.dump"))
			{
				schvoc = SchemaVocab.deserialise(idump, new Schema[]{schema});
			}

			List<File> files = new ArrayList<>();
			// TODO: formalise which files get included; note that scanning them all can be slow
			for (File f : new File("data/ontology").listFiles()) if (f.getName().endsWith(".owl")) files.add(f);
			if (false)
				for (File f : new File("data/preprocessed").listFiles()) if (f.getName().endsWith(".owl")) files.add(f);
			else
				Util.writeln("** skipping files in [data/preprocessed]"); // (includes original DTO, which takes a long time)
				
			Util.writeln("# files to read: " + files.size());

			ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF);
			for (File f : files)
			{
				Util.writeln("    reading: " + f.getCanonicalPath());
				try (Reader rdr = new FileReader(f))
				{
					ontology.read(rdr, null);
				}
			}
		}
		catch (Exception ex) {throw new OntologyException(ex.getMessage());}

		Util.writeln("Collating values from schema...");
		Set<String> schemaValues = new HashSet<>();
		for (SchemaVocab.StoredTree stored : schvoc.getTrees())
			for (SchemaTree.Node node : stored.tree.getFlat())
				schemaValues.add(node.uri);

		Util.writeln("Read complete: counting triples...");
		long timeThen = new Date().getTime();
		int numTriples = 0;
		for (StmtIterator iter = ontology.listStatements(); iter.hasNext();)
		{
			Statement stmt = iter.next();
			numTriples++;
			long timeNow = new Date().getTime();
			if (timeNow > timeThen + 2000)
			{
				Util.writeln("    so far: " + numTriples);
				timeThen = timeNow;
			}

			Resource subj = stmt.getSubject();
			if (subj.isAnon()) putAdd(anonStatements, subj.toString(), stmt);
		}
		Util.writeln("    total triples inferred: " + numTriples);

		Util.writeln("Extracting class labels...");

		int numClasses = 0;
		timeThen = new Date().getTime();
		for (Iterator<OntClass> it = ontology.listClasses(); it.hasNext();)
		{
			OntClass ontClass = it.next();
			numClasses++;
			long timeNow = new Date().getTime();
			if (timeNow > timeThen + 2000)
			{
				Util.writeln("    so far: " + numClasses);
				timeThen = timeNow;
			}

			for (NodeIterator labels = ontClass.listPropertyValues(RDFS.label); labels.hasNext();)
			{
				RDFNode labelNode = labels.next();
				Literal label = labelNode.asLiteral();
				uriToLabel.put(ontClass.getURI(), label.getString());
				labelToURI.put(label.getString(), ontClass.getURI());
			}
		}

		Util.writeln("    number of classes: " + numClasses);

		int numProperties = 0;
		int hasInverseCounter = 0;
		int forAllCounter = 0;
		int forSomeCounter = 0;
		int cardinalityCounter = 0;
		int maxCardinalityCounter = 0;
		int minCardinalityCounter = 0;

		Util.writeln("Extracting property labels...");

		timeThen = new Date().getTime();
		for (Iterator<OntProperty> it = ontology.listOntProperties(); it.hasNext();)
		{
			OntProperty ontProp = it.next();
			numProperties++;
			long timeNow = new Date().getTime();
			if (timeNow > timeThen + 2000)
			{
				Util.writeln("    so far: " + numProperties);
				timeThen = timeNow;
			}

			if (ontProp.hasInverse()) hasInverseCounter++;

			for (NodeIterator labels = ontProp.listPropertyValues(RDFS.label); labels.hasNext();)
			{
				RDFNode labelNode = labels.next();
				Literal label = labelNode.asLiteral();
				uriToLabel.put(ontProp.getURI(), label.getString());
				labelToURI.put(label.getString(), ontProp.getURI());
			}
		}

		// fill in missing labels (this probably shouldn't be necessary, but...)
		Property propLabel = ontology.createProperty(ModelSchema.PFX_RDFS + "label");
		for (StmtIterator iter = ontology.listStatements(null, propLabel, (RDFNode)null); iter.hasNext();)
		{
			Statement stmt = iter.next();
			Resource subject = stmt.getSubject();
			RDFNode object = stmt.getObject();
			if (subject.isURIResource() && object.isLiteral()) uriToLabel.put(subject.getURI(), object.asLiteral().getString());
		}

		Util.writeln("    number of properties: " + numProperties);

		Util.writeln("Total labels: " + uriToLabel.size());

		Util.writeln("---- Main Iteration ----");

		timeThen = new Date().getTime();
		for (Iterator<OntClass> it = ontology.listClasses(); it.hasNext();)
		{
			// pull out the ontology sequence for this axiom:
			//    o = class of interest
			//    c = the parent class inditing it
			//    p = the property containing the axiom
			//    v = value required by the axiom

			OntClass o = it.next();
			if (!schemaValues.contains(o.asResource().getURI())) continue;

			for (Iterator<OntClass> i = o.listSuperClasses(); i.hasNext();)
			{
				OntClass c = i.next();

				if (c.isRestriction()) //go over each axiom of a particular class and put the class and axioms to the bag
				{
					Restriction r = c.asRestriction(); //restriction == axiom
					if (r.isAllValuesFromRestriction()) // only axioms
					{
						AllValuesFromRestriction av = r.asAllValuesFromRestriction();
						OntProperty p = av.getOnProperty();
						String pname = nameNode(p);
						OntClass v = (OntClass)av.getAllValuesFrom();

						Resource[] sequence = expandSequence(v);
						boolean anySchema = false;
						for (Resource s : sequence) if (schemaValues.contains(s.getURI()))
						{
							anySchema = true;
							break;
						}
						if (!anySchema) continue;

						String key = nameNode(o);
						String val = "ALL: property=[" + pname + "] value=";
						if (sequence.length == 0) val += "{nothing}";	
						String objectURIs = null;
						//StringBuilder oURIs = null;
						String objectLabels = null;
						String[] uriArray = new String[sequence.length];
						for (int n = 0; n < sequence.length; n++)
						{
							val += (n > 0 ? "," : "") + "[" + nameNode(sequence[n]) + "]";
							objectURIs += "[" + sequence[n] + "]";
							//oURIs.append(sequence[n] + ";");
							objectLabels += "[" + nameNode(sequence[n]) + "]";
							uriArray[n] = "" + sequence[n];

							//here I want to add all the subclasses for 
							//OntClass v as well in order to do better suggestions for the
							//axiom backed up suggestions for [property, value (and its subclasses)]

						}
						if (!putAdd(axioms, o.getURI() + "::" + key, val)) continue;
						forAllCounter++;
						propTypeCount.put(pname, propTypeCount.getOrDefault(pname, 0) + 1);
						if (!putAdd(onlyAxioms, o.getURI() + "::" + key, val)) continue; //this is added for JSON

						/*axiomsForAll.add(new AssayAxiomsAll(o.getURI(), p.getURI(), objectURIs, "only", uriArray));
						assayAxioms.add(new AssayAxioms(o.getURI(), p.getURI(), "only", uriArray));*/
												
						Rule rule = new Rule(Type.LIMIT, new Term(o.getURI(), false));
						rule.impact = new Term[uriArray.length];
						for (int n = 0; n < uriArray.length; n++) rule.impact[n] = new Term(uriArray[n], true);
						axvoc.addRule(rule);
					}
					else if (r.isSomeValuesFromRestriction())
					{
						SomeValuesFromRestriction av = r.asSomeValuesFromRestriction();
						OntProperty p = av.getOnProperty();
						String pname = nameNode(p);
						OntClass v = (OntClass)av.getSomeValuesFrom();

						Resource[] sequence = expandSequence(v);
						boolean anySchema = false;
						for (Resource s : sequence) if (schemaValues.contains(s.getURI()))
						{
							anySchema = true;
							break;
						}
						if (!anySchema) continue;

						String key = nameNode(o);
						String val = "SOME: property=[" + pname + "] value=";
						if (sequence.length == 0) val += "{nothing}";
						String objectURIs = null;
						String objectLabels = null;
						String[] uriArray = new String[sequence.length];
						for (int n = 0; n < sequence.length; n++)
						{
							val += (n > 0 ? "," : "") + "[" + nameNode(sequence[n]) + "]";
							objectURIs += "[" + sequence[n] + "]";
							objectLabels += "[" + nameNode(sequence[n]) + "]";
							uriArray[n] = "" + sequence[n];
						}
						for (int n = 0; n < sequence.length; n++)
							val += (n > 0 ? "," : "") + "[" + nameNode(sequence[n]) + "]";

						if (!putAdd(axioms, o.getURI() + "::" + key, val)) continue;
						forSomeCounter++;
						propTypeCount.put(pname, propTypeCount.getOrDefault(pname, 0) + 1);
						if (!putAdd(someAxioms, o.getURI() + "::" + key, val)) continue;
						
						//if (!(Arrays.asList(redundantURIs).contains(o.getURI())))
						//axiomsForSome.add(new AssayAxiomsSome(o.getURI(), p.getURI(), objectURIs, "some", uriArray));
						//assayAxioms.add(new AssayAxioms(o.getURI(), p.getURI(), "some", uriArray));
						//someAxiomsArray.put(ac.createJSONObject(o.getURI(), p.getURI(), objectURIs,"some"));//this is for the axiom json
						
						// TODO: is there a rule type for "Some"?
					}
					else if (r.isMaxCardinalityRestriction())
					{
						MaxCardinalityRestriction av = r.asMaxCardinalityRestriction();
						OntProperty p = av.getOnProperty();
						String pname = nameNode(p);
						int maximum = av.getMaxCardinality();

						String key = nameNode(o);
						String val = "MAX: property=[" + pname + "] maximum=" + maximum;
						if (!putAdd(axioms, o.getURI() + "::" + key, val)) continue;
						maxCardinalityCounter++;
						propTypeCount.put(pname, propTypeCount.getOrDefault(pname, 0) + 1);

						// TODO: map to a rule (0 = blank?)		
					}
					else if (r.isMinCardinalityRestriction())
					{
						MinCardinalityRestriction av = r.asMinCardinalityRestriction();
						OntProperty p = av.getOnProperty();
						String pname = nameNode(p);
						int minimum = av.getMinCardinality();

						String key = nameNode(o);
						String val = "MIN: property=[" + pname + "] minimum=" + minimum;
						if (!putAdd(axioms, o.getURI() + "::" + key, val)) continue;
						minCardinalityCounter++;
						propTypeCount.put(pname, propTypeCount.getOrDefault(pname, 0) + 1);

						// TODO: map to a rule (>0 = required?)				
					}
					else if (r.isCardinalityRestriction())
					{
						CardinalityRestriction av = r.asCardinalityRestriction();
						OntProperty p = av.getOnProperty();
						String pname = nameNode(p);
						int cardinality = av.getCardinality();

						String key = nameNode(o);
						String val = "EQ: property=[" + pname + "] cardinality=" + cardinality;
						if (!putAdd(axioms, o.getURI() + "::" + key, val)) continue;
						cardinalityCounter++;
						propTypeCount.put(pname, propTypeCount.getOrDefault(pname, 0) + 1);
						
						// TODO: map to a rule (blank/required?)
					}
				}
			}

			for (Iterator<OntClass> i = o.listEquivalentClasses(); i.hasNext();)
			{
				OntClass c = i.next();

				if (c.isIntersectionClass()) // go over each axiom of a particular class and put the class and axioms to the bag
				{
					Restriction r = null; // restriction == axiom
					try {r = c.asRestriction();}
					catch (ConversionException ex) {continue;} // silent failure
					
					if (r.isAllValuesFromRestriction()) // only axioms
					{
						AllValuesFromRestriction av = r.asAllValuesFromRestriction();
						OntProperty p = av.getOnProperty();
						String pname = nameNode(p);
						OntClass v = (OntClass)av.getAllValuesFrom();

						Resource[] sequence = expandSequence(v);
						boolean anySchema = false;
						for (Resource s : sequence) if (schemaValues.contains(s.getURI()))
						{
							anySchema = true;
							break;
						}
						if (!anySchema) continue;

						String key = nameNode(o);
						String val = "ALL: property=[" + pname + "] value=";
						if (sequence.length == 0) val += "{nothing}";
						String objectURIs = null;
						String objectLabels = null;
						String[] uriArray = new String[sequence.length];
						for (int n = 0; n < sequence.length; n++)
						{
							val += (n > 0 ? "," : "") + "[" + nameNode(sequence[n]) + "]";
							objectURIs += "[" + sequence[n] + "]";
							objectLabels += "[" + nameNode(sequence[n]) + "]";
							uriArray[n] = "" + sequence[n];
						}
						if (!putAdd(axioms, o.getURI() + "::" + key, val)) continue;
						forAllCounter++;
						propTypeCount.put(pname, propTypeCount.getOrDefault(pname, 0) + 1);
						if (!putAdd(onlyAxioms, o.getURI() + "::" + key, val)) continue;//this is added for JSON

						//axiomsForAll.add(new AssayAxiomsAll(o.getURI(), p.getURI(), objectURIs, "only", uriArray));

						Rule rule = new Rule(Type.LIMIT, new Term(o.getURI(), false));
						rule.impact = new Term[uriArray.length];
						for (int n = 0; n < uriArray.length; n++) rule.impact[n] = new Term(uriArray[n], false);
						axvoc.addRule(rule);
					}
					else if (r.isSomeValuesFromRestriction())
					{
						SomeValuesFromRestriction av = r.asSomeValuesFromRestriction();
						OntProperty p = av.getOnProperty();
						String pname = nameNode(p);
						OntClass v = (OntClass)av.getSomeValuesFrom();

						Resource[] sequence = expandSequence(v);
						boolean anySchema = false;
						for (Resource s : sequence) if (schemaValues.contains(s.getURI()))
						{
							anySchema = true;
							break;
						}
						if (!anySchema) continue;

						String key = nameNode(o);
						String val = "SOME: property=[" + pname + "] value=";
						if (sequence.length == 0) val += "{nothing}";
						String objectURIs = null;
						String objectLabels = null;
						String[] uriArray = new String[sequence.length];
						for (int n = 0; n < sequence.length; n++)
						{
							val += (n > 0 ? "," : "") + "[" + nameNode(sequence[n]) + "]";
							objectURIs += "[" + sequence[n] + "]";
							objectLabels += "[" + nameNode(sequence[n]) + "]";
							uriArray[n] = "" + sequence[n];
						}
						for (int n = 0; n < sequence.length; n++)
							val += (n > 0 ? "," : "") + "[" + nameNode(sequence[n]) + "]";

						if (!putAdd(axioms, o.getURI() + "::" + key, val)) continue;
						forSomeCounter++;
						propTypeCount.put(pname, propTypeCount.getOrDefault(pname, 0) + 1);
						if (!putAdd(someAxioms, o.getURI() + "::" + key, val)) continue;
						
						/*if (!redundantURISet.contains(o.getURI()))
							axiomsForSome.add(new AssayAxiomsSome(o.getURI(), p.getURI(), objectURIs, "some", uriArray));
						//someAxiomsArray.put(ac.createJSONObject(o.getURI(), p.getURI(), objectURIs,"some"));//this is for the axiom json*/
						// TODO: use these?
					}
					else if (r.isMaxCardinalityRestriction())
					{
						MaxCardinalityRestriction av = r.asMaxCardinalityRestriction();
						OntProperty p = av.getOnProperty();
						String pname = nameNode(p);
						int maximum = av.getMaxCardinality();

						String key = nameNode(o);
						String val = "MAX: property=[" + pname + "] maximum=" + maximum;
						if (!putAdd(axioms, o.getURI() + "::" + key, val)) continue;
						maxCardinalityCounter++;
						propTypeCount.put(pname, propTypeCount.getOrDefault(pname, 0) + 1);

						// TODO: use these?
					}
					else if (r.isMinCardinalityRestriction())
					{
						MinCardinalityRestriction av = r.asMinCardinalityRestriction();
						OntProperty p = av.getOnProperty();
						String pname = nameNode(p);
						int minimum = av.getMinCardinality();

						String key = nameNode(o);
						String val = "MIN: property=[" + pname + "] minimum=" + minimum;
						if (!putAdd(axioms, o.getURI() + "::" + key, val)) continue;
						minCardinalityCounter++;
						propTypeCount.put(pname, propTypeCount.getOrDefault(pname, 0) + 1);

						// TODO: use these?
					}
					else if (r.isCardinalityRestriction())
					{
						CardinalityRestriction av = r.asCardinalityRestriction();
						OntProperty p = av.getOnProperty();
						String pname = nameNode(p);
						int cardinality = av.getCardinality();

						String key = nameNode(o);
						String val = "EQ: property=[" + pname + "] cardinality=" + cardinality;
						if (!putAdd(axioms, o.getURI() + "::" + key, val)) continue;
						cardinalityCounter++;
						propTypeCount.put(pname, propTypeCount.getOrDefault(pname, 0) + 1);

						// TODO: use these?
					}
				}
			}

			long timeNow = new Date().getTime();
			if (timeNow > timeThen + 2000)
			{
				Util.writeln("    so far: " + axioms.size());
				timeThen = timeNow;
			}
		}

		Util.writeln("\n---- Category Counts ----");
		Util.writeln("terms with axioms: " + axioms.size());
		Util.writeln("for all axioms: " + forAllCounter);
		Util.writeln("for some axioms: " + forSomeCounter);
		Util.writeln("for max axioms: " + maxCardinalityCounter);
		Util.writeln("for min axioms: " + minCardinalityCounter);
		Util.writeln("for exactly axioms: " + cardinalityCounter);
		Util.writeln("properties with inverse: " + hasInverseCounter);
		
		Util.writeln("\nTotal Axiom Rules:");
		int numLimit = 0, numExclude = 0, numBlank = 0, numRequired = 0;
		for (Rule r : axvoc.getRules())
		{
			if (r.type == Type.LIMIT) numLimit++;
			else if (r.type == Type.EXCLUDE) numExclude++;
			else if (r.type == Type.BLANK) numBlank++;
			else if (r.type == Type.REQUIRED) numRequired++;
		}
		Util.writeln("    limit: " + numLimit);
		Util.writeln("    exclude: " + numExclude);
		Util.writeln("    blank: " + numBlank);
		Util.writeln("    required: " + numRequired);

		Util.writeln("Scanning complete.");
	}
	
	// extract as axiom rules and export using the binary format
	public void exportDump(String fn) throws IOException, OntologyException
	{
		File f = new File(fn).getAbsoluteFile();
		Util.writeln("Writing rules dump to: " + f.getPath());
		axvoc.serialise(f);
	}
	
	// exports the axioms as a very simple text file of correlated pairs of URIs
	public void exportPair(String fn) throws IOException
	{
		File f = new File(fn).getAbsoluteFile();
		Util.writeln("Writing pairs to: " + f.getPath());
		
		try (BufferedWriter wtr = new BufferedWriter(new FileWriter(f)))
		{
			for (Rule r : axvoc.getRules()) if (r.type == Type.LIMIT && r.impact != null)
			{
				for (int n = 0; n < r.impact.length; n++)
				{
					wtr.write(r.subject.valueURI);
					wtr.write(" ");
					wtr.write(r.impact[n].valueURI);
					wtr.write(" " + r.impact[n].wholeBranch + "\n");
				}
			}
		}
	}

	// export a human-readable text summary	
	public void exportText(String fn) throws IOException
	{
		File f = new File(fn).getAbsoluteFile();
		Util.writeln("Writing whole output to: " + f.getPath());

		try (PrintWriter wtr = new PrintWriter(f))
		{
			wtr.println("---- Property Counts ----");
			for (String key : propTypeCount.keySet())
				wtr.println("[" + key + "] count=" + propTypeCount.get(key));

			wtr.println("\n==== Axioms ====");

			List<Schema.Group> stack = new ArrayList<>();
			stack.add(schema.getRoot());
			while (stack.size() > 0)
			{
				Schema.Group group = stack.remove(0);

				for (Schema.Assignment assn : group.assignments)
				{
					wtr.println("\n---- " + assn.name + " <" + ModelSchema.collapsePrefix(assn.propURI) + "> ----");
					Set<String> wantURI = new HashSet<>();
					for (SchemaVocab.StoredTree stored : schvoc.getTrees()) if (stored.assignment != null && stored.tree != null)
					{
						if (stored.assignment.propURI.equals(assn.propURI)) for (SchemaTree.Node node : stored.tree.getFlat())
							wantURI.add(node.uri);
					}

					for (String key : axioms.keySet())
					{
						String[] bits = key.split("::");
						String uri = bits[0], name = bits[1];
						if (!wantURI.contains(uri)) continue;

						wtr.println("[" + name + "]:");
						List<String> values = new ArrayList<>(axioms.get(key));
						Collections.sort(values);
						for (String val : values) wtr.println("    " + val);
					}
				}

				stack.addAll(group.subGroups);
			}
		}	
	}
	
	// ------------ private methods ------------

	// adds a value to a list-within-map
	protected static boolean putAdd(Map<String, Set<String>> map, String key, String val)
	{
		return map.computeIfAbsent(key, k -> new HashSet<>()).add(val);
	}

	protected static <T> void putAdd(Map<String, List<T>> map, String key, T stmt)
	{
		map.computeIfAbsent(key, k -> new ArrayList<>()).add(stmt);
	}

	// turns a URI into a readable name, which includes the label if available
	private String nameURI(String uri)
	{
		if (uri == null) throw new NullPointerException();
		String label = uriToLabel.get(uri), abbrev = ModelSchema.collapsePrefix(uri);
		String name = label == null ? "" : label + " ";
		return name + "<" + abbrev + ">";
	}

	private String nameNode(RDFNode node)
	{
		if (node == null) return "{null}";
		if (node.isLiteral()) return "\"" + node.asLiteral().getLexicalForm() + "\"";
		if (node.isAnon()) return "{anon}";
		if (node.isURIResource()) return nameURI(node.asResource().getURI());
		return "{???}";
	}

	// takes a resource that may well be an anonymous value: which means go through and expand it out into a chain of resources
	private Resource[] expandSequence(Resource v)
	{
		if (!v.isAnon()) return new Resource[]{v};

		Property rdfType = ontology.createProperty(ModelSchema.PFX_RDF + "type");
		List<Resource> sequence = new ArrayList<>(), anonymous = new ArrayList<>();
		anonymous.add(v);
		while (anonymous.size() > 0)
		{
			List<Statement> statements = anonStatements.get(anonymous.remove(0).toString());
			if (statements != null) for (Statement stmt : statements)
			{
				Property prop = stmt.getPredicate();
				if (prop.equals(rdfType)) continue;
				RDFNode object = stmt.getObject();
				if (!object.isResource()) continue;
				if (!object.isAnon())
					sequence.add(object.asResource());
				else
					anonymous.add(object.asResource());
			}
		}
		return sequence.toArray(new Resource[sequence.size()]);
	}
}
