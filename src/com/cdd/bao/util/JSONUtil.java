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

package com.cdd.bao.util;

import java.io.*;
import java.util.*;
import org.json.*;

/*
	Static utilities to make working with the JSON classes a bit more convenient.
*/

public class JSONUtil
{
    /**
     * Assuming that a JSONArray contains a sequence of integers, returns a primitive Java array with the values.
     */
    public static int[] intArray(JSONArray arr) throws JSONException
    {
		int[] ret = new int[arr.length()];
		for (int n = 0; n < ret.length; n++) ret[n] = arr.getInt(n);
		return ret;
    }

    /**
     * Assuming that a JSONArray contains a sequence of floating point numbers, returns a primitive Java array with the values.
     */
    public static float[] floatArray(JSONArray arr) throws JSONException
    {
		float[] ret = new float[arr.length()];
		for (int n = 0; n < ret.length; n++) ret[n] = (float)arr.getDouble(n);
		return ret;
    }

    /**
     * Assuming that a JSONArray contains a sequence of floating point numbers, returns a primitive Java array with the values.
     */
    public static double[] doubleArray(JSONArray arr) throws JSONException
    {
		double[] ret = new double[arr.length()];
		for (int n = 0; n < ret.length; n++) ret[n] = arr.getDouble(n);
		return ret;
    }
    
    /**
     * Assuming that a JSONArray contains a sequence of strings, returns a primitive Java array with the values.
     */
    public static String[] stringArray(JSONArray arr) throws JSONException
    {
		String[] ret = new String[arr.length()];
		for (int n = 0; n < ret.length; n++) ret[n] = arr.getString(n);
		return ret;
	}
    
	/**
	 * Assuming that a JSONArray contains entirely JSONObject instances, returns them in a normal Java array.
	 */
    public static JSONObject[] objectArray(JSONArray arr) throws JSONException
    {
		JSONObject[] ret = new JSONObject[arr.length()];
		for (int n = 0; n < ret.length; n++) ret[n] = arr.getJSONObject(n);
		return ret;
    }

	/**
	 * Assuming that a JSONArray contains entirely JSONArray instances, returns them in a normal Java array.
	 */
    public static JSONArray[] arrayOfArrays(JSONArray arr) throws JSONException
    {
		JSONArray[] ret = new JSONArray[arr.length()];
		for (int n = 0; n < ret.length; n++) ret[n] = arr.getJSONArray(n);
		return ret;
    }
}
