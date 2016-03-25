/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.tomp2p.audiovideowrapper;

import com.github.sarxos.webcam.Webcam;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javax.sound.sampled.LineUnavailableException;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.RgbToYuv420j;
import org.jcodec.scale.Transform;
import org.jcodec.scale.Yuv420jToRgb;

/**
 *
 * @author draft
 */
public class H264Wrapper {
    
    final static ExecutorService exService = new ThreadPoolExecutor(1, 1,
                                    0L, TimeUnit.MILLISECONDS,
                                    new ArrayBlockingQueue<Runnable>(1), new ThreadPoolExecutor.DiscardPolicy());

    private final static int FPS = 10;

    private static int w = 0, h = 0;
    private static boolean running = true;
    private static Webcam webcam;

    public static int getW() {
        return w;
    }

    public static int getH() {
        return h;
    }

    public static VideoData decodeAndPlay(final ImageView imageView) throws LineUnavailableException {
        final Transform transform2 = new Yuv420jToRgb();
        final H264Decoder decoder = new H264Decoder(); // qp

        return new VideoData() {
            private long baseline = 0;
            private Picture target1 = null;
            private Picture rgb = null;
            private BufferedImage bi = null;

            @Override
            public void created(ByteBuffer bb, long time, int w0, int h0) {
                try {
                    w = w0;
                    h = h0;
                    if (bb.limit() == 0) {
                        return;
                    }
                    if (target1 == null && rgb == null && bi == null) {
                        target1 = Picture.create((w + 15) & ~0xf, (h + 15) & ~0xf,
                                ColorSpace.YUV420J);
                        rgb = Picture.create(w, h, ColorSpace.RGB);
                        bi = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
                    }

                    Picture dec = decoder.decodeFrame(bb, target1.getData());
                    transform2.transform(dec, rgb);
                    AWTUtil.toBufferedImage(rgb, bi);
                    WritableImage newImg = SwingFXUtils.toFXImage(bi, null);

                    //FPS stuff
                    long now = System.currentTimeMillis();
                    if (baseline == 0) {
                        baseline = now - time;
                    } else {
                        long wait = (baseline + time) - now;
                        if (wait > 0) {
                            Thread.sleep(wait);
                        }/* else {
                            System.out.println("FPS too high in decoding!" + wait);
                        }*/
                    }
                    imageView.setImage(newImg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

    }

    public static void stopRecordeAndEncode() {
        running = false;
        webcam.close();
        exService.shutdown();
    }

    public static void recordAndEncode(final Webcam webcam0, final VideoData onFrame) {
        webcam = webcam0;
        webcam.open();
        final H264Encoder encoder = new H264Encoder(); // qp
        final Transform transform = new RgbToYuv420j();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long start, wait;
                    Picture yuv = null;
                    while (running) {
                        start = System.currentTimeMillis();
                        BufferedImage rgb = webcam.getImage();
                        if (yuv == null) {
                            w = rgb.getWidth();
                            h = rgb.getHeight();
                            yuv = Picture.create(w, h, ColorSpace.YUV420);
                        }
                        transform.transform(AWTUtil.fromBufferedImage(rgb), yuv);
                        final ByteBuffer ff = encoder.encodeFrame(yuv, ByteBuffer.allocate(w * h * 3));

                        exService.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    onFrame.created(ff, System.currentTimeMillis(), w, h);
                                }
                                 catch (Exception e) {
                                     e.printStackTrace();
                                 }
                            }
                        });

                        //FPS stuff
                        wait = (start + (1000 / FPS)) - System.currentTimeMillis();
                        if (wait > 0) {
                            Thread.sleep(wait);
                        }/* else {
                            System.out.println("FPS too high in encoding!" + wait);
                        }*/
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
