package jake2.client;

import jake2.Defines;
import jake2.common.render.TRenderExport;
import jake2.game.TVar;
import jake2.game.cmdalias_t;
import jake2.game.TEntityState;
import jake2.io.FileSystem;
import jake2.network.TNetAddr;
import jake2.qcommon.TBuffer;
import jake2.render.TModel;

import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.Random;

import static jake2.Defines.MAX_MSGLEN;
import static jake2.Defines.VIDREF_GL;

/**
 * Context ist the collection of global variables and constants.
 * It is more elegant to use these vars by inheritance to separate
 * it with eclipse refactoring later.
 *
 * As consequence you dont have to touch that much code this time.
 */
public final class Context {

    public static final String __DATE__ = "2003";

    public static final float VERSION = 3.21f;

    public static final String BASEDIRNAME = "baseq2";

    public static FileSystem fileSystem = null;

    /*
     * global variables
     */
    public static int curtime = 0;
    public static boolean cmd_wait;

    public static int alias_count;
    public static int c_traces;
    public static int c_brush_traces;
    public static int c_pointcontents;
    public static int server_state;

    public static TVar cl_add_blend;
    public static TVar cl_add_entities;
    public static TVar cl_add_lights;
    public static TVar cl_add_particles;
    public static TVar cl_anglespeedkey;
    public static TVar cl_autoskins;
    public static TVar cl_footsteps;
    public static TVar cl_forwardspeed;
    public static TVar cl_gun;
    public static TVar cl_maxfps;
    public static TVar cl_noskins;
    public static TVar cl_pitchspeed;
    public static TVar cl_predict;
    public static TVar cl_run;
    public static TVar cl_sidespeed;
    public static TVar cl_stereo;
    public static TVar cl_stereo_separation;
    public static TVar cl_timedemo = new TVar();
    public static TVar cl_timeout;
    public static TVar cl_upspeed;
    public static TVar cl_yawspeed;
    public static TVar dedicated;
    public static TVar developer;
    public static TVar fixedtime;
    public static TVar freelook;
    public static TVar host_speeds;
    public static TVar log_stats;
    public static TVar logfile_active;
    public static TVar lookspring;
    public static TVar lookstrafe;
    public static TVar nostdout;
    public static TVar sensitivity;
    public static TVar showtrace;
    public static TVar timescale;
    public static TVar in_mouse;
    public static TVar in_joystick;


    public static TBuffer net_message = TBuffer.createWithSize(MAX_MSGLEN);

    /*
    =============================================================================

                            COMMAND BUFFER

    =============================================================================
    */

    public static cmdalias_t cmd_alias;

    //=============================================================================

    public static int time_before_game;
    public static int time_after_game;
    public static int time_before_ref;
    public static int time_after_ref;

    public static FileWriter log_stats_file = null;

    public static TVar m_pitch;
    public static TVar m_yaw;
    public static TVar m_forward;
    public static TVar m_side;

    public static TVar cl_lightlevel;

    //
    //	   userinfo
    //
    public static TVar info_password;
    public static TVar info_spectator;
    public static TVar name;
    public static TVar skin;
    public static TVar rate;
    public static TVar fov;
    public static TVar msg;
    public static TVar hand;
    public static TVar gender;
    public static TVar gender_auto;

    public static TVar cl_vwep;

    public static TClientStatic cls = new TClientStatic();
    public static TClientState cl = new TClientState();

    public static TClEentity cl_entities[] = new TClEentity[Defines.MAX_EDICTS];
    static {
        for (int i = 0; i < cl_entities.length; i++) {
            cl_entities[i] = new TClEentity();
        }
    }

    public static TEntityState cl_parse_entities[] = new TEntityState[Defines.MAX_PARSE_ENTITIES];

    static {
        for (int i = 0; i < cl_parse_entities.length; i++)
        {
            cl_parse_entities[i] = new TEntityState(null);
        }
    }

    public static TVar rcon_client_password;
    public static TVar rcon_address;

    public static TVar cl_shownet;
    public static TVar cl_showmiss;
    public static TVar cl_showclamp;

    public static TVar cl_paused;

