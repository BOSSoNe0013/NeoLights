package com.b1project.neolights;

import dorkbox.systemTray.MenuEntry;
import dorkbox.systemTray.SystemTray;
import dorkbox.systemTray.SystemTrayMenuAction;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Copyright (C) 2016 Cyril Bosselut <bossone0013@gmail.com>
 * <p>
 * This file is part of NeoLights
 * <p>
 * NeoLights is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License as published by
 * the Apache Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * <p>
 * This libraries are distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the Apache License
 * along with this program.  If not, see <http://www.apache.org/licenses/LICENSE-2.0>.
 */
public class Main {

    private final static String DEFAULT_HOST_ADDRESS = "192.168.7.2";
    private final static int DEFAULT_HOST_PORT = 45045;
    private final static String REQ_SERIAL_RGB_VALUE = "tty/rgb";
    private final static int DISPLAY_OFFSET_X = 1440;
    private final static int DISPLAY_OFFSET_Y = 0;
    private final static int DISPLAY_WIDTH = 1920;
    private final static int DISPLAY_HEIGHT = 1080;
    private final static int SAMPLE_HEIGHT = 200;

    private static Socket socket;
    private static PrintWriter outPrintWriter;
    private static int r_top_old = 0;
    private static int g_top_old = 0;
    private static int b_top_old = 0;
    private static int bottom_r_old = 0;
    private static int bottom_g_old = 0;
    private static int bottom_b_old = 0;
    private static boolean notification_visible = false;
    private static boolean pause_grabber = false;
    private static String host_address = DEFAULT_HOST_ADDRESS;
    private static int host_port = DEFAULT_HOST_PORT;
    private final static AppIcon APP_ICON = new AppIcon("/icon.png", "Application icon");
    private final static AppIcon APP_ICON_OFF = new AppIcon("/icon_off.png", "Application icon disabled");

