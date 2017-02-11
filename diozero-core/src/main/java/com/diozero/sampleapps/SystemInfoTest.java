package com.diozero.sampleapps;

import org.pmw.tinylog.Logger;

import com.diozero.util.BoardInfo;
import com.diozero.util.SystemInfo;

public class SystemInfoTest {
	public static void main(String[] args) {
		Logger.info("Operating System id: {}", SystemInfo.getOperatingSystemId());
		Logger.info("Operating System version: {} {}",
				SystemInfo.getOperatingSystemVersion(), SystemInfo.getOperatingSystemVersionId());
		
		BoardInfo board_info = SystemInfo.getBoardInfo();
		
		Logger.info("Name: {}", board_info.getName());
		Logger.info("Make: {}", board_info.getMake());
		Logger.info("Model: {}", board_info.getModel());
		Logger.info("Memory: {}", Integer.valueOf(board_info.getMemory()));
		
		Logger.info("GPIOs: {}", board_info.getGpios());
	}
}
