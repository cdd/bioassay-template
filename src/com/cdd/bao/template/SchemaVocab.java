/*
	BioAssay Express (BAE)

	(c) 2016-2017 Collaborative Drug Discovery Inc.
*/

package com.cdd.bao.template;

import com.cdd.bao.util.*;
import com.cdd.bao.template.*;
import static com.cdd.bao.template.Schema.*;
import static com.cdd.bao.template.Vocabulary.*;

import java.util.*;
import java.io.*;

/*
	SchemaVocab: a distillation of the triple-derived Vocabulary class that provides a memory-efficient/fast-readable version of
	just the information that is required to construct SchemaTree instances for use by the rest of the project.
	
	Note that whenever a binary dump file fill of trees is deserialised, it is automatically submitted to the SchemaTree class's
	internal cache, meaning that it is from this point thereafter accessible via that singleton.
*/

public class SchemaVocab
{
	private String[] prefixes;

	public final static class StoredTerm
	{
		public String uri;
		public String label, descr;
	}
	private StoredTerm[] termList;
	private Map<String, Integer> termLookup = new HashMap<>(); // uri-to-index into termList

	public final static class StoredTree
	{
		public String schemaPrefix; // unique identifier for the template
		public String locator; // the assignment branch locator in the template
		public Assignment assignment;
		public SchemaTree tree;
	}
	private List<StoredTree> treeList = new ArrayList<>();
		
	private final String SEP = "::";

	// ------------ public methods ------------

	// instantiate a new object by distilling the relevant information from model-derived vocabulary & templates
	public SchemaVocab(Vocabulary vocab, Schema[] templates)
	{
		Set<String> allTerms = new HashSet<>();
	
		for (int n = 0; n < templates.length; n++)
		{
			List<Schema.Group> stack = new ArrayList<>();
			stack.add(templates[n].getRoot());
			while (stack.size() > 0)
			{
				Schema.Group grp = stack.remove(0);
				for (Schema.Assignment assn : grp.assignments)
				{
					StoredTree stored = new StoredTree();
					stored.schemaPrefix = templates[n].getSchemaPrefix();
					stored.locator = templates[n].locatorID(assn);
					stored.assignment = assn;
					stored.tree = new SchemaTree(assn, vocab);
					treeList.add(stored);
					// (... and a map version too...)

					allTerms.add(assn.propURI);					
					for (SchemaTree.Node node : stored.tree.getFlat()) allTerms.add(node.uri);
				}
				stack.addAll(grp.subGroups);
			}
		}
		
		// (actually want all terms, just in case; because there are residuals: maybe take them out later)
		for (String uri : vocab.getAllURIs()) allTerms.add(uri);
		
		String[] termURI = allTerms.toArray(new String[allTerms.size()]);
		Arrays.sort(termURI);
		termList = new StoredTerm[termURI.length];
		for (int n = 0; n < termURI.length; n++)
		{
			termList[n] = new StoredTerm();
			termList[n].uri = termURI[n];
			termList[n].label = vocab.getLabel(termURI[n]);
			termList[n].descr = vocab.getDescr(termURI[n]);

			termLookup.put(termURI[n], n);
		}
		
		derivePrefixes();

		/*Util.writeln("** unique terms: " + allTerms.size());
		Util.writeln("** prefixes: " + prefixes.length);
		for (String pfx : prefixes) Util.writeln("    " + pfx);*/
	}
	
	// write the content as raw binary
	public void serialise(OutputStream ostr) throws IOException
	{
		DataOutputStream data = new DataOutputStream(ostr);
		
		data.writeInt(prefixes.length);
		for (String pfx : prefixes) data.writeUTF(pfx);
		
		data.writeInt(termList.length);
		for (StoredTerm term : termList)
		{
			int pfx = -1;
			String uri = term.uri;
			for (int n = 0; n < prefixes.length; n++) if (uri.startsWith(prefixes[n]))
			{
				pfx = n;
				uri = uri.substring(prefixes[n].length());
				break;
			}
			data.writeInt(pfx);
			data.writeUTF(uri);
			data.writeUTF(term.label == null ? "" : term.label);
			data.writeUTF(term.descr == null ? "" : term.descr);
		}
		
		data.writeInt(treeList.size());
		for (int n = 0; n < treeList.size(); n++)
		{
			StoredTree stored = treeList.get(n);
			data.writeUTF(stored.schemaPrefix);
			data.writeUTF(stored.locator);
			
			SchemaTree.Node[] flat = stored.tree.getFlat();
			data.writeInt(flat.length);
			for (SchemaTree.Node node : flat)
			{
				data.writeInt(termLookup.get(node.uri));
				data.writeInt(node.depth);
				data.writeInt(node.parentIndex);
				data.writeInt(node.childCount);
				data.writeInt(node.schemaCount);
				data.writeBoolean(node.inSchema);
				data.writeBoolean(node.isExplicit);
			}
		}
	}
	
