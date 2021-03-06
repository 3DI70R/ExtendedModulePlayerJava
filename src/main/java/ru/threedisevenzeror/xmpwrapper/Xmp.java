package ru.threedisevenzeror.xmpwrapper;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Created by ThreeDISevenZeroR on 27.06.16.
 */
public class Xmp implements Closeable {

    private static final XmpNative lib = XmpNative.Instance;

    // sample format flags
    public static final int FORMAT_8BIT = (1 << 0); // Mix to 8-bit instead of 16
    public static final int FORMAT_UNSIGNED = (1 << 1); // Mix to unsigned samples
    public static final int FORMAT_MONO = (1 << 2); // Mix to mono instead of stereo

    // dsp effect types
    public static final int DSP_LOWPASS = (1 << 0); // Lowpass filter effect
    public static final int DSP_ALL = (DSP_LOWPASS);

    // player flags
    public static final int FLAGS_VBLANK = (1 << 0); // Use vblank timing
    public static final int FLAGS_FX9BUG = (1 << 1); // Emulate FX9 bug
    public static final int FLAGS_FIXLOOP = (1 << 2); // Emulate sample loop bug

    // sample flags
    public static final int XMP_SMPCTL_SKIP = (1 << 0); // Don't load samples

    // Hidden params
    private static final int PARAM_INTERPOLATION_TYPE = 2;
    private static final int PARAM_STATE = 8;

    // Cached enum values
    private static final State[] PLAYER_STATES = State.values();
    private static final InterpolationType[] INTERPOLATION_TYPES = InterpolationType.values();

    // player state
    public enum State {
        Unloaded, // Context created
        Loaded, // Module loaded
        Playing // Module playing
    }

    // interpolation types
    public enum InterpolationType {
        Neareset, // Nearest neighbor
        Linear, // Linear (default)
        Spline, // Cubic spline
    }

    public enum Parameter {

        AmplificationFactor(0),
        StereoMixing(1),
        DspEffectFlags(3),
        Flags(4),
        CurrentModuleFlags(5),
        SampleControlFlags(6),
        Volume(7),
        SMIXVolume(9),
        DefaultPan(10);

        private int code;

        Parameter(int c) {
            this.code = c;
        }
    }

    public interface Channel {

        int FLAG_SYNTH = (1 << 0); // Channel is synthesized
        int FLAG_MUTE = (1 << 1);  // Channel is muted
        int FLAG_SPLIT = (1 << 2);  // Split Amiga channel in bits 5-4
        int FLAG_SURROUND = (1 << 4);  // Surround channel

        int getPan();
        int getVolume();
        int getFlags();
    }

    public interface TestInfo {

        String getName();
        String getType();
    }

    public interface Sample {

        int FLAG_16BIT = (1 << 0);  // 16bit sample
        int FLAG_LOOP = (1 << 1);  // Sample is looped
        int FLAG_LOOP_BIDIR = (1 << 2);  // Bidirectional sample loop
        int FLAG_LOOP_REVERSE = (1 << 3);  // Backwards sample loop
        int FLAG_LOOP_FULL = (1 << 4);  // Play full sample before looping
        int FLAG_SYNTH = (1 << 15); // Data contains synth patch

        String getName();
        int getLength();
        int getLoopStart();
        int getLoopEnd();
        int getFlags();
        byte[] getSampleData();
    }

    public interface Pattern {

    }

    public interface Event {

        int getNote();
        int getInstrument();
        int getVolume();
        int getPrimaryEffectType();
        int getPrimaryEffectParams();
        int getSecondaryEffectType();
        int getSecondaryEffectParams();
    }

    public interface Envelope {

        int XMP_ENVELOPE_ON = (1 << 0);  // Envelope is enabled
        int XMP_ENVELOPE_SUS = (1 << 1);  // Envelope has sustain point
        int XMP_ENVELOPE_LOOP = (1 << 2);  // Envelope has loop
        int XMP_ENVELOPE_FLT = (1 << 3);  // Envelope is used for filter
        int XMP_ENVELOPE_SLOOP = (1 << 4);  // Envelope has sustain loop
        int XMP_ENVELOPE_CARRY = (1 << 5);  // Don't reset envelope position

        int getFlags();
        int getEnvelopePoints();
        int getEnvelopeScaling();
        int getSustainStartPoint();
        int getSustainEndPoint();
        int getLoopStartPoint();
        int getLoopEndPoint();
        short[] getPoints();
    }

    public interface InstrumentKey {

        int getNumber();
        int getTranspose();
    }

    public interface SubInstrument {

