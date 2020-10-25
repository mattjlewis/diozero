package com.diozero.internal.provider.sysfs;

// struct gpiod_line
public class GpioLine {
	private static final String GPIO_LINE_NUMBER_PATTERN = "GPIO(\\d+)";
	private static final int INVALID_GPIO_NUM = -1;
	
	public static enum Direction {
		INPUT, OUTPUT;
	}

	// Line informational flags
	// https://elixir.bootlin.com/linux/v4.9.127/source/include/uapi/linux/gpio.h#L29
	private static final int GPIOLINE_FLAG_KERNEL = (1 << 0);
	private static final int GPIOLINE_FLAG_IS_OUT = (1 << 1);
	private static final int GPIOLINE_FLAG_ACTIVE_LOW = (1 << 2);
	private static final int GPIOLINE_FLAG_OPEN_DRAIN = (1 << 3);
	private static final int GPIOLINE_FLAG_OPEN_SOURCE = (1 << 4);
	
	private int offset;
	private boolean reserved;
	private Direction direction;
	private boolean activeLow;
	private boolean openDrain;
	private boolean openSource;
	private String name;
	private String consumer;
	private int fd;
	private int gpioNum = INVALID_GPIO_NUM;
	
	public GpioLine(int offset, int flags, String name, String consumer) {
		this.offset = offset;
		this.reserved = ((flags & GPIOLINE_FLAG_KERNEL) == 0) ? false : true;
		this.direction = ((flags & GPIOLINE_FLAG_IS_OUT) == 0) ? Direction.INPUT : Direction.OUTPUT;
		this.activeLow = ((flags & GPIOLINE_FLAG_ACTIVE_LOW) == 0) ? false : true;
		this.openDrain = ((flags & GPIOLINE_FLAG_OPEN_DRAIN) == 0) ? false : true;;
		this.openSource = ((flags & GPIOLINE_FLAG_OPEN_SOURCE) == 0) ? false : true;;;
		this.name = name;
		this.consumer = consumer;
		
		if (name.matches(GPIO_LINE_NUMBER_PATTERN)) {
			gpioNum = Integer.parseInt(name.replaceAll(GPIO_LINE_NUMBER_PATTERN, "$1"));
		}
	}
	
	public boolean isGpio() {
		return gpioNum != INVALID_GPIO_NUM;
	}
	
	public int getGpioNum() {
		return gpioNum;
	}

	public int getOffset() {
		return offset;
	}

	public boolean isReserved() {
		return reserved;
	}

	public Direction getDirection() {
		return direction;
	}

	public boolean isActiveLow() {
		return activeLow;
	}

	public boolean isOpenDrain() {
		return openDrain;
	}

	public boolean isOpenSource() {
		return openSource;
	}

	public String getName() {
		return name;
	}

	public String getConsumer() {
		return consumer;
	}

	int getFd() {
		return fd;
	}

	void setFd(int fd) {
		this.fd = fd;
	}
}
