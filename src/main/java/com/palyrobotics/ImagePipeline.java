package com.palyrobotics;

import static org.opencv.imgproc.Imgproc.FONT_HERSHEY_PLAIN;

import java.awt.*;
import java.util.ArrayList;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import com.palyrobotics.config.Configs;
import com.palyrobotics.config.VisionConfig;

/**
 * Pipeline to detect and communicate various frc objects
 * <p>
 *
 * @author Quintin Dwight - Video capture
 * @author Aditya Oberai - Cargo detection with opencv
 */
public class ImagePipeline {

	static {
		// The OpenCV jar just contains a wrapper that allows us to interface with the
		// implementation of OpenCV written in C++
		// So, we have to load those C++ libraries explicitly and linked them properly.
		// Just having the jars is not sufficient, OpenCV must be installed into the
		// filesystem manually.
		// I prefer to build it from source using CMake
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

	}

	private static final ImagePipeline sImagePipeline = new ImagePipeline();
	private final VisionConfig mVisionConfig = Configs.get(VisionConfig.class);

	private final long IDLE_SLEEP_MS = 200L;
	private static final MatOfByte mStreamMat = new MatOfByte();
	private final VideoCapture mVideoCapture = new VideoCapture(0);
	private ArrayList<MatOfPoint> mContoursCandidates = new ArrayList<>();
	private Mat mCaptureMatHSV = new Mat(); // main frame which is transferred to user that has
	private Mat mFrameHSV = new Mat(); // intermediate frame used in object detection in which image is cleaned up and
										// objects are found
	private Mat mUnprocessedStream = new Mat();
	private Moments mContourCoor = new Moments();
	private Point centroidPoint = new Point();
	private int largestContourIndex = -1;

	private float[] radiusOfContour = new float[1];
	private MatOfPoint2f contourToFloat = new MatOfPoint2f();

	Robot robot;

