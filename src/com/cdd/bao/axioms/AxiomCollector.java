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
			
		public AssayAxiomsAll(String cURI, String cLabel, String aType, String pLabel, String pURI, String oLabels, String oURIs){
			this.classURI = cURI;
			this.classLabel = cLabel;
			this.axiomType = aType;
			this.predicateLabel = pLabel;
			this.predicateURI = pURI;
			this.objectLabels = oLabels;
			this.objectURIs = oURIs;
		}
		
		public AssayAxiomsAll(String cURI, String pURI, String oURIs, String aType){
			this.classURI = cURI;
			this.axiomType = aType;
			this.predicateURI = pURI;
			this.objectURIs = oURIs;
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
					json.put("objectURI", axiom.eliminateRedundantURIs());
					json.put("objectLabels", axiom.getObjectLabels());
					json.put("axiomType", axiom.getAxiomType());
					
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
					json.put("objectURI", axiom.eliminateRedundantURIs());
					json.put("objectLabels", axiom.getObjectLabels());
					json.put("axiomType", axiom.getAxiomType());
					json.put("arrayURIs", axiom.processURIArray(axiom.getURIArray()));
					json.put("arrayLabels", axiom.labelsFromURIArray(axiom.getURIArray()));
					
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
