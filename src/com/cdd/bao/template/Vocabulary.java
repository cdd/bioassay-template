/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.template;

import com.cdd.bao.*;
import com.cdd.bao.util.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;

/*
	Loads and stores the current list of vocabulary terms. Basically, it pulls in the local definition of the
	BioAssay Ontology and some of its related links, and makes them conveniently available. 
*/

public class Vocabulary
{
	private static String mutex = new String("!");
	private static Vocabulary singleton = null;

	private Map<String, String> uriToLabel = new TreeMap<>(); // labels for each URI (one-to-one)
	private Map<String, String[]> labelToURI = new TreeMap<>(); // URIs for each label (one-to-many)
	private Map<String, String> uriToDescr = new HashMap<>(); // descriptions for each URI (many are absent)
	// URIs that are known to be involved in property & class relationships, respectively
	private Set<String> uriProperties = new HashSet<>(), uriValues = new HashSet<>();
	
	public static class Branch
	{
		public String uri, label;
		public List<Branch> parents = new ArrayList<>();
		public List<Branch> children = new ArrayList<>();
		
		public Branch(String uri, String label)
		{
			this.uri = uri;
			this.label = label;
		}
	}
	
	public static class Hierarchy
	{
		public Map<String, Branch> uriToBranch = new HashMap<>();
		public List<Branch> rootBranches = new ArrayList<>();
	}
	private Hierarchy properties = null, values = null;

	// ------------ public methods ------------

	// accesses a single instance: if not loaded already, will block; may fail if the files are not found; threadsafe
	public static Vocabulary globalInstance() throws IOException
	{
		synchronized (mutex)
		{
			if (singleton == null) singleton = new Vocabulary();
			return singleton;
		}
	}

	// initialises the vocabulary by loading up all the BAO & related terms; note that this is slow, so avoid constructing
	// this object any more often than necessary
	public Vocabulary() throws IOException
	{
		// several options for BAO loading configurability; right now it goes for local files first, then looks in the JAR file (if there
		// is one); loading from an external endpoint might be interesting, too
		String cwd = System.getProperty("user.dir");
		try {loadLabels(new File(cwd + "/bao"));}
		catch (Exception ex) {throw new IOException("Vocabulary loading failed", ex);}
	}
	
	// fetches the label/description for a given URI; if there is none, returns null
	public String getLabel(String uri) {return uriToLabel.get(uri);}
	public String getDescr(String uri) {return uriToDescr.get(uri);}
	
	// finds the URI that matches a given label; singular version tries to disambiguate if there are multiple URIs sharing
	// the same label (there are a few of these)
	public String[] getURIList(String label) {return labelToURI.get(label);}
	public String findURIForLabel(String label)
	{
		String[] list = labelToURI.get(label);
		if (list == null) return null;
		return list[0];
	}
	
	// grab all of the URIs
	public String[] getAllURIs() {return uriToLabel.keySet().toArray(new String[uriToLabel.size()]);}
	
	// just the URIs involved in a property/class hierarchy
	public String[] getPropertyURIs() {return uriProperties.toArray(new String[uriProperties.size()]);}
	public String[] getValueURIs() {return uriValues.toArray(new String[uriValues.size()]);}
	
	/*
	// fetches a specific root branch, if there is one
	public Branch getBranch(String uri) {return uri == null || uri.length() == 0 ? null : uriToBranch.get(uri);}

	// fetches the roots that can be used to create some number of hierarchies
	public Branch[] getRootBranches() {return rootBranches.toArray(new Branch[rootBranches.size()]);}
	*/
	
	// fetch the branch structure hierarchy for either properties or values
	public Hierarchy getPropertyHierarchy() {return properties;}
	public Hierarchy getValueHierarchy() {return values;}
	
	// ------------ private methods ------------

