/*
 * Main.java
 * Copyright (C) 2003
 *
 * $Id: Main.java,v 1.6 2008-03-02 14:56:23 cawe Exp $
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
import jake2.game.*;
import jake2.qcommon.*;
import jake2.render.*;
import jake2.util.*;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static jake2.render.Base.*;


/**
 * Main
 * 
 * @author cwei
 */
public class Main {

	public static int[] d_8to24table = new int[256];

	int c_visible_lightmaps;
	int c_visible_textures;

	int registration_sequence;

	// this a hack for function pointer test
	// default disabled
	boolean qglColorTableEXT = false;
	boolean qglActiveTextureARB = false;
	boolean qglPointParameterfEXT = false;
	boolean qglLockArraysEXT = false;
	boolean qwglSwapIntervalEXT = false;

	/*
	====================================================================
	
	from gl_rmain.c
	
	====================================================================
	*/

	int TEXTURE0 = GL13.GL_TEXTURE0;
	int TEXTURE1 = GL13.GL_TEXTURE1;

	TModel r_worldmodel;

	float gldepthmin, gldepthmax;

	TGlConfig gl_config = new TGlConfig();
	TGlState gl_state = new TGlState();

	TImage r_notexture; // use for bad textures
	TImage r_particletexture; // little dot for particles

	TEntity currententity;
	TModel currentmodel;

	cplane_t frustum[] = { new cplane_t(), new cplane_t(), new cplane_t(), new cplane_t()};

	int r_visframecount; // bumped when going to a new PVS
	int r_framecount; // used for dlight push checking

	int c_brush_polys, c_alias_polys;

	float v_blend[] = { 0, 0, 0, 0 }; // final blending color

	//
	//	   view origin
	//
	float[] vecUp = { 0, 0, 0 };
	float[] vecForward = { 0, 0, 0 };
	float[] vecRight = { 0, 0, 0 };
	float[] r_origin = { 0, 0, 0 };

	//float r_world_matrix[] = new float[16];
	FloatBuffer r_world_matrix = Lib.newFloatBuffer(16);
	
	float r_base_world_matrix[] = new float[16];

	//
	//	   screen size info
	//
	TRefDef r_newrefdef = new TRefDef();

	int r_viewcluster, r_viewcluster2, r_oldviewcluster, r_oldviewcluster2;

	TVar r_norefresh;
	TVar r_drawentities;
	TVar r_drawworld;
	TVar r_speeds;
	TVar r_fullbright;
	TVar r_novis;
	TVar r_nocull;
	TVar r_lerpmodels;
	TVar r_lefthand;

	TVar r_lightlevel;
	// FIXME: This is a HACK to get the client's light level

	TVar gl_nosubimage;
	TVar gl_allow_software;

	TVar gl_vertex_arrays;

	TVar gl_particle_min_size;
	TVar gl_particle_max_size;
	TVar gl_particle_size;
	TVar gl_particle_att_a;
	TVar gl_particle_att_b;
	TVar gl_particle_att_c;

	TVar gl_ext_swapinterval;
	TVar gl_ext_palettedtexture;
	TVar gl_ext_multitexture;
	TVar gl_ext_pointparameters;
	TVar gl_ext_compiled_vertex_array;

	TVar gl_log;
	TVar gl_bitdepth;
	TVar gl_drawbuffer;
	TVar gl_driver;
	TVar gl_lightmap;
	TVar gl_shadows;
	TVar gl_mode;
	TVar gl_dynamic;
	TVar gl_monolightmap;
	TVar gl_modulate;
	TVar gl_nobind;
	TVar gl_round_down;
	TVar gl_picmip;
	TVar gl_skymip;
	TVar gl_showtris;
	TVar gl_ztrick;
	TVar gl_finish;
	TVar gl_clear;
	TVar gl_cull;
	TVar gl_polyblend;
	TVar gl_flashblend;
	TVar gl_playermip;
	TVar gl_saturatelighting;
	TVar gl_swapinterval;
	TVar gl_texturemode;
	TVar gl_texturealphamode;
	TVar gl_texturesolidmode;
	TVar gl_lockpvs;

	TVar gl_3dlabs_broken;

	TVar vid_gamma;
	TVar vid_ref;

	// ============================================================================
	// to port from gl_rmain.c, ...
	// ============================================================================

	/**
	 * R_CullBox
	 * Returns true if the box is completely outside the frustum
	 */
	final boolean R_CullBox(float[] mins, float[] maxs) {
		assert(mins.length == 3 && maxs.length == 3) : "vec3_t bug";

		if (r_nocull.value != 0)
			return false;

		for (int i = 0; i < 4; i++) {
			if (Math3D.BoxOnPlaneSide(mins, maxs, frustum[i]) == 2)
				return true;
		}
		return false;
	}

	/**
	 * R_RotateForEntity
	 */
	final void R_RotateForEntity(TEntity e) {
		GL11.glTranslatef(e.origin[0], e.origin[1], e.origin[2]);

		GL11.glRotatef(e.angles[1], 0, 0, 1);
		GL11.glRotatef(-e.angles[0], 0, 1, 0);
		GL11.glRotatef(-e.angles[2], 1, 0, 0);
	}

	/*
	=============================================================
	
	   SPRITE MODELS
	
	=============================================================
	*/

	// stack variable
	private final float[] point = { 0, 0, 0 };
	/**
	 * R_DrawSpriteModel
	 */
	void R_DrawSpriteModel(TEntity e) {
		float alpha = 1.0F;

		qfiles.dsprframe_t frame;
		qfiles.dsprite_t psprite;

		// don't even bother culling, because it's just a single
		// polygon without a surface data

		psprite = (qfiles.dsprite_t) currentmodel.extradata;

		e.frame %= psprite.numframes;

		frame = psprite.frames[e.frame];

		if ((e.flags & Defines.RF_TRANSLUCENT) != 0)
			alpha = e.alpha;

		if (alpha != 1.0F)
			GL11.glEnable(GL11.GL_BLEND);

		GL11.glColor4f(1, 1, 1, alpha);


		RenderAPIImpl.image.GL_Bind(currentmodel.skins[e.frame].texnum);
		RenderAPIImpl.image.GL_TexEnv(GL11.GL_MODULATE);

		if (alpha == 1.0)
			GL11.glEnable(GL11.GL_ALPHA_TEST);
		else
			GL11.glDisable(GL11.GL_ALPHA_TEST);

		GL11.glBegin(GL11.GL_QUADS);

		GL11.glTexCoord2f(0, 1);
		Math3D.VectorMA(e.origin, -frame.origin_y, vecUp, point);
		Math3D.VectorMA(point, -frame.origin_x, vecRight, point);
		GL11.glVertex3f(point[0], point[1], point[2]);

		GL11.glTexCoord2f(0, 0);
		Math3D.VectorMA(e.origin, frame.height - frame.origin_y, vecUp, point);
		Math3D.VectorMA(point, -frame.origin_x, vecRight, point);
		GL11.glVertex3f(point[0], point[1], point[2]);

		GL11.glTexCoord2f(1, 0);
		Math3D.VectorMA(e.origin, frame.height - frame.origin_y, vecUp, point);
		Math3D.VectorMA(point, frame.width - frame.origin_x, vecRight, point);
		GL11.glVertex3f(point[0], point[1], point[2]);

		GL11.glTexCoord2f(1, 1);
		Math3D.VectorMA(e.origin, -frame.origin_y, vecUp, point);
		Math3D.VectorMA(point, frame.width - frame.origin_x, vecRight, point);
		GL11.glVertex3f(point[0], point[1], point[2]);

		GL11.glEnd();

		GL11.glDisable(GL11.GL_ALPHA_TEST);
		RenderAPIImpl.image.GL_TexEnv(GL11.GL_REPLACE);

		if (alpha != 1.0F)
			GL11.glDisable(GL11.GL_BLEND);

		GL11.glColor4f(1, 1, 1, 1);
	}

