package com.b1project.neolights;


import com.sun.jna.*;
import com.sun.jna.platform.unix.X11;

import java.util.Collections;
import java.util.List;

/**
 * Copyright (C) 2015 Cyril Bosselut <bossone0013@gmail.com>
 * <p>
 * This file is part of NeoJava examples for UDOO
 * <p>
 * NeoJava examples for UDOO is free software: you can redistribute it and/or modify
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
@SuppressWarnings("WeakerAccess")
class DisplayInfo {

    private X11.Display display;

    DisplayInfo() {
        display = X11.INSTANCE.XOpenDisplay(null);
        if (display == null) {
          display = X11.INSTANCE.XOpenDisplay(":0.0");
        }
        if (display == null) {
          throw new RuntimeException("Could not find a display, please setup your DISPLAY environment variable");
        }
        ExtVersionMajor major = new ExtVersionMajor();
        ExtVersionMinor minor = new ExtVersionMinor();
        DPMS.INSTANCE.DPMSGetVersion(display, major, minor);
        System.out.printf("DPMS version: %d.%d\n", major.value, minor.value);
    }

    public static class DPMSPower extends Structure {
        public int level;

        @Override
        protected List getFieldOrder() {
            return Collections.singletonList("level");
        }
    }

    public static class DPMSState extends Structure {
        public boolean enable = false;

        @Override
        protected List getFieldOrder() {
            return Collections.singletonList("enable");
        }
    }

    public static class ExtVersionMajor extends Structure {
        public int value;
        @Override
        protected List getFieldOrder() {
            return Collections.singletonList("value");
        }
    }

    public static class ExtVersionMinor extends Structure {
        public int value;
        @Override
        protected List getFieldOrder() {
            return Collections.singletonList("value");
        }
    }

    @SuppressWarnings("unused")
    interface DPMS extends Library {
        DPMS INSTANCE = (DPMS) Native.loadLibrary("Xext", DPMS.class);
        int DPMSGetVersion(X11.Display display, ExtVersionMajor major, ExtVersionMinor minor);
        boolean DPMSCapable(X11.Display display);
        int DPMSInfo(X11.Display display, DPMSPower power_level, DPMSState state);
        int DPMSEnable(X11.Display display);
        int DPMSDisable(X11.Display display);
        int DPMSForceLevel(X11.Display display, int level);
    }

    /**
     * request Energy Star (DPMS) status
     * @return int status 0 = ON, 1 = STANDBY, 2 = SUSPEND, 3 = OFF
     */
    int getDisplayStatus(){
        DPMSPower pwr = new DPMSPower();
        DPMSState state = new DPMSState();
        if(DPMS.INSTANCE.DPMSCapable(display)) {
            DPMS.INSTANCE.DPMSInfo(display, pwr, state);
            if(!state.enable){
                DPMS.INSTANCE.DPMSEnable(display);
            }
        }
        return pwr.level;
    }
}
