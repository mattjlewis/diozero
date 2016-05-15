# LCD Displays

## I2C LCDs

*class* **com.diozero.I2CLcd**{: .descname } (*controller=1*, *deviceAddress=0x27*, *columns*, *rows*) [source](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/I2CLcd.java){: .viewcode-link } [&para;](LCDDisplays.md#i2c-lcd "Permalink to this definition"){: .headerlink }

: Supports the Hitachi HD44780 controller attached via the NCP PCF8574 I/O expansion board.

    * **controller** (*int=1*) - I2C bus controller number (defaults to 1).
    
    * **deviceAddress** (*int=0x27*) - I2C bus address (defaults to 0x27).
    
    * **columns** (*int*) - Number of columns for the LCD.
    
    * **rows** (*int*) - Number of rows for the LCD.
    
    *int* **getColumnCount** ()
    
    : Get the number of columns for the display.
    
    *int* **getRowCount** ()
    
    : Get the number of rows for the display.
    
    *boolean* **isBacklightOn** ()
    
    : Return true if the display backlight is on.
    
    **setBacklightOn** (*backlight*)
    
    : Set the display backlight on / off.
    
    * **backlight** (*boolean*) - New backlight value.
    
    **setCursorPosition** (*column*, *row*)
    
    : Set the display cursor position.
    
    * **column** (*int*) - New column value (starts at 0).
    
    * **row** (*int*) - New row value (starts at 0).
    
    **setCharacter** (*column*, *row*, *character*)
    
    : Display character at the specified position.
    
    * **column** (*int*) - Column (starts at 0).
    
    * **row** (*int*) - Row (starts at 0).
    
    * **character** (*char*) - Character to display.
    
    **setText** (*row*, *text*)
    
    : Display text on the specified row.
    
    * **row** (*int*) - Row (starts at 0).
    
    * **text** (*String*) - text to display.
    
    **addText** (*text*)
    
    : Add text at the current cursor position.
    
    * **text** (*String*) - text to display.
    
    **addText** (*character*)
    
    : Add character at the current cursor position.
    
    * **character** (*char*) - character to display.
    
    **addText** (*code*)
    
    : Add character code at the current cursor position.
    
    * **code** (*byte*) - Character code to display.
    
    **clear** ()
    
    : Clear the display and reset the cursor position.
    
    **returnHome** ()
    
    : Return the cursor to the default position.
    
    **entryModeControl** (*increment*, *shiftDisplay*)
    
    : Control the display entry mode.
    
    * **increment** (*boolean*) - The cursor or blinking moves to the right when incremented by 1 and to the left when decremented by 1
    
    * **shiftDisplay** (*boolean*) - Shifts the entire display either to the right (I/D = 0) or to the left (I/D = 1) when true. The display does not shift if false. If true, it will seem as if the cursor does not move but the display does.
    
    *boolean* **isIncrementOn** ()
    
    : Returns the current increment / decrement value.
    
    *boolean* **isShiftDisplayOn** ()
    
    : Returns the current cursor / display shift value.
    
    **displayControl** (*displayOn*, *cursorOn*, *blinkOn*)
    
    : Control the display / cursor.
    
    * **displayOn** (*boolean*) - Turn the display on or off.
    
    * **cursorEnabled** (*boolean*) - Turn the cursor on or off.
    
    * **blinkEnabled** (*boolean*) - Enable / disable cursor blinking.
    
    *boolean* **isCursorEnabled** ()
    
    : Return the current cursor display state.
    
    *boolean* **isBlinkEnabled** ()
    
    : Return the current cursor display state.
    
    **cursorOrDisplayShift** (*displayShift*, *shiftRight*)
    
    : Cursor or display shift shifts the cursor position or display to the right or left without writing or reading display data. This function is used to correct or search the display. In a 2-line display, the cursor moves to the second line when it passes the 40th digit of the first line. Note that the first and second line displays will shift at the same time. When the displayed data is shifted repeatedly each line moves only horizontally. The second line display does not shift into the first line position.
    
    * **displayShift** (*boolean*) - Shift the display if true, the cursor if false.
    
    * **shiftRight** (*boolean*) - Shift to the right if true, to the left if false.
    
    **shiftDisplayRight** ()
    
    : Shift the display to the right.
    
    **shiftDisplayLeft** ()
    
    : Shift the display to the left.
    
    **moveCursorRight** ()
    
    : Move the cursor to the right.
    
    **moveCursorLeft** ()
    
    : Move the cursor to the left.
    
    **createChar** (*location*, *charMap*)
    
    : Create a new custom character.
    
    * **location** (*int*) - Character location (0..7).
    
    * **charMap** (*byte[]*) - New character definition, array length must be 8.
