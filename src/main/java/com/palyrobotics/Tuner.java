package com.palyrobotics;

import java.awt.*;
import java.util.Arrays;

import javax.swing.*;

/**
 * @author Aditya Oberai
 *         <p>
 *         Interfaces with tuner of hsv values as well as the auto tuning to
 *         find contours
 */
public class Tuner extends JPanel {

	static Tuner sTuner = new Tuner();
	// upper and lower bounds for contour finding
	static int sHValMin = 5;
	static int sSValMin = 150;
	static int sVValMin = 153;
	static int sHValMax = 15;
	static int sSValMax = 206;
	static int sVValMax = 255;

	// represents the HSV of the pixel cursor is pointing at
	private float[] mCursorHSV;

	private static JSlider sHValMinSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, 5);
	private static JSlider sSValMinSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, 150);
	private static JSlider sVValMinSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, 153);
	private static JSlider sHValMaxSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, 15);
	private static JSlider sSValMaxSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, 206);
	private static JSlider sVValMaxSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, 255);
	private static JSlider sAreaFilterSlider = new JSlider(JSlider.HORIZONTAL, 0, 100000, 10000);
	private static JSlider sBlurAmountSlider = new JSlider(JSlider.HORIZONTAL, 10, 100, 50);

	private static JFrame mJFrame = new JFrame();
	private static JPanel mJPanel = new JPanel();

	// represents current contour finding states on JFrame
	private JTextField mMinColor = new JTextField();
	private JTextField mMaxColor = new JTextField();

	private JLabel mMinColorHSV = new JLabel();
	private JLabel mMaxColorHSV = new JLabel();
	private JLabel mCursorHSVLabel = new JLabel();

	private Robot mColorGetter; // used to find HSV at a cursor position

	{
		try {
			mColorGetter = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
	}

	// individual component of cursor HSV values used as an intermediate step
	private int mRComponent = 0;
	private int mGComponent = 0;
	private int mBComponent = 0;

	public void setSlidersToEyedropper() {
		setsHValMaxSlider();
		setsHValMinSlider();
		setsSValMaxSlider();
		setsSValMinSlider();
		setsVValMaxSlider();
		setsVValMinSlider();
	}

	public Tuner() {
	}

	public void initializeTuner() {
		mJPanel.setLayout(new FlowLayout());
		mJFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		mJFrame.setPreferredSize(new Dimension(300, 800));
		mJFrame.setFocusable(true);
		mJFrame.setLocation(new Point(1600, 0));
		mJFrame.setResizable(false);
		mJPanel.add(new Label("H Value Min"));
		mJPanel.add(sHValMinSlider);

		mJPanel.add(new Label("S Value Min"));
		mJPanel.add(sSValMinSlider);

		mJPanel.add(new Label("V Value Min"));
		mJPanel.add(sVValMinSlider);

		mMinColor.setColumns(100);
		mJPanel.add(mMinColorHSV);

		mJPanel.add(mMinColor);

		mJPanel.add(new Label("H Value Max"));
		mJPanel.add(sHValMaxSlider);

		mJPanel.add(new Label("S Value Max"));
		mJPanel.add(sSValMaxSlider);

		mJPanel.add(new Label("V Value Max"));
		mJPanel.add(sVValMaxSlider);

		mMaxColor.setColumns(100);
		mJPanel.add(mMaxColorHSV);

		mJPanel.add(mMaxColor);
		mJPanel.add(new JLabel("Blur Factor:"));
		mJPanel.add(sBlurAmountSlider);
		mJPanel.add(new JLabel("Min Area Filter:"));
		mJPanel.add(sAreaFilterSlider);

		mJPanel.add(mCursorHSVLabel);

		mJPanel.add(new JLabel("|~-------------------------------~|"));
		mJPanel.add(new JLabel(new ImageIcon("src/resources/GraingerHSVChart.jpg")));

		mJFrame.add(mJPanel);
		mJFrame.pack();
		mJFrame.setVisible(true);
	}

	// void setsHValMinSlider(){
	// sHValMinSlider.setValue();
	// }
	public void getHSVValues() {
		sHValMinSlider.addChangeListener(event -> {
			sHValMin = sHValMinSlider.getValue();
		});
		sSValMinSlider.addChangeListener(event -> {
			sSValMin = sSValMinSlider.getValue();
		});
		sVValMinSlider.addChangeListener(event -> {
			sVValMin = sVValMinSlider.getValue();
		});
		sHValMaxSlider.addChangeListener(event -> {
			sHValMax = sHValMaxSlider.getValue();
		});
		sSValMaxSlider.addChangeListener(event -> {
			sSValMax = sSValMaxSlider.getValue();
		});
		sVValMaxSlider.addChangeListener(event -> {
			sVValMax = sVValMaxSlider.getValue();
		});
		// System.out.println(new Scalar(sHValMin, ((sSValMin * 20)/51), (sVValMin *
		// 20)/51));
		mMinColor.setBackground(new Color(Color.HSBtoRGB(sHValMin, ((sSValMin * 51) / 20), (sVValMin * 51) / 20)));
		mMaxColor.setBackground(new Color(Color.HSBtoRGB(sHValMax, ((sSValMax * 51) / 20), (sVValMax * 51) / 20)));
		mMinColorHSV.setText(sHValMin + ", " + sSValMin + ", " + sVValMin);
		mMaxColorHSV.setText(sHValMax + ", " + sSValMax + ", " + sVValMax);

		mRComponent = mColorGetter
				.getPixelColor(MouseInfo.getPointerInfo().getLocation().x, MouseInfo.getPointerInfo().getLocation().y)
				.getRed();
		mGComponent = mColorGetter
				.getPixelColor(MouseInfo.getPointerInfo().getLocation().x, MouseInfo.getPointerInfo().getLocation().y)
				.getGreen();
		mBComponent = mColorGetter
				.getPixelColor(MouseInfo.getPointerInfo().getLocation().x, MouseInfo.getPointerInfo().getLocation().y)
				.getBlue();
		mCursorHSV = Color.RGBtoHSB(mRComponent, mGComponent, mBComponent, null);
		mCursorHSV[0] *= 255;
		mCursorHSV[0] = (int) mCursorHSV[0];
		mCursorHSV[1] *= 255;
		mCursorHSV[1] = (int) mCursorHSV[1];
		mCursorHSV[2] *= 255;
		mCursorHSV[2] = (int) mCursorHSV[2];
		mCursorHSVLabel.setText("EyeDropper HSV" + Arrays.toString(mCursorHSV));
		// System.out.println(Arrays.toString(mCursorHSV));
	}

	public void setsHValMinSlider() {
		if (mCursorHSV[0] > 35) {
			sHValMinSlider.setValue((int) mCursorHSV[0] - 35);
		} else {
			sHValMinSlider.setValue(0);

		}
	}

	public void setsHValMaxSlider() {
		if (mCursorHSV[1] < 220) {
			sHValMaxSlider.setValue((int) mCursorHSV[0] + 35);
		} else {
			sHValMaxSlider.setValue(255);

		}
	}

	public void setsSValMaxSlider() {
		if (mCursorHSV[2] < 205) {
			sSValMaxSlider.setValue((int) mCursorHSV[1] + 50);
		} else {
			sSValMaxSlider.setValue(255);

		}
	}

	public void setsSValMinSlider() {
		if (mCursorHSV[2] > 50) {
			sSValMinSlider.setValue((int) mCursorHSV[1] - 50);
		} else {
			sSValMinSlider.setValue(0);
		}
	}

	public void setsVValMinSlider() {
		if (mCursorHSV[2] > 195) {
			System.out.println("entered");
			sVValMinSlider.setValue((int) mCursorHSV[2] - 60);
		} else {
			sVValMinSlider.setValue(0);

		}

	}

	public void setsVValMaxSlider() {
		if (mCursorHSV[2] < 210) {
			sVValMaxSlider.setValue((int) mCursorHSV[2] + 45);
		} else {
			sVValMaxSlider.setValue(255);
		}
	}

	public static Tuner getInstance() {
		return sTuner;
	}

	public int getAreaFilter() {
		return sAreaFilterSlider.getValue();
	}

	public int getBlurFactor() {
		return sBlurAmountSlider.getValue();
	}

}
