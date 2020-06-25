package com.palyrobotics;

import javax.swing.*;

/**
 * Runs sequence of events to occur in order
 */
public class Processes extends JFrame {

	public static void main(String[] args) {
		Tuner.getInstance().initializeTuner();
		KeyListener.setupKeyListener();
		ImagePipeline.getInstance().setCaptureParameters();
		ImagePipeline.getInstance().handleCapture();
	}
}
