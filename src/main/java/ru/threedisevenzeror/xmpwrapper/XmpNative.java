package ru.threedisevenzeror.xmpwrapper;

import com.sun.jna.*;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.PointerByReference;

import java.util.Arrays;
import java.util.List;

/**
 * Created by ThreeDISevenZeroR on 27.06.16.
 */
interface XmpNative extends Library {

    String LIBRARY_NAME = "libxmp";

    XmpNative Instance = (XmpNative) Native.loadLibrary(LIBRARY_NAME, XmpNative.class);
    NativeLibrary LibraryInstance = NativeLibrary.getInstance(LIBRARY_NAME);

    int VersionCode = LibraryInstance.getGlobalVariableAddress("xmp_vercode").getInt(0);
    String VersionName = LibraryInstance.getGlobalVariableAddress("xmp_version").getPointer(0).getString(0);

    int XMP_NAME_SIZE = 64;	// Size of module name and type

    int XMP_KEY_OFF = 0x81;	// Note number for key off event
    int XMP_KEY_CUT = 0x82;	// Note number for key cut event
    int XMP_KEY_FADE = 0x83; // Note number for fade event

    // sample flags
    int XMP_SMPCTL_SKIP = (1 << 0); // Don't load samples

    // limits
    int XMP_MAX_KEYS = 121;	// Number of valid keys
    int XMP_MAX_ENV_POINTS = 32;	// Max number of envelope points
    int XMP_MAX_MOD_LENGTH = 256;	// Max number of patterns in module
    int XMP_MAX_CHANNELS = 64;	// Max number of channels in module
    int XMP_MAX_SRATE = 49170;	// max sampling rate (Hz)
    int XMP_MIN_SRATE = 4000;	// min sampling rate (Hz)
    int XMP_MIN_BPM = 20;	// min BPM
    // frame rate = (50 * bpm / 125) Hz
    // frame size = (sampling rate * channels * size) / frame rate
    int XMP_MAX_FRAMESIZE = (5 * XMP_MAX_SRATE * 2 / XMP_MIN_BPM);

    // error codes
    int XMP_END = -1;
    int XMP_ERROR_INTERNAL = -2;	// Internal error
    int XMP_ERROR_FORMAT = -3;	// Unsupported module format
    int XMP_ERROR_LOAD = -4;	// Error loading file
    int XMP_ERROR_DEPACK = -5;	// Error depacking file
    int XMP_ERROR_SYSTEM = -6;	// System error
    int XMP_ERROR_INVALID = -7;	// Invalid parameter
    int XMP_ERROR_STATE = -8;	// Invalid player state

    int XMP_PERIOD_BASE	= 6847; // C4 period

    class UnsignedInt extends IntegerType {

        public UnsignedInt() {
            this(0);
        }

        public UnsignedInt(long value) {
            super(4, true);
            setValue(value);
        }
    }

    class UnsignedChar extends IntegerType {

        public UnsignedChar() {
            this(0);
        }

        public UnsignedChar(int value) {
            super(1, true);
            setValue(value);
        }
    }

    class Channel extends Structure implements Xmp.Channel {

        public int pan; // Channel pan (0x80 is center)
        public int vol; // Channel volume
        public int flg; // Channel flags

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("pan", "vol", "flg");
        }

        @Override
        public int getPan() {
            return pan;
        }

        @Override
        public int getVolume() {
            return vol;
        }

