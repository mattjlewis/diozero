package com.diozero.devices;

import com.diozero.api.RuntimeIOException;

/**
 * Simplified LDC 2-row, 16-column display with integrated I<sup>2</sup>C controller: best guess for the hardware
 * identifier is "GH1602-2502". The
 * <p>
 * TODO see <a href="https://github.com/Pi4J/pi4j-example-components">Pi4J/pi4j-example-components</a>", Apache v2 License.
 */
public class GH1602Lcd implements LcdInterface {

    public GH1602Lcd(LcdConnection lcdConnection) {
        throw new UnsupportedOperationException("Device is not currently supported");
    }
    @Override
    public int getColumnCount() {
        return 16;
    }

    @Override
    public int getRowCount() {
        return 2;
    }

    @Override
    public boolean isBacklightEnabled() {
        return false;
    }

    @Override
    public LcdInterface setBacklightEnabled(boolean backlightEnabled) {
        return this;
    }

    @Override
    public LcdInterface setCursorPosition(int column, int row) {
        return this;
    }

    @Override
    public LcdInterface setCharacter(int column, int row, char character) {
        return this;
    }

    @Override
    public LcdInterface setText(int row, String text) {
        return this;
    }

    @Override
    public LcdInterface addText(String text) {
        return this;
    }

    @Override
    public LcdInterface addText(char character) {
        return this;
    }

    @Override
    public LcdInterface addText(int code) {
        return this;
    }

    @Override
    public LcdInterface clear() {
        return this;
    }

    @Override
    public LcdInterface returnHome() {
        return this;
    }

    @Override
    public LcdInterface autoscrollOn() {
        return this;
    }

    @Override
    public LcdInterface autoscrollOff() {
        return this;
    }

    @Override
    public boolean isIncrementOn() {
        return false;
    }

    @Override
    public boolean isShiftDisplayOn() {
        return false;
    }

    @Override
    public LcdInterface displayControl(boolean displayOn, boolean cursorEnabled, boolean blinkEnabled) {
        return this;
    }

    @Override
    public LcdInterface displayOn() {
        return this;
    }

    @Override
    public LcdInterface displayOff() {
        return this;
    }

    @Override
    public LcdInterface cursorOn() {
        return this;
    }

    @Override
    public LcdInterface cursorOff() {
        return this;
    }

    @Override
    public LcdInterface blinkOn() {
        return this;
    }

    @Override
    public LcdInterface blinkOff() {
        return this;
    }

    @Override
    public boolean isCursorEnabled() {
        return false;
    }

    @Override
    public boolean isBlinkEnabled() {
        return false;
    }

    @Override
    public LcdInterface shiftDisplayRight() {
        return this;
    }

    @Override
    public LcdInterface shiftDisplayLeft() {
        return this;
    }

    @Override
    public LcdInterface moveCursorRight() {
        return this;
    }

    @Override
    public LcdInterface moveCursorLeft() {
        return this;
    }

    @Override
    public LcdInterface createChar(int location, byte[] charMap) {
        return this;
    }

    @Override
    public void close() throws RuntimeIOException {
        displayOff();
    }
}
