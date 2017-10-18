package com.cdd.bao.axioms;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.core.RDFDataset.Node;


public class AxiomCollector {
	
	public static String[] redundantURIs = {"http://www.bioassayontology.org/bao#BAO_0000035",
			"http://www.bioassayontology.org/bao#BAO_0000179",
			"http://www.bioassayontology.org/bao#BAO_0002202",
			"http://www.bioassayontology.org/bao#BAO_0000015",
			"http://www.bioassayontology.org/bao#BAO_0000026",
			"http://www.bioassayontology.org/bao#BAO_0000019", 
			"http://www.bioassayontology.org/bao#BAO_0000248",
			"http://www.bioassayontology.org/bao#BAO_0000015",
				 "http://www.bioassayontology.org/bao#BAO_0000264",
				 "http://www.bioassayontology.org/bao#BAO_0000074",
				 "http://www.bioassayontology.org/bao#BAO_0002202",
				 "http://www.bioassayontology.org/bao#BAO_0003075",
				 "http://www.bioassayontology.org/bao#BAO_0000019",
				 "http://www.bioassayontology.org/bao#BAO_0000029"};
	
    public static Map<String, Set<String>> onlyAxioms = ScanAxioms.onlyAxioms;
	
	public static Map<String, String> uriToLabel = ScanAxioms.uriToLabel;
	
	public static ArrayList<AssayAxiomsAll> axiomsForAll = ScanAxioms.axiomsForAll;
	public static ArrayList<AssayAxiomsSome> axiomsForSome = ScanAxioms.axiomsForSome;
	
	public static ArrayList<AssayAxiomsAll> nonRedundantAxiomsForAll = new ArrayList<AssayAxiomsAll>();
	public static ArrayList<AssayAxiomsSome> nonRedundantAxiomsForSome = new ArrayList<AssayAxiomsSome>();
	public static HashMap someAxiomsMap = new HashMap<String, AssayAxiomsSome>();
	public static HashMap allAxiomsMap = new HashMap<String, AssayAxiomsAll>();
	public static HashMap axiomMap = new HashMap<String, ArrayList<String>>();
	
	
	//Alright so axiom elimination is two parts in Alex's mind:
	// first eliminate the URIs that are already in the common assay template
	//but for those you can add checks for security and dependencies
	//second you want to eliminate general axioms in the form
	//assay has bioassay spec some bioassay spec
	
	
	public static final class AssayAxiomsAll{
		public String classURI;
		public String classLabel;
		public String axiomType;
		public String predicateLabel;
		public String predicateURI;
		public String objectLabels;
		public String objectURIs;	
		public String[] uriArray;
			
		public AssayAxiomsAll(String cURI, String cLabel, String aType, String pLabel, String pURI, String oLabels, String oURIs ){
			this.classURI = cURI;
			this.classLabel = cLabel;
			this.axiomType = aType;
			this.predicateLabel = pLabel;
			this.predicateURI = pURI;
			this.objectLabels = oLabels;
			this.objectURIs = oURIs;
		}
		
		 public String processURIArray(String[] uriArray){
	        	
				String uris = "";
				uriArray = this.uriArray;
				for(int i =0; i<uriArray.length; i++){
					uris +=uriArray[i]+ "\t";
					
				}
				       
				return uris;
			}
	        
	       public String labelsFromURIArray(String[] uriArray){
	        	
				String uris = "";
				String labels ="";
				uriArray = this.uriArray;
				for(int i =0; i<uriArray.length; i++){
					uris +=uriArray[i]+ "\t";
					if(uriToLabel.get(uriArray[i]) != null)
						labels += uriToLabel.get(uriArray[i]) + "\t";
					
				}
				       
				return labels;
			}
	       
