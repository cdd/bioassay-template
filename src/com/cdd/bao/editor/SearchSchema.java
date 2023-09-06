/*
	BioAssay Ontology Annotator Tools

	Copyright 2016-2023 Collaborative Drug Discovery, Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package com.cdd.bao.editor;

import java.util.*;

import org.apache.commons.lang3.*;

import com.cdd.bao.editor.EditSchema.*;
import com.cdd.bao.template.*;

import javafx.scene.*;
import javafx.scene.control.*;

/*
	Search Schema: search groups / assignments from the currently editable schema.
*/

public class SearchSchema
{
	// houses search state to facilitate stepping through results
	public final static class State
	{
		String searchText;
		boolean useDescr;
		int index;
		List<TreeItem<Branch>> found;
	}

	// return true if search text matches any of uri, name, or description
	private static boolean isMatch(String uri, String name, String descr, String searchText)
	{
		boolean match = (uri != null && StringUtils.indexOfIgnoreCase(uri, searchText) >= 0) 
						|| (name != null && StringUtils.indexOfIgnoreCase(name, searchText) >= 0)
						|| (descr != null && StringUtils.indexOfIgnoreCase(descr, searchText) >= 0);

		Vocabulary v = Vocabulary.globalInstance();
		if (!match && v.isLoaded() && uri != null)
		{
			// try matching alternate labels
			String[] altLabels = v.getAltLabels(uri);
			if (altLabels != null)
			{
				for (int k = 0; k < altLabels.length; k++)
					if (StringUtils.indexOfIgnoreCase(altLabels[k], searchText) >= 0) return true;
			}
		}
		return match;
	}

	// return list of nodes that match search text
	public static List<TreeItem<Branch>> find(TreeView<Branch> treeView, String searchText, boolean useDescr)
	{
		List<TreeItem<Branch>> found = new ArrayList<>();
		Deque<TreeItem<Branch>> stack = new ArrayDeque<>();

		// maybe we should skip over the root since it is not shown
		stack.addFirst(treeView.getRoot());
		while (stack.size() > 0)
		{
			TreeItem<Branch> curItem = stack.removeFirst();
			Branch curBranch = curItem.getValue();
			if (curBranch.group != null)
			{
				Schema.Group sg = curBranch.group;
				String descr = useDescr ? sg.descr : null;
				if (isMatch(sg.groupURI, sg.name, descr, searchText)) found.add(curItem);
			}
			else if (curBranch.assignment != null) 
			{
				Schema.Assignment asmt = curBranch.assignment;
				String descr = useDescr ? asmt.descr : null;
				if (isMatch(asmt.propURI, asmt.name, descr, searchText)) found.add(curItem);
			}
			for (TreeItem<Branch> ti : curItem.getChildren()) stack.addFirst(ti);
		}
		return found;
	}
}
