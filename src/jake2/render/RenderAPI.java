package jake2.render;

import jake2.common.Dimension;
import jake2.client.TRefDef;
import jake2.render.opengl.GLDriver;


public interface RenderAPI {

    void setGLDriver(GLDriver impl);

    boolean R_Init(int vid_xpos, int vid_ypos);

    boolean R_Init2();

    void R_Shutdown();

    void R_BeginRegistration(String map);

    TModel R_RegisterModel(String name);

    TImage R_RegisterSkin(String name);

    TImage Draw_FindPic(String name);

    void R_SetSky(String name, float rotate, float[] axis);

    void R_EndRegistration();

    void R_RenderFrame(TRefDef fd);

    void Draw_GetPicSize(Dimension dim, String name);

    void Draw_Pic(int x, int y, String name);

    void Draw_StretchPic(int x, int y, int w, int h, String name);

    void Draw_Char(int x, int y, int num);

    void Draw_TileClear(int x, int y, int w, int h, String name);

    void Draw_Fill(int x, int y, int w, int h, int c);

    void Draw_FadeScreen();

    void Draw_StretchRaw(int x, int y, int w, int h, int cols, int rows,
            byte[] data);

    void R_SetPalette(byte[] palette);

    void R_BeginFrame(float camera_separation);

    void GL_ScreenShot_f();
}
