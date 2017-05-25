/*
 * Draw.java
 * Copyright (C) 2003
 *
 * $Id: Draw.java,v 1.4 2008-03-02 14:56:23 cawe Exp $
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
import jake2.client.Dimension;
import jake2.client.VID;
import jake2.qcommon.Com;
import jake2.render.RenderAPIImpl;
import jake2.render.TImage;
import jake2.util.Lib;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static jake2.render.Base.*;
import static jake2.render.fast.Main.d_8to24table;

/**
 * Draw
 * (gl_draw.c)
 *
 * @author cwei
 */
public class Draw {

    /*
    ===============
    Draw_InitLocal
    ===============
    */
    void Draw_InitLocal() {
        // load console characters (don't bilerp characters)
        RenderAPIImpl.image.draw_chars = RenderAPIImpl.image.GL_FindImage("pics/conchars.pcx", it_pic);
        RenderAPIImpl.image.bindTexture(RenderAPIImpl.image.draw_chars.texnum);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    /*
    ================
    Draw_Char

    Draws one 8*8 graphics character with 0 being transparent.
    It can be clipped to the top of the screen to allow the console to be
    smoothly scrolled off.
    ================
    */
    public void Draw_Char(int x, int y, int num) {

        num &= 255;

        if ((num & 127) == 32) return; // space

        if (y <= -8) return; // totally off screen

        int row = num >> 4;
        int col = num & 15;

        float frow = row * 0.0625f;
        float fcol = col * 0.0625f;
        float size = 0.0625f;

        RenderAPIImpl.image.bindTexture(RenderAPIImpl.image.draw_chars.texnum);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(fcol, frow);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(fcol + size, frow);
        GL11.glVertex2f(x + 8, y);
        GL11.glTexCoord2f(fcol + size, frow + size);
        GL11.glVertex2f(x + 8, y + 8);
        GL11.glTexCoord2f(fcol, frow + size);
        GL11.glVertex2f(x, y + 8);
        GL11.glEnd();
    }


    /*
    =============
    Draw_FindPic
    =============
    */
    public TImage Draw_FindPic(String name) {
        if (!name.startsWith("/") && !name.startsWith("\\")) {
            return RenderAPIImpl.image.GL_FindImage(name, it_pic);
        } else {
            return RenderAPIImpl.image.GL_FindImage(name.substring(1), it_pic);
        }
    }


    /*
    =============
    Draw_GetPicSize
    =============
    */
    public void Draw_GetPicSize(Dimension dim, String pic) {

        TImage image = Draw_FindPic(pic);
        dim.width = (image != null) ? image.width : -1;
        dim.height = (image != null) ? image.height : -1;
    }

    /*
    =============
    Draw_StretchPic
    =============
    */
    public void Draw_StretchPic(int x, int y, int w, int h, String pic) {

        TImage image;

        image = Draw_FindPic(pic);
        if (image == null) {
            VID.Printf(VID.PRINT_ALL, "Can't find pic: " + pic + '\n');
            return;
        }

        if (RenderAPIImpl.image.scrap_dirty)
            RenderAPIImpl.image.Scrap_Upload();

        if (((RenderAPIImpl.main.gl_config.renderer == GL_RENDERER_MCD) || ((RenderAPIImpl.main.gl_config.renderer & GL_RENDERER_RENDITION) != 0)) && !image.has_alpha)
            GL11.glDisable(GL11.GL_ALPHA_TEST);

        RenderAPIImpl.image.bindTexture(image.texnum);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(image.sl, image.tl);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(image.sh, image.tl);
        GL11.glVertex2f(x + w, y);
        GL11.glTexCoord2f(image.sh, image.th);
        GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(image.sl, image.th);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();

        if (((RenderAPIImpl.main.gl_config.renderer == GL_RENDERER_MCD) || ((RenderAPIImpl.main.gl_config.renderer & GL_RENDERER_RENDITION) != 0)) && !image.has_alpha)
            GL11.glEnable(GL11.GL_ALPHA_TEST);
    }


    /*
    =============
    Draw_Pic
    =============
    */
    public void Draw_Pic(int x, int y, String pic) {
        TImage image;

        image = Draw_FindPic(pic);
        if (image == null) {
            VID.Printf(VID.PRINT_ALL, "Can't find pic: " + pic + '\n');
            return;
        }
        if (RenderAPIImpl.image.scrap_dirty)
            RenderAPIImpl.image.Scrap_Upload();

        if (((RenderAPIImpl.main.gl_config.renderer == GL_RENDERER_MCD) || ((RenderAPIImpl.main.gl_config.renderer & GL_RENDERER_RENDITION) != 0)) && !image.has_alpha)
            GL11.glDisable(GL11.GL_ALPHA_TEST);

        RenderAPIImpl.image.bindTexture(image.texnum);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(image.sl, image.tl);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(image.sh, image.tl);
        GL11.glVertex2f(x + image.width, y);
        GL11.glTexCoord2f(image.sh, image.th);
        GL11.glVertex2f(x + image.width, y + image.height);
        GL11.glTexCoord2f(image.sl, image.th);
        GL11.glVertex2f(x, y + image.height);
        GL11.glEnd();

        if (((RenderAPIImpl.main.gl_config.renderer == GL_RENDERER_MCD) || ((RenderAPIImpl.main.gl_config.renderer & GL_RENDERER_RENDITION) != 0)) && !image.has_alpha)
            GL11.glEnable(GL11.GL_ALPHA_TEST);
    }

    /*
    =============
    Draw_TileClear

    This repeats a 64*64 tile graphic to fill the screen around a sized down
    refresh window.
    =============
    */
    public void Draw_TileClear(int x, int y, int w, int h, String pic) {
        TImage image;

        image = Draw_FindPic(pic);
        if (image == null) {
            VID.Printf(VID.PRINT_ALL, "Can't find pic: " + pic + '\n');
            return;
        }

        if (((RenderAPIImpl.main.gl_config.renderer == GL_RENDERER_MCD) || ((RenderAPIImpl.main.gl_config.renderer & GL_RENDERER_RENDITION) != 0)) && !image.has_alpha)
            GL11.glDisable(GL11.GL_ALPHA_TEST);

        RenderAPIImpl.image.bindTexture(image.texnum);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(x / 64.0f, y / 64.0f);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f((x + w) / 64.0f, y / 64.0f);
        GL11.glVertex2f(x + w, y);
        GL11.glTexCoord2f((x + w) / 64.0f, (y + h) / 64.0f);
        GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(x / 64.0f, (y + h) / 64.0f);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();

        if (((RenderAPIImpl.main.gl_config.renderer == GL_RENDERER_MCD) || ((RenderAPIImpl.main.gl_config.renderer & GL_RENDERER_RENDITION) != 0)) && !image.has_alpha)
            GL11.glEnable(GL11.GL_ALPHA_TEST);
    }


    /*
    =============
    Draw_Fill

    Fills a box of pixels with a single color
    =============
    */
    public void Draw_Fill(int x, int y, int w, int h, int colorIndex) {

        if (colorIndex > 255)
            Com.Error(Defines.ERR_FATAL, "Draw_Fill: bad color");

        GL11.glDisable(GL11.GL_TEXTURE_2D);

        int color = d_8to24table[colorIndex];

        GL11.glColor3ub(
                (byte) ((color >> 0) & 0xff), // r
                (byte) ((color >> 8) & 0xff), // g
                (byte) ((color >> 16) & 0xff) // b
        );

        GL11.glBegin(GL11.GL_QUADS);

        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x + w, y);
        GL11.glVertex2f(x + w, y + h);
        GL11.glVertex2f(x, y + h);

        GL11.glEnd();
        GL11.glColor3f(1, 1, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    //=============================================================================

    /*
    ================
    Draw_FadeScreen
    ================
    */
    public void Draw_FadeScreen() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0, 0, 0, 0.8f);
        GL11.glBegin(GL11.GL_QUADS);

        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(vid.getWidth(), 0);
        GL11.glVertex2f(vid.getWidth(), vid.getHeight());
        GL11.glVertex2f(0, vid.getHeight());

        GL11.glEnd();
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }

// ====================================================================