    // client/anorms.h
    public static final float bytedirs[][] = { /**
                                */
        { -0.525731f, 0.000000f, 0.850651f }, {
            -0.442863f, 0.238856f, 0.864188f }, {
            -0.295242f, 0.000000f, 0.955423f }, {
            -0.309017f, 0.500000f, 0.809017f }, {
            -0.162460f, 0.262866f, 0.951056f }, {
            0.000000f, 0.000000f, 1.000000f }, {
            0.000000f, 0.850651f, 0.525731f }, {
            -0.147621f, 0.716567f, 0.681718f }, {
            0.147621f, 0.716567f, 0.681718f }, {
            0.000000f, 0.525731f, 0.850651f }, {
            0.309017f, 0.500000f, 0.809017f }, {
            0.525731f, 0.000000f, 0.850651f }, {
            0.295242f, 0.000000f, 0.955423f }, {
            0.442863f, 0.238856f, 0.864188f }, {
            0.162460f, 0.262866f, 0.951056f }, {
            -0.681718f, 0.147621f, 0.716567f }, {
            -0.809017f, 0.309017f, 0.500000f }, {
            -0.587785f, 0.425325f, 0.688191f }, {
            -0.850651f, 0.525731f, 0.000000f }, {
            -0.864188f, 0.442863f, 0.238856f }, {
            -0.716567f, 0.681718f, 0.147621f }, {
            -0.688191f, 0.587785f, 0.425325f }, {
            -0.500000f, 0.809017f, 0.309017f }, {
            -0.238856f, 0.864188f, 0.442863f }, {
            -0.425325f, 0.688191f, 0.587785f }, {
            -0.716567f, 0.681718f, -0.147621f }, {
            -0.500000f, 0.809017f, -0.309017f }, {
            -0.525731f, 0.850651f, 0.000000f }, {
            0.000000f, 0.850651f, -0.525731f }, {
            -0.238856f, 0.864188f, -0.442863f }, {
            0.000000f, 0.955423f, -0.295242f }, {
            -0.262866f, 0.951056f, -0.162460f }, {
            0.000000f, 1.000000f, 0.000000f }, {
            0.000000f, 0.955423f, 0.295242f }, {
            -0.262866f, 0.951056f, 0.162460f }, {
            0.238856f, 0.864188f, 0.442863f }, {
            0.262866f, 0.951056f, 0.162460f }, {
            0.500000f, 0.809017f, 0.309017f }, {
            0.238856f, 0.864188f, -0.442863f }, {
            0.262866f, 0.951056f, -0.162460f }, {
            0.500000f, 0.809017f, -0.309017f }, {
            0.850651f, 0.525731f, 0.000000f }, {
            0.716567f, 0.681718f, 0.147621f }, {
            0.716567f, 0.681718f, -0.147621f }, {
            0.525731f, 0.850651f, 0.000000f }, {
            0.425325f, 0.688191f, 0.587785f }, {
            0.864188f, 0.442863f, 0.238856f }, {
            0.688191f, 0.587785f, 0.425325f }, {
            0.809017f, 0.309017f, 0.500000f }, {
            0.681718f, 0.147621f, 0.716567f }, {
            0.587785f, 0.425325f, 0.688191f }, {
            0.955423f, 0.295242f, 0.000000f }, {
            1.000000f, 0.000000f, 0.000000f }, {
            0.951056f, 0.162460f, 0.262866f }, {
            0.850651f, -0.525731f, 0.000000f }, {
            0.955423f, -0.295242f, 0.000000f }, {
            0.864188f, -0.442863f, 0.238856f }, {
            0.951056f, -0.162460f, 0.262866f }, {
            0.809017f, -0.309017f, 0.500000f }, {
            0.681718f, -0.147621f, 0.716567f }, {
            0.850651f, 0.000000f, 0.525731f }, {
            0.864188f, 0.442863f, -0.238856f }, {
            0.809017f, 0.309017f, -0.500000f }, {
            0.951056f, 0.162460f, -0.262866f }, {
            0.525731f, 0.000000f, -0.850651f }, {
            0.681718f, 0.147621f, -0.716567f }, {
            0.681718f, -0.147621f, -0.716567f }, {
            0.850651f, 0.000000f, -0.525731f }, {
            0.809017f, -0.309017f, -0.500000f }, {
            0.864188f, -0.442863f, -0.238856f }, {
            0.951056f, -0.162460f, -0.262866f }, {
            0.147621f, 0.716567f, -0.681718f }, {
            0.309017f, 0.500000f, -0.809017f }, {
            0.425325f, 0.688191f, -0.587785f }, {
            0.442863f, 0.238856f, -0.864188f }, {
            0.587785f, 0.425325f, -0.688191f }, {
            0.688191f, 0.587785f, -0.425325f }, {
            -0.147621f, 0.716567f, -0.681718f }, {
            -0.309017f, 0.500000f, -0.809017f }, {
            0.000000f, 0.525731f, -0.850651f }, {
            -0.525731f, 0.000000f, -0.850651f }, {
            -0.442863f, 0.238856f, -0.864188f }, {
            -0.295242f, 0.000000f, -0.955423f }, {
            -0.162460f, 0.262866f, -0.951056f }, {
            0.000000f, 0.000000f, -1.000000f }, {
            0.295242f, 0.000000f, -0.955423f }, {
            0.162460f, 0.262866f, -0.951056f }, {
            -0.442863f, -0.238856f, -0.864188f }, {
            -0.309017f, -0.500000f, -0.809017f }, {
            -0.162460f, -0.262866f, -0.951056f }, {
            0.000000f, -0.850651f, -0.525731f }, {
            -0.147621f, -0.716567f, -0.681718f }, {
            0.147621f, -0.716567f, -0.681718f }, {
            0.000000f, -0.525731f, -0.850651f }, {
            0.309017f, -0.500000f, -0.809017f }, {
            0.442863f, -0.238856f, -0.864188f }, {
            0.162460f, -0.262866f, -0.951056f }, {
            0.238856f, -0.864188f, -0.442863f }, {
            0.500000f, -0.809017f, -0.309017f }, {
            0.425325f, -0.688191f, -0.587785f }, {
            0.716567f, -0.681718f, -0.147621f }, {
            0.688191f, -0.587785f, -0.425325f }, {
            0.587785f, -0.425325f, -0.688191f }, {
            0.000000f, -0.955423f, -0.295242f }, {
            0.000000f, -1.000000f, 0.000000f }, {
            0.262866f, -0.951056f, -0.162460f }, {
            0.000000f, -0.850651f, 0.525731f }, {
            0.000000f, -0.955423f, 0.295242f }, {
            0.238856f, -0.864188f, 0.442863f }, {
            0.262866f, -0.951056f, 0.162460f }, {
            0.500000f, -0.809017f, 0.309017f }, {
            0.716567f, -0.681718f, 0.147621f }, {
            0.525731f, -0.850651f, 0.000000f }, {
            -0.238856f, -0.864188f, -0.442863f }, {
            -0.500000f, -0.809017f, -0.309017f }, {
            -0.262866f, -0.951056f, -0.162460f }, {
            -0.850651f, -0.525731f, 0.000000f }, {
            -0.716567f, -0.681718f, -0.147621f }, {
            -0.716567f, -0.681718f, 0.147621f }, {
            -0.525731f, -0.850651f, 0.000000f }, {
            -0.500000f, -0.809017f, 0.309017f }, {
            -0.238856f, -0.864188f, 0.442863f }, {
            -0.262866f, -0.951056f, 0.162460f }, {
            -0.864188f, -0.442863f, 0.238856f }, {
            -0.809017f, -0.309017f, 0.500000f }, {
            -0.688191f, -0.587785f, 0.425325f }, {
            -0.681718f, -0.147621f, 0.716567f }, {
            -0.442863f, -0.238856f, 0.864188f }, {
            -0.587785f, -0.425325f, 0.688191f }, {
            -0.309017f, -0.500000f, 0.809017f }, {
            -0.147621f, -0.716567f, 0.681718f }, {
            -0.425325f, -0.688191f, 0.587785f }, {
            -0.162460f, -0.262866f, 0.951056f }, {
            0.442863f, -0.238856f, 0.864188f }, {
            0.162460f, -0.262866f, 0.951056f }, {
            0.309017f, -0.500000f, 0.809017f }, {
            0.147621f, -0.716567f, 0.681718f }, {
            0.000000f, -0.525731f, 0.850651f }, {
            0.425325f, -0.688191f, 0.587785f }, {
            0.587785f, -0.425325f, 0.688191f }, {
            0.688191f, -0.587785f, 0.425325f }, {
            -0.955423f, 0.295242f, 0.000000f }, {
            -0.951056f, 0.162460f, 0.262866f }, {
            -1.000000f, 0.000000f, 0.000000f }, {
            -0.850651f, 0.000000f, 0.525731f }, {
            -0.955423f, -0.295242f, 0.000000f }, {
            -0.951056f, -0.162460f, 0.262866f }, {
            -0.864188f, 0.442863f, -0.238856f }, {
            -0.951056f, 0.162460f, -0.262866f }, {
            -0.809017f, 0.309017f, -0.500000f }, {
            -0.864188f, -0.442863f, -0.238856f }, {
            -0.951056f, -0.162460f, -0.262866f }, {
            -0.809017f, -0.309017f, -0.500000f }, {
            -0.681718f, 0.147621f, -0.716567f }, {
            -0.681718f, -0.147621f, -0.716567f }, {
            -0.850651f, 0.000000f, -0.525731f }, {
            -0.688191f, 0.587785f, -0.425325f }, {
            -0.587785f, 0.425325f, -0.688191f }, {
            -0.425325f, 0.688191f, -0.587785f }, {
            -0.425325f, -0.688191f, -0.587785f }, {
            -0.587785f, -0.425325f, -0.688191f }, {
            -0.688191f, -0.587785f, -0.425325f }
    };

    public static boolean userinfo_modified = false;

    public static final TConsole console = new TConsole();
    public static TVar con_notifytime;
    public static TVideoDef viddef = new TVideoDef();
    // RendererFactory interface used by VID, SCR, ...
    public static TRenderExport re = null;

    public static boolean[] keydown = new boolean[256];
    public static boolean chat_team = false;
    public static String chat_buffer = "";
    public static byte[][] key_lines = new byte[32][];

    static {
        for (int i = 0; i < key_lines.length; i++)
            key_lines[i] = new byte[Defines.MAXCMDLINE];
    }

    public static int edit_line;

    public static TVar crosshair;
    public static TRect scr_vrect = new TRect();
    public static int sys_frame_time;
    public static int chat_bufferlen = 0;
    public static int gun_frame;
    public static TModel gun_model;
    public static TNetAddr net_from = new TNetAddr();

    // logfile
    public static RandomAccessFile logfile = null;

    public static float vec3_origin[] = { 0.0f, 0.0f, 0.0f };

    public static TVar m_filter;
    public static int vidref_val = VIDREF_GL;

    public static Random rnd = new Random();
}
