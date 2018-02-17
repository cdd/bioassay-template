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

package com.cdd.bao.axioms;

import java.util.*;

public class AxiomRelationships
{
	public enum AxiomClassType
	{
		DEFINED, EQUIVALENT
	}

	public static class AxiomClass
	{
		public AxiomClassType type;
		public List<String> relatedAll;
		public List<String> relatedSome;
		public List<String> relatedInverse;
		
		public AxiomClass()
		{
			relatedAll = new ArrayList<>();
			relatedSome = new ArrayList<>();
			relatedInverse = new ArrayList<>();
		}
	}

	public AxiomClass definedAxiomClass;
	public AxiomClass equivalentAxiomClass;
	
	public AxiomRelationships()
	{
		definedAxiomClass = new AxiomClass();
		equivalentAxiomClass = new AxiomClass();
	}
}
