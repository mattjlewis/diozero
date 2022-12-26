package com.diozero.devices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for basic text-handling in LCD interfaces.
 */
class LcdInterfaceTest {
    LcdInterface mockInterface = mock(LcdInterface.class);

    List<Character> textOutputPerRow = new ArrayList<>();
    List<String> textOutput = new ArrayList<>();

    String hexChars = "0123456789ABCDEF";

    @BeforeEach
    void setUp() {
        textOutputPerRow.clear();
        textOutput.clear();

        when(mockInterface.addText(anyChar())).thenAnswer((Answer<LcdInterface>) invoked -> {
            textOutputPerRow.add(invoked.getArgument(0));
            return mockInterface;
        });
        // the assumption is if the curos position is being set, move the previous data off as a single "line" of text
        when(mockInterface.setCursorPosition(anyInt(), anyInt())).thenAnswer((Answer<LcdInterface>) invoked -> {
            // if there's no text, do not capture
            if (! textOutputPerRow.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                textOutputPerRow.forEach(sb::append);
                textOutputPerRow.clear();
                textOutput.add(sb.toString());
            }
            return mockInterface;
        });
        // cheating...
        when(mockInterface.returnHome()).thenAnswer((Answer<LcdInterface>) invoked -> mockInterface.setCursorPosition(0,0));
        when(mockInterface.setText(anyInt(), anyString())).thenCallRealMethod();
        when(mockInterface.addText(anyString())).thenCallRealMethod();
        when(mockInterface.displayText(anyString())).thenCallRealMethod();
        when(mockInterface.getColumnCount()).thenReturn(16);
        when(mockInterface.getRowCount()).thenReturn(2);
    }

    @Test
    public void basicSingleCharacterOutput() {
        for (byte character : hexChars.getBytes()) {
            mockInterface.addText((char)character);
        }
        mockInterface.returnHome(); // trigger capture
        assertEquals(hexChars, textOutput.get(0));
    }

    @Test
    public void basicTwoLineOutput() {
        String secondLine = "Hello, World!!!!";
        mockInterface.setText(0, hexChars);
        mockInterface.setText(1, secondLine);
        mockInterface.returnHome(); // trigger capture

        assertEquals(hexChars, textOutput.get(0));
        assertEquals(secondLine, textOutput.get(1));
    }

    @Test
    public void truncateString() {
        mockInterface.setText(0, hexChars + " This is the end.");
        mockInterface.returnHome(); // trigger capture

        assertEquals(hexChars, textOutput.get(0));
    }

    @Test
    public void padString() {
        String beginning = "Beginning";
        mockInterface.setText(0, beginning);
        mockInterface.returnHome(); // trigger capture

        assertTrue(textOutput.get(0).startsWith(beginning));
        assertEquals(beginning + "       ", textOutput.get(0));
    }

    @Test
    public void displayTextWithNewlines() {
        mockInterface.displayText("Hello,\nWorld!");
        mockInterface.returnHome(); // trigger capture

        assertTrue(textOutput.get(0).startsWith("Hello,"));
        assertTrue(textOutput.get(1).startsWith("World!"));
    }
}
