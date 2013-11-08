/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <jni.h>
#include <time.h>
#include <android/log.h>
#include <android/bitmap.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

#define  LOG_TAG    "libplasma"
//#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
//#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

/* Set to 1 to enable debug log traces. */
#define DEBUG 0

/* Set to 1 to optimize memory stores when generating plasma. */
#define OPTIMIZE_WRITES  1

/* Return current time in milliseconds */
static double now_ms(void)
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return tv.tv_sec*1000. + tv.tv_usec/1000.;
}

/* We're going to perform computations for every pixel of the target
 * bitmap. floating-point operations are very slow on ARMv5, and not
 * too bad on ARMv7 with the exception of trigonometric functions.
 *
 * For better performance on all platforms, we're going to use fixed-point
 * arithmetic and all kinds of tricks
 */

typedef int32_t  Fixed;

#define  FIXED_BITS           16
#define  FIXED_ONE            (1 << FIXED_BITS)
#define  FIXED_AVERAGE(x,y)   (((x) + (y)) >> 1)

#define  FIXED_FROM_INT(x)    ((x) << FIXED_BITS)
#define  FIXED_TO_INT(x)      ((x) >> FIXED_BITS)

#define  FIXED_FROM_FLOAT(x)  ((Fixed)((x)*FIXED_ONE))
#define  FIXED_TO_FLOAT(x)    ((x)/(1.*FIXED_ONE))

#define  FIXED_MUL(x,y)       (((int64_t)(x) * (y)) >> FIXED_BITS)
#define  FIXED_DIV(x,y)       (((int64_t)(x) * FIXED_ONE) / (y))

#define  FIXED_DIV2(x)        ((x) >> 1)
#define  FIXED_AVERAGE(x,y)   (((x) + (y)) >> 1)

#define  FIXED_FRAC(x)        ((x) & ((1 << FIXED_BITS)-1))
#define  FIXED_TRUNC(x)       ((x) & ~((1 << FIXED_BITS)-1))

#define  FIXED_FROM_INT_FLOAT(x,f)   (Fixed)((x)*(FIXED_ONE*(f)))

typedef int32_t  Angle;

#define  ANGLE_BITS              9

#if ANGLE_BITS < 8
#  error ANGLE_BITS must be at least 8
#endif

#define  ANGLE_2PI               (1 << ANGLE_BITS)
#define  ANGLE_PI                (1 << (ANGLE_BITS-1))
#define  ANGLE_PI2               (1 << (ANGLE_BITS-2))
#define  ANGLE_PI4               (1 << (ANGLE_BITS-3))

#define  ANGLE_FROM_FLOAT(x)   (Angle)((x)*ANGLE_PI/M_PI)
#define  ANGLE_TO_FLOAT(x)     ((x)*M_PI/ANGLE_PI)

#if ANGLE_BITS <= FIXED_BITS
#  define  ANGLE_FROM_FIXED(x)     (Angle)((x) >> (FIXED_BITS - ANGLE_BITS))
#  define  ANGLE_TO_FIXED(x)       (Fixed)((x) << (FIXED_BITS - ANGLE_BITS))
#else
#  define  ANGLE_FROM_FIXED(x)     (Angle)((x) << (ANGLE_BITS - FIXED_BITS))
#  define  ANGLE_TO_FIXED(x)       (Fixed)((x) >> (ANGLE_BITS - FIXED_BITS))
#endif

static Fixed  angle_sin_tab[ANGLE_2PI+1];

static void init_angles(void)
{
    int  nn;
    for (nn = 0; nn < ANGLE_2PI+1; nn++) {
        double  radians = nn*M_PI/ANGLE_PI;
        angle_sin_tab[nn] = FIXED_FROM_FLOAT(sin(radians));
    }
}

static __inline__ Fixed angle_sin( Angle  a )
{
    return angle_sin_tab[(uint32_t)a & (ANGLE_2PI-1)];
}

static __inline__ Fixed angle_cos( Angle  a )
{
    return angle_sin(a + ANGLE_PI2);
}

static __inline__ Fixed fixed_sin( Fixed  f )
{
    return angle_sin(ANGLE_FROM_FIXED(f));
}

static __inline__ Fixed  fixed_cos( Fixed  f )
{
    return angle_cos(ANGLE_FROM_FIXED(f));
}

