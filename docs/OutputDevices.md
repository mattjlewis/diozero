# Output Devices

## Digital LED

LED control, taken from [LEDTest](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/LEDTest.java):

```java
try (LED led = new LED(pin)) {
	led.on();
	SleepUtil.sleepSeconds(.5);
	led.off();
	SleepUtil.sleepSeconds(.5);
	led.toggle();
	SleepUtil.sleepSeconds(.5);
	led.toggle();
	SleepUtil.sleepSeconds(.5);
	led.blink(0.5f, 0.5f, 10, false);
}
```

## PWM LED

Code, taken from [PwmLedTest](https://github.com/mattjlewis/diozero/blob/master/diozero-core/src/main/java/com/diozero/sampleapps/PwmLedTest.java):

```java
float delay = 0.5f;
try (PwmLed led = new PwmLed(pin)) {
	led.on();
	SleepUtil.sleepSeconds(delay);
	led.off();
	SleepUtil.sleepSeconds(delay);
	led.toggle();
	SleepUtil.sleepSeconds(delay);
	led.toggle();
	SleepUtil.sleepSeconds(delay);
	led.setValue(.25f);
	SleepUtil.sleepSeconds(delay);
	led.toggle();
	SleepUtil.sleepSeconds(delay);
	led.setValue(.5f);
	SleepUtil.sleepSeconds(delay);
	led.blink(0.5f, 0.5f, 5, false);
	led.pulse(1, 50, 5, false);
} catch (RuntimeIOException e) {
	Logger.error(e, "Error: {}", e);
}
```

## RGB LED

## Buzzer

## PWM Output inc. servo control
