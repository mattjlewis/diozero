package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     HD44780Lcd.java
 *
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.diozero.api.DeviceInterface;
import com.diozero.api.RuntimeIOException;

/**
 * Abstraction for LCD devices: provides common functionality that can be "chained" together
 * to provide a fluent-style interface.
 */
public interface LcdInterface extends DeviceInterface {
    int getColumnCount();

    /**
     * Checks to see if the requested column is out of range.
     *
     * @param column the column
     */
    default void columnCheck(int column) {
        if (column < 0 || column >= getColumnCount()) {
            throw new IllegalArgumentException("Invalid column (" + column + "), must be 0.." + (getColumnCount() - 1));
        }
    }

    int getRowCount();

    /**
     * Checks to see if the requested row is out of bounds.
     *
     * @param row the row
     */
    default void rowCheck(int row) {
        if (row < 0 || row >= getRowCount()) {
            throw new IllegalArgumentException("Invalid row (" + row + "), must be 0.." + (getRowCount() - 1));
        }
    }

    boolean isBacklightEnabled();

    LcdInterface setBacklightEnabled(boolean backlightEnabled);

    LcdInterface setCursorPosition(int column, int row);

    /**
     * Sets a character on the display at a specific location.
     * @param column the column
     * @param row the row
     * @param character the character
     * @return this
     */
    default LcdInterface setCharacter(int column, int row, char character) {
        setCursorPosition(column, row);
        return addText(character);
    }

    /**
     * Writes the text to the display, starting at the home position. Newlines ({@code '\n'})
     * will wrap text to the next line. Excessive characters and lines are <b>trimmed</b>.
     *
     * @param text Text to display
     * @return this
     */
    default LcdInterface displayText(String text) {
        String[] splitText = text.split("\n");
        int minRows = Math.min(getRowCount(), splitText.length);
        for (int i = 0; i < minRows; i++) {
            setText(i, splitText[i]);
        }
        return this;
    }

    /**
     * Send string to display
     *
     * @param row  Row number (starts at 0)
     * @param text Text to display
     * @return this
     */
    default LcdInterface setText(int row, String text) {
        rowCheck(row);

        int columns = getColumnCount();
        int l = text.length();

        // Trim the string to the length of the column or pad to fill the rest
        String textToSend = l > columns ? text.substring(0, columns) : text + " ".repeat(getColumnCount() - l);

        // Set the cursor position to the start of the specified row
        setCursorPosition(0, row);
        return addText(textToSend);
    }

    /**
     * Add the string to the display at the current position.
     *
     * @param text the text
     * @return this
     */
    default LcdInterface addText(String text) {
        for (byte character : text.getBytes()) {
            addText((char)character);
        }
        return this;
    }

    /**
     * Add a <b>character</b> (e.g. alphanumeric) to the display at the current position.
     * Typically, this advances the cursor position by 1 in the set direction.
     *
     * @param character the text character
     * @return this
     */
    LcdInterface addText(char character);

    /**
     * Add a <b>special character</b> to the display at the current position.
     * Typically, this advances the cursor position by 1 in the set direction.
     *
     * @param code the character code
     * @return this
     * @see #createChar(int, byte[])
     */
    LcdInterface addText(int code);

    /**
     * Clear the display
     *
     * @return this
     */
    LcdInterface clear();

    /**
     * Return the cursor to the home position
     *
     * @return this
     */
    LcdInterface returnHome();

    LcdInterface autoscrollOn();

    LcdInterface autoscrollOff();

    boolean isIncrementOn();

    boolean isShiftDisplayOn();

    LcdInterface displayControl(boolean displayOn, boolean cursorEnabled, boolean blinkEnabled);

    LcdInterface displayOn();

    LcdInterface displayOff();

    LcdInterface cursorOn();

    LcdInterface cursorOff();

    LcdInterface blinkOn();

    LcdInterface blinkOff();

    boolean isCursorEnabled();

    boolean isBlinkEnabled();

    LcdInterface shiftDisplayRight();

    LcdInterface shiftDisplayLeft();

    LcdInterface moveCursorRight();

    LcdInterface moveCursorLeft();

    /**
     * Upload a bit-map of a "special character" to the specified location.
     *
     * @param location the location/code for this character
     * @param charMap  the bit-map for the character (one byte per "lines" in the dot matrix)
     * @return this
     * @see Characters
     * @see #addText(int)
     */
    LcdInterface createChar(int location, byte[] charMap);

    @Override
    void close() throws RuntimeIOException;

    class Characters {
        private static final Map<String, byte[]> CHARACTERS = new HashMap<>();

