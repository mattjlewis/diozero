package com.diozero.devices.sandpit.motor;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AbstractStepperMotor.java
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.diozero.api.RuntimeIOException;
import com.diozero.api.function.Action;
import com.diozero.util.SleepUtil;

import static com.diozero.util.SleepUtil.NS_IN_SEC;

/**
 * Handles most of the math, execution, and timing, for sending steps to the motor.
 */
public abstract class AbstractStepperMotor implements StepperMotorInterface {
    private final List<StepperMotorEventListener> listeners = new CopyOnWriteArrayList<>();
    private Action stopAction = () -> {
    };
    private Action moveAction = () -> {
    };

    // can only execute one action at a time
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> running;
    private final AtomicBoolean runFlag = new AtomicBoolean(false);
    private final int stepsPerRotation;
    private final float strideAngle;
    protected final BasicStepperController controller;
    private int defaultFrequency;
    private long pausePerStep;

    /**
     * Constructor.
     *
     * @param strideAngle the degrees a step will rotate - this is <b>REQUIRED</b>
     * @param controller  the controller
     */
    protected AbstractStepperMotor(float strideAngle, BasicStepperController controller) {
        this.strideAngle = strideAngle;
        this.controller = controller;
        stepsPerRotation = (int)Math.ceil(360.0 / strideAngle);
    }

    protected AbstractStepperMotor(int stepsPerRotation, BasicStepperController controller) {
        this.stepsPerRotation = stepsPerRotation;
        this.controller = controller;
        strideAngle = stepsPerRotation / 360f;
    }

    @Override
    public float getStrideAngle() {
        return strideAngle;
    }

    @Override
    public long getStepsPerRotation() {
        return stepsPerRotation;
    }

    @Override
    public float getDefaultSpeed() {
        return frequencyToRpm(defaultFrequency);
    }

    @Override
    public void setDefaultSpeed(float speed) {
        setDefaultFrequency(rpmToFrequency(speed));
    }

    @Override
    public int getDefaultFrequency() {
        return defaultFrequency;
    }

    @Override
    public void setDefaultFrequency(int frequencyInHz) {
        defaultFrequency = frequencyInHz;
        pausePerStep = Math.round((1.0 / defaultFrequency) * NS_IN_SEC);
    }

    @Override
    public void step(Direction direction) {
        getStepFunction(direction).accept(pausePerStep);
    }

    @Override
    public void stop() {
        runFlag.set(false);
        while (running != null && !running.isDone()) {
            SleepUtil.busySleep(2 * pausePerStep);
            running.cancel(true);
        }

        running = null;
        controller.stop();
    }

    /**
     * Indicates that the code thinks the axle is rotating.
     *
     * @return {@code true} if turning
     */
    public boolean isRunning() {
        return runFlag.get();
    }

    @Override
    public void rotate(float angle, float speed) {
        if (!runFlag.compareAndSet(false, true)) return;

        Consumer<Long> stepFunction = getStepFunction(angle);
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
    public void rotate(float angle, float maxSpeed, Duration accelerationTime) {
        throw new UnsupportedOperationException("Not yet");
    }

    @Override
    public void start(Direction direction, float speed) {
        if (!runFlag.compareAndSet(false, true)) return;

        running = executor.submit(() -> {
            Consumer<Long> stepFunction = getStepFunction(direction);
            long pause = calculateStepPauseFromVelocity(speed);

            // notify stuff
            fireEvent(true);

            while (runFlag.get()) {
                stepFunction.accept(pause);
            }
            runFlag.set(false);

            // notify stuff
            fireEvent(false);
        });
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

    @Override
    public void close() throws RuntimeIOException {
        stop();
        controller.close();
        executor.shutdownNow();
    }

    /**
     * Get the consumer for this direction.
     *
     * @param direction ibid
     * @return which step function to execute
     */
    protected Consumer<Long> getStepFunction(Direction direction) {
        if (direction == Direction.COUNTERCLOCKWISE) return controller::stepBackward;
        return controller::stepForward;
    }

    /**
     * Get the consumer for this angle, negative being counter-clockwise.
     *
     * @param angle ibid
     * @return which step function to execute
     */
    protected Consumer<Long> getStepFunction(float angle) {
        if (angle < 0) return controller::stepBackward;
        return controller::stepForward;
    }

    protected void fireEvent(boolean start) {
        if (start) {
            moveAction.action();
        }
        else {
            stopAction.action();
        }

        if (listeners.isEmpty()) return;

        StepperMotorEvent event = new StepperMotorEvent(this, Instant.now(), start);
        for (StepperMotorEventListener listener : listeners) {
            if (start) {
                listener.start(event);
            }
            else {
                listener.stop(event);
            }
        }
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
