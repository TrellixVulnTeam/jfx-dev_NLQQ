/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package javafx.animation;


import com.sun.javafx.test.BuilderTestBase;

import java.util.Arrays;
import java.util.Collection;
import javafx.animation.PathTransition.OrientationType;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public final class PathTransition_builder_Test extends BuilderTestBase {
    @Parameters
    public static Collection data() {
        BuilderTestBase.Configuration cfg = new BuilderTestBase.Configuration(PathTransition.class);

        cfg.addProperty("duration", Duration.INDEFINITE);
        cfg.addProperty("path", new Rectangle());
        cfg.addProperty("orientation", OrientationType.ORTHOGONAL_TO_TANGENT);
        cfg.addProperty("node", new Rectangle());

        return Arrays.asList(new Object[] {
            config(cfg)
        });
    }

    public PathTransition_builder_Test(final Configuration configuration) {
        super(configuration);
    }
}
