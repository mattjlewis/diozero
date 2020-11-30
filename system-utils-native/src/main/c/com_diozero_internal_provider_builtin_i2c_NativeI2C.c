/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Native System Utilities
 * Filename:     com_diozero_internal_provider_builtin_i2c_NativeI2C.c
 *
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>

#include <fcntl.h>
#include <sys/stat.h>
#include <sys/ioctl.h>

#if defined(__linux__)
#include <linux/i2c-dev.h>
#include <i2c/smbus.h>
#else
#define I2C_SLAVE 0
#define I2C_SLAVE_FORCE 0
#define I2C_FUNCS 0
#endif

#include "com_diozero_internal_provider_builtin_i2c_NativeI2C.h"
#include <jni.h>

#if __WORDSIZE == 32
#define long_t uint32_t
#else
#define long_t uint64_t
#endif

int selectSlave(int fd, int deviceAddress, uint8_t force) {
	int rc = ioctl(fd, force ? I2C_SLAVE_FORCE : I2C_SLAVE, deviceAddress);
	if (rc < 0) {
		perror("EI2C rror in selectSlave");
		return -errno;
	}

	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_getFuncs(
		JNIEnv* env, jclass clz, jint fd) {
	uint32_t funcs;
	int rc = ioctl(fd, I2C_FUNCS, &funcs);
	if (rc < 0) {
		perror("I2C Error in getFuncs");
		return -errno;
	}

	return funcs;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_selectSlave(
		JNIEnv* env, jclass clz, jint fd, jint deviceAddress, jboolean force) {
	return selectSlave(fd, deviceAddress, force == JNI_TRUE);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_smbusOpen(
		JNIEnv* env, jclass clz, jstring i2cAdapter, jint deviceAddress, jboolean force) {
	jboolean is_copy;
	const char* device = (*env)->GetStringUTFChars(env, i2cAdapter, &is_copy);

	//int fd = open(device, O_RDWR | O_NONBLOCK);
	int fd = open(device, O_RDWR);
	(*env)->ReleaseStringUTFChars(env, i2cAdapter, device);
	if (fd < 0) {
		perror("I2C Error in smbusOpen");
		return -errno;
	}

	int rc = selectSlave(fd, deviceAddress, force == JNI_TRUE);
	if (rc < 0) {
		close(fd);
		return rc;
	}

	return fd;
}

JNIEXPORT void JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_smbusClose(
		JNIEnv* env, jclass clz, jint fd) {
	close(fd);
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_writeQuick(
		JNIEnv* env, jclass clz, jint fd, jbyte bit) {
	int rc = i2c_smbus_write_quick(fd, bit);
	if (rc < 0) {
		perror("I2C Error in i2c_smbus_write_quick");
		return -errno;
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_readByte(
		JNIEnv* env, jclass clz, jint fd) {
	int rc = i2c_smbus_read_byte(fd);
	if (rc < 0) {
		perror("I2C Error in i2c_smbus_read_byte");
		return -errno;
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_writeByte(
		JNIEnv* env, jclass clz, jint fd, jbyte value) {
	int rc = i2c_smbus_write_byte(fd, value);
	if (rc < 0) {
		perror("I2C Error in i2c_smbus_write_byte");
		return -errno;
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_readBytes(
		JNIEnv* env, jclass clz, jint fd, jint rxLength, jbyteArray rxData) {
	jboolean is_copy;
	jbyte* rx_buf = (*env)->GetByteArrayElements(env, rxData, &is_copy);

	int rc = read(fd, (uint8_t*) rx_buf, rxLength);

	// mode = 0 : copy back the content and free the elems buffer
	(*env)->ReleaseByteArrayElements(env, rxData, rx_buf, 0);

	if (rc < 0) {
		perror("I2C Error in readBytes");
		return -errno;
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_writeBytes(
		JNIEnv* env, jclass clz, jint fd, jint txLength, jbyteArray txData) {
	jboolean is_copy;
	jbyte* tx_buf = (*env)->GetByteArrayElements(env, txData, &is_copy);

	int rc = write(fd, tx_buf, txLength);

	// mode = JNI_ABORT as there were no changes made
	(*env)->ReleaseByteArrayElements(env, txData, tx_buf, JNI_ABORT);

	if (rc < 0) {
		perror("I2C Error in writeBytes");
		return -errno;
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_readByteData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress) {
	int rc = i2c_smbus_read_byte_data(fd, registerAddress);

	if (rc < 0) {
		perror("I2C Error in i2c_smbus_read_byte_data");
		return -errno;
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_writeByteData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jbyte value) {
	int rc = i2c_smbus_write_byte_data(fd, registerAddress, value);

	if (rc < 0) {
		perror("I2C Error in i2c_smbus_write_byte_data");
		return -errno;
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_readWordData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress) {
	int rc = i2c_smbus_read_word_data(fd, registerAddress);

	if (rc < 0) {
		perror("I2C Error in i2c_smbus_read_word_data");
		return -errno;
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_writeWordData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jshort value) {
	int rc = i2c_smbus_write_word_data(fd, registerAddress, value);

	if (rc < 0) {
		perror("I2C Error in i2c_smbus_write_word_data");
		return -errno;
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_processCall(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jshort value) {
	int rc = i2c_smbus_process_call(fd, registerAddress, value);

	if (rc < 0) {
		perror("I2C Error in i2c_smbus_process_call");
		return -errno;
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_readBlockData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jbyteArray rxData) {
	jboolean is_copy;
	jbyte* rx_buf = (*env)->GetByteArrayElements(env, rxData, &is_copy);

	int rc = i2c_smbus_read_block_data(fd, registerAddress, (uint8_t*) rx_buf);

	(*env)->ReleaseByteArrayElements(env, rxData, rx_buf, 0);

	if (rc < 0) {
		perror("I2C Error in i2c_smbus_read_block_data");
		return -errno;
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_writeBlockData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jint txLength, jbyteArray txData) {
	jboolean is_copy;
	jbyte* tx_buf = (*env)->GetByteArrayElements(env, txData, &is_copy);

	int rc = i2c_smbus_write_block_data(fd, registerAddress, txLength, (uint8_t*) tx_buf);

	(*env)->ReleaseByteArrayElements(env, txData, tx_buf, JNI_ABORT);

	if (rc < 0) {
		perror("I2C Error in i2c_smbus_write_block_data");
		return -errno;
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_blockProcessCall(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jint txLength, jbyteArray txData, jbyteArray rxData) {
	jboolean is_copy;
	jbyte* rx_buf = (*env)->GetByteArrayElements(env, rxData, &is_copy);
	jbyte* tx_buf = (*env)->GetByteArrayElements(env, txData, &is_copy);

	int rc = i2c_smbus_block_process_call(fd, registerAddress, txLength, (uint8_t*) tx_buf);

	(*env)->ReleaseByteArrayElements(env, txData, tx_buf, JNI_ABORT);

	if (rc < 0) {
		perror("I2C Error in i2c_smbus_block_process_call");

		(*env)->ReleaseByteArrayElements(env, rxData, rx_buf, JNI_ABORT);

		return -errno;
	}

	memcpy(rx_buf, tx_buf, rc);

	(*env)->ReleaseByteArrayElements(env, rxData, rx_buf, 0);

	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_readI2CBlockData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jint rxLength, jbyteArray rxData) {
	jboolean is_copy;
	jbyte* rx_buf = (*env)->GetByteArrayElements(env, rxData, &is_copy);

	int rc = i2c_smbus_read_i2c_block_data(fd, registerAddress, rxLength, (uint8_t*) rx_buf);

	(*env)->ReleaseByteArrayElements(env, rxData, rx_buf, 0);

	if (rc < 0) {
		perror("I2C Error in i2c_smbus_read_i2c_block_data");
		return -errno;
	}
	return rc;
}

JNIEXPORT jint JNICALL Java_com_diozero_internal_provider_builtin_i2c_NativeI2C_writeI2CBlockData(
		JNIEnv* env, jclass clz, jint fd, jint registerAddress, jint txLength, jbyteArray txData) {
	jboolean is_copy;
	jbyte* tx_buf = (*env)->GetByteArrayElements(env, txData, &is_copy);

	int rc = i2c_smbus_write_i2c_block_data(fd, registerAddress, txLength, (uint8_t*) tx_buf);

	(*env)->ReleaseByteArrayElements(env, txData, tx_buf, JNI_ABORT);

	if (rc < 0) {
		perror("I2C Error in i2c_smbus_write_i2c_block_data");
		return -errno;
	}
	return rc;
}
