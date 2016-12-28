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

package com.cdd.bao;

import java.util.*;

import com.cdd.bao.template.*;
import com.cdd.bao.util.*;
import com.cdd.bao.editor.*;

/*
	Entrypoint for all command line functionality: delegates to the appropriate corner.
*/

public class Main
{
	public static void main(String[] argv)
	{
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.OFF);	
	
		if (argv.length > 0 && (argv[0].equals("-h") || argv[0].equals("--help")))
		{
			printHelp();
			return;
		}
		
		// TODO: add option to read in multiple schema files and export them to a single "vocab.dump"
		// TODO: read in an RDF/TTL file and export only the relevant predicates in minimal TTL (to reduce file sizes)
		
		if (argv.length == 0) new MainApplication().exec(new String[0]);
		else if (argv[0].equals("edit")) 
		{
			String[] subset = Arrays.copyOfRange(argv, 1, argv.length);
			new MainApplication().exec(subset);
		}
		else if (argv[0].equals("geneont"))
		{
			try
			{
				ImportGeneOntology impgo = new ImportGeneOntology();
				impgo.load(argv[1]);
				impgo.save(argv[2]);
			}
			catch (Exception ex) {ex.printStackTrace();}
		}
		else
		{
			Util.writeln("Unknown option '" + argv[0] + "'");
			printHelp();
		}
	}
	
	public static void printHelp()
	{
		Util.writeln("BioAssay Ontology Annotator Tools");
		Util.writeln("Options:");
		Util.writeln("    edit {files...}");
		Util.writeln("    geneont {infile} {outfile}");
	}
}
