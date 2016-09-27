package com.b1project.neolights;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.PrintWriter;
import java.net.Socket;

public class Main {

    private final static String DEFAULT_HOST_ADDRESS = "192.168.7.2";
    private final static int DEFAULT_HOST_PORT = 45045;
    private final static String REQ_SERIAL_RGB_VALUE = "tty/rgb";

    private static Socket socket;
    private static PrintWriter outPrintWriter;

    public static void main(String[] args) {
        FFmpegFrameGrabber grabber = null;
        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    System.out.println("Shouting down ...");
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

            int r = 0;
            int g = 0;
            int b = 0;
            int r_old = 0;
            int g_old = 0;
            int b_old = 0;
            String loader = "-";

            int x = 1440;
            int y = 0;
            int width = 1920;
            int height = 200;
            grabber = new FFmpegFrameGrabber(":0.0+" + x + "," + y);
            grabber.setFormat("x11grab");
            grabber.setImageWidth(width);
            grabber.setImageHeight(height);
            grabber.start();

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
                    final opencv_core.IplImage screenshot = converter.convert(grabber.grab());

                    for (int i = 0; i < width; i = i + 4) {
                        for (int j = 0; j < height; j = j + 4) {
                            opencv_core.CvScalar scalar = opencv_core.cvGet2D(screenshot, j, i);
                            r += (int) scalar.val(2);
                            g += (int) scalar.val(1);
                            b += (int) scalar.val(0);
                        }
                    }
                    r = Math.round(r / (480 * 50)); //average red (remember that I skipped ever alternate pixel)
                    g = Math.round(g / (480 * 50)); //average green
                    b = Math.round(b / (480 * 50)); //average blue*
                }
                else{
                    r = 0xaa;
                    g = 0x00;
                    b = 0x00;
                }
                if(r != r_old || g != g_old || b != b_old) {
                    System.out.printf("  Changes detected, sending request...\r");
                    final String request = String.format("{\"method\":\"%s\",\"detailMessage\":\"%d,%d,%d\"}", REQ_SERIAL_RGB_VALUE, r, g, b);
                    Thread t = new Thread() {
                        public void run(){
                            sendRequest(request);
                        }
                    };
                    t.start();
                    r_old = r;
                    g_old = g;
                    b_old = b;
                }
                Thread.sleep(100);
            }

        }
        catch (InterruptedException e){
            System.err.println("Process interrupted");
        }
        /*catch (NotFoundException e){
            System.err.println("X11 DPMS extension not supported by system");
        }*/ catch (FrameGrabber.Exception e) {
            System.err.println("Grabber fails");
        }
        finally {
            if(grabber != null) {
                try {
                    grabber.stop();
                } catch (FrameGrabber.Exception e) {
                    System.err.println("Can't stop grabber");
                }
            }
            closeSocket();
        }
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
