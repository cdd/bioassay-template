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
	AxiomVocab: serialisable collection of "axioms", which are distilled out from various sources to provide useful guidelines
	about how to annotate an assay.
*/

public class AxiomVocab
{
	private String[] prefixes;
	
	public enum Type
	{
		LIMIT(1),
		EXCLUDE(2),
		BLANK(3),
		REQUIRED(4);
		
		private final int raw;
		Type(int raw) {this.raw = raw;}
		public int raw() {return this.raw;}
	}
	
	public class Term
	{
		public String branchURI;
		public String valueURI;
		public boolean wholeBranch;
		
		public Term(String branchURI, String valueURI, boolean wholeBranch)
		{
			this.branchURI = branchURI;
			this.valueURI = valueURI;
			this.wholeBranch = wholeBranch;
		}
		public Term(String branchURI)
		{
			this.branchURI = branchURI;
			this.valueURI = null;
			this.wholeBranch = false;
		}
		public Term(String valueURI, boolean wholeBranch)
		{
			this.branchURI = null;
			this.valueURI = valueURI;
			this.wholeBranch = wholeBranch;
		}
	}
	
	public class Rule
	{
		public Type type;

		// selection of the subject domain; any blank parts are considered to be wildcards
		public Term subject;
		
		// the object domain of the rule: the meaning varies depending on type
		public Term[] impact;
	}
	private List<Rule> rules = new ArrayList<>();

	// ------------ public methods ------------

	public AxiomVocab()
	{
	}

	// access to content	
	public int numRules() {return rules.size();}
	public Rule getRule(int idx) {return rules.get(idx);}
	public Rule[] getRules() {return rules.toArray(new Rule[rules.size()]);}
	public void addRule(Rule rule) {rules.add(rule);}
	public void setRule(int idx, Rule rule) {rules.set(idx, rule);}
	
	// write the content as raw binary
	public void serialise(OutputStream ostr) throws IOException
	{		
		// compose a unique list of URIs, for brevity purposes
		Set<String> termSet = new HashSet<>();
		for (Rule rule : rules)
		{
			if (rule.subject != null)
			{
				if (rule.subject.branchURI != null) termSet.add(rule.subject.branchURI);
				if (rule.subject.valueURI != null) termSet.add(rule.subject.valueURI);
			}
			if (rule.impact != null) for (Term term : rule.impact)
			{
				if (term.branchURI != null) termSet.add(term.branchURI);
				if (term.valueURI != null) termSet.add(term.valueURI);
			}
		}

		// make a list of unique prefixes
		List<String> pfxList = new ArrayList<>();
		Map<String, Integer> pfxMap = new HashMap<>();
		for (String uri : termSet)
		{
			int i = Math.max(uri.lastIndexOf('/'), uri.lastIndexOf('#'));
			if (i < 0) continue;
			String pfx = uri.substring(0, i);
			if (pfxMap.containsKey(pfx)) continue;
			pfxMap.put(pfx, pfxList.size());
			pfxList.add(pfx);
		}

		String[] termList = termSet.toArray(new String[termSet.size()]);
		Map<String, Integer> termMap = new HashMap<>();
		for (int n = 0; n < termList.length; n++) termMap.put(termList[n], n);

		// start the writing: header summaries first
		DataOutputStream data = new DataOutputStream(ostr);

		data.writeInt(pfxList.size());
		for (String pfx : pfxList) data.writeUTF(pfx);
		
		data.writeInt(termList.length);
		for (String uri : termList)
		{
			int i = Math.max(uri.lastIndexOf('/'), uri.lastIndexOf('#'));
			if (i < 0)
			{
				data.writeInt(-1);
				data.writeUTF(uri);
			}
			else
			{
				data.writeInt(pfxMap.get(uri.substring(0, i)));
				data.writeUTF(uri.substring(i));
			}
		}
		
		// write out each rule
		data.writeInt(rules.size());
		for (Rule rule : rules)
		{
			data.writeInt(rule.type.raw);

			data.writeUTF(rule.subject == null ? null : rule.subject.branchURI);
			data.writeUTF(rule.subject == null ? null : rule.subject.valueURI);
			data.writeBoolean(rule.subject == null ? false : rule.subject.wholeBranch);

			data.writeInt(rule.impact == null ? 0 : rule.impact.length);
			if (rule.impact != null) for (Term term : rule.impact)
			{
				data.writeUTF(term.branchURI);
				data.writeUTF(term.valueURI);
				data.writeBoolean(term.wholeBranch);
			}
		}
	}
	
	public static AxiomVocab deserialise(InputStream istr, Schema[] templates) throws IOException
	{
		AxiomVocab av = new AxiomVocab();

		DataInputStream data = new DataInputStream(istr);
	
		/* TODO: adapt this code (originally from SchemaVocab), to mirror the serialisation
		
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
		}*/
		
		return av;
	}
	
	// ------------ private methods ------------
	
}



