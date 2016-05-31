#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/file.h>
#include <jni.h>

#include "GpioMMapTest.h"

// ARMv6
#define pi_peri_phys_armv6 0x20000000;
// ARMv7
#define pi_peri_phys_armv7 0x3F000000;

#define SYST_BASE_OFFSET  0x00003000
#define GPIO_TIMER_OFFSET 0x0000B000
#define DMA_BASE_OFFSET   0x00007000
#define GPIO_PADS_OFFSET  0x00100000
#define CLK_BASE_OFFSET   0x00101000
#define GPIO_BASE_OFFSET  0x00200000
#define PCM_BASE_OFFSET   0x00203000
#define SPI_BASE_OFFSET   0x00204000
#define PWM_BASE_OFFSET   0x0020C000
#define AUX_BASE_OFFSET   0x00215000
#define DMA15_BASE_OFFSET 0x00E05000

#define GPIO_LEN  0xB4


#define GPSET0 7
#define GPSET1 8

#define GPCLR0 10
#define GPCLR1 11

#define GPLEV0 13
#define GPLEV1 14

#define GPPUD     37
#define GPPUDCLK0 38
#define GPPUDCLK1 39

#define PI_BANK (gpio>>5)
#define PI_BIT  (1<<(gpio&0x1F))

#define PI_OFF 0
#define PI_ON 1

#define PI_CLEAR 0
#define PI_SET 1

#define PI_LOW 0
#define PI_HIGH 1

#define PI_INPUT 0
#define PI_OUTPUT 1

#define PI_ALT0   4
#define PI_ALT1   5
#define PI_ALT2   6
#define PI_ALT3   7
#define PI_ALT4   3
#define PI_ALT5   2

#define	PAGE_SIZE		(4*1024)
#define	BLOCK_SIZE		(4*1024)

static volatile uint32_t piPeriBase;
static volatile uint32_t gpioPads;
static volatile uint32_t gpioClockBase;
static volatile uint32_t gpioBase;
static volatile uint32_t gpioTimer;
static volatile uint32_t gpioPwm;

static int fdMem = -1;
static uint32_t *gpioAddr = MAP_FAILED;

// gpioToGPFSEL:
//	Map a BCM_GPIO pin to it's Function Selection
//	control port. (GPFSEL 0-5)
//	Groups of 10 - 3 bits per Function - 30 bits per port
// = gpio / 10

static uint8_t gpioToGPFSEL [] =
{
  0,0,0,0,0,0,0,0,0,0,
  1,1,1,1,1,1,1,1,1,1,
  2,2,2,2,2,2,2,2,2,2,
  3,3,3,3,3,3,3,3,3,3,
  4,4,4,4,4,4,4,4,4,4,
  5,5,5,5,5,5,5,5,5,5,
} ;


// gpioToShift
//	Define the shift up for the 3 bits per pin in each GPFSEL port
// = (gpio % 10) * 3

static uint8_t gpioToShift [] =
{
  0,3,6,9,12,15,18,21,24,27,
  0,3,6,9,12,15,18,21,24,27,
  0,3,6,9,12,15,18,21,24,27,
  0,3,6,9,12,15,18,21,24,27,
  0,3,6,9,12,15,18,21,24,27,
  0,3,6,9,12,15,18,21,24,27,
} ;

// gpioToGPSET:
//	(Word) offset to the GPIO Set registers for each GPIO pin

static uint8_t gpioToGPSET [] =
{
   7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
   8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
} ;

// gpioToGPCLR:
//	(Word) offset to the GPIO Clear registers for each GPIO pin

static uint8_t gpioToGPCLR [] =
{
  10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,10,
  11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,11,
} ;


// gpioToGPLEV:
//	(Word) offset to the GPIO Input level registers for each GPIO pin

static uint8_t gpioToGPLEV [] =
{
  13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,13,
  14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,14,
} ;

/* Java VM interface */
static JavaVM* globalJavaVM = NULL;

int getPinMode(unsigned int gpioPin) {
	int fSel = gpioToGPFSEL[gpioPin];
	int shift = gpioToShift[gpioPin];

	return (gpioAddr[fSel] >> shift) & 7;
}

void setPinMode(unsigned int gpioPin, unsigned int mode) {
	int fSel = gpioToGPFSEL[gpioPin];
	int shift = gpioToShift[gpioPin];

	if (mode == PI_INPUT) {
		*(gpioAddr + fSel) = (*(gpioAddr + fSel) & ~(7 << shift)); // Sets bits to zero = input
	} else if (mode == PI_OUTPUT) {
		*(gpioAddr + fSel) = (*(gpioAddr + fSel) & ~(7 << shift)) | (1 << shift);
	}
}

int gpioRead(unsigned int gpioPin) {
	if ((*(gpioAddr + gpioToGPLEV[gpioPin]) & (1 << (gpioPin & 31))) != 0) {
		return PI_HIGH;
	} else {
		return PI_LOW;
	}
}

void gpioWrite(unsigned int gpioPin, unsigned int level) {
	if (level == PI_LOW) {
		*(gpioAddr + gpioToGPCLR[gpioPin]) = 1 << (gpioPin & 31);
	} else {
		*(gpioAddr + gpioToGPSET[gpioPin]) = 1 << (gpioPin & 31);
	}
}

