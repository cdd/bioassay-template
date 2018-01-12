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

package com.cdd.bao.util;

import com.cdd.bao.template.*;

import java.io.*;

/*
	Key on file suffixes to employ appropriate (de)serialiser.  If ".ttl" suffix, then
	(de)serialise (from)to TURTLE-formatted files defining RDF triples; if ".json" suffix,
	then (de)serialise (from)to JSON-formatted files.
*/

public class UtilIO
{
	public static Schema deserialise(File file) throws IOException
	{
		Schema schema = null;

		if (file.getName().endsWith(".ttl")) schema = ModelSchema.deserialise(file);
		else if (file.getName().endsWith(".json")) schema = Schema.deserialise(file);
		else throw new IOException("Can only deserialise from .ttl or .json format.");
		
		return schema;
	}

	public static void serialise(Schema schema, File file) throws IOException
	{
		try (OutputStream ostr = new FileOutputStream(file))
		{
			if (file.getName().endsWith(".ttl")) ModelSchema.serialise(schema, ostr);
			else if (file.getName().endsWith(".json")) schema.serialise(ostr);
			else throw new IOException("Should serialise to .ttl or .json format only.");
		}
	}
}
