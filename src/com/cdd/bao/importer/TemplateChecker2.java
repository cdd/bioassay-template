/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2017 Collaborative Drug Discovery Inc.
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

package com.cdd.bao.importer;

import com.cdd.bao.template.*;
import static com.cdd.bao.template.Schema.*;
import com.cdd.bao.util.*;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

import org.json.*;

/*
	Loads up a schema and runs through the template content, looking for possible flaws that could cause trouble
	further down the line.
*/

public class TemplateChecker2
{
	private Schema schema;
	private Vocabulary vocab;
	private Consumer<List<Diagnostic>> consumer;

	public static final class Diagnostic
	{
		List<Group> groupNest; // nested group location of issue in schema tree
		String propURI; // URI if issue triggered within assignment; null otherwise
		String issue; // diagnostic message describing issue

		public Diagnostic(List<Group> groupNest, String issue)
		{
			this(groupNest, null, issue);
		}
		public Diagnostic(List<Group> groupNest, String propURI, String issue)
		{
			this.groupNest = Collections.unmodifiableList(groupNest);
			this.propURI = propURI;
			this.issue = issue;
		}
		public List<Group> getGroupNest()
		{
			return groupNest;
		}
		public String getPropURI()
		{
			return propURI;
		}
		public String getIssue()
		{
			return issue;
		}

		// output should look like:
		//     groupNest :: propURI :: issue
		// where groupNest is a concatenation of all parents to the node where the diagnostic issue occurs
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			for (Group g : groupNest) sb.append(g != null ? g.name : "").append("::");
			sb.append(propURI).append("::");
			sb.append(issue);
			return sb.toString();
		}
	}

	private List<Diagnostic> diagnostics = new ArrayList<>();

	// ------------ public methods ------------

	public TemplateChecker2(String fn, Consumer<List<Diagnostic>> consumer) throws IOException, JSONException
	{
		this(new File(fn), consumer);
	}
	public TemplateChecker2(File schemaFile, Consumer<List<Diagnostic>> consumer) throws IOException, JSONException
	{
		try (InputStream is = new FileInputStream(schemaFile))
		{
			this.schema = SchemaUtil.deserialise(is).schema;
			this.consumer = consumer;
		}
	}
	public TemplateChecker2(Schema schema, Consumer<List<Diagnostic>> consumer)
	{
		this.schema = schema;
		this.consumer = consumer;
	}

	public void perform() throws IOException, JSONException
	{
		vocab = new Vocabulary();
		vocab.addListener(new Vocabulary.Listener()
		{
			public void vocabLoadingProgress(Vocabulary vocab, float progress) {Util.writeFlush(".");}
			public void vocabLoadingException(Exception ex) {ex.printStackTrace();}
		});
		vocab.load(null, null);

		List<Group> groupNest = new ArrayList<>();
		checkGroup(groupNest, schema.getRoot());

		// here we propagate diagnostics to logic owned by the consumer
		consumer.accept(Collections.unmodifiableList(this.diagnostics));
	}

	// ------------ private methods ------------

	private void checkGroup(List<Group> groupNest, Group group)
	{
		groupNest.add(group);

		if (Util.isBlank(group.name)) diagnostics.add(new Diagnostic(groupNest, "group name should not be blank"));

		if (group.parent == null) {} // don't care
		else if (Util.isBlank(group.groupURI)) diagnostics.add(new Diagnostic(groupNest, "group has no URI"));
		else
		{
			if (!vocab.hasPropertyURI(group.groupURI))
				diagnostics.add(new Diagnostic(groupNest, "group URI <" + group.groupURI + "> not a known property"));
		}

		if (Util.isBlank(group.descr)) diagnostics.add(new Diagnostic(groupNest, "group has no description"));

		Set<String> usedName = new HashSet<>(), usedURI = new HashSet<>();
		
		// go through all of the assignments and check them out
		for (int n = 0; n < group.assignments.size(); n++)
		{
			Assignment assn = group.assignments.get(n);
			
			if (Util.isBlank(assn.name))
				diagnostics.add(new Diagnostic(groupNest, assn.propURI, "assignment name should not be blank"));
			else if (usedName.contains(assn.name))
				diagnostics.add(new Diagnostic(groupNest, assn.propURI, "name has been used previously in this group"));
			else usedName.add(assn.name);

			if (Util.isBlank(assn.propURI))
				diagnostics.add(new Diagnostic(groupNest, assn.propURI, "assignment has no URI"));
			else if (usedURI.contains(assn.propURI))
				diagnostics.add(new Diagnostic(groupNest, assn.propURI, "URI <" + assn.propURI + "> has been used previously in this group"));
			else 
			{
				if (!vocab.hasPropertyURI(assn.propURI))
					diagnostics.add(new Diagnostic(groupNest, assn.propURI, "assignment property URI <" + assn.propURI + "> not a known property"));
				usedURI.add(assn.propURI);
			}

			if (Util.isBlank(assn.descr))
				diagnostics.add(new Diagnostic(groupNest, assn.propURI, "assignment has no description"));

			Set<String> valueURI = new HashSet<>();
			for (int i = 0; i < assn.values.size(); i++)
			{
				Value val = assn.values.get(i);

				if (Util.isBlank(val.name))
					diagnostics.add(new Diagnostic(groupNest, assn.propURI, "value#" + (i + 1) + " has no name (URI=<" + val.uri + ">)"));

				if (Util.isBlank(val.uri))
					diagnostics.add(new Diagnostic(groupNest, assn.propURI, "value#" + (i + 1) + " has no URI (name=[" + val.name + "])"));
				else if (valueURI.contains(val.uri))
					diagnostics.add(new Diagnostic(groupNest, assn.propURI, "value# " + (i + 1) + " URI <" + val.uri + "> is duplicated"));
				else
				{
					if (!vocab.hasValueURI(val.uri))
						diagnostics.add(new Diagnostic(groupNest, assn.propURI, "value# " + (i + 1) + " URI <" + val.uri + "> not a known value"));
					valueURI.add(val.uri);
				}
			}
		}
		
		// run through subgroups and check for duplicate URIs, prior to recursively analyzing them individually
		for (int n = 0; n < group.subGroups.size(); n++)
		{
			Group subgrp = group.subGroups.get(n);
			if (Util.isBlank(subgrp.groupURI)) {} // will complain later
			if (usedURI.contains(subgrp.groupURI))
				diagnostics.add(new Diagnostic(groupNest, "subgroup #" + (n + 1) + " [" + subgrp.name + "] has duplicate URI: <" + subgrp.groupURI + ">"));
			else usedURI.add(subgrp.groupURI);
		}

		// copy groupNest before passing as argument to avoid conflict between invocations of checkGroup
		for (Group subgrp : group.subGroups) checkGroup(new ArrayList<>(groupNest), subgrp);
	}
}
