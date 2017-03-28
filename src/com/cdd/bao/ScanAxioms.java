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
	public static void ontologyReader() throws OntologyException 
	{ 
		OntModel ontology; 
		String baseURI; 
		InputStream in = null;
		Restriction restriction = null;
		int forAllCounter = 0;
		int forSomeCounter = 0;
		int cardinalityCounter = 0;
		int maxCardinalityCounter = 0;
		int minCardinalityCounter = 0;
		int hasInverseCounter = 0;
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
		};

	 	Map<String, String> uriToLabel = new HashMap<String, String>();
			 
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
		
		Map<String, List<String>> triples = new LinkedHashMap<>();

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
				uriToLabel.put(ontClass.getURI(), label.toString());
			}
		}
		
		Util.writeln("    number of classes: " + numClasses);
		
		Util.writeln("Extracting property labels...");

		int numProperties = 0;
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
				uriToLabel.put(ontProp.getURI(), label.toString());
			}
		}
		
		Util.writeln("    number of properties: " + numProperties);
		
		Util.writeln("Total labels: " + uriToLabel.size());
		
		Util.writeln("---- Main Iteration ----");

		for (Iterator<OntClass> it = ontology.listClasses(); it.hasNext();)
		{
			OntClass ontClass1 = it.next();
			
			for(Iterator<OntClass> i = ontClass1.listSuperClasses(); i.hasNext();)
			{
				OntClass c = i.next();
				if (c.isRestriction()) //go over each axiom of a particular class and put the class and axioms to the bag
				{ //uriToLabel.getOrDefault(uri, ModelSchema.collapsePrefix(uri));
					Restriction r = c.asRestriction(); //restriction == axiom
					if (r.isAllValuesFromRestriction()) // only axioms
					{ 
						AllValuesFromRestriction av = r.asAllValuesFromRestriction();
						OntProperty p = (OntProperty)av.getOnProperty();
						OntClass c3 = (OntClass)av.getAllValuesFrom();
						
						String key = "\n" + "class " + uriToLabel.getOrDefault(ontClass1.getURI(), ModelSchema.collapsePrefix(ontClass1.getURI()));
						String val =  " on property " + uriToLabel.getOrDefault(p.getURI(), ModelSchema.collapsePrefix(p.getURI()))  + "\t"
								+ " all values from class " +  uriToLabel.getOrDefault(c3.getURI(), ModelSchema.collapsePrefix(c3.getURI())) +"\n";
						putAdd(triples, key + "\n", val);
						forAllCounter++;
						
					
					}
					else if (r.isSomeValuesFromRestriction())
					{
						SomeValuesFromRestriction av = r.asSomeValuesFromRestriction();
						OntProperty p = (OntProperty)av.getOnProperty();
						OntClass c2 = (OntClass)av.getSomeValuesFrom();
						
							/* String key = "\n" + "class " + uriToLabel.getOrDefault(ontClass1.getURI(), ModelSchema.collapsePrefix(ontClass1.getURI()));
						String val =  " on property " + uriToLabel.getOrDefault(p.getURI(), ModelSchema.collapsePrefix(p.getURI()))  + "\t"
								+ " some values from class " +  uriToLabel.getOrDefault(c2.getURI(), ModelSchema.collapsePrefix(c2.getURI())) +"\n";*/
							forSomeCounter++;
					   
					}
					else if (r.isMaxCardinalityRestriction())
					{
						MaxCardinalityRestriction av = r.asMaxCardinalityRestriction();
						OntProperty p = (OntProperty)av.getOnProperty();
						
						String key = "\n" + "class " + uriToLabel.getOrDefault(ontClass1.getURI(), ModelSchema.collapsePrefix(ontClass1.getURI()));
						String val =  " on property " + uriToLabel.getOrDefault(p.getURI(), ModelSchema.collapsePrefix(p.getURI()))  + "\t";
								
						putAdd(triples, key + "\n", val);
									  
						maxCardinalityCounter++;
					}
					else if (r.isMinCardinalityRestriction())
					{
						MinCardinalityRestriction av = r.asMinCardinalityRestriction();
						OntProperty p = (OntProperty)av.getOnProperty();

						String key = "\n" + "class " + uriToLabel.getOrDefault(ontClass1.getURI(), ModelSchema.collapsePrefix(ontClass1.getURI()));
						String val =  " on property " + uriToLabel.getOrDefault(p.getURI(), ModelSchema.collapsePrefix(p.getURI()))  + "\t";
						putAdd(triples, key +"\n", val);
										  
						minCardinalityCounter++;
								 
							 
					}
					else if (r.isCardinalityRestriction())
					{
						CardinalityRestriction av = r.asCardinalityRestriction();
						OntProperty p = (OntProperty)av.getOnProperty();
						// OntClass c2 = (OntResoure)av.getCardinality(p);
						int cardinality = av.getCardinality();
						// if(c2.getURI() != null){
						String key = "class " + uriToLabel.get(ontClass1.getURI());
						String val = "on property " + uriToLabel.get(p.getURI())+ 
									 /*" some values from class " + uriToLabel2.get(c2.getURI()) + */ "\n";
						putAdd(triples, key + "\n", val);
						
						cardinalityCounter++;

						// }
						 /*triples.put("class " + ontClass1.getURI(), 
								 "on property " + p.getLocalName()+ 
								 " some values from class " + c2.getLocalName() + "\n");*/
						// System.out.println("Some values from class" + av.getSomeValuesFrom().getURI() + "on property" + av.getOnProperty().getURI());
					}
				}
			}
		}
			 
		Util.writeln("" + triples.toString());

		Util.writeln("the triples: " + triples.toString());
		Util.writeln("for all axioms: " + forAllCounter);
		Util.writeln("for some axioms: " + forSomeCounter);
		Util.writeln("for max axioms: " + maxCardinalityCounter);
		Util.writeln("for min axioms: " + minCardinalityCounter);
		Util.writeln("for exactly axioms: " + cardinalityCounter);
		Util.writeln("properties with inverse: " + hasInverseCounter);
		try
		{
			PrintWriter writer = new PrintWriter("axiomsAndCounts.txt", "UTF-8");
		    writer.println("" + triples.toString());

			//writer.println("the triples: " + triples.toString());
			for (String key : triples.keySet())
			{
				List<String> values = triples.get(key);
		    	writer.println( key + "\n" + values.toString() +"\n");
		    }
			writer.println("for all axioms: " + forAllCounter);
			writer.println("for some axioms: " + forSomeCounter);
			writer.println("for max axioms: " + maxCardinalityCounter);
			writer.println("for min axioms: " + minCardinalityCounter);
			writer.println("for exactly axioms: " + cardinalityCounter);
			writer.println("properties with inverse: " + hasInverseCounter);
		    
		    writer.close();
		}
		catch (IOException e) 
		{
		   // do something
		}
	}
	
	// adds a value to a list-within-map
	private static void putAdd(Map<String, List<String>> map, String key, String val)
	{
		List<String> values = map.get(key);
		if (values == null) values = new ArrayList<>();
		values.add(val);
		map.put(key, values);
	}
	
}