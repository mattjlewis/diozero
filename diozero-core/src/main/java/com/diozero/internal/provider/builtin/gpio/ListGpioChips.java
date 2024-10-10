package com.diozero.internal.provider.builtin.gpio;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ListGpioChips.java
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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Comparator;

public class ListGpioChips {
	public static void main(String[] args) {
		try {
			Files.list(Paths.get("src/test/resources/dev"))
					.filter(p -> p.getFileName().toString().startsWith("gpiochip")) //
					.sorted(Comparator.comparing(p -> p.getFileName().toString())) //
					.distinct() //
					.forEach(ListGpioChips::printInfo);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void printInfo(Path p) {
		System.out.format("%s symbolic link: %b, regular file (nofollow_links): %b, regular file: %b, directory: %b\n",
				p, Boolean.valueOf(Files.isSymbolicLink(p)),
				Boolean.valueOf(Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)),
				Boolean.valueOf(Files.isRegularFile(p)), Boolean.valueOf(Files.isDirectory(p)));
		try {
			var attrs = Files.readAttributes(p, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			System.out.format("directory: %b, other: %b, regular: %b, symbolic link: %b\n", attrs.isDirectory(),
					attrs.isOther(), attrs.isRegularFile(), attrs.isSymbolicLink());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
