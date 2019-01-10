package com.b1project.neolights;

import com.b1project.neolights.interfaces.DBusLightsInterface;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

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
class Main implements DBusLightsInterface {

    private final static String DEFAULT_HOST_ADDRESS = "192.168.7.2";
    private final static int DEFAULT_HOST_PORT = 45045;
    private final static String REQ_SERIAL_RGB_VALUE = "tty/rgb";
    private final static String REQ_BOARD_REBOOT = "board/reboot";
    private final static int DISPLAY_OFFSET_X = 0;
    private final static int DISPLAY_OFFSET_Y = 0;
    private final static int DISPLAY_WIDTH = 3840;
    private final static int DISPLAY_HEIGHT = 1600;
    private final static int SAMPLE_HEIGHT = 200;
    private final static int DEFAULT_TEST_PASS = 1500;

    private static Socket socket;
    private static PrintWriter outPrintWriter;
    private static int r_default = 0x77;
    private static int g_default = 0;
    private static int b_default = 0;
    private static int r_top_old = 0;
    private static int g_top_old = 0;
    private static int b_top_old = 0;
    private static int bottom_r_old = 0;
    private static int bottom_g_old = 0;
    private static int bottom_b_old = 0;
    private static boolean notification_visible = false;
    private static boolean pause_grabber = false;
    private static boolean should_quit = false;
    private static String host_address = DEFAULT_HOST_ADDRESS;
    private static int host_port = DEFAULT_HOST_PORT;
    private final static AppIcon APP_ICON = new AppIcon("/icon.png", "Application icon");
    private final static AppIcon APP_ICON_OFF = new AppIcon("/icon_off.png", "Application icon disabled");

    public static void main(String[] args) {
        System.setProperty("SWT_GTK3", "0");
        Runtime.getRuntime().addShutdownHook(new Thread(Main::quit));
        host_address = args[0];
        host_port = Integer.parseInt(args[1]);
        SwingUtilities.invokeLater(Main::showTrayIcon);
        openSocket();
        new Main().start();
    }

    private void start() {
        try {
            System.out.println("Start service");
            DBusConnection dbusConnection = DBusConnection.getConnection(DBusConnection.SESSION);
            dbusConnection.requestBusName("com.b1project.neolights.Main");
            dbusConnection.exportObject("/com/b1project/neolights/Main", this);
            while (!should_quit) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
            System.out.println("Should quit service");
            dbusConnection.disconnect();
        }
        catch (DBusException e) {
            e.printStackTrace();
        }
    }

    private static void exit() {
        try {
            should_quit = true;
            pause_grabber = true;
            if(socket != null) {
                Thread.sleep(200);
                sendColorValue(0, 0, 0, 0, 0, 0);
                Thread.sleep(2);
            }
            System.exit(0);

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            System.err.println("\nError: " + e.getMessage());
        }
    }

    @Override
    public boolean getGrabberStatus() {
        return !pause_grabber;
    }

    @Override
    public boolean toggleGrabber() {
        try {
            set_grabber_status(!pause_grabber);
        }
        catch (Exception ignore){}
        return getGrabberStatus();
    }

    @Override
    public void quitGrabber() {
        exit();
    }

    @Override
    public int[] getStandbyColor() {
        return new int[]{r_default, g_default, b_default};
    }

    @Override
    public void setStandbyColor(int red, int green, int blue) {
        r_default = red;
        g_default = green;
        b_default = blue;
    }

    @Override
    public void runBenchmark() {
        benchmark();
    }

    private static void quit() {
        try {
            System.out.println("\nShutting down ...");
            Thread.sleep(2);
            closeSocket();
            Thread.sleep(500);

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            System.err.println("\nError: " + e.getMessage());
        }
    }

    private static void showTrayIcon(){
        if (SystemTray.get() == null){
            System.err.println("\nTray icon not supported");
            return;
        }
        System.out.println("\nAdding tray icon");
        final SystemTray tray = SystemTray.get();
        //SystemTray.COMPATIBILITY_MODE = true;
        tray.setImage(APP_ICON.getURL());
        tray.setStatus("NeoLights");

        //Add components to pop-up menu
        tray.getMenu().add(new MenuItem("Pause grabber", e -> set_grabber_status(!pause_grabber)));
        tray.getMenu().add(new MenuItem("About", e -> about()));
        tray.getMenu().add(new MenuItem("Benchmark", e -> benchmark()));
        tray.getMenu().add(new MenuItem("Reboot board", e -> reboot()));
        tray.getMenu().add(new MenuItem("Quit", e -> exit()));
    }

