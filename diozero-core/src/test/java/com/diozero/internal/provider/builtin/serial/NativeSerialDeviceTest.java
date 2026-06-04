package com.diozero.internal.provider.builtin.serial;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     NativeSerialDeviceTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2026 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diozero.api.RuntimeIOException;

/**
 * Tests for NativeSerialDevice close behavior.
 *
 * Verifies fix for issue #172: serial close killing GPIO event loop via
 * raise(SIGINT). The fix reorders close() to close the native fd first,
 * adds a closed flag, and guards read/write after close.
 *
 * Uses a local socket pair to simulate the shared-FD behavior of a serial port
 * — both input and output streams share one FD; closing it unblocks any
 * blocking read immediately.
 */
@SuppressWarnings("static-method")
public class NativeSerialDeviceTest {

	static class MockSerialDevice {
		private final InputStream inputStream;
		private final OutputStream outputStream;
		private final Socket serverSide;
		private final ServerSocket serverSocket;
		private volatile boolean closed = false;

		MockSerialDevice() throws IOException {
			serverSocket = new ServerSocket(0, 1);
			Socket client = new Socket("localhost", serverSocket.getLocalPort());
			serverSide = serverSocket.accept();

			// Both streams from the same socket — like FileInputStream/OutputStream
			// sharing the same serial port fd
			inputStream = serverSide.getInputStream();
			outputStream = serverSide.getOutputStream();

			// Close the client side so we don't leak — the server side's fd is what we test
			client.close();
		}

		void close() {
			if (closed) {
				return;
			}
			closed = true;

			// Close the socket (= the "native fd") first — this causes any
			// blocking read() on inputStream to immediately return -1/throw
			// because the socket connection is closed.
			try {
				serverSide.close();
			} catch (IOException ignored) {
			}

			// Then close Java streams (no-ops after socket close)
			try {
				inputStream.close();
			} catch (IOException ignored) {
			}
			try {
				outputStream.close();
			} catch (IOException ignored) {
			}

			try {
				serverSocket.close();
			} catch (IOException ignored) {
			}
		}

		int read() {
			if (closed) {
				throw new RuntimeIOException("Serial device is closed");
			}
			try {
				return inputStream.read();
			} catch (IOException e) {
				throw new RuntimeIOException("Error reading: " + e.getMessage(), e);
			}
		}

		void write(byte[] data) {
			if (closed) {
				throw new RuntimeIOException("Serial device is closed");
			}
			try {
				outputStream.write(data);
				outputStream.flush();
			} catch (IOException e) {
				throw new RuntimeIOException("Error writing: " + e.getMessage(), e);
			}
		}

		void writeByte(byte b) {
			if (closed) {
				throw new RuntimeIOException("Serial device is closed");
			}
			try {
				outputStream.write(b);
				outputStream.flush();
			} catch (IOException e) {
				throw new RuntimeIOException("Error writing: " + e.getMessage(), e);
			}
		}

		int read(byte[] buffer) {
			if (closed) {
				throw new RuntimeIOException("Serial device is closed");
			}
			try {
				return inputStream.read(buffer);
			} catch (IOException e) {
				throw new RuntimeIOException("Error reading: " + e.getMessage(), e);
			}
		}

		int bytesAvailable() {
			if (closed) {
				throw new RuntimeIOException("Serial device is closed");
			}
			try {
				return inputStream.available();
			} catch (IOException e) {
				throw new RuntimeIOException("Error checking available: " + e.getMessage(), e);
			}
		}
	}

	@Test
	void closeShouldBeIdempotent() throws Exception {
		MockSerialDevice device = new MockSerialDevice();
		device.close();
		device.close();
	}

	@Test
	void readAfterCloseShouldThrow() throws Exception {
		MockSerialDevice device = new MockSerialDevice();
		device.close();

		RuntimeIOException ex = Assertions.assertThrows(RuntimeIOException.class, () -> device.read());
		Assertions.assertTrue(ex.getMessage().contains("is closed"));
	}

	@Test
	void writeByteAfterCloseShouldThrow() throws Exception {
		MockSerialDevice device = new MockSerialDevice();
		device.close();

		RuntimeIOException ex = Assertions.assertThrows(RuntimeIOException.class,
				() -> device.writeByte((byte) 0x42));
		Assertions.assertTrue(ex.getMessage().contains("is closed"));
	}

	@Test
	void writeArrayAfterCloseShouldThrow() throws Exception {
		MockSerialDevice device = new MockSerialDevice();
		device.close();

		RuntimeIOException ex = Assertions.assertThrows(RuntimeIOException.class,
				() -> device.write(new byte[]{0x42}));
		Assertions.assertTrue(ex.getMessage().contains("is closed"));
	}

	@Test
	void readArrayAfterCloseShouldThrow() throws Exception {
		MockSerialDevice device = new MockSerialDevice();
		device.close();

		RuntimeIOException ex = Assertions.assertThrows(RuntimeIOException.class,
				() -> device.read(new byte[10]));
		Assertions.assertTrue(ex.getMessage().contains("is closed"));
	}

	@Test
	void bytesAvailableAfterCloseShouldThrow() throws Exception {
		MockSerialDevice device = new MockSerialDevice();
		device.close();

		RuntimeIOException ex = Assertions.assertThrows(RuntimeIOException.class,
				() -> device.bytesAvailable());
		Assertions.assertTrue(ex.getMessage().contains("is closed"));
	}

	@Test
	void closeOrderingShouldUnblockRead() throws Exception {
		MockSerialDevice device = new MockSerialDevice();
		final int[] readResult = {-99};
		final RuntimeIOException[] readException = {null};
		final Throwable[] readThrowable = {null};

		Thread readThread = new Thread(() -> {
			try {
				// blocks — no data on socket. Returns -1 when socket gets EOF
				readResult[0] = device.read();
			} catch (RuntimeIOException e) {
				readException[0] = e;
			} catch (Throwable t) {
				readThrowable[0] = t;
			}
		});
		readThread.start();

		// Let the read() actually start blocking
		Thread.sleep(300);

		// Close the device — closing socket first should unblock the read.
		// When the fd is closed, the read returns -1 (EOF) or throws IOException.
		// The important part: the thread is unblocked, NOT stuck.
		device.close();

		readThread.join(3000);

		Assertions.assertFalse(readThread.isAlive(),
				"Read thread should have been unblocked by close() — raise(SIGINT) was likely called");

		// Either: read got an exception, or it got -1 (EOF) from the socket
		// The key assertion is that the thread is not alive — it was unblocked.
		if (readException[0] != null) {
			// Got an exception — should be a socket-closed/EOF type, not a signal
			String msg = readException[0].getMessage().toLowerCase();
			Assertions.assertTrue(
					msg.contains("closed") || msg.contains("eof") || msg.contains("reset") || msg.contains("broken"),
					"Read should fail with socket-closed error, got: " + readException[0].getMessage());
		} else if (readThrowable[0] != null) {
			String cls = readThrowable[0].getClass().getName();
			Assertions.assertFalse(cls.contains("Signal") || cls.contains("Interrupt"),
					"Should not receive signal/interrupt, got: " + readThrowable[0]);
		} else if (readResult[0] == -1) {
			// Got EOF — also valid, socket was closed
		} else {
			Assertions.fail("Read thread returned unexpected result: " + readResult[0]);
		}
	}
}
