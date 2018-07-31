package com.cdd.bao.template;

import com.cdd.bao.axioms.AxiomCollector.*;
import com.cdd.bao.template.AxiomVocab.Rule;
import com.cdd.bao.template.AxiomVocab.Term;
import com.cdd.bao.ScanAxioms;
import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.*;

public class AxiomVocabulary

{
	/* LIMIT = axioms with keyword "some"; 
	 * EXCLUDE = axioms with keyword "not";  
	 * REQUIRED = axioms with keyword "only"
	 * */
	public enum Type
	{
		LIMIT(1),
		EXCLUDE(2),
		BLANK(3),
		REQUIRED(4);
		
		private final int raw;
		Type(int raw){this.raw = raw;}
		public int raw(){return this.raw;}
		public static Type valueOf(int rawVal)
		{
			for (Type t : values()) if (t.raw == rawVal) return t;
			return LIMIT;
		}
	}//end of Type
	
	public static class Term
	{
		public String valueURI;
		public boolean wholeBranch;
		
		public Term(String valueURI, boolean wholeBranch)
		{
			this.valueURI = valueURI;
			this.wholeBranch = wholeBranch;
		}
		
		//for predicate terms
		public Term(String valueURI)
		{
			this.valueURI = valueURI;
		}
		
		@Override 
		public boolean equals(Object obj)
		{
			if(obj == null || !(obj instanceof Term))  return false;
			Term other = (Term) obj;
			return Util.safeString(valueURI).equals(Util.safeString(other.valueURI)) && wholeBranch == other.wholeBranch;
		}
		
	}//end of Term
	
	public static class Rule 
	{
		//LIMIT, EXCLUDE, or REQUIRED, currently only LIMIT axioms are processed 
		public Type type;
		
		// selection of the subject domain; any blank parts are considered to be wildcards
		public Term subject;
		
		//the relationship (predicate) for the axiom, for better rating the rule's impact for DTO
		public Term predicate;
		
		// the object domain of the rule: the meaning varies depending on type
		public Term[] impact;
		
		public Rule(){}
		public Rule(Type type, Term subject){this(type,subject,null);}
		public Rule(Type type, Term subject, Term[] impact)
		{
			this.type = type;
			this.subject = subject;
			this.impact = impact;
		}
		public Rule(Type type, Term subject, Term predicate, Term[] impact)
		{
			this.type = type;
			this.subject = subject;
			this.predicate = predicate;
			this.impact = impact;
		}
		
		@Override
		public String toString()
		{
			StringBuilder str = new StringBuilder();
			if(type.equals(Type.LIMIT))  str.append("LIMIT type axiom");
			else if (type.equals(Type.EXCLUDE)) str.append("EXCLUDE type axiom");
			else if (type.equals(Type.REQUIRED)) str.append("REQUIRED type axiom");
			else if (type.equals(Type.BLANK)) str.append("BLANK type axiom");
			
			str.append("subject [ "+ subject +"]");
			str.append("impacts: [");
			for(int n =0; n<ArrayUtils.getLength(impact); n++) str.append((n == 0 ? "" : ",") + impact[n]);
			str.append("]");
			
			return str.toString();	
			
		}
		
		public String rulesFormatString()
		{
			StringBuilder str = new StringBuilder();
			
			for(int i = 0; i < ArrayUtils.getLength(impact);i++)
			{
				str.append(subject + " ");
				//str.append(predicate+ " ");
				str.append(impact[i]);
			}
			
			return str.toString();
			
		}
		
		public String fullRulesFormatString()
		{
			StringBuilder str = new StringBuilder();
			
			str.append(subject + " ");
			str.append(predicate + " ");
			str.append(impact[1]);
				
			return str.toString();
			
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (obj == null || !(obj instanceof Rule)) return false;
			Rule other = (Rule)obj;
			if (type != other.type) return false;
			if ((subject == null && other.subject != null) || (subject != null && !subject.equals(other.subject))) return false;
			//if ((predicate == null && other.predicate != null) || (predicate != null && !predicate.equals(other.predicate))) return false;
			int sz = ArrayUtils.getLength(impact);
			if (sz != ArrayUtils.getLength(other.impact)) return false;
			for (int n = 0; n < sz; n++) if (!impact[n].equals(other.impact[n])) return false;
			return true;
		}
		
	}//end of Rule
	
	private List<Rule> rules = new ArrayList<>();
	
	public AxiomVocabulary()
	{
	}
	
	//access to content
	public int numRules() {return rules.size();}
	public Rule getRule(int idx){return rules.get(idx);}
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
		//compose a unique list of URIs, brevity purposes
		Set<String> termSet = new HashSet<>();
		for(Rule rule : rules) 
		{
			if(rule.subject != null)
			{
				if(rule.subject.valueURI != null) termSet.add(rule.subject.valueURI);
			}
			if(rule.impact != null) for (Term term : rule.impact)
			{
				if(term.valueURI != null) termSet.add(term.valueURI);
			}
		}
		
	}

}