	private void loadLabels(File baseDir) throws IOException
	{
		Model model = ModelFactory.createDefaultModel();

		// first step: load files from the packaged JAR-file, if there is one
		CodeSource jarsrc = getClass().getProtectionDomain().getCodeSource();
		if (jarsrc != null)
		{
            URL jar = jarsrc.getLocation();

        	ZipInputStream zip = new ZipInputStream(jar.openStream());
            ZipEntry ze = null;
        
            while ((ze = zip.getNextEntry()) != null) 
            {
                String path = ze.getName();
                if (path.startsWith("data/bao/") && (path.endsWith(".owl") || path.endsWith(".ttl")))
                {
                	InputStream res = getClass().getResourceAsStream("/" + path);
                	try {RDFDataMgr.read(model, res, path.endsWith(".owl") ? Lang.RDFXML : Lang.TURTLE);}
                	catch (Exception ex) {throw new IOException("Failed to load from JAR file: " + path);}
                	res.close();
                }
            }
            
            zip.close();
    	}

		// second step: load files from the local directory; this is the only source when debugging; it is done second because it is valid to
		// provide content that extends-or-overwrites the default
		if (baseDir != null && baseDir.isDirectory()) for (File f : baseDir.listFiles())
		{
			String fn = f.getName();
			if (!fn.endsWith(".owl") && !fn.endsWith(".ttl")) continue;
			//Util.writeln("Loading: " + f.getPath());
			try {RDFDataMgr.read(model, f.getPath(), fn.endsWith(".owl") ? Lang.RDFXML : Lang.TURTLE);}
			catch (Exception ex) {throw new IOException("Failed to load " + f, ex);}
		}
		//Util.writeln("Done.");
	
		Property propLabel = model.createProperty(ModelSchema.PFX_RDFS + "label");
		Property propDescr = model.createProperty(ModelSchema.PFX_OBO + "IAO_0000115");
		Property subPropOf = model.createProperty(ModelSchema.PFX_RDFS + "subPropertyOf");
		Property subClassOf = model.createProperty(ModelSchema.PFX_RDFS + "subClassOf");
		Property rdfType = model.createProperty(ModelSchema.PFX_RDF + "type");
		Resource owlDataType = model.createResource(ModelSchema.PFX_OWL + "DatatypeProperty");
		Resource owlObjProp = model.createResource(ModelSchema.PFX_OWL + "ObjectProperty");
		
		Set<String> anyProp = new HashSet<>(), anyValue = new HashSet<>();

		// iterate over the list looking for label definitions
		StmtIterator iter = model.listStatements();
		while (iter.hasNext())
		{
			Statement stmt = iter.next();
			
			Resource subject = stmt.getSubject();
			Property predicate = stmt.getPredicate();
			RDFNode object = stmt.getObject();

			// separately collect everything that's part of the class/property hierarchy
			if (predicate.equals(subPropOf) && object.isURIResource())
			{
				anyProp.add(subject.getURI());
				anyProp.add(((Resource)object).getURI());
			}
			else if (predicate.equals(subClassOf) && object.isURIResource())
			{
				anyValue.add(subject.getURI());
				anyValue.add(((Resource)object).getURI());
			}
			else if (predicate.equals(rdfType) && (object.equals(owlDataType) || object.equals(owlObjProp)))
			{
				anyProp.add(subject.getURI());
			}
			
			if (!object.isLiteral()) continue;

			String uri = subject.getURI();
			String label = object.asLiteral().getString();

			if (predicate.equals(propLabel))
			{
    			uriToLabel.put(uri, label);
    			String[] list = labelToURI.get(label);
    			if (list != null)
    			{
    				list = Arrays.copyOf(list, list.length + 1);
    				list[list.length - 1] = uri;
    				labelToURI.put(label, list);
    			}
    			else labelToURI.put(label, new String[]{uri});
			}
			else if (predicate.equals(propDescr))
			{
				uriToDescr.put(uri, label);
			}
		}

		// go over the label-to-URI list and whenever there are multiple cases, try to favour the BAO version first (expect a few to
		// slip through though)
		for (String label : labelToURI.keySet())
		{
			String[] list = labelToURI.get(label);
			if (list.length == 1) continue;
			Arrays.sort(list);
			int idx = -1;
			for (int n = 0; n < list.length; n++) if (list[n].startsWith(ModelSchema.PFX_BAO)) {idx = n; break;}
			// NOTE: if there's more than one BAO-based label, that with the lowest sort order will be retained; there are a 
			// couple of these in the list
			if (idx >= 0) labelToURI.put(label, new String[]{list[idx]});
		}
		
		// spool class/property values: only those that have a label
		for (String uri : anyProp) if (uriToLabel.containsKey(uri)) uriProperties.add(uri);
		for (String uri : anyValue) if (uriToLabel.containsKey(uri)) uriValues.add(uri);
		
		// build up the hierarchies for properties and classes, respectively
		properties = generateBranch(model, subPropOf);
		values = generateBranch(model, subClassOf);

		// properties need a bit more attention, because singletons need to be represented too
		for (int pass = 0; pass < 2; pass++)
   		for (StmtIterator it = model.listStatements(null, rdfType, pass == 0 ? owlDataType : owlObjProp); it.hasNext();)
   		{
			Statement st = it.next();
			String uri = st.getSubject().getURI();
			if (properties.uriToBranch.containsKey(uri)) continue;
			String label = uriToLabel.get(uri);
			if (label == null) continue;
			Branch branch = new Branch(uri, label);
			properties.uriToBranch.put(uri, branch);
			properties.rootBranches.add(branch);
   		}
		properties.rootBranches.sort((v1, v2) -> 
		{
			String str1 = (v1.children.size() > 0 ? "A" : "Z") + v1.label;
			String str2 = (v2.children.size() > 0 ? "A" : "Z") + v2.label;
			return str1.compareTo(str2);
		});
	}

