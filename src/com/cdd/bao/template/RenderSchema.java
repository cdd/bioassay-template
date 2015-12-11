/*
 * BioAssay Ontology Annotator Tools
 * 
 * (c) 2014-2015 Collaborative Drug Discovery Inc.
 */

package com.cdd.bao.template;

import com.cdd.bao.*;
import com.cdd.bao.template.*;
import com.cdd.bao.util.*;
import static com.cdd.bao.template.RenderContext.*;

import java.io.*;
import java.util.*;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.common.*;

/*
	Rendering a schema as a publication quality PDF file.
*/

public class RenderSchema
{
	private Schema schema;
	private PDDocument document = new PDDocument();
	private Vocabulary vocab;
	
	// context for rendering a page: the general idea is to make it a 2-pass system; for the first pass, the PDPage instance is null, and all operations
	// have no effects, except to update the maximum width/height; for the second pass
	private static final class Context
	{
		PDPage page = null;
		float x = 0, y = 0, w = 0, h = 0;
	}

	// ------------ public methods ------------	

	public RenderSchema(Schema schema)
	{
		this.schema = schema;

		try {vocab = Vocabulary.globalInstance();}
		catch (IOException ex) {}
	}

	// creates a new page and crams the template onto it
	public void createPageTemplate() throws IOException
	{
		RenderContext ctx = new RenderContext();
		
		renderPageTemplate(ctx);
		if (ctx.width == 0 || ctx.height == 0) return;
		
        PDPage page = new PDPage(new PDRectangle(ctx.width, ctx.height));
        document.addPage(page);
        
        ctx.stream = new PDPageContentStream(document, page);
        renderPageTemplate(ctx);
        ctx.stream.close();
	}

	// creates a page showing the property & value hierarchies, respectively	
	public void createPageProperties() throws IOException
	{
		RenderContext ctx = new RenderContext();
		
		renderPageHierarchy(ctx, vocab.getPropertyHierarchy());
		if (ctx.width == 0 || ctx.height == 0) return;
		
        PDPage page = new PDPage(new PDRectangle(ctx.width, ctx.height));
        document.addPage(page);
        
        ctx.stream = new PDPageContentStream(document, page);
		renderPageHierarchy(ctx, vocab.getPropertyHierarchy());
        ctx.stream.close();
	}
	public void createPageValues() throws IOException
	{
		RenderContext ctx = new RenderContext();
		
		renderPageHierarchy(ctx, vocab.getValueHierarchy());
		if (ctx.width == 0 || ctx.height == 0) return;
		
        PDPage page = new PDPage(new PDRectangle(ctx.width, ctx.height));
        document.addPage(page);
        
        ctx.stream = new PDPageContentStream(document, page);
		renderPageHierarchy(ctx, vocab.getValueHierarchy());
        ctx.stream.close();
	}
	
	public void write(File file) throws IOException
	{
		document.save(file);
		//document.close();
	}
	
	// ------------ private methods ------------	

	private void renderPageTemplate(RenderContext ctx)
	{
		float y = 0;
		final float pad = 5;
		
		Schema.Group root = schema.getRoot();
		float nameW = ctx.measureLine(root.name, 15)[0];
		ctx.drawLine(0, 1, nameW, 1, 0x808080, 0.5f);
		ctx.drawText(0, y, root.name, 15, 0x000000, TXTALIGN_LEFT | TXTALIGN_TOP); y += 20;
		ctx.drawText(0, y, "URI: " + schema.getSchemaPrefix(), 8, 0x808080, TXTALIGN_LEFT | TXTALIGN_TOP); y += 15;
		// !! .descr, as a paragraph
		ctx.drawLine(0, y, nameW, y, 0x808080, 0.5f);
	
		y += pad;
	
		// create a flattened assignment list
		List<Schema.Assignment> assignments = new ArrayList<>();
		List<Schema.Group> stack = new ArrayList<>();
		stack.add(root);
		while (stack.size() > 0)
		{
			Schema.Group grp = stack.remove(0);
			assignments.addAll(grp.assignments);
			stack.addAll(grp.subGroups);
		}
		
		// estimate widths & heights for each assignment, then split into blocks of columns
		float[] assnW = new float[assignments.size()], assnH = new float[assignments.size()];
		for (int n = 0; n < assignments.size(); n++)
		{
			Schema.Assignment assn = assignments.get(n);
			assnW[n] = 0;
			for (Schema.Value val : assn.values) assnW[n] = Math.max(assnW[n], ctx.measureLine(val.name, 10)[0]);
			assnW[n] += ctx.measureLine(assn.name, 12)[0] + 50 + 5 * pad;
			assnH[n] = Math.max(27, assn.values.size() * 12);
			if (assn.parent.parent != null) assnH[n] += 15;
		}
		int[][] blocks = arrangeColumns(assnW, assnH);
		
		float topY = y, leftX = 0;
		
		for (int[] blk : blocks)
		{
			float curY = topY, maxX = leftX;
			for (int b : blk)
			{
				float[] xy = renderAssignment(ctx, leftX, curY, assignments.get(b));
				maxX = Math.max(maxX, xy[0]);
				curY = xy[1] + pad;
			}
			leftX = maxX;
		}
	}

	private void renderPageHierarchy(RenderContext ctx, Vocabulary.Hierarchy hier)
	{
		// flatten out the branches, and keep them in order
		List<Vocabulary.Branch> segments = new ArrayList<>(hier.rootBranches);
		for (int n = 0; n < segments.size(); n++)
		{
			Vocabulary.Branch branch = segments.get(n);
			int pos = n + 1;
			for (Vocabulary.Branch child : branch.children) if (child.children.size() > 0) segments.add(pos++, child);
		}
		
		//for (Vocabulary.Branch branch : flat) Util.writeln(branch.label + " -> children:"+branch.children.size());
		
		float y = 0;
		final float pad = 5;
		
		// estimate widths & heights for each assignment, then split into blocks of columns
		float[] segW = new float[segments.size()], segH = new float[segments.size()];
		for (int n = 0; n < segments.size(); n++)
		{
			Vocabulary.Branch branch = segments.get(n);
			segW[n] = 0;
			for (Vocabulary.Branch child : branch.children) segW[n] = Math.max(segW[n], ctx.measureLine(child.label, 10)[0]);
			segW[n] += ctx.measureLine(branch.label, 12)[0] + 50 + 5 * pad;
			segH[n] = Math.max(15, branch.children.size() * 12);
		}
		int[][] blocks = arrangeColumns(segW, segH);
		
		float topY = y, leftX = 0;
		
		for (int[] blk : blocks)
		{
			float curY = topY, maxX = leftX;
			for (int b : blk)
			{
				float[] xy = renderBranch(ctx, leftX, curY, segments.get(b));
				maxX = Math.max(maxX, xy[0]);
				curY = xy[1] + pad;
			}
			leftX = maxX;
		}
	}	
	
	private float[] renderAssignment(RenderContext ctx, float x, float y, Schema.Assignment assn)
	{
		final float pad = 5;

		// draw the group label, if not directly descended from root
		Schema.Group group = assn.parent;
		float maxX = x;
		if (group.parent != null)
		{
    		//String label = group.name;
    		//for (Schema.Group look = group.parent; look.parent != null; look = look.parent) label = look.name + "\u25BA" + label;
    		String label = "";
    		for (Schema.Group look = group; look.parent != null; look = look.parent) label = look.name + " / " + label;
			ctx.drawText(x + pad, y, label, 12, 0x000000, TXTALIGN_LEFT | TXTALIGN_TOP); 
			y += 15;
			maxX = x + pad + ctx.measureLine(label, 12)[0];
		}
	
		float ly = y, ry = y;
	
		float nameW = ctx.measureLine(assn.name, 12)[0], arrowW = 50;
		ctx.drawText(x + pad, ly, assn.name, 12, 0x000000, TXTALIGN_LEFT | TXTALIGN_TOP); 
		drawArrow(ctx, x + 2 * pad + nameW, ly + 9, arrowW, 5);
		ly += 15;
		
		String propLabel = vocab.getLabel(assn.propURI);
		ctx.drawText(x + 2 * pad, ly, propLabel, 8, 0x404040, TXTALIGN_LEFT | TXTALIGN_TOP); 
		ly += 12;
		// !! .descr, as paragraph

		float rightX = x + 3 * pad + nameW + arrowW;

		for (Schema.Value val : assn.values)
		{
			String label = val.name;
			//label += " " + val.uri;
			ctx.drawText(rightX, ry, label, 10, 0x404040, TXTALIGN_LEFT | TXTALIGN_TOP); 
			ry += 12;
			maxX = Math.max(maxX, rightX + ctx.measureLine(label, 10)[0]);
		}

		ctx.drawLine(rightX - 0.5f * pad, y, rightX - 0.5f * pad, ry, 0xC0C0C0, 1);

		return new float[]{maxX, Math.max(ly, ry)};
	}

	private float[] renderBranch(RenderContext ctx, float x, float y, Vocabulary.Branch branch)
	{
		final float pad = 5;

		float ly = y, ry = y;
	
		float nameW = ctx.measureLine(branch.label, 12)[0], arrowW = 50;
		ctx.drawText(x + pad, ly, branch.label, 12, 0x000000, TXTALIGN_LEFT | TXTALIGN_TOP); 

		if (branch.children.size() == 0) return new float[]{x + 2 * pad + nameW, ly + 15};

		drawArrow(ctx, x + 2 * pad + nameW, ly + 9, arrowW, 5);
		ly += 15;
		
		
		float rightX = x + 3 * pad + nameW + arrowW;

		float maxX = rightX;
		for (Vocabulary.Branch child : branch.children)
		{
			ctx.drawText(rightX, ry, child.label, 10, 0x404040, TXTALIGN_LEFT | TXTALIGN_TOP); 
			ry += 12;
			maxX = Math.max(maxX, rightX + ctx.measureLine(child.label, 10)[0]);
		}

		ctx.drawLine(rightX - 0.5f * pad, y, rightX - 0.5f * pad, ry, 0xC0C0C0, 1);

		return new float[]{maxX, Math.max(ly, ry)};
	}
	
	private void drawArrow(RenderContext ctx, float x, float y, float w, float sz)
	{
		ctx.drawLine(x, y, x + w, y, 0x000000, 1);
		float[] px = new float[]{x + w, x + w - sz, x + w - sz};
		float[] py = new float[]{y, y - 0.5f * sz, y + 0.5f * sz};
		ctx.drawPoly(px, py, NOCOLOUR, 0, 0x000000, true);
	}

	/*private float renderGroup(RenderContext ctx, float y, Schema.Group group)
	{
		String label = group.name;
		for (Schema.Group look = group.parent; look.parent != null; look = look.parent) label = look.name + "\u25BA" + label;
		ctx.drawText(0, y, label, 15, 0x000000, TXTALIGN_LEFT | TXTALIGN_TOP); y += 20;
		
		for (Schema.Assignment assn : group.assignments) y = renderAssignment(ctx, y, assn) + 5;
		for (Schema.Group subGroup : group.subGroups) y = renderGroup(ctx, y, subGroup) + 5;

		return y;
	}*/
	
	/*private void renderNonsense(RenderContext ctx) throws IOException
	{
		ctx.drawRect(50, 250, 400, 200, 0xC0C0C0, 1, 0xF0F0F0);
		ctx.drawRect(5, 5, 490, 490, 0x008000, 2, RenderContext.NOCOLOUR);
		ctx.drawLine(0, 0, 500, 500, 0xFF0000, 2);
		ctx.drawLine(0, 250, 500, 250, 0x0000FF, 10);

		ctx.drawPoly(new float[]{100,150,100}, new float[]{100,150,200}, 0xFFFF00, 1, 0x800000, true);
		ctx.drawCurve(new float[]{100,150,200}, new float[]{300,250,300}, new boolean[]{false,true,false}, 0x00FFFF, 1, 0x808000, false);
		//ctx.drawCurve(new float[]{100,130,170,200}, new float[]{300,250,250,300}, new boolean[]{false,true,true,false}, 0x00FFFF, 1, 0x808000, false);
		
		float[] wad = ctx.measureFont("gfnordy", 20);
		ctx.drawLine(10, 10, 10 + wad[0], 10, 0x0000FF, 0.5f);
		ctx.drawLine(10, 10 + wad[1], 10 + wad[0], 10 + wad[1], 0x0000FF, 0.5f);
		ctx.drawLine(10, 10 + wad[1] + wad[2], 10 + wad[0], 10 + wad[1] + wad[2], 0x0000FF, 0.5f);
		ctx.drawText(10, 10, "gfnordy", 20, 0x000000, RenderContext.TXTALIGN_TOP | RenderContext.TXTALIGN_LEFT);
	}*/
	
	// given a series of objects of a certain height, returns a collection of blocks that represent an attempt to arrange them with columns
	// of an even height; not an exact science, but it's better than random
	private int[][] arrangeColumns(float[] width, float[] height)
	{
		// pick a maximum height: columns will be stacked up to this size if possible
		final int num = width.length;
		float totalArea = 0, unitH = 0;
		for (int n = 0; n < num; n++)
		{
			totalArea += width[n] * height[n];
			unitH = Math.max(unitH, height[n]);
		}
		
		float maxH = Math.max(unitH, (float)Math.sqrt(0.5 * totalArea));
	
		List<int[]> blocks = new ArrayList<>();
		for (int n = 0; n < num; n++) blocks.add(new int[]{n});
		
		while (true)
		{
			int best1 = -1, best2 = -1;
			float score = Float.NaN;
		
			for (int i = 0; i < blocks.size() - 1; i++) for (int j = i + 1; j < blocks.size(); j++)
			{
				float residual = maxH;
				for (int n : blocks.get(i)) residual -= height[n];
				for (int n : blocks.get(j)) residual -= height[n];
				if (residual < 0) continue; // not allowed to go over
				if (best1 < 0 || residual < score)
				{
					best1 = i;
					best2 = j;
					score = residual;
				}
			}
			
			if (best1 < 0) break;
			int[] blk1 = blocks.get(best1), blk2 = blocks.get(best2);
			int[] blk = Arrays.copyOf(blk1, blk1.length + blk2.length);
			for (int n = 0; n < blk2.length; n++) blk[blk1.length + n] = blk2[n];
			blocks.set(best1, blk);
			blocks.remove(best2);
		}		
	
		for (int[] blk : blocks) Arrays.sort(blk);
		
		// put the smallest one last: there's usually an odd one out, and it looks better that way
		int smallest = 0;
		float minH = Float.POSITIVE_INFINITY;
		for (int n = 0; n < blocks.size(); n++)
		{
			float h = 0;
			for (int b : blocks.get(n)) h += height[b];
			if (h < minH) {smallest = n; minH = h;}
		}
		blocks.add(blocks.remove(smallest));
	
		return blocks.toArray(new int[blocks.size()][]);
	}
}


