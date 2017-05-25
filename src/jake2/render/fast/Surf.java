/*
 * Surf.java
 * Copyright (C) 2003
 *
 * $Id: Surf.java,v 1.3 2006-11-21 02:22:19 cawe Exp $
 */
/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
package jake2.render.fast;

import jake2.Defines;
import jake2.client.*;
import jake2.common.TDynamicLight;
import jake2.game.cplane_t;
import jake2.qcommon.Com;
import jake2.render.*;
import jake2.util.Lib;
import jake2.util.Math3D;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.GL11;

import java.nio.*;
import java.util.Arrays;

import static jake2.render.Base.BACKFACE_EPSILON;
import static jake2.render.Base.TEXNUM_LIGHTMAPS;

/**
 * Surf
 *  
 * @author cwei
 */
public class Surf {

	// GL_RSURF.C: surface-related refresh code
	float[] modelorg = {0, 0, 0};		// relative to viewpoint

	TMapSurface r_alpha_surfaces;

	static final int DYNAMIC_LIGHT_WIDTH = 128;
	static final int DYNAMIC_LIGHT_HEIGHT = 128;

	static final int LIGHTMAP_BYTES = 4;

	static final int BLOCK_WIDTH = 128;
	static final int BLOCK_HEIGHT = 128;

	static final int MAX_LIGHTMAPS = 128;

	int c_visible_lightmaps;
	int c_visible_textures;

	static final int GL_LIGHTMAP_FORMAT = GL11.GL_RGBA;

	static class gllightmapstate_t 
	{
		int internal_format;
		int current_lightmap_texture;

		TMapSurface[] lightmap_surfaces = new TMapSurface[MAX_LIGHTMAPS];
		int[] allocated = new int[BLOCK_WIDTH];

		// the lightmap texture data needs to be kept in
		// main memory so texsubimage can update properly
		//byte[] lightmap_buffer = new byte[4 * BLOCK_WIDTH * BLOCK_HEIGHT];
		IntBuffer lightmap_buffer = Lib.newIntBuffer(BLOCK_WIDTH * BLOCK_HEIGHT, ByteOrder.LITTLE_ENDIAN);
				
		public gllightmapstate_t() {
			for (int i = 0; i < MAX_LIGHTMAPS; i++)
				lightmap_surfaces[i] = new TMapSurface();
		}
		
		public void clearLightmapSurfaces() {
			for (int i = 0; i < MAX_LIGHTMAPS; i++)
				// TODO lightmap_surfaces[i].clear();
				lightmap_surfaces[i] = new TMapSurface();
		}
		
	} 

	gllightmapstate_t gl_lms = new gllightmapstate_t();

	/*
	=============================================================

		BRUSH MODELS

	=============================================================
	*/

	/**
	 * R_TextureAnimation
	 * Returns the proper texture for a given time and base texture
	 */
	TImage R_TextureAnimation(TMapTexInfo tex)
	{
		if (tex.next == null)
			return tex.image;

		int c = RenderAPIImpl.main.currententity.frame % tex.numframes;
		while (c != 0)
		{
			tex = tex.next;
			c--;
		}

		return tex.image;
	}

	/**
	 * DrawGLPoly
	 */
	void DrawGLPoly(TGlPoly p)
	{
		GL11.glDrawArrays(GL11.GL_POLYGON, p.pos, p.numverts);
	}

	/**
	 * DrawGLFlowingPoly
	 * version that handles scrolling texture
	 */
	void DrawGLFlowingPoly(TGlPoly p)
	{
		float scroll = -64 * ( (RenderAPIImpl.main.r_newrefdef.time / 40.0f) - (int)(RenderAPIImpl.main.r_newrefdef.time / 40.0f) );
		if(scroll == 0.0f)
			scroll = -64.0f;
		p.beginScrolling(scroll);
		GL11.glDrawArrays(GL11.GL_POLYGON, p.pos, p.numverts);
		p.endScrolling();
	}

	/**
	 * R_DrawTriangleOutlines
	*/
	void R_DrawTriangleOutlines()
	{
        if (RenderAPIImpl.main.gl_showtris.value == 0)
            return;

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1, 1, 1, 1);