        int getDefaultVolume();
        int getGlobalVolume();
        int getPan();
        int getTranspose();
        int getFinetune();
        int getVibratoVaveform();
        int getVibratoDepth();
        int getVibratoRate();
        int getVibratoSweep();
        int getRandomVariation();
        int getSampleNumber();
        int getNewNoteAction();
        int getDuplicateCheckType();
        int getDuplicateCheckAction();
        int getInitialFilterCutoff();
        int getInitialFilterResonance();
    }

    public interface Instrument {

        String getName();
        int getVolume();
        int getSampleCount();
        int getFadeout();
        Envelope getAmplitudeEnvelope();
        Envelope getPanEnvelope();
        Envelope getFrequencyEnvelope();
        InstrumentKey[] getKeys();
        SubInstrument getSubInstrument();
    }

    public interface Module {

        String getName();
        String getType();

        int getPatternCount();
        int getTrackCount();
        int getTracksPerPattern();
        int getNumberOfInstruments();
        int getNumberOfSamples();
        int getInitialSpeed();
        int getInitialBpm();
        int getModuleLengthInPatterns();
        int getRestartPosition();
        int getGlobalVolume();

        Channel[] getChannels();
        Instrument[] getInstruments();
        Sample[] getSamples();
        int[] getOrders();
    }

    public interface Sequence {
        int getEntryPoint();
        int getDuration();
    }

    public interface ModuleInfo {

        byte[] getMd5();
        int getVolumeScale();
        Module getModule();
        String getComment();
        int getSequenceCount();
        Sequence[] getSequences();
    }

    public interface FrameInfo {

        int getPosition();
        int getPattern();
        int getRow();
        int getRowCount();
        int getFrame();
        int getSpeed();
        int getBpm();
        int getTime();
        int getFrameTime();
        int getEstimatedTime();
        int getVolume();
        int getLoopCount();
        int getSequence();
        int getVirtualChannelsCount();
        int getUsedVirtualChannelsCount();
        ChannelInfo[] getChannels();

        byte[] getBuffer();
        int getBufferSize();
        int read(byte[] buffer, int offset, int count);
    }

    public interface ChannelInfo {

        int getPeriod();
        int getPosition();
        int getPitchBend();
        int getNote();
        int getInstrumentIndex();
        int getSampleIndex();
        int getVolume();
        int getPan();
        Event getEvent();
    }

    private XmpNative.Context context;
    private XmpNative.ModuleInfo sharedModuleInfo = new XmpNative.ModuleInfo();
    private XmpNative.FrameInfo sharedFrame = new XmpNative.FrameInfo();

