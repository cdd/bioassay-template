/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.template;

import com.cdd.bao.*;

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
	
	public static class Branch
	{
		public String uri;
		public List<Branch> parents = new ArrayList<>();
		public List<Branch> children = new ArrayList<>();
		
		public Branch(String uri) {this.uri = uri;}
	}
	private List<Branch> rootBranches = new ArrayList<>();

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
	
	// fetches the roots that can be used to create some number of hierarchies
	public Branch[] getRootBranches() {return rootBranches.toArray(new Branch[rootBranches.size()]);}
	
	// ------------ private methods ------------

	private void loadLabels(File baseDir)
	{
		Model model = ModelFactory.createDefaultModel();

		// first step: load files from the packaged JAR-file, if there is one
		CodeSource jarsrc = getClass().getProtectionDomain().getCodeSource();
		if (jarsrc != null)
		{
            URL jar = jarsrc.getLocation();
            try
            {
            	ZipInputStream zip = new ZipInputStream(jar.openStream());
                ZipEntry ze = null;
            
                while ((ze = zip.getNextEntry()) != null) 
                {
                    String path = ze.getName();
                    if (path.startsWith("data/bao/") && path.endsWith(".owl"))
                    {
                    	InputStream res = getClass().getResourceAsStream("/" + path);
                    	RDFDataMgr.read(model, res, Lang.RDFXML);
                    	res.close();
                    }
                }
                
                zip.close();
            }
            catch (IOException ex) {ex.printStackTrace();}
    	}

		// second step: load files from the local directory; this is the only source when debugging; it is done second because it is valid to
		// provide content that extends-or-overwrites the default
		if (baseDir != null && baseDir.isDirectory()) for (File f : baseDir.listFiles())
		{
			if (!f.getName().endsWith(".owl")) continue;
			//Util.writeln("Loading: " + f.getPath());
			RDFDataMgr.read(model, f.getPath(), Lang.RDFXML);
		}
		
		// iterate over the list looking for label definitions
		StmtIterator iter = model.listStatements();
		while (iter.hasNext())
		{
			Statement stmt = iter.next();
			
			Resource subject = stmt.getSubject();
			Property predicate = stmt.getPredicate();
			RDFNode object = stmt.getObject();

			if (!object.isLiteral()) continue;

			String uri = subject.getURI();
			String label = object.asLiteral().getString();

			if (predicate.getLocalName().equals("label"))
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
			else if (predicate.getLocalName().equals("IAO_0000115"))
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
		
		/*Util.writeln("All labels and their meanings:");
		for (String label : labelToURI.keySet())
		{
			String[] list = labelToURI.get(label);
			Util.writeln("'" + label + "': " + Arrays.toString(list));
			for (String uri : list) 
			{
				String descr = uriToDescr.get(uri);
				if (descr != null) Util.writeln("   : " + descr);
			}
		}
		
		Util.writeln("Labelled Items:" + uriToLabel.size() + ", descriptions: " + uriToDescr.size());*/
		
		generateBranch(model);
	}
	
	// looks over the entire class inheritance system, and builds a collection of trees
	private void generateBranch(Model model)
	{
		Map<String, Branch> uriToBranch = new HashMap<>();
		
		Property subClassOf = model.createProperty(ModelSchema.PFX_RDFS + "subClassOf");
		for (StmtIterator it = model.listStatements(null, subClassOf, (RDFNode)null); it.hasNext();)
		{
			Statement st = it.next();
			String uriChild = st.getSubject().toString(), uriParent = st.getObject().toString();
			
			if (!uriToLabel.containsKey(uriChild) || !uriToLabel.containsKey(uriParent)) continue;
			
			//Util.writeln("{"+uriParent+":"+getLabel(uriParent)+"} -> {"+uriChild+":"+getLabel(uriChild)+"}");
			
			Branch child = uriToBranch.get(uriChild), parent = uriToBranch.get(uriParent);
			if (child == null) 
			{
				child = new Branch(uriChild);
				uriToBranch.put(uriChild, child);
			}
			if (parent == null)
			{
				parent = new Branch(uriParent);
				uriToBranch.put(uriParent, parent);
			}
			
			parent.children.add(child);
			child.parents.add(parent);
		}
		
		// anything with zero parents is a "root": this is all that is needed
		for (Branch branch : uriToBranch.values()) if (branch.parents.size() == 0) rootBranches.add(branch);
	}
}
