/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.scene.control;

import com.sun.javafx.scene.control.ContextMenuContent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.MenuBarSkin;
import javafx.scene.control.skin.MenuButtonSkinBase;
import javafx.scene.layout.VBox;


/**
 *
 */
public class MenuBarMenuButtonShim {

//    // can only access the getNodeForMenu method in MenuBarSkin from this package.
//    public static MenuButton getNodeForMenu(MenuBarSkin skin, int i) {
//        return skin.getNodeForMenu(i);
//    }

    public static ContextMenu getSubMenu(ContextMenuContent cmc) {
      return cmc.getSubMenu();
    }

//    public static Skin getPopupSkin(MenuButton mb) {
//        return ((MenuButtonSkinBase)mb.getSkin()).popup.getSkin();
//    }

//    public static ContextMenuContent getMenuContent(MenuButton mb) {
//        ContextMenuContent cmc = (ContextMenuContent)getPopupSkin(mb).getNode();
//        return cmc;
//    }

    public static ContextMenuContent getSubMenuContent(ContextMenuContent cmc) {
        ContextMenu cm = cmc.getSubMenu();
        return (cm != null) ? (ContextMenuContent)cm.getSkin().getNode() : null;
    }

    public static ContextMenuContent.MenuItemContainer getDisplayNodeForMenuItem(ContextMenuContent cmc, int i) {
        VBox itemsContainer = cmc.getItemsContainer();
        return (i < itemsContainer.getChildren().size()) ?
            (ContextMenuContent.MenuItemContainer)itemsContainer.getChildren().get(i) : null;
    }

    public static void setCurrentFocusedIndex(ContextMenuContent cmc, int i) {
        cmc.setCurrentFocusedIndex(i);
    }

//    public static int getFocusedIndex(MenuBarSkin skin) {
//        return skin.getFocusedMenuIndex();
//    }
}