    public Xmp() {
        context = lib.xmp_create_context();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override
    public void close() {
        if(context != null) {
            endPlayer();
            releaseModule();
            lib.xmp_free_context(context);
            context = null;
        }
    }

    public void loadModule(String path) {
        checkError(lib.xmp_load_module(context, path));
    }

    public void loadModule(byte[] module) {
        checkError(lib.xmp_load_module_from_memory(context, module, new NativeLong(module.length)));
    }

    public void loadModule(InputStream stream) throws IOException {
        byte[] buffer = new byte[16384];
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int readed;
        while ((readed = stream.read(buffer)) > 0) {
            result.write(buffer, 0, readed);
        }
        loadModule(result.toByteArray());
    }

    public void setInstrumentPath(String path) {
        checkError(lib.xmp_set_instrument_path(context, path));
    }

    public void scanModule() {
        lib.xmp_scan_module(context);
    }

    public ModuleInfo getModuleInfo() {
        lib.xmp_get_module_info(context, sharedModuleInfo);
        return sharedModuleInfo;
    }

    public void startPlayer(int sampleRate) {
        startPlayer(sampleRate, 0);
    }

    public void startPlayer(int sampleRate, int format) {
        checkError(lib.xmp_start_player(context, sampleRate, format));
    }

    public void stopModule() {
        lib.xmp_stop_module(context);
    }

    public void restartModule() {
        lib.xmp_restart_module(context);
    }

    public void muteChannel(int channel, boolean mute) {
        checkError(lib.xmp_channel_mute(context, channel, mute ? 1 : 0));
    }

    public boolean isChannelMuted(int channel) {
        return checkError(lib.xmp_channel_mute(context, channel, -1)) == 1;
    }

    public void setChannelVolume(int channel, int volume) {
        checkError(lib.xmp_channel_vol(context, channel, volume));
    }

    public int getChannelVolume(int channel) {
        return checkError(lib.xmp_channel_vol(context, channel, -1));
    }

    public boolean playFrame() {
        int status = lib.xmp_play_frame(context);

        if(status >= 0) {
            return true;
        } else if(status == XmpNative.XMP_END) {
            return false;
        }

        checkError(status);
        return false;
    }

    public FrameInfo getCurrentFrame() {
        lib.xmp_get_frame_info(context, sharedFrame);
        return sharedFrame;
    }

    public boolean playBuffer(byte[] buffer, int loopCount) {
        int status = lib.xmp_play_buffer(context, buffer, buffer.length, loopCount);

        if(status >= 0) {
            return true;
        } else if(status == XmpNative.XMP_END) {
            return false;
        }

        checkError(status);
        return false;
    }

    public int nextPosition() {
        return checkError(lib.xmp_next_position(context));
    }

    public int prevPosition() {
        return checkError(lib.xmp_prev_position(context));
    }

    public int setPosition(int position) {
        return checkError(lib.xmp_set_position(context, position));
    }

    public int seekTime(long value, TimeUnit unit) {
        return seekTime((int) unit.toMillis(value));
    }

    public int seekTime(int miliseconds) {
        return checkError(lib.xmp_seek_time(context, miliseconds));
    }

    public void endPlayer() {
        lib.xmp_end_player(context);
    }

    public void releaseModule() {
        lib.xmp_release_module(context);
    }

    public void setParam(Parameter param, int value) {
        setParam(param.code, value);
    }

    public int getParam(Parameter param) {
        return getParam(param.code);
    }

    public void injectEvent(int channel, Event event) {
        injectEvent(channel, event.getNote(), event.getInstrument(), event.getVolume(),
                event.getPrimaryEffectType(), event.getPrimaryEffectParams(),
                event.getSecondaryEffectType(), event.getSecondaryEffectParams());
    }

    public void injectEvent(int channel, int note, int instrument, int volume,
                            int primaryEffect,  int primaryEffectParams,
                            int secondaryEffect, int secondaryEffectParams) {
        XmpNative.Event event = new XmpNative.Event();
        event.note = new XmpNative.UnsignedChar(note);
        event.ins = new XmpNative.UnsignedChar(instrument);
        event.vol = new XmpNative.UnsignedChar(volume);
        event.fxt = new XmpNative.UnsignedChar(primaryEffect);
        event.fxp = new XmpNative.UnsignedChar(primaryEffectParams);
        event.f2t = new XmpNative.UnsignedChar(secondaryEffect);
        event.f2p = new XmpNative.UnsignedChar(secondaryEffectParams);
        event._flag = new XmpNative.UnsignedChar(0);
        lib.xmp_inject_event(context, channel, event);
    }

    private void setParam(int param, int value) {
        checkError(lib.xmp_set_player(context, param, value));
    }

    private int getParam(int param) {
        return checkError(lib.xmp_get_player(context, param));
    }

    public void setInterpolationType(InterpolationType type) {
        setParam(PARAM_INTERPOLATION_TYPE, type.ordinal());
    }

    public InterpolationType getInterpolationType() {
        return INTERPOLATION_TYPES[getParam(PARAM_INTERPOLATION_TYPE)];
    }

    public State getPlayerState() {
        return PLAYER_STATES[getParam(PARAM_STATE)];
    }

    // Static functions

    public static TestInfo testModule(String path) {
        XmpNative.TestInfo info = new XmpNative.TestInfo();
        checkError(lib.xmp_test_module(path, info));
        return info;
    }

    public static String[] getFormatList() {
        return lib.xmp_get_format_list();
    }

    public static String getLibraryVersion() {
        return XmpNative.VersionName;
    }

    public static int getLibraryVersionCode() {
        return XmpNative.VersionCode;
    }

    private static int checkError(int code) {

        String message = null;

        if(code >= 0) {
            return code;
        }

        switch (code) {
            case XmpNative.XMP_ERROR_SYSTEM: {
                throw new LastErrorException(Native.getLastError());
            }
            case XmpNative.XMP_END: message = "End"; break;
            case XmpNative.XMP_ERROR_INTERNAL: message = "Internal error"; break;
            case XmpNative.XMP_ERROR_FORMAT: message = "Unsupported module format"; break;
            case XmpNative.XMP_ERROR_DEPACK: message = "Error depacking file"; break;
            case XmpNative.XMP_ERROR_LOAD: message = "Error loading file"; break;
            case XmpNative.XMP_ERROR_INVALID:  message = "Invalid parameter"; break;
            case XmpNative.XMP_ERROR_STATE: message = "Invalid player state"; break;
        }

        if(message == null) {
            message = "Unknown error: " + code;
        }

        throw new Error(message);
    }
}