        TMapSurface surf;
        TGlPoly p;
        int j;	
        for (int i = 0; i < MAX_LIGHTMAPS; i++) {
             for (surf = gl_lms.lightmap_surfaces[i]; surf != null; surf = surf.lightmapchain) {
                for (p = surf.polys; p != null; p = p.chain) {
                    for (j = 2; j < p.numverts; j++) {
                        GL11.glBegin(GL11.GL_LINE_STRIP);
						GL11.glVertex3f(p.x(0), p.y(0), p.z(0));
						GL11.glVertex3f(p.x(j-1), p.y(j-1), p.z(j-1));
						GL11.glVertex3f(p.x(j), p.y(j), p.z(j));
						GL11.glVertex3f(p.x(0), p.y(0), p.z(0));
                        GL11.glEnd();
                    }
                }
            }
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	private final IntBuffer temp2 = Lib.newIntBuffer(34 * 34, ByteOrder.LITTLE_ENDIAN);

	/**
	 * R_RenderBrushPoly
	 */
	void R_RenderBrushPoly(TMapSurface fa)
	{
		RenderAPIImpl.main.c_brush_polys++;

		TImage image = R_TextureAnimation(fa.texinfo);

		if ((fa.flags & Defines.SURF_DRAWTURB) != 0)
		{	
			RenderAPIImpl.image.bindTexture( image.texnum );

			// warp texture, no lightmaps
			RenderAPIImpl.image.GL_TexEnv(GL11.GL_MODULATE);
			GL11.glColor4f( RenderAPIImpl.main.gl_state.inverse_intensity,
					RenderAPIImpl.main.gl_state.inverse_intensity,
					RenderAPIImpl.main.gl_state.inverse_intensity,
						1.0F );
			RenderAPIImpl.warp.EmitWaterPolys (fa);
			RenderAPIImpl.image.GL_TexEnv(GL11.GL_REPLACE);

			return;
		}
		else
		{
			RenderAPIImpl.image.bindTexture( image.texnum );
			RenderAPIImpl.image.GL_TexEnv(GL11.GL_REPLACE);
		}

		//	  ======
		//	  PGM
		if((fa.texinfo.flags & Defines.SURF_FLOWING) != 0)
			DrawGLFlowingPoly(fa.polys);
		else
			DrawGLPoly (fa.polys);
		//	  PGM
		//	  ======

		// ersetzt goto
		boolean gotoDynamic = false;
		/*
		** check for lightmap modification
		*/
		int maps;
		for ( maps = 0; maps < Defines.MAXLIGHTMAPS && fa.styles[maps] != (byte)255; maps++ )
		{
			if ( RenderAPIImpl.main.r_newrefdef.lightstyles[fa.styles[maps] & 0xFF].white != fa.cached_light[maps] ) {
				gotoDynamic = true;
				break;
			}
		}
		
		// this is a hack from cwei
		if (maps == 4) maps--;

		// dynamic this frame or dynamic previously
		boolean is_dynamic = false;
		if ( gotoDynamic || ( fa.dlightframe == RenderAPIImpl.main.r_framecount ) )
		{
			//	label dynamic:
			if ( RenderAPIImpl.main.gl_dynamic.value != 0 )
			{
				if (( fa.texinfo.flags & (Defines.SURF_SKY | Defines.SURF_TRANS33 | Defines.SURF_TRANS66 | Defines.SURF_WARP ) ) == 0)
				{
					is_dynamic = true;
				}
			}
		}

		if ( is_dynamic )
		{
			if ( ( (fa.styles[maps] & 0xFF) >= 32 || fa.styles[maps] == 0 ) && ( fa.dlightframe != RenderAPIImpl.main.r_framecount ) )
			{
				// ist ersetzt durch temp2:	unsigned	temp[34*34];
				int smax, tmax;

				smax = (fa.extents[0]>>4)+1;
				tmax = (fa.extents[1]>>4)+1;

				RenderAPIImpl.light.R_BuildLightMap( fa, temp2, smax);
				RenderAPIImpl.light.R_SetCacheState( fa );

				RenderAPIImpl.image.bindTexture( RenderAPIImpl.main.gl_state.lightmap_textures + fa.lightmaptexturenum );

				GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0,
								  fa.light_s, fa.light_t, 
								  smax, tmax, 
								  GL_LIGHTMAP_FORMAT,
						GL11.GL_UNSIGNED_BYTE, temp2 );

				fa.lightmapchain = gl_lms.lightmap_surfaces[fa.lightmaptexturenum];
				gl_lms.lightmap_surfaces[fa.lightmaptexturenum] = fa;
			}
			else
			{
				fa.lightmapchain = gl_lms.lightmap_surfaces[0];
				gl_lms.lightmap_surfaces[0] = fa;
			}
		}
		else
		{
			fa.lightmapchain = gl_lms.lightmap_surfaces[fa.lightmaptexturenum];
			gl_lms.lightmap_surfaces[fa.lightmaptexturenum] = fa;
		}
	}


	/**
	 * R_DrawAlphaSurfaces
	 * Draw water surfaces and windows.
	 * The BSP tree is waled front to back, so unwinding the chain
	 * of alpha_surfaces will draw back to front, giving proper ordering.
	 */
	void R_DrawAlphaSurfaces()
	{
		RenderAPIImpl.main.r_world_matrix.clear();
		//
		// go back to the world matrix
		//
		GL11.glLoadMatrixf(RenderAPIImpl.main.r_world_matrix);

		GL11.glEnable (GL11.GL_BLEND);
		RenderAPIImpl.image.GL_TexEnv(GL11.GL_MODULATE);
		

		// the textures are prescaled up for a better lighting range,
		// so scale it back down
		float intens = RenderAPIImpl.main.gl_state.inverse_intensity;

		GL11.glInterleavedArrays(GL11.GL_T2F_V3F, TGlPoly.BYTE_STRIDE, globalPolygonInterleavedBuf);

		for (TMapSurface s = r_alpha_surfaces; s != null ; s=s.texturechain)
		{
			RenderAPIImpl.image.bindTexture(s.texinfo.image.texnum);
			RenderAPIImpl.main.c_brush_polys++;
			if ((s.texinfo.flags & Defines.SURF_TRANS33) != 0)
				GL11.glColor4f (intens, intens, intens, 0.33f);
			else if ((s.texinfo.flags & Defines.SURF_TRANS66) != 0)
				GL11.glColor4f (intens, intens, intens, 0.66f);
			else
				GL11.glColor4f (intens,intens,intens,1);
			if ((s.flags & Defines.SURF_DRAWTURB) != 0)
				RenderAPIImpl.warp.EmitWaterPolys(s);
			else if((s.texinfo.flags & Defines.SURF_FLOWING) != 0)			// PGM	9/16/98
				DrawGLFlowingPoly(s.polys);							// PGM
			else
				DrawGLPoly(s.polys);
		}

		RenderAPIImpl.image.GL_TexEnv(GL11.GL_REPLACE);
		GL11.glColor4f (1,1,1,1);
		GL11.glDisable (GL11.GL_BLEND);

		r_alpha_surfaces = null;
	}

