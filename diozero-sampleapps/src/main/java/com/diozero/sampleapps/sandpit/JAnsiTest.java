package com.diozero.sampleapps.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     JAnsiTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

import static org.fusesource.jansi.Ansi.ansi;

import org.fusesource.jansi.AnsiConsole;
import org.tinylog.Logger;

public class JAnsiTest {
	public static void main(String[] args) {
		// Attempt to initialise Jansi
		try {
			AnsiConsole.systemInstall();
		} catch (Throwable t) {
			// Ignore
			Logger.error(t, "Jansi native library not available on this platform: {}", t);
		}

		System.out.println(
				ansi().fgRed().a("Hello").a(" ").fgGreen().a("World!").reset().a(" ").bgBrightRed().a("Matt").reset());
		// AnsiConsole.out().format(render("@|bold Hello %s|@%n"), "diozero");
		System.out.format(ansi().render("@|bold Hello %s|@%n").toString(), "diozero");
		System.out.println(ansi().bold().fgGreen().a("Hello").boldOff().fgDefault().a(" diozero"));
	}
}
