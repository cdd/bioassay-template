package com.cdd.bao;

import java.util.*;
import java.io.*;

import org.apache.commons.lang3.*;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.apache.jena.util.iterator.*;

import com.cdd.bao.template.*;
import com.cdd.bao.util.*;
import com.cdd.bao.editor.*;

/*
	This class is to read axioms into a file and group them based on class
	so it should have the structure
	class :: {axiom_prop1 obj_1, axiom_prop2 obj2, axiom_prop3 obj3, .. , axiom_n obj_n}

    after this class is used, we should be able to get the value (from annotations) 
    based on that value, get the key , i.e. class , and get all the other values in the set
    in order to predict the rest for the annotations.
*/

public class ScanAxioms 
{
	private OntModel ontology; 
	private Map<String, String> uriToLabel = new HashMap<String, String>();

	public ScanAxioms()
	{
	}

	public void exec() throws OntologyException 
	{ 
		/*
		//assayKit, bioassay, bioassaySpecification, biologicalProcess, modeOfAction, molecular function
		String irrelevantIRIs[] = 
		{
			"http://www.bioassayontology.org/bao#BAO_0000248",
			"http://www.bioassayontology.org/bao#BAO_0000015", 
			"http://www.bioassayontology.org/bao#BAO_0000026",
			"http://www.bioassayontology.org/bao#BAO_0000264",
			"http://www.bioassayontology.org/bao#BAO_0000074",
			"http://www.bioassayontology.org/bao#BAO_0002202",
			"http://www.bioassayontology.org/bao#BAO_0003075"
		};*/
					 
	 	try 
	  	{
	  		List<File> files = new ArrayList<>();
	  		for (File f : new File("data/ontology").listFiles()) if (f.getName().endsWith(".owl")) files.add(f);
	  		//for (File f : new File("data/preprocessed").listFiles()) if (f.getName().endsWith(".owl")) files.add(f);
	  		Util.writeln("# files to read: " + files.size());

   			ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF); 
			for (File f : files)
			{
//if (!f.getName().equals("bao_vocabulary_phenotype.owl")) continue; // !!			
				Util.writeln("    reading: " + f.getCanonicalPath());
				//org.apache.jena.riot.RDFDataMgr.read(ontology, f.getPath(), org.apache.jena.riot.Lang.RDFXML);
				Reader rdr = new FileReader(f);
				ontology.read(rdr, null);
				rdr.close();
			}
				  	
	  		/* one file approach
   			//ontology path
   			String path = "/Users/handemcginty/home/"; 
   
   			in = new FileInputStream(path + "bao_complete_merged.owl"); 

   			ontology = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_RDFS_INF); 
   			ontology = (OntModel) ontology.read(in, "http://www.bioassayontology.org/bao/bao_complete_merged.owl");  
   			in.close(); 
   			System.out.println("Read BAO merged ontology");*/
   		}
   		catch (Exception e) {throw new OntologyException(e.getMessage());}
		
		Util.writeln("Read complete: counting triples...");
		long timeThen = new Date().getTime();
		int numTriples = 0;
		for (StmtIterator iter = ontology.listStatements(); iter.hasNext(); iter.next()) 
		{
			numTriples++;
			long timeNow = new Date().getTime();
			if (timeNow > timeThen + 2000) {Util.writeln("    so far: " + numTriples); timeThen = timeNow;}
		}
		Util.writeln("    total triples inferred: " + numTriples);
		
		Map<String, List<String>> axioms = new TreeMap<>();

		Util.writeln("Extracting class labels...");
		
		int numClasses = 0;
		timeThen = new Date().getTime();
		for (Iterator<OntClass> it = ontology.listClasses(); it.hasNext();)
		{
			OntClass ontClass = it.next();
			numClasses++;
			long timeNow = new Date().getTime();
			if (timeNow > timeThen + 2000) {Util.writeln("    so far: " + numClasses); timeThen = timeNow;}

			for (NodeIterator labels = ontClass.listPropertyValues(RDFS.label); labels.hasNext();)
			{
				RDFNode labelNode = labels.next();
				Literal label = labelNode.asLiteral();
				uriToLabel.put(ontClass.getURI(), label.getString());
			}
		}
		
		Util.writeln("    number of classes: " + numClasses);
		
		Util.writeln("Extracting property labels...");

