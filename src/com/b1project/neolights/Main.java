package com.b1project.neolights;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.PrintWriter;
import java.net.Socket;

/**
 * Copyright (C) 2015 Cyril Bosselut <bossone0013@gmail.com>
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

    private static Socket socket;
    private static PrintWriter outPrintWriter;
    private static int r_top_old = 0;
    private static int g_top_old = 0;
    private static int b_top_old = 0;
    private static int bottom_r_old = 0;
    private static int bottom_g_old = 0;
    private static int bottom_b_old = 0;

    public static void main(String[] args) {
        FFmpegFrameGrabber topGrabber = null;
        FFmpegFrameGrabber bottomGrabber = null;
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    sendColorValue(0, 0, 0, 0, 0, 0);
                    Thread.sleep(2);
                    System.out.println("Shutting down ...");
                    closeSocket();

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        try {
            DisplayInfo displayInfo = new DisplayInfo();

            openSocket(args[0], Integer.parseInt(args[1]));

            int top_r = 0;
            int top_g = 0;
            int top_b = 0;
            int bottom_r = 0;
            int bottom_g = 0;
            int bottom_b = 0;
            String loader = "-";

            int x = 1440;
            int top_y = 0;
            int bottom_y = 880;
            int width = 1920;
            int height = 200;
            topGrabber = new FFmpegFrameGrabber(":0.0+" + x + "," + top_y);
            topGrabber.setFormat("x11grab");
            topGrabber.setImageWidth(width);
            topGrabber.setImageHeight(height);
            topGrabber.start();

            bottomGrabber = new FFmpegFrameGrabber(":0.0+" + x + "," + bottom_y);
            bottomGrabber.setFormat("x11grab");
            bottomGrabber.setImageWidth(width);
            bottomGrabber.setImageHeight(height);
            bottomGrabber.start();

            //noinspection InfiniteLoopStatement
            while(true) {
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

                    for (int i = 0; i < width; i = i + 4) {
                        for (int j = 0; j < height; j = j + 4) {
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
            System.err.println("Process interrupted");
        }
        catch (FrameGrabber.Exception e) {
            System.err.println("Grabber fails");
        }
        finally {
            if(topGrabber != null) {
                try {
                    topGrabber.stop();
                } catch (FrameGrabber.Exception e) {
                    System.err.println("Can't stop top grabber");
                }
            }
            if(bottomGrabber != null) {
                try {
                    bottomGrabber.stop();
                } catch (FrameGrabber.Exception e) {
                    System.err.println("Can't stop bottom grabber");
                }
            }
            closeSocket();
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
            public void run(){
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

    private static void openSocket(String serverUri, int serverPort){
        if(serverUri.equals("")){
            serverUri = DEFAULT_HOST_ADDRESS;
        }
        if(serverPort == 0){
            serverPort = DEFAULT_HOST_PORT;
        }

        try {
            if(socket == null || socket.isConnected()){
                socket = new Socket(serverUri, serverPort);
            }

            Thread.sleep(2);
            outPrintWriter = new PrintWriter(socket.getOutputStream(), true);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static void closeSocket(){
        if(socket != null && socket.isConnected()){
            try {
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendRequest(String request){
        if(socket != null && outPrintWriter != null){
            outPrintWriter.println(request);
            System.out.printf("  Done                                \r");
        }
    }

}
