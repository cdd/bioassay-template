/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2016-2018 Collaborative Drug Discovery Inc.
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

import com.cdd.bao.util.*;
import static com.cdd.bao.template.Schema.*;
import static com.cdd.bao.template.Vocabulary.*;
import org.apache.commons.lang3.*;

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
	private static final int MAGIC_NUMBER = 0xDEADBEEF; // has to start with this number, else is not correct
	private static final int CURRENT_VERSION = 3; // serialisation version, required to match
	
	private String[] prefixes;

	public final static class StoredTerm
	{
		public String uri;
		public String label, descr;
		public String[] altLabels;
		public String[] externalURLs;
		public String pubchemSource;
		public boolean pubchemImport;
	}
	private StoredTerm[] termList;
	private Map<String, Integer> termLookup = new HashMap<>(); // uri-to-index into termList

	public final static class StoredTree
	{
		public String schemaPrefix; // unique identifier for the template
		public String propURI; // together with groupNest, uniquely identifies
		public String[] groupNest; // an assignment part of a schema dumped to a file 
		public Assignment assignment;
		public SchemaTree tree;
	}
	private List<StoredTree> treeList = new ArrayList<>();
	
	public final static class StoredRemapTo
	{
		public String fromURI; // original term
		public String toURI; // new term
	}
	private Map<String, StoredRemapTo> remappings = new HashMap<>(); // uri-to-remapping

	private static final String SEP = "::";

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
					stored.propURI = assn.propURI;
					stored.groupNest = assn.groupNest();
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
			termList[n].altLabels = ArrayUtils.removeElement(vocab.getAltLabels(termURI[n]), termList[n].label);
			termList[n].externalURLs = vocab.getExternalURLs(termURI[n]);
			termList[n].pubchemSource = vocab.getPubChemSource(termURI[n]);	
			termList[n].pubchemImport = vocab.getPubChemImport(termURI[n]);

			termLookup.put(termURI[n], n);
		}
		derivePrefixes();

		// save remappings
		for (Map.Entry<String, String> rTo : vocab.getRemappings().entrySet())
		{
			StoredRemapTo curRemapTo = new StoredRemapTo();
			curRemapTo.fromURI = rTo.getKey();
			curRemapTo.toURI = rTo.getValue();
			remappings.put(curRemapTo.fromURI, curRemapTo);
		}

		/*Util.writeln("** unique terms: " + allTerms.size());
		Util.writeln("** prefixes: " + prefixes.length);
		for (String pfx : prefixes) Util.writeln("    " + pfx);*/
	}
	
	// returns an instance that has only the trees for the indicated schema prefix
	public SchemaVocab singleTemplate(String schemaPrefix)
	{
		SchemaVocab dup = new SchemaVocab();
		dup.prefixes = new String[]{schemaPrefix};
		dup.termList = Arrays.copyOf(termList, termList.length);
		for (int n = 0; n < termList.length; n++) dup.termLookup.put(termList[n].uri, n);
		for (StoredTree tree : treeList) if (tree.schemaPrefix.equals(schemaPrefix)) dup.treeList.add(tree);
		return dup;
	}
	
	// write the content as raw binary
	public void serialise(OutputStream ostr) throws IOException
	{
		DataOutputStream data = new DataOutputStream(ostr);
		
		data.writeInt(MAGIC_NUMBER);
		data.writeInt(CURRENT_VERSION);
		
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
			
			data.writeInt(Util.length(term.altLabels));
			if (term.altLabels != null) for (String label : term.altLabels) data.writeUTF(label);
			data.writeInt(Util.length(term.externalURLs));
			if (term.externalURLs != null) for (String url : term.externalURLs) data.writeUTF(url);
			
			data.writeUTF(term.pubchemSource == null ? "" : term.pubchemSource);
			data.writeBoolean(term.pubchemImport);
		}
		
		data.writeInt(treeList.size());
		for (int n = 0; n < treeList.size(); n++)
		{
			StoredTree stored = treeList.get(n);
			data.writeUTF(stored.schemaPrefix);
			data.writeUTF(stored.assignment.propURI);

			String[] groupNest = stored.assignment.groupNest();
			data.writeInt(groupNest.length);
			for (int i = 0; i < groupNest.length; i++) data.writeUTF(groupNest[i]);

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

		data.writeInt(remappings.size());
		for (StoredRemapTo rTo : remappings.values())
		{
			data.writeUTF(rTo.fromURI);
			data.writeUTF(rTo.toURI);
		}

		data.flush();
	}
	
	// read the raw binary content; a list of all available templates needs to be provided; note that the loaded trees
	// are all kept in the SchemaTree's static cache; note also that it is important to make sure that the templates
	// are persistent, because the individual assignment objects are used for direct lookups
	public static SchemaVocab deserialise(InputStream istr, Schema[] templates) throws IOException
	{
		DataInputStream data = new DataInputStream(istr);
	
		int magic = data.readInt();
		if (magic != MAGIC_NUMBER) throw new IOException("Not a vocabulary file.");
		
		int version = data.readInt();
		if (version != CURRENT_VERSION) throw new IOException("Vocabulary file is the wrong version.");
	
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
			
			int numAltLabels = data.readInt();
			if (numAltLabels > 0)
			{
				term.altLabels = new String[numAltLabels];
				for (int i = 0; i < numAltLabels; i++) term.altLabels[i] = data.readUTF();
			}
			int numURLs = data.readInt();
			if (numURLs > 0)
			{
				term.externalURLs = new String[numURLs];
				for (int i = 0; i < numURLs; i++) term.externalURLs[i] = data.readUTF();
			}
			term.pubchemSource = data.readUTF();
			term.pubchemImport = data.readBoolean();
			
			sv.termList[n] = term;
			sv.termLookup.put(term.uri, n);
		}
		
		int ntree = data.readInt();
		for (int n = 0; n < ntree; n++)
		{
			StoredTree stored = new StoredTree();
			stored.schemaPrefix = data.readUTF();
			stored.propURI = data.readUTF();

			int lenGroupNest = data.readInt();
			stored.groupNest = new String[lenGroupNest];
			for (int i = 0; i < stored.groupNest.length; i++) stored.groupNest[i] = data.readUTF();
			
			for (Schema schema : templates) if (stored.schemaPrefix.equals(schema.getSchemaPrefix()))
			{
				// arbitrarily choose the first assignment in case several matches are found 
				Assignment[] asmtFound = schema.findAssignmentByProperty(stored.propURI, stored.groupNest);
				stored.assignment = asmtFound.length > 0 ? asmtFound[0] : null;
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

				node.altLabels = sv.termList[termidx].altLabels;
				node.externalURLs = sv.termList[termidx].externalURLs;
				node.pubchemSource = sv.termList[termidx].pubchemSource;
				node.pubchemImport = sv.termList[termidx].pubchemImport;
				
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

		int nremappings = data.readInt();
		for (int n = 0; n < nremappings; ++n)
		{
			StoredRemapTo curRemapTo = new StoredRemapTo();
			curRemapTo.fromURI = data.readUTF();
			curRemapTo.toURI = data.readUTF();
			sv.remappings.put(curRemapTo.fromURI, curRemapTo);
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
	public String[] getAltLabels(String uri)
	{
		if (uri == null) return null;
		int idx = termLookup.getOrDefault(uri, -1);
		if (idx < 0) return null;
		return termList[idx].altLabels;
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
			Util.writeln("Schema [" + stored.schemaPrefix + "], propURI: " + stored.propURI + ", name:" + (assn == null ? "?" : assn.name));
			int maxDepth = 0;
			int[] depths = new int[20];
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

	// get remappings
	public Map<String, StoredRemapTo> getRemappings() {return Collections.unmodifiableMap(remappings);}
	
	// information about content: generally just for debugging/stats purposes
	public int numTerms() {return termList.length;}
	public StoredTerm[] getTerms() {return termList;}
	public int numPrefixes() {return prefixes.length;}
	public StoredTree[] getTrees() {return treeList.toArray(new StoredTree[treeList.size()]);}

	// update internal data structures to reflect addition of named terms and any related remappings
	public void addTerms(List<StoredTerm> newTerms, Map<String, StoredRemapTo> newTermRemappings)
	{
		StoredTerm[] newTermList = (StoredTerm[]) ArrayUtils.addAll(termList, newTerms.toArray(new StoredTerm[0]));
		for (int k = termList.length; k < newTermList.length; k++)
		{
			termLookup.put(newTermList[k].uri, new Integer(k));
			
			StoredRemapTo srt = newTermRemappings.get(newTermList[k].uri);
			if (srt != null) remappings.put(newTermList[k].uri, srt);
		}
		termList = newTermList;
	}
	
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
