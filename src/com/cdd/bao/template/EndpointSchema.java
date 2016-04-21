/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2016 Collaborative Drug Discovery Inc.
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
import com.cdd.bao.template.Schema.*;
import com.cdd.bao.util.*;

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

	private final String SPARQL_PREFIXES =
		"PREFIX rdfs: <" + ModelSchema.PFX_RDFS + ">\n" + 
		"PREFIX bat: <" + ModelSchema.PFX_BAT + ">";
		
	private final Map<Schema, Map<String, Schema.Assignment>> cacheURIAssn = new HashMap<>();

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
			SPARQL_PREFIXES,
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
    			String uri = qs.get("template").asResource().getURI();
    			templates.add(uri);
    		}
    		return templates.toArray(new String[templates.size()]);
		}
		catch (Exception ex) {throw new IOException(formulateError(sparql), ex);}
	}
	
	// given the URI for a template, pulls out the URI for all of the assays that reference the template
	public String[] enumerateAssays(String rootURI) throws IOException
	{
		String sparql = String.join("\n", new String[]
		{
			SPARQL_PREFIXES,
			"SELECT ?assay WHERE",
			"{",
			"  ?assay a bat:" + ModelSchema.BAT_ASSAY + " ;",
			"         bat:" + ModelSchema.USES_TEMPLATE + " <" + rootURI + "> .",
			"}"
		});

		try
		{
    		QueryExecution qex = QueryExecutionFactory.sparqlService(endpoint, sparql);
    		List<String> assays = new ArrayList<>();
    		for (ResultSet rs = qex.execSelect(); rs.hasNext();)
    		{
    			QuerySolution qs = rs.next();
    			String uri = qs.get("assay").asResource().getURI();
    			assays.add(uri);
    		}
    		return assays.toArray(new String[assays.size()]);
		}
		catch (Exception ex) {throw new IOException(formulateError(sparql), ex);}
	}

	// given the URI for the root of a template, creates a new Schema and populates it with the template contents
	public Schema obtainTemplate(String rootURI) throws IOException
	{
		Schema schema = new Schema();

		Map<String, Schema.Assignment> uriToAssn = new HashMap<>();
		cacheURIAssn.put(schema, uriToAssn);

		int pfxsz = rootURI.lastIndexOf('#');
		if (pfxsz == 0) throw new IOException("Unable to determine root prefix from " + rootURI + ": must contain a '#' symbol.");
		schema.setSchemaPrefix(rootURI.substring(0, pfxsz + 1));

		fetchGroup(schema.getRoot(), rootURI, uriToAssn);
	
		return schema;
	}
	
	// given a schema that already has the template definition, fetches and passes a specific assay URI, and appends it to the list
	public void obtainAssay(Schema schema, String assayURI) throws IOException
	{
		Schema.Assay assay = fetchAssay(schema, assayURI);
		schema.appendAssay(assay);
	}
	
	// ------------ private methods ------------	

	// for a given category node, pulls out and parses all of its assignments and subcategories
	private void fetchGroup(Schema.Group group, String groupURI, Map<String, Schema.Assignment> uriToAssn) throws IOException
	{
		// summary information
	
		String sparql = String.join("\n", new String[]
		{
			SPARQL_PREFIXES,
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
    		group.name = getLiteral(qs.get("label"));
    		group.descr = getLiteral(qs.get("descr"));
		}
		catch (IOException ex) {throw ex;}
		catch (Exception ex) {throw new IOException(formulateError(sparql), ex);}
	
		// assignments

		sparql = String.join("\n", new String[]
		{
			SPARQL_PREFIXES,
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
    			String assnURI = getURI(qs.get("assn"));
    			Schema.Assignment assn = new Schema.Assignment(group, getLiteral(qs.get("label")), getURI(qs.get("propURI")));
    			assn.descr = getLiteral(qs.get("descr"));
    			
    			fetchValues(assn, getURI(qs.get("assn").asResource()));

    			group.assignments.add(assn);
    			
    			uriToAssn.put(assnURI, assn);
    		}
		}
		catch (IOException ex) {throw ex;}
		catch (Exception ex) {throw new IOException(formulateError(sparql), ex);}

		// subgroups
	
		sparql = String.join("\n", new String[]
		{
			SPARQL_PREFIXES,
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
    			Schema.Group subgrp = new Schema.Group(group, getLiteral(qs.get("label")));
    			subgrp.descr = getLiteral(qs.get("descr"));
    			
    			fetchGroup(subgrp, getURI(qs.get("subgroup")), uriToAssn);
    			
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
			SPARQL_PREFIXES,
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

		// !! todo: add the 'isWholeBranch' optional query...

		try
		{
    		QueryExecution qex = QueryExecutionFactory.sparqlService(endpoint, sparql);

    		for (ResultSet rs = qex.execSelect(); rs.hasNext();)
    		{
    			QuerySolution qs = rs.next();
    			Schema.Value val = new Schema.Value(getURI(qs.get("mapsTo")), getLiteral(qs.get("label")));
    			val.descr = getLiteral(qs.get("descr"));
    			
    			assn.values.add(val);
    		}
		}
		//catch (IOException ex) {throw ex;}
		catch (Exception ex) {throw new IOException(formulateError(sparql), ex);}
	}

	// fetches and returns an instantiated assay corresponding to the root URI
	private Schema.Assay fetchAssay(Schema schema, String assayURI) throws IOException
	{
		Map<String, Schema.Assignment> uriToAssn = cacheURIAssn.get(schema);
	
		String sparql = String.join("\n", new String[]
		{
			SPARQL_PREFIXES,
			"SELECT ?label ?descr ?para ?origin WHERE",
			"{",
			"  <" + assayURI + "> a bat:" + ModelSchema.BAT_ASSAY + " ;",
			"      rdfs:label ?label ;",
			"      bat:" + ModelSchema.IN_ORDER + " ?order .",
			"  OPTIONAL {<" + assayURI + "> bat:" + ModelSchema.HAS_DESCRIPTION + " ?descr}",
			"  OPTIONAL {<" + assayURI + "> bat:" + ModelSchema.HAS_PARAGRAPH + " ?para}",
			"  OPTIONAL {<" + assayURI + "> bat:" + ModelSchema.HAS_ORIGIN + " ?origin}",
			"}",
			"ORDER BY ?order"
		});

		Schema.Assay assay = null;
		try
		{
    		QueryExecution qex = QueryExecutionFactory.sparqlService(endpoint, sparql);

    		for (ResultSet rs = qex.execSelect(); rs.hasNext();)
    		{
    			QuerySolution qs = rs.next();
    			assay = new Schema.Assay(getLiteral(qs.get("label")));
    			assay.descr = getLiteral(qs.get("descr"));
    			assay.para = getLiteral(qs.get("para"));
    			assay.originURI = getLiteral(qs.get("origin"));
    			break;
    		}
		}
		catch (Exception ex) {throw new IOException(formulateError(sparql), ex);}

		if (assay == null) throw new IOException("Assay URI " + assayURI + " not found.");
		
		fetchAnnotations(schema, assay, assayURI, uriToAssn);
		
		return assay;
	}
	
	private void fetchAnnotations(Schema schema, Schema.Assay assay, String assayURI, Map<String, Schema.Assignment> uriToAssn) throws IOException
	{
		String sparql = String.join("\n", new String[]
		{
			SPARQL_PREFIXES,
			"SELECT ?assn ?property ?value ?label ?descr ?literal WHERE",
			"{",
			"  <" + assayURI + "> bat:" + ModelSchema.HAS_ANNOTATION + " ?blank .",
			"  ?blank bat:" + ModelSchema.IS_ASSIGNMENT + " ?assn .",
			/*"  OPTIONAL {?blank [bat:" + ModelSchema.HAS_PROPERTY + " ?property ;",
			"                    bat:" + ModelSchema.HAS_VALUE + " ?value]}",*/
			"  OPTIONAL {?blank bat:" + ModelSchema.HAS_PROPERTY + " ?property}",
			"  OPTIONAL {?blank bat:" + ModelSchema.HAS_VALUE + " ?value}",
			"  OPTIONAL {?blank rdfs:label ?label}",
			"  OPTIONAL {?blank bat:" + ModelSchema.HAS_DESCRIPTION + " ?descr}",
			"  OPTIONAL {?blank bat:" + ModelSchema.HAS_LITERAL + " ?literal}",
			"}"
		});
		
		try
		{
    		QueryExecution qex = QueryExecutionFactory.sparqlService(endpoint, sparql);

    		for (ResultSet rs = qex.execSelect(); rs.hasNext();)
    		{
    			QuerySolution qs = rs.next();
    			String assnURI = getURI(qs.get("assn"));
    			Schema.Assignment assn = uriToAssn.get(assnURI);
    			String propURI = getURI(qs.get("property")), valueURI = getURI(qs.get("value"));
    			String label = getLiteral(qs.get("label")), descr = getLiteral(qs.get("descr")), literal = getLiteral(qs.get("literal"));
  
  				if (propURI.length() > 0 && valueURI.length() > 0)
  				{
  					Schema.Value value = new Schema.Value(valueURI, label);
  					value.descr = descr;
  					assay.annotations.add(new Schema.Annotation(assn, value));
  				}
  				else if (literal.length() > 0)
  				{
  					assay.annotations.add(new Schema.Annotation(assn, literal));
  				}
    		}
		}
		catch (Exception ex) {throw new IOException(formulateError(sparql), ex);}
	}

	// standard syntax for noting the query & endpoint when something went wrong
	private String formulateError(String sparql)
	{
		return "Error with endpoint [" + endpoint + "], running SPARQL query:\n" + sparql;
	}

	// very tolerant fetching of String content: any failure converts to blank string
	private String getURI(RDFNode node)
	{
		try {if (node != null && node.isURIResource()) return node.asResource().getURI();}
		catch (Exception ex) {}
		return "";
	}
	private String getLiteral(RDFNode node)
	{
		try {if (node != null && node.isLiteral()) return node.asLiteral().getString();}
		catch (Exception ex) {}
		return "";
	}
}


