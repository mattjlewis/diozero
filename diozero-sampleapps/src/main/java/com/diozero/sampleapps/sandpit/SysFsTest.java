package com.diozero.sampleapps.sandpit;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     SysFsTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class SysFsTest {
	public static void main(String[] args) {
		try {
			test(12);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void test(int gpio) throws IOException, InterruptedException {
		Path direction_path = Paths.get("/sys/class/gpio/gpio" + gpio + "/direction");
		File direction_file = direction_path.toFile();
		
		if (! direction_file.exists()) {
			String export_file = "/sys/class/gpio/export";
			System.out.println("echo \"" + gpio + "\" > " + export_file);
			try (FileWriter export = new FileWriter(export_file)) {
				export.write(Integer.toString(gpio));
			}
		}
		
		long max_wait = 500;
		long start = System.currentTimeMillis();
		while (! direction_file.canWrite()) {
			System.out.println("Can't write, waiting");
			Thread.sleep(20);
			if (System.currentTimeMillis() - start > max_wait) {
				System.out.println("Waited too long");
				return;
			}
		}
		
		boolean append = true;
		String dir = "out";
		System.out.println("echo \"" + dir + "\" " + (append ? ">> " : "> ") + direction_file);
		System.out.println(direction_file.exists() + ", " + direction_file.canRead() + ", " + direction_file.canWrite());
		try (OutputStream os = Files.newOutputStream(direction_path, StandardOpenOption.APPEND)) {
			os.write(dir.getBytes());
		}

		System.out.println("Opening value file");
		try (RandomAccessFile raf = new RandomAccessFile("/sys/class/gpio/gpio" + gpio + "/value", "rw")) {
			for (int i=0; i<10; i++) {
				String v = "1";
				System.out.println("Setting value to '" + v + "' for " + gpio);
				raf.write(v.getBytes());
				Thread.sleep(1000);
				v = "0";
				System.out.println("Setting value to '" + v + "' for " + gpio);
				raf.seek(0);
				raf.write(v.getBytes());
				Thread.sleep(1000);
			}
		}
	}
}
