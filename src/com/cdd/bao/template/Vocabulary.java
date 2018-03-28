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

package com.cdd.bao.template;

import com.cdd.bao.*;
import com.cdd.bao.util.*;
import com.cdd.bao.validator.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;
import org.apache.commons.lang3.*;

/*
	Loads and stores the current list of vocabulary terms. Basically, it pulls in the local definition of the
	BioAssay Ontology and some of its related links, and makes them conveniently available. 
*/

public class Vocabulary
{
	private static String mutex = new String("!");
	private static Vocabulary singleton = null;
	private static String[] ontoExtraFiles = null, ontoExclFiles = null;

	private boolean loadingComplete = false;
	private Map<String, String> uriToLabel = new TreeMap<>(); // labels for each URI (one-to-one)
	private Map<String, String[]> labelToURI = new TreeMap<>(); // URIs for each label (one-to-many)
	private Map<String, String> uriToDescr = new HashMap<>(); // descriptions for each URI (many are absent)
	private Map<String, String[]> uriToExternalURLs = new HashMap<>();
	private Map<String, String[]> uriToAlternateLabels = new HashMap<>();
	private Map<String, String> uriToPubChemSource = new HashMap<>();
	private Map<String, Boolean> uriToPubChemImport = new HashMap<>();
	// URIs that are known to be involved in property & class relationships, respectively
	private Set<String> uriProperties = new HashSet<>(), uriValues = new HashSet<>();
	private Map<String, String[]> equivalence = new HashMap<>(); // interchangeable URIs: A->[B] means that all terms [B] are noted as being the same
	private Set<String> prefParent = new HashSet<>(); // preferred parent URIs: when building the tree, and there's a choice between equivalences

	private Map<String, String> remappings = new HashMap<>();
	
	private static final String SEP = "::";
	
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
		
		// convenience: first parent is special - it's usually the recommended path to trace
		public Branch firstParent()
		{
			return parents.size() == 0 ? null : parents.get(0);
		}
		
		// shallow copy: child items are pointers
		public Branch clone()
		{
			Branch dup = new Branch(uri, label);
			dup.parents.addAll(parents);
			dup.children.addAll(children);
			return dup;
		}
		public Branch deepClone()
		{
			Branch dup = new Branch(uri, label);
			for (Branch p : parents) dup.parents.add(p.deepClone());
			for (Branch c : children) dup.children.add(c.deepClone());
			return dup;
		}
		
