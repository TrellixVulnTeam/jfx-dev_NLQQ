/*
 * Copyright (C) 2010 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"

#if ENABLE(DEVICE_ORIENTATION)

#include "JSDeviceOrientationEvent.h"

#include "DeviceOrientationData.h"
#include "DeviceOrientationEvent.h"
#include <runtime/JSCJSValueInlines.h>
#include <runtime/StructureInlines.h>

using namespace JSC;

namespace WebCore {

JSValue JSDeviceOrientationEvent::alpha(ExecState&) const
{
    DeviceOrientationEvent& imp = wrapped();
    if (!imp.orientation()->canProvideAlpha())
        return jsNull();
    return jsNumber(imp.orientation()->alpha());
}

JSValue JSDeviceOrientationEvent::beta(ExecState&) const
{
    DeviceOrientationEvent& imp = wrapped();
    if (!imp.orientation()->canProvideBeta())
        return jsNull();
    return jsNumber(imp.orientation()->beta());
}

JSValue JSDeviceOrientationEvent::gamma(ExecState&) const
{
    DeviceOrientationEvent& imp = wrapped();
    if (!imp.orientation()->canProvideGamma())
        return jsNull();
    return jsNumber(imp.orientation()->gamma());
}

#if PLATFORM(IOS)
JSValue JSDeviceOrientationEvent::webkitCompassHeading(ExecState&) const
{
    DeviceOrientationEvent& imp = wrapped();
    if (!imp.orientation()->canProvideCompassHeading())
        return jsNull();
    return jsNumber(imp.orientation()->compassHeading());
}

JSValue JSDeviceOrientationEvent::webkitCompassAccuracy(ExecState&) const
{
    DeviceOrientationEvent& imp = wrapped();
    if (!imp.orientation()->canProvideCompassAccuracy())
        return jsNull();
    return jsNumber(imp.orientation()->compassAccuracy());
}
#endif

#if !PLATFORM(IOS)
JSValue JSDeviceOrientationEvent::absolute(ExecState&) const
{
    DeviceOrientationEvent& imp = wrapped();
    if (!imp.orientation()->canProvideAbsolute())
        return jsNull();
    return jsBoolean(imp.orientation()->absolute());
}
#endif

JSValue JSDeviceOrientationEvent::initDeviceOrientationEvent(ExecState& state)
{
    const String type = state.argument(0).toString(&state)->value(&state);
    bool bubbles = state.argument(1).toBoolean(&state);
    bool cancelable = state.argument(2).toBoolean(&state);
    // If alpha, beta or gamma are null or undefined, mark them as not provided.
    // Otherwise, use the standard JavaScript conversion.
    bool alphaProvided = !state.argument(3).isUndefinedOrNull();
    double alpha = state.argument(3).toNumber(&state);
    bool betaProvided = !state.argument(4).isUndefinedOrNull();
    double beta = state.argument(4).toNumber(&state);
    bool gammaProvided = !state.argument(5).isUndefinedOrNull();
    double gamma = state.argument(5).toNumber(&state);
#if PLATFORM(IOS)
    bool compassHeadingProvided = !state.argument(6).isUndefinedOrNull();
    double compassHeading = state.argument(6).toNumber(&state);
    bool compassAccuracyProvided = !state.argument(7).isUndefinedOrNull();
    double compassAccuracy = state.argument(7).toNumber(&state);
    RefPtr<DeviceOrientationData> orientation = DeviceOrientationData::create(alphaProvided, alpha, betaProvided, beta, gammaProvided, gamma, compassHeadingProvided, compassHeading, compassAccuracyProvided, compassAccuracy);
#else
    bool absoluteProvided = !state.argument(6).isUndefinedOrNull();
    bool absolute = state.argument(6).toBoolean(&state);
    RefPtr<DeviceOrientationData> orientation = DeviceOrientationData::create(alphaProvided, alpha, betaProvided, beta, gammaProvided, gamma, absoluteProvided, absolute);
#endif
    wrapped().initDeviceOrientationEvent(type, bubbles, cancelable, orientation.get());
    return jsUndefined();
}

} // namespace WebCore

#endif // ENABLE(DEVICE_ORIENTATION)
