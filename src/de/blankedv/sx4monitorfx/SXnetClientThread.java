/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.blankedv.sx4monitorfx;

import static de.blankedv.sx4monitorfx.SX4Monitor.INVALID_INT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;

/**
 * communicates with the SX3-PC server program (usually on port 4104)
 *
 * runs on own thread, using a BlockingQueue for queing the commands can be
 * shutdown by calling the shutdown method.
 *
 * @author mblank
 *
 */
public class SXnetClientThread extends Thread {

    // we using 3 different lists for the different types of lanbahn data
    // 1... 999 => usually mapped to hardware addresses
    // 1000...1999 => virtual signals, turnouts
    // 2000...2999 => virtual sensors, used for routing

    public volatile static long timeOfLastMsgReceived = System.currentTimeMillis();
    public volatile static boolean shutdownFlag = false;
    public static SimpleStringProperty connString = new SimpleStringProperty("");
    public static final int SXMAX = 111;

    private final static boolean DEBUG = true;
    private volatile boolean clientTerminated;

    private InetAddress ip;
    private final int port = 4104;
    private Socket socket;
    private PrintWriter out = null;
    private BufferedReader in = null;

    
    
    public SXnetClientThread() {

        clientTerminated = false;

        List<InetAddress> myip = NIC.getmyip();
        // loadPrefs();

        if (!myip.isEmpty()) {
            ip = myip.get(0);
        } else {
            System.out.println("no network adapter, cannot listen to sxnet messages.");
        }
    }
    
    public SXnetClientThread(InetAddress ipaddr) {
        ip = ipaddr;
        clientTerminated = false;

    }
    
    public void shutdown() {
        shutdownFlag = true;
    }

    @Override
    public void run() {

        shutdownFlag = false;
        clientTerminated = false;
        //Thread.currentThread().setPriority(1);  no effect
        connect();
  
        while ((shutdownFlag == false) && (!Thread.currentThread().isInterrupted())) {
            try {
                while ((in != null) && (in.ready())) {
                    String in1 = in.readLine();
                    if (DEBUG) {
                        System.out.println("msgFromServer: " + in1);
                    }
                    handleMsgFromServer(in1.toUpperCase());
                    timeOfLastMsgReceived = System.currentTimeMillis();
                    
                }
                Thread.sleep(20);  // without a short sleep here, CPU utilization is always 100%
            } catch (IOException e) {
                System.out.println("INVALID_INT: reading from socket - " + e.getMessage());
            } catch (InterruptedException ex) { 
                Logger.getLogger(SXnetClientThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        clientTerminated = true;
        if (socket != null) {
            try {
                socket.close();
                System.out.println("SXnetClientThread - socket closed");
            } catch (IOException e) {
                System.out.println("SXnetClientThread - " + e.getMessage());
            }
        }
        if (DEBUG) {
            System.out.println("SXnetClientThread stopped.");
        }
    }

    private void connect() {
        if (DEBUG) {
            System.out.println("SXnet trying conn to - " + ip + ":" + port);
        }
        try {
            SocketAddress socketAddress = new InetSocketAddress(ip, port);

            // create a socket
            socket = new Socket();
            socket.connect(socketAddress, 2000);
            //socket.setSoTimeout(2000);  // set read timeout to 2000 msec   

            //socket.setSoLinger(true, 0);  // force close, dont wait.
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
            String conn = in.readLine();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    connString.set("SXnet connected to: " + ip.toString().substring(1)+" "+conn);
                }
            });

            if (DEBUG) {
                System.out.println("SXnet connected to: "  + ip.toString().substring(1)+" "+conn);
            }

        } catch (Exception e) {
            System.out.println("SXnetClientThread.connect - Exception: " + e.getMessage());
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    connString.set(e.getMessage());
                }
            });
            shutdownFlag = true; // terminate this thread and show alert window

        }

    }

    public void send(String command) {
        if (shutdownFlag || clientTerminated) {
            return;
        }
        if (out == null) {
            if (DEBUG) {
                System.out.println("out=null, could not send: " + command);
            }
        } else {
            try {
                out.println(command);
                out.flush();
                if (DEBUG) {
                    System.out.println("sent: " + command);
                }
                // handleMsgFromServer(command);   // ==> local echo
            } catch (Exception e) {
                if (DEBUG) {
                    System.out.println("could not send: " + command);
                }
                System.out.println(e.getClass().getName() + " " + e.getMessage());
            }
        }
    }

    /**
     * SX Net Protocol (all msg terminated with CR)
     *
     *
     * for a list of channels (which the client has set or read in the past) all
     * changes are transmitted back to the client
     */
    private void handleMsgFromServer(String msg) {
        // check whether there is an application to send info to -
        // to avoid crash if application has stopped but thread is still running

        String[] info = null;
        msg = msg.toUpperCase();

        int addr;
        int data;

        String[] cmds = msg.split(";");  // multiple commands per line possible, separated by semicolon
        for (String cmd : cmds) {
            cmd = cmd.trim();
            if (cmd.length() != 0) { // message should contain valid data
                info = cmd.split("\\s+");  // one or more whitespace
                switch (info[0]) {
                    case "X":
                    case "XLOCO":
                    case "S":    // local echo
                    case "SX":   // local echo
                        if (info.length >= 3) {
                            addr = getChannelFromString(info[1]);
                            data = getDataFromString(info[2]);
                            if ((addr != INVALID_INT) && (data != INVALID_INT)) {
                                SX4Monitor.update(addr, data);  // sxData[] get updated in SX4Monitor
                            }
                        }
                        break;
                    case "XPOWER":
                        if (info.length >= 2) {
                            data = getChannelFromString(info[1]);
                            if (data != INVALID_INT) {
                                SX4Monitor.updatePower(data);
                            }
                        }
                        break;
                    default:

                }
            }
        }
    }

    private int getDataFromString(String s) {
        // converts String to integer between 0 and 255 (=SX Data)
        Integer data = INVALID_INT;
        try {
            data = Integer.parseInt(s);
            if ((data < 0) || (data > 255)) {
                data = INVALID_INT;
            }
        } catch (Exception e) {
            data = INVALID_INT;
        }
        return data;
    }

    private int getChannelFromString(String s) {
        Integer channel = INVALID_INT;
        try {
            channel = Integer.parseInt(s);
            if ((channel >= 0) && (channel <= SXMAX)) {
                return channel;
            } else {
                channel = INVALID_INT;
            }
        } catch (Exception e) {

        }
        return channel;
    }

}