	/**
	 * DrawTextureChains
	 */
	void DrawTextureChains()
	{
		c_visible_textures = 0;

		TMapSurface s;
		TImage image;
		int i;
		for (i = 0; i < RenderAPIImpl.image.numgltextures ; i++)
		{
			image = RenderAPIImpl.image.gltextures[i];

			if (image.registration_sequence == 0)
				continue;
			if (image.texturechain == null)
				continue;
			c_visible_textures++;

			for ( s = image.texturechain; s != null ; s=s.texturechain)
			{
				if ( ( s.flags & Defines.SURF_DRAWTURB) == 0 )
					R_RenderBrushPoly(s);
			}
		}

		RenderAPIImpl.image.GL_EnableMultitexture( false );
		for (i = 0; i < RenderAPIImpl.image.numgltextures ; i++)
		{
			image = RenderAPIImpl.image.gltextures[i];

			if (image.registration_sequence == 0)
				continue;
			s = image.texturechain;
			if (s == null)
				continue;

			for ( ; s != null ; s=s.texturechain)
			{
				if ( (s.flags & Defines.SURF_DRAWTURB) != 0 )
					R_RenderBrushPoly(s);
			}

			image.texturechain = null;
		}

		RenderAPIImpl.image.GL_TexEnv(GL11.GL_REPLACE);
	}

	// direct buffer
	private final IntBuffer temp = Lib.newIntBuffer(128 * 128, ByteOrder.LITTLE_ENDIAN);
	
	/**
	 * GL_RenderLightmappedPoly
	 * @param surf
	 */
	void GL_RenderLightmappedPoly( TMapSurface surf )
	{

		// ersetzt goto
		boolean gotoDynamic = false;
		int map;
		for ( map = 0; map < Defines.MAXLIGHTMAPS && (surf.styles[map] != (byte)255); map++ )
		{
			if ( RenderAPIImpl.main.r_newrefdef.lightstyles[surf.styles[map] & 0xFF].white != surf.cached_light[map] ) {
				gotoDynamic = true;
				break;
			}
		}

		// this is a hack from cwei
		if (map == 4) map--;

		// dynamic this frame or dynamic previously
		boolean is_dynamic = false;
		if ( gotoDynamic || ( surf.dlightframe == RenderAPIImpl.main.r_framecount ) )
		{
			//	label dynamic:
			if ( RenderAPIImpl.main.gl_dynamic.value != 0 )
			{
				if ( (surf.texinfo.flags & (Defines.SURF_SKY | Defines.SURF_TRANS33 | Defines.SURF_TRANS66 | Defines.SURF_WARP )) == 0 )
				{
					is_dynamic = true;
				}
			}
		}

		TGlPoly p;
		TImage image = R_TextureAnimation( surf.texinfo );
		int lmtex = surf.lightmaptexturenum;

		if ( is_dynamic )
		{
			// ist raus gezogen worden int[] temp = new int[128*128];
			int smax, tmax;

			if ( ( (surf.styles[map] & 0xFF) >= 32 || surf.styles[map] == 0 ) && ( surf.dlightframe != RenderAPIImpl.main.r_framecount ) )
			{
				smax = (surf.extents[0]>>4)+1;
				tmax = (surf.extents[1]>>4)+1;

				RenderAPIImpl.light.R_BuildLightMap( surf, temp, smax);
				RenderAPIImpl.light.R_SetCacheState( surf );

				RenderAPIImpl.image.GL_MBind(RenderAPIImpl.main.TEXTURE1, RenderAPIImpl.main.gl_state.lightmap_textures + surf.lightmaptexturenum );

				lmtex = surf.lightmaptexturenum;

				GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0,
								  surf.light_s, surf.light_t, 
								  smax, tmax, 
								  GL_LIGHTMAP_FORMAT,
						GL11.GL_UNSIGNED_BYTE, temp );

			}
			else
			{
				smax = (surf.extents[0]>>4)+1;
				tmax = (surf.extents[1]>>4)+1;

				RenderAPIImpl.light.R_BuildLightMap( surf, temp, smax);

				RenderAPIImpl.image.GL_MBind(RenderAPIImpl.main.TEXTURE1, RenderAPIImpl.main.gl_state.lightmap_textures + 0 );

				lmtex = 0;

				GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0,
								  surf.light_s, surf.light_t, 
								  smax, tmax, 
								  GL_LIGHTMAP_FORMAT,
						GL11.GL_UNSIGNED_BYTE, temp );

			}

			RenderAPIImpl.main.c_brush_polys++;

			RenderAPIImpl.image.GL_MBind(RenderAPIImpl.main.TEXTURE0, image.texnum );
			RenderAPIImpl.image.GL_MBind(RenderAPIImpl.main.TEXTURE1, RenderAPIImpl.main.gl_state.lightmap_textures + lmtex );

