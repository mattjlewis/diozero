package com.diozero.devices.sandpit.motor;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AbstractChopperStepperMotor.java
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

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.tinylog.Logger;

import com.diozero.api.function.Action;

/**
 * Abstractions of most of the generic functionality of a stepper. Note that the "steps per rotation" is treated as a
 * <b>fixed</b> physical aspect of the motor.
 *
 * @author E. A. Graham Jr.
 */
public abstract class AbstractChopperStepperMotor extends AbstractStepperMotor implements ChopperStepperMotorInterface {
    // attributes of the motor
    protected int defaultFrequency;

    // can only execute one action at a time
    protected final AtomicBoolean runFlag = new AtomicBoolean(false);

    private final List<StepperMotorEventListener> listeners = new CopyOnWriteArrayList<>();
    // default actions that do nothing
    private Action stopAction = () -> {
    };
    private Action moveAction = () -> {
    };


    public AbstractChopperStepperMotor(int stepsPerRotation, ChopperStepperController controller) {
        super(stepsPerRotation, controller);

    }

    public AbstractChopperStepperMotor(float strideAngle, ChopperStepperController controller) {
        super(strideAngle, controller);
    }


    @Override
    public int getDefaultFrequency() {
        return defaultFrequency;
    }

    public void setDefaultFrequency(int frequencyInHz) {
        defaultFrequency = frequencyInHz;
    }

    @Override
    public void stop() {
        ((ChopperStepperController)controller).stop();
        if (runFlag.getAndSet(false)) fireEvent(false);
    }

    @Override
    public void start(Direction direction, float speed) {
        if (!runFlag.compareAndSet(false, true)) return;
        fireEvent(true);
        run(direction, speed);
    }

    /**
     * Allow implementations to do different things here.
     *
     * @param direction the direction to rotate
     * @param speed     the speed to rotate (may change the "default" speed)
     */
    protected abstract void run(Direction direction, float speed);

    /**
     * Indicates that the code thinks the axle is rotating.
     *
     * @return {@code true} if turning
     */
    public boolean isRunning() {
        return runFlag.get();
    }

    @Override
    public void addEventListener(StepperMotorEventListener listener) {
        Objects.requireNonNull(listener);
        listeners.add(listener);
    }

    @Override
    public void removeEventListener(StepperMotorEventListener listener) {
        Objects.requireNonNull(listener);
        listeners.remove(listener);
    }

    @Override
    public void onMove(Action action) {
        Objects.requireNonNull(action);
        moveAction = action;
    }

    @Override
    public void onStop(Action action) {
        Objects.requireNonNull(action);
        stopAction = action;
    }

    protected void fireEvent(boolean start) {
        // TODO run this in another thread?
        if (start) {
            moveAction.action();
        }
        else {
            stopAction.action();
        }

        if (listeners.isEmpty()) return;

        StepperMotorEvent event = new StepperMotorEvent(this, Instant.now(), start);
        for (StepperMotorEventListener listener : listeners) {
            try {
                if (start) {
                    listener.start(event);
                }
                else {
                    listener.stop(event);
                }
            }
            catch (Throwable t) {
                Logger.error(t, "Error in listener loop: start = " + start);
            }
        }
    }
}
