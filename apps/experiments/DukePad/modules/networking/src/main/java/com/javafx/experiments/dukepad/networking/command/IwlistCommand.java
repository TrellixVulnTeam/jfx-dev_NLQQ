/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.javafx.experiments.dukepad.networking.command;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.List;

public final class IwlistCommand extends Command {

    private static final String IWLIST_COMMAND = "iwlist";
    private static final String SCANNING_PARAM = "scanning";
    private static final String NETWORK_INTERFACE = "wlan0"; //pi
    private StringProperty netInterface = new SimpleStringProperty(NETWORK_INTERFACE);
    private StringProperty parameter = new SimpleStringProperty(SCANNING_PARAM);

    /**
     * Get the base command to be run (no options)
     *
     * @return The name of the command to be run
     */
    @Override
    public String getCommandName() {
        return IWLIST_COMMAND;
    }

    /**
     * Set interface
     *
     * @param netInterface The interface whose info we want. Default is "wlan0"
     */
    public void setNetInterface(String netInterface) {
        this.netInterface.setValue(netInterface);
    }

    /**
     * Get the interface
     *
     * @return The network interface to be queried/processed
     */
    public String getNetInterface() {
        return netInterface.getValue();
    }

    /**
     * Get the property that holds the interface
     *
     * @return The property that holds the network interface to be queried/processed
     */
    public StringProperty netInterfaceProperty() {
        return netInterface;
    }

    /**
     * Set command parameter (e.g. scanning)
     *
     * @param parameter The iwlist command parameter (e.g. scanning, frequency,...)
     */
    public void setParameter(String parameter) {
        this.parameter.setValue(parameter);
    }

    /**
     * Get the parameter
     *
     * @return The iwlist parameter (e.g. scanning)
     */
    public String getParameter() {
        return parameter.getValue();
    }

    /**
     * Get the property that holds the iwlist command parameter
     *
     * @return The property that holds the iwlist command parameter
     */
    public StringProperty parameterProperty() {
        return parameter;
    }

    /**
     * Builds/generates the iwlist command to be executed.
     *
     * @return a List containing the platform-specific iwlist command
     */
    @Override
    public List<String> buildCommand() {
        List<String> commandParts = super.buildCommand();
        commandParts.add(getNetInterface());
        commandParts.add(getParameter());
        return commandParts;
    }
}
