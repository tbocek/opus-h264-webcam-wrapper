package net.tomp2p.opuswrapper;


import com.github.sarxos.webcam.Webcam;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.RgbToYuv420j;
import org.jcodec.scale.Transform;
import org.jcodec.scale.Yuv420jToRgb;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author draft
 */
public class TestVideo extends Application {

    private final static int FPS = 10;
    private final static BlockingQueue<Pair<ByteBuffer, Long>> LIST = new ArrayBlockingQueue<>(1000);
    private final static ImageView IMG = new ImageView();
            /*"file:/home/draft/NetBeansProjects/TestVideo/hello-world.png");*/
    
    private static int w = 0, h = 0;
    private static boolean running = true;

    public static void main(String[] args) throws Exception {
        Webcam webcam = Webcam.getDefault();
        webcam.open();
        H264Encoder encoder = new H264Encoder(); // qp
        Transform transform = new RgbToYuv420j();

        //long start = System.currentTimeMillis();
        long start, wait;
        Picture yuv = null;
        //encode 4s
        for (int i = 0; i < 60; i++) {
            start = System.currentTimeMillis();
            BufferedImage rgb = webcam.getImage();
            if (yuv == null) {
                w = rgb.getWidth();
                h = rgb.getHeight();
                yuv = Picture.create(w, h, ColorSpace.YUV420);
            }
            ByteBuffer buf = ByteBuffer.allocate(w * h * 3);
            transform.transform(AWTUtil.fromBufferedImage(rgb), yuv);
            ByteBuffer ff = encoder.encodeFrame(yuv, buf);
            LIST.add(new Pair<>(ff, System.currentTimeMillis()));
            System.out.println("cam pic:" + i);
            
            //FPS stuff
            wait = (start + 1000 / FPS) - System.currentTimeMillis();
            if (wait > 0) {
                Thread.sleep(wait);
            } else {
                System.out.println("FPS too high!" + wait);
            }

        }
        
        //Now the LIST can be transfered, you need the LIST, w, h and FPS

        //decode 4s
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Transform transform2 = new Yuv420jToRgb();
                    H264Decoder decoder = new H264Decoder(); // qp
                    long baseline = 0;
                    
                    Picture target1 = Picture.create((w + 15) & ~0xf, (h + 15) & ~0xf,
                                ColorSpace.YUV420J);
                    Picture rgb = Picture.create(w, h, ColorSpace.RGB);
                    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
                    while (running) {
                        Pair<ByteBuffer, Long> p = LIST.take();
                        ByteBuffer bb = p.element0();
                        if (bb.limit() == 0) {
                            return;
                        }
                        System.out.println("decoding...");
                        Picture dec = decoder.decodeFrame(bb, target1.getData());
                        transform2.transform(dec, rgb);
                        AWTUtil.toBufferedImage(rgb, bi);
                        WritableImage newImg = SwingFXUtils.toFXImage(bi, null);      

                        //FPS stuff
                        long now = System.currentTimeMillis();
                        if (baseline == 0) {
                            baseline = now - p.element1();
                        } else {
                            long wait = (baseline + p.element1()) - now;
                            if (wait > 0) {
                                Thread.sleep(wait);
                            } else {
                                System.out.println("FPS too high!" + wait);
                            }
                        }
                        IMG.setImage(newImg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Hello World!");
        StackPane root = new StackPane();
        root.getChildren().add(IMG);
        primaryStage.setScene(new Scene(root, w, h));
        primaryStage.show();
        
    }

    @Override
    public void stop() {
        running = false;
        LIST.add(new Pair<>(ByteBuffer.wrap(new byte[0]), 0L));
    }

}