	// looks over the entire class inheritance system, and builds a collection of trees
	private Hierarchy generateBranch(Model model, Property verb)
	{
		Hierarchy hier = new Hierarchy();
		
		for (int pass = 0; pass < 2; pass++) // want to do BAO first
		{
    		for (StmtIterator it = model.listStatements(null, verb, (RDFNode)null); it.hasNext();)
    		{
    			Statement st = it.next();
    			String uriChild = st.getSubject().toString(), uriParent = st.getObject().toString();
    
    			String labelChild = uriToLabel.get(uriChild), labelParent = uriToLabel.get(uriParent);
    			if (labelChild == null || labelParent == null) continue;
    			
    			//Util.writeln("{"+uriParent+":"+getLabel(uriParent)+"} -> {"+uriChild+":"+getLabel(uriChild)+"}");
    			
    			Branch child = hier.uriToBranch.get(uriChild), parent = hier.uriToBranch.get(uriParent);
   				boolean isBAO = uriParent.startsWith(ModelSchema.PFX_BAO);
   				if (isBAO != (pass == 0)) continue; // BAO first, other second
   				if (pass == 1 && child != null && child.parents.size() > 0) continue; // if second pass, and already parented, then don't add the non-BAO part of the hierarchy

    			if (child == null) 
    			{
    				child = new Branch(uriChild, labelChild);
    				hier.uriToBranch.put(uriChild, child);
    			}
    			if (parent == null)
    			{
    				parent = new Branch(uriParent, labelParent);
    				hier.uriToBranch.put(uriParent, parent);
    			}
    			
    			parent.children.add(child);
    			child.parents.add(parent);
    		}
		}

		// anything with zero parents is a "root": this is all that is needed
		for (Branch branch : hier.uriToBranch.values()) if (branch.parents.size() == 0) hier.rootBranches.add(branch);
		hier.rootBranches.sort((v1, v2) -> v1.label.compareTo(v2.label));

		// sort each level by name
		List<Branch> stack = new ArrayList<>(hier.rootBranches);
		while (stack.size() > 0)
		{
			Branch branch = stack.remove(0);
			branch.children.sort((v1, v2) -> v1.label.compareTo(v2.label));
			stack.addAll(branch.children);
		}
		
		return hier;
	}
}
