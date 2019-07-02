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
import org.json.*;

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
		EXCLUDE(2);
		/* use case is unclear for these
		BLANK(3),
		REQUIRED(4);*/
		
		private final int raw;
		Type(int raw) {this.raw = raw;}
		public int raw() {return this.raw;}
		public static Type valueOf(int rawVal)
		{
			for (Type t : values()) if (t.raw == rawVal) return t;
			return null;
		}
	}

	public static class Term
	{
		public String valueURI = null;
		public boolean wholeBranch = false;
	
		public Term() {}
		public Term(String valueURI, boolean wholeBranch)
		{
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
	
	public static class Keyword
	{
		public String text = null; // short string that must be present, and separated by a boundary
		public String propURI = null; // identifies eligible literal-type assignments; null = look in main text
		
		public Keyword() {}
		public Keyword(String text, String propURI)
		{
			this.text = text;
			this.propURI = propURI;
		}
		
		@Override
		public String toString()
		{
			return ModelSchema.collapsePrefix(text) + "/" + propURI;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (obj == null || !(obj instanceof Keyword)) return false;
			Keyword other = (Keyword)obj;
			return Util.safeString(text).equals(Util.safeString(other.text)) && propURI == other.propURI;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(text, propURI);
		}		
	}
	
	public static class Rule
	{
		public Type type = null;

		// selection of the subject domain; either subject or keyword must be filled out
		public Term subject = null;
		public Keyword keyword = null; 
		
		// the object domain of the rule: the meaning varies depending on type
		public Term[] impact = null;
		
		public Rule() {}
		public Rule(Type type)
		{
			this.type = type;
		}
		public Rule(Type type, Term subject) {this(type, subject, null);}
		public Rule(Type type, Term subject, Term[] impact)
		{
			this.type = type;
			this.subject = subject;
			this.impact = impact;
		}
		public Rule(Type type, Keyword keyword) {this(type, keyword, null);}
		public Rule(Type type, Keyword keyword, Term[] impact)
		{
			this.type = type;
			this.keyword = keyword;
			this.impact = impact;
		}
		
		@Override
		public String toString()
		{
			StringBuilder str = new StringBuilder();
			str.append(type.toString() + " type axiom; ");
			if (subject != null) str.append("subject: [" + subject + "]");
			if (keyword != null) str.append("keyword: [" + keyword + "]");
			
			StringJoiner sj = new StringJoiner(",");
			if (impact != null) for (Term s : impact) sj.add(s.toString());
			str.append("impacts: [" + sj.toString() + "])");

			return str.toString();
		}
		
		// method for generating output for rules analysis code, currently all the rules we have extracted fall into the LIMIT category
		public String rulesFormatString()
		{
			StringBuilder str = new StringBuilder();
			str.append(type.toString() + " type axiom; ");

			for (int i = 0; i < ArrayUtils.getLength(impact);i++)
			{
				if (subject != null) str.append("[" + subject + "]");
				if (keyword != null) str.append("[" + keyword + "]");
				str.append("=>[" + impact[i] + "]" + "\n");
			}

			return str.toString();
		}
		
		// method for generating output for rules analysis code, currently all the rules we have extracted fall into the LIMIT category
		public String rulesFormatFullString()
		{
			StringBuilder str = new StringBuilder();
			str.append(type.toString() + " type axiom; ");

			for (int i = 0; i < ArrayUtils.getLength(impact);i++)
			{
				if (subject != null) str.append("[" + subject + "]");
				if (keyword != null) str.append("[" + keyword + "]");
				str.append("[" + impact[i] + "]" + "\n");
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
			if (keyword == null)
			{
				if (other.keyword != null) return false;
			}
			else
			{
				if (!keyword.equals(other.keyword)) return false;
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
	public void deleteRule(int idx) {rules.remove(idx);}
	
	// write the content as JSON (somewhat human readable, with optional vocab-to-label translation)
	public void serialise(File file) throws IOException {serialise(file, null);}
	public void serialise(File file, SchemaVocab schvoc) throws IOException
	{
		try (Writer wtr = new FileWriter(file)) {serialise(wtr, schvoc);}
	}
	public void serialise(Writer wtr, SchemaVocab schvoc) throws IOException
	{
		JSONArray jsonRules = new JSONArray();
		for (Rule rule : rules) jsonRules.put(formatJSONRule(rule, schvoc));
		jsonRules.write(wtr, 2);
	}
	
	// parse out the raw binary into a living object
	public static AxiomVocab deserialise(File file) throws IOException
	{
		try (Reader rdr = new FileReader(file)) {return deserialise(rdr);}
	}
	public static AxiomVocab deserialise(Reader rdr) throws IOException
	{
		JSONArray json = new JSONArray(new JSONTokener(rdr));
	
		AxiomVocab av = new AxiomVocab();
		for (int n = 0; n < json.length(); n++)
		{
			JSONObject jsonRule = json.optJSONObject(n);
			if (jsonRule == null) continue; // is OK to skip
			try
			{
				Rule rule = parseJSONRule(jsonRule);
				av.addRule(rule);
			}
			catch (IOException ex) {throw new IOException("Parsing error: " + ex.getMessage() + " for rule: " + jsonRule.toString(), ex);}
		}

		/*DataInputStream data = new DataInputStream(istr);
	
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
		}*/

		return av;
	}
	
	public List<Rule> findRedundantRules(String[] redundantURIs)
	{
		List<Rule> redundantRules = new ArrayList<>();
		String[]redundantURIList = redundantURIs;
		
		return redundantRules;
	}
	
	// ------------ private methods ------------
	
	// turning rule objects into JSON
	private JSONObject formatJSONRule(Rule rule, SchemaVocab schvoc)
	{
		JSONObject json = new JSONObject();
		json.put("type", rule.type.toString().toLowerCase());
		if (rule.subject != null) json.put("subject", formatJSONTerm(rule.subject, schvoc));
		if (rule.keyword != null) json.put("keyword", formatJSONKeyword(rule.keyword, schvoc));
		
		JSONArray jsonImpact = new JSONArray();
		if (rule.impact != null) for (Term term : rule.impact) jsonImpact.put(formatJSONTerm(term, schvoc));
		json.put("impact", jsonImpact);
		
		return json;
	}
	private JSONObject formatJSONTerm(Term term, SchemaVocab schvoc)
	{
		JSONObject json = new JSONObject();
		if (schvoc != null) json.put("label", schvoc.getLabel(term.valueURI));
		json.put("valueURI", term.valueURI);
		json.put("wholeBranch", term.wholeBranch);
		return json;
	}
	private JSONObject formatJSONKeyword(Keyword keyword, SchemaVocab schvoc)
	{
		JSONObject json = new JSONObject();
		json.put("text", keyword.text);
		if (keyword.propURI != null)
		{
			json.put("propURI", keyword.propURI);
			if (schvoc != null) json.put("propLabel", schvoc.getLabel(keyword.propURI));
		}
		return json;
	}

	// unpacking JSON-formatted objects into rules (or hard fail)
	private static Rule parseJSONRule(JSONObject json) throws IOException
	{
		Rule rule = new Rule();

		String strType = json.getString("type");
		try {rule.type = Type.valueOf(strType.toUpperCase());}
		catch (Exception ex) {throw new IOException("Invalid rule type: " + strType);}
		
		JSONObject jsonSubject = json.optJSONObject("subject"), jsonKeyword = json.optJSONObject("keyword");
		if (jsonSubject != null) rule.subject = parseJSONTerm(jsonSubject);
		else if (jsonKeyword != null) rule.keyword = parseJSONKeyword(jsonKeyword);
		else throw new IOException("Rule must provide subject or keyword.");
		
		JSONArray jsonImpact = json.optJSONArray("impact");
		List<Term> impact = new ArrayList<>();
		for (int n = 0; n < jsonImpact.length(); n++)
		{
			JSONObject jsonRule = jsonImpact.optJSONObject(n);
			if (jsonRule != null) impact.add(parseJSONTerm(jsonRule));
		}
		rule.impact = impact.toArray(new Term[impact.size()]);
		
		return rule;
	}
	private static Term parseJSONTerm(JSONObject json) throws IOException
	{
		Term term = new Term();
		term.valueURI = ModelSchema.expandPrefix(json.getString("valueURI"));
		term.wholeBranch = json.optBoolean("wholeBranch", false);
		return term;
	}
	private static Keyword parseJSONKeyword(JSONObject json) throws IOException
	{
		Keyword keyword = new Keyword();
		keyword.text = json.getString("text");
		if (json.has("propURI")) keyword.propURI = ModelSchema.expandPrefix(json.getString("propURI"));
		return keyword;
	}
}



