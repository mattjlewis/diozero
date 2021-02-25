package com.diozero.devices.oled;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SSD1306.java
 *
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import com.diozero.api.I2CDevice;

@SuppressWarnings("unused")
public class SSD1306i2c extends AbstractSSD1306 {

    private final I2CDevice i2cDevice;

    public SSD1306i2c(int controller, int address, int width, int height, boolean externalVcc) {
        super(width, height, externalVcc);

        this.i2cDevice = I2CDevice.builder(address).setController(controller).build();

        init();
    }

    public SSD1306i2c(int controller, int address) {
        this(controller, address, WIDTH, HEIGHT, false);
    }


    @Override
    public void close() {
        super.close();
        i2cDevice.close();
    }

    protected void transferDisplayBuffer() {
        byte[] buf = new byte[16];
        for (int i = 0; i < ((getWidth() * getHeight() / 8) / 16); i++) {
            // send a bunch of render in one xmission
            System.arraycopy(getDisplayBuffer(), 16 * i, buf, 0, buf.length);
            i2cDevice.writeBlockData((byte) 0x40, buf);
        }
    }

    @Override
    protected void writeCommand(byte... commands) {
        for (byte command : commands) {
            i2cDevice.writeByteData(0x00, command);
        }
    }
}
