/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.tomp2p.audiovideowrapper;

import com.sun.jna.Native;
import com.sun.jna.ptr.PointerByReference;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 *
 * @author Thomas Bocek
 */
public class OpusWrapper {
    
    public static AudioFormat FORMAT = new AudioFormat(8000.0f, 16, 1, true, true);

    static {
        try {
            System.loadLibrary("opus");
        } catch (UnsatisfiedLinkError e1) {
            try {
                File f = Native.extractFromResourcePath("opus");
                System.load(f.getAbsolutePath());
            } catch (Exception e2) {
                e1.printStackTrace();
                e2.printStackTrace();
            }
        }
    }

    private static TargetDataLine microphone = null;
    private static SourceDataLine speaker = null;

    private static final IntBuffer errorEncoder = IntBuffer.allocate(4);
    private static final PointerByReference opusEncoder = Opus.INSTANCE.opus_encoder_create(8000, 1,
            Opus.OPUS_APPLICATION_VOIP, errorEncoder);

    private static final IntBuffer errorDecoder = IntBuffer.allocate(4);
    private static final PointerByReference opusDecoder = Opus.INSTANCE.opus_decoder_create(8000, 1,
            errorDecoder);

    private static boolean running = false;

    @Override
    protected void finalize() throws Throwable {
        try {
            Opus.INSTANCE.opus_encoder_destroy(opusEncoder);
            Opus.INSTANCE.opus_decoder_destroy(opusDecoder);
        } finally {
            super.finalize();
        }
    }
    
    public static void stopDecodeAndPlay() {
        speaker.stop();
        speaker.close();
    }
    
    public static AudioData decodeAndPlay(AudioFormat format) throws LineUnavailableException {
        speaker = AudioSystem.getSourceDataLine(format);
        speaker.open(format);
        speaker.start();
        return new AudioData() {
            @Override
            public void created(ByteBuffer buffer) {
                ShortBuffer shortBuffer = decodeOne(buffer);
                short[] shortAudioBuffer = new short[shortBuffer.remaining()];
                shortBuffer.get(shortAudioBuffer);
                byte[] audio = ShortToByte_Twiddle_Method(shortAudioBuffer);
                speaker.write(audio, 0, audio.length);
            }
        };
    }
    
    private static ShortBuffer decodeOne(ByteBuffer dataBuffer) {
        ShortBuffer shortBuffer = ShortBuffer.allocate(80);
        byte[] transferedBytes = new byte[dataBuffer.remaining()];
        dataBuffer.get(transferedBytes);
        int decoded = Opus.INSTANCE.opus_decode(opusDecoder, transferedBytes, transferedBytes.length,
                    shortBuffer, 80, 0);
        shortBuffer.position(shortBuffer.position() + decoded);
        shortBuffer.flip();
        return shortBuffer;
    }

    private static byte[] ShortToByte_Twiddle_Method(final short[] input) {
        final int len = input.length;
        final byte[] buffer = new byte[len * 2];
        for (int i = 0; i < len; i++) {
            buffer[(i * 2) + 1] = (byte) (input[i]);
            buffer[(i * 2)] = (byte) (input[i] >> 8);
        }
        return buffer;
    }

    public static void stopRecordeAndEncode() {
        running = false;
        microphone.stop();
        microphone.close();
    }

    public static void recordAndEncode(AudioFormat format, final AudioData onFrame)
            throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("not supported");
        }

        microphone = AudioSystem.getTargetDataLine(format);
        // Obtain and open the line.
        microphone.open(format);

        // Assume that the TargetDataLine, line, has already been obtained and
        // opened.
        final byte[] data = new byte[microphone.getBufferSize()];
        // Begin audio capture.
        microphone.start();
        final ShortBuffer shortBuffer = ShortBuffer.allocate(80);
        running = true;
        // Here, stopped is a global boolean set by another thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                int numBytesRead;
                while (running) {
                    // Read the next chunk of data from the TargetDataLine.
                    numBytesRead = microphone.read(data, 0, data.length);
                    // Save this chunk of data.
                    for (int i = 0; i < numBytesRead ; i += 2) {
                        int b1 = data[i + 1] & 0xff;
                        int b2 = data[i] << 8;
                        shortBuffer.put((short) (b1 | b2));
                        if (shortBuffer.position() == 80) {
                            shortBuffer.flip();
                            ByteBuffer buffer = encodeOne(shortBuffer);
                            onFrame.created(buffer);
                            shortBuffer.clear();
                        }
                    }
                    
                }
            }
        }).start();
    }

    private static ByteBuffer encodeOne(ShortBuffer shortBuffer) {
        ByteBuffer dataBuffer = ByteBuffer.allocate(80);
        //frame size 10ms
        int read = Opus.INSTANCE.opus_encode(opusEncoder, shortBuffer, 80, dataBuffer, 80);
        dataBuffer.position(dataBuffer.position() + read);
        dataBuffer.flip();
        return dataBuffer;
    }
}
