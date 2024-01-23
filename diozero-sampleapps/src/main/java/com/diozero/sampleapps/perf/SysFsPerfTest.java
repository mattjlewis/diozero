package com.diozero.sampleapps.perf;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     SysFsPerfTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.tinylog.Logger;

public class SysFsPerfTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <gpio> [read iterations] [toggle iterations]", SysFsPerfTest.class.getName());
			System.exit(1);
		}
		int gpio = Integer.parseInt(args[0]);
		int read_iterations = 200_000;
		if (args.length > 1) {
			read_iterations = Integer.parseInt(args[1]);
		}
		int toggle_iterations = 20_000;
		if (args.length > 2) {
			toggle_iterations = Integer.parseInt(args[1]);
		}

		try {
			for (int i = 0; i < 5; i++) {
				testRafRead(gpio, read_iterations);
			}
			for (int i = 0; i < 5; i++) {
				testRafToggle(gpio, toggle_iterations);
			}
			for (int i = 0; i < 5; i++) {
				testMmapRead(gpio, read_iterations);
			}
			for (int i = 0; i < 5; i++) {
				testMmapToggle(gpio, toggle_iterations);
			}
		} catch (IOException e) {
			Logger.error(e, "Error: {}", e);
		}
	}

	private static void testRafRead(int gpio, int iterations) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile("/sys/class/gpio/gpio" + gpio + "/value", "rw")) {
			long start = System.currentTimeMillis();
			for (int i = 0; i < iterations; i++) {
				raf.seek(0);
				raf.read();
			}
			long duration = System.currentTimeMillis() - start;
			double frequency = iterations / (duration / 1000.0);
			Logger.info("Random Access File read: {#,###.###} ms per iteration, frequency {#,###.#}",
					Double.valueOf(duration * 1000 / (double) iterations), Double.valueOf(frequency));
		}
	}

	private static void testRafToggle(int gpio, int iterations) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile("/sys/class/gpio/gpio" + gpio + "/value", "rw")) {
			long start = System.currentTimeMillis();
			for (int i = 0; i < iterations; i++) {
				raf.seek(0);
				raf.write('1');
				raf.seek(0);
				raf.write('0');
			}
			long duration = System.currentTimeMillis() - start;
			Logger.info("Random Access File toggle: {0.000} ms per iteration",
					Double.valueOf(duration * 1000 / (double) iterations));
		}
	}

	private static void testMmapRead(int gpio, int iterations) throws IOException {
		try (FileChannel fc = FileChannel.open(Paths.get("/sys/class/gpio/gpio" + gpio + "/value"),
				StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.SYNC)) {
			MappedByteBuffer mbb = fc.map(MapMode.READ_WRITE, 0, 1);
			long start = System.currentTimeMillis();
			for (int i = 0; i < iterations; i++) {
				mbb.get(0);
			}
			long duration = System.currentTimeMillis() - start;
			Logger.info("mmap read: {0.000} ms per iteration", Double.valueOf(duration * 1000 / (double) iterations));
		}
	}

	private static void testMmapToggle(int gpio, int iterations) throws IOException {
		try (FileChannel fc = FileChannel.open(Paths.get("/sys/class/gpio/gpio" + gpio + "/value"),
				StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.SYNC)) {
			MappedByteBuffer mbb = fc.map(MapMode.READ_WRITE, 0, 1);
			long start = System.currentTimeMillis();
			for (int i = 0; i < iterations; i++) {
				mbb.put(0, (byte) '1');
				mbb.put(0, (byte) '0');
			}
			long duration = System.currentTimeMillis() - start;
			Logger.info("mmap toggle: {0.000} ms per iteration", Double.valueOf(duration * 1000 / (double) iterations));
		}
	}
}