	// ==================================================================================

	// stack variable
	private final float[] shadelight = { 0, 0, 0 };
	/**
	 * R_DrawNullModel
	*/
	void R_DrawNullModel() {
		if ((currententity.flags & Defines.RF_FULLBRIGHT) != 0) {
			// cwei wollte blau: shadelight[0] = shadelight[1] = shadelight[2] = 1.0F;
			shadelight[0] = shadelight[1] = shadelight[2] = 0.0F;
			shadelight[2] = 0.8F;
		}
		else {
			RenderAPIImpl.light.R_LightPoint(currententity.origin, shadelight);
		}

		GL11.glPushMatrix();
		R_RotateForEntity(currententity);

		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glColor3f(shadelight[0], shadelight[1], shadelight[2]);

		// this replaces the TRIANGLE_FAN
		//glut.glutWireCube(gl, 20);

		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glVertex3f(0, 0, -16);
		int i;
		for (i=0 ; i<=4 ; i++) {
			GL11.glVertex3f((float)(16.0f * Math.cos(i * Math.PI / 2)), (float)(16.0f * Math.sin(i * Math.PI / 2)), 0.0f);
		}
		GL11.glEnd();
		
		GL11.glBegin(GL11.GL_TRIANGLE_FAN);
		GL11.glVertex3f (0, 0, 16);
		for (i=4 ; i>=0 ; i--) {
			GL11.glVertex3f((float)(16.0f * Math.cos(i * Math.PI / 2)), (float)(16.0f * Math.sin(i * Math.PI / 2)), 0.0f);
		}
		GL11.glEnd();

		
		GL11.glColor3f(1, 1, 1);
		GL11.glPopMatrix();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	/**
	 * R_DrawEntitiesOnList
	 */
	void R_DrawEntitiesOnList() {
		if (r_drawentities.value == 0.0f)
			return;

		// draw non-transparent first
		int i;
		for (i = 0; i < r_newrefdef.num_entities; i++) {
			currententity = r_newrefdef.entities[i];
			if ((currententity.flags & Defines.RF_TRANSLUCENT) != 0)
				continue; // solid

			if ((currententity.flags & Defines.RF_BEAM) != 0) {
				R_DrawBeam(currententity);
			}
			else {
				currentmodel = currententity.model;
				if (currentmodel == null) {
					R_DrawNullModel();
					continue;
				}
				switch (currentmodel.type) {
					case mod_alias :
						RenderAPIImpl.mesh.R_DrawAliasModel(currententity);
						break;
					case mod_brush :
						RenderAPIImpl.surf.R_DrawBrushModel(currententity);
						break;
					case mod_sprite :
						RenderAPIImpl.main.R_DrawSpriteModel(currententity);
						break;
					default :
						Com.Error(Defines.ERR_DROP, "Bad modeltype");
						break;
				}
			}
		}
		// draw transparent entities
		// we could sort these if it ever becomes a problem...
		GL11.glDepthMask(false); // no z writes
		for (i = 0; i < r_newrefdef.num_entities; i++) {
			currententity = r_newrefdef.entities[i];
			if ((currententity.flags & Defines.RF_TRANSLUCENT) == 0)
				continue; // solid

			if ((currententity.flags & Defines.RF_BEAM) != 0) {
				R_DrawBeam(currententity);
			}
			else {
				currentmodel = currententity.model;

				if (currentmodel == null) {
					R_DrawNullModel();
					continue;
				}
				switch (currentmodel.type) {
					case mod_alias :
						RenderAPIImpl.mesh.R_DrawAliasModel(currententity);
						break;
					case mod_brush :
						RenderAPIImpl.surf.R_DrawBrushModel(currententity);
						break;
					case mod_sprite :
						R_DrawSpriteModel(currententity);
						break;
					default :
						Com.Error(Defines.ERR_DROP, "Bad modeltype");
						break;
				}
			}
		}
		GL11.glDepthMask(true); // back to writing
	}
	
	// stack variable 
	private final float[] up = { 0, 0, 0 };
	private final float[] right = { 0, 0, 0 };
	/**
	 * GL_DrawParticles
	 */
	void GL_DrawParticles(int num_particles) {
		float origin_x, origin_y, origin_z;

		Math3D.VectorScale(vecUp, 1.5f, up);
		Math3D.VectorScale(vecRight, 1.5f, right);

		RenderAPIImpl.image.GL_Bind(r_particletexture.texnum);
		GL11.glDepthMask(false); // no z buffering
		GL11.glEnable(GL11.GL_BLEND);
		RenderAPIImpl.image.GL_TexEnv(GL11.GL_MODULATE);

		GL11.glBegin(GL11.GL_TRIANGLES);

		FloatBuffer sourceVertices = particle_t.vertexArray;
		IntBuffer sourceColors = particle_t.colorArray;
		float scale;
		int color;
		for (int j = 0, i = 0; i < num_particles; i++) {
			origin_x = sourceVertices.get(j++);
			origin_y = sourceVertices.get(j++);
			origin_z = sourceVertices.get(j++);

			// hack a scale up to keep particles from disapearing
			scale =
				(origin_x - r_origin[0]) * vecForward[0]
					+ (origin_y - r_origin[1]) * vecForward[1]
					+ (origin_z - r_origin[2]) * vecForward[2];

			scale = (scale < 20) ? 1 :  1 + scale * 0.004f;

			color = sourceColors.get(i);

			GL11.glColor4f(
				((color) & 0xFF) / 255.0f,
				((color >> 8) & 0xFF) / 255.0f,
				((color >> 16) & 0xFF) / 255.0f,
				((color >>> 24)) /255.0f
			);
			// first vertex
			GL11.glTexCoord2f(0.0625f, 0.0625f);
			GL11.glVertex3f(origin_x, origin_y, origin_z);
			// second vertex
			GL11.glTexCoord2f(1.0625f, 0.0625f);
			GL11.glVertex3f(origin_x + up[0] * scale, origin_y + up[1] * scale, origin_z + up[2] * scale);
			// third vertex
			GL11.glTexCoord2f(0.0625f, 1.0625f);
			GL11.glVertex3f(origin_x + right[0] * scale, origin_y + right[1] * scale, origin_z + right[2] * scale);
		}
		GL11.glEnd();

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glDepthMask(true); // back to normal Z buffering
		RenderAPIImpl.image.GL_TexEnv(GL11.GL_REPLACE);
	}

	/**
	 * R_DrawParticles
	 */
	void R_DrawParticles() {

		if (gl_ext_pointparameters.value != 0.0f && qglPointParameterfEXT) {

			//GL11.GL11.glEnableClientState(GL_VERTEX_ARRAY);
			GL11.glVertexPointer(3, GL11.GL_INT, 0, particle_t.vertexArray);
			GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
			GL11.glColorPointer(4, GL11.GL_INT, 0, particle_t.getColorAsByteBuffer());
			
			GL11.glDepthMask(false);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glPointSize(gl_particle_size.value);
			
			GL11.glDrawArrays(GL11.GL_POINTS, 0, r_newrefdef.num_particles);
			
			GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
			//GL11.GL11.glDisableClientState(GL_VERTEX_ARRAY);

			GL11.glDisable(GL11.GL_BLEND);
			GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			GL11.glDepthMask(true);
			GL11.glEnable(GL11.GL_TEXTURE_2D);

		}
		else {
			GL_DrawParticles(r_newrefdef.num_particles);
		}
	}

	/**
	 * R_PolyBlend
	 */
	void R_PolyBlend() {
		if (gl_polyblend.value == 0.0f)
			return;

		if (v_blend[3] == 0.0f)
			return;

		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_TEXTURE_2D);

		GL11.glLoadIdentity();

		// FIXME: get rid of these
		GL11.glRotatef(-90, 1, 0, 0); // put Z going up
		GL11.glRotatef(90, 0, 0, 1); // put Z going up

		GL11.glColor4f(v_blend[0], v_blend[1], v_blend[2], v_blend[3]);

		GL11.glBegin(GL11.GL_QUADS);

		GL11.glVertex3f(10, 100, 100);
		GL11.glVertex3f(10, -100, 100);
		GL11.glVertex3f(10, -100, -100);
		GL11.glVertex3f(10, 100, -100);
		GL11.glEnd();

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_ALPHA_TEST);