    public static void main(String[] args) {
        System.setProperty("SWT_GTK3", "0");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    pause_grabber = true;
                    Thread.sleep(200);
                    System.out.println("\nShutting down ...");
                    closeSocket();
                    Thread.sleep(500);

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    System.err.println("\nError: " + e.getMessage());
                }
            }
        });
        host_address = args[0];
        host_port = Integer.parseInt(args[1]);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                showTrayIcon();
            }
        });
        openSocket();
    }

    private static void showTrayIcon(){
        if (SystemTray.getSystemTray() == null){
            System.err.println("\nTray icon not supported");
            return;
        }
        System.out.println("\nAdding tray icon");
        final SystemTray tray = SystemTray.getSystemTray();
        SystemTray.COMPATIBILITY_MODE = true;
        tray.setIcon(APP_ICON.getURL());
        tray.setStatus("NeoLights");

        //Add components to pop-up menu
        tray.addMenuEntry("Pause grabber", new SystemTrayMenuAction() {
            @Override
            public void onClick(SystemTray systemTray, MenuEntry menuEntry) {
                set_grabber_status(false);
            }
        });
        tray.addMenuEntry("About", new SystemTrayMenuAction() {
            @Override
            public void onClick(SystemTray systemTray, MenuEntry menuEntry) {
                Package p = Main.class.getPackage();
                String appName = p.getImplementationTitle();
                String version = p.getImplementationVersion();
                String vendor = p.getImplementationVendor();
                String url = "https://github.com/BOSSoNe0013/NeoLights";
                GridBagConstraints constraints = new GridBagConstraints();
                constraints.gridx = 0;
                constraints.gridy = 0;
                constraints.weightx = 1.0f;
                constraints.weighty = 1.0f;
                constraints.insets = new Insets(5, 5, 5, 5);
                constraints.fill = GridBagConstraints.BOTH;
                JPanel panel = new JPanel(new GridBagLayout());
                JLabel appNameLabel =
                        new JLabel(
                                "<html><span style='font-weight:bold;font-size:1.52em'>" + appName + "<span></html>"
                        );
                panel.add(appNameLabel, constraints);
                constraints.gridy++;
                JLabel appVendorLabel = new JLabel(vendor);
                panel.add(appVendorLabel, constraints);
                constraints.gridy++;
                JLabel appVersionLabel = new JLabel("ver. " + version);
                panel.add(appVersionLabel, constraints);
                constraints.gridy++;
                panel.add(new JLabel("GitHub:"), constraints);
                constraints.gridy++;
                JLabel gitHubUrlLabel =
                        new JLabel(
                                "<html><a href='" + url + "'>" + url + "</a></html>"
                        );
                gitHubUrlLabel.setFocusable(true);
                panel.add(gitHubUrlLabel, constraints);
                JOptionPane.showMessageDialog(null,
                        panel,
                        "About NeoLights",
                        JOptionPane.INFORMATION_MESSAGE,
                        APP_ICON.getIcon());
            }
        });
        tray.addMenuEntry("Quit", new SystemTrayMenuAction() {
            @Override
            public void onClick(SystemTray systemTray, MenuEntry menuEntry) {
                System.exit(0);
            }
        });
    }

    private static void scan_display(){
        FFmpegFrameGrabber topGrabber = null;
        FFmpegFrameGrabber bottomGrabber = null;
        try {
            DisplayInfo displayInfo = new DisplayInfo();
            OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
            int top_r = 0;
            int top_g = 0;
            int top_b = 0;
            int bottom_r = 0;
            int bottom_g = 0;
            int bottom_b = 0;
            String loader = "-";

            int bottom_y = DISPLAY_HEIGHT + DISPLAY_OFFSET_Y - SAMPLE_HEIGHT;
            topGrabber = new FFmpegFrameGrabber(":0.0+" + DISPLAY_OFFSET_X + "," + DISPLAY_OFFSET_Y);
            topGrabber.setFormat("x11grab");
            topGrabber.setImageWidth(DISPLAY_WIDTH);
            topGrabber.setImageHeight(SAMPLE_HEIGHT);
            topGrabber.start();

            bottomGrabber = new FFmpegFrameGrabber(":0.0+" + DISPLAY_OFFSET_X + "," + bottom_y);
            bottomGrabber.setFormat("x11grab");
            bottomGrabber.setImageWidth(DISPLAY_WIDTH);
            bottomGrabber.setImageHeight(SAMPLE_HEIGHT);
            bottomGrabber.start();

            //noinspection InfiniteLoopStatement
            while(true) {
                if(pause_grabber){
                    System.out.println("\n Grabber in standby");
                    break;
                }
                if(socket == null || socket.isClosed()){
                    ask_for_connection();
                    break;
                }
                System.out.printf("  Scanning %s                          \r", loader);
                switch (loader){
                    case "/":
                        loader = "-";
                        break;
                    case "|":
                        loader = "/";
                        break;
                    case "\\":
                        loader = "|";
                        break;
                    case "-":
                        loader = "\\";
                        break;
                }

                if(displayInfo.getDisplayStatus() == 0) {
                    final opencv_core.IplImage top_screenshot = converter.convert(topGrabber.grab());
                    final opencv_core.IplImage bottom_screenshot = converter.convert(bottomGrabber.grab());

                    for (int i = 0; i < DISPLAY_WIDTH; i = i + 4) {
                        for (int j = 0; j < SAMPLE_HEIGHT; j = j + 4) {
                            opencv_core.CvScalar top_scalar = opencv_core.cvGet2D(top_screenshot, j, i);
                            opencv_core.CvScalar bottom_scalar = opencv_core.cvGet2D(bottom_screenshot, j, i);
                            top_r += (int) top_scalar.val(2);
                            top_g += (int) top_scalar.val(1);
                            top_b += (int) top_scalar.val(0);
                            bottom_r += (int) bottom_scalar.val(2);
                            bottom_g += (int) bottom_scalar.val(1);
                            bottom_b += (int) bottom_scalar.val(0);
                        }
                    }
                    top_r = Math.round(top_r / (480 * 50)); //average red
                    top_g = Math.round(top_g / (480 * 50)); //average green
                    top_b = Math.round(top_b / (480 * 50)); //average blue
                    bottom_r = Math.round(bottom_r / (480 * 50)); //average red
                    bottom_g = Math.round(bottom_g / (480 * 50)); //average green
                    bottom_b = Math.round(bottom_b / (480 * 50)); //average blue
                    top_screenshot.close();
                    bottom_screenshot.close();
                    top_screenshot.release();
                    bottom_screenshot.release();
                }
                else{
                    top_r = 0x77;
                    top_g = 0x00;
                    top_b = 0x00;
                    bottom_r = 0x77;
                    bottom_g = 0x00;
                    bottom_b = 0x00;
                }
                if(top_r != r_top_old || top_g != g_top_old || top_b != b_top_old
                        || bottom_r != bottom_r_old || bottom_g != bottom_g_old || bottom_b != bottom_b_old) {
                    System.out.printf("  Changes detected, sending request...\r");
                    sendColorValue(top_r, top_g, top_b, bottom_r, bottom_g, bottom_b);
                }
                Thread.sleep(100);
            }
        }
        catch (InterruptedException e){
            System.err.println("\nProcess interrupted");
        }
        catch (FrameGrabber.Exception e) {
            System.err.println("\nGrabber fails");
        }
        finally {
            if(topGrabber != null) {
                try {
                    topGrabber.stop();
                    topGrabber.release();
                } catch (FrameGrabber.Exception e) {
                    System.err.println("\nCan't stop top grabber");
                }
            }
            if(bottomGrabber != null) {
                try {
                    bottomGrabber.stop();
                    bottomGrabber.release();
                } catch (FrameGrabber.Exception e) {
                    System.err.println("\nCan't stop bottom grabber");
                }
            }
            closeSocket();
        }
    }

    private static void set_grabber_status(boolean status){
        final SystemTray tray = SystemTray.getSystemTray();
        if(pause_grabber && status){
            pause_grabber = false;
            tray.updateMenuEntry("Run grabber", "Pause grabber", new SystemTrayMenuAction() {
                @Override
                public void onClick(SystemTray systemTray, MenuEntry menuEntry) {
                    set_grabber_status(false);
                }
            });
            tray.setIcon(APP_ICON.getURL());
            openSocket();
        }
        else{
            pause_grabber = true;
            closeSocket();
            tray.updateMenuEntry("Pause grabber", "Run grabber", new SystemTrayMenuAction() {
                @Override
                public void onClick(SystemTray systemTray, MenuEntry menuEntry) {
                    set_grabber_status(true);
                }
            });
            tray.setIcon(APP_ICON_OFF.getURL());
        }
    }

    private static void sendColorValue(int top_r, int top_g, int top_b,
                                       int bottom_r, int bottom_g, int bottom_b){
        final String request = String.format(
                "{\"method\":\"%s\",\"detailMessage\":\"%d,%d,%d|%d,%d,%d\"}",
                REQ_SERIAL_RGB_VALUE,
                top_r, top_g, top_b,
                bottom_r, bottom_g, bottom_b
        );
        Thread t = new Thread() {
            public void run() {
                sendRequest(request);
            }
        };
        t.start();
        r_top_old = top_r;
        g_top_old = top_g;
        b_top_old = top_b;
        bottom_r_old = bottom_r;
        bottom_g_old = bottom_g;
        bottom_b_old = bottom_b;
    }

    private static void openSocket(){
        System.out.println("\nOpening connection ...");
        if(host_address.equals("")){
            host_address = DEFAULT_HOST_ADDRESS;
        }
        if(host_port == 0){
            host_port = DEFAULT_HOST_PORT;
        }

        try {
            if(socket == null || !socket.isConnected()){
                socket = new Socket(host_address, host_port);
                socket.setSoTimeout(500);
            }

            Thread.sleep(2);
            outPrintWriter = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("\nConnected");
                Thread t = new Thread() {
                    public void run(){
                        scan_display();
                    }
                };
                t.start();
        }
        catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
            ask_for_connection();

        }
    }

    private static void closeSocket(){
        if(socket != null && socket.isConnected()){
            try {
                sendColorValue(0, 0, 0, 0, 0, 0);
                Thread.sleep(2);
                sendColorValue(0, 0, 0, 0, 0, 0);
                Thread.sleep(2);
                socket.close();
                socket = null;
            } catch (Exception e) {
                System.err.println("\nCan't close socket");
                socket = null;
            }
        }
    }

    private static void sendRequest(String request){
        if(socket != null && outPrintWriter != null){
            if(outPrintWriter.checkError()){
                closeSocket();
                ask_for_connection();
            }
            else {
                outPrintWriter.println(request);
                System.out.printf("  Done                                \r");
            }
        }
        else{
            ask_for_connection();
        }
    }

    private static void ask_for_connection(){
        if (notification_visible || pause_grabber){
            return;
        }
        notification_visible = true;
        final JFrame frame = new JFrame();
        frame.setSize(300, 125);
        frame.setUndecorated(true);
        frame.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0f;
        constraints.weighty = 1.0f;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.BOTH;
        String headerTxt = "NeoLights";
        JLabel headingLabel = new JLabel(headerTxt);
        ImageIcon headingIcon = APP_ICON.getIcon();
        headingLabel.setIcon(headingIcon);
        headingLabel.setOpaque(false);
        frame.add(headingLabel, constraints);
        constraints.gridx++;
        constraints.weightx = 0f;
        constraints.weighty = 0f;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTH;
        JButton closeButton = new JButton(new AbstractAction("X") {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                notification_visible = false;
                System.exit(0);
            }
        });
        closeButton.setMargin(new Insets(1, 4, 1, 4));
        closeButton.setFocusable(false);
        frame.add(closeButton, constraints);
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 1.0f;
        constraints.weighty = 1.0f;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 2;
        String message = "Connection to light system closed. What do you want to do ?";
        JLabel messageLabel = new JLabel("<HtMl>"+message);
        frame.add(messageLabel, constraints);
        frame.setLocation(DISPLAY_OFFSET_X + DISPLAY_WIDTH - frame.getWidth(),
                DISPLAY_OFFSET_Y + DISPLAY_HEIGHT - frame.getHeight());
        //TODO add buttons
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.weightx = 0.0f;
        constraints.weighty = 1.0f;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        JButton retryButton = new JButton(new AbstractAction("Retry") {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                notification_visible = false;
                openSocket();
            }
        });
        retryButton.setMargin(new Insets(1, 4, 1, 4));
        retryButton.setFocusable(false);
        frame.add(retryButton, constraints);
        constraints.gridx++;
        constraints.weightx = 0.0f;
        constraints.weighty = 1.0f;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.SOUTH;
        JButton closeButton2 = new JButton(new AbstractAction("Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                notification_visible = false;
                System.exit(0);
            }
        });
        closeButton2.setMargin(new Insets(1, 4, 1, 4));
        closeButton2.setFocusable(false);
        frame.add(closeButton2, constraints);

        frame.setVisible(true);
        /*while (notification_visible){
            // wait
        }*/
    }
}
