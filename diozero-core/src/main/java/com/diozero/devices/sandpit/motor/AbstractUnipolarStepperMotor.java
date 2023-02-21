package com.diozero.devices.sandpit.motor;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AbstractUnipolarStepperMotor.java
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

import java.util.concurrent.Future;
import java.util.function.Consumer;

import com.diozero.util.DiozeroScheduler;

import static com.diozero.util.SleepUtil.NS_IN_SEC;

/**
 * A unipolar driver is handled by sending timed on/off to the appropriate pins. This class handles most of the math,
 * execution, and timing, for sending steps to the motor.
 *
 * @author E. A. Graham Jr.
 */
public abstract class AbstractUnipolarStepperMotor extends AbstractStepperMotor {

    private long pausePerStep;
    private Consumer<Long> stepForward;
    private Consumer<Long> stepBackward;
    private Future<?> running;

    /**
     * Constructor.
     *
     * @param strideAngle the degrees a step will rotate - this is <b>REQUIRED</b>
     * @param controller  the controller
     */
    protected AbstractUnipolarStepperMotor(float strideAngle, UnipolarStepperController controller) {
        super(strideAngle, controller);
    }

    protected AbstractUnipolarStepperMotor(int stepsPerRotation, UnipolarStepperController controller) {
        super(stepsPerRotation, controller);
        stepForward = controller::stepForward;
        stepBackward = controller::stepBackward;
    }

    @Override
    public void setDefaultFrequency(int frequencyInHz) {
        super.setDefaultFrequency(frequencyInHz);
        pausePerStep = Math.round((1.0 / defaultFrequency) * NS_IN_SEC);
    }

    @Override
    public void step(Direction direction) {
        ((direction.getDirection() < 0) ? stepBackward : stepForward).accept(pausePerStep);
    }

    @Override
    public void rotate(float angle, float speed) {
        if (!runFlag.getAndSet(true)) return;

        Consumer<Long> stepFunction = (angle < 0) ? stepBackward : stepForward;
        int steps = Math.round((Math.abs(angle) * stepsPerRotation / 360f));
        long pause = calculateStepPauseFromVelocity(speed);

        // notify stuff
        fireEvent(true);

        while ((steps--) > 0) {
            if (!runFlag.get()) break;
            stepFunction.accept(pause);
        }
        runFlag.set(false);

        // notify stuff
        fireEvent(false);
    }

    @Override
    public void stop() {
        super.stop();
        // the boolean run-flag is intended to "protect" the state, so it doesn't matter if this is called multiple
        // times - or even if never started
        if (running != null) {
            running.cancel(true);
            running = null;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because the timing functions are software-controlled, run a background thread.
     */
    protected void run(Direction direction, float speed) {
        Consumer<Long> stepFunction = (direction.getDirection() < 0) ? stepBackward : stepForward;
        long pause = calculateStepPauseFromVelocity(speed);

        running = DiozeroScheduler.getNonDaemonInstance().submit(() -> {
            while (runFlag.get()) {
                stepFunction.accept(pause);
            }
        });

    }

    /**
     * Calculate how long a "pause" between steps is necessary for this velocity (degrees/second). This does
     * <b>NOT</b> check to see if the maximum velocity of the motor is exceeded.
     *
     * @param velocity the desired velocity
     * @return the pause in nanoseconds between steps for this velocity
     */
    protected long calculateStepPauseFromVelocity(float velocity) {
        int hertz = rpmToFrequency(velocity);
        return Math.round((1. / hertz) * NS_IN_SEC);
    }
}