    @Override
    public void showAboutDialog() {
        about();
    }

    private static void about() {
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

    private static void benchmark() {
        boolean grabber_status = pause_grabber;
        if(!pause_grabber) {
            set_grabber_status(true);
        }
        FFmpegFrameGrabber topGrabber = null;
        FFmpegFrameGrabber bottomGrabber = null;
        long total_time_ms = 0;
        long duration;
        System.out.println("\nStart benchmark");
        try {
            DisplayInfo displayInfo = new DisplayInfo();
            OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
            int top_r = 0;
            int top_g = 0;
            int top_b = 0;
            int bottom_r = 0;
            int bottom_g = 0;
            int bottom_b = 0;
            int pass = 0;

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
            int max_pass = DEFAULT_TEST_PASS;
            while(pass < max_pass){
                if(displayInfo.getDisplayStatus() != 0) {
                    System.err.println("\nScreen is not awake");
                    break;
                }
                long start = System.currentTimeMillis();
                final opencv_core.IplImage top_screenshot = converter.convert(topGrabber.grab());
                final UByteIndexer top_indexer = top_screenshot.createIndexer();
                final opencv_core.IplImage bottom_screenshot = converter.convert(bottomGrabber.grab());
                final UByteIndexer bottom_indexer = bottom_screenshot.createIndexer();

                int i = 0;
                while (i < DISPLAY_WIDTH) {
                    int j = 0;
                    while (j < SAMPLE_HEIGHT) {
                        top_r += top_indexer.get(j, i, 2);
                        top_g += top_indexer.get(j, i, 1);
                        top_b += top_indexer.get(j, i, 0);
                        bottom_r += bottom_indexer.get(j, i, 2);
                        bottom_g += bottom_indexer.get(j, i, 1);
                        bottom_b += bottom_indexer.get(j, i, 0);
                        j += 4;
                    }
                    i += 4;
                }

                top_indexer.release();
                bottom_indexer.release();

                top_r = Math.round(top_r / (480f * 50)); //average red
                top_g = Math.round(top_g / (480f * 50)); //average green
                top_b = Math.round(top_b / (480f * 50)); //average blue
                bottom_r = Math.round(bottom_r / (480f * 50)); //average red
                bottom_g = Math.round(bottom_g / (480f * 50)); //average green
                bottom_b = Math.round(bottom_b / (480f * 50)); //average blue
                duration = System.currentTimeMillis() - start;
                total_time_ms += duration;
                pass += 1;
                if(pass % (5 * max_pass / 100) == 0){
                    System.out.print('█');
                }
            }
            show_benchmark_result(pass, total_time_ms);
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
            System.out.println("\nBenchmark complete");
            if(!grabber_status) {
                set_grabber_status(false);
            }
        }
    }

    private static void show_benchmark_result(int pass, long total_time_ms){
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0f;
        constraints.weighty = 1.0f;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.fill = GridBagConstraints.BOTH;
        double total_time = (double) total_time_ms / 1000;
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel benchmarkResultLabel =
                new JLabel(
                        "<html><span style='font-weight:bold;font-size:1.52em'>Total time: " + total_time + "s<span></html>"
                );
        panel.add(benchmarkResultLabel, constraints);
        constraints.gridy++;
        double average = total_time / pass;
        JLabel benchmarkAverageLabel =
                new JLabel(
                        "<html><span>Average: " + average + "</span></html>"
                );
        panel.add(benchmarkAverageLabel, constraints);
        constraints.gridy++;
        double fps = pass / total_time;
        JLabel benchmarkFPSLabel =
                new JLabel(
                        "<html><span>Frame/s: " + fps + "</span></html>"
                );
        panel.add(benchmarkFPSLabel, constraints);
        constraints.gridy++;
        JLabel benchmarkPassLabel =
                new JLabel(
                        "<html><span>Pass: " + pass + "</span></html>"
                );
        panel.add(benchmarkPassLabel, constraints);
        JOptionPane.showMessageDialog(null,
                panel,
                "Benchmark Results",
                JOptionPane.INFORMATION_MESSAGE,
                APP_ICON.getIcon());
    }

    private static FFmpegFrameGrabber make_grabber(int y_pos) {
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(":0.0+" + DISPLAY_OFFSET_X + "," + y_pos);
        grabber.setFormat("x11grab");
        grabber.setImageWidth(DISPLAY_WIDTH);
        grabber.setImageHeight(SAMPLE_HEIGHT);
        return grabber;
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

            topGrabber = make_grabber(DISPLAY_OFFSET_Y);
            topGrabber.start();

            int bottom_y = DISPLAY_HEIGHT + DISPLAY_OFFSET_Y - SAMPLE_HEIGHT;
            bottomGrabber = make_grabber(bottom_y);
            bottomGrabber.start();

            while(true) {
                if(pause_grabber){
                    System.out.println("\nGrabber in standby");
                    Thread.sleep((int)(displayInfo.getRefreshRate() * 7.5));
                    break;
                }
                if(socket == null || socket.isClosed()){
                    ask_for_connection();
                    break;
                }
                System.out.printf("    Scanning %s                          \r", loader);
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
                    final UByteIndexer top_indexer = top_screenshot.createIndexer();
                    final opencv_core.IplImage bottom_screenshot = converter.convert(bottomGrabber.grab());
                    final UByteIndexer bottom_indexer = bottom_screenshot.createIndexer();

                    int i = 0;
                    while (i < DISPLAY_WIDTH) {
                        int j = 0;
                        while (j < SAMPLE_HEIGHT) {
                            top_r += top_indexer.get(j, i, 2);
                            top_g += top_indexer.get(j, i, 1);
                            top_b += top_indexer.get(j, i, 0);
                            bottom_r += bottom_indexer.get(j, i, 2);
                            bottom_g += bottom_indexer.get(j, i, 1);
                            bottom_b += bottom_indexer.get(j, i, 0);
                            j +=+ 4;
                        }
                        i += 4;
                    }

                    top_indexer.release();
                    bottom_indexer.release();

                    top_r = Math.round(top_r / (480f * 50)); //average red
                    top_g = Math.round(top_g / (480f * 50)); //average green
                    top_b = Math.round(top_b / (480f * 50)); //average blue
                    bottom_r = Math.round(bottom_r / (480f * 50)); //average red
                    bottom_g = Math.round(bottom_g / (480f * 50)); //average green
                    bottom_b = Math.round(bottom_b / (480f * 50)); //average blue
                }
                else{
                    top_r = 0x77;
                    top_g = 0x00;
                    top_b = 0x00;
                    bottom_r = 0x77;
                    bottom_g = 0x00;
                    bottom_b = 0x00;
                }
                process_scan_result(top_r, top_g, top_b, bottom_r, bottom_g, bottom_b);
                //Thread.sleep((displayInfo.getRefreshRate()));
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

    private static void process_scan_result(int top_r, int top_g, int top_b,
                                       int bottom_r, int bottom_g, int bottom_b){
        if(top_r != r_top_old || top_g != g_top_old || top_b != b_top_old
                || bottom_r != bottom_r_old || bottom_g != bottom_g_old || bottom_b != bottom_b_old) {
            System.out.print("    Changes detected, sending request...\r");
            sendColorValue(top_r, top_g, top_b, bottom_r, bottom_g, bottom_b);
            r_top_old = top_r;
            g_top_old = top_g;
            b_top_old = top_b;
            bottom_r_old = bottom_r;
            bottom_g_old = bottom_g;
            bottom_b_old = bottom_b;
        }
    }

    private static void set_grabber_status(boolean status){
        final SystemTray tray = SystemTray.get();
        MenuItem menuItem = (MenuItem) tray.getMenu().getFirst();
        pause_grabber = status;
        if(!pause_grabber ){
            try {
                menuItem.setText("Pause grabber");
                tray.setImage(APP_ICON.getURL());
            }
            catch (NullPointerException e){
                System.err.println("\nError: can't update tray icon menu");
                System.err.println("\nError: " + e.getMessage());
            }
            finally {
                openSocket();
            }
        }
        else{
            try {
                menuItem.setText("Run grabber");
                tray.setImage(APP_ICON_OFF.getURL());
            }
            catch (NullPointerException e){
                System.err.println("\nError: can't update tray icon menu");
                System.err.println("\nError: " + e.getMessage());
            }
            finally {
                try {
                    sendColorValue(r_default, g_default, b_default, r_default, g_default, b_default);
                    Thread.sleep(2);
                    closeSocket();
                } catch (Exception e) {
                    System.err.println("\nError: " + e.getMessage());
                    ask_for_connection();

                }
            }
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
        Thread t = new Thread(() -> sendRequest(request));
        t.start();
    }

    private static void reboot(){
        set_grabber_status(false);
        final String request = String.format(
                "{\"method\":\"%s\"}",
                REQ_BOARD_REBOOT
        );
        Thread t = new Thread(() -> sendRequest(request));
        t.start();
        //closeSocket();
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
                Thread t = new Thread(Main::scan_display);
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
                if(!pause_grabber) {
                    System.out.print("    Done                                \r");
                }
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

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return null;
    }
}
