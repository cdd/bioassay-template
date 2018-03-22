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

package com.cdd.bao.validator;

import java.io.*;
import java.util.*;

/*
	Use logic in this class to detect cycles in remapping directives. 
*/

public class RemappingChecker
{
	public static void validateRemappings(Map<String, String> remappings) throws IOException
	{
		List<String> dependencies = new ArrayList<>();
		for (String uri : remappings.keySet())
		{
			dependencies.clear();
			while (remappings.containsKey(uri))
			{
				uri = remappings.get(uri);
				if (!dependencies.add(uri)) throw new IOException(buildMessage(dependencies));
			}
		}
	}

	private static String buildMessage(List<String> terms)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Cannot load vocabulary because the following remapped terms lead to a cycle: ");

		boolean appendCommand = false;
		for (String t : terms)
		{
			if (appendCommand) sb.append(", ");
			sb.append(t);
			appendCommand = true;
		}
		return sb.toString();
	}
}
