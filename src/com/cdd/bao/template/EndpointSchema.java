/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.template;

import com.cdd.bao.*;
import com.cdd.bao.template.Schema.*;

import java.io.*;
import java.util.*;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.*;
import org.apache.jena.query.*;

/*
	Endpoint Schema: provides functionality for querying a remote SPARQL endpoint, for fetching templates and assays, and serialising
	them using pieces of the Schema class.
*/

public class EndpointSchema
{
	private String endpoint;

	// ------------ private data: content ------------	

	public EndpointSchema(String endpoint)
	{
		this.endpoint = endpoint;
	}
	
	// finds all of the templates that exist within the repository: returns the root URIs for each of them
	public String[] enumerateTemplates() throws IOException
	{
		String sparql = String.join("\n", new String[]
		{
			"PREFIX rdfs: <" + ModelSchema.PFX_RDFS + ">",
			"PREFIX bat: <" + ModelSchema.PFX_BAT + ">",
			"SELECT ?template WHERE",
			"{",
			"  ?template a bat:" + ModelSchema.BAT_ROOT + " .",
			"  ?template a bat:" + ModelSchema.BAT_GROUP + " .",
			"}"
		});
		
		try
		{
    		QueryExecution qex = QueryExecutionFactory.sparqlService(endpoint, sparql);
    		List<String> templates = new ArrayList<>();
    		for (ResultSet rs = qex.execSelect(); rs.hasNext();)
    		{
    			QuerySolution qs = rs.next();
    			String uri = qs.get("template").toString();
    			templates.add(uri);
    		}
    		return templates.toArray(new String[templates.size()]);
		}
		catch (Exception ex) {throw new IOException(formulateError(sparql), ex);}
	}

	// given the URI for the root of a template, creates a new Schema and populates it with the template contents
	public Schema fetchTemplate(String rootURI) throws IOException
	{
		Schema schema = new Schema();

		int pfxsz = rootURI.lastIndexOf('#');
		if (pfxsz == 0) throw new IOException("Unable to determine root prefix from " + rootURI + ": must contain a '#' symbol.");
		schema.setSchemaPrefix(rootURI.substring(0, pfxsz + 1));

		fetchGroup(schema.getRoot(), rootURI);
	
		return schema;
	}
	
	// (and: assays...)
	
	// ------------ private methods ------------	

//		catch (Exception ex) {throw new IOException(formulateError(sparql), ex);}

