package com.palyrobotics;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

public class KeyListener implements NativeKeyListener {

	public KeyListener() {
	}

	@Override
	public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {

		if (nativeKeyEvent.getKeyChar() == 's') { // s for select
			Tuner.getInstance().setSlidersToEyedropper();
		}
	}

	@Override
	public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
	}

	@Override
	public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {
	}

	public static void setupKeyListener() {
		try {
			GlobalScreen.registerNativeHook();
		} catch (NativeHookException ex) {
			System.err.println("There was a problem registering the native hook.");
			System.err.println(ex.getMessage());

			System.exit(1);
		}

		// Construct the example object.
		KeyListener keyListener = new KeyListener();

		// Add the appropriate listeners.
		GlobalScreen.addNativeKeyListener(keyListener);

	}
}