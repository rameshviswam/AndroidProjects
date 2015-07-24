package com.ramesh.rvcam;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Deque;

public class TCPServerThread implements Runnable {
    public static final int SERVER_PORT = 1456;
    public RV_Camera mainClass;
    private ServerSocket server;
    private Socket socket;

    private boolean isSendingDataReady = false;
    private BufferedReader streamIn;
    private DataInputStream streamIn_new;
    private DataOutputStream streamOut;

    private Deque<Byte> receiveBuffer = new ArrayDeque<Byte>(256);
    private Deque<Byte> sendBuffer = new ArrayDeque<Byte>(256);

    private boolean isReceivingPayload = false;
    private int payloadId = 0;
    private int payloadSize = 0;

    public static final int HEADER_SIZE = 4;

    private void sendEchoBack(int id) {
        byte[] sendBuf = { 'P', 'I', 'N', 'G', ' ', 'A', 'C', 'K'};
        sendResponse(sendBuf, id+1, sendBuf.length);
    }

    public void sendSnapshotResponse(int id, byte[] sendBuf) {
        Log.e("RV..." , "id: "+ id);
        Log.e("RV...", "length: "+ sendBuf.length);
        //Log.e("RV...", sendBuf.toString());
        sendResponse(sendBuf, id, sendBuf.length);
    }

    private void sendSnapshotResponse(int id) {
        byte[] sendBuf = {1, 2, 3, 4, 5, 6};
        sendResponse(sendBuf, id+1, sendBuf.length);
    }

    private void sendResponse(byte[] sendBuf, int id, int length)  {
        for(int i =0; i < sendBuf.length; ++i) {
            sendBuffer.push(sendBuf[length - i -1]);
        }

        byte idByteArr[] = BigInteger.valueOf(id).toByteArray();
        for(int i = 0; i < 4 - idByteArr.length; ++i) {
            sendBuffer.push((byte)0);
        }

        for(int i = 0; i < idByteArr.length; ++i) {
            sendBuffer.push(idByteArr[i]);
        }

        byte lengthByteArr[] = BigInteger.valueOf(length).toByteArray();
        //Log.e("RV...", "lengthByteArr " + lengthByteArr.length);

        for(int i = 0; i < 4 - lengthByteArr.length; ++i) {
            sendBuffer.push((byte)0);
        }

        for(int i = 0; i < lengthByteArr.length; ++i) {
            sendBuffer.push(lengthByteArr[i]);
        }
        isSendingDataReady = true;
    }

    public void parseReceivedPacket() {

        if(isReceivingPayload == false) {
            if(receiveBuffer.size() >= 2 * HEADER_SIZE ){
                byte[] length = new byte[HEADER_SIZE];
                for(int i=0; i < HEADER_SIZE; i++) {
                    length[i] = receiveBuffer.pop();
                }
                ByteBuffer buffer = ByteBuffer.wrap(length);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                payloadSize = buffer.getInt();

                byte[] length1 = new byte[HEADER_SIZE];
                for(int i=0; i < HEADER_SIZE; i++) {
                    length1[i] = receiveBuffer.pop();
                }
                ByteBuffer buffer1 = ByteBuffer.wrap(length1);
                buffer1.order(ByteOrder.LITTLE_ENDIAN);
                payloadId = buffer1.getInt();

                isReceivingPayload = true;
            }
        }

        if(isReceivingPayload == true) {
            if(receiveBuffer.size() >= payloadSize) {
                byte[] payload = new byte[payloadSize];
                for(int i=0; i < payloadSize; i++) {
                    payload[payloadSize - i -1] = receiveBuffer.pop();
                }
                if(payloadId == 1) {
                    sendEchoBack(payloadId);
                }
                else if(payloadId == 3) {
                   mainClass.takePicture();
                    Log.e("RV....", "statrring takePicture");
                    //sendSnapshotResponse(payloadId);
                }
            }
            isReceivingPayload = false;
        }
    }

    public void run() {
        try {
            boolean done = false;

            openSocket();
            openStream();

            while (done == false) {
                try {
                    byte[] line = new byte[256];
                    if(streamIn_new.available() >0) {
                        int readData = streamIn_new.read(line, 0, 256);

                        Log.e("RV....", "inside run loop2");

                        if (readData > 0) {
                            for (int i = 1; i <= readData; ++i) {
                                receiveBuffer.push(line[readData - i]);
                            }
                            Log.e("RV....:", "received data" + readData);
                        }
                        //Log.e("RV....", "inside loop");
                        Log.e("RV....", "inside run loop3");
                        while (receiveBuffer.isEmpty() == false) {
                            parseReceivedPacket();
                        }
                    }
                    if(isSendingDataReady == true) {
                        if (sendBuffer.isEmpty() != true) {
                            //Log.e("RV...", "in sendBuffer: " + sendBuffer.size());
                            int sendSize = sendBuffer.size();
                            byte[] sendBuf = new byte[sendSize];

                            for (int i = 0; i < sendSize; ++i) {
                                sendBuf[i] = sendBuffer.pop();
                            }
                            streamOut.write(sendBuf, 0, sendSize);
                            Log.e("RV...", "sent data: " + sendSize);

                        }
                        isSendingDataReady = false;
                    }
                } catch (IOException ioe) {
                    Log.e("RV..EXCEPT.....", ioe.getLocalizedMessage());
                    done = true;
                }
            }
            closeStream();
        } catch (IOException ioe) {
            Log.e("RV..EXCEPT.....", ioe.getLocalizedMessage());
        }
        finally {
            try {
                closeStream();
                closeSocket();
            } catch(IOException ioe) {
                Log.e("RV..EXCEPT.....", ioe.getLocalizedMessage());
            }
        }
    }

    private void openSocket() throws IOException {
        System.out.println("Binding to port " + SERVER_PORT + ", please wait  ...");
        server = new ServerSocket(SERVER_PORT);
        Log.e("RV..............", "Server started: " + server);

        Log.e("RV............", "Waiting for a client ...");
        socket = server.accept();
        Log.e("RV............", ("Client accepted: " + socket));
    }

    private void openStream() throws IOException {
        //streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        streamIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        streamOut = new DataOutputStream(socket.getOutputStream());
        streamIn_new = new DataInputStream(socket.getInputStream());
    }

    private void closeSocket() throws IOException {
        if (socket != null)
            socket.close();
    }

    private void closeStream() throws IOException {
        if (streamIn != null)
            streamIn.close();
        if (streamIn_new != null)
            streamIn_new.close();

        if (streamOut != null)
            streamOut.close();
    }
}
