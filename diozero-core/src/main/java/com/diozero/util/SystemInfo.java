package com.diozero.util;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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
import java.io.*;
import java.util.Properties;
import java.util.ServiceLoader;

import org.pmw.tinylog.Logger;

public class SystemInfo {
	private static final String OS_RELEASE_FILE = "/etc/os-release";
	private static final String CPUINFO_FILE = "/proc/cpuinfo";
	
	private static boolean initialised;
	private static Properties osReleaseProperties;
	private static BoardInfo boardInfo;
	
	private static synchronized void initialise() throws RuntimeIOException {
		if (! initialised) {
			osReleaseProperties = new Properties();
			try (Reader reader = new FileReader(OS_RELEASE_FILE)) {
				osReleaseProperties.load(reader);
				
				initialised = true;
			} catch (IOException e) {
				throw new RuntimeIOException("Error loading properties file '" + OS_RELEASE_FILE, e);
			}
			
			ProcessBuilder pb = new ProcessBuilder("cat", CPUINFO_FILE);
			BufferedReader reader = null;
			String revision_string = null;
			try {
				Process proc = pb.start();
				reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				String line;
				do {
					line = reader.readLine();
					if (line != null && line.startsWith("Revision")) {
						revision_string = line.split(":")[1].trim();
					}
				} while (line != null);
			} catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
				Logger.error(e, "Error reading " + CPUINFO_FILE, e.getMessage());
			} finally {
				if (reader != null) { try { reader.close(); } catch (IOException e) {} }
			}
			
			boardInfo = lookupBoardInfo(revision_string);
		}
	}
	
	protected static BoardInfo lookupBoardInfo(String revision) {
		BoardInfo board_info = null;
		ServiceLoader<BoardInfoProvider> service_loader = ServiceLoader.load(BoardInfoProvider.class);
		for (BoardInfoProvider board_info_provider : service_loader) {
			board_info = board_info_provider.lookup(revision);
			if (board_info != null) {
				break;
			}
		}
		return board_info;
	}

	public static String getOsReleaseProperty(String property) {
		return osReleaseProperties.getProperty(property);
	}

	public static String getOperatingSystemId() {
		initialise();
		
		return osReleaseProperties.getProperty("ID");
	}

	public static String getOperatingSystemVersion() {
		initialise();
		
		return osReleaseProperties.getProperty("VERSION");
	}

	public static String getOperatingSystemVersionId() {
		initialise();
		
		return osReleaseProperties.getProperty("VERSION_ID");
	}
	
	public static BoardInfo getBoardInfo() {
		return boardInfo;
	}
}