    IntBuffer image32 = Lib.newIntBuffer(256 * 256);
    ByteBuffer image8 = Lib.newByteBuffer(256 * 256);


    /*
    =============
    Draw_StretchRaw
    =============
    */
    public void Draw_StretchRaw(int x, int y, int w, int h, int cols, int rows, byte[] data) {
        int i, j, trows;
        int sourceIndex;
        int frac, fracstep;
        float hscale;
        int row;
        float t;

        RenderAPIImpl.image.bindTexture(0);

        if (rows <= 256) {
            hscale = 1;
            trows = rows;
        } else {
            hscale = rows / 256.0f;
            trows = 256;
        }
        t = rows * hscale / 256;

        if (!RenderAPIImpl.main.qglColorTableEXT) {
            //int[] image32 = new int[256*256];
            image32.clear();
            int destIndex = 0;

            for (i = 0; i < trows; i++) {
                row = (int) (i * hscale);
                if (row > rows)
                    break;
                sourceIndex = cols * row;
                destIndex = i * 256;
                fracstep = cols * 0x10000 / 256;
                frac = fracstep >> 1;
                for (j = 0; j < 256; j++) {
                    image32.put(destIndex + j, RenderAPIImpl.main.r_rawpalette[data[sourceIndex + (frac >> 16)] & 0xff]);
                    frac += fracstep;
                }
            }
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, RenderAPIImpl.image.gl_tex_solid_format, 256, 256, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, image32);
        } else {
            //byte[] image8 = new byte[256*256];
            image8.clear();
            int destIndex = 0;

            for (i = 0; i < trows; i++) {
                row = (int) (i * hscale);
                if (row > rows)
                    break;
                sourceIndex = cols * row;
                destIndex = i * 256;
                fracstep = cols * 0x10000 / 256;
                frac = fracstep >> 1;
                for (j = 0; j < 256; j++) {
                    image8.put(destIndex + j, data[sourceIndex + (frac >> 16)]);
                    frac += fracstep;
                }
            }

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,
                    0,
                    GL_COLOR_INDEX8_EXT,
                    256, 256,
                    0,
                    GL11.GL_COLOR_INDEX,
                    GL11.GL_UNSIGNED_BYTE,
                    image8);
        }
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        if ((RenderAPIImpl.main.gl_config.renderer == GL_RENDERER_MCD) || ((RenderAPIImpl.main.gl_config.renderer & GL_RENDERER_RENDITION) != 0))
            GL11.glDisable(GL11.GL_ALPHA_TEST);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(x + w, y);
        GL11.glTexCoord2f(1, t);
        GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(0, t);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();

        if ((RenderAPIImpl.main.gl_config.renderer == GL_RENDERER_MCD) || ((RenderAPIImpl.main.gl_config.renderer & GL_RENDERER_RENDITION) != 0))
            GL11.glEnable(GL11.GL_ALPHA_TEST);
    }

}
