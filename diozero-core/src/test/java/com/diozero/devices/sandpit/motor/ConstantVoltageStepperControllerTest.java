package com.diozero.devices.sandpit.motor;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.diozero.devices.sandpit.motor.ConstantVoltageStepperController.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test basic functioning of CV controllers.
 */
public class ConstantVoltageStepperControllerTest {
    private static class TestStepperPin implements StepperPin {
        //
        List<Boolean> requests = new ArrayList<>();
        @Override
        public void setValue(boolean onOff) {
            requests.add(onOff);
        }

        @Override
        public void close() {
            // ignored
        }
    }

    private final TestStepperPin[] unipolarPins = new TestStepperPin[4];
    private AbstractConstantVoltageController unipolarController;

    private final BiPolarTerminal terminalA = new BiPolarTerminal(new TestStepperPin(), new TestStepperPin());
    private final BiPolarTerminal terminalB = new BiPolarTerminal(new TestStepperPin(), new TestStepperPin());
    private AbstractConstantVoltageController biploarController;

    @BeforeEach
    void setUp() {
        for (int i = 0; i < 4; i++) {
            unipolarPins[i] = new TestStepperPin();
        }
        unipolarController = new UnipolarCVController(unipolarPins);
        biploarController = new BipolarCVController(terminalA, terminalB);
    }

    @Test
    public void unipolarMicroStepForward() {
        unipolarController.microStepForward();
        for (int i = 0; i < 4; i++) {
            assertEquals(1, unipolarPins[i].requests.size(), "Pin " + i + " not requested.");
        }
        assertTrue(unipolarPins[3].requests.get(0), "Pin 4 did not fire");
        for (int i = 0; i < 3; i++) {
            assertFalse(unipolarPins[i].requests.get(0), "Pin " + i + " fired");
        }
    }

    @Test
    public void unipolarMicroStepBackward() {
        unipolarController.microStepBackward();
        for (int i = 0; i < 4; i++) {
            assertEquals(1, unipolarPins[0].requests.size(), "Pin " + i + " not requested.");
        }
        assertTrue(unipolarPins[0].requests.get(0), "Pin 1 did not fire");
        for (int i = 1; i < 4; i++) {
            assertFalse(unipolarPins[i].requests.get(0), "Pin " + i + " fired");
        }
    }

    @Test
    public void unipolarFullStepForward() {
        unipolarController.stepForward(1);

        for (int i = 0; i < 4; i++) {
            TestStepperPin pin = unipolarPins[i];
            assertEquals(4, pin.requests.size(), "Did not get all firings on " + i);
            switch (i) {
                case 3: assertEquals(List.of(true, false, false, false), pin.requests); break;
                case 2: assertEquals(List.of(false, true, false, false), pin.requests); break;
                case 1: assertEquals(List.of(false, false, true, false), pin.requests); break;
                case 0: assertEquals(List.of(false, false, false, true), pin.requests); break;
            }
        }
    }

    @Test
    public void unipolarFullStepBackward() {
        unipolarController.stepBackward(1);

        for (int i = 0; i < 4; i++) {
            TestStepperPin pin = unipolarPins[i];
            assertEquals(4, pin.requests.size(), "Did not get all firings on " + i);
            switch (i) {
                case 0: assertEquals(List.of(true, false, false, false), pin.requests); break;
                case 1: assertEquals(List.of(false, true, false, false), pin.requests); break;
                case 2: assertEquals(List.of(false, false, true, false), pin.requests); break;
                case 3: assertEquals(List.of(false, false, false, true), pin.requests); break;
            }
        }
    }

    @Test
    public void uniploarTwoForward() {
        unipolarController.stepForward(1);
        unipolarController.stepForward(1);

        for (int i = 0; i < 4; i++) {
            TestStepperPin pin = unipolarPins[i];
            assertEquals(8, pin.requests.size(), "Did not get all firings on " + i);
            switch (i) {
                case 3: assertEquals(List.of(true, false, false, false,true, false, false, false), pin.requests); break;
                case 2: assertEquals(List.of(false, true, false, false,false, true, false, false), pin.requests); break;
                case 1: assertEquals(List.of(false, false, true, false,false, false, true, false), pin.requests); break;
                case 0: assertEquals(List.of(false, false, false, true,false, false, false, true), pin.requests); break;
            }
        }
    }

    @Test
    public void bipolarMicroStepForward() {
        biploarController.microStepForward();

        assertTrue(((TestStepperPin)terminalB.plus).requests.get(0));
        assertFalse(((TestStepperPin)terminalA.minus).requests.get(0));
        assertFalse(((TestStepperPin)terminalB.minus).requests.get(0));
        assertFalse(((TestStepperPin)terminalA.plus).requests.get(0));
    }
    @Test
    public void bipolarMicroStepBackward() {
        biploarController.microStepBackward();

        assertFalse(((TestStepperPin)terminalB.plus).requests.get(0));
        assertFalse(((TestStepperPin)terminalA.minus).requests.get(0));
        assertFalse(((TestStepperPin)terminalB.minus).requests.get(0));
        assertTrue(((TestStepperPin)terminalA.plus).requests.get(0));
    }

    @Test
    public void bipolarTwoBackward() {
        biploarController.stepBackward(1);
        biploarController.stepBackward(1);

        assertEquals(List.of(true, false, false, false,true, false, false, false),
                     ((TestStepperPin)terminalA.plus).requests);
        assertEquals(List.of(false, true, false, false,false, true, false, false),
                     ((TestStepperPin)terminalB.minus).requests);
        assertEquals(List.of(false, false, true, false, false, false, true, false),
                     ((TestStepperPin)terminalA.minus).requests);
        assertEquals(List.of(false, false, false, true,false, false, false, true),
                     ((TestStepperPin)terminalB.plus).requests);
    }
}
