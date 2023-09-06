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

package com.cdd.bao.util;

import com.cdd.bao.*;

import java.io.*;
import java.util.*;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

/*
	Lineup: a vertical layout support component for JavaFX, which provides the major value added functionality of including a label widget that is
	aligned so that all labels have the same level of indenting, and baseline-aligns with the top of the content.
*/

public class Lineup extends Pane
{
	// bitmask of available options for units; 0=all default
	public static final int NOINDENT = 1;

	private class Unit
	{
		private Region widget;
		private Label label;
		private int stretchX, stretchY, optMask;
	}
	private List<Unit> content = new ArrayList<>();

	private int padding, margin;

	// ------------ public methods ------------

	// dir: must be VERTICAL or HORIZONTAL
	// padding: number of pixels between each component, and around the edges
	public Lineup()
	{
		this(0, 0);
	}
	public Lineup(int padding)
	{
		this(padding, 0);
	}
	public Lineup(int padding, int margin)
	{
		this.padding = padding;
		this.margin = margin;
		//setMaxWidth(Double.MAX_VALUE);
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
			if (u.label != null) getChildren().remove(u.label);
			content.remove(u);
			requestLayout();
			requestParentLayout();
			break;
		}
	}
	
	// workhorse widget-addition
	public Region add(Region widget, String title, int stretchX, int stretchY, int optMask)
	{
		if (widget != null)
		{
			getChildren().add(widget);
			if (!widget.isVisible()) widget.setVisible(true);
		}

		Unit u = new Unit();
		u.widget = widget;
		u.label = null;
		u.stretchX = stretchX;
		u.stretchY = stretchY;
		u.optMask = optMask;

		if (title != null)
		{
			u.label = new Label(title);
			getChildren().add(u.label);
		}

		content.add(u);
		requestLayout();
		requestParentLayout();

		return widget;
	}
	
	// convenience shortcuts
	public Region add(Region widget) {return add(widget, null, 0, 0, 0);}
	public Region add(Region widget, String title) {return add(widget, title, 0, 0, 0);}
	public Region add(Region widget, String title, int stretchX, int stretchY) {return add(widget, title, stretchX, stretchY, 0);}
	
	// wraps a series of widgets into a flow pane
	public Region addGroup(Region[] widgets, String title, int stretchX, int stretchY, int optMask)
	{
		FlowPane flow = new FlowPane(Orientation.HORIZONTAL);
		for (Region r : widgets) flow.getChildren().add(r);
		//flow.setPadding(new Insets(padding));
		flow.setHgap(padding);
		return add(flow, title, stretchX, stretchY, optMask);
	}
	public Region addGroup(Region[] widgets) {return addGroup(widgets, null, 0, 0, 0);}
	public Region addGroup(Region[] widgets, String title) {return addGroup(widgets, title, 0, 0, 0);}
	public Region addGroup(Region[] widgets, String title, int stretchX, int stretchY) {return addGroup(widgets, title, stretchX, stretchY, 0);}
	
	// adds a little bit of "nothing", which takes up no space unless there's too much
	public void addPadding()
	{
		add(null, null, 1, 1);
	}
	
	// ------------ layout methods ------------
	
	public Orientation getContentBias() {return Orientation.HORIZONTAL;}
	
	protected double computeMinWidth(double height)
	{
		int tw = calculateTitleWidth(), mw = 0;
		for (Unit u : content) if (u.widget != null)
		{
			int w = (int)Math.ceil(u.widget.minWidth(10000));
			if ((u.optMask & NOINDENT) == 0) w += tw;
			mw = Math.max(mw, w);
		}
		return mw + 2 * margin;
	}
	protected double computeMinHeight(double width)
	{
		int tw = calculateTitleWidth(), mh = 0;
		for (Unit u : content)
		{
			int h = 0;
			if (u.label != null) h = Math.max(h, (int)Math.ceil(u.label.minHeight(tw)));
			if (u.widget != null) h = Math.max(h, (int)Math.ceil(u.widget.minHeight(width - tw)));
			mh += h;
		}
		return mh + 2 * margin + (content.size() - 1) * padding;
	}
	protected double computePrefWidth(double height) 
	{
		int tw = calculateTitleWidth(), pw = 0;
		for (Unit u : content) if (u.widget != null)
		{
			int w = (int)Math.ceil(u.widget.prefWidth(10000));
			if ((u.optMask & NOINDENT) == 0) w += tw;
			pw = Math.max(pw, w);
		}
		return pw + 2 * margin;
	}
	protected double computePrefHeight(double width) 
	{
		int tw = calculateTitleWidth(), ph = 0;
		for (Unit u : content)
		{
			int h = 0;
			if (u.label != null) h = Math.max(h, (int)Math.ceil(u.label.prefHeight(tw)));
			if (u.widget != null) h = Math.max(h, (int)Math.ceil(u.widget.prefHeight(width - tw)));
			ph += h;
		}
		return ph + 2 * margin + (content.size() - 1) * padding;
	}
	protected void layoutChildren()
	{
		double width = getWidth(), height = getHeight();
		
		int totalStretch = 0, stretchPoints = (int)Math.floor(height - computePrefHeight(width));
		if (stretchPoints > 0) for (Unit u : content) totalStretch += u.stretchY;
		int overallTW = calculateTitleWidth(), residual = stretchPoints;
		
		for (int n = 0, y = margin; n < content.size(); n++)
		{
			Unit u = content.get(n);
			double th = u.label == null ? 0 : u.label.prefHeight(overallTW);
			double wh = u.widget == null ? 0 : u.widget.prefHeight(width - overallTW);
			int h = (int)Math.ceil(Math.max(th, wh));
			int tw = overallTW;
			if ((u.optMask & NOINDENT) != 0) tw = (int)Math.ceil(u.label == null ? 0 : u.label.minWidth(h) + padding);
			int cw = (int)Math.floor(width - tw - 2 * margin);

			if (u.stretchY > 0 && stretchPoints > 0)
			{
				int extra = (int)Math.min(Math.ceil(stretchPoints * u.stretchY / (double)totalStretch), residual);
				h += extra;
				residual -= extra;
			}
			double baselineLabel = u.label == null ? Double.NEGATIVE_INFINITY : u.label.getBaselineOffset();
			double baselineWidget = u.widget == null ? Double.NEGATIVE_INFINITY : u.widget.getBaselineOffset();
			if (u.label != null)
			{
				int ty = y;
				if (Double.isInfinite(baselineWidget)) ty += (int)(0.5f * (h - th));
				else if (u.widget != null && th < wh) ty += (int)(baselineWidget - baselineLabel);	
				layoutInArea(u.label, margin, ty, tw, h, 0, Insets.EMPTY, HPos.LEFT, VPos.TOP);
			}
			if (u.widget != null)
			{
				int wy = y;
				if (Double.isInfinite(baselineWidget)) wy += (int)(0.5f * (h - wh));
				else if (u.label != null && wh < th) wy += (int)(baselineLabel - baselineWidget);
				double w = u.stretchX == 0 ? Math.min(cw, u.widget.prefWidth(h)) : cw;
				layoutInArea(u.widget, margin + tw, wy, w, h, 0, Insets.EMPTY, HPos.LEFT, VPos.TOP);
			}

			y += h + padding;
		}
	}
	
	// ------------ private methods ------------
	
	private int calculateTitleWidth()
	{
		int w = 0;
		for (Unit u : content)
		{
			if (u.label == null) continue;
			w = Math.max(w, (int)Math.ceil(u.label.prefWidth(10000)));
		}
		if (w > 0) w += padding;
		return w;
	}
}
