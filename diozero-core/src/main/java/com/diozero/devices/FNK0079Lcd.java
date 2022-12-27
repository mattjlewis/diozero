package com.diozero.devices;

import com.diozero.api.RuntimeIOException;

import static com.diozero.util.SleepUtil.sleepNanos;

/**
 * Simplified LCD with integrated I2C controller:
 * <a href="https://github.com/Freenove/Freenove_LCD_Module">Freenove LCD Module 1602</a></li>
 * <p>
 * Reference material:
 * <ul>
 *     <li><a href="https://github.com/Freenove/Freenove_LCD_Module">Freenove LCD Module 1602</a></li>
 *     <li><a href="https://github.com/Pi4J/pi4j-example-components">Pi4J/pi4j-example-components</a>", Apache v2 License.</li>
 * </ul>
 */
public class FNK0079Lcd implements LcdInterface {
    // display commands
    private static final int CLEAR_DISPLAY = 0x01;
    private static final int RETURN_HOME = 0x02;

    // RAM select
    private static final int SELECT_CGRAM_ADDR = 0x40;
    private static final int SELECT_DDRAM_ADDR = 0x80;
    // row offsets (OR with DDRAM select to write to the display)
    private static final byte[] LCD_ROW_OFFSETS = {0x00, 0x40};

    // display entry mode
    private static final int ENTRY_MODE_SET = 0x04;    // control bit
    private static final int ENTRY_LEFT = 0x00;
    private static final int ENTRY_RIGHT = 0x02; // moves cursor to the right after write (increase RAM address by 1)
    // TODO what does this really do?
    private static final int ENTRY_ENABLE_SHIFT = 0x01;
    private static final int ENTRY_DISABLE_SHIFT = 0x00;

    // display on/off controls
    private static final int DISPLAY_CONTROL = 0x08;    // control bit
    private static final int DISPLAY_ON = 0x04;
    private static final int CURSOR_ON = 0x02;
    private static final int BLINK_ON = 0x01;

    // display/cursor shift
    private static final int MOVE_CONTROL = 0x10; // control bit
    private static final int DISPLAY_MOVE = 0x08;
    private static final int CURSOR_MOVE = 0x00;
    private static final int MOVE_RIGHT = 0x04;
    private static final int MOVE_LEFT = 0x00;

    // function set
    private static final int LCD_FUNCTION_SET = 0x20;    // control bit
    private static final int LCD_8BIT_MODE = 0x10;
    private static final int LCD_4BIT_MODE = 0x00;
    private static final int LCD_2LINE = 0x08;
    // N.B. these are not a thing for this
    private static final int LCD_1LINE = 0x00;
    private static final int LCD_5x10DOTS = 0x04;
    private static final int LCD_5x8DOTS = 0x00;

    // backlight control
    private static final int BACKLIGHT = 0x08;
    private static final int NO_BACKLIGHT = 0x00;

    // data transfer
    private static final int ENABLE_BIT = 0x04; // Enable bit
    private static final int RW_BIT = 0x02; // Read/Write bit
    private static final int REGISTER_SELECT = 0x01; // Register select bit

    // the actual interface
    private final LcdConnection lcdConnection;

    private boolean backlight = false;
    private boolean displayOn = true;
    private boolean cursorVisible = false;
    private boolean cursorBlinking = false;

    /**
     * Default constructor for PCF8574-backpack on controller bus 1 (Raspberry Pi).
     */
    public FNK0079Lcd() {
        this(1);
    }

    /**
     * Default constructor for PCF8574-backpack.
     *
     * @param controller the I2C bus controller number
     */
    public FNK0079Lcd(int controller) {
        this(new LcdConnection.PCF8574LcdConnection(controller));
    }

    public FNK0079Lcd(LcdConnection lcdConnection) {
        this.lcdConnection = lcdConnection;

        // this serves as a "reset" in case the display is wonky
        writeCommand(RETURN_HOME | CLEAR_DISPLAY);
        writeCommand(RETURN_HOME | CLEAR_DISPLAY);
        writeCommand(RETURN_HOME | CLEAR_DISPLAY);
        writeCommand(RETURN_HOME);

        // init the interface (2 lines, 5x8, 4-bit mode
        writeCommand(LCD_FUNCTION_SET | LCD_2LINE | LCD_5x8DOTS | LCD_4BIT_MODE);

        // initialize display settings - display on, cursor off, blink off
        displayControl(true, false, false);
        // by default, set up for Indo-European left-to-right
        writeCommand(ENTRY_MODE_SET | ENTRY_RIGHT | ENTRY_DISABLE_SHIFT);

        // turn it on
        clear();
        setBacklightEnabled(true);
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
        return backlight;
    }

    @Override
    public LcdInterface setBacklightEnabled(boolean backlightEnabled) {
        backlight = backlightEnabled;
        executeCommand(backlight ? BACKLIGHT : 0);
        return this;
    }

