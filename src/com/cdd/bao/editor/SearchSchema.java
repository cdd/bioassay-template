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

package com.cdd.bao.editor;

import java.util.*;

import com.cdd.bao.editor.EditSchema.*;
import com.cdd.bao.template.*;

import javafx.scene.*;
import javafx.scene.control.*;

/*
	Search Schema: search groups / assignments from the currently editable schema
*/

public class SearchSchema
{
	// houses search state to facilitate stepping through results
	public static class State
	{
		String searchText;
		int index;
		List<TreeItem<Branch>> found;
	}

	// return true if search text matches any of uri, name, or description.
	private static boolean isMatch(String uri, String name, String descr, String searchText)
	{
		return (uri != null && uri.indexOf(searchText) >= 0)
				|| (name != null && name.indexOf(searchText) >= 0)
				|| (descr != null && descr.indexOf(searchText) >= 0);
	}

	// return list of nodes that match search text.
	public static List<TreeItem<Branch>> find(TreeView<Branch> treeView, String searchText)
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
				if (isMatch(sg.groupURI, sg.name, sg.descr, searchText)) found.add(curItem);
			}
			else if (curBranch.assignment != null) 
			{
				Schema.Assignment asmt = curBranch.assignment;
				if (isMatch(asmt.propURI, asmt.name, asmt.descr, searchText)) found.add(curItem);
			}
			for (TreeItem<Branch> ti : curItem.getChildren())
				stack.addFirst(ti);
		}
		return found;
	}
}
