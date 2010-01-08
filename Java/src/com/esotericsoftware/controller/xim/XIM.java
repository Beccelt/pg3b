
package com.esotericsoftware.controller.xim;

import static com.esotericsoftware.minlog.Log.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.HashMap;

import com.esotericsoftware.controller.device.Axis;
import com.esotericsoftware.controller.device.Button;
import com.esotericsoftware.controller.device.Device;
import com.esotericsoftware.controller.util.WindowsRegistry;

/**
 * Controls the XIM2 hardware.
 */
public class XIM extends Device {
	static boolean loaded;
	static {
		load();
	}

	static void load () {
		if (loaded) return;
		loaded = true;
		String ximPath = WindowsRegistry.get("HKCU/Software/XIM", "");
		if (ximPath != null) {
			try {
				System.load(ximPath + "/SiUSBXp.dll");
				System.load(ximPath + "/XIMCore.dll");
				System.loadLibrary("xim32");
			} catch (Throwable ex) {
				if (ERROR) error("Error loading XIM native libraries.", ex);
			}
		} else {
			if (ERROR) {
				error("XIM installation path not found in registry at: HKCU/Software/XIM\n"
					+ "Please ensure the XIM360 software is installed.");
			}
		}
	}

	static private HashMap<Integer, String> statusToMessage = new HashMap();
	static {
		statusToMessage.put(0, "OK");
		statusToMessage.put(101, "INVALID_INPUT_REFERENCE");
		statusToMessage.put(102, "INVALID_MODE");
		statusToMessage.put(103, "INVALID_STICK_VALUE");
		statusToMessage.put(104, "INVALID_TRIGGER_VALUE");
		statusToMessage.put(105, "INVALID_TIMEOUT_VALUE");
		statusToMessage.put(107, "INVALID_BUFFER");
		statusToMessage.put(108, "INVALID_DEADZONE_TYPE");
		statusToMessage.put(109, "HARDWARE_ALREADY_CONNECTED");
		statusToMessage.put(109, "HARDWARE_NOT_CONNECTED");
		statusToMessage.put(401, "DEVICE_NOT_FOUND");
		statusToMessage.put(402, "DEVICE_CONNECTION_FAILED");
		statusToMessage.put(403, "CONFIGURATION_FAILED");
		statusToMessage.put(404, "READ_FAILED");
		statusToMessage.put(405, "WRITE_FAILED");
		statusToMessage.put(406, "TRANSFER_CORRUPTION");
		statusToMessage.put(407, "NEEDS_CALIBRATION");
	}

	static private int[] buttonToIndex = new int[Button.values().length];
	static {
		buttonToIndex[Button.rightShoulder.ordinal()] = 0;
		buttonToIndex[Button.rightStick.ordinal()] = 1;
		buttonToIndex[Button.leftShoulder.ordinal()] = 2;
		buttonToIndex[Button.leftStick.ordinal()] = 3;
		buttonToIndex[Button.a.ordinal()] = 4;
		buttonToIndex[Button.b.ordinal()] = 5;
		buttonToIndex[Button.x.ordinal()] = 6;
		buttonToIndex[Button.y.ordinal()] = 7;
		buttonToIndex[Button.up.ordinal()] = 8;
		buttonToIndex[Button.down.ordinal()] = 9;
		buttonToIndex[Button.left.ordinal()] = 10;
		buttonToIndex[Button.right.ordinal()] = 11;
		buttonToIndex[Button.start.ordinal()] = 12;
		buttonToIndex[Button.back.ordinal()] = 13;
		buttonToIndex[Button.guide.ordinal()] = 14;
	}

	static private int[] axisToIndex = new int[Axis.values().length];
	static {
		axisToIndex[Axis.rightStickX.ordinal()] = 0;
		axisToIndex[Axis.rightStickY.ordinal()] = 1;
		axisToIndex[Axis.leftStickX.ordinal()] = 2;
		axisToIndex[Axis.leftStickY.ordinal()] = 3;
		axisToIndex[Axis.rightTrigger.ordinal()] = 4;
		axisToIndex[Axis.leftTrigger.ordinal()] = 5;
	}

	private final ByteBuffer stateByteBuffer;
	private final ShortBuffer buttonStateBuffer, axisStateBuffer;

	public XIM () throws IOException {
		checkResult(connect());

		stateByteBuffer = ByteBuffer.allocateDirect(28);
		stateByteBuffer.order(ByteOrder.nativeOrder());
		buttonStateBuffer = stateByteBuffer.asShortBuffer();
		stateByteBuffer.position(16);
		axisStateBuffer = stateByteBuffer.slice().order(ByteOrder.nativeOrder()).asShortBuffer();
	}

	public void close () {
		disconnect();
	}

	public void setButton (Button button, boolean pressed) throws IOException {
		int value = pressed ? 1 : 0;
		int index = buttonToIndex[button.ordinal()];
		synchronized (this) {
			// Button states are stored as bytes packed into shorts.
			short existingValue = buttonStateBuffer.get(index / 2);
			int first, second;
			if (index % 2 == 0) {
				first = value & 0xFF;
				second = existingValue >> 8;
			} else {
				first = existingValue & 0xFF;
				second = value & 0xFF;
			}
			buttonStateBuffer.put(index / 2, (short)(first + (second << 8)));
			if (collectingChangesThread != Thread.currentThread()) checkResult(setState(stateByteBuffer, 200));
		}
	}

	public void setAxis (Axis axis, float state) throws IOException {
		int index = axisToIndex[axis.ordinal()];
		synchronized (this) {
			axisStateBuffer.put(index, (short)(32767 * state));
			if (collectingChangesThread != Thread.currentThread()) checkResult(setState(stateByteBuffer, 200));
		}
	}

	/**
	 * If true, the thumbsticks can be used while the XIM is running.
	 * @throws IOException When communication with the XIM fails.
	 */
	public void setThumsticksEnabled (boolean enabled) throws IOException {
		checkResult(setMode(enabled ? 1 : 0));
	}

	private void checkResult (int status) throws IOException {
		if (status == 0) return;
		throw new IOException("Error communicating with XIM: " + statusToMessage.get(status));
	}

	public String toString () {
		return "XIM";
	}

	static private native int connect ();

	static private native void disconnect ();

	static private native int setMode (int mode);

	static private native int setState (ByteBuffer byteBuffer, float timeout);
}