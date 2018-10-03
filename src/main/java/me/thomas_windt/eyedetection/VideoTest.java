package me.thomas_windt.eyedetection;


import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;


import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VideoTest {

    public static void main(String[] args) {
        OpenCV.loadShared();
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        final VideoCapture capture = new VideoCapture(1);
        if (capture.isOpened()) {
            final JFrame frame = new JFrame();
            System.out.println("Opened");

            Runnable frameGrabber = new Runnable() {
                public void run() {
                    try {
                        frame.getGraphics().drawImage(grabFrame(capture),0,0,null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            frame.setVisible(true);
            ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
            timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);

        }

    }

    public static Image grabFrame(VideoCapture capture) throws IOException {

        Mat frame = new Mat();
        capture.read(frame);

        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return ImageIO.read(new ByteArrayInputStream(buffer.toArray()));

    }
}
