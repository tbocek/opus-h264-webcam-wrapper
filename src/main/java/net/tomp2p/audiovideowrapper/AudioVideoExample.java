package net.tomp2p.audiovideowrapper;


import com.github.sarxos.webcam.Webcam;
import java.awt.Dimension;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author draft
 */
public class AudioVideoExample extends Application {

    private final static ImageView IMG = new ImageView();
    
    public static void main(String[] args) throws Exception {
        Webcam webcam = Webcam.getDefault();
        Dimension[] d = webcam.getViewSizes();
        webcam.setViewSize(d[d.length-1]);
        
        AudioData frameAudio = OpusWrapper.decodeAndPlay(OpusWrapper.FORMAT);
        OpusWrapper.recordAndEncode(OpusWrapper.FORMAT, frameAudio);
        
        VideoData frameVideo = H264Wrapper.decodeAndPlay(IMG);
        H264Wrapper.recordAndEncode(webcam, frameVideo);
        
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Audio Video Test");
        StackPane root = new StackPane();
        root.getChildren().add(IMG);
        while(H264Wrapper.getH() == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
               ex.printStackTrace();
            }
        }
        primaryStage.setScene(new Scene(root, H264Wrapper.getW(), H264Wrapper.getH()));
        primaryStage.show();
        
    }

    @Override
    public void stop() {
        OpusWrapper.stopDecodeAndPlay();
        OpusWrapper.stopRecordeAndEncode();
        H264Wrapper.stopRecordeAndEncode();
    }

}
