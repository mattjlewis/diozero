package com.diozero.sbc;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     LocalBoardInfoUtil.java
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

import java.util.Objects;

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.BoardInfoProvider;

/**
 * <p>
 * Utility class for accessing information for the local board that the
 * application is executing on. Access to board info should typically done by
 * calling
 * {@link com.diozero.internal.spi.NativeDeviceFactoryInterface#getBoardInfo
 * getBoardInfo()} on the {@link DeviceFactoryHelper#getNativeDeviceFactory
 * automatically discovered}
 * {@link com.diozero.internal.spi.NativeDeviceFactoryInterface native device
 * factory}
 * </p>
 * <p>
 * Note some boards are accessed remotely (e.g. Firmata protocol and pigpio
 * sockets) hence this information may differ to the actual device you are
 * controlling.
 * </p>
 */
public class LocalBoardInfoUtil {
	private static boolean initialised;
	private static BoardInfo localBoardInfo;

	private static synchronized void initialiseLocalBoardInfo() throws RuntimeIOException {
		if (!initialised) {
			localBoardInfo = resolveLocalBoardInfo(LocalSystemInfo.getInstance());
			// Only initialise the pins for the resolved board
			// Don't initialise the pins! That is done within the base native device factory
			// localBoardInfo.populateBoardPinInfo();

			initialised = true;
		}
	}

	// Package private so it can called from unit tests
	static BoardInfo resolveLocalBoardInfo(LocalSystemInfo localSysInfo) {
		return BoardInfoProvider.loadInstances().map(bip -> bip.lookup(localSysInfo)).filter(Objects::nonNull)
				.findFirst().orElseGet(() -> UnknownBoardInfo.get(localSysInfo));
	}

	/**
	 * Returns information for the local device only. Note some providers work over
	 * a remote connection - if you want information for the device you are
	 * controlling please use:<br>
	 * {@code DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo()}
	 *
	 * @return BoardInfo instance describing the local device.
	 */
	public static BoardInfo lookupLocalBoardInfo() {
		initialiseLocalBoardInfo();

		return localBoardInfo;
	}
}
