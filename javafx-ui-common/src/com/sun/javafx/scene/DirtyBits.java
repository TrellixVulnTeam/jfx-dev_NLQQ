/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene;


public enum DirtyBits {

    // Dirty bits for the Node class
    NODE_CACHE,
    NODE_CLIP,
    NODE_EFFECT,
    NODE_OPACITY,
    NODE_TRANSFORM,
    NODE_BOUNDS,
    NODE_TRANSFORMED_BOUNDS,
    NODE_VISIBLE,
    NODE_DEPTH_TEST,
    NODE_BLENDMODE,
    NODE_CSS,

    // Dirty bits for various subclasses of Node
    NODE_GEOMETRY,  // Used by ImageView, MediaView, and subclasses of Shape
    NODE_SMOOTH,    // Used by ImageView, MediaView, and subclasses of Shape
    NODE_VIEWPORT,  // Used by ImageView and MediaView
    NODE_CONTENTS,  // Used by ImageView, MediaView, Text, WebView, and subclasses of Shape

    // Dirty bits for the Parent class
    PARENT_CHILDREN,  // children removed, added or permuted

    // Dirty bits for the Shape class
    SHAPE_FILL,
    SHAPE_FILLRULE,
    SHAPE_MODE,
    SHAPE_STROKE,
    SHAPE_STROKEATTRS,

    // Dirty bits for the Region class
    REGION_SHAPE,    // Used when shape in region is dirty

    // Dirty bits for the Text class
    TEXT_ATTRS,
    TEXT_FONT,
    TEXT_SELECTION,
    TEXT_HELPER,

    // Dirty bits for the MediaView class
    MEDIAVIEW_MEDIA,

    // Dirty bits for the WebView class
    WEBVIEW_VIEW,

    // Dirty bits for various subclasses of Effect
    EFFECT_EFFECT,    // Used when Effect is dirty

    // NOTE: The following MUST be the last enum value in this class. The ordinal
    // of this enum indicates the number of dirty bits in this set, exclusive of
    // the MAX_DIRTY bit itself, which will never be set or tested.
    MAX_DIRTY;

    private long mask;

    private DirtyBits() {
        mask = 1 << ordinal();
    }

    public final long getMask() { return mask; }
}
