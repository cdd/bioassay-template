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

package com.cdd.bao.util;


import java.util.*;

import javafx.geometry.*;
import javafx.scene.layout.*;

/*
	RowLine: functions as a combination Hbox/FlowPane, providing the ability to stretch to fit, and specify vertical
	alignment.
*/

public class RowLine extends Pane
{
	// vertical alignment options
	public static final int TOP = 1;
	public static final int BOTTOM = 2;
	public static final int CENTRE = 3;
	public static final int BASELINE = 4;

	private class Unit
	{
		private Region widget;
		private float stretchX;
		private int valign;
	}
	private List<Unit> content = new ArrayList<>();

	private int padding, margin;

	// ------------ public methods ------------

	// dir: must be VERTICAL or HORIZONTAL
	// padding: number of pixels between each component, and around the edges
	public RowLine()
	{
		this(0, 0);
	}
	public RowLine(int padding)
	{
		this(padding, 0);
	}
	public RowLine(int padding, int margin)
	{
		this.padding = padding;
		this.margin = margin;
	}
	
	// convenient instantiators: very common to have just 2 widgets
	public static RowLine pair(int padding, Region widget1, float stretch1, Region widget2, float stretch2)
	{
		return pair(padding, 0, widget1, stretch1, BASELINE, widget2, stretch2, BASELINE);
	}
	public static RowLine pair(int padding, int margin, Region widget1, float stretch1, int valign1, Region widget2, float stretch2, int valign2)
	{
		RowLine row = new RowLine(padding, margin);
		row.add(widget1, stretch1, valign1);
		row.add(widget2, stretch2, valign2);
		return row;
	}
	
	// for cleaning house
	
	public void clear()
	{
		getChildren().clear();
		content.clear();
		requestLayout();
		requestParentLayout();
	}
	public void remove(Region widget)
	{
		for (Unit u : content) if (u.widget == widget)
		{
			getChildren().remove(widget);
			content.remove(u);
			requestLayout();
			requestParentLayout();
			break;
		}
	}
	
	// workhorse widget-addition
	public Region add(Region widget) {return add(widget, 0, BASELINE);}
	public Region add(Region widget, float stretchX) {return add(widget, stretchX, BASELINE);}
	public Region add(Region widget, float stretchX, int valign)
	{
		Unit u = new Unit();
		u.widget = widget;
		u.stretchX = stretchX;
		u.valign = valign;

		getChildren().add(u.widget);

		content.add(u);
		requestLayout();
		requestParentLayout();

		return widget;
	}
	
	// ------------ layout methods ------------
	
	protected double computeMinWidth(double height)
	{
		double mw = 2 * margin + padding * Math.max(0, content.size() - 1);
		for (Unit u : content) mw += u.widget.minWidth(height);
		return mw;
	}
	protected double computeMinHeight(double width)
	{
		double mh = 0;
		for (Unit u : content) mh = Math.max(mh, u.widget.minHeight(width));
		return mh + 2 * margin;
	}
	protected double computePrefWidth(double height) 
	{
		double pw = 2 * margin + padding * Math.max(0, content.size() - 1);
		for (Unit u : content) pw += u.widget.prefWidth(height);
		return pw;
	}
	protected double computePrefHeight(double width) 
	{
		double ph = 0;
		for (Unit u : content) ph = Math.max(ph, u.widget.prefHeight(width));
		return ph + 2 * margin;
	}
	public double getBaselineOffset()
	{
		double baseline = 0;
		for (Unit u : content) baseline = Math.max(baseline, u.widget.getBaselineOffset());
		return baseline;
	}
	protected void layoutChildren()
	{
		double width = getWidth(), height = getHeight();
		double baseline = getBaselineOffset();
	
		final int sz = content.size();
		double totalStretch = 0, totalWidth = 0;
		int maxWidth = (int)(width - 2 * margin - padding * Math.max(0, sz - 1));
		double[] w = new double[sz], h = new double[sz];
		for (int n = 0; n < sz; n++)
		{
			Unit u = content.get(n);
			totalStretch += u.stretchX;
			w[n] = Math.ceil(u.widget.prefWidth(height));
			h[n] = Math.max(u.widget.minHeight(w[n]), Math.min(height, u.widget.prefHeight(w[n])));
			totalWidth += w[n];
		}
		if (totalStretch > 0 && Math.round(totalWidth) != Math.round(maxWidth))
		{
			int pixels = maxWidth - (int)totalWidth;
			double allot = pixels / totalStretch;
			for (int n = 0; n < sz; n++)
			{
				Unit u = content.get(n);
				if (u.stretchX <= 0) continue;
				int extra = Math.min(pixels, (int)(u.stretchX * allot));
				w[n] += extra;
				pixels -= extra;
			}
		}

		double x = 0;
		for (int n = 0; n < sz; n++)
		{
			Unit u = content.get(n);
			double y = 0; // TOP
			if (u.valign == BOTTOM) y = height - h[n];
			else if (u.valign == CENTRE) y = height - 0.5 * h[n];
			else if (u.valign == BASELINE) y = baseline - u.widget.getBaselineOffset();
			layoutInArea(u.widget, x, y, w[n], h[n], 0, Insets.EMPTY, HPos.LEFT, VPos.TOP);
			x += w[n] + padding;
		}
	}
	
	// ------------ private methods ------------
}
