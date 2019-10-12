#include "com_diozero_ws281xj_rpiws281x_WS281x.h"

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include <clk.h>
#include <gpio.h>
#include <dma.h>
#include <pwm.h>
#include <ws2811.h>


#define TARGET_FREQ WS2811_TARGET_FREQ
#define DMA 5

ws2811_t led_string = {
	.freq = 0,
	.dmanum = 0,
	.channel = {
		[0] = {
			.gpionum = 0,
			.count = 0,
			.invert = 0,
			.brightness = 0
		}, [1] = {
			.gpionum = 0,
			.count = 0,
			.invert = 0,
			.brightness = 0
		}
	}
};

unsigned char is_closed = 0;

/* The VM calls this function upon loading the native library. */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved) {
	return JNI_VERSION_1_8;
}

/*
 * Class:     com_diozero_ws281xj_rpiws281x_WS281xNative
 * Method:    initialise
 * Signature: (IIIIIII)Ljava/nio/ByteBuffer
 */
JNIEXPORT jobject JNICALL Java_com_diozero_ws281xj_rpiws281x_WS281xNative_initialise(
		JNIEnv* env, jclass clz, jint frequency, jint dmaNum, jint gpioNum,
		jint brightness, jint numLeds, jint stripType, jint channel) {
	if (channel < 0 || channel >= RPI_PWM_CHANNELS) {
		fprintf(stderr, "Error channel must be 0..%d\n", RPI_PWM_CHANNELS - 1);
		return NULL;
	}

	led_string.freq = frequency;
	led_string.dmanum = dmaNum;
	led_string.channel[channel].gpionum = gpioNum;
	led_string.channel[channel].count = numLeds;
	led_string.channel[channel].brightness = brightness;
	led_string.channel[channel].strip_type = stripType;
	int rc = ws2811_init(&led_string);
	if (rc != 0) {
		fprintf(stderr, "ws2811_init failed: %s\n", ws2811_get_return_t_str(rc));
		return NULL;
	}

	jobject direct_buffer = (*env)->NewDirectByteBuffer(env, led_string.channel[channel].leds, numLeds*sizeof(ws2811_led_t));

	return direct_buffer;
}

/*
 * Class:     com_diozero_ws281xj_rpiws281x_WS281xNative
 * Method:    terminate
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_diozero_ws281xj_rpiws281x_WS281xNative_terminate(JNIEnv* env, jclass clz) {
	if (is_closed == 0) {
		ws2811_fini(&led_string);
	}
	is_closed = 1;
}

/*
 * Class:     com_diozero_ws281xj_rpiws281x_WS281xNative
 * Method:    render
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_diozero_ws281xj_rpiws281x_WS281xNative_render(JNIEnv* env, jclass clz) {
	return ws2811_render(&led_string);
}