	       public static AssayAxioms generalizeOnlyAxiom (AssayAxiomsAll onlyAxiom){
	   		
	   		/*mapClass2Axioms.put( new AssayAxioms((mapSome.get(classURI).getClassURI()),
	   		    			(mapSome.get(classURI).getClassLabel()),(mapSome.get(classURI).getAxiomType()),
	   		    			(mapSome.get(classURI).getPredicateLabel()), (mapSome.get(classURI).getPredicateURI()),
	   		    			(mapSome.get(classURI).getObjectLabels()), (mapSome.get(classURI).getObjectURIs())), classURI);*/
	   		
	   		AssayAxioms genAxiom = null;
	   		
	   		genAxiom = new AssayAxioms(onlyAxiom.getClassLabel(), onlyAxiom.getClassLabel(), onlyAxiom.getAxiomType(), 
	   						           onlyAxiom.getPredicateLabel(), onlyAxiom.getPredicateURI(), onlyAxiom.getObjectLabels(), onlyAxiom.getObjectURIs());
	   		
	   		
	   		return genAxiom;
	   		
	   		
	   		
	   	}
		public AssayAxiomsAll(String cURI, String pURI, String oURIs, String aType, String[] uriArray){
			this.classURI = cURI;
			this.axiomType = aType;
			this.predicateURI = pURI;
			this.objectURIs = oURIs;
			this.uriArray = uriArray;
		} 
		
		public String[] getURIArray()
		{
			return this.uriArray;
		}

		public String getClassURI()
		{
			return classURI;
		}

		public String getClassLabel()
		{
			return uriToLabel.get(this.classURI);
		}

		public String getPredicateURI()
		{
			return predicateURI;
		}
		
		public String getPredicateLabel(){
			return uriToLabel.get(this.predicateURI);
		}
		
		public String getObjectURIs(){
			objectURIs = objectURIs.replaceAll("null", "");
			Pattern pattern = Pattern.compile("\\[(.*?)\\]");
			Matcher matcher = pattern.matcher(objectURIs);
			if (matcher.find()) 
			     this.objectURIs +=  matcher.group(1) +"\t";
			
			
			return this.objectURIs;
		}
		public String eliminateRedundantURIs(){
			
			String currentURI = this.objectURIs;
			
			currentURI = currentURI.replaceAll("null", "");
			Pattern pattern = Pattern.compile("\\[(.*?)\\]");
			Matcher matcher = pattern.matcher(currentURI);
			while (matcher.find()) 
				 currentURI =  matcher.group(1);
			
			
			
			 	 //String currentURI = this.objectURIs;
			 	 
			 	 if(Arrays.asList(redundantURIs).contains(currentURI))
			 	 {	// System.out.println("told ya!");
			 		 currentURI.replaceAll(currentURI, "");
			 		 currentURI += "\t redundant axiom";
			 	 }
			 	 else
			 		 currentURI = this.objectURIs;
			       
			return currentURI;
		}
		
		public String getObjectLabels(){
			
			Pattern pattern = Pattern.compile("\\[(.*?)\\]");
			Matcher matcher = pattern.matcher(objectURIs);
			while (matcher.find()) 
			     this.objectLabels +=  uriToLabel.get(matcher.group(1)) +"\t";
			
			objectLabels = objectLabels.replaceAll("null", "");
			return this.objectLabels;		
		}
		
		public String getAxiomType(){
			return this.axiomType;
		}
		
		
		public String toString(){
			String axiomString = null;
			axiomString = this.getClassURI() + " ; "+ this.getPredicateURI() + " ; "+ 
		                       this.getObjectURIs() + " ; " + this.getAxiomType() + " . ";
		                       
			return axiomString;
		}
		
	}
	
	public static final class AssayAxiomsSome{
		public  String classURI;
		public String classLabel;
		public String axiomType;
		public String predicateLabel;
		public String predicateURI;
		public String objectLabels;
		public String objectURIs;	
		public String[] uriArray;
		
		public AssayAxiomsSome(String cURI, String cLabel, String pLabel, String pURI, String oLabels, String oURIs, String aType){
			this.classURI = cURI;
			this.classLabel = cLabel;
			this.axiomType = aType;
			this.predicateLabel = pLabel;
			this.predicateURI = pURI;
			this.objectLabels = oLabels;
			this.objectURIs = oURIs;
		} 
		