JavaVM* getGlobalJavaVM() {
	return globalJavaVM;
}

/* The VM calls this function upon loading the native library. */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved) {
	printf("JNI_OnLoad()\n");
	globalJavaVM = jvm;

	return JNI_VERSION_1_8;
}

/*
 * Class:     GpioMMapTest
 * Method:    initialise
 * Signature: ()Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_GpioMMapTest_initialise
  (JNIEnv *env, jclass clz) {

	int useGpioMem = 1;

	if (useGpioMem) {
		piPeriBase = 0;
		fdMem = open("/dev/gpiomem", O_RDWR | O_SYNC | O_CLOEXEC);
		//fdMem = open("/dev/fb0", O_RDWR | O_SYNC | O_CLOEXEC);
	} else {
		piPeriBase = pi_peri_phys_armv7;
		//piPeriBase = 0;
		fdMem = open("/dev/mem", O_RDWR | O_SYNC | O_CLOEXEC);
	}
	if (fdMem < 0) {
		printf("You don't have permission to run this program, try running as root\n");
		return NULL;
	}

	// Set the offsets into the memory interface.
	gpioPads = piPeriBase + GPIO_PADS_OFFSET;
	gpioClockBase = piPeriBase + CLK_BASE_OFFSET;
	//gpioBase = piPeriBase + GPIO_BASE_OFFSET;
	gpioBase = piPeriBase;
	gpioTimer = piPeriBase + GPIO_TIMER_OFFSET;
	gpioPwm = piPeriBase + PWM_BASE_OFFSET;

	printf("gpioBase=0x%x, GPIO_LEN=0x%x\n", gpioBase, GPIO_LEN);
	//gpioAddr = mmap(0, GPIO_LEN, PROT_READ|PROT_WRITE|PROT_EXEC, MAP_SHARED|MAP_LOCKED, fdMem, GPIO_BASE);
	gpioAddr = (uint32_t *)mmap(NULL, BLOCK_SIZE, PROT_READ|PROT_WRITE, MAP_SHARED, fdMem, gpioBase);
	//gpioAddr = (uint32_t *)mmap(NULL, BLOCK_SIZE, PROT_READ|PROT_WRITE, MAP_SHARED, fdMem, 0);
	printf("gpioAddr=%p\n", (void*)gpioAddr);

	if (gpioAddr == MAP_FAILED) {
		printf("mmap failed\n");
		return NULL;
	}

	if (env == NULL) {
		return NULL;
	}

	//jobject direct_buffer = (*env)->NewDirectByteBuffer(env, gpioAddr, GPIO_LEN);
	jobject direct_buffer = (*env)->NewDirectByteBuffer(env, gpioAddr, BLOCK_SIZE);

	return direct_buffer;
}

JNIEXPORT void JNICALL Java_GpioMMapTest_test
  (JNIEnv *env, jclass clz) {
	int i;
	for (i=0; i<20; i++) {
		printf("&gpioAddr[%d]=%p, gpioAddr[%d]=0x%x\n", i, (void*)&gpioAddr[i], i, gpioAddr[i]);
	}
}

/*
 * Class:     GpioMMapTest
 * Method:    terminate
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_GpioMMapTest_terminate
  (JNIEnv *env, jclass clz) {
	munmap((void *)gpioAddr, GPIO_LEN);
	close(fdMem);
}

int main(int argc, char *argv[]) {
	Java_GpioMMapTest_initialise(NULL, NULL);
	if (gpioAddr == MAP_FAILED) {
		printf("Error in initialise()\n");
		return -1;
	}

	printf("gpioAddr=%p\n", (void*)gpioAddr);
	int i;
	for (i=0; i<20; i++) {
		printf("&gpioAddr[%d]=%p, gpioAddr[%d]=0x%x\n", i, (void*)&gpioAddr[i], i, gpioAddr[i]);
	}

	int gpio_pin = 12;

	int reg = gpio_pin / 10;
	int shift = (gpio_pin % 10) * 3;

	int old_reg_val = gpioAddr[reg];
	int old_mode = (gpioAddr[reg] >> shift) & 7;
	int old_mode2 = (*(gpioAddr + reg) >> shift) & 7;
	printf("reg=%d, shift=%d, old_reg_val=0x%x, old_mode=0x%x, old_mode2=0x%x\n", reg, shift, old_reg_val, old_mode, old_mode2);

	if (1) {
		printf("Forcing quit\n");
		Java_GpioMMapTest_terminate(NULL, NULL);
		return -1;
	}

	printf("Setting pin mode...\n");
	setPinMode(gpio_pin, PI_OUTPUT);
	sleep(1);
	printf("Setting high...\n");
	gpioWrite(gpio_pin, PI_HIGH);
	sleep(1);
	printf("Setting low...\n");
	gpioWrite(gpio_pin, PI_LOW);
	sleep(1);

	printf("Done.\n");
	Java_GpioMMapTest_terminate(NULL, NULL);

	return 0;
}