	// read the raw binary content; a list of all available templates needs to be provided; note that the loaded trees
	// are all kept in the SchemaTree's static cache; note also that it is important to make sure that the templates
	// are persistent, because the individual assignment objects are used for direct lookups
	public static SchemaVocab deserialise(InputStream istr, Schema[] templates) throws IOException
	{
		DataInputStream data = new DataInputStream(istr);
	
		SchemaVocab sv = new SchemaVocab();
		
		int nprefix = data.readInt();
		sv.prefixes = new String[nprefix];
		for (int n = 0; n < nprefix; n++) sv.prefixes[n] = data.readUTF();

		int nterm = data.readInt();
		sv.termList = new StoredTerm[nterm];
		for (int n = 0; n < nterm; n++)
		{
			StoredTerm term = new StoredTerm();
			int pfx = data.readInt();
			term.uri = data.readUTF();
			if (pfx >= 0) term.uri = sv.prefixes[pfx] + term.uri;
			term.label = data.readUTF();
			term.descr = data.readUTF();
			sv.termList[n] = term;
			sv.termLookup.put(term.uri, n);
		}
		
		int ntree = data.readInt();
		for (int n = 0; n < ntree; n++)
		{
			StoredTree stored = new StoredTree();
			stored.schemaPrefix = data.readUTF();
			stored.locator = data.readUTF();
			
			for (Schema schema : templates) if (stored.schemaPrefix.equals(schema.getSchemaPrefix()))
			{
				stored.assignment = schema.obtainAssignment(stored.locator);
				break;
			}
			
			int nflat = data.readInt();
			SchemaTree.Node[] flat = new SchemaTree.Node[nflat];
			for (int i = 0; i < nflat; i++)
			{
				SchemaTree.Node node = new SchemaTree.Node();
				int termidx = data.readInt();
				node.uri = sv.termList[termidx].uri;
				node.label = sv.termList[termidx].label;
				node.descr = sv.termList[termidx].descr;
				
				node.depth = data.readInt();
				node.parentIndex = data.readInt();
				node.childCount = data.readInt();
				node.schemaCount = data.readInt();
				node.inSchema = data.readBoolean();
				node.isExplicit = data.readBoolean();
				
				if (node.parentIndex >= 0)
				{
					node.parent = flat[node.parentIndex];
					node.parent.children.add(node);
				}
				
				flat[i] = node;
			}
			stored.tree = new SchemaTree(flat, stored.assignment);
			
			sv.treeList.add(stored);
		}
		
		return sv;
	}
	
	// returns the object for a given term, based on the URI; if not in the vocabulary, returns null
	public StoredTerm getTerm(String uri)
	{
		int idx = termLookup.getOrDefault(uri, -1);
		if (idx < 0) return null;
		return termList[idx];
	}
	public String getLabel(String uri)
	{
		if (uri == null) return null;
		int idx = termLookup.getOrDefault(uri, -1);
		if (idx < 0) return null;
		return termList[idx].label;
	}
	public String getDescr(String uri)
	{
		if (uri == null) return null;
		int idx = termLookup.getOrDefault(uri, -1);
		if (idx < 0) return null;
		return termList[idx].descr;
	}
	
	// print out some concise stats about the current state
	public void debugSummary()
	{
		Util.writeln("# terms: " + termList.length);
		Util.writeln("# prefixes: " + prefixes.length);
		for (String pfx : prefixes) Util.writeln("    " + pfx);
		Util.writeln("# trees: " + treeList.size());
		for (StoredTree stored : treeList)
		{
			Schema.Assignment assn = stored.tree.getAssignment();
			Util.writeln("Schema [" + stored.schemaPrefix + "], locator: " + stored.locator + ", name:" + assn.name);
			int maxDepth = 0, depths[] = new int[20];
			for (SchemaTree.Node node : stored.tree.getFlat()) if (node.depth < depths.length)
			{
				maxDepth = Math.max(maxDepth, node.depth);
				depths[node.depth]++;
			}
			String str = "";
			for (int n = 0; n <= maxDepth; n++) str += " " + depths[n];
			Util.writeln("    # nodes: " + stored.tree.getFlat().length + ", depths:" + str);
		}
	}
	
	// information about content: generally just for debugging/stats purposes
	public int numTerms() {return termList.length;}
	public StoredTerm[] getTerms() {return termList;}
	public int numPrefixes() {return prefixes.length;}
	public StoredTree[] getTrees() {return treeList.toArray(new StoredTree[treeList.size()]);}

	// ------------ private methods ------------
	
	private SchemaVocab() {}
	
	// given a list of terms, generate a list of common prefixes
	private void derivePrefixes()
	{
		Set<String> pfxset = new HashSet<>();
		
		for (StoredTerm term : termList)
		{
			int i = Math.max(term.uri.lastIndexOf('/'), term.uri.lastIndexOf('#'));
			if (i < 0) continue;
			pfxset.add(term.uri.substring(0, i));
		}
		
		prefixes = pfxset.toArray(new String[pfxset.size()]);
		Arrays.sort(prefixes);
	}
}



