/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.glass.ui.gtk;

import com.sun.glass.ui.*;
import java.nio.IntBuffer;

final class GtkRobot extends Robot {

    @Override
    protected void _create() {
        // no-op
    }

    @Override
    protected void _destroy() {
        // no-op
    }

    @Override
    protected native void _keyPress(int code);

    @Override
    protected native void _keyRelease(int code);

    @Override
    protected native void _mouseMove(int x, int y);

    @Override
    protected native void _mousePress(int buttons);

    @Override
    protected native void _mouseRelease(int buttons);

    @Override
    protected native void _mouseWheel(int wheelAmt);

    @Override
    protected native int _getMouseX();

    @Override
    protected native int _getMouseY();

    @Override
    protected int _getPixelColor(int x, int y) {
        Screen mainScreen = Screen.getMainScreen();
        x = (int) Math.floor((x + 0.5) * mainScreen.getPlatformScaleX());
        y = (int) Math.floor((y + 0.5) * mainScreen.getPlatformScaleY());
        int[] result = new int[1];
        _getScreenCapture(x, y, 1, 1, result);
        return result[0];
    }

    @Override native protected void _getScreenCapture(int x, int y, int width, int height, int[] data);
}
