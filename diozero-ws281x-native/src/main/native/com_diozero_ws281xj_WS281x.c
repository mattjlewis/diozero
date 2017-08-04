#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include <clk.h>
#include <gpio.h>
#include <dma.h>
#include <pwm.h>
#include <ws2811.h>

#include "com_diozero_ws281xj_WS281x.h"

#define TARGET_FREQ WS2811_TARGET_FREQ
#define GPIO_PIN 18
#define DMA 5
#define LED_COUNT 60

ws2811_t led_string = {
	.freq = TARGET_FREQ,
	.dmanum = DMA,
	.channel = {
		[0] = {
			.gpionum = GPIO_PIN,
			.count = LED_COUNT,
			.invert = 0,
			.brightness = 63
		}, [1] = {
			.gpionum = 0,
			.count = 0,
			.invert = 0,
			.brightness = 0
		}
	}
};

/* The VM calls this function upon loading the native library. */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved) {
	return JNI_VERSION_1_8;
}

/* This function is called when the native library gets unloaded by the VM. */
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* jvm, void* reserved) {
	ws2811_fini(&led_string);
}

/*
 * Class:     com_diozero_ws281xj_WS281xNative
 * Method:    initialise
 * Signature: (IIIII)Ljava/nio/ByteBuffer
 */
JNIEXPORT jobject JNICALL Java_com_diozero_ws281xj_WS281xNative_initialise(
		JNIEnv* env, jclass clz, jint frequency, jint dmaNum, jint gpioNum, jint brightness, jint numLeds) {
	led_string.freq = frequency;
	led_string.dmanum = dmaNum;
	led_string.channel[0].gpionum = gpioNum;
	led_string.channel[0].count = numLeds;
	led_string.channel[0].brightness = brightness;
	int rc = ws2811_init(&led_string);
	if (rc != 0) {
		return NULL;
	}

	jobject direct_buffer = (*env)->NewDirectByteBuffer(env, led_string.channel[0].leds, numLeds*sizeof(ws2811_led_t));

	return direct_buffer;
}

/*
 * Class:     com_diozero_ws281xj_WS281xNative
 * Method:    terminate
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_diozero_ws281xj_WS281xNative_terminate(JNIEnv* env, jclass clz) {
	ws2811_fini(&led_string);
}

/*
 * Class:     com_diozero_ws281xj_WS281xNative
 * Method:    render
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_diozero_ws281xj_WS281xNative_render(JNIEnv* env, jclass clz) {
	return ws2811_render(&led_string);
}

int main(int argc, char *argv[]) {
	printf("Hello\n");
	int rc = ws2811_init(&led_string);
	if (rc != 0) {
		printf("Error %d\n", rc);
		return -1;
	}
	printf("led_string.channel[0].gpionum=%d\n", led_string.channel[0].gpionum);
	printf("led_string.channel[0].strip_type=%d\n", led_string.channel[0].strip_type);
	return 0;
}
