package com.diozero.internal.spi;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     GpioDeviceFactoryInterface.java
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

import com.diozero.api.*;

public interface GpioDeviceFactoryInterface extends DeviceFactoryInterface {
    default GpioDigitalInputDeviceInterface provisionDigitalInputDevice(
            PinInfo pinInfo,
            GpioPullUpDown pud,
            GpioEventTrigger trigger) throws RuntimeIOException {

        return registerPinDevice(pinInfo, (k) -> {
            if (!pinInfo.isSupported(DeviceMode.DIGITAL_INPUT)) {
                throw new InvalidModeException("Invalid mode (digital input) for pin " + pinInfo);
            }
            return createDigitalInputDevice(k, pinInfo, pud, trigger);
        });
    }

    default GpioDigitalOutputDeviceInterface provisionDigitalOutputDevice(PinInfo pinInfo, boolean initialValue)
            throws RuntimeIOException {
        return registerPinDevice(pinInfo, (k) -> {
            if (!pinInfo.isSupported(DeviceMode.DIGITAL_OUTPUT)) {
                throw new InvalidModeException("Invalid mode (digital output) for pin " + pinInfo);
            }
            return createDigitalOutputDevice(k, pinInfo, initialValue);
        });
    }

    default GpioDigitalInputOutputDeviceInterface provisionDigitalInputOutputDevice(PinInfo pinInfo, DeviceMode mode)
            throws RuntimeIOException {
        return registerPinDevice(pinInfo, (k) -> {
            if (!pinInfo.getModes().containsAll(PinInfo.DIGITAL_IN_OUT)) {
                throw new InvalidModeException("Invalid mode (digital input/output) for pin " + pinInfo);
            }
            if (mode != DeviceMode.DIGITAL_INPUT && mode != DeviceMode.DIGITAL_OUTPUT) {
                throw new InvalidModeException("Invalid mode, must be DIGITAL_INPUT or DIGITAL_OUTPUT");
            }

            return createDigitalInputOutputDevice(k, pinInfo, mode);
        });
    }


    GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
                                                             GpioEventTrigger trigger);

    GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo, boolean initialValue);

    GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo, DeviceMode mode);
}
