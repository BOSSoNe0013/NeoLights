package com.b1project.neolights;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * Copyright (C) 2016 Cyril Bosselut <bossone0013@gmail.com>
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
class AppIcon {
        private String path = "";
        private String description = "Application Icon";

        AppIcon(String path, String description){
            this.path = path;
            this.description = description;
        }

        ImageIcon getIcon(){
            java.net.URL imgURL = this.getURL();
            if (imgURL != null) {
                return new ImageIcon(imgURL, this.description);
            } else {
                System.err.println("Couldn't find file: " + this.path);
                return null;
            }
        }

        Image getImage(){
            return this.getIcon().getImage();
        }

        URL getURL(){
            return  getClass().getResource(this.path);
        }
}