		public String toString()
		{
			String str = "[" + label + "] <" + ModelSchema.collapsePrefix(uri) + ">\n";
			str += "parents:";
			for (Branch br : parents) str += " " + "[" + br.label + "] <" + ModelSchema.collapsePrefix(br.uri) + ">";
			str += "\nchildren:";
			for (Branch br : children) str += " " + "[" + br.label + "] <" + ModelSchema.collapsePrefix(br.uri) + ">";
			return str;
		}
	}
	
	public static class Hierarchy
	{
		public Map<String, Branch> uriToBranch = new HashMap<>();
		public List<Branch> rootBranches = new ArrayList<>();
	}
	private Hierarchy properties = null, values = null;
	
	public interface Listener
	{
		public void vocabLoadingProgress(Vocabulary vocab, float progress);		
		public void vocabLoadingException(Exception ex);
	}
	private Set<Listener> listeners = new HashSet<>();

	// ------------ public methods ------------

	// optionally call this before initialisation to specify a list of files that should also be included in the ontologies to load;
	// this is useful for supplementing the default list with custom files
	public static void setExtraOntology(String[] ontoExtraFiles)
	{
		Vocabulary.ontoExtraFiles = ontoExtraFiles;
	}

	// as above, but a list of filenames that should not be included in the ontology list (not with prefixes: matching is done by just
	// the filename proper)
	public static void setExclOntology(String[] ontoExclFiles)
	{
		Vocabulary.ontoExclFiles = ontoExclFiles;
	}

	// accesses a single instance: generally returns instantly; if this is not the first invocation, it will spawn a thread to load
	// the ontologies in the background, and return a partially loaded instance; subsequent calls will return the same instance, 
	// which may or may not have finished loading; the caller may optionally provide a listener, which will receive progress updates 
	// and error messages
	public static Vocabulary globalInstance() {return globalInstance(null);}
	public static Vocabulary globalInstance(Listener listener)
	{
		synchronized (mutex)
		{
			if (singleton != null) 
			{
				singleton.addListener(listener);
				synchronized (singleton.listeners) {for (Listener l : singleton.listeners) l.vocabLoadingProgress(singleton, 1);}
				return singleton;
			}
			
			// create a new one; note that the listener is added before the loading begins, so that exceptions can be sent
			// to the right place
			singleton = new Vocabulary();
			singleton.addListener(listener);
			new Thread(() -> 
			{
				try {singleton.load(null, null);}
				catch (Exception ex)
				{
					synchronized (singleton.listeners) {for (Listener l : singleton.listeners) l.vocabLoadingException(ex);}
				}
				
			}).start();
			return singleton;
		}
	}

	// creates an instance, which needs to be loaded subsequently (this is how the global singleton method gets it rolling)
	public Vocabulary()
	{
	}
	
	// direct constructor: use this for cases where the lifecycle is managed more explicitly
	public Vocabulary(String ontoDir, String[] extraFiles) throws IOException
	{
		load(ontoDir, extraFiles);
	}

	// initialises the vocabulary by loading up all the BAO & related terms; note that this is slow, so avoid constructing
	// this object any more often than necessary
	public void load(String ontoDir, String[] extraFiles) throws IOException
	{
		List<File> extra = new ArrayList<>();
		if (extraFiles != null) for (String fn : extraFiles) extra.add(new File(fn));
		if (ontoExtraFiles != null) for (String fn : ontoExtraFiles) extra.add(new File(fn).getCanonicalFile());
		Set<String> exclude = new HashSet<>();
		if (ontoExclFiles != null) for (String fn : ontoExclFiles) exclude.add(fn);

		// several options for BAO loading configurability; right now it goes for local files first, then looks in the JAR file (if there
		// is one); loading from an external endpoint might be interesting, too
		if (ontoDir == null)
		{
			String cwd = System.getProperty("user.dir");
			ontoDir = cwd + "/ontology";
			if (!new File(ontoDir).exists()) ontoDir = cwd + "/data/ontology";
		}
		
		try
		{
			loadLabels(new File(ontoDir), extra.toArray(new File[extra.size()]), exclude);
		}
		catch (Exception ex) 
		{
			throw new IOException("Vocabulary loading failed", ex);
		}
		finally 
		{
			loadingComplete = true;
			synchronized (listeners) {for (Listener l : listeners) l.vocabLoadingProgress(this, 1);}
		}
	}

	// as above, but does not take a directory parameter: only the expicitly indicated files will be loaded; this is intended mainly for testing
	public void loadExplicit(String[] files) throws IOException
	{
		List<File> extra = new ArrayList<>();
		for (String fn : files) extra.add(new File(fn));
		try
		{
			loadLabels(null, extra.toArray(new File[extra.size()]), new HashSet<>());	
		}
		catch (Exception ex) 
		{
			throw new IOException("Vocabulary loading failed", ex);
		}
		finally 
		{
			loadingComplete = true;
			synchronized (listeners) {for (Listener l : listeners) l.vocabLoadingProgress(this, 1);}
		}
	}
	
	// false if the vocabulary is being loaded (usually in a different thread)
	public boolean isLoaded() {return loadingComplete;}
	
	// add/remove listener, for monitoring progress toward loading of the ontologies
	public void addListener(Listener listener) 
	{
		if (listener == null) return;
		synchronized (listeners) {listeners.add(listener);}
	}
	public void removeListener(Listener listener) 
	{
		synchronized (listeners) {listeners.remove(listener);}
	}
	
	// fetches the label/description for a given URI; if there is none, returns null
	public String getLabel(String uri) {return uriToLabel.get(uri);}
	public String getDescr(String uri) {return uriToDescr.get(uri);}
	public String[] getAltLabels(String uri) {return uriToAlternateLabels.get(uri);}
	public String[] getExternalURLs(String uri) {return uriToExternalURLs.get(uri);}
	public String getPubChemSource(String uri) {return uriToPubChemSource.get(uri);}
	public boolean getPubChemImport(String uri) {return uriToPubChemImport.getOrDefault(uri, false);} // changes null to false
	
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
	
	// test for existence
	public int numProperties() {return uriProperties.size();}
	public int numValues() {return uriValues.size();}
	public boolean hasPropertyURI(String uri) {return uriProperties.contains(uri);}
	public boolean hasValueURI(String uri) {return uriValues.contains(uri);}
	
	// just the URIs involved in a property/class hierarchy
	public String[] getPropertyURIs() {return uriProperties.toArray(new String[uriProperties.size()]);}
	public String[] getValueURIs() {return uriValues.toArray(new String[uriValues.size()]);}
	
	// for a given URI, returns a list of URIs that are registered as being equivalent to it, or null if none
	public String[] equivalentURIs(String uri) {return equivalence.get(uri);}
	
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

	private void loadLabels(File baseDir, File[] extra, Set<String> exclude) throws IOException
	{
		Model model = ModelFactory.createDefaultModel();

		// preliminary analysis: done now in order to estimate the total size of the files needing to be loaded
		CodeSource jarsrc = getClass().getProtectionDomain().getCodeSource();
		Set<String> allFiles = new HashSet<>();
		if (baseDir != null && baseDir.isDirectory()) 
		{
			File[] list = baseDir.listFiles();
			for (File f : list)
			{
				String fn = f.getCanonicalPath();
				if (fn.endsWith(".owl") || fn.endsWith(".ttl")) allFiles.add(fn);
			}
		}
		for (File f : extra)
		{
			String fn = f.getCanonicalPath();
			if (fn.endsWith(".owl") || fn.endsWith(".ttl")) allFiles.add(fn);
		}		
		List<File> files = new ArrayList<>();
		long progressSize = 0, totalSize = 0;
		for (String fn : allFiles) 
		{
			File f = new File(fn);
			if (exclude.contains(f.getName())) continue;
			totalSize += f.length();
			files.add(f);
		}

		// first step: load files from the packaged JAR-file, if there is one
		if (jarsrc != null)
		{
			URL jar = jarsrc.getLocation();

			// first pass: figure out how many bytes we're talking about
			ZipInputStream zip = new ZipInputStream(jar.openStream());
			ZipEntry ze = null;
			while ((ze = zip.getNextEntry()) != null) 
			{
				String path = ze.getName();
				if (path.startsWith("data/ontology/") && (path.endsWith(".owl") || path.endsWith(".ttl"))) totalSize += ze.getSize();
			}

			// second pass: read it in
			zip = new ZipInputStream(jar.openStream());
			ze = null;
			
			while ((ze = zip.getNextEntry()) != null) 
			{
				String path = ze.getName();
				if (path.startsWith("data/ontology/") && (path.endsWith(".owl") || path.endsWith(".ttl")) && !exclude.contains(new File(path).getName()))
				{
					progressSize += ze.getSize();
					InputStream res = getClass().getResourceAsStream("/" + path);
					try {RDFDataMgr.read(model, res, path.endsWith(".ttl") ? Lang.TURTLE : Lang.RDFXML);}
					catch (Exception ex) {throw new IOException("Failed to load from JAR file: " + path);}
					res.close();
			
					float progress = (float)progressSize / totalSize;
					synchronized (listeners) {for (Listener l : listeners) l.vocabLoadingProgress(this, progress);}
				}
			}
			
			zip.close();
		}

		// second step: load files from the local directory; this is the only source when debugging; it is done second because it is valid to
		// provide content that extends-or-overwrites the default
		files.sort((f1, f2) -> (int)(f1.length() - f2.length()));
		for (File f : files)
		{
			try 
			{
				URL fileURL = new File(f.getPath()).toURI().toURL(); // changing file to a URL for passing into Jena's RDF reader			
				RDFDataMgr.read(model, fileURL.getPath(), f.getName().endsWith(".ttl") ? Lang.TURTLE : Lang.RDFXML);
			}
			catch (Exception ex) 
			{
				throw new IOException("Failed to load " + f, ex);
			}

			progressSize += f.length();
			float progress = (float)progressSize / totalSize;
			synchronized (listeners) {for (Listener l : listeners) l.vocabLoadingProgress(this, progress);}
		}

		// find remappings, as distinguished from reparenting via preferredParent
		Property batRemapTo = model.createProperty(ModelSchema.PFX_BAT + "remapTo");
		for (StmtIterator iter = model.listStatements(null, batRemapTo, (RDFNode)null); iter.hasNext();)
		{
			Statement stmt = iter.next();
			Resource subject = stmt.getSubject();
			RDFNode object = stmt.getObject();

			if (!object.isResource()) throw new IOException(ModelSchema.PFX_BAT + "remapTo directive requires a resource as the object.");

			String srcURI = subject.getURI();
			String dstURI = object.asResource().getURI();
			remappings.put(srcURI, dstURI);
		}
		RemappingChecker.validateRemappings(remappings); // throws exception if cycle detected in remappings

		Property propLabel = model.createProperty(ModelSchema.PFX_RDFS + "label");
		Property propDescr = model.createProperty(ModelSchema.PFX_OBO + "IAO_0000115");
		Property subPropOf = model.createProperty(ModelSchema.PFX_RDFS + "subPropertyOf");
		Property subClassOf = model.createProperty(ModelSchema.PFX_RDFS + "subClassOf");
		Property pubchemImport = model.createProperty(ModelSchema.PFX_BAE + "pubchemImport");
		Property pubchemSource = model.createProperty(ModelSchema.PFX_BAE + "pubchemSource");
		Property externalURL = model.createProperty(ModelSchema.PFX_BAE + "externalURL");
		Property altLabel1 = model.createProperty(ModelSchema.PFX_BAE + "altLabel");
		Property altLabel2 = model.createProperty(ModelSchema.PFX_OBO + "IAO_0000118");
		Property altLabel3 = model.createProperty(ModelSchema.PFX_OBO + "IAO_0000111");
		Property altLabel4 = model.createProperty("http://www.ebi.ac.uk/efo/alternative_term");
		Property rdfType = model.createProperty(ModelSchema.PFX_RDF + "type");
		Resource owlDataType = model.createResource(ModelSchema.PFX_OWL + "DatatypeProperty");
		Resource owlObjProp = model.createResource(ModelSchema.PFX_OWL + "ObjectProperty");
		Property notSubClass = model.createProperty(ModelSchema.PFX_BAT + "notSubClass");
		Resource resEliminated = model.createResource(ModelSchema.PFX_BAT + "eliminated");
		
		Set<String> anyProp = new HashSet<>(), anyValue = new HashSet<>();

		// pull out subclass cancellation directives
		Set<String> classBreakers = new HashSet<>(), eliminatedTerms = new HashSet<>();
		for (StmtIterator iter = model.listStatements(null, notSubClass, (RDFNode)null); iter.hasNext();)
		{
			Statement stmt = iter.next();
			Resource subject = stmt.getSubject();
			Resource object = stmt.getObject().asResource();

			String subjURI = remapIfAny(subject.getURI());
			String objURI = remapIfAny(object.getURI());
			classBreakers.add(subjURI + "::" + objURI);
		}
		for (StmtIterator iter = model.listStatements(null, rdfType, resEliminated); iter.hasNext();)
		{
			Statement stmt = iter.next();
			eliminatedTerms.add(stmt.getSubject().getURI());
		}
		eliminatedTerms.addAll(remappings.keySet()); // all remapped terms are eliminated by implication

		// iterate over the list looking for label definitions
		for (StmtIterator iter = model.listStatements(); iter.hasNext();)
		{
			Statement stmt = iter.next();
			
			Resource subject = stmt.getSubject();
			Property predicate = stmt.getPredicate();
			RDFNode object = stmt.getObject();

			if (predicate.equals(batRemapTo) && remappings.containsKey(subject.getURI())) continue;

			String subjURI = remapIfAny(subject.getURI());
			String objURI = object.isURIResource() ? remapIfAny(object.asResource().getURI()) : null;

			if (!subject.isURIResource() || !predicate.isURIResource()) continue;
			if (eliminatedTerms.contains(subjURI)) continue;

			// separately collect everything that's part of the class/property hierarchy
			if (predicate.equals(subPropOf) && object.isURIResource())
			{
				anyProp.add(objURI);
				anyProp.add(subjURI);
			}
			else if (predicate.equals(subClassOf) && object.isURIResource())
			{
				anyValue.add(objURI);
				anyValue.add(subjURI);
			}
			else if (predicate.equals(rdfType) && (object.equals(owlDataType) || object.equals(owlObjProp)))
			{
				anyProp.add(subjURI);
			}
			
			if (!object.isLiteral()) continue;

			String label = object.asLiteral().getString();

			if (predicate.equals(propLabel))
			{
				uriToLabel.put(subjURI, label);
				String[] list = labelToURI.get(label);
				labelToURI.put(label, ArrayUtils.add(list, subjURI));
			}
			else if (predicate.equals(propDescr))
			{
				uriToDescr.put(subjURI, label);
			}
			else if (predicate.equals(pubchemSource))
			{
				uriToPubChemSource.put(subjURI, label);
			}
			else if (predicate.equals(pubchemImport))
			{
				uriToPubChemImport.put(subjURI, object.asLiteral().getBoolean());
			}
			else if (predicate.equals(externalURL))
			{
				String[] list = uriToExternalURLs.get(subjURI);
				uriToExternalURLs.put(subjURI, ArrayUtils.add(list, label));
			}
			else if (predicate.equals(altLabel1) || predicate.equals(altLabel2) || predicate.equals(altLabel3) || predicate.equals(altLabel4))
			{
				String[] list = uriToAlternateLabels.get(subjURI);
				if (!ArrayUtils.contains(list, label)) uriToAlternateLabels.put(subjURI, ArrayUtils.add(list, label));
			}
		}

		// pull out the equivalences
		Property owlEquivalence = model.createProperty(ModelSchema.PFX_OWL + "equivalentClass");
		for (StmtIterator iter = model.listStatements(null, owlEquivalence, (RDFNode)null); iter.hasNext();)
		{
			Statement stmt = iter.next();
			String uri1 = remapIfAny(stmt.getSubject().getURI());
			String uri2 = remapIfAny(stmt.getObject().asResource().getURI());
			if (uri1 == null || uri2 == null) continue;
			addEquivalence(uri1, uri2);
			addEquivalence(uri2, uri1);
		}
		
		// pull in the "preferred parent" indicators from the corrections list
		Resource batPref = model.createResource(ModelSchema.PFX_BAT + "preferredParent");
		for (StmtIterator iter = model.listStatements(null, rdfType, batPref); iter.hasNext();)
		{
			Statement stmt = iter.next();
			String uri = remapIfAny(stmt.getSubject().getURI());
			prefParent.add(uri);
		}

		// final labels: these are used to override existing labels - otherwise conflicts can introduce an order dependency
		Property batLabel = model.createProperty(ModelSchema.PFX_BAT + "finalLabel");
		for (StmtIterator iter = model.listStatements(null, batLabel, (RDFNode)null); iter.hasNext();)
		{
			Statement stmt = iter.next();
			String uri = remapIfAny(stmt.getSubject().getURI());
			String label = stmt.getObject().asLiteral().getString();

			if (eliminatedTerms.contains(uri)) continue;
			
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
		properties = generateBranch(model, subPropOf, null);
		values = generateBranch(model, subClassOf, classBreakers);

		// properties need a bit more attention, because singletons need to be represented too
		for (int pass = 0; pass < 2; pass++)
		{
			for (StmtIterator it = model.listStatements(null, rdfType, pass == 0 ? owlDataType : owlObjProp); it.hasNext();)
			{
				Statement st = it.next();
				String uri = remapIfAny(st.getSubject().getURI());
				if (eliminatedTerms.contains(uri)) continue;

				if (properties.uriToBranch.containsKey(uri)) continue;
				String label = uriToLabel.get(uri);
				if (label == null) continue;
				Branch branch = new Branch(uri, label);
				properties.uriToBranch.put(uri, branch);
				properties.rootBranches.add(branch);
			}
		}
		properties.rootBranches.sort((v1, v2) -> 
		{
			if (v1.children.size() > 0 && v2.children.size() == 0) return -1;
			if (v1.children.size() == 0 && v2.children.size() > 0) return 1;
			return v1.label.compareTo(v2.label);
		});
				
		//countTripleTypes(model);
	}

	// looks over the entire class inheritance system, and builds a collection of trees
	private Hierarchy generateBranch(Model model, Property verb, Set<String> classBreakers)
	{
		Hierarchy hier = new Hierarchy();
		
		// build the tree
		for (int pass = 0; pass < 2; pass++) // want to do BAO first
		{
			for (StmtIterator it = model.listStatements(null, verb, (RDFNode)null); it.hasNext();)
			{
				Statement st = it.next();
				String uriChild = remapIfAny(st.getSubject().toString());
				String uriParent = remapIfAny(st.getObject().toString());
				
				if (uriChild.equals(uriParent)) continue; // yes this really does happen (ontology bug)

				String labelChild = uriToLabel.get(uriChild), labelParent = uriToLabel.get(uriParent);
				if (labelChild == null || labelParent == null) continue;
				
				//Util.writeln("{"+uriParent+":"+getLabel(uriParent)+"} -> {"+uriChild+":"+getLabel(uriChild)+"}");
				
				Branch child = hier.uriToBranch.get(uriChild), parent = hier.uriToBranch.get(uriParent);
				
				if (classBreakers != null && classBreakers.contains(uriChild + SEP + uriParent)) continue;

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
		
		// do child/branch reparenting whenever there is a "preferred" parent opportunity
		Map<String, String> remapTo = new HashMap<>();
		for (String prefURI : prefParent)
		{
			String[] others = equivalence.get(prefURI);
			if (others != null) for (String badURI : others) remapTo.put(badURI, prefURI);
		}
		for (Branch branch : hier.uriToBranch.values())
		{
			while (true)
			{
				boolean anything = false;
				outer: for (int i = 0; i < branch.children.size(); i++)
				{
					Branch brFrom = branch.children.get(i);
					String toURI = remapTo.get(brFrom.uri);
					if (toURI == null) continue;
					for (int j = 0; j < branch.children.size(); j++) if (i != j)
					{
						Branch brTo = branch.children.get(j);
						if (!brTo.uri.equals(toURI)) continue;
						
						brTo.children.addAll(brFrom.children);
						branch.children.remove(i);
						
						// remove every reference to the parent that is being taken out
						for (Branch cull : hier.uriToBranch.values()) 
						{
							for (int n = 0; n < cull.parents.size(); n++) if (cull.parents.get(n) == brFrom) cull.parents.set(n, brTo);
							// (may leave duplicates as stains: is this OK?)
						}
						
						anything = true;
						break outer;
					}
				}
				
				if (!anything) break;
			}
		}

		// anything with zero parents is a "root": this is all that is needed; and: any branch that has more than one parent gets checked
		// to see if some but not all have a "preferred" parent
		for (Branch branch : hier.uriToBranch.values()) 
		{
			if (branch.parents.size() == 0) hier.rootBranches.add(branch);
			else if (branch.parents.size() >= 2)
			{
				int npref = 0;
				for (Branch br : branch.parents) if (prefParent.contains(br.uri)) npref++;
				if (npref > 0 && npref < branch.parents.size())
				{
					for (Iterator<Branch> iter = branch.parents.iterator(); iter.hasNext();)
						if (!prefParent.contains(iter.next().uri)) iter.remove();
				}
			}
		}
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
	
	// to be run occasionally for interests' sake
	private void countTripleTypes(Model model)
	{
		int bao = 0, other = 0;

		Property subClassOf = model.createProperty(ModelSchema.PFX_RDFS + "subClassOf");
		StmtIterator iter = model.listStatements();
		while (iter.hasNext())
		{
			Statement stmt = iter.next();
			
			Resource subject = stmt.getSubject();
			Property predicate = stmt.getPredicate();
			RDFNode object = stmt.getObject();
			
			if (predicate.equals(subClassOf))
			{
				String uri = subject.getURI();
				if (uri.startsWith(ModelSchema.PFX_BAO)) bao++; else other++;
			}
		}
		
		Util.writeln("Total BAO classes: " + bao);
		Util.writeln("Other classes:     " + other);
	}

	// note that two URIs are equivalent to each other; should be called twice, with (A,B) and (B,A)
	private void addEquivalence(String uri1, String uri2)
	{
		String[] other = equivalence.get(uri1);
		if (other != null)
		{
			for (String look : other) if (look.equals(uri2)) return;
			other = Arrays.copyOf(other, other.length + 1);
			other[other.length - 1] = uri2;
			equivalence.put(uri1, other);
		}
		else equivalence.put(uri1, new String[]{uri2});
	}

	// if input URI is remapped, then return last URI in remapping sequence; otherwise, simply return the input URI
	// NOTE: assume that there is no cycle in remappings by the time this method is invoked
	private String remapIfAny(String uri)
	{
		while (remappings.containsKey(uri)) uri = remappings.get(uri);
		return uri;
	}
}