	// for a given category node, pulls out and parses all of its assignments and subcategories
	private void fetchGroup(Schema.Group group, String groupURI) throws IOException
	{
		// summary information
	
		String sparql = String.join("\n", new String[]
		{
			"PREFIX rdfs: <" + ModelSchema.PFX_RDFS + ">",
			"PREFIX bat: <" + ModelSchema.PFX_BAT + ">",
			"SELECT ?label ?descr WHERE",
			"{",
			"  <" + groupURI + "> rdfs:label ?label .",
			"  OPTIONAL {<" + groupURI + "> bat:" + ModelSchema.HAS_DESCRIPTION + " ?descr}",
			"}"
		});

		try
		{
    		QueryExecution qex = QueryExecutionFactory.sparqlService(endpoint, sparql);
    		ResultSet rs = qex.execSelect();
    		if (!rs.hasNext()) throw new IOException("Missing label/description. " + formulateError(sparql));
    		QuerySolution qs = rs.next();
    		group.name = qs.get("label").toString();
    		RDFNode resDescr = qs.get("descr");
    		if (resDescr != null) group.descr = resDescr.toString();
		}
		catch (IOException ex) {throw ex;}
		catch (Exception ex) {throw new IOException(formulateError(sparql), ex);}
	
		// assignments
	
		sparql = String.join("\n", new String[]
		{
			"PREFIX rdfs: <" + ModelSchema.PFX_RDFS + ">",
			"PREFIX bat: <" + ModelSchema.PFX_BAT + ">",
			"SELECT ?assn ?label ?descr ?propURI WHERE",
			"{",
			"  <" + groupURI + "> bat:" + ModelSchema.HAS_ASSIGNMENT + " ?assn .",
			"  ?assn a bat:" + ModelSchema.BAT_ASSIGNMENT + " .",
			"  ?assn rdfs:label ?label",
			"  OPTIONAL {?assn bat:" + ModelSchema.HAS_DESCRIPTION + " ?descr}",
			"  ?assn bat:" + ModelSchema.HAS_PROPERTY + " ?propURI . ",
			"  ?assn bat:" + ModelSchema.IN_ORDER + " ?order .",
			"}",
			"ORDER BY ?order"
		});

		try
		{
    		QueryExecution qex = QueryExecutionFactory.sparqlService(endpoint, sparql);

    		for (ResultSet rs = qex.execSelect(); rs.hasNext();)
    		{
    			QuerySolution qs = rs.next();
    			Schema.Assignment assn = new Schema.Assignment(group, qs.get("label").toString(), qs.get("propURI").toString());
    			RDFNode resDescr = qs.get("descr");
    			if (resDescr != null) assn.descr = resDescr.toString();
    			
    			fetchValues(assn, qs.get("assn").toString());
    			
    			group.assignments.add(assn);
    		}
		}
		catch (IOException ex) {throw ex;}
		catch (Exception ex) {throw new IOException(formulateError(sparql), ex);}

		// subgroups
	
		sparql = String.join("\n", new String[]
		{
			"PREFIX rdfs: <" + ModelSchema.PFX_RDFS + ">",
			"PREFIX bat: <" + ModelSchema.PFX_BAT + ">",
			"SELECT ?subgroup ?label ?descr WHERE",
			"{",
			"  <" + groupURI + "> bat:" + ModelSchema.HAS_GROUP + " ?subgroup .",
			"  ?subgroup a bat:" + ModelSchema.BAT_GROUP + " .",
			"  ?subgroup rdfs:label ?label",
			"  OPTIONAL {?subgroup bat:" + ModelSchema.HAS_DESCRIPTION + " ?descr}",
			"  ?subgroup bat:" + ModelSchema.IN_ORDER + " ?order .",
			"}",
			"ORDER BY ?order"
		});

		try
		{
    		QueryExecution qex = QueryExecutionFactory.sparqlService(endpoint, sparql);

    		for (ResultSet rs = qex.execSelect(); rs.hasNext();)
    		{
    			QuerySolution qs = rs.next();
    			Schema.Group subgrp = new Schema.Group(group, qs.get("label").toString());
    			RDFNode resDescr = qs.get("descr");
    			if (resDescr != null) subgrp.descr = resDescr.toString();
    			
    			fetchGroup(subgrp, qs.get("subgroup").toString());
    			
    			group.subGroups.add(subgrp);
    		}
		}
		catch (IOException ex) {throw ex;}
		catch (Exception ex) {throw new IOException(formulateError(sparql), ex);}
	}

	// pulls out all the values corresponding to an assignment
	private void fetchValues(Schema.Assignment assn, String assnURI) throws IOException
	{
		String sparql = String.join("\n", new String[]
		{
			"PREFIX rdfs: <" + ModelSchema.PFX_RDFS + ">",
			"PREFIX bat: <" + ModelSchema.PFX_BAT + ">",
			"SELECT ?mapsTo ?label ?descr WHERE",
			"{",
			"  <" + assnURI + "> bat:" + ModelSchema.HAS_VALUE + " ?blank .",
			"  ?blank bat:" + ModelSchema.MAPS_TO + " ?mapsTo .",
			"  ?blank rdfs:label ?label",
			"  OPTIONAL {?blank bat:" + ModelSchema.HAS_DESCRIPTION + " ?descr}",
			"  ?blank bat:" + ModelSchema.IN_ORDER + " ?order .",
			"}",
			"ORDER BY ?order"
		});

		try
		{
    		QueryExecution qex = QueryExecutionFactory.sparqlService(endpoint, sparql);

    		for (ResultSet rs = qex.execSelect(); rs.hasNext();)
    		{
    			QuerySolution qs = rs.next();
    			Schema.Value val = new Schema.Value(qs.get("mapsTo").toString(), qs.get("label").toString());
    			RDFNode resDescr = qs.get("descr");
    			if (resDescr != null) val.descr = resDescr.toString();
    			
    			assn.values.add(val);
    		}
		}
		//catch (IOException ex) {throw ex;}
		catch (Exception ex) {throw new IOException(formulateError(sparql), ex);}
	}

	// standard syntax for noting the query & endpoint when something went wrong
	private String formulateError(String sparql)
	{
		return "Error with endpoint [" + endpoint + "], running SPARQL query:\n" + sparql;
	}

}


