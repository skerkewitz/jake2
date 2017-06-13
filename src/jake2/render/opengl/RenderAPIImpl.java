package jake2.render.opengl;

import jake2.common.Dimension;
import jake2.client.TRefDef;
import jake2.render.Base;
import jake2.render.TImage;
import jake2.render.TModel;
import jake2.render.fast.*;
import jake2.render.opengl.GLDriver;
import jake2.render.opengl.RenderAPI;

/**
 * Created by tropper on 23.05.17.
 */
public class RenderAPIImpl implements RenderAPI {

    public static GLDriver glImpl;

    public static RenderMain renderMain = new RenderMain();
    public static Image image = new Image();
    public static Light light = new Light();
    public static Mesh mesh = new Mesh();
    public static Model model = new Model();
    public static Surf surf = new Surf();
    public static Warp warp = new Warp();
    public static Misc misc = new Misc();
    public static Draw draw = new Draw();

    @Override
    public void setGLDriver(GLDriver impl) {
        glImpl = impl;
    }

    @Override
    public boolean R_Init(int vid_xpos, int vid_ypos) {
        return renderMain.R_Init(0, 0);
    }

    @Override
    public boolean R_Init2() {
        return renderMain.R_Init2();
    }

    @Override
    public void R_Shutdown() {
        renderMain.R_Shutdown();
    }

    @Override
    public void R_BeginRegistration(String map) {
        model.R_BeginRegistration(map);
    }

    @Override
    public TModel R_RegisterModel(String name) {
        return model.R_RegisterModel(name);
    }

    @Override
    public TImage R_RegisterSkin(String name) {
        return image.R_RegisterSkin(name);
    }

    @Override
    public TImage Draw_FindPic(String name) {
        return draw.Draw_FindPic(name);
    }

    @Override
    public void R_SetSky(String name, float rotate, float[] axis) {
        warp.R_SetSky(name, rotate, axis);
    }

    @Override
    public void R_EndRegistration() {
        model.R_EndRegistration();
    }

    @Override
    public void R_RenderFrame(TRefDef fd) {
        renderMain.renderFrame(fd);
    }

    @Override
    public void Draw_GetPicSize(Dimension dim, String name) {
        draw.Draw_GetPicSize(dim, name);
    }

    @Override
    public void Draw_Pic(int x, int y, String name) {
        draw.Draw_Pic(x, y, name);
    }

    @Override
    public void Draw_StretchPic(int x, int y, int w, int h, String name) {
        draw.Draw_StretchPic(x, y, w, h, name);
    }

    @Override
    public void Draw_Char(int x, int y, int num) {
        draw.Draw_Char(x, y, num);
    }

    @Override
    public void Draw_TileClear(int x, int y, int w, int h, String name) {
        draw.Draw_TileClear(x, y, w, h, name);
    }

    @Override
    public void Draw_Fill(int x, int y, int w, int h, int c) {
        draw.Draw_Fill(x, y, w, h, c);
    }

    @Override
    public void Draw_FadeScreen() {
        draw.Draw_FadeScreen();
    }

    @Override
    public void Draw_StretchRaw(int x, int y, int w, int h, int cols, int rows, byte[] data) {
        draw.Draw_StretchRaw(x, y, w, h, cols, rows, data);
    }

    @Override
    public void R_SetPalette(byte[] palette) {
        renderMain.R_SetPalette(palette);
    }

    @Override
    public void R_BeginFrame(float camera_separation) {
        renderMain.R_BeginFrame(camera_separation);
    }

    @Override
    public void GL_ScreenShot_f() {
        misc.GL_ScreenShot_f(Base.vid);
    }
}