			// ==========
			//	  PGM
			if ((surf.texinfo.flags & Defines.SURF_FLOWING) != 0)
			{
				float scroll;
		
				scroll = -64 * ( (RenderAPIImpl.main.r_newrefdef.time / 40.0f) - (int)(RenderAPIImpl.main.r_newrefdef.time / 40.0f) );
				if(scroll == 0.0f)
					scroll = -64.0f;

				for ( p = surf.polys; p != null; p = p.chain )
				{
				    p.beginScrolling(scroll);
					GL11.glDrawArrays(GL11.GL_POLYGON, p.pos, p.numverts);
				    p.endScrolling();
				}
			}
			else
			{
				for ( p = surf.polys; p != null; p = p.chain )
				{
					GL11.glDrawArrays(GL11.GL_POLYGON, p.pos, p.numverts);
				}
			}
			// PGM
			// ==========
		}
		else
		{
			RenderAPIImpl.main.c_brush_polys++;

			RenderAPIImpl.image.GL_MBind(RenderAPIImpl.main.TEXTURE0, image.texnum );
			RenderAPIImpl.image.GL_MBind(RenderAPIImpl.main.TEXTURE1, RenderAPIImpl.main.gl_state.lightmap_textures + lmtex);
			
			// ==========
			//	  PGM
			if ((surf.texinfo.flags & Defines.SURF_FLOWING) != 0)
			{
				float scroll;
		
				scroll = -64 * ( (RenderAPIImpl.main.r_newrefdef.time / 40.0f) - (int)(RenderAPIImpl.main.r_newrefdef.time / 40.0f) );
				if(scroll == 0.0)
					scroll = -64.0f;

				for ( p = surf.polys; p != null; p = p.chain )
				{
				    p.beginScrolling(scroll);
					GL11.glDrawArrays(GL11.GL_POLYGON, p.pos, p.numverts);
				    p.endScrolling();
				}
			}
			else
			{
			// PGM
			//  ==========
				for ( p = surf.polys; p != null; p = p.chain )
				{
					GL11.glDrawArrays(GL11.GL_POLYGON, p.pos, p.numverts);
				}
				
			// ==========
			// PGM
			}
			// PGM
			// ==========
		}
	}

	/**
	 * R_DrawInlineBModel
	 */
	void R_DrawInlineBModel()
	{
		// calculate dynamic lighting for bmodel
		if ( RenderAPIImpl.main.gl_flashblend.value == 0 )
		{
			TDynamicLight lt;
			for (int k=0 ; k<RenderAPIImpl.main.r_newrefdef.num_dlights ; k++)
			{
				lt = RenderAPIImpl.main.r_newrefdef.dlights[k];
				RenderAPIImpl.light.R_MarkLights(lt, 1<<k, RenderAPIImpl.main.currentmodel.nodes[RenderAPIImpl.main.currentmodel.firstnode]);
			}
		}

		// psurf = &currentmodel->surfaces[currentmodel->firstmodelsurface];
		int psurfp = RenderAPIImpl.main.currentmodel.firstmodelsurface;
		TMapSurface[] surfaces = RenderAPIImpl.main.currentmodel.surfaces;
		//psurf = surfaces[psurfp];

		if ( (RenderAPIImpl.main.currententity.flags & Defines.RF_TRANSLUCENT) != 0 )
		{
			GL11.glEnable (GL11.GL_BLEND);
			GL11.glColor4f (1,1,1,0.25f);
			RenderAPIImpl.image.GL_TexEnv(GL11.GL_MODULATE);
		}

		//
		// draw texture
		//
		TMapSurface psurf;
		cplane_t pplane;
		float dot;
		for (int i=0 ; i<RenderAPIImpl.main.currentmodel.nummodelsurfaces ; i++)
		{
			psurf = surfaces[psurfp++];
			// find which side of the node we are on
			pplane = psurf.plane;

			dot = Math3D.DotProduct(modelorg, pplane.normal) - pplane.dist;

			// draw the polygon
			if (((psurf.flags & Defines.SURF_PLANEBACK) != 0 && (dot < -BACKFACE_EPSILON)) ||
				((psurf.flags & Defines.SURF_PLANEBACK) == 0 && (dot > BACKFACE_EPSILON)))
			{
				if ((psurf.texinfo.flags & (Defines.SURF_TRANS33 | Defines.SURF_TRANS66)) != 0 )
				{	// add to the translucent chain
					psurf.texturechain = r_alpha_surfaces;
					r_alpha_surfaces = psurf;
				}
				else if ( (psurf.flags & Defines.SURF_DRAWTURB) == 0 )
				{
					GL_RenderLightmappedPoly( psurf );
				}
				else
				{
					RenderAPIImpl.image.GL_EnableMultitexture( false );
					R_RenderBrushPoly( psurf );
					RenderAPIImpl.image.GL_EnableMultitexture( true );
				}
			}
		}
		
		if ( (RenderAPIImpl.main.currententity.flags & Defines.RF_TRANSLUCENT) != 0 ) {
			GL11.glDisable (GL11.GL_BLEND);
			GL11.glColor4f (1,1,1,1);
			RenderAPIImpl.image.GL_TexEnv(GL11.GL_REPLACE);
		}
	}

	// stack variable
	private final float[] mins = {0, 0, 0};
	private final float[] maxs = {0, 0, 0};
	private final float[] org = {0, 0, 0};
	private final float[] forward = {0, 0, 0};
	private final float[] right = {0, 0, 0};
	private final float[] up = {0, 0, 0};
	/**
	 * R_DrawBrushModel
	 */
	void R_DrawBrushModel(TEntity e)
	{
		if (RenderAPIImpl.main.currentmodel.nummodelsurfaces == 0)
			return;

		RenderAPIImpl.main.currententity = e;
		RenderAPIImpl.main.gl_state.currenttextures[0] = RenderAPIImpl.main.gl_state.currenttextures[1] = -1;

		boolean rotated;
		if (e.angles[0] != 0 || e.angles[1] != 0 || e.angles[2] != 0)
		{
			rotated = true;
			for (int i=0 ; i<3 ; i++)
			{
				mins[i] = e.origin[i] - RenderAPIImpl.main.currentmodel.radius;
				maxs[i] = e.origin[i] + RenderAPIImpl.main.currentmodel.radius;
			}
		}
		else
		{
			rotated = false;
			Math3D.VectorAdd(e.origin, RenderAPIImpl.main.currentmodel.mins, mins);
			Math3D.VectorAdd(e.origin, RenderAPIImpl.main.currentmodel.maxs, maxs);
		}

		if (RenderAPIImpl.main.R_CullBox(mins, maxs)) return;

		GL11.glColor3f (1,1,1);
		
		// memset (gl_lms.lightmap_surfaces, 0, sizeof(gl_lms.lightmap_surfaces));
		
		// TODO wird beim multitexturing nicht gebraucht
		//gl_lms.clearLightmapSurfaces();
		
		Math3D.VectorSubtract (RenderAPIImpl.main.r_newrefdef.vieworg, e.origin, modelorg);
		if (rotated)
		{
			Math3D.VectorCopy (modelorg, org);
			Math3D.AngleVectors (e.angles, forward, right, up);
			modelorg[0] = Math3D.DotProduct (org, forward);
			modelorg[1] = -Math3D.DotProduct (org, right);
			modelorg[2] = Math3D.DotProduct (org, up);
		}

		GL11.glPushMatrix();
		
		e.angles[0] = -e.angles[0];	// stupid quake bug
		e.angles[2] = -e.angles[2];	// stupid quake bug
		RenderAPIImpl.main.R_RotateForEntity(e);
		e.angles[0] = -e.angles[0];	// stupid quake bug
		e.angles[2] = -e.angles[2];	// stupid quake bug

		RenderAPIImpl.image.GL_EnableMultitexture( true );
		RenderAPIImpl.image.GL_SelectTexture(RenderAPIImpl.main.TEXTURE0);
		RenderAPIImpl.image.GL_TexEnv(GL11.GL_REPLACE);
		GL11.glInterleavedArrays(GL11.GL_T2F_V3F, TGlPoly.BYTE_STRIDE, globalPolygonInterleavedBuf);
		RenderAPIImpl.image.GL_SelectTexture(RenderAPIImpl.main.TEXTURE1);
		RenderAPIImpl.image.GL_TexEnv(GL11.GL_MODULATE);
		GL11.glTexCoordPointer(2, GL11.GL_FLOAT, TGlPoly.BYTE_STRIDE, globalPolygonTexCoord1Buf);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

		R_DrawInlineBModel();

		ARBMultitexture.glClientActiveTextureARB(RenderAPIImpl.main.TEXTURE1);
		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

		RenderAPIImpl.image.GL_EnableMultitexture( false );

		GL11.glPopMatrix();
	}

	/*
	=============================================================

		WORLD MODEL

	=============================================================
	*/

	/**
	 * R_RecursiveWorldNode
	 */
	void R_RecursiveWorldNode (TMNode node)
	{
		if (node.contents == Defines.CONTENTS_SOLID)
			return;		// solid
		
		if (node.visframe != RenderAPIImpl.main.r_visframecount)
			return;
			
		if (RenderAPIImpl.main.R_CullBox(node.mins, node.maxs))
			return;
	
		int c;
		TMapSurface mark;
		// if a leaf node, draw stuff
		if (node.contents != -1)
		{
			TMLeaf pleaf = (TMLeaf)node;

			// check for door connected areas
			if (RenderAPIImpl.main.r_newrefdef.areabits != null)
			{
				if ( ((RenderAPIImpl.main.r_newrefdef.areabits[pleaf.area >> 3] & 0xFF) & (1 << (pleaf.area & 7)) ) == 0 )
					return;		// not visible
			}

			int markp = 0;

			mark = pleaf.getMarkSurface(markp); // first marked surface
			c = pleaf.nummarksurfaces;

			if (c != 0)
			{
				do
				{
					mark.visframe = RenderAPIImpl.main.r_framecount;
					mark = pleaf.getMarkSurface(++markp); // next surface
				} while (--c != 0);
			}

			return;
		}

		// node is just a decision point, so go down the apropriate sides

		// find which side of the node we are on
		cplane_t plane = node.plane;
		float dot;
		switch (plane.type)
		{
		case Defines.PLANE_X:
			dot = modelorg[0] - plane.dist;
			break;
		case Defines.PLANE_Y:
			dot = modelorg[1] - plane.dist;
			break;
		case Defines.PLANE_Z:
			dot = modelorg[2] - plane.dist;
			break;
		default:
			dot = Math3D.DotProduct(modelorg, plane.normal) - plane.dist;
			break;
		}

		int side, sidebit;
		if (dot >= 0.0f)
		{
			side = 0;
			sidebit = 0;
		}
		else
		{
			side = 1;
			sidebit = Defines.SURF_PLANEBACK;
		}

		// recurse down the children, front side first
		R_RecursiveWorldNode(node.children[side]);

		// draw stuff
		TMapSurface surf;
		TImage image;
		//for ( c = node.numsurfaces, surf = r_worldmodel.surfaces[node.firstsurface]; c != 0 ; c--, surf++)
		for ( c = 0; c < node.numsurfaces; c++)
		{
			surf = RenderAPIImpl.main.r_worldmodel.surfaces[node.firstsurface + c];
			if (surf.visframe != RenderAPIImpl.main.r_framecount)
				continue;

			if ( (surf.flags & Defines.SURF_PLANEBACK) != sidebit )
				continue;		// wrong side

			if ((surf.texinfo.flags & Defines.SURF_SKY) != 0)
			{	// just adds to visible sky bounds
				RenderAPIImpl.warp.R_AddSkySurface(surf);
			}
			else if ((surf.texinfo.flags & (Defines.SURF_TRANS33 | Defines.SURF_TRANS66)) != 0)
			{	// add to the translucent chain
				surf.texturechain = r_alpha_surfaces;
				r_alpha_surfaces = surf;
			}
			else
			{
				if (  ( surf.flags & Defines.SURF_DRAWTURB) == 0 )
				{
					GL_RenderLightmappedPoly( surf );
				}
				else
				{
					// the polygon is visible, so add it to the texture
					// sorted chain
					// FIXME: this is a hack for animation
					image = R_TextureAnimation(surf.texinfo);
					surf.texturechain = image.texturechain;
					image.texturechain = surf;
				}
			}
		}
		// recurse down the back side
		R_RecursiveWorldNode(node.children[1 - side]);
	}

	private final TEntity worldEntity = new TEntity();
	
	/**
	 * R_DrawWorld
	 */
	void R_DrawWorld()
	{
		if (RenderAPIImpl.main.r_drawworld.value == 0)
			return;

		if ( (RenderAPIImpl.main.r_newrefdef.rdflags & Defines.RDF_NOWORLDMODEL) != 0 )
			return;

		RenderAPIImpl.main.currentmodel = RenderAPIImpl.main.r_worldmodel;

		Math3D.VectorCopy(RenderAPIImpl.main.r_newrefdef.vieworg, modelorg);

		TEntity ent = worldEntity;
		// auto cycle the world frame for texture animation
		ent.clear();
		ent.frame = (int)(RenderAPIImpl.main.r_newrefdef.time*2);
		RenderAPIImpl.main.currententity = ent;

		RenderAPIImpl.main.gl_state.currenttextures[0] = RenderAPIImpl.main.gl_state.currenttextures[1] = -1;

		GL11.glColor3f (1,1,1);
		// memset (gl_lms.lightmap_surfaces, 0, sizeof(gl_lms.lightmap_surfaces));
		// TODO wird bei multitexture nicht gebraucht
		//gl_lms.clearLightmapSurfaces();

		RenderAPIImpl.warp.R_ClearSkyBox();

		RenderAPIImpl.image.GL_EnableMultitexture( true );

		RenderAPIImpl.image.GL_SelectTexture(RenderAPIImpl.main.TEXTURE0);
		RenderAPIImpl.image.GL_TexEnv(GL11.GL_REPLACE);
		GL11.glInterleavedArrays(GL11.GL_T2F_V3F, TGlPoly.BYTE_STRIDE, globalPolygonInterleavedBuf);
		RenderAPIImpl.image.GL_SelectTexture(RenderAPIImpl.main.TEXTURE1);
		GL11.glTexCoordPointer(2, GL11.GL_FLOAT, TGlPoly.BYTE_STRIDE, globalPolygonTexCoord1Buf);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

		if ( RenderAPIImpl.main.gl_lightmap.value != 0)
			RenderAPIImpl.image.GL_TexEnv(GL11.GL_REPLACE);
		else 
			RenderAPIImpl.image.GL_TexEnv(GL11.GL_MODULATE);
				
		R_RecursiveWorldNode(RenderAPIImpl.main.r_worldmodel.nodes[0]); // root node
				
		ARBMultitexture.glClientActiveTextureARB(RenderAPIImpl.main.TEXTURE1);
		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

		RenderAPIImpl.image.GL_EnableMultitexture( false );

		DrawTextureChains();
		RenderAPIImpl.warp.R_DrawSkyBox();
		R_DrawTriangleOutlines();
	}

	final byte[] fatvis = new byte[Defines.MAX_MAP_LEAFS / 8];

	/**
	 * R_MarkLeaves
	 * Mark the leaves and nodes that are in the PVS for the current
	 * cluster
	 */
	void R_MarkLeaves()
	{
		if (RenderAPIImpl.main.r_oldviewcluster == RenderAPIImpl.main.r_viewcluster && RenderAPIImpl.main.r_oldviewcluster2 == RenderAPIImpl.main.r_viewcluster2 && RenderAPIImpl.main.r_novis.value == 0 && RenderAPIImpl.main.r_viewcluster != -1)
			return;

		// development aid to let you run around and see exactly where
		// the pvs ends
		if (RenderAPIImpl.main.gl_lockpvs.value != 0)
			return;

		RenderAPIImpl.main.r_visframecount++;
		RenderAPIImpl.main.r_oldviewcluster = RenderAPIImpl.main.r_viewcluster;
		RenderAPIImpl.main.r_oldviewcluster2 = RenderAPIImpl.main.r_viewcluster2;

		int i;
		if (RenderAPIImpl.main.r_novis.value != 0 || RenderAPIImpl.main.r_viewcluster == -1 || RenderAPIImpl.main.r_worldmodel.vis == null)
		{
			// mark everything
			for (i=0 ; i<RenderAPIImpl.main.r_worldmodel.numleafs ; i++)
				RenderAPIImpl.main.r_worldmodel.leafs[i].visframe = RenderAPIImpl.main.r_visframecount;
			for (i=0 ; i<RenderAPIImpl.main.r_worldmodel.numnodes ; i++)
				RenderAPIImpl.main.r_worldmodel.nodes[i].visframe = RenderAPIImpl.main.r_visframecount;
			return;
		}

		byte[] vis = RenderAPIImpl.model.Mod_ClusterPVS(RenderAPIImpl.main.r_viewcluster, RenderAPIImpl.main.r_worldmodel);
		int c;
		// may have to combine two clusters because of solid water boundaries
		if (RenderAPIImpl.main.r_viewcluster2 != RenderAPIImpl.main.r_viewcluster)
		{
			// memcpy (fatvis, vis, (r_worldmodel.numleafs+7)/8);
			System.arraycopy(vis, 0, fatvis, 0, (RenderAPIImpl.main.r_worldmodel.numleafs+7) >> 3);
			vis = RenderAPIImpl.model.Mod_ClusterPVS(RenderAPIImpl.main.r_viewcluster2, RenderAPIImpl.main.r_worldmodel);
			c = (RenderAPIImpl.main.r_worldmodel.numleafs + 31) >> 5;
			c <<= 2;
			for (int k=0 ; k<c ; k+=4) {
				fatvis[k] |= vis[k];
				fatvis[k + 1] |= vis[k + 1];
				fatvis[k + 2] |= vis[k + 2];
				fatvis[k + 3] |= vis[k + 3];
			}

			vis = fatvis;
		}

		TMNode node;
		TMLeaf leaf;
		int cluster;
		for ( i=0; i < RenderAPIImpl.main.r_worldmodel.numleafs; i++)
		{
			leaf = RenderAPIImpl.main.r_worldmodel.leafs[i];
			cluster = leaf.cluster;
			if (cluster == -1)
				continue;
			if (((vis[cluster>>3] & 0xFF) & (1 << (cluster & 7))) != 0)
			{
				node = leaf;
				do
				{
					if (node.visframe == RenderAPIImpl.main.r_visframecount)
						break;
					node.visframe = RenderAPIImpl.main.r_visframecount;
					node = node.parent;
				} while (node != null);
			}
		}
	}

	/*
	=============================================================================

	  LIGHTMAP ALLOCATION

	=============================================================================
	*/

	/**
	 * LM_InitBlock
	 */
	void LM_InitBlock()
	{
		Arrays.fill(gl_lms.allocated, 0);
	}

	/**
	 * LM_UploadBlock
	 * @param dynamic
	 */
	void LM_UploadBlock( boolean dynamic )
	{
		int texture = ( dynamic ) ? 0 : gl_lms.current_lightmap_texture;

		RenderAPIImpl.image.bindTexture( RenderAPIImpl.main.gl_state.lightmap_textures + texture );
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

		gl_lms.lightmap_buffer.rewind();
		if ( dynamic )
		{
			int height = 0;
			for (int i = 0; i < BLOCK_WIDTH; i++ )
			{
				if ( gl_lms.allocated[i] > height )
					height = gl_lms.allocated[i];
			}

			GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D,
							  0,
							  0, 0,
							  BLOCK_WIDTH, height,
							  GL_LIGHTMAP_FORMAT,
					GL11.GL_UNSIGNED_BYTE,
							  gl_lms.lightmap_buffer );
		}
		else
		{
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D,
						   0, 
						   gl_lms.internal_format,
						   BLOCK_WIDTH, BLOCK_HEIGHT, 
						   0, 
						   GL_LIGHTMAP_FORMAT,
					GL11.GL_UNSIGNED_BYTE,
						   gl_lms.lightmap_buffer );
			if ( ++gl_lms.current_lightmap_texture == MAX_LIGHTMAPS )
				Com.Error( Defines.ERR_DROP, "LM_UploadBlock() - MAX_LIGHTMAPS exceeded\n" );
				
			//debugLightmap(gl_lms.lightmap_buffer, 128, 128, 4);
		}
	}

	/**
	 * LM_AllocBlock
	 * @param w
	 * @param h
	 * @param pos
	 * @return a texture number and the position inside it
	 */
	boolean LM_AllocBlock (int w, int h, Image.pos_t pos)
	{
		int best = BLOCK_HEIGHT;
		int x = pos.x; 
		int best2;
		int i, j;
		for (i=0 ; i<BLOCK_WIDTH-w ; i++)
		{
			best2 = 0;

			for (j=0 ; j<w ; j++)
			{
				if (gl_lms.allocated[i+j] >= best)
					break;
				if (gl_lms.allocated[i+j] > best2)
					best2 = gl_lms.allocated[i+j];
			}
			if (j == w)
			{	// this is a valid spot
				pos.x = x = i;
				pos.y = best = best2;
			}
		}

		if (best + h > BLOCK_HEIGHT)
			return false;

		for (i=0 ; i<w ; i++)
			gl_lms.allocated[x + i] = best + h;

		return true;
	}

	/**
	 * GL_BuildPolygonFromSurface
	 */
	void GL_BuildPolygonFromSurface(TMapSurface fa)
	{
		// reconstruct the polygon
		TMEdge[] pedges = RenderAPIImpl.main.currentmodel.edges;
		int lnumverts = fa.numedges;
		//
		// draw texture
		//
		// poly = Hunk_Alloc (sizeof(TGlPoly) + (lnumverts-4) * VERTEXSIZE*sizeof(float));
		TGlPoly poly = Polygon.create(lnumverts);

		poly.next = fa.polys;
		poly.flags = fa.flags;
		fa.polys = poly;

		int lindex;
		float[] vec;
		TMEdge r_pedge;
		float s, t;
		for (int i=0 ; i<lnumverts ; i++)
		{
			lindex = RenderAPIImpl.main.currentmodel.surfedges[fa.firstedge + i];

			if (lindex > 0)
			{
				r_pedge = pedges[lindex];
				vec = RenderAPIImpl.main.currentmodel.vertexes[r_pedge.v[0]].position;
			}
			else
			{
				r_pedge = pedges[-lindex];
				vec = RenderAPIImpl.main.currentmodel.vertexes[r_pedge.v[1]].position;
			}
			s = Math3D.DotProduct (vec, fa.texinfo.vecs[0]) + fa.texinfo.vecs[0][3];
			s /= fa.texinfo.image.width;

			t = Math3D.DotProduct (vec, fa.texinfo.vecs[1]) + fa.texinfo.vecs[1][3];
			t /= fa.texinfo.image.height;

			poly.x(i, vec[0]);
			poly.y(i, vec[1]);
			poly.z(i, vec[2]);
			
			poly.s1(i, s);
			poly.t1(i, t);

			//
			// lightmap texture coordinates
			//
			s = Math3D.DotProduct (vec, fa.texinfo.vecs[0]) + fa.texinfo.vecs[0][3];
			s -= fa.texturemins[0];
			s += fa.light_s*16;
			s += 8;
			s /= BLOCK_WIDTH*16; //fa.texinfo.texture.width;

			t = Math3D.DotProduct (vec, fa.texinfo.vecs[1]) + fa.texinfo.vecs[1][3];
			t -= fa.texturemins[1];
			t += fa.light_t*16;
			t += 8;
			t /= BLOCK_HEIGHT*16; //fa.texinfo.texture.height;

			poly.s2(i, s);
			poly.t2(i, t);
		}
	}

	/**
	 * GL_CreateSurfaceLightmap
	 */
	void GL_CreateSurfaceLightmap(TMapSurface surf)
	{
		if ( (surf.flags & (Defines.SURF_DRAWSKY | Defines.SURF_DRAWTURB)) != 0)
			return;

		int smax = (surf.extents[0]>>4)+1;
		int tmax = (surf.extents[1]>>4)+1;
		
		Image.pos_t lightPos = new Image.pos_t(surf.light_s, surf.light_t);

		if ( !LM_AllocBlock( smax, tmax, lightPos ) )
		{
			LM_UploadBlock( false );
			LM_InitBlock();
			lightPos = new Image.pos_t(surf.light_s, surf.light_t);
			if ( !LM_AllocBlock( smax, tmax, lightPos ) )
			{
				Com.Error( Defines.ERR_FATAL, "Consecutive calls to LM_AllocBlock(" + smax +"," + tmax +") failed\n");
			}
		}
		
		// kopiere die koordinaten zurueck
		surf.light_s = lightPos.x;
		surf.light_t = lightPos.y;

		surf.lightmaptexturenum = gl_lms.current_lightmap_texture;
		
		IntBuffer base = gl_lms.lightmap_buffer;
		base.position(surf.light_t * BLOCK_WIDTH + surf.light_s);

		RenderAPIImpl.light.R_SetCacheState( surf );
		RenderAPIImpl.light.R_BuildLightMap(surf, base.slice(), BLOCK_WIDTH);
	}

	TLightStyle[] lightstyles;
	private final IntBuffer dummy = Lib.newIntBuffer(128*128);

	/**
	 * GL_BeginBuildingLightmaps
	 */
	void GL_BeginBuildingLightmaps(TModel m)
	{
		// static TLightStyle	lightstyles[MAX_LIGHTSTYLES];
		int i;

		// init lightstyles
		if ( lightstyles == null ) {
			lightstyles = new TLightStyle[Defines.MAX_LIGHTSTYLES];
			for (i = 0; i < lightstyles.length; i++)
			{
				lightstyles[i] = new TLightStyle();
			}
		}

		// memset( gl_lms.allocated, 0, sizeof(gl_lms.allocated) );
		Arrays.fill(gl_lms.allocated, 0);

		RenderAPIImpl.main.r_framecount = 1;		// no dlightcache

		RenderAPIImpl.image.GL_EnableMultitexture( true );
		RenderAPIImpl.image.GL_SelectTexture(RenderAPIImpl.main.TEXTURE1);

		/*
		** setup the base lightstyles so the lightmaps won't have to be regenerated
		** the first time they're seen
		*/
		for (i=0 ; i < Defines.MAX_LIGHTSTYLES ; i++)
		{
			lightstyles[i].rgb[0] = 1;
			lightstyles[i].rgb[1] = 1;
			lightstyles[i].rgb[2] = 1;
			lightstyles[i].white = 3;
		}
		RenderAPIImpl.main.r_newrefdef.lightstyles = lightstyles;

		if (RenderAPIImpl.main.gl_state.lightmap_textures == 0)
		{
			RenderAPIImpl.main.gl_state.lightmap_textures = TEXNUM_LIGHTMAPS;
		}

		gl_lms.current_lightmap_texture = 1;

		/*
		** if mono lightmaps are enabled and we want to use alpha
		** blending (a,1-a) then we're likely running on a 3DLabs
		** Permedia2.  In a perfect world we'd use a GL_ALPHA lightmap
		** in order to conserve space and maximize bandwidth, however 
		** this isn't a perfect world.
		**
		** So we have to use alpha lightmaps, but stored in GL_RGBA format,
		** which means we only get 1/16th the color resolution we should when
		** using alpha lightmaps.  If we find another board that supports
		** only alpha lightmaps but that can at least support the GL_ALPHA
		** format then we should change this code to use real alpha maps.
		*/
		
		char format = RenderAPIImpl.main.gl_monolightmap.string.toUpperCase().charAt(0);
		
		if ( format == 'A' )
		{
			gl_lms.internal_format = RenderAPIImpl.image.gl_tex_alpha_format;
		}
		/*
		** try to do hacked colored lighting with a blended texture
		*/
		else if ( format == 'C' )
		{
			gl_lms.internal_format = RenderAPIImpl.image.gl_tex_alpha_format;
		}
		else if ( format == 'I' )
		{
			gl_lms.internal_format = GL11.GL_INTENSITY8;
		}
		else if ( format == 'L' ) 
		{
			gl_lms.internal_format = GL11.GL_LUMINANCE8;
		}
		else
		{
			gl_lms.internal_format = RenderAPIImpl.image.gl_tex_solid_format;
		}

		/*
		** initialize the dynamic lightmap texture
		*/
		RenderAPIImpl.image.bindTexture( RenderAPIImpl.main.gl_state.lightmap_textures + 0 );
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D,
					   0, 
					   gl_lms.internal_format,
					   BLOCK_WIDTH, BLOCK_HEIGHT, 
					   0, 
					   GL_LIGHTMAP_FORMAT,
				GL11.GL_UNSIGNED_BYTE,
					   dummy );
	}

	/**
	 * GL_EndBuildingLightmaps
	 */
	void GL_EndBuildingLightmaps()
	{
		LM_UploadBlock( false );
		RenderAPIImpl.image.GL_EnableMultitexture( false );
	}
	
	/*
	 * new buffers for vertex array handling
	 */
	static FloatBuffer globalPolygonInterleavedBuf = Polygon.getInterleavedBuffer();
	static FloatBuffer globalPolygonTexCoord1Buf = null;

	static {
	 	globalPolygonInterleavedBuf.position(TGlPoly.STRIDE - 2);
	 	globalPolygonTexCoord1Buf = globalPolygonInterleavedBuf.slice();
		globalPolygonInterleavedBuf.position(0);
	 }

    //ImageFrame frame;
	
//	void debugLightmap(byte[] buf, int w, int h, float scale) {
//		IntBuffer pix = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
//		
//		int[] pixel = new int[w * h];
//		
//		pix.get(pixel);
//		
//		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
//		image.setRGB(0,  0, w, h, pixel, 0, w);
//		AffineTransformOp op = new AffineTransformOp(AffineTransform.getScaleInstance(scale, scale), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
//		BufferedImage tmp = op.filter(image, null);
//		
//		if (frame == null) {
//			frame = new ImageFrame(null);
//			frame.show();
//		} 
//		frame.showImage(tmp);
//		
//	}

}
