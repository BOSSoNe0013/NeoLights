package com.b1project.neolights.interfaces;

import org.freedesktop.DBus;
import org.freedesktop.dbus.DBusInterface;

/**
 * Copyright (C) 2018 Cyril Bosselut <bossone0013@gmail.com>
 * <p>
 * This file is part of NeoLights
 * <p>
 * NeoLights is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This libraries are distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public interface DBusLightsInterface extends DBusInterface {

    @DBus.Description("Get grabber status")
    boolean getGrabberStatus();

    @DBus.Description("Toggle grabber status")
    boolean toggleGrabber();

    @DBus.Description("Terminate the application")
    void quitGrabber();

    @DBus.Description("Get standby color")
    int[] getStandbyColor();

    @DBus.Description("Set standby color")
    void setStandbyColor(int red, int green, int blue);

    @DBus.Description("Start screen grabber benchmark")
    void runBenchmark();

    @DBus.Description("Display About dialog")
    void showAboutDialog();
}