		public AssayAxiomsSome(String cURI, String pURI, String oURIs, String aType,String[] uriArray){
			this.classURI = cURI;
			this.axiomType = aType;
			this.predicateURI = pURI;
			this.objectURIs = oURIs;
			this.uriArray = uriArray;
		} 
		
		
		public String getClassURI(){
			return classURI;
		}
		
		public String getClassLabel(){
			return uriToLabel.get(this.classURI);
		}
		
		public String getPredicateURI(){
			return predicateURI;
		}
		
		public String getPredicateLabel(){
			return uriToLabel.get(this.predicateURI);
		}
		
		public String getObjectURIs(){
			objectURIs = objectURIs.replaceAll("null", "");
			Pattern pattern = Pattern.compile("\\[(.*?)\\]");
			Matcher matcher = pattern.matcher(objectURIs);
			if (matcher.find()) 
			     this.objectURIs +=  matcher.group(1) +"\t";
			
			
			return this.objectURIs;
		}
		
		public String[] getURIArray(){
			return this.uriArray;
		}
		
		public static AssayAxioms generalizeSomeAxiom (AssayAxiomsSome someAxiom){
			
			/*mapClass2Axioms.put( new AssayAxioms((mapSome.get(classURI).getClassURI()),
			    			(mapSome.get(classURI).getClassLabel()),(mapSome.get(classURI).getAxiomType()),
			    			(mapSome.get(classURI).getPredicateLabel()), (mapSome.get(classURI).getPredicateURI()),
			    			(mapSome.get(classURI).getObjectLabels()), (mapSome.get(classURI).getObjectURIs())), classURI);*/
			
			AssayAxioms genAxiom = null;
			
			genAxiom = new AssayAxioms(someAxiom.getClassLabel(), someAxiom.getClassLabel(), someAxiom.getAxiomType(), 
							           someAxiom.getPredicateLabel(), someAxiom.getPredicateURI(), someAxiom.getObjectLabels(), someAxiom.getObjectURIs());
			
			
			return genAxiom;
			
			
			
		}
		
		
		public String eliminateRedundantURIs(){
			
			String currentURI = this.objectURIs;
			
			currentURI = currentURI.replaceAll("null", "");
			Pattern pattern = Pattern.compile("\\[(.*?)\\]");
			Matcher matcher = pattern.matcher(currentURI);
			while (matcher.find()) 
				 currentURI =  matcher.group(1);
			
			
			
			 	 //String currentURI = this.objectURIs;
			 	 
			 	 if(Arrays.asList(redundantURIs).contains(currentURI))
			 	 {	// System.out.println("told ya!");
			 		 currentURI.replaceAll(currentURI, "");
			 		 currentURI += "\t redundant axiom";
			 	 }
			 	 else
			 		 currentURI = this.objectURIs;
			       
			return currentURI;
		}
		
        public String processURIArray(String[] uriArray){
        	
			String uris = "";
			uriArray = this.uriArray;
			for(int i =0; i<uriArray.length; i++){
				uris +=uriArray[i]+ "\t";
				
			}
			       
			return uris;
		}
        
       public String labelsFromURIArray(String[] uriArray){
        	
			String uris = "";
			String labels ="";
			uriArray = this.uriArray;
			for(int i =0; i<uriArray.length; i++){
				uris +=uriArray[i]+ "\t";
				if(uriToLabel.get(uriArray[i]) != null)
					labels += uriToLabel.get(uriArray[i]) + "\t";
				
			}
			       
			return labels;
		}
		
		public String getObjectLabels(){
			
			Pattern pattern = Pattern.compile("\\[(.*?)\\]");
			Matcher matcher = pattern.matcher(objectURIs);
			while (matcher.find()) 
			     this.objectLabels +=  uriToLabel.get(matcher.group(1)) +"\t";
			
			objectLabels = objectLabels.replaceAll("null", "");
			return this.objectLabels;		
		}
		
