package me.thomas_windt.eyedetection;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.*;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The controller for our application, where the application logic is
 * implemented. It handles the button for starting/stopping the camera and the
 * acquired video stream.
 *
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @author <a href="http://max-z.de">Maximilian Zuleger</a> (minor fixes)
 * @version 2.0 (2016-09-17)
 * @since 1.0 (2013-10-20)
 */
public class FXHelloCVController {
    // the id of the camera to be used
    private static int cameraId = 0;
    int absoluteFaceSize = 0;
    // the FXML button
    @FXML
    private Button button;
    // the FXML image view
    @FXML
    private ImageView currentFrame;

    @FXML
    private Spinner spinner;
    @FXML
    private Slider slider1;
    @FXML
    private Slider slider2;
    // a timer for acquiring the video stream
    private ScheduledExecutorService timer;
    // the OpenCV object that realizes the video capture
    private VideoCapture capture = new VideoCapture();
    private CascadeClassifier faceCascade = new CascadeClassifier();
    private CascadeClassifier eyeCascade = new CascadeClassifier();
    private FeatureDetector blobDetector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
    // a flag to change the button behavior
    private boolean cameraActive = false;

    /**
     * The action triggered by pushing the button on the GUI
     *
     * @param event the push button event
     */
    @FXML
    protected void startCamera(ActionEvent event) {
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5));
        faceCascade.load("D:\\UltimateProjects\\EyeDetection\\src\\main\\resources\\haarcascade_frontalface_alt.xml");
        eyeCascade.load("D:\\UltimateProjects\\EyeDetection\\src\\main\\resources\\haarcascade_eye.xml");
        System.out.println(faceCascade.empty());
        if (!this.cameraActive) {
            // start the video capture
            this.capture.open(cameraId);

            // is the video stream available?
            if (this.capture.isOpened()) {
                this.cameraActive = true;

                // grab a frame every 33 ms (30 frames/sec)
                Runnable frameGrabber = new Runnable() {

                    @Override
                    public void run() {
                        // effectively grab and process a single frame
                        Mat frame = grabFrame();
                        // convert and show the frame
                        Image imageToShow = Utils.mat2Image(frame);
                        updateImageView(currentFrame, imageToShow);
                    }
                };

                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 500, TimeUnit.MILLISECONDS);

                // update the button content
                this.button.setText("Stop Camera");
            } else {
                // log the error
                System.err.println("Impossible to open the camera connection...");
            }
        } else {
            // the camera is not active at this point
            this.cameraActive = false;
            // update again the button content
            this.button.setText("Start Camera");

            // stop the timer
            this.stopAcquisition();
        }
    }

    /**
     * Get a frame from the opened video stream (if any)
     *
     * @return the {@link Mat} to show
     */
    private Mat grabFrame() {
        // init everything
        Mat frame = new Mat();

        // check if the capture is open
        if (this.capture.isOpened()) {
            try {
                // read the current frame
                this.capture.read(frame);

                // if the frame is not empty, process it
                if (!frame.empty()) {
                    MatOfRect faces = new MatOfRect();
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
                    Imgproc.equalizeHist(frame, frame);

                    if (absoluteFaceSize == 0) {
                        int height = frame.rows();
                        if (Math.round(height * 0.2f) > 0) {
                            absoluteFaceSize = Math.round(height * 0.2f);
                        }
                    }


                    this.faceCascade.detectMultiScale(frame, faces, 1.1, 2, Objdetect.CASCADE_SCALE_IMAGE, new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());
                    Rect[] facesArray = faces.toArray();

                    for (int i = 0; i < facesArray.length; ) {
                        Mat sliced = new Mat(frame, facesArray[i]);

                        MatOfRect eyes = new MatOfRect();
                        this.eyeCascade.detectMultiScale(sliced, eyes);
                        Rect[] eyeArray = eyes.toArray();
                        for (int j = 0; j < eyeArray.length; j++) {
                            Imgproc.threshold(sliced, sliced, slider1.getValue(), slider2.getValue(), (Integer) spinner.getValue());
                            Imgproc.rectangle(sliced, eyeArray[j].tl(), eyeArray[j].br(), new Scalar(0, 0, 0), 2);
                            // Imgproc.resize(sliced,resized,size);

                            Mat eye = new Mat(sliced, eyeArray[j]);
                            MatOfKeyPoint points = new MatOfKeyPoint();
                            blobDetector.detect(eye, points);

                            for (KeyPoint p : points.toList()) {
                                Imgproc.drawMarker(eye, p.pt, new Scalar(255, 0, 0));
                            }
                            return eye;
                        }
                        //Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);
                    }
                }

            } catch (Exception e) {
                // log the error
                System.err.println("Exception during the image elaboration: " + e);
            }
        }

        return frame;
    }

    /**
     * Stop the acquisition from the camera and release all the resources
     */
    private void stopAcquisition() {
        if (this.timer != null && !this.timer.isShutdown()) {
            try {
                // stop the timer
                this.timer.shutdown();
                this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // log any exception
                System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
            }
        }

        if (this.capture.isOpened()) {
            // release the camera
            this.capture.release();
        }
    }

    /**
     * Update the {@link ImageView} in the JavaFX main thread
     *
     * @param view  the {@link ImageView} to update
     * @param image the {@link Image} to show
     */
    private void updateImageView(ImageView view, Image image) {
        Utils.onFXThread(view.imageProperty(), image);
    }

    /**
     * On application close, stop the acquisition from the camera
     */
    protected void setClosed() {
        this.stopAcquisition();
    }

}