package com.diozero.devices.oled;

import java.util.List;

/**
 * This is a "paged" display, so each image written is several updates. The internal RAM is typically 132 bytes per
 * page row.
 */
public class SH1106 extends MonochromeSsdOled {
    static final List<byte[]> _INIT_SEQUENCE = List.of(
            new byte[] { DISPLAY_CLOCK_DIV_OSC_FREQ, (byte)0x80 }, // divide ratio/oscillator: divide by 2, fOsc (POR)}
            new byte[] { SET_MULTIPLEX_RATIO, (byte)0x3f }, // multiplex ratio = 64 (POR)
            new byte[] { SET_DISPLAY_OFFSET, (byte)0x00 }, // set display offset mode = 0x0
            new byte[] { SET_DISPLAY_START_LINE_0 }, // set start line
            new byte[] { SET_IREF_INTERNAL, (byte)0x8b }, // turn on DC/DC
            new byte[] { SET_SEGMENT_REMAP_ON }, // segment remap = 1 (POR=0, down rotation)
            new byte[] { COM_OUTPUT_SCAN_DIR_REMAPPED }, // scan decrement
            new byte[] { SET_COM_PINS_HW_CONFIG, (byte)0x12 }, // set com pins
            new byte[] { SET_CONTRAST, (byte)0xff }, // contrast setting = 0xff
            new byte[] { SET_PRECHARGE_PERIOD, (byte)0x1f }, // pre-charge/dis-charge period mode: 2 DCLKs/2 DCLKs (POR)
            new byte[] { SET_VCOMH_DESELECT_LEVEL, (byte)0x40 }, // VCOM deselect level = 0.770 (POR)
            new byte[] { SET_MEMORY_ADDR_MODE, (byte)0x20 }, //
            new byte[] { (byte)0x33 },  // turn on VPP to 9V
            new byte[] { NORMAL_DISPLAY }, // normal (not reversed) display
            new byte[] { RESUME_TO_RAM_CONTENT_DISPLAY } // entire display off, retain RAM, normal status (POR)
    );
    private final byte rowOffset;

    /**
     * Most common
     * @param device the connection
     */
    public SH1106(SsdOledCommunicationChannel device) {
        this(device,DEFAULT_WIDTH, Height.TALL.lines);
    }

    /**
     * Constructor for variables sizes.
     * @param device the connection
     * @param width the width of the display
     * @param height the height of the display
     */
    public SH1106(SsdOledCommunicationChannel device, int width, int height) {
        super(device, width, height);
        // because this driver actually has 132 ram bytes, offset by 2 to get the display "centered"
        if (width == DEFAULT_WIDTH) rowOffset = 2;
        else rowOffset = 0;
    }

    /**
     * Constructor for variables sizes and "centering" of the display (e.g. 132 bytes RAM, but 128 display)
     *
     * @param device the connection
     * @param width the width of the display
     * @param height the height of the display
     */
    public SH1106(SsdOledCommunicationChannel device, int width, int height, byte rowOffset) {
        super(device, width, height);
        this.rowOffset = rowOffset;
    }

    @Override
    protected void init() {
        setDisplayOn(false);
        _INIT_SEQUENCE.forEach((this::command));
        setDisplayOn(true);
    }

    @Override
    protected void goTo(int x, int y) {
        throw new UnsupportedOperationException("Not currently supporred in this class of display.");
    }

    /**
     * {@inheritDoc}
     * <p>>
     * Over-ridden for page mode. This <i>may</i> be more generic and thus can be moved to the super class.
     * </p>
     */
    public void show() {
        byte columnStart = (byte)(SET_LOWER_COLUMN_START_ADDR | rowOffset);

        for (int p = 0; p < pages; p++) {
            byte selectRow = (byte)(SET_PAGE_START_ADDR | p);

            device.sendCommand(SET_HIGHER_COLUMN_START_ADDR);
            device.sendCommand(columnStart);
            device.sendCommand(selectRow);
            device.sendData(getBuffer(), width * p, width);
        }
    }
}