		public String getAxiomType(){
			return this.axiomType;
		}
		
		public String toString(){
			String axiomString = null;
			axiomString = this.getClassURI() + " ; "+ this.getPredicateURI() + " ; "+ 
		                       this.getObjectURIs() + " ; " + this.getAxiomType() + " . ";
		                       
			return axiomString;
		}
		
	}
	
	
	public static final class AssayAxioms{
		public String classURI;
		public String classLabel;
		public String axiomType;
		public String predicateLabel;
		public String predicateURI;
		public String objectLabels;
		public String objectURIs;	
		public String[] uriArray;
			
		public AssayAxioms(String cURI, String cLabel, String aType, String pLabel, String pURI, String oLabels, String oURIs ){
			this.classURI = cURI;
			this.classLabel = cLabel;
			this.axiomType = aType;
			this.predicateLabel = pLabel;
			this.predicateURI = pURI;
			this.objectLabels = oLabels;
			this.objectURIs = oURIs;
		}
		
		 public String processURIArray(String[] uriArray){
	        	
				String uris = "";
				uriArray = this.uriArray;
				for(int i =0; i<uriArray.length; i++){
					uris +=uriArray[i]+ "\t";
					
				}
				       
				return uris;
			}
	        
	       public String labelsFromURIArray(String[] uriArray){
	        	
				String uris = "";
				String labels ="";
				uriArray = this.uriArray;
				for(int i =0; i<uriArray.length; i++){
					uris +=uriArray[i]+ "\t";
					if(uriToLabel.get(uriArray[i]) != null)
						labels += uriToLabel.get(uriArray[i]) + "\t";
					
				}
				       
				return labels;
			}
		public AssayAxioms(String cURI, String pURI, String oURIs, String aType, String[] uriArray){
			this.classURI = cURI;
			this.axiomType = aType;
			this.predicateURI = pURI;
			this.objectURIs = oURIs;
			this.uriArray = uriArray;
		} 
		
		public AssayAxioms(String cURI, String pURI, String oURIs, String[] uriArray){
			this.classURI = cURI;
			this.predicateURI = pURI;
			this.objectURIs = oURIs;
			this.uriArray = uriArray;
		} 
		
		public String[] getURIArray(){
			return this.uriArray;
		}
		public String getClassURI(){
			return classURI;
		}
		
		public String getClassLabel(){
			return uriToLabel.get(this.classURI);
		}
		
		public String getPredicateURI(){
			return predicateURI;
		}
		
		public String getPredicateLabel(){
			return uriToLabel.get(this.predicateURI);
		}
		
		public String getObjectURIs(){
			objectURIs = objectURIs.replaceAll("null", "");
			Pattern pattern = Pattern.compile("\\[(.*?)\\]");
			Matcher matcher = pattern.matcher(objectURIs);
			if (matcher.find()) 
			     this.objectURIs +=  matcher.group(1) +"\t";
			
			
			return this.objectURIs;
		}
		public String eliminateRedundantURIs(){
			
			String currentURI = this.objectURIs;
			
			currentURI = currentURI.replaceAll("null", "");
			Pattern pattern = Pattern.compile("\\[(.*?)\\]");
			Matcher matcher = pattern.matcher(currentURI);
			while (matcher.find()) 
				 currentURI =  matcher.group(1);
			
			
			
			 	 //String currentURI = this.objectURIs;
			 	 
			 	 if(Arrays.asList(redundantURIs).contains(currentURI))
			 	 {	// System.out.println("told ya!");
			 		 currentURI.replaceAll(currentURI, "");
			 		 currentURI += "\t redundant axiom";
			 	 }
			 	 else
			 		 currentURI = this.objectURIs;
			       
			return currentURI;
		}
		
		public String getObjectLabels(){
			
			Pattern pattern = Pattern.compile("\\[(.*?)\\]");
			Matcher matcher = pattern.matcher(objectURIs);
			while (matcher.find()) 
			     this.objectLabels +=  uriToLabel.get(matcher.group(1)) +"\t";
			
			objectLabels = objectLabels.replaceAll("null", "");
			return this.objectLabels;		
		}
		
