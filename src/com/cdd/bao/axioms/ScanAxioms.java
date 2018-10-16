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

import org.apache.commons.lang3.*;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;
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
	//private Model outModel = ModelFactory.createDefaultModel();
	private OntModel outModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF);

	// list of URIs that are useless because of their generality
	private static String[] redundantURIs = 
	{
		"",
		"http://purl.obolibrary.org/obo/CHEBI_35222", // inhibitor
		"http://www.bioassayontology.org/bao#BAO_0000015", // bioassay
		"http://www.bioassayontology.org/bao#BAO_0000019", // assay format
		"http://www.bioassayontology.org/bao#BAO_0000026", // bioassay specification
		"http://www.bioassayontology.org/bao#BAO_0000029", // assay screening campaign stage
		"http://www.bioassayontology.org/bao#BAO_0000035", // physical detection method
		"http://www.bioassayontology.org/bao#BAO_0000074", // mode of action
		"http://www.bioassayontology.org/bao#BAO_0000076", // screened entity
		"http://www.bioassayontology.org/bao#BAO_0000179", // endpoint
		"http://www.bioassayontology.org/bao#BAO_0000248", // assay kit
		"http://www.bioassayontology.org/bao#BAO_0000264", // biological process
		"http://www.bioassayontology.org/bao#BAO_0000512", // assay footprint
		"http://www.bioassayontology.org/bao#BAO_0002001", // measured entity
		"http://www.bioassayontology.org/bao#BAO_0002030", // coupled substrate
		"http://www.bioassayontology.org/bao#BAO_0002091", // commercial company
		"http://www.bioassayontology.org/bao#BAO_0002202", // assay design method
		"http://www.bioassayontology.org/bao#BAO_0002626", // biologics and screening manufacturer
		"http://www.bioassayontology.org/bao#BAO_0002628", // instrumentation manufacturer
		"http://www.bioassayontology.org/bao#BAO_0003028", // assay method
		"http://www.bioassayontology.org/bao#BAO_0003043", // molecular entity
		"http://www.bioassayontology.org/bao#BAO_0003060", // potentiator
		"http://www.bioassayontology.org/bao#BAO_0003063", // substrate
		"http://www.bioassayontology.org/bao#BAO_0003075", // molecular function
		"http://www.bioassayontology.org/bao#BAO_0003102", // has role
		"http://www.bioassayontology.org/bao#BAO_0003111", // nucleic acid
		"http://www.bioassayontology.org/bao#BAO_0003112", // assay bioassay component
		"http://www.bioassayontology.org/bao#BAO_0003115", // assay result component
	};
	public Set<String> redundantURISet = new HashSet<>(Arrays.asList(redundantURIs));
	//private static String hasRoleURI = "http://www.bioassayontology.org/bao#BAO_0003102";

	private Set<String> schemaValues = new HashSet<>();
	private int numProperties = 0;
	private int hasInverseCounter = 0;
	private int forAllCounter = 0;
	private int forSomeCounter = 0;
	private int cardinalityCounter = 0;
	private int maxCardinalityCounter = 0;
	private int minCardinalityCounter = 0;
	
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
			String subjectURI = o.getURI(), subjectName = nameNode(o);
			if (!schemaValues.contains(subjectURI) || redundantURISet.contains(subjectURI)) continue;

			for (Iterator<OntClass> i = o.listSuperClasses(); i.hasNext();)
			{
				OntClass c = i.next();

				if (c.isRestriction()) // go over each axiom of a particular class and put the class and axioms to the bag
				{
					Restriction r = c.asRestriction(); // restriction == axiom
					if (r.isAllValuesFromRestriction()) processAxiomAll(subjectURI, subjectName, r);
					else if (r.isSomeValuesFromRestriction()) processAxiomSome(subjectURI, subjectName, r);
					else if (r.isMinCardinalityRestriction()) processCardinality(subjectURI, subjectName, r);
					else if (r.isMaxCardinalityRestriction()) processCardinality(subjectURI, subjectName, r);
					else if (r.isCardinalityRestriction()) processCardinality(subjectURI, subjectName, r);
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
					
					if (r.isAllValuesFromRestriction()) processAxiomAll(subjectURI, subjectName, r);
					else if (r.isSomeValuesFromRestriction()) processAxiomSome(subjectURI, subjectName, r);
					else if (r.isMinCardinalityRestriction()) processCardinality(subjectURI, subjectName, r);
					else if (r.isMaxCardinalityRestriction()) processCardinality(subjectURI, subjectName, r);
					else if (r.isCardinalityRestriction()) processCardinality(subjectURI, subjectName, r);
				}
			}

			long timeNow = new Date().getTime();
			if (timeNow > timeThen + 2000)
			{
				Util.writeln("    so far: " + axvoc.numRules());
				timeThen = timeNow;
			}
		}

		Util.writeln("\n---- Category Counts ----");
		Util.writeln("for all axioms: " + forAllCounter);
		Util.writeln("for some axioms: " + forSomeCounter);
		Util.writeln("for max axioms: " + maxCardinalityCounter);
		Util.writeln("for min axioms: " + minCardinalityCounter);
		Util.writeln("for exactly axioms: " + cardinalityCounter);
		Util.writeln("properties with inverse: " + hasInverseCounter);
		
		Util.writeln("\nTotal Axiom Rules:");
		int numLimit = 0, numExclude = 0; //, numBlank = 0, numRequired = 0;
		for (Rule r : axvoc.getRules())
		{
			if (r.type == Type.LIMIT) numLimit++;
			else if (r.type == Type.EXCLUDE) numExclude++;
			/*else if (r.type == Type.BLANK) numBlank++;
			else if (r.type == Type.REQUIRED) numRequired++;*/
		}
		Util.writeln("    limit: " + numLimit);
		Util.writeln("    exclude: " + numExclude);
		/*Util.writeln("    blank: " + numBlank);
		Util.writeln("    required: " + numRequired);*/

		Util.writeln("Scanning complete.");
	}

	// extract as axiom rules and export using the binary format
	public void exportDump(String fn) throws IOException
	{
		File f = new File(fn).getAbsoluteFile();
		Util.writeln("Writing rules dump to: " + f.getPath());
		axvoc.serialise(f);
	}
	
	// save the ontology representation of the scanned-out axioms
	public void exportOntology(String fn) throws IOException
	{
		outModel.setNsPrefix("bao", ModelSchema.PFX_BAO);
		outModel.setNsPrefix("bat", ModelSchema.PFX_BAT);
		outModel.setNsPrefix("src", ModelSchema.PFX_SOURCE);
		outModel.setNsPrefix("obo", ModelSchema.PFX_OBO);
		outModel.setNsPrefix("rdf", ModelSchema.PFX_RDF);
		outModel.setNsPrefix("rdfs", ModelSchema.PFX_RDFS);
		outModel.setNsPrefix("xsd", ModelSchema.PFX_XSD);
		outModel.setNsPrefix("dto", ModelSchema.PFX_DTO);
		
		File f = new File(fn).getAbsoluteFile();
		Util.writeln("Writing ontology extract to: " + f.getPath());
		
		try (OutputStream ostr = new FileOutputStream(fn))
		{
			RDFDataMgr.write(ostr, outModel, RDFFormat.TURTLE);
		}
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
			wtr.println("Total Axiom Rules:");
			int numLimit = 0, numExclude = 0;//, numBlank = 0, numRequired = 0;
			for (Rule r : axvoc.getRules())
			{
				if (r.type == Type.LIMIT) numLimit++;
				else if (r.type == Type.EXCLUDE) numExclude++;
				/*else if (r.type == Type.BLANK) numBlank++;
				else if (r.type == Type.REQUIRED) numRequired++;*/
			}
			wtr.println("    limit: " + numLimit);
			wtr.println("    exclude: " + numExclude);
			/*wtr.println("    blank: " + numBlank);
			wtr.println("    required: " + numRequired);*/

			wtr.println("\n==== Axioms ====");

			Map<String, List<AxiomVocab.Rule>> ruleSubject = new HashMap<>();
			for (AxiomVocab.Rule rule : axvoc.getRules()) putAdd(ruleSubject, rule.subject.valueURI, rule);

			List<Schema.Group> stack = new ArrayList<>();
			stack.add(schema.getRoot());
			while (stack.size() > 0)
			{
				Schema.Group group = stack.remove(0);

				for (Schema.Assignment assn : group.assignments)
				{
					SchemaTree tree = null;
					for (SchemaVocab.StoredTree stored : schvoc.getTrees()) if (stored.assignment != null && stored.tree != null)
						if (stored.assignment.propURI.equals(assn.propURI)) {tree = stored.tree; break;}
					if (tree == null) continue;
					
					wtr.println("\n---- " + assn.name + " <" + ModelSchema.collapsePrefix(assn.propURI) + "> ----");

					for (SchemaTree.Node node : tree.getFlat())
					{
						List<AxiomVocab.Rule> rules = ruleSubject.get(node.uri);
						if (rules == null) continue;
						for (AxiomVocab.Rule rule : rules)
						{
							wtr.write("  ");
							wtr.write(rule.type.toString() + ": ");
							wtr.write("[" + ModelSchema.collapsePrefix(rule.subject.valueURI) + (rule.subject.wholeBranch ? "*" : "") + 
									  "/" + uriToLabel.get(rule.subject.valueURI) + "]");
									  
							wtr.write(" ==> ");
							
							StringJoiner sj = new StringJoiner(",");
							if (rule.impact != null) for (Term s : rule.impact) 
								sj.add(ModelSchema.collapsePrefix(s.valueURI) + (s.wholeBranch ? "*" : "") + "/" + uriToLabel.get(s.valueURI));
							wtr.write(" [" + sj.toString() + "])");
							wtr.write("\n");
						}
					}
				}

				stack.addAll(group.subGroups);
			}
		}	
	}
	
	// ------------ private methods ------------

	// process an "ALL" axiom, which is converted to a limitation rule
	private void processAxiomAll(String subjectURI, String subjectName, Restriction r)
	{
		AllValuesFromRestriction av = r.asAllValuesFromRestriction();
		OntProperty p = av.getOnProperty();
		String pname = nameNode(p);
		OntClass v = (OntClass)av.getAllValuesFrom();

		Resource[] sequence = expandSequence(v);
		if (sequence.length == 0) return;

		Rule rule = new Rule(Type.LIMIT, new Term(subjectURI, true /* whole branch? */));
		rule.impact = new Term[sequence.length];
		for (int n = 0; n < rule.impact.length; n++) rule.impact[n] = new Term(sequence[n].getURI(), true);
		addRule(rule);
		
		forAllCounter++;
	}
	
	// process a "SOME" axiom, which is converted to a limitation rule
	private void processAxiomSome(String subjectURI, String subjectName, Restriction r)
	{
		SomeValuesFromRestriction av = r.asSomeValuesFromRestriction();
		OntProperty p = av.getOnProperty();
		String pname = nameNode(p);
		OntClass v = (OntClass)av.getSomeValuesFrom();

		Resource[] sequence = expandSequence(v);
		if (sequence.length == 0) return;

		Rule rule = new Rule(Type.LIMIT, new Term(subjectURI, true /* whole branch? */));
		rule.impact = new Term[sequence.length];
		for (int n = 0; n < rule.impact.length; n++) rule.impact[n] = new Term(sequence[n].getURI(), true);
		addRule(rule);
		
		forSomeCounter++;
	}	

	// cardinality restriction: not currently used, but they could be
	private void processCardinality(String subjectURI, String subjectName, Restriction r)
	{
		if (r.isMaxCardinalityRestriction())
		{
			MaxCardinalityRestriction av = r.asMaxCardinalityRestriction();
			OntProperty p = av.getOnProperty();
			String pname = nameNode(p);
			int maximum = av.getMaxCardinality();
			// ...
			maxCardinalityCounter++;
		}
		else if (r.isMinCardinalityRestriction())
		{
			MinCardinalityRestriction av = r.asMinCardinalityRestriction();
			OntProperty p = av.getOnProperty();
			String pname = nameNode(p);
			int minimum = av.getMinCardinality();
			// ...
			minCardinalityCounter++;
		}
		else if (r.isCardinalityRestriction())
		{
			CardinalityRestriction av = r.asCardinalityRestriction();
			OntProperty p = av.getOnProperty();
			String pname = nameNode(p);
			int cardinality = av.getCardinality();
			// ...
			cardinalityCounter++;
		}
	}

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

	// takes a resource that may well be an anonymous value: which means go through and expand it out into a chain of resources;
	// excludes anything in the redundant list/not in the schema items list
	private Resource[] expandSequence(Resource v)
	{
		if (!v.isAnon()) 
		{
			if (!v.isResource()) return new Resource[0];
			String uri = v.asResource().getURI();
			if (redundantURISet.contains(uri) || !schemaValues.contains(uri)) return new Resource[0];
			return new Resource[]{v};
		}

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
				{
					Resource r = object.asResource();
					String uri = r.getURI();
					if (!redundantURISet.contains(uri) && schemaValues.contains(uri)) sequence.add(r);
				}
				else anonymous.add(object.asResource());
			}
		}
		return sequence.toArray(new Resource[sequence.size()]);
	}
	
	// adds a rule to the vocabulary, after checking for duplicates
	private void addRule(Rule rule)
	{
		for (Rule look : axvoc.getRules()) if (look.equals(rule)) return;
		axvoc.addRule(rule);
		
		Property rdfType = ontology.createProperty(ModelSchema.PFX_RDF + "type");
		for (int n = 0; n < rule.impact.length; n++) 
		{
			OntClass obj = outModel.createClass(rule.impact[n].valueURI);
			//OntProperty pred = outModel.createOntProperty(rule.predicate.valueURI);
			if (rule.type == Type.LIMIT) 
			{
				AllValuesFromRestriction values = outModel.createAllValuesFromRestriction(rule.subject.valueURI, rdfType, obj);
			}
			// ... other types...
		}
	}
}