		int numProperties = 0;
		int hasInverseCounter = 0;
		timeThen = new Date().getTime();
		for (Iterator<OntProperty> it = ontology.listOntProperties(); it.hasNext();)
		{
			OntProperty ontProp = it.next();
			numProperties++;
			long timeNow = new Date().getTime();
			if (timeNow > timeThen + 2000) {Util.writeln("    so far: " + numProperties); timeThen = timeNow;}

			if (ontProp.hasInverse()) hasInverseCounter++;

			for (NodeIterator labels = ontProp.listPropertyValues(RDFS.label); labels.hasNext();)
			{
				RDFNode labelNode = labels.next();
				Literal label = labelNode.asLiteral();
				uriToLabel.put(ontProp.getURI(), label.getString());
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

		int forAllCounter = 0;
		int forSomeCounter = 0;
		int cardinalityCounter = 0;
		int maxCardinalityCounter = 0;
		int minCardinalityCounter = 0;

		for (Iterator<OntClass> it = ontology.listClasses(); it.hasNext();)
		{
			// pull out the ontology sequence for this axiom:
			//    o = class of interest
			//    c = the parent class inditing it
			//    p = the property containing the axiom
			//    v = value required by the axiom
		
			OntClass o = it.next();
			
			for (Iterator<OntClass> i = o.listSuperClasses(); i.hasNext();)
			{
				OntClass c = i.next();
				if (c.isRestriction()) //go over each axiom of a particular class and put the class and axioms to the bag
				{ //uriToLabel.getOrDefault(uri, ModelSchema.collapsePrefix(uri));
					Restriction r = c.asRestriction(); //restriction == axiom
					if (r.isAllValuesFromRestriction()) // only axioms
					{ 
						AllValuesFromRestriction av = r.asAllValuesFromRestriction();
						OntProperty p = (OntProperty)av.getOnProperty();
						OntClass v = (OntClass)av.getAllValuesFrom();
						
						String key = nameNode(o);
						String val = "ALL: property=[" + nameNode(p) + "] value=[" + nameNode(v) + "]";
						putAdd(axioms, key, val);
						forAllCounter++;
					}
					else if (r.isSomeValuesFromRestriction())
					{
						SomeValuesFromRestriction av = r.asSomeValuesFromRestriction();
						OntProperty p = (OntProperty)av.getOnProperty();
						OntClass v = (OntClass)av.getSomeValuesFrom();

						String key = nameNode(o);
						String val = "SOME: property=[" + nameNode(p) + "] value=[" + nameNode(v) + "]";
						putAdd(axioms, key, val);
						forSomeCounter++;
					}
					else if (r.isMaxCardinalityRestriction())
					{
						MaxCardinalityRestriction av = r.asMaxCardinalityRestriction();
						OntProperty p = (OntProperty)av.getOnProperty();
						int maximum = av.getMaxCardinality();
						
						String key = nameNode(o);
						String val = "MAX: property=[" + nameNode(p) + "] maximum=" + maximum;
						putAdd(axioms, key, val);
						maxCardinalityCounter++;
					}
					else if (r.isMinCardinalityRestriction())
					{
						MinCardinalityRestriction av = r.asMinCardinalityRestriction();
						OntProperty p = (OntProperty)av.getOnProperty();
						int minimum = av.getMinCardinality();

						String key = nameNode(o);
						String val = "MIN: property=[" + nameNode(p) + "] minimum=" + minimum;
						putAdd(axioms, key, val);
						minCardinalityCounter++;
					}
					else if (r.isCardinalityRestriction())
					{
						CardinalityRestriction av = r.asCardinalityRestriction();
						OntProperty p = (OntProperty)av.getOnProperty();
						int cardinality = av.getCardinality();

						String key = nameNode(o);
						String val = "EQ: property=[" + nameNode(p) + "] cardinarlity=" + cardinality;
						putAdd(axioms, key, val);
						cardinalityCounter++;
					}
				}
			}
		}
		
		Util.writeln("\n---- Category Counts ----");
		Util.writeln("total axioms: " + axioms.size());
		Util.writeln("for all axioms: " + forAllCounter);
		Util.writeln("for some axioms: " + forSomeCounter);
		Util.writeln("for max axioms: " + maxCardinalityCounter);
		Util.writeln("for min axioms: " + minCardinalityCounter);
		Util.writeln("for exactly axioms: " + cardinalityCounter);
		Util.writeln("properties with inverse: " + hasInverseCounter);
		
		File f = new File("/tmp/axioms.txt");
		Util.writeln("\nWriting whole output to: " + f.getPath());
		
		try
		{
			PrintWriter wtr = new PrintWriter(f);
		
			for (String key : axioms.keySet())
			{
				wtr.println("[" + key + "]:");
				List<String> values = axioms.get(key);
				Collections.sort(values);
				for (String val : values) wtr.println("    " + val);
			}

		    wtr.close();
		}
		catch (IOException e) {throw new OntologyException(e.getMessage());}
		
		Util.writeln("Done.");
	}
	
	// adds a value to a list-within-map
	private void putAdd(Map<String, List<String>> map, String key, String val)
	{
		List<String> values = map.get(key);
		if (values == null) values = new ArrayList<>();
		values.add(val);
		map.put(key, values);
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
	
}