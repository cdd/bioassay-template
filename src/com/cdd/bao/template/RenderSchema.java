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
	
	public void write(File file) throws IOException
	{
		document.save(file);
		//document.close();
	}
	
	// ------------ private methods ------------	

	private void renderPageTemplate(RenderContext ctx)
	{
		float y = 0;
		
		Schema.Group root = schema.getRoot();
		float nameW = ctx.measureLine(root.name, 15)[0];
		ctx.drawLine(0, 1, nameW, 1, 0x808080, 0.5f);
		ctx.drawText(0, y, root.name, 15, 0x000000, TXTALIGN_LEFT | TXTALIGN_TOP); y += 20;
		ctx.drawText(0, y, "URI: " + schema.getSchemaPrefix(), 8, 0x808080, TXTALIGN_LEFT | TXTALIGN_TOP); y += 15;
		// !! .descr, as a paragraph
		ctx.drawLine(0, y, nameW, y, 0x808080, 0.5f);
	
		y += 5;
	
		for (Schema.Assignment assn : root.assignments) y = renderAssignment(ctx, y, assn) + 5;
		for (Schema.Group subGroup : root.subGroups) y = renderGroup(ctx, y, subGroup) + 5;
	}
	
	private float renderAssignment(RenderContext ctx, float y, Schema.Assignment assn)
	{
		float ly = y, ry = y;
		final float pad = 5;
	
		float nameW = ctx.measureLine(assn.name, 12)[0], arrowW = 50;
		ctx.drawText(pad, ly, assn.name, 12, 0x000000, TXTALIGN_LEFT | TXTALIGN_TOP); 
		drawArrow(ctx, 2 * pad + nameW, ly + 9, arrowW, 5);
		ly += 15;
		
		String propLabel = vocab.getLabel(assn.propURI);
		ctx.drawText(2 * pad, ly, propLabel, 8, 0x404040, TXTALIGN_LEFT | TXTALIGN_TOP); ly += 12;
		// !! .descr, as paragraph

		float rightX = 3 * pad + nameW + arrowW;

		for (Schema.Value val : assn.values)
		{
			String label = val.name;
			//label += " " + val.uri;
			ctx.drawText(rightX, ry, label, 10, 0x404040, TXTALIGN_LEFT | TXTALIGN_TOP); 
			ry += 12;
		}

		return Math.max(ly, ry);
	}
	
	private void drawArrow(RenderContext ctx, float x, float y, float w, float sz)
	{
		ctx.drawLine(x, y, x + w, y, 0x000000, 1);
		float[] px = new float[]{x + w, x + w - sz, x + w - sz};
		float[] py = new float[]{y, y - 0.5f * sz, y + 0.5f * sz};
		ctx.drawPoly(px, py, NOCOLOUR, 0, 0x000000, true);
	}

	private float renderGroup(RenderContext ctx, float y, Schema.Group group)
	{
		String label = group.name;
		for (Schema.Group look = group.parent; look.parent != null; look = look.parent) label = look.name + "\u25BA" + label;
		ctx.drawText(0, y, label, 15, 0x000000, TXTALIGN_LEFT | TXTALIGN_TOP); y += 20;
		
		for (Schema.Assignment assn : group.assignments) y = renderAssignment(ctx, y, assn) + 5;
		for (Schema.Group subGroup : group.subGroups) y = renderGroup(ctx, y, subGroup) + 5;

		return y;
	}
	
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
}


