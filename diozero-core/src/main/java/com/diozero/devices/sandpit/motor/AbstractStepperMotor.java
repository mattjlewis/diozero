package com.diozero.devices.sandpit.motor;

import com.diozero.api.RuntimeIOException;

/**
 * Basic management and physical characteristics for a stepper.
 */
public abstract class AbstractStepperMotor implements StepperMotorInterface {
    protected final int stepsPerRotation;
    protected final float strideAngle;
    protected final StepperMotorController controller;

    /**
     * @param stepsPerRotation how many steps required to complete a full rotation
     * @param controller the controller
     */
    public AbstractStepperMotor(int stepsPerRotation, StepperMotorController controller) {
        this.stepsPerRotation = stepsPerRotation;
        strideAngle = 360f / stepsPerRotation;
        this.controller = controller;
    }

    /**
     * @param strideAngle how many degrees a "step" will rotate (angle = 360 / stepsPerRotation)
     * @param controller  the controller
     */
    public AbstractStepperMotor(float strideAngle, StepperMotorController controller) {
        this.strideAngle = strideAngle;
        stepsPerRotation = (int)Math.floor(360f / strideAngle);
        this.controller = controller;
    }

    @Override
    public StepperMotorController getController() {
        return controller;
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
    public void close() throws RuntimeIOException {
        controller.close();
    }
}
