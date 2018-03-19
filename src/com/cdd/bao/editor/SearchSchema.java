/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2018 Collaborative Drug Discovery Inc.
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
				for (int k = 0; k < altLabels.length; ++k)
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
