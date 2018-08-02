/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2017-2018 Collaborative Drug Discovery Inc.
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

package com.cdd.bao.axioms;

import com.cdd.bao.template.*;
import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.*;

/*
	AxiomVocab: serialisable collection of "axioms", which are distilled out from various sources to provide useful guidelines
	about how to annotate an assay.
*/

public class AxiomVocab
{
	/*
		LIMIT = presence of a term implies the exclusive existence of other terms
		EXCLUDE = presence of a term implies that other terms are not eligible
		BLANK = presence of a term implies that another branch category should not be populated
		REQUIRED = presence of a term implies that another branch category should have something (i.e. not blank)
	*/
	
	public enum Type
	{
		LIMIT(1),
		EXCLUDE(2),
		BLANK(3),
		REQUIRED(4);
		
		private final int raw;
		Type(int raw) {this.raw = raw;}
		public int raw() {return this.raw;}
		public static Type valueOf(int rawVal)
		{
			for (Type t : values()) if (t.raw == rawVal) return t;
			return LIMIT;
		}

		/*@Override
		public String toString()
		{
			if (this.equals(Type.LIMIT)) return "LIMIT";
			else if (this.equals(Type.EXCLUDE)) return "EXCLUDE";
			else if (this.equals(Type.REQUIRED)) return "REQUIRED";
			else return "BLANK";
		}*/
	}

	public static class Term
	{
		//public String branchURI;
		public String valueURI;
		public boolean wholeBranch;
	
		public Term(String valueURI, boolean wholeBranch)
		{
			//this.branchURI = null;
			this.valueURI = valueURI;
			this.wholeBranch = wholeBranch;
		}
		
		@Override
		public String toString()
		{
			return ModelSchema.collapsePrefix(valueURI) + "/" + wholeBranch;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (obj == null || !(obj instanceof Term)) return false;
			Term other = (Term)obj;
			return Util.safeString(valueURI).equals(Util.safeString(other.valueURI)) && wholeBranch == other.wholeBranch;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(valueURI, wholeBranch);
		}
	}
	
	public static class Rule
	{
		public Type type;

		// selection of the subject domain; any blank parts are considered to be wildcards
		public Term subject;

		// the object domain of the rule: the meaning varies depending on type
		public Term[] impact;
		
		/*Currently we don't care about the predicate, because for annotations, that's built in the template.
		 * However, if we want to extract these axioms as a subgraph from the ontology, then we would need it.
		 * In that case, add the following term (and add a new constructor):
		 * public Term predicate;
		*/
		
		public Rule() {}
		public Rule(Type type, Term subject) {this(type, subject, null);}
		public Rule(Type type, Term subject, Term[] impact)
		{
			this.type = type;
			this.subject = subject;
			this.impact = impact;
		}

		@Override
		public String toString()
		{
			StringBuilder str = new StringBuilder();
			str.append(type.toString() + " type axiom; ");
			str.append("subject: [" + subject + "]");
			
			StringJoiner sj = new StringJoiner(",");
			if (impact != null) for (Term s : impact) sj.add(s.toString());
			str.append("impacts: [" + sj.toString() + "])");

			return str.toString();
		}
		
		//method for generating output for rules analysis code, currently all the rules we have extracted fall into the LIMIT category
		//TODO: add code for printing the output content into a file with file headers Peter provided.
		public String rulesFormatString()
		{
			StringBuilder str = new StringBuilder();
			str.append(type.toString() + " type axiom; ");

			for (int i = 0; i < ArrayUtils.getLength(impact);i++)
			{
				str.append("[" + subject + "]");
				str.append("=>[" + impact[i] + "]" + "\n");
			}

			return str.toString();
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof Rule)) return false;
			Rule other = (Rule)obj;
			if (type != other.type) return false;
			if (subject == null) 
			{
				if (other.subject != null) return false;
			} 
			else 
			{
				if (!subject.equals(other.subject)) return false;
			}
			int sz = ArrayUtils.getLength(impact);
			if (sz != ArrayUtils.getLength(other.impact)) return false;
			for (int n = 0; n < sz; n++) if (!impact[n].equals(other.impact[n])) return false;
			return true;
		}
		
		@Override
		public int hashCode()
		{
			if (impact == null)
				return Objects.hash(type, subject);
			else
				return Objects.hash(type, subject, Arrays.asList(impact));
		}
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
	public void serialise(File file) throws IOException
	{
		OutputStream ostr = new FileOutputStream(file);
		serialise(ostr);
		ostr.close();
	}
	public void serialise(OutputStream ostr) throws IOException
	{		
		// compose a unique list of URIs, for brevity purposes
		Set<String> termSet = new HashSet<>();
		for (Rule rule : rules)
		{
			if (rule.subject != null)
			{
				//if (rule.subject.branchURI != null) termSet.add(rule.subject.branchURI);
				if (rule.subject.valueURI != null) termSet.add(rule.subject.valueURI);
			}
			if (rule.impact != null) for (Term term : rule.impact)
			{
				//if (term.branchURI != null) termSet.add(term.branchURI);
				if (term.valueURI != null) termSet.add(term.valueURI);
			}
		}

		// make a list of unique prefixes
		List<String> pfxList = new ArrayList<>();
		Map<String, Integer> pfxMap = new LinkedHashMap<>();
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

			//data.writeUTF(rule.subject == null ? null : rule.subject.branchURI);
			data.writeUTF(rule.subject == null ? null : rule.subject.valueURI);
			data.writeBoolean(rule.subject == null ? false : rule.subject.wholeBranch);

			data.writeInt(rule.impact == null ? 0 : rule.impact.length);
			if (rule.impact != null) for (Term term : rule.impact)
			{
				//data.writeUTF(term.branchURI);
				data.writeUTF(term.valueURI);
				data.writeBoolean(term.wholeBranch);
			}
		}
	}
	
	// parse out the raw binary into a living object
	public static AxiomVocab deserialise(File file) throws IOException
	{
		InputStream istr = new FileInputStream(file);
		try {return deserialise(istr);}
		finally {istr.close();}
	}
	public static AxiomVocab deserialise(InputStream istr) throws IOException
	{
		AxiomVocab av = new AxiomVocab();

		DataInputStream data = new DataInputStream(istr);
	
		int nprefix = data.readInt();
		String[] pfxList = new String[nprefix];
		for (int n = 0; n < nprefix; n++) pfxList[n] = data.readUTF();
				
		int nterm = data.readInt();
		String[] termList = new String[nterm];
		for (int n = 0; n < nterm; n++) 
		{
			int pfx = data.readInt();
			String str = data.readUTF();
			termList[n] = pfx < 0 ? str : pfxList[pfx] + str;
		}
		
		int nrule = data.readInt();
		for (int n = 0; n < nrule; n++)
		{
			Rule r = new Rule();
			r.type = Type.valueOf(data.readInt());
			r.subject = new Term(data.readUTF(), data.readBoolean());
			
			int nimpact = data.readInt();
			r.impact = new Term[nimpact];
			for (int i = 0; i < nimpact; i++) r.impact[i] = new Term(data.readUTF(), data.readBoolean());
			
			av.addRule(r);
		}

		return av;
	}
	
	public List<Rule> findRedundantRules(String[] redundantURIs)
	{
		List<Rule> redundantRules = new ArrayList<>();
		String[]redundantURIList = redundantURIs;
		
		return redundantRules;
	}
	
	// ------------ private methods ------------
	
}