        static {
            CHARACTERS.put("0", new byte[]{0xe, 0x1b, 0x1b, 0x1b, 0x1b, 0x1b, 0xe});
            CHARACTERS.put("1", new byte[]{0x2, 0x6, 0xe, 0x6, 0x6, 0x6, 0x6});
            CHARACTERS.put("2", new byte[]{0xe, 0x1b, 0x3, 0x6, 0xc, 0x18, 0x1f});
            CHARACTERS.put("3", new byte[]{0xe, 0x1b, 0x3, 0xe, 0x3, 0x1b, 0xe});
            CHARACTERS.put("4", new byte[]{0x3, 0x7, 0xf, 0x1b, 0x1f, 0x3, 0x3});
            CHARACTERS.put("5", new byte[]{0x1f, 0x18, 0x1e, 0x3, 0x3, 0x1b, 0xe});
            CHARACTERS.put("6", new byte[]{0xe, 0x1b, 0x18, 0x1e, 0x1b, 0x1b, 0xe});
            CHARACTERS.put("7", new byte[]{0x1f, 0x3, 0x6, 0xc, 0xc, 0xc, 0xc});
            CHARACTERS.put("8", new byte[]{0xe, 0x1b, 0x1b, 0xe, 0x1b, 0x1b, 0xe});
            CHARACTERS.put("9", new byte[]{0xe, 0x1b, 0x1b, 0xf, 0x3, 0x1b, 0xe});
            CHARACTERS.put("10", new byte[]{0x17, 0x15, 0x15, 0x15, 0x17, 0x0, 0x1f});
            CHARACTERS.put("11", new byte[]{0xa, 0xa, 0xa, 0xa, 0xa, 0x0, 0x1f});
            CHARACTERS.put("12", new byte[]{0x17, 0x11, 0x17, 0x14, 0x17, 0x0, 0x1f});
            CHARACTERS.put("13", new byte[]{0x17, 0x11, 0x13, 0x11, 0x17, 0x0, 0x1f});
            CHARACTERS.put("14", new byte[]{0x15, 0x15, 0x17, 0x11, 0x11, 0x0, 0x1f});
            CHARACTERS.put("15", new byte[]{0x17, 0x14, 0x17, 0x11, 0x17, 0x0, 0x1f});
            CHARACTERS.put("16", new byte[]{0x17, 0x14, 0x17, 0x15, 0x17, 0x0, 0x1f});
            CHARACTERS.put("17", new byte[]{0x17, 0x11, 0x12, 0x12, 0x12, 0x0, 0x1f});
            CHARACTERS.put("18", new byte[]{0x17, 0x15, 0x17, 0x15, 0x17, 0x0, 0x1f});
            CHARACTERS.put("19", new byte[]{0x17, 0x15, 0x17, 0x11, 0x17, 0x0, 0x1f});
            CHARACTERS.put("circle", new byte[]{0x0, 0xe, 0x11, 0x11, 0x11, 0xe, 0x0});
            CHARACTERS.put("cdot", new byte[]{0x0, 0xe, 0x11, 0x15, 0x11, 0xe, 0x0});
            CHARACTERS.put("donut", new byte[]{0x0, 0xe, 0x1f, 0x1b, 0x1f, 0xe, 0x0});
            CHARACTERS.put("ball", new byte[]{0x0, 0xe, 0x1f, 0x1f, 0x1f, 0xe, 0x0});
            CHARACTERS.put("square", new byte[]{0x0, 0x1f, 0x11, 0x11, 0x11, 0x1f, 0x0});
            CHARACTERS.put("sdot", new byte[]{0x0, 0x1f, 0x11, 0x15, 0x11, 0x1f, 0x0});
            CHARACTERS.put("fbox", new byte[]{0x0, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x0});
            CHARACTERS.put("sbox", new byte[]{0x0, 0x0, 0xe, 0xa, 0xe, 0x0, 0x0});
            CHARACTERS.put("sfbox", new byte[]{0x0, 0x0, 0xe, 0xe, 0xe, 0x0, 0x0});
            CHARACTERS.put("bigpointerright", new byte[]{0x8, 0xc, 0xa, 0x9, 0xa, 0xc, 0x8});
            CHARACTERS.put("bigpointerleft", new byte[]{0x2, 0x6, 0xa, 0x12, 0xa, 0x6, 0x2});
            CHARACTERS.put("arrowright", new byte[]{0x8, 0xc, 0xa, 0x9, 0xa, 0xc, 0x8});
            CHARACTERS.put("arrowleft", new byte[]{0x2, 0x6, 0xa, 0x12, 0xa, 0x6, 0x2});
            CHARACTERS.put("ascprogress1", new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10});
            CHARACTERS.put("ascprogress2", new byte[]{0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18});
            CHARACTERS.put("ascprogress3", new byte[]{0x1c, 0x1c, 0x1c, 0x1c, 0x1c, 0x1c, 0x1c, 0x1c});
            CHARACTERS.put("ascprogress4", new byte[]{0x1e, 0x1e, 0x1e, 0x1e, 0x1e, 0x1e, 0x1e, 0x1e});
            CHARACTERS.put("fullprogress", new byte[]{0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f});
            CHARACTERS.put("descprogress1", new byte[]{1, 1, 1, 1, 1, 1, 1, 1});
            CHARACTERS.put("descprogress2", new byte[]{3, 3, 3, 3, 3, 3, 3, 3});
            CHARACTERS.put("descprogress3", new byte[]{7, 7, 7, 7, 7, 7, 7, 7});
            CHARACTERS.put("descprogress4", new byte[]{15, 15, 15, 15, 15, 15, 15, 15});
            CHARACTERS.put("ascchart1", new byte[]{31, 0, 0, 0, 0, 0, 0, 0});
            CHARACTERS.put("ascchart2", new byte[]{31, 31, 0, 0, 0, 0, 0, 0});
            CHARACTERS.put("ascchart3", new byte[]{31, 31, 31, 0, 0, 0, 0, 0});
            CHARACTERS.put("ascchart4", new byte[]{31, 31, 31, 31, 0, 0, 0, 0});
            CHARACTERS.put("ascchart5", new byte[]{31, 31, 31, 31, 31, 0, 0, 0});
            CHARACTERS.put("ascchart6", new byte[]{31, 31, 31, 31, 31, 31, 0, 0});
            CHARACTERS.put("ascchart7", new byte[]{31, 31, 31, 31, 31, 31, 31, 0});
            CHARACTERS.put("descchart1", new byte[]{0, 0, 0, 0, 0, 0, 0, 31});
            CHARACTERS.put("descchart2", new byte[]{0, 0, 0, 0, 0, 0, 31, 31});
            CHARACTERS.put("descchart3", new byte[]{0, 0, 0, 0, 0, 31, 31, 31});
            CHARACTERS.put("descchart4", new byte[]{0, 0, 0, 0, 31, 31, 31, 31});
            CHARACTERS.put("descchart5", new byte[]{0, 0, 0, 31, 31, 31, 31, 31});
            CHARACTERS.put("descchart6", new byte[]{0, 0, 31, 31, 31, 31, 31, 31});
            CHARACTERS.put("descchart7", new byte[]{0, 31, 31, 31, 31, 31, 31, 31});
            CHARACTERS.put("borderleft1", new byte[]{1, 1, 1, 1, 1, 1, 1, 1});
            CHARACTERS.put("borderleft2", new byte[]{3, 2, 2, 2, 2, 2, 2, 3});
            CHARACTERS.put("borderleft3", new byte[]{7, 4, 4, 4, 4, 4, 4, 7});
            CHARACTERS.put("borderleft4", new byte[]{15, 8, 8, 8, 8, 8, 8, 15});
            CHARACTERS.put("borderleft5", new byte[]{31, 16, 16, 16, 16, 16, 16, 31});
            CHARACTERS.put("bordertopbottom5", new byte[]{31, 0, 0, 0, 0, 0, 0, 31});
            CHARACTERS.put("borderright1", new byte[]{16, 16, 16, 16, 16, 16, 16, 16});
            CHARACTERS.put("borderright2", new byte[]{24, 8, 8, 8, 8, 8, 8, 24});
            CHARACTERS.put("borderright3", new byte[]{28, 4, 4, 4, 4, 4, 4, 28});
            CHARACTERS.put("borderright4", new byte[]{30, 2, 2, 2, 2, 2, 2, 30});
            CHARACTERS.put("borderright5", new byte[]{31, 1, 1, 1, 1, 1, 1, 31});
            CHARACTERS.put("box1", new byte[]{3, 3, 3, 0, 0, 0, 0});
            CHARACTERS.put("box2", new byte[]{24, 24, 24, 0, 0, 0, 0});
            CHARACTERS.put("box3", new byte[]{27, 27, 27, 0, 0, 0, 0});
            CHARACTERS.put("box4", new byte[]{0, 0, 0, 0, 3, 3, 3});
            CHARACTERS.put("box5", new byte[]{3, 3, 3, 0, 3, 3, 3});
            CHARACTERS.put("box6", new byte[]{24, 24, 24, 0, 3, 3, 3});
            CHARACTERS.put("box7", new byte[]{27, 27, 27, 0, 3, 3, 3});
            CHARACTERS.put("box8", new byte[]{0, 0, 0, 0, 24, 24, 24});
            CHARACTERS.put("box9", new byte[]{3, 3, 3, 0, 24, 24, 24});
            CHARACTERS.put("box10", new byte[]{24, 24, 24, 0, 24, 24, 24});
            CHARACTERS.put("box11", new byte[]{27, 27, 27, 0, 24, 24, 24});
            CHARACTERS.put("box12", new byte[]{0, 0, 0, 0, 27, 27, 27});
            CHARACTERS.put("box13", new byte[]{3, 3, 3, 0, 27, 27, 27});
            CHARACTERS.put("box14", new byte[]{24, 24, 24, 0, 27, 27, 27});
            CHARACTERS.put("box15", new byte[]{27, 27, 27, 0, 27, 27, 27});
            CHARACTERS.put("euro", new byte[]{3, 4, 30, 8, 30, 8, 7});
            CHARACTERS.put("cent", new byte[]{0, 0, 14, 17, 16, 21, 14, 8});
            CHARACTERS.put("speaker", new byte[]{1, 3, 15, 15, 15, 3, 1});
            CHARACTERS.put("sound", new byte[]{8, 16, 0, 24, 0, 16, 8});
            CHARACTERS.put("x", new byte[]{0, 27, 14, 4, 14, 27, 0});
            CHARACTERS.put("target", new byte[]{0, 10, 17, 21, 17, 10, 0});
            CHARACTERS.put("pointerright", new byte[]{0, 8, 12, 14, 12, 8, 0});
            CHARACTERS.put("pointerup", new byte[]{0, 0, 4, 14, 31, 0, 0});
            CHARACTERS.put("pointerleft", new byte[]{0, 2, 6, 14, 6, 2, 0});
            CHARACTERS.put("pointerdown", new byte[]{0, 0, 31, 14, 4, 0, 0});
            CHARACTERS.put("arrowne", new byte[]{0, 15, 3, 5, 9, 16, 0});
            CHARACTERS.put("arrownw", new byte[]{0, 30, 24, 20, 18, 1, 0});
            CHARACTERS.put("arrowsw", new byte[]{0, 1, 18, 20, 24, 30, 0});
            CHARACTERS.put("arrowse", new byte[]{0, 16, 9, 5, 3, 15, 0});
            CHARACTERS.put("dice1", new byte[]{0, 0, 0, 4, 0, 0, 0});
            CHARACTERS.put("dice2", new byte[]{0, 16, 0, 0, 0, 1, 0});
            CHARACTERS.put("dice3", new byte[]{0, 16, 0, 4, 0, 1, 0});
            CHARACTERS.put("dice4", new byte[]{0, 17, 0, 0, 0, 17, 0});
            CHARACTERS.put("dice5", new byte[]{0, 17, 0, 4, 0, 17, 0});
            CHARACTERS.put("dice6", new byte[]{0, 17, 0, 17, 0, 17, 0});
            CHARACTERS.put("bell", new byte[]{4, 14, 14, 14, 31, 0, 4});
            CHARACTERS.put("smile", new byte[]{0, 10, 0, 17, 14, 0, 0});
            CHARACTERS.put("note", new byte[]{2, 3, 2, 14, 30, 12, 0});
            CHARACTERS.put("clock", new byte[]{0, 14, 21, 23, 17, 14, 0});
            CHARACTERS.put("heart", new byte[]{0, 10, 31, 31, 31, 14, 4, 0});
            CHARACTERS.put("duck", new byte[]{0, 12, 29, 15, 15, 6, 0});
            CHARACTERS.put("check", new byte[]{0, 1, 3, 22, 28, 8, 0});
            CHARACTERS.put("retarrow", new byte[]{1, 1, 5, 9, 31, 8, 4});
            CHARACTERS.put("runninga", new byte[]{6, 6, 5, 14, 20, 4, 10, 17});
            CHARACTERS.put("runningb", new byte[]{6, 6, 4, 14, 14, 4, 10, 10});
            CHARACTERS.put("space_invader", new byte[]{0x00, 0x0e, 0x15, 0x1f, 0x0a, 0x04, 0x0a, 0x11});
            CHARACTERS.put("smilie", new byte[]{0x00, 0x00, 0x0a, 0x00, 0x00, 0x11, 0x0e, 0x00});
            CHARACTERS.put("frownie", new byte[]{0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x0e, 0x11});
        }

        /**
         * Get the specs for the specified character
         *
         * @param name the name of the character
         * @return the uploadable byte-spec
         */
        public static byte[] get(String name) {
            return CHARACTERS.get(name);
        }

        /**
         * @return the names of the defined characters
         */
        public static Collection<String> getCharacters() {
            return CHARACTERS.keySet();
        }
    }
}