    @Override
    public LcdInterface setCursorPosition(int column, int row) {
        rowCheck(row);
        columnCheck(column);
        writeCommand(SELECT_DDRAM_ADDR | column + LCD_ROW_OFFSETS[row]);
        return this;
    }

    @Override
    public LcdInterface addText(char character) {
        writeSplitCommand(character, REGISTER_SELECT);
        return this;
    }

    @Override
    public LcdInterface addText(int code) {
        writeSplitCommand(code, REGISTER_SELECT);
        return this;
    }

    @Override
    public LcdInterface clear() {
        writeCommand(CLEAR_DISPLAY);
        return this;
    }

    @Override
    public LcdInterface returnHome() {
        writeCommand(RETURN_HOME);
        return this;
    }

    @Override
    public LcdInterface autoscrollOn() {
        // no-op
        return this;
    }

    @Override
    public LcdInterface autoscrollOff() {
        // no-op
        return this;
    }

    @Override
    public boolean isIncrementOn() {
        return true;
    }

    @Override
    public boolean isShiftDisplayOn() {
        return false;
    }

    @Override
    public LcdInterface displayControl(boolean displayOn, boolean cursorEnabled, boolean blinkEnabled) {
        this.displayOn = displayOn;
        this.cursorVisible = cursorEnabled;
        this.cursorBlinking = blinkEnabled;

        int command = DISPLAY_CONTROL |
                (displayOn ? DISPLAY_ON : 0) |
                (cursorVisible ? CURSOR_ON : 0) |
                (cursorBlinking ? BLINK_ON : 0);
        writeCommand(command);
        return this;
    }

    @Override
    public LcdInterface displayOn() {
        displayControl(true, cursorVisible, cursorBlinking);
        return this;
    }

    @Override
    public LcdInterface displayOff() {
        displayControl(false, cursorVisible, cursorBlinking);
        return this;
    }

    @Override
    public LcdInterface cursorOn() {
        displayControl(displayOn, true, cursorBlinking);
        return this;
    }

    @Override
    public LcdInterface cursorOff() {
        displayControl(displayOn, false, cursorBlinking);
        return this;
    }

    @Override
    public LcdInterface blinkOn() {
        displayControl(displayOn, cursorVisible, true);
        return this;
    }

    @Override
    public LcdInterface blinkOff() {
        displayControl(displayOn, cursorVisible, false);
        return this;
    }

    @Override
    public boolean isCursorEnabled() {
        return cursorVisible;
    }

    @Override
    public boolean isBlinkEnabled() {
        return cursorBlinking;
    }

    @Override
    public LcdInterface shiftDisplayRight() {
        throw new UnsupportedOperationException("Currently not implemented - impacts display unpredictably.");
//        return this;
    }

    @Override
    public LcdInterface shiftDisplayLeft() {
        throw new UnsupportedOperationException("Currently not implemented - impacts display unpredictably.");
//        return this;
    }

    @Override
    public LcdInterface moveCursorRight() {
        writeCommand(MOVE_CONTROL | CURSOR_MOVE | MOVE_RIGHT);
        return this;
    }

    @Override
    public LcdInterface moveCursorLeft() {
        writeCommand(MOVE_CONTROL | CURSOR_MOVE | MOVE_LEFT);
        return this;
    }

    @Override
    public LcdInterface createChar(int location, byte[] charMap) {
        if (location < 0 || location > 7) {
            throw new IllegalArgumentException("Invalid location (" + location + ") , must be 0..7");
        }
        if (charMap.length != 8) {
            throw new IllegalArgumentException("Invalid charMap length (" + charMap.length + ") , must be 8");
        }

        writeCommand(SELECT_CGRAM_ADDR | location << 3);

        for (int i = 0; i < 8; i++) {
            writeSplitCommand(charMap[i], REGISTER_SELECT);
        }
        return this;
    }

    @Override
    public void close() throws RuntimeIOException {
        backlight = false;
        displayOff();
    }

    /**
     * Write a command to the LCD
     */
    private void writeCommand(int cmd) {
        writeSplitCommand(cmd, 0);
    }

    /**
     * Write a command in nibbles
     */
    private void writeSplitCommand(int cmd, int mode) {
        writeFourBits(mode | cmd & 0xF0);
        writeFourBits(mode | cmd << 4 & 0xF0);
    }

    /**
     * Write the four bits of a byte, with backlight control and enable on/off
     *
     * @param data the byte that is sent
     */
    private void writeFourBits(int data) {
        int sendData = data | (backlight ? BACKLIGHT : NO_BACKLIGHT);

        lcdConnection.write((byte) sendData);

        lcdConnection.write((byte) (sendData | ENABLE_BIT));
        sleepNanos(0, 500_000);
        lcdConnection.write((byte) (sendData & ~ENABLE_BIT));
        sleepNanos(0, 100_000);
    }

    /**
     * Execute a direct command on the display - typically for hardware control
     */
    private void executeCommand(int cmd) {
        lcdConnection.write((byte) cmd);
        sleepNanos(0, 100_000);
    }
}