        @Override
        public int getFlags() {
            return flg;
        }
    }

    class Pattern extends Structure {

        public int rows; // Number of rows
        public int[] index = new int[1]; // Track index

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("rows", "index");
        }
    }

    class Event extends Structure {

        public UnsignedChar note; // Note number (0 means no note)
        public UnsignedChar ins; // Patch number
        public UnsignedChar vol; // Volume (0 to basevol)
        public UnsignedChar fxt; // Effect type
        public UnsignedChar fxp; // Effect parameter
        public UnsignedChar f2t; // Secondary effect type
        public UnsignedChar f2p; // Secondary effect parameter
        public UnsignedChar _flag; // Internal (reserved) flags

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("note", "ins", "vol", "fxt", "fxp", "f2t", "f2p", "_flag");
        }
    }

    class Track extends Structure {

        public int rows; // Number of rows
        public Event[] event = new Event[1]; // Event data

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("rows", "event");
        }
    }

    class Envelope extends Structure {

        public static final int XMP_ENVELOPE_ON = (1 << 0);  // Envelope is enabled
        public static final int XMP_ENVELOPE_SUS = (1 << 1);  // Envelope has sustain point
        public static final int XMP_ENVELOPE_LOOP = (1 << 2);  // Envelope has loop
        public static final int XMP_ENVELOPE_FLT = (1 << 3);  // Envelope is used for filter
        public static final int XMP_ENVELOPE_SLOOP = (1 << 4);  // Envelope has sustain loop
        public static final int XMP_ENVELOPE_CARRY = (1 << 5);  // Don't reset envelope position

        public int flg; // Flags
        public int npt; // Number of envelope points
        public int scl; // Envelope scaling
        public int sus; // Sustain start point
        public int sue; // Sustain end point
        public int lps; // Loop start point
        public int lpe; // Loop end point
        public short[] data = new short[XMP_MAX_ENV_POINTS * 2];

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("flg", "npt", "scl", "sus", "sue", "lps", "lpe", "data");
        }
    }

    class InstrumentKey extends Structure {

        public UnsignedChar ins; // Instrument number for each key
        public byte xpo; // Instrument transpose for each key

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("ins", "xpo");
        }
    }

    class SubInstrument extends Structure {

        public static class ByReference extends SubInstrument implements Structure.ByReference {}

        public static final int XMP_INST_NNA_CUT = 0x00;
        public static final int XMP_INST_NNA_CONT = 0x01;
        public static final int XMP_INST_NNA_OFF = 0x02;
        public static final int XMP_INST_NNA_FADE = 0x03;

        public static final int XMP_INST_DCT_OFF = 0x00;
        public static final int XMP_INST_DCT_NOTE = 0x01;
        public static final int XMP_INST_DCT_SMP = 0x02;
        public static final int XMP_INST_DCT_INST = 0x03;

        public static final int XMP_INST_DCA_CUT = XMP_INST_NNA_CUT;
        public static final int XMP_INST_DCA_OFF = XMP_INST_NNA_OFF;
        public static final int XMP_INST_DCA_FADE = XMP_INST_NNA_FADE;

        public int vol; // Default volume
        public int gvl; // Global volume
        public int pan; // Pan
        public int xpo; // Transpose
        public int fin; // Finetune
        public int vwf; // Vibrato waveform
        public int vde; // Vibrato depth
        public int vra; // Vibrato rate
        public int vsw; // Vibrato sweep
        public int rvv; // Random volume/pan variation (IT)
        public int sid; // Sample number
        public int nna; // New note action
        public int dct; // Duplicate check type
        public int dca; // Duplicate check action
        public int ifc; // Initial filter cutoff
        public int ifr; // Initial filter resonance

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("vol", "gvl", "pan", "xpo", "fin", "vwf", "vde",
                    "vra", "vsw", "rvv", "sid", "nna", "dct", "dca", "ifc", "ifr");
        }
    }

    class Instrument extends Structure implements Xmp.Instrument {

        public static class ByReference extends Instrument implements Structure.ByReference { }

        public byte[] name = new byte[32]; // Instrument name
        public int vol; // Instrument volume
        public int nsm; // Number of samples
        public int rls; // Release (fadeout)
        public Envelope aei; // Amplitude envelope info
        public Envelope pei; // Pan envelope info
        public Envelope fei; // Frequency envelope info
        public InstrumentKey[] keys = new InstrumentKey[XMP_MAX_KEYS];
        public SubInstrument.ByReference sub;
        public Pointer extra; // Extra fields

        @Override
        public String getName() {
            return Native.toString(name);
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("name", "vol", "nsm", "rls", "aei", "pei", "fei", "keys", "sub", "extra");
        }
    }

    class Sample extends Structure implements Xmp.Sample {

        public static class ByReference extends Sample implements Structure.ByReference { }

        public byte[] name = new byte[32]; // Sample name
        public int len; // Sample length
        public int lps; // Loop start
        public int lpe; // Loop end
        public int flg; // Flags
        public Pointer data; // Sample data

        @Override
        public String getName() {
            return Native.toString(name);
        }

        @Override
        public int getLength() {
            return len;
        }

        @Override
        public int getLoopStart() {
            return lps;
        }

        @Override
        public int getLoopEnd() {
            return lpe;
        }

        @Override
        public int getFlags() {
            return flg;
        }

        @Override
        public byte[] getSampleData() {
            return data.getByteArray(0, len);
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("name", "len", "lps", "lpe", "flg", "data");
        }
    }

    class Sequence extends Structure implements Xmp.Sequence {

        public static class ByReference extends Sequence implements Structure.ByReference {}

        public int entry_point;
        public int duration;

        @Override
        public int getEntryPoint() {
            return entry_point;
        }

        @Override
        public int getDuration() {
            return duration;
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("entry_point", "duration");
        }
    }

    class Module extends Structure implements Xmp.Module {

        public static class ByReference extends Module implements Structure.ByReference {}

        public byte[] name = new byte[XMP_NAME_SIZE]; // Module title
        public byte[] type = new byte[XMP_NAME_SIZE]; // Module format
        public int pat; // Number of patterns
        public int trk; // Number of tracks
        public int chn; // Tracks per pattern
        public int ins; // Number of instruments
        public int smp; // Number of samples
        public int spd; // Initial speed
        public int bpm; // Initial BPM
        public int len; // Module length in patterns
        public int rst; // Restart position
        public int gvl; // Global volume
        public PointerByReference xxp; // Patterns
        public PointerByReference xxt; // Tracks
        public Instrument.ByReference xxi; // Instruments
        public Sample.ByReference xxs; // Samples
        public Channel[] xxc = new Channel[XMP_MAX_CHANNELS]; // Channel info
        public UnsignedChar[] xxo = new UnsignedChar[XMP_MAX_MOD_LENGTH]; // Orders

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("name", "type", "pat", "trk", "chn", "ins", "smp", "spd",
                    "bpm", "len", "rst", "gvl", "xxp", "xxt", "xxi", "xxs", "xxc", "xxo");
        }

        @Override
        public String getName() {
            return Native.toString(name);
        }

        @Override
        public String getType() {
            return Native.toString(type);
        }

        @Override
        public int getPatternCount() {
            return pat;
        }

        @Override
        public int getTrackCount() {
            return trk;
        }

        @Override
        public int getTracksPerPattern() {
            return chn;
        }

        @Override
        public int getNumberOfInstruments() {
            return ins;
        }

        @Override
        public int getNumberOfSamples() {
            return smp;
        }

        @Override
        public int getInitialSpeed() {
            return spd;
        }

        @Override
        public int getInitialBpm() {
            return bpm;
        }

        @Override
        public int getModuleLengthInPatterns() {
            return len;
        }

        @Override
        public int getRestartPosition() {
            return rst;
        }

        @Override
        public int getGlobalVolume() {
            return gvl;
        }

        @Override
        public Xmp.Instrument[] getInstruments() {
            Structure[] struct = xxi.toArray(ins);
            return Arrays.copyOf(struct, struct.length, Xmp.Instrument[].class);
        }

        @Override
        public Xmp.Sample[] getSamples() {
            Structure[] struct = xxs.toArray(smp);
            return Arrays.copyOf(struct, struct.length, Xmp.Sample[].class);
        }

        @Override
        public Xmp.Channel[] getChannels() {
            return xxc;
        }

        @Override
        public int[] getOrders() {
            int[] result = new int[XMP_MAX_MOD_LENGTH];
            for(int i = 0; i < XMP_MAX_MOD_LENGTH; i++) {
                result[i] = xxo[i].intValue();
            }
            return result;
        }
    }

    class TestInfo extends Structure implements Xmp.TestInfo {

        public byte[] name = new byte[XMP_NAME_SIZE]; // Module title
        public byte[] type = new byte[XMP_NAME_SIZE]; // Module format

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("name", "type");
        }

        @Override
        public String getName() {
            return Native.toString(name);
        }

        @Override
        public String getType() {
            return Native.toString(type);
        }
    }

    class ModuleInfo extends Structure implements Xmp.ModuleInfo {

        public byte[] md5 = new byte[16]; // MD5 message digest
        public int vol_base; // Volume scale
        public Module.ByReference mod; // Pointer to module data
        public String comment; // Comment text, if any
        public int num_sequences; // Number of valid sequences
        public Sequence.ByReference seq_data; // Pointer to sequence data

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("md5", "vol_base", "mod", "comment", "num_sequences", "seq_data");
        }

        @Override
        public byte[] getMd5() {
            return md5;
        }

        @Override
        public int getVolumeScale() {
            return vol_base;
        }

        @Override
        public Xmp.Module getModule() {
            return mod;
        }

        @Override
        public String getComment() {
            return comment;
        }

        @Override
        public int getSequenceCount() {
            return num_sequences;
        }

        @Override
        public Xmp.Sequence[] getSequences() {
            Structure[] seqs = seq_data.toArray(num_sequences);
            return Arrays.copyOf(seqs, seqs.length, Xmp.Sequence[].class);
        }
    }

    // Current channel information
    class ChannelInfo extends Structure implements Xmp.ChannelInfo {

        public UnsignedInt period;	// Sample period
        public UnsignedInt position; // Sample position
        public short pitchbend; // Linear bend from base note*/
        public UnsignedChar note; // Current base note number
        public UnsignedChar instrument; // Current instrument number
        public UnsignedChar sample; // Current sample number
        public UnsignedChar volume; // Current volume
        public UnsignedChar pan; // Current stereo pan
        public UnsignedChar reserved; // Reserved
        public Event event; // Current track event

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("period", "position", "pitchbend", "note",
                    "instrument", "sample", "volume", "pan", "reserved", "event");
        }
    }

    // Current frame information
    class FrameInfo extends Structure implements Xmp.FrameInfo {

        public int pos; // Current position
        public int pattern; // Current pattern
        public int row; // Current row in pattern
        public int num_rows; // Number of rows in current pattern
        public int frame; // Current frame
        public int speed; // Current replay speed
        public int bpm; // Current bpm
        public int time; // Current module time in ms
        public int total_time; // Estimated replay time in ms*/
        public int frame_time; // Frame replay time in us
        public Pointer buffer; // Pointer to sound buffer
        public int buffer_size; // Used buffer size
        public int total_size; // Total buffer size
        public int volume; // Current master volume
        public int loop_count; // Loop counter
        public int virt_channels; // Number of virtual channels
        public int virt_used; // Used virtual channels
        public int sequence; // Current sequence
        public ChannelInfo[] channel_info = new ChannelInfo[XMP_MAX_CHANNELS];

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("pos", "pattern", "row", "num_rows", "frame", "speed", "bpm", "time",
                    "total_time", "frame_time", "buffer", "buffer_size", "total_size", "volume",
                    "loop_count", "virt_channels", "virt_used", "sequence", "channel_info");
        }

        @Override
        public int getPosition() {
            return pos;
        }

        @Override
        public int getPattern() {
            return pattern;
        }

        @Override
        public int getRow() {
            return row;
        }

        @Override
        public int getRowCount() {
            return num_rows;
        }

        @Override
        public int getFrame() {
            return frame;
        }

        @Override
        public int getSpeed() {
            return speed;
        }

        @Override
        public int getBpm() {
            return bpm;
        }

        @Override
        public int getTime() {
            return time;
        }

        @Override
        public int getFrameTime() {
            return frame_time;
        }

        @Override
        public int getEstimatedTime() {
            return total_time;
        }

        @Override
        public int getVolume() {
            return volume;
        }

        @Override
        public int getLoopCount() {
            return loop_count;
        }

        @Override
        public int getSequence() {
            return sequence;
        }

        @Override
        public int getVirtualChannelsCount() {
            return virt_channels;
        }

        @Override
        public int getUsedVirtualChannelsCount() {
            return virt_used;
        }

        @Override
        public Xmp.ChannelInfo[] getChannels() {
            return channel_info;
        }

        @Override
        public byte[] getBuffer() {
            return buffer.getByteArray(0, buffer_size);
        }

        @Override
        public int getBufferSize() {
            return buffer_size;
        }

        @Override
        public int read(byte[] out, int offset, int count) {
            int toRead = Math.min(buffer_size, count);
            buffer.read(0, out, offset, count);
            return toRead;
        }
    }

    class Context extends PointerType {}

    Context xmp_create_context();

    void xmp_free_context(Context context);

    int xmp_test_module(String path, TestInfo outInfo);

    int xmp_load_module(Context context, String path);

    void xmp_scan_module(Context context);

    void xmp_release_module(Context context);

    int xmp_start_player(Context context, int rate, int format);

    int xmp_play_frame(Context context);

    int xmp_play_buffer(Context context, byte[] buffer, int size, int loopCount);

    void xmp_get_frame_info(Context context, FrameInfo outInfo);

    void xmp_end_player(Context context);

    void xmp_inject_event(Context context, int channel, Event event);

    void xmp_get_module_info(Context context, ModuleInfo outInfo);

    String[] xmp_get_format_list();

    int xmp_next_position(Context context);

    int xmp_prev_position(Context context);

    int xmp_set_position(Context context, int position);

    void xmp_stop_module(Context context);

    void xmp_restart_module(Context context);

    int xmp_seek_time(Context context, int time);

    int xmp_channel_mute(Context context, int channel, int operationFlag);

    int xmp_channel_vol(Context context, int chanel, int volume);

    int xmp_set_player(Context context, int param, int value);

    int xmp_get_player(Context context, int param);

    int xmp_set_instrument_path(Context context, String path);

    int xmp_load_module_from_memory(Context context, byte[] data, NativeLong length);

    int xmp_load_module_from_file(Context context, Pointer fileStream, NativeLong length);
}
