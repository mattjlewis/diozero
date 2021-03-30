#ifndef _Included_com_diozero_util
#define _Included_com_diozero_util
#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

jlong getEpochTimeMillis();
jlong getEpochTimeMillis2();
jlong getEpochTimeNanos();
jlong getJavaTimeNanos();

#ifdef __cplusplus
}
#endif
#endif