		public String getAxiomType(){
			return this.axiomType;
		}
		
		public String toString(){
			String axiomString = null;
			axiomString = this.getClassURI() + " ; "+ this.getPredicateURI() + " ; "+ 
		                       this.getObjectURIs() + " ; " + this.getAxiomType() + " . ";
		                       
			return axiomString;
		}
		
		
		
	}
	
	
	public static final class PropertiesWithInverse{
		
	}

		
	public AssayAxiomsAll axiomsAll = null;
	public AssayAxiomsSome axiomsSome = null;
	
	

	
/*
	
	public static Map<String, Set<String>> onlyAxioms = ScanAxioms.onlyAxioms;
	
	public static Map<String, String> uriToLabel = ScanAxioms.uriToLabel;
	
	public static ArrayList<AssayAxiomsAll> axiomsForAll = ScanAxioms.axiomsForAll;
	public static ArrayList<AssayAxiomsSome> axiomsForSome = ScanAxioms.axiomsForSome;
	
	*/
	
	public static JSONArray serialiseAxiom() throws IOException
	{
		JSONArray jsonFinal = new JSONArray();

	
		
		for( AssayAxiomsAll axiom : axiomsForAll)
		{	 JSONObject json = new JSONObject();
			try
			{	
				
				if (!(axiom.eliminateRedundantURIs().endsWith("redundant axiom"))){
				
					//JSONObject obj = new JSONObject();
					json.put("classURI", axiom.getClassURI() );
					json.put("classLabel", axiom.getClassLabel());
					json.put("predicateURI", axiom.getPredicateURI());
					json.put("predicateLabel", axiom.getPredicateLabel());
					//json.put("objectURI", axiom.eliminateRedundantURIs());
					//json.put("objectLabels", axiom.getObjectLabels());
					json.put("objectURIs", axiom.processURIArray(axiom.getURIArray()));
					json.put("objectLabels", axiom.labelsFromURIArray(axiom.getURIArray()));
					json.put("axiomType", axiom.getAxiomType());
					//public AssayAxiomsSome(String cURI, String pURI, String oURIs, String aType,String[] uriArray)
					nonRedundantAxiomsForAll.add(new AssayAxiomsAll (axiom.getClassURI(), axiom.getPredicateURI(), 
							                       axiom.getObjectURIs(), axiom.getAxiomType(),
							                       axiom.getURIArray()));
					
					jsonFinal.put( json );
				 }	
					
				
			}catch(Exception e){
				System.out.println("you failed to generate the JSON!");
			}
		 }
		
		
		try {
			axiomJSONWriter("onlyNew.json",jsonFinal);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsonFinal;
		
	}
	
	public static JSONArray serialiseAxiomSome() throws IOException
	{
		JSONArray jsonFinal = new JSONArray();
		
		for( AssayAxiomsSome axiom : axiomsForSome)
		{	 JSONObject json = new JSONObject();
			try
			{	
				
				if (!(axiom.eliminateRedundantURIs().endsWith("redundant axiom"))){
				
					//JSONObject obj = new JSONObject();
					json.put("classURI", axiom.getClassURI() );
					json.put("classLabel", axiom.getClassLabel());
					json.put("predicateURI", axiom.getPredicateURI());
					json.put("predicateLabel", axiom.getPredicateLabel());
					json.put("objectURIs", axiom.processURIArray(axiom.getURIArray()));
					json.put("objectLabels", axiom.labelsFromURIArray(axiom.getURIArray()));
					//json.put("objectURI", axiom.eliminateRedundantURIs());
					//json.put("objectLabels", axiom.getObjectLabels());
					json.put("axiomType", axiom.getAxiomType());
					nonRedundantAxiomsForSome.add(new AssayAxiomsSome (axiom.getClassURI(), axiom.getPredicateURI(), 
		                       axiom.getObjectURIs(), axiom.getAxiomType(),
		                       axiom.getURIArray()));
					
					jsonFinal.put( json );
				 }
					
				
			}catch(Exception e){
				System.out.println("you failed to generate the JSON!");
			}
		 }
		
		
		try {
			axiomJSONWriter("someNew.json",jsonFinal);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsonFinal;
		
		
	}
	
	
	
  
	
	 public void mergeAxiomMaps() {
		    Map< AssayAxioms, String> mapClass2Axioms = new HashMap< AssayAxioms, String>();
		    Map<AssayAxiomsAll,String> mapAll = new HashMap<AssayAxiomsAll,String>();
		    Map<AssayAxiomsSome, String> mapSome = new HashMap<AssayAxiomsSome, String>();
		    
		    for(AssayAxiomsSome axiom : axiomsForSome){
		    	mapSome.put(axiom, axiom.getClassURI());
		    	mapClass2Axioms.put(axiom.generalizeSomeAxiom(axiom), axiom.getClassURI());
		    	System.out.println("Class Label " + axiom.getClassLabel() + "Axiom Some String " + axiom.toString() + "Size of Map: "+ mapSome.size());
		    }
		    
		    for(AssayAxiomsAll axiom : axiomsForAll){
		    	mapAll.put(axiom, axiom.getClassURI());
		    	mapClass2Axioms.put(axiom.generalizeOnlyAxiom(axiom), axiom.getClassURI());
		    	System.out.println("Class Label " + axiom.getClassLabel() + "Axiom All String " + axiom.toString()  + "Size of Map: "+ mapAll.size());
		    }
		    
		    
		    
	  /*ArrayList<AssayAxioms> validAxiomsInBAO = new ArrayList<AssayAxioms>();
	  
		    
		    Set<String> valueSet = null;
		    valueSet.retainAll(mapSome.values());
		    valueSet.retainAll(mapAll.values());
		    
		    for (String classURI :valueSet){
		    	if(mapSome.containsValue(classURI)){
		    		
		    		AssayAxioms newAxiom = new AssayAxioms()
		    		mapClass2Axioms.put(axiomsForSome., value)
		    		
		    	}
		    	mapClass2Axioms.put( new AssayAxioms((mapSome.get(classURI).getClassURI()),
		    			(mapSome.get(classURI).getClassLabel()),(mapSome.get(classURI).getAxiomType()),
		    			(mapSome.get(classURI).getPredicateLabel()), (mapSome.get(classURI).getPredicateURI()),
		    			(mapSome.get(classURI).getObjectLabels()), (mapSome.get(classURI).getObjectURIs())), classURI);
		    	
		    }*/
		    
		    System.out.println("Map Class To axioms Map : \n"+mapSome);
		    System.out.println("Map Class To axioms Map : \n"+mapAll);
		    System.out.println("Map Class To axioms Map : \n"+mapClass2Axioms);
		    System.out.println("Size of Axioms for Some : " + axiomsForSome.size());
		    System.out.println("Size of Axioms for All : " + axiomsForAll.size());
		    System.out.println("Size of Some Map "  +  mapSome.size()); 
		    System.out.println("Size of All Map "  +  mapAll.size());
		    System.out.println("Size of Merged Map "  +  mapClass2Axioms.size());
		    
	 }
	 /*public static <AssayAxioms, String> Set<AssayAxioms> getKeysByValue(Map<AssayAxioms, String> map, String value) {
		    Set<AssayAxioms> keys = new Set<AssayAxioms>();
		    for (Entry<AssayAxioms, String> entry : map.entrySet()) {
		        if (Object.equals(value, entry.getValue())) {
		            keys.add(entry.getKey());
		        }
		    }
		    return keys;
		}*/
	
	
	public static void collectAxiomsList(){
		for(AssayAxiomsSome axiom : nonRedundantAxiomsForSome){
			axiomMap.put(axiom.getClassURI(), axiom.getURIArray());
		}
	}
	
	public static void findAllAxiomsOfAssay() throws IOException{
		JSONArray fileArray = new JSONArray();
		for( AssayAxiomsAll axiom : nonRedundantAxiomsForAll)
		{
			fileArray.put(findAllAxiomsOfAssay(axiom.getClassURI()));
		}
		try {
			axiomJSONWriter("AllAxiomsPerClass.json",fileArray);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static JSONArray findAllAxiomsOfAssay(String uri) throws IOException{
		//in this method I want to use the two arraylists of nonRedundantAxioms and put the axioms per class together.
		JSONArray fileArray = new JSONArray();
		for( AssayAxiomsAll axiom : nonRedundantAxiomsForAll)
		{
			JSONArray jsonFinal = new JSONArray();
			for(AssayAxiomsSome axiomSome : nonRedundantAxiomsForSome){
				if((axiom.getClassURI()).equals(axiomSome.getClassURI())){
					
					 JSONObject json = new JSONObject();
						try
						{	
							
							if (!(axiom.eliminateRedundantURIs().endsWith("redundant axiom"))){
							
								//JSONObject obj = new JSONObject();
								json.put("classURI", axiom.getClassURI() );
								json.put("classLabel", axiom.getClassLabel());
								json.put("predicateURI", axiom.getPredicateURI());
								json.put("predicateLabel", axiom.getPredicateLabel());
								json.put("predicateURI_forSome", axiomSome.getPredicateURI());
							//	json.put("predicateLabel_forSome", axiomSome.getPredicateLabel());
							//	json.put("objectURIs_forSomeAxioms", axiomSome.processURIArray(axiomSome.getURIArray()));
							//	json.put("objectLabels_forSomeAxioms", axiomSome.labelsFromURIArray(axiomSome.getURIArray()));
								json.put("objectURIs", axiom.processURIArray(axiom.getURIArray()));
								json.put("objectLabels", axiom.labelsFromURIArray(axiom.getURIArray()));
								//json.put("axiomType", axiom.getAxiomType());
								//if one of my objects is a sub class of assay method, 
								//I want to carry its axioms to the assay level as well for this exercise.
								//actually it doesn't have to be assay method
								//if I have further axioms connected to my object URIs, I want them to 
								//connect to my assay json object.
								//if(Arrays.asList(axiomSome.getURIArray()).contains("http://www.bioassayontology.org/bao#BAO_0003028")){
								
								String[] forSomeObjURIs = axiomSome.getURIArray();
								ArrayList forSomeObjURIsList = new ArrayList(Arrays.asList(forSomeObjURIs));									for(int i = 0; i< forSomeObjURIs.length; i++){
										if(nonRedundantAxiomsForSome.contains(forSomeObjURIs[i])){//this is one of the obj URIs
											findAllAxiomsOfAssay(forSomeObjURIs[i]);
										}
								
								}
							
								
								jsonFinal.put( json );
							 }	
								fileArray.put(jsonFinal);
							
						}catch(Exception e){
							System.out.println("you failed to generate the JSON!");
						}
						
					
				}
			}
			
		}
		
		return fileArray;
		
	}
	
	
	
	public static void axiomJSONWriter(String fileName, JSONArray obj) throws IOException, JSONException{
		FileWriter axiomJSON = new FileWriter(fileName);
		try{
			
			//for(int n = 0; n < obj.length(); n++)
			//{
			  //  JSONObject object = obj.getJSONObject(n);
			    axiomJSON.write(obj.toString());
			  
			//}
			
			System.out.println("Successfully copied JSONObject to file...");
		}catch(IOException e){
			e.printStackTrace();
			
		}finally{
			axiomJSON.flush();
			axiomJSON.close();
		}
		System.out.println("Done with file " + fileName + "...");
	}
	
	
	public void  createJSON(String fileName, JSONArray jsonArray) throws JSONException, IOException{
		
		axiomJSONWriter(fileName,jsonArray);
		
	}
	
		
	
}