		GL11.glColor4f(1, 1, 1, 1);
	}

	// =======================================================================

	/**
	 * SignbitsForPlane
	 */
	int SignbitsForPlane(cplane_t out) {
		// for fast box on planeside test
		int bits = 0;
		for (int j = 0; j < 3; j++) {
			if (out.normal[j] < 0)
				bits |= (1 << j);
		}
		return bits;
	}

	/**
	 * R_SetFrustum
	 */
	void R_SetFrustum() {
		// rotate VPN right by FOV_X/2 degrees
		Math3D.RotatePointAroundVector(frustum[0].normal, vecUp, vecForward, - (90f - r_newrefdef.fov_x / 2f));
		// rotate VPN left by FOV_X/2 degrees
		Math3D.RotatePointAroundVector(frustum[1].normal, vecUp, vecForward, 90f - r_newrefdef.fov_x / 2f);
		// rotate VPN up by FOV_X/2 degrees
		Math3D.RotatePointAroundVector(frustum[2].normal, vecRight, vecForward, 90f - r_newrefdef.fov_y / 2f);
		// rotate VPN down by FOV_X/2 degrees
		Math3D.RotatePointAroundVector(frustum[3].normal, vecRight, vecForward, - (90f - r_newrefdef.fov_y / 2f));

		for (int i = 0; i < 4; i++) {
			frustum[i].type = Defines.PLANE_ANYZ;
			frustum[i].dist = Math3D.DotProduct(r_origin, frustum[i].normal);
			frustum[i].signbits = (byte) SignbitsForPlane(frustum[i]);
		}
	}

	// =======================================================================

	// stack variable
	private final float[] temp = {0, 0, 0};
	/**
	 * R_SetupFrame
	 */
	void R_SetupFrame() {
		r_framecount++;

		//	build the transformation matrix for the given view angles
		Math3D.VectorCopy(r_newrefdef.vieworg, r_origin);

		Math3D.AngleVectors(r_newrefdef.viewangles, vecForward, vecRight, vecUp);

		//	current viewcluster
		TMLeaf leaf;
		if ((r_newrefdef.rdflags & Defines.RDF_NOWORLDMODEL) == 0) {
			r_oldviewcluster = r_viewcluster;
			r_oldviewcluster2 = r_viewcluster2;
			leaf = RenderAPIImpl.model.Mod_PointInLeaf(r_origin, r_worldmodel);
			r_viewcluster = r_viewcluster2 = leaf.cluster;

			// check above and below so crossing solid water doesn't draw wrong
			if (leaf.contents == 0) { // look down a bit
				Math3D.VectorCopy(r_origin, temp);
				temp[2] -= 16;
				leaf = RenderAPIImpl.model.Mod_PointInLeaf(temp, r_worldmodel);
				if ((leaf.contents & Defines.CONTENTS_SOLID) == 0 && (leaf.cluster != r_viewcluster2))
					r_viewcluster2 = leaf.cluster;
			}
			else { // look up a bit
				Math3D.VectorCopy(r_origin, temp);
				temp[2] += 16;
				leaf = RenderAPIImpl.model.Mod_PointInLeaf(temp, r_worldmodel);
				if ((leaf.contents & Defines.CONTENTS_SOLID) == 0 && (leaf.cluster != r_viewcluster2))
					r_viewcluster2 = leaf.cluster;
			}
		}

		for (int i = 0; i < 4; i++)
			v_blend[i] = r_newrefdef.blend[i];

		c_brush_polys = 0;
		c_alias_polys = 0;

		// clear out the portion of the screen that the NOWORLDMODEL defines
		if ((r_newrefdef.rdflags & Defines.RDF_NOWORLDMODEL) != 0) {
			GL11.glEnable(GL11.GL_SCISSOR_TEST);
			GL11.glClearColor(0.3f, 0.3f, 0.3f, 1.0f);
			GL11.glScissor(
				r_newrefdef.x,
				vid.getHeight() - r_newrefdef.height - r_newrefdef.y,
				r_newrefdef.width,
				r_newrefdef.height);
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			GL11.glClearColor(1.0f, 0.0f, 0.5f, 0.5f);
			GL11.glDisable(GL11.GL_SCISSOR_TEST);
		}
	}

	/**
	 * MYgluPerspective
	 * 
	 * @param fovy
	 * @param aspect
	 * @param zNear
	 * @param zFar
	 */
	void MYgluPerspective(double fovy, double aspect, double zNear, double zFar) {
		double ymax = zNear * Math.tan(fovy * Math.PI / 360.0);
		double ymin = -ymax;

		double xmin = ymin * aspect;
		double xmax = ymax * aspect;

		xmin += - (2 * gl_state.camera_separation) / zNear;
		xmax += - (2 * gl_state.camera_separation) / zNear;

		GL11.glFrustum(xmin, xmax, ymin, ymax, zNear, zFar);
	}

	/**
	 * R_SetupGL
	 */
	void R_SetupGL() {

		//
		// set up viewport
		//
		//int x = (int) Math.floor(r_newrefdef.x * vid.getWidth() / vid.getWidth());
		int x = r_newrefdef.x;
		//int x2 = (int) Math.ceil((r_newrefdef.x + r_newrefdef.width) * vid.getWidth() / vid.getWidth());
		int x2 = r_newrefdef.x + r_newrefdef.width;
		//int y = (int) Math.floor(vid.getHeight() - r_newrefdef.y * vid.getHeight() / vid.getHeight());
		int y = vid.getHeight() - r_newrefdef.y;
		//int y2 = (int) Math.ceil(vid.getHeight() - (r_newrefdef.y + r_newrefdef.height) * vid.getHeight() / vid.getHeight());
		int y2 = vid.getHeight() - (r_newrefdef.y + r_newrefdef.height);

		int w = x2 - x;
		int h = y - y2;

		GL11.glViewport(x, y2, w, h);

		//
		// set up projection matrix
		//
		float screenaspect = (float) r_newrefdef.width / r_newrefdef.height;
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		MYgluPerspective(r_newrefdef.fov_y, screenaspect, 4, 4096);

		GL11.glCullFace(GL11.GL_FRONT);

		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();

		GL11.glRotatef(-90, 1, 0, 0); // put Z going up
		GL11.glRotatef(90, 0, 0, 1); // put Z going up
		GL11.glRotatef(-r_newrefdef.viewangles[2], 1, 0, 0);
		GL11.glRotatef(-r_newrefdef.viewangles[0], 0, 1, 0);
		GL11.glRotatef(-r_newrefdef.viewangles[1], 0, 0, 1);
		GL11.glTranslatef(-r_newrefdef.vieworg[0], -r_newrefdef.vieworg[1], -r_newrefdef.vieworg[2]);

		GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, r_world_matrix);
		r_world_matrix.clear();

		//
		// set drawing parms
		//
		if (gl_cull.value != 0.0f)
			GL11.glEnable(GL11.GL_CULL_FACE);
		else
			GL11.glDisable(GL11.GL_CULL_FACE);

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}

	int trickframe = 0;

	/**
	 * R_Clear
	 */
	void R_Clear() {
		if (gl_ztrick.value != 0.0f) {

			if (gl_clear.value != 0.0f) {
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
			}

			trickframe++;
			if ((trickframe & 1) != 0) {
				gldepthmin = 0;
				gldepthmax = 0.49999f;
				GL11.glDepthFunc(GL11.GL_LEQUAL);
			}
			else {
				gldepthmin = 1;
				gldepthmax = 0.5f;
				GL11.glDepthFunc(GL11.GL_GEQUAL);
			}
		}
		else {
			if (gl_clear.value != 0.0f)
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			else
				GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

			gldepthmin = 0;
			gldepthmax = 1;
			GL11.glDepthFunc(GL11.GL_LEQUAL);
		}
		GL11.glDepthRange(gldepthmin, gldepthmax);
	}

	/**
	 * R_Flash
	 */
	void R_Flash() {
		R_PolyBlend();
	}

	/**
	 * R_RenderView
	 * r_newrefdef must be set before the first call
	 */
	void R_RenderView(TRefDef fd) {

		if (r_norefresh.value != 0.0f)
			return;

		r_newrefdef = fd;

		// included by cwei
		if (r_newrefdef == null) {
			Com.Error(Defines.ERR_DROP, "R_RenderView: TRefDef fd is null");
		}

		if (r_worldmodel == null && (r_newrefdef.rdflags & Defines.RDF_NOWORLDMODEL) == 0)
			Com.Error(Defines.ERR_DROP, "R_RenderView: NULL worldmodel");

		if (r_speeds.value != 0.0f) {
			c_brush_polys = 0;
			c_alias_polys = 0;
		}

		RenderAPIImpl.light.R_PushDlights();

		if (gl_finish.value != 0.0f)
			GL11.glFinish();

		R_SetupFrame();

		R_SetFrustum();

		R_SetupGL();

		RenderAPIImpl.surf.R_MarkLeaves(); // done here so we know if we're in water

		RenderAPIImpl.surf.R_DrawWorld();

		R_DrawEntitiesOnList();

		RenderAPIImpl.light.renderDynamicLights();

		R_DrawParticles();

		RenderAPIImpl.surf.R_DrawAlphaSurfaces();

		R_Flash();

		if (r_speeds.value != 0.0f) {
			VID.Printf(
				Defines.PRINT_ALL,
				"%4i wpoly %4i epoly %i tex %i lmaps\n",
				new Vargs(4).add(c_brush_polys).add(c_alias_polys).add(c_visible_textures).add(c_visible_lightmaps));
		}
	}

	/**
	 * R_SetGL2D
	 */
	void R_SetGL2D() {
		// set 2D virtual screen size
		GL11.glViewport(0, 0, vid.getWidth(), vid.getHeight());
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, vid.getWidth(), vid.getHeight(), 0, -99999, 99999);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glColor4f(1, 1, 1, 1);
	}

	// stack variable
	private final float[] light = { 0, 0, 0 };
	/**
	 *	R_SetLightLevel
	 */
	void R_SetLightLevel() {
		if ((r_newrefdef.rdflags & Defines.RDF_NOWORLDMODEL) != 0)
			return;

		// save off light value for server to look at (BIG HACK!)

		RenderAPIImpl.light.R_LightPoint(r_newrefdef.vieworg, light);

		// pick the greatest component, which should be the same
		// as the mono value returned by software
		if (light[0] > light[1]) {
			if (light[0] > light[2])
				r_lightlevel.value = 150 * light[0];
			else
				r_lightlevel.value = 150 * light[2];
		}
		else {
			if (light[1] > light[2])
				r_lightlevel.value = 150 * light[1];
			else
				r_lightlevel.value = 150 * light[2];
		}
	}

	/**
	 * R_RenderFrame
	 */
	public void R_RenderFrame(TRefDef fd) {
		R_RenderView(fd);
		R_SetLightLevel();
		R_SetGL2D();
	}

	/**
	 * R_Register
	 */
	protected void R_Register() {
		r_lefthand = ConsoleVar.Get("hand", "0", TVar.CVAR_FLAG_USERINFO | TVar.CVAR_FLAG_ARCHIVE);
		r_norefresh = ConsoleVar.Get("r_norefresh", "0", 0);
		r_fullbright = ConsoleVar.Get("r_fullbright", "0", 0);
		r_drawentities = ConsoleVar.Get("r_drawentities", "1", 0);
		r_drawworld = ConsoleVar.Get("r_drawworld", "1", 0);
		r_novis = ConsoleVar.Get("r_novis", "0", 0);
		r_nocull = ConsoleVar.Get("r_nocull", "0", 0);
		r_lerpmodels = ConsoleVar.Get("r_lerpmodels", "1", 0);
		r_speeds = ConsoleVar.Get("r_speeds", "0", 0);

		r_lightlevel = ConsoleVar.Get("r_lightlevel", "1", 0);

		gl_nosubimage = ConsoleVar.Get("gl_nosubimage", "0", 0);
		gl_allow_software = ConsoleVar.Get("gl_allow_software", "0", 0);

		gl_particle_min_size = ConsoleVar.Get("gl_particle_min_size", "2", TVar.CVAR_FLAG_ARCHIVE);
		gl_particle_max_size = ConsoleVar.Get("gl_particle_max_size", "40", TVar.CVAR_FLAG_ARCHIVE);
		gl_particle_size = ConsoleVar.Get("gl_particle_size", "40", TVar.CVAR_FLAG_ARCHIVE);
		gl_particle_att_a = ConsoleVar.Get("gl_particle_att_a", "0.01", TVar.CVAR_FLAG_ARCHIVE);
		gl_particle_att_b = ConsoleVar.Get("gl_particle_att_b", "0.0", TVar.CVAR_FLAG_ARCHIVE);
		gl_particle_att_c = ConsoleVar.Get("gl_particle_att_c", "0.01", TVar.CVAR_FLAG_ARCHIVE);

		gl_modulate = ConsoleVar.Get("gl_modulate", "1.5", TVar.CVAR_FLAG_ARCHIVE);
		gl_log = ConsoleVar.Get("gl_log", "0", 0);
		gl_bitdepth = ConsoleVar.Get("gl_bitdepth", "0", 0);
		gl_mode = ConsoleVar.Get("gl_mode", "3", TVar.CVAR_FLAG_ARCHIVE); // 640x480
		gl_lightmap = ConsoleVar.Get("gl_lightmap", "0", 0);
		gl_shadows = ConsoleVar.Get("gl_shadows", "0", TVar.CVAR_FLAG_ARCHIVE);
		gl_dynamic = ConsoleVar.Get("gl_dynamic", "1", 0);
		gl_nobind = ConsoleVar.Get("gl_nobind", "0", 0);
		gl_round_down = ConsoleVar.Get("gl_round_down", "1", 0);
		gl_picmip = ConsoleVar.Get("gl_picmip", "0", 0);
		gl_skymip = ConsoleVar.Get("gl_skymip", "0", 0);
		gl_showtris = ConsoleVar.Get("gl_showtris", "0", 0);
		gl_ztrick = ConsoleVar.Get("gl_ztrick", "0", 0);
		gl_finish = ConsoleVar.Get("gl_finish", "0", TVar.CVAR_FLAG_ARCHIVE);
		gl_clear = ConsoleVar.Get("gl_clear", "0", 0);
		gl_cull = ConsoleVar.Get("gl_cull", "1", 0);
		gl_polyblend = ConsoleVar.Get("gl_polyblend", "1", 0);
		gl_flashblend = ConsoleVar.Get("gl_flashblend", "0", 0);
		gl_playermip = ConsoleVar.Get("gl_playermip", "0", 0);
		gl_monolightmap = ConsoleVar.Get("gl_monolightmap", "0", 0);
		gl_driver = ConsoleVar.Get("gl_driver", "opengl32", TVar.CVAR_FLAG_ARCHIVE);
		gl_texturemode = ConsoleVar.Get("gl_texturemode", "GL_LINEAR_MIPMAP_NEAREST", TVar.CVAR_FLAG_ARCHIVE);
		gl_texturealphamode = ConsoleVar.Get("gl_texturealphamode", "default", TVar.CVAR_FLAG_ARCHIVE);
		gl_texturesolidmode = ConsoleVar.Get("gl_texturesolidmode", "default", TVar.CVAR_FLAG_ARCHIVE);
		gl_lockpvs = ConsoleVar.Get("gl_lockpvs", "0", 0);

		gl_vertex_arrays = ConsoleVar.Get("gl_vertex_arrays", "1", TVar.CVAR_FLAG_ARCHIVE);

		gl_ext_swapinterval = ConsoleVar.Get("gl_ext_swapinterval", "1", TVar.CVAR_FLAG_ARCHIVE);
		gl_ext_palettedtexture = ConsoleVar.Get("gl_ext_palettedtexture", "0", TVar.CVAR_FLAG_ARCHIVE);
		gl_ext_multitexture = ConsoleVar.Get("gl_ext_multitexture", "1", TVar.CVAR_FLAG_ARCHIVE);
		gl_ext_pointparameters = ConsoleVar.Get("gl_ext_pointparameters", "1", TVar.CVAR_FLAG_ARCHIVE);
		gl_ext_compiled_vertex_array = ConsoleVar.Get("gl_ext_compiled_vertex_array", "1", TVar.CVAR_FLAG_ARCHIVE);

		gl_drawbuffer = ConsoleVar.Get("gl_drawbuffer", "GL_BACK", 0);
		gl_swapinterval = ConsoleVar.Get("gl_swapinterval", "0", TVar.CVAR_FLAG_ARCHIVE);

		gl_saturatelighting = ConsoleVar.Get("gl_saturatelighting", "0", 0);

		gl_3dlabs_broken = ConsoleVar.Get("gl_3dlabs_broken", "1", TVar.CVAR_FLAG_ARCHIVE);

		VID.vid_fullscreen = ConsoleVar.Get("vid_fullscreen", "0", TVar.CVAR_FLAG_ARCHIVE);
		vid_gamma = ConsoleVar.Get("vid_gamma", "1.0", TVar.CVAR_FLAG_ARCHIVE);
		vid_ref = ConsoleVar.Get("vid_ref", "lwjgl", TVar.CVAR_FLAG_ARCHIVE);

		Cmd.AddCommand("imagelist", new xcommand_t() {
			public void execute() {
				RenderAPIImpl.image.GL_ImageList_f();
			}
		});

		Cmd.AddCommand("screenshot", new xcommand_t() {
			public void execute() {
				RenderAPIImpl.glImpl.screenshot();
			}
		});
		Cmd.AddCommand("modellist", new xcommand_t() {
			public void execute() {
				RenderAPIImpl.model.Mod_Modellist_f();
			}
		});
		Cmd.AddCommand("gl_strings", new xcommand_t() {
			public void execute() {
				RenderAPIImpl.misc.GL_Strings_f();
			}
		});
	}

	/**
	 * R_SetMode
	 */
	protected boolean R_SetMode() {
		boolean fullscreen = (VID.vid_fullscreen.value > 0.0f);

		VID.vid_fullscreen.modified = false;
		gl_mode.modified = false;

		Dimension dim = new Dimension(vid.getWidth(), vid.getHeight());

		int err; //  enum rserr_t
		if ((err = RenderAPIImpl.glImpl.setMode(dim, (int) gl_mode.value, fullscreen)) == rserr_ok) {
			gl_state.prev_mode = (int) gl_mode.value;
		}
		else {
			if (err == rserr_invalid_fullscreen) {
				ConsoleVar.SetValue("VID.vid_fullscreen", 0);
				VID.vid_fullscreen.modified = false;
				VID.Printf(Defines.PRINT_ALL, "ref_gl::R_SetMode() - fullscreen unavailable in this mode\n");
				if ((err = RenderAPIImpl.glImpl.setMode(dim, (int) gl_mode.value, false)) == rserr_ok)
					return true;
			}
			else if (err == rserr_invalid_mode) {
				ConsoleVar.SetValue("gl_mode", gl_state.prev_mode);
				gl_mode.modified = false;
				VID.Printf(Defines.PRINT_ALL, "ref_gl::R_SetMode() - invalid mode\n");
			}

			// try setting it back to something safe
			if ((err = RenderAPIImpl.glImpl.setMode(dim, gl_state.prev_mode, false)) != rserr_ok) {
				VID.Printf(Defines.PRINT_ALL, "ref_gl::R_SetMode() - could not revert to safe mode\n");
				return false;
			}
		}
		return true;
	}

	float[] r_turbsin = new float[256];

    /**
     * R_Init
     */
    protected boolean R_Init() {
        return R_Init(0, 0);
    }
	/**
	 * R_Init
	 */
	public boolean R_Init(int vid_xpos, int vid_ypos) {

		assert(Warp.SIN.length == 256) : "warpsin table bug";

		// fill r_turbsin
		for (int j = 0; j < 256; j++) {
			r_turbsin[j] = Warp.SIN[j] * 0.5f;
		}

		VID.Printf(Defines.PRINT_ALL, "ref_gl version: " + REF_VERSION + '\n');

		RenderAPIImpl.image.Draw_GetPalette();

		R_Register();

		// set our "safe" modes
		gl_state.prev_mode = 3;

		// create the window and set up the context
		if (!R_SetMode()) {
			VID.Printf(Defines.PRINT_ALL, "ref_gl::R_Init() - could not R_SetMode()\n");
			return false;
		}
		return true;
	}

	/**
	 * R_Init2
	 */
	public boolean R_Init2() {
		VID.MenuInit();

		/*
		** get our various GL strings
//		*/
		gl_config.vendor_string = GL11.glGetString(GL11.GL_VENDOR);
		VID.Printf(Defines.PRINT_ALL, "GL_VENDOR: " + gl_config.vendor_string + '\n');
		gl_config.renderer_string = GL11.glGetString(GL11.GL_RENDERER);
		VID.Printf(Defines.PRINT_ALL, "GL_RENDERER: " + gl_config.renderer_string + '\n');
		gl_config.version_string = GL11.glGetString(GL11.GL_VERSION);
		VID.Printf(Defines.PRINT_ALL, "GL_VERSION: " + gl_config.version_string + '\n');
		gl_config.extensions_string = GL11.glGetString(GL11.GL_EXTENSIONS);
		VID.Printf(Defines.PRINT_ALL, "GL_EXTENSIONS: " + gl_config.extensions_string + '\n');

		gl_config.parseOpenGLVersion();

		String renderer_buffer = gl_config.renderer_string.toLowerCase();
		String vendor_buffer = gl_config.vendor_string.toLowerCase();

		if (renderer_buffer.indexOf("voodoo") >= 0) {
			if (renderer_buffer.indexOf("rush") < 0)
				gl_config.renderer = GL_RENDERER_VOODOO;
			else
				gl_config.renderer = GL_RENDERER_VOODOO_RUSH;
		}
		else if (vendor_buffer.indexOf("sgi") >= 0)
			gl_config.renderer = GL_RENDERER_SGI;
		else if (renderer_buffer.indexOf("permedia") >= 0)
			gl_config.renderer = GL_RENDERER_PERMEDIA2;
		else if (renderer_buffer.indexOf("glint") >= 0)
			gl_config.renderer = GL_RENDERER_GLINT_MX;
		else if (renderer_buffer.indexOf("glzicd") >= 0)
			gl_config.renderer = GL_RENDERER_REALIZM;
		else if (renderer_buffer.indexOf("gdi") >= 0)
			gl_config.renderer = GL_RENDERER_MCD;
		else if (renderer_buffer.indexOf("pcx2") >= 0)
			gl_config.renderer = GL_RENDERER_PCX2;
		else if (renderer_buffer.indexOf("verite") >= 0)
			gl_config.renderer = GL_RENDERER_RENDITION;
		else
			gl_config.renderer = GL_RENDERER_OTHER;

		String monolightmap = gl_monolightmap.string.toUpperCase();
		if (monolightmap.length() < 2 || monolightmap.charAt(1) != 'F') {
			if (gl_config.renderer == GL_RENDERER_PERMEDIA2) {
				ConsoleVar.Set("gl_monolightmap", "A");
				VID.Printf(Defines.PRINT_ALL, "...using gl_monolightmap 'a'\n");
			}
			else if ((gl_config.renderer & GL_RENDERER_POWERVR) != 0) {
				ConsoleVar.Set("gl_monolightmap", "0");
			}
			else {
				ConsoleVar.Set("gl_monolightmap", "0");
			}
		}

		// power vr can't have anything stay in the framebuffer, so
		// the screen needs to redraw the tiled background every frame
		if ((gl_config.renderer & GL_RENDERER_POWERVR) != 0) {
			ConsoleVar.Set("scr_drawall", "1");
		}
		else {
			ConsoleVar.Set("scr_drawall", "0");
		}

		// MCD has buffering issues
		if (gl_config.renderer == GL_RENDERER_MCD) {
			ConsoleVar.SetValue("gl_finish", 1);
		}

		if ((gl_config.renderer & GL_RENDERER_3DLABS) != 0) {
            gl_config.allow_cds = !(gl_3dlabs_broken.value != 0.0f);
		}
		else {
			gl_config.allow_cds = true;
		}

		if (gl_config.allow_cds)
			VID.Printf(Defines.PRINT_ALL, "...allowing CDS\n");
		else
			VID.Printf(Defines.PRINT_ALL, "...disabling CDS\n");

		/*
		** grab extensions
		*/
		if (gl_config.extensions_string.indexOf("GL_EXT_compiled_vertex_array") >= 0
			|| gl_config.extensions_string.indexOf("GL_SGI_compiled_vertex_array") >= 0) {
			VID.Printf(Defines.PRINT_ALL, "...enabling GL_EXT_compiled_vertex_array\n");
			//		 qGL11.glLockArraysEXT = ( void * ) qwglGetProcAddress( "glLockArraysEXT" );
            qglLockArraysEXT = gl_ext_compiled_vertex_array.value != 0.0f;
			//		 qGL11.glUnlockArraysEXT = ( void * ) qwglGetProcAddress( "glUnlockArraysEXT" );
			//qglUnlockArraysEXT = true;
		}
		else {
			VID.Printf(Defines.PRINT_ALL, "...GL_EXT_compiled_vertex_array not found\n");
			qglLockArraysEXT = false;
		}

		if (gl_config.extensions_string.indexOf("WGL_EXT_swap_control") >= 0) {
			qwglSwapIntervalEXT = true;
			VID.Printf(Defines.PRINT_ALL, "...enabling WGL_EXT_swap_control\n");
		} else {
			qwglSwapIntervalEXT = false;
			VID.Printf(Defines.PRINT_ALL, "...WGL_EXT_swap_control not found\n");
		}

		if (gl_config.extensions_string.indexOf("GL_EXT_point_parameters") >= 0) {
			if (gl_ext_pointparameters.value != 0.0f) {
				//			 qGL11.glPointParameterfEXT = ( void (APIENTRY *)( GLenum, GLfloat ) ) qwglGetProcAddress( "glPointParameterfEXT" );
				qglPointParameterfEXT = true;
				//			 qGL11.glPointParameterfvEXT = ( void (APIENTRY *)( GLenum, const GLfloat * ) ) qwglGetProcAddress( "glPointParameterfvEXT" );
				VID.Printf(Defines.PRINT_ALL, "...using GL_EXT_point_parameters\n");
			}
			else {
				VID.Printf(Defines.PRINT_ALL, "...ignoring GL_EXT_point_parameters\n");
			}
		}
		else {
			VID.Printf(Defines.PRINT_ALL, "...GL_EXT_point_parameters not found\n");
		}

		// #ifdef __linux__
		//	 if ( strstr( gl_config.extensions_string, "3DFX_set_global_palette" ))
		//	 {
		//		 if ( gl_ext_palettedtexture->value )
		//		 {
		//			 VID.Printf( Defines.PRINT_ALL, "...using 3DFX_set_global_palette\n" );
		//			 qgl3DfxSetPaletteEXT = ( void ( APIENTRY * ) (GLuint *) )qwGL11.glGetProcAddress( "gl3DfxSetPaletteEXT" );
		////			 qglColorTableEXT = Fake_glColorTableEXT;
		//		 }
		//		 else
		//		 {
		//			 VID.Printf( Defines.PRINT_ALL, "...ignoring 3DFX_set_global_palette\n" );
		//		 }
		//	 }
		//	 else
		//	 {
		//		 VID.Printf( Defines.PRINT_ALL, "...3DFX_set_global_palette not found\n" );
		//	 }
		// #endif

		if (!qglColorTableEXT
			&& gl_config.extensions_string.indexOf("GL_EXT_paletted_texture") >= 0
			&& gl_config.extensions_string.indexOf("GL_EXT_shared_texture_palette") >= 0) {
			if (gl_ext_palettedtexture.value != 0.0f) {
				VID.Printf(Defines.PRINT_ALL, "...using GL_EXT_shared_texture_palette\n");
				qglColorTableEXT = false; // true; TODO jogl bug
			}
			else {
				VID.Printf(Defines.PRINT_ALL, "...ignoring GL_EXT_shared_texture_palette\n");
				qglColorTableEXT = false;
			}
		}
		else {
			VID.Printf(Defines.PRINT_ALL, "...GL_EXT_shared_texture_palette not found\n");
		}

		if (gl_config.extensions_string.indexOf("GL_ARB_multitexture") >= 0) {
			// check if the extension realy exists
			try {
				ARBMultitexture.glClientActiveTextureARB(ARBMultitexture.GL_TEXTURE0_ARB);
				// seems to work correctly
				VID.Printf(Defines.PRINT_ALL, "...using GL_ARB_multitexture\n");
				qglActiveTextureARB = true;
				TEXTURE0 = ARBMultitexture.GL_TEXTURE0_ARB;
				TEXTURE1 = ARBMultitexture.GL_TEXTURE1_ARB;
			} catch (Exception e) {
				qglActiveTextureARB = false;
			}
		}
		else {
			qglActiveTextureARB = false;
			VID.Printf(Defines.PRINT_ALL, "...GL_ARB_multitexture not found\n");
		}

		if (!(qglActiveTextureARB)) {
            VID.Printf(Defines.PRINT_ALL, "Missing multi-texturing!\n");
            return false;
        }

		RenderAPIImpl.misc.GL_SetDefaultState();

		RenderAPIImpl.image.GL_InitImages();
		RenderAPIImpl.model.Mod_Init();
		RenderAPIImpl.misc.R_InitParticleTexture();
		RenderAPIImpl.draw.Draw_InitLocal();

		int err = GL11.glGetError();
		if (err != GL11.GL_NO_ERROR)
			VID.Printf(
				Defines.PRINT_ALL,
				"GL11.glGetError() = 0x%x\n\t%s\n",
				new Vargs(2).add(err).add("" + GL11.glGetString(err)));

        RenderAPIImpl.glImpl.endFrame();
		return true;
	}

	/**
	 * R_Shutdown
	 */
	public void R_Shutdown() {
		Cmd.RemoveCommand("modellist");
		Cmd.RemoveCommand("screenshot");
		Cmd.RemoveCommand("imagelist");
		Cmd.RemoveCommand("gl_strings");

		RenderAPIImpl.model.Mod_FreeAll();

		RenderAPIImpl.image.GL_ShutdownImages();

		/*
		 * shut down OS specific OpenGL stuff like contexts, etc.
		 */
		RenderAPIImpl.glImpl.shutdown();
	}

	/**
	 * R_BeginFrame
	 */
	public void R_BeginFrame(float camera_separation) {
	    
	    vid.update();


		gl_state.camera_separation = camera_separation;

		/*
		** change modes if necessary
		*/
		if (gl_mode.modified || VID.vid_fullscreen.modified) {
			// FIXME: only restart if CDS is required
			TVar ref;

			ref = ConsoleVar.Get("vid_ref", "lwjgl", 0);
			ref.modified = true;
		}

		if (gl_log.modified) {
			RenderAPIImpl.glImpl.enableLogging((gl_log.value != 0.0f));
			gl_log.modified = false;
		}

		if (gl_log.value != 0.0f) {
			RenderAPIImpl.glImpl.logNewFrame();
		}

		/*
		** update 3Dfx gamma -- it is expected that a user will do a vid_restart
		** after tweaking this value
		*/
		if (vid_gamma.modified) {
			vid_gamma.modified = false;

			if ((gl_config.renderer & GL_RENDERER_VOODOO) != 0) {
				// wird erstmal nicht gebraucht

				/* 
				char envbuffer[1024];
				float g;
				
				g = 2.00 * ( 0.8 - ( vid_gamma->value - 0.5 ) ) + 1.0F;
				Com_sprintf( envbuffer, sizeof(envbuffer), "SSTV2_GAMMA=%f", g );
				putenv( envbuffer );
				Com_sprintf( envbuffer, sizeof(envbuffer), "SST_GAMMA=%f", g );
				putenv( envbuffer );
				*/
				VID.Printf(Defines.PRINT_DEVELOPER, "gamma anpassung fuer VOODOO nicht gesetzt");
			}
		}

		RenderAPIImpl.glImpl.beginFrame(camera_separation);

		/*
		** go into 2D mode
		*/
		GL11.glViewport(0, 0, vid.getWidth(), vid.getHeight());
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, vid.getWidth(), vid.getHeight(), 0, -99999, 99999);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		GL11.glLoadIdentity();
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glColor4f(1, 1, 1, 1);

		/*
		** draw buffer stuff
		*/
		if (gl_drawbuffer.modified) {
			gl_drawbuffer.modified = false;

			if (gl_state.camera_separation == 0 || !gl_state.stereo_enabled) {
				if (gl_drawbuffer.string.equalsIgnoreCase("GL_FRONT"))
					GL11.glDrawBuffer(GL11.GL_FRONT);
				else
					GL11.glDrawBuffer(GL11.GL_BACK);
			}
		}

		/*
		** texturemode stuff
		*/
		if (gl_texturemode.modified) {
			RenderAPIImpl.image.GL_TextureMode(gl_texturemode.string);
			gl_texturemode.modified = false;
		}

		if (gl_texturealphamode.modified) {
			RenderAPIImpl.image.GL_TextureAlphaMode(gl_texturealphamode.string);
			gl_texturealphamode.modified = false;
		}

		if (gl_texturesolidmode.modified) {
			RenderAPIImpl.image.GL_TextureSolidMode(gl_texturesolidmode.string);
			gl_texturesolidmode.modified = false;
		}

		/*
		** swapinterval stuff
		*/
		RenderAPIImpl.misc.GL_UpdateSwapInterval();

		//
		// clear screen if desired
		//
		R_Clear();
	}

	int[] r_rawpalette = new int[256];

	/**
	 * R_SetPalette
	 */
	public void R_SetPalette(byte[] palette) {
		// 256 RGB values (768 bytes)
		// or null
		int i;
		int color = 0;

		if (palette != null) {
			int j =0;
			for (i = 0; i < 256; i++) {
				color = (palette[j++] & 0xFF) << 0;
				color |= (palette[j++] & 0xFF) << 8;
				color |= (palette[j++] & 0xFF) << 16;
				color |= 0xFF000000;
				r_rawpalette[i] = color;
			}
		}
		else {
			for (i = 0; i < 256; i++) {
				r_rawpalette[i] = d_8to24table[i] | 0xff000000;
			}
		}
		RenderAPIImpl.image.GL_SetTexturePalette(r_rawpalette);

		GL11.glClearColor(0, 0, 0, 0);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		GL11.glClearColor(1f, 0f, 0.5f, 0.5f);
	}

	static final int NUM_BEAM_SEGS = 6;
	float[][] start_points = new float[NUM_BEAM_SEGS][3];
	// array of vec3_t
	float[][] end_points = new float[NUM_BEAM_SEGS][3]; // array of vec3_t

	// stack variable
	private final float[] perpvec = { 0, 0, 0 }; // vec3_t
	private final float[] direction = { 0, 0, 0 }; // vec3_t
	private final float[] normalized_direction = { 0, 0, 0 }; // vec3_t
	private final float[] oldorigin = { 0, 0, 0 }; // vec3_t
	private final float[] origin = { 0, 0, 0 }; // vec3_t
	/**
	 * R_DrawBeam
	 */
	void R_DrawBeam(TEntity e) {
		oldorigin[0] = e.oldorigin[0];
		oldorigin[1] = e.oldorigin[1];
		oldorigin[2] = e.oldorigin[2];

		origin[0] = e.origin[0];
		origin[1] = e.origin[1];
		origin[2] = e.origin[2];

		normalized_direction[0] = direction[0] = oldorigin[0] - origin[0];
		normalized_direction[1] = direction[1] = oldorigin[1] - origin[1];
		normalized_direction[2] = direction[2] = oldorigin[2] - origin[2];

		if (Math3D.VectorNormalize(normalized_direction) == 0.0f)
			return;

		Math3D.PerpendicularVector(perpvec, normalized_direction);
		Math3D.VectorScale(perpvec, e.frame / 2, perpvec);

		for (int i = 0; i < 6; i++) {
			Math3D.RotatePointAroundVector(
				start_points[i],
				normalized_direction,
				perpvec,
				(360.0f / NUM_BEAM_SEGS) * i);

			Math3D.VectorAdd(start_points[i], origin, start_points[i]);
			Math3D.VectorAdd(start_points[i], direction, end_points[i]);
		}

		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDepthMask(false);

		float r = (d_8to24table[e.skinnum & 0xFF]) & 0xFF;
		float g = (d_8to24table[e.skinnum & 0xFF] >> 8) & 0xFF;
		float b = (d_8to24table[e.skinnum & 0xFF] >> 16) & 0xFF;

		r *= 1 / 255.0f;
		g *= 1 / 255.0f;
		b *= 1 / 255.0f;

		GL11.glColor4f(r, g, b, e.alpha);

		GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
		
		float[] v;
		
		for (int i = 0; i < NUM_BEAM_SEGS; i++) {
			v = start_points[i];
			GL11.glVertex3f(v[0], v[1], v[2]);
			v = end_points[i];
			GL11.glVertex3f(v[0], v[1], v[2]);
			v = start_points[(i + 1) % NUM_BEAM_SEGS];
			GL11.glVertex3f(v[0], v[1], v[2]);
			v = end_points[(i + 1) % NUM_BEAM_SEGS];
			GL11.glVertex3f(v[0], v[1], v[2]);
		}
		GL11.glEnd();

		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDepthMask(true);
	}
}