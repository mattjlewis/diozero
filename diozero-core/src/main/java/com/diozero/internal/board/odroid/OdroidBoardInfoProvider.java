package com.diozero.internal.board.odroid;

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


import java.util.HashMap;
import java.util.Map;

import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;

public class OdroidBoardInfoProvider implements BoardInfoProvider {
	public static final OdroidBoardInfo ORDOID_C2 = new OdroidBoardInfo(Model.C2, 2048);
	
	public static final String MAKE = "Odroid";
	
	public static enum Model {
		C0, U2_U3, C1, XU_3_4, C2;
	}
	
	private static Map<String, BoardInfo> BOARDS;
	static {
		BOARDS = new HashMap<>();
		
		// TODO Verify C0
		//BOARDS.put("????", new OdroidBoardInfo(Model.C0, 1024));
		BOARDS.put("0000", new OdroidBoardInfo(Model.U2_U3, 2048));
		BOARDS.put("000a", new OdroidBoardInfo(Model.C1, 1024));
		BOARDS.put("0100", new OdroidBoardInfo(Model.XU_3_4, 2048));
		BOARDS.put("020b", ORDOID_C2);
	}

	@Override
	public BoardInfo lookup(String revisionString) {
		return BOARDS.get(revisionString);
	}

	public static class OdroidBoardInfo extends BoardInfo {
		public OdroidBoardInfo(Model model, int memory) {
			super(MAKE, model.toString(), memory);
		}
		
		@Override
		public String getLibraryPath() {
			return MAKE.toLowerCase() + "/" + getModel().toLowerCase();
		}
	}
}