	{
		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	private ArrayList<Moments> mContourPointGetter = new ArrayList<>();

	/**
	 * These are the min and max values upon which the contours are found. The
	 * algorithm masks all pixels that are not between those range HSV values
	 */
	// private final Scalar kOrangeMin = new Scalar(5, 150, 153);
	// private final Scalar kOrangeMax = new Scalar(15, 206, 255);
	// private final Scalar kGreenMin = new Scalar(142, 255, 255); // TODO tune to
	// reflective green tape
	// private final Scalar kGreenMax = new Scalar(87, 255, 84);

	/**
	 * Used for marking up objects within image. Only for visual interface
	 */
	private final Scalar kBlack = new Scalar(0, 0, 0); // colors used to point out objects within live video feed
	private final Scalar kWhite = new Scalar(256, 256, 256);
	private final Scalar kRed = new Scalar(0, 0, 256);
	private final Scalar kPink = new Scalar(100, 100, 256);
	private long mFps = 0;

	private void drawBallContours() {
		preprocessImage();
		findContours();
		if (contourExists()) {
			findLargestContour();
			if (validContour(largestContourIndex)) {
				getCentroid();
				drawData();
			}
			reset();
		}
	}

	void handleCapture() {

		while (mVideoCapture.isOpened()) {

			long initialTime = System.currentTimeMillis();

			Tuner.getInstance().getHSVValues();
			boolean shouldCapture = mVisionConfig.showImage;

			if (shouldCapture) {
				readFrame();
			} else {
				try {
					Thread.sleep(IDLE_SLEEP_MS);
				} catch (InterruptedException sleepException) {
					Thread.currentThread().interrupt();
					break;
				}
			}
			mFps = 1000 / (System.currentTimeMillis() - initialTime);
		}
		HighGui.destroyAllWindows();
		mVideoCapture.release();
	}

	private void getCentroid() {
		mContourPointGetter.add(0, Imgproc.moments(mContoursCandidates.get(largestContourIndex), false));
		mContourCoor = mContourPointGetter.get(0);
		centroidPoint.x = (int) (mContourCoor.get_m10() / mContourCoor.get_m00());
		centroidPoint.y = (int) (mContourCoor.get_m01() / mContourCoor.get_m00());
	}

	public void findContours() {
		final Scalar lowerBoundHSV = new Scalar(Tuner.sHValMin, Tuner.sSValMin, Tuner.sVValMin);
		final Scalar upperBoundHSV = new Scalar(Tuner.sHValMax, Tuner.sSValMax, Tuner.sVValMax);
		Core.inRange(mFrameHSV, lowerBoundHSV, upperBoundHSV, mFrameHSV); // masks image to only allow orange objects
		Imgproc.findContours(mFrameHSV, mContoursCandidates, new Mat(), Imgproc.RETR_EXTERNAL,
				Imgproc.CHAIN_APPROX_SIMPLE); // Takes the top level contour in image
	}

	private void findLargestContour() {
		for (int i = 0; i < mContoursCandidates.size(); i++) {
			if (largestContourIndex == -1) {
				largestContourIndex = i;
			} else if (Imgproc.contourArea(mContoursCandidates.get(i)) > Imgproc
					.contourArea(mContoursCandidates.get(largestContourIndex))) {
				largestContourIndex = i;
			}
		}
	}

	Boolean contourExists() {
		if (mContoursCandidates.size() > 0) {
			return true;
		}
		return false;
	}

	Boolean validContour(int index) {
		return Imgproc.contourArea(mContoursCandidates.get(index)) > Tuner.getInstance().getAreaFilter();
	}

	private void drawData() {
		Imgproc.drawContours(mCaptureMatHSV, mContoursCandidates, largestContourIndex, kWhite, 10);

		mContoursCandidates.get(largestContourIndex).convertTo(contourToFloat, CvType.CV_32F);
		Imgproc.minEnclosingCircle(contourToFloat, centroidPoint, radiusOfContour); // Takes contour and extrapolates
																					// circle

		// Imgproc.circle(mCaptureMatHSV, centroidPoint, (int) radiusOfContour[0],
		// kPink, 10); // draws black circle at contour centroid
		// Imgproc.line(mCaptureMatHSV, new Point(mVisionData.centroidPoint.x, 0),
		// new Point(mVisionData.centroidPoint.x, mCaptureMatHSV.rows()), kBlack, 5);
		// Imgproc.line(mCaptureMatHSV, new Point(mCaptureMatHSV.cols() / 2, 0),
		// new Point(mCaptureMatHSV.cols() / 2, mCaptureMatHSV.rows()), kRed, 5); //
		// draws center line
	}

	private boolean readFrame() {
		if (mVideoCapture.read(mCaptureMatHSV)) {
			mUnprocessedStream = mCaptureMatHSV.clone(); // image is cloned before mCaptureHSV becomes processed and
			if (mVisionConfig.showImage) {
				drawBallContours(); // edited with visual contours
				displayFPS();
				HighGui.imshow("Vision", mCaptureMatHSV);
				HighGui.waitKey(1);
			}
			return Imgcodecs.imencode(".jpg", mFrameHSV, mStreamMat, new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 40));
		} else {
			System.err.println("Opened camera, but could not read from it.");
			return false;
		}
	}

	private void preprocessImage() {
		mFrameHSV = mCaptureMatHSV.clone();
		Imgproc.blur(mFrameHSV, mFrameHSV,
				new Size(Tuner.getInstance().getBlurFactor(), Tuner.getInstance().getBlurFactor())); // Blur to remove
																										// noise from
																										// image
		Imgproc.cvtColor(mFrameHSV, mFrameHSV, Imgproc.COLOR_BGR2HSV);
	}

	void setCaptureParameters() {
//		mVideoCapture.set(Videoio.CAP_PROP_FOURCC, VideoWriter.fourcc('H', '2', '6', '4'));
		mVideoCapture.set(Videoio.CAP_PROP_AUTO_EXPOSURE, 0.25); // setting exposure to manual set mode
		mVideoCapture.set(Videoio.CAP_PROP_FRAME_WIDTH, mVisionConfig.captureWidth);
		mVideoCapture.set(Videoio.CAP_PROP_FRAME_HEIGHT, mVisionConfig.captureHeight);
		mVideoCapture.set(Videoio.CAP_PROP_FPS, mVisionConfig.captureFps);
	}

	private void displayFPS() {
		Imgproc.putText(mCaptureMatHSV, "FPS: " + mFps, new Point(mCaptureMatHSV.cols() - 200, 75), FONT_HERSHEY_PLAIN,
				3, kWhite);
	}

	private void reset() {
		mContourPointGetter.clear();
		mContoursCandidates.clear();
		largestContourIndex = -1;
	}

	static ImagePipeline getInstance() {
		return sImagePipeline;
	}

	MatOfByte getStreamMat() {
		return mStreamMat;
	}

}