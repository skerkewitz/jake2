/*
 * LWJGLSoundImpl.java
 * Copyright (C) 2004
 *
 * $Id: LWJGLSoundImpl.java,v 1.10 2007-05-11 20:33:53 cawe Exp $
 */
package jake2.sound.lwjgl;

import jake2.Defines;
import jake2.client.Context;
import jake2.game.Cmd;
import jake2.game.TEntityState;
import jake2.game.TVar;
import jake2.qcommon.Command;
import jake2.qcommon.ConsoleVar;
import jake2.io.FileSystem;
import jake2.sound.*;
import jake2.util.Lib;
import org.lwjgl.openal.*;

import java.nio.*;
import java.util.List;

import static jake2.Defines.CS_PLAYERSKINS;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.*;
import static org.lwjgl.openal.EXTThreadLocalContext.alcSetThreadContext;


/**
 * LWJGLSoundDriverImpl
 *
 * @author dsanders/cwei
 */
public final class LWJGLSoundDriverImpl implements SoundDriver {

    static {
        Sound.register(new LWJGLSoundDriverImpl());
    }

    private TVar s_volume;

    // the last 4 buffers are used for cinematics streaming
    private IntBuffer buffers = Lib.newIntBuffer(MAX_SFX + STREAM_QUEUE);
    private long device;

    // singleton
    private LWJGLSoundDriverImpl() {
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundImpl#init()
     */
    public boolean Init() {

        try {
            initOpenAL();
            checkError();
        } catch (Exception e) {
            Command.Printf(e.getMessage() + '\n');
            return false;
        }

        // set the listerner (master) volume
        s_volume = ConsoleVar.Get("s_volume", "0.7", TVar.CVAR_FLAG_ARCHIVE);
        AL10.alGenBuffers(buffers);
        int count = Channel.init(buffers);
        Command.Printf("... using " + count + " channels\n");
        AL10.alDistanceModel(AL10.AL_INVERSE_DISTANCE_CLAMPED);
        Cmd.AddCommand("play", () -> Play());
        Cmd.AddCommand("stopsound", () -> StopAllSounds());
        Cmd.AddCommand("soundlist", () -> SoundList());
        Cmd.AddCommand("soundinfo", () -> SoundInfo_f());

        num_sfx = 0;

        Command.Printf("sound sampling rate: 44100Hz\n");

        StopAllSounds();
        Command.Printf("------------------------------------\n");
        return true;
    }


    private void initOpenAL() {
//		device = ALC10.nalcOpenDevice(0);
//		ALCCapabilities deviceCaps = ALC.createCapabilities(device);
//
//		String deviceName = null;
//
//		String os = QSystem.getProperty("os.name");
//		if (os.startsWith("Windows")) {
//		    deviceName = "DirectSound3D";
//		}
//
//		String defaultSpecifier = ALC10.alcGetString(device, ALC10.ALC_DEFAULT_DEVICE_SPECIFIER);
//
//		Command.Printf(os + " using " + ((deviceName == null) ? defaultSpecifier : deviceName) + '\n');
//
//		// Check for an error.
//		if (ALC10.alcGetError(device) != ALC10.ALC_NO_ERROR)
//		{
//		    Command.DPrintf("Error with SoundDevice");
//		}

        device = alcOpenDevice((ByteBuffer) null);
        if (device == 0) {
            throw new IllegalStateException("Failed to open the default device.");
        }

        ALCCapabilities deviceCaps = ALC.createCapabilities(device);

        //assertTrue(deviceCaps.OpenALC10);

        System.out.println("OpenALC10: " + deviceCaps.OpenALC10);
        System.out.println("OpenALC11: " + deviceCaps.OpenALC11);
        System.out.println("caps.ALC_EXT_EFX = " + deviceCaps.ALC_EXT_EFX);

        if (deviceCaps.OpenALC11) {
            List<String> devices = ALUtil.getStringList(0, ALC_ALL_DEVICES_SPECIFIER);
            if (devices == null) {
//				checkALCError(NULL);
            } else {
                for (int i = 0; i < devices.size(); i++) {
                    System.out.println(i + ": " + devices.get(i));
                }
            }
        }

        String defaultDeviceSpecifier = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        //assertTrue(defaultDeviceSpecifier != null);
        System.out.println("Default device: " + defaultDeviceSpecifier);

        long context = alcCreateContext(device, (IntBuffer) null);
        alcSetThreadContext(context);
        AL.createCapabilities(deviceCaps);

        System.out.println("ALC_FREQUENCY: " + alcGetInteger(device, ALC_FREQUENCY) + "Hz");
        System.out.println("ALC_REFRESH: " + alcGetInteger(device, ALC_REFRESH) + "Hz");
        System.out.println("ALC_SYNC: " + (alcGetInteger(device, ALC_SYNC) == ALC_TRUE));
        System.out.println("ALC_MONO_SOURCES: " + alcGetInteger(device, ALC_MONO_SOURCES));
        System.out.println("ALC_STEREO_SOURCES: " + alcGetInteger(device, ALC_STEREO_SOURCES));


    }

    void exitOpenAL() {
        ALC10.alcCloseDevice(device);
    }

    // TODO check the sfx direct buffer size
    // 2MB sfx buffer
    private ByteBuffer sfxDataBuffer = Lib.newByteBuffer(2 * 1024 * 1024);

    /* (non-Javadoc)
     * @see jake2.sound.SoundImpl#RegisterSound(jake2.sound.TSound)
     */
    private void initBuffer(byte[] samples, int bufferId, int freq) {
        ByteBuffer data = sfxDataBuffer.slice();
        data.put(samples).flip();
        AL10.alBufferData(buffers.get(bufferId), AL10.AL_FORMAT_MONO16,
                data, freq);
    }

    private void checkError() {
        Command.DPrintf("AL Error: " + alErrorString() + '\n');
    }

    private String alErrorString() {
        int error;
        String message = "";
        if ((error = AL10.alGetError()) != AL10.AL_NO_ERROR) {
            switch (error) {
                case AL10.AL_INVALID_OPERATION:
                    message = "invalid operation";
                    break;
                case AL10.AL_INVALID_VALUE:
                    message = "invalid value";
                    break;
                case AL10.AL_INVALID_ENUM:
                    message = "invalid enum";
                    break;
                case AL10.AL_INVALID_NAME:
                    message = "invalid name";
                    break;
                default:
                    message = "" + error;
            }
        }
        return message;
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundImpl#Shutdown()
     */
    public void Shutdown() {
        StopAllSounds();
        Channel.shutdown();
        AL10.alDeleteBuffers(buffers);
        exitOpenAL();

        Cmd.RemoveCommand("play");
        Cmd.RemoveCommand("stopsound");
        Cmd.RemoveCommand("soundlist");
        Cmd.RemoveCommand("soundinfo");

        // free all sounds
        for (int i = 0; i < num_sfx; i++) {
            if (known_sfx[i].getName() == null)
                continue;
            known_sfx[i].clear();
        }
        num_sfx = 0;
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundImpl#StartSound(float[], int, int, jake2.sound.TSound, float, float, float)
     */
    public void StartSound(float[] origin, int entnum, int entchannel, TSound sfx, float fvol, float attenuation, float timeofs) {

        if (sfx == null)
            return;

        if (sfx.getName().charAt(0) == '*')
            sfx = RegisterSexedSound(Context.cl_entities[entnum].current, sfx.getName());

        if (LoadSound(sfx) == null)
            return; // can't load sound

        if (attenuation != Defines.ATTN_STATIC)
            attenuation *= 0.5f;

        PlaySound.allocate(origin, entnum, entchannel, buffers.get(sfx.getBufferId()), fvol, attenuation, timeofs);
    }

    private FloatBuffer listenerOrigin = Lib.newFloatBuffer(3);
    private FloatBuffer listenerOrientation = Lib.newFloatBuffer(6);

    /* (non-Javadoc)
     * @see jake2.sound.SoundImpl#Update(float[], float[], float[], float[])
     */
    public void Update(float[] origin, float[] forward, float[] right, float[] up) {

        Channel.convertVector(origin, listenerOrigin);
        AL10.alListenerfv(AL10.AL_POSITION, listenerOrigin);

        Channel.convertOrientation(forward, up, listenerOrientation);
        AL10.alListenerfv(AL10.AL_ORIENTATION, listenerOrientation);

        // set the master volume
        AL10.alListenerf(AL10.AL_GAIN, s_volume.value);

        Channel.addLoopSounds();
        Channel.addPlaySounds();
        Channel.playAllSounds(listenerOrigin);
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundImpl#StopAllSounds()
     */
    public void StopAllSounds() {
        // mute the listener (master)
        AL10.alListenerf(AL10.AL_GAIN, 0);
        PlaySound.reset();
        Channel.reset();
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundDriver#getName()
     */
    public String getName() {
        return "lwjgl";
    }

    int s_registration_sequence;
    boolean s_registering;

    /* (non-Javadoc)
     * @see jake2.sound.SoundDriver#BeginRegistration()
     */
    public void BeginRegistration() {
        s_registration_sequence++;
        s_registering = true;
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundDriver#RegisterSound(java.lang.String)
     */
    public TSound RegisterSound(String name) {
        TSound sfx = FindName(name, true);
        sfx.setRegistration_sequence(s_registration_sequence);

        if (!s_registering)
            LoadSound(sfx);

        return sfx;
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundDriver#EndRegistration()
     */
    public void EndRegistration() {
        int i;
        TSound sfx;
        // free any sounds not from this registration sequence
        for (i = 0; i < num_sfx; i++) {
            sfx = known_sfx[i];
            if (sfx.getName() == null)
                continue;
            if (sfx.getRegistration_sequence() != s_registration_sequence) {
                // don't need this sound
                sfx.clear();
            }
        }

        // load everything in
        for (i = 0; i < num_sfx; i++) {
            sfx = known_sfx[i];
            if (sfx.getName() == null)
                continue;
            LoadSound(sfx);
        }

        s_registering = false;
    }

    TSound RegisterSexedSound(TEntityState ent, String base) {

        TSound sfx = null;

        // determine what model the client is using
        String model = null;
        int n = CS_PLAYERSKINS + ent.number - 1;
        if (Context.cl.configstrings[n] != null) {
            int p = Context.cl.configstrings[n].indexOf('\\');
            if (p >= 0) {
                p++;
                model = Context.cl.configstrings[n].substring(p);
                //strcpy(model, p);
                p = model.indexOf('/');
                if (p > 0)
                    model = model.substring(0, p);
            }
        }
        // if we can't figure it out, they're male
        if (model == null || model.length() == 0)
            model = "male";

        // see if we already know of the model specific sound
        String sexedFilename = "#players/" + model + "/" + base.substring(1);
        //Com_sprintf (sexedFilename, sizeof(sexedFilename), "#players/%s/%s", model, base+1);
        sfx = FindName(sexedFilename, false);

        if (sfx != null) return sfx;

        //
        // fall back strategies
        //
        // not found , so see if it exists
        if (FileSystem.FileLength(sexedFilename.substring(1)) > 0) {
            // yes, register it
            return RegisterSound(sexedFilename);
        }
        // try it with the female sound in the pak0.pak
        if (model.equalsIgnoreCase("female")) {
            String femaleFilename = "player/female/" + base.substring(1);
            if (FileSystem.FileLength("sound/" + femaleFilename) > 0)
                return AliasName(sexedFilename, femaleFilename);
        }
        // no chance, revert to the male sound in the pak0.pak
        String maleFilename = "player/male/" + base.substring(1);
        return AliasName(sexedFilename, maleFilename);
    }


    static TSound[] known_sfx = new TSound[MAX_SFX];

    static {
        for (int i = 0; i < known_sfx.length; i++)
            known_sfx[i] = new TSound();
    }

    static int num_sfx;

    TSound FindName(String name, boolean create) {
        int i;


        if (name == null)
            Command.Error(Defines.ERR_FATAL, "S_FindName: NULL\n");
        if (name.length() == 0)
            Command.Error(Defines.ERR_FATAL, "S_FindName: empty name\n");

        if (name.length() >= Defines.MAX_QPATH)
            Command.Error(Defines.ERR_FATAL, "SoundDriver name too long: " + name);

        // see if already loaded
        for (i = 0; i < num_sfx; i++)
            if (name.equals(known_sfx[i].getName())) {
                return known_sfx[i];
            }

        if (!create)
            return null;

        // find a free sfx
        for (i = 0; i < num_sfx; i++)
            if (known_sfx[i].getName() == null)
                // registration_sequence < s_registration_sequence)
                break;

        if (i == num_sfx) {
            if (num_sfx == MAX_SFX)
                Command.Error(Defines.ERR_FATAL, "S_FindName: out of TSound");
            num_sfx++;
        }

        TSound sfx = known_sfx[i];
        sfx.clear();
        sfx.setName(name);
        sfx.setRegistration_sequence(s_registration_sequence);
        sfx.setBufferId(i);

        return sfx;
    }

    /*
    ==================
	S_AliasName

	==================
     */
    TSound AliasName(String aliasname, String truename) {
        TSound sfx = null;
        String s;
        int i;

        s = new String(truename);

        // find a free sfx
        for (i = 0; i < num_sfx; i++)
            if (known_sfx[i].getName() == null)
                break;

        if (i == num_sfx) {
            if (num_sfx == MAX_SFX)
                Command.Error(Defines.ERR_FATAL, "S_FindName: out of TSound");
            num_sfx++;
        }

        sfx = known_sfx[i];
        sfx.clear();
        sfx.setName(new String(aliasname));
        sfx.setRegistration_sequence(s_registration_sequence);
        sfx.setTruename(s);
        // set the AL bufferId
        sfx.setBufferId(i);

        return sfx;
    }

    /*
	==============
	S_LoadSound
	==============
     */

    private static WaveLoader waveLoader = new WaveLoader();

    public TSoundData LoadSound(TSound s) {
        if (s.isCached()) return s.getData();
        TSoundData sc = waveLoader.LoadSound(s);
        if (sc != null) {
            initBuffer(sc.data, s.getBufferId(), sc.speed);
            s.setCached(true);
            // free samples for GC
            s.getData().data = null;
        }
        return sc;
    }

    /* (non-Javadoc)
     * @see jake2.sound.SoundDriver#StartLocalSound(java.lang.String)
     */
    public void StartLocalSound(String name) {
        TSound sound = RegisterSound(name);
        if (sound == null) {
            Command.Printf("S_StartLocalSound: can't data " + name + "\n");
            return;
        }
        StartSound(null, Context.cl.playernum + 1, 0, sound, 1, 1, 0);
    }

    private ShortBuffer streamBuffer = sfxDataBuffer.slice().order(ByteOrder.BIG_ENDIAN).asShortBuffer();

    /* (non-Javadoc)
     * @see jake2.sound.SoundDriver#RawSamples(int, int, int, int, byte[])
     */
    public void RawSamples(int samples, int rate, int width, int channels, ByteBuffer data) {
        int format;
        if (channels == 2) {
            format = (width == 2) ? AL10.AL_FORMAT_STEREO16
                    : AL10.AL_FORMAT_STEREO8;
        } else {
            format = (width == 2) ? AL10.AL_FORMAT_MONO16
                    : AL10.AL_FORMAT_MONO8;
        }

        // convert to signed 16 bit samples
        if (format == AL10.AL_FORMAT_MONO8) {
            ShortBuffer sampleData = streamBuffer;
            int value;
            for (int i = 0; i < samples; i++) {
                value = (data.get(i) & 0xFF) - 128;
                sampleData.put(i, (short) value);
            }
            format = AL10.AL_FORMAT_MONO16;
            width = 2;
            data = sfxDataBuffer.slice();
        }

        Channel.updateStream(data, samples * channels * width, format, rate);
    }

    public void disableStreaming() {
        Channel.disableStreaming();
    }
    /*
	===============================================================================

	console functions

	===============================================================================
     */

    void Play() {
        int i;
        String name;
        TSound sfx;

        i = 1;
        while (i < Cmd.Argc()) {
            name = new String(Cmd.Argv(i));
            if (name.indexOf('.') == -1)
                name += ".wav";

            sfx = RegisterSound(name);
            StartSound(null, Context.cl.playernum + 1, 0, sfx, 1.0f, 1.0f, 0.0f);
            i++;
        }
    }

    void SoundList() {
        int i;
        TSound sfx;
        TSoundData sc;
        int size, total;

        total = 0;
        for (i = 0; i < num_sfx; i++) {
            sfx = known_sfx[i];
            if (sfx.getRegistration_sequence() == 0)
                continue;
            sc = sfx.getData();
            if (sc != null) {
                size = sc.length * sc.width * (sc.stereo + 1);
                total += size;
                if (sc.loopstart >= 0)
                    Command.Printf("L");
                else
                    Command.Printf(" ");
                Command.Printf("(%2db) %6i : %s\n", sc.width * 8, size, sfx.getName());
            } else {
                if (sfx.getName().charAt(0) == '*')
                    Command.Printf("  placeholder : " + sfx.getName() + "\n");
                else
                    Command.Printf("  not loaded  : " + sfx.getName() + "\n");
            }
        }
        Command.Printf("Total resident: " + total + "\n");
    }

    void SoundInfo_f() {
        Command.Printf("%5d stereo\n", 1);
        Command.Printf("%5d samples\n", 22050);
        Command.Printf("%5d samplebits\n", 16);
        Command.Printf("%5d speed\n", 44100);
    }
}
