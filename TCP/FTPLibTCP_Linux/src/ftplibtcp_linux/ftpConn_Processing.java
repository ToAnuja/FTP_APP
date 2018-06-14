/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ftplibtcp_linux;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author root
 */
class ftpConn_Processing extends Thread {

    private DataInputStream dis;
    private DataOutputStream dos;
    private final Socket clientSocket;

    private int fileSizeBytes = 0;
    private byte StartUp = 0;
    int count=0;
    private int recvPktCount = 0;
    private byte[] dstbuffer = null;
    private String fileName = "";
    int TOTAL_BUFF_SIZE = 1500;
    private final byte FTP_CLEINT = 1;
    private final byte FTP_SERVER = 2;
    private final byte TCP_HEADER_SIZE = 3;
    private final byte INIT_CONN = 1;

    private final String downloadFtpPath;

    ftpConn_Processing(Socket sktClient, int threadCount, String clientIP, String downloadPath) {
        System.out.println("ftpConn_Processing is established ="+clientIP+" threadCount="+threadCount);
        clientSocket = sktClient;
        downloadFtpPath = downloadPath;
    }

    @Override
    public void run() {

        try {
            if (!clientSocket.isClosed()) {
                OutputStream out = clientSocket.getOutputStream();
                dos = new DataOutputStream(out);

                if (dos != null && clientSocket.isConnected()) {
                    //send initial message header
                    sendInitialMsgHeader();
                    InputStream in = clientSocket.getInputStream();
                    dis = new DataInputStream(in);

                    while (true) {
                        byte[] buffer = new byte[TOTAL_BUFF_SIZE];
                        if (dis != null && clientSocket.isConnected()) {
                            int len = dis.read(buffer);
                            if (len > 0) {
                                analyzeRecvPacket(buffer);
                            } else {
                                dis.close();
                                dos.close();
                                clientSocket.close();
                                break;
                            }
                        } else {
                            dis.close();
                            dos.close();
                            clientSocket.close();
                            break;
                        } 
                    }
                }
            } 

        } catch (IOException ex) {
            try {
                clientSocket.close();
            } catch (IOException ex1) {
            }
        } 
    }

    private void sendInitialMsgHeader() {
        try {
            byte[] buffer = new byte[TCP_HEADER_SIZE];
            buffer[0] = FTP_CLEINT;
            buffer[1] = FTP_SERVER;
            buffer[2] = INIT_CONN;
            dos.write(buffer);
            dos.flush();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ftpConn_Processing.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void analyzeRecvPacket(byte[] recvBuff) {
        count++;
        int index = 0;
        byte indexByte = recvBuff[index++];
        System.out.println("bufer[0]=" + indexByte+" count="+count); 
        if (indexByte == 1 && StartUp == 0) {
            StartUp = 1;
            fileSizeBytes = byteToInteger(recvBuff, index);
            System.out.println("RECVVVVVV fileSizeBytes  =" + fileSizeBytes);
            index += 4;
            
            byte[] fileSize = new byte[100];
            System.arraycopy(recvBuff, index, fileSize, 0, fileSize.length);

            fileName = new String(recvBuff, index, fileSize.length).trim();

            int breakup = 1;
            if (fileSizeBytes > TOTAL_BUFF_SIZE) {
                breakup = fileSizeBytes / TOTAL_BUFF_SIZE;
                int extraCnt = fileSizeBytes % TOTAL_BUFF_SIZE;
                if (extraCnt != 0) {
                    breakup = breakup + 1;
                }
            } 
            dstbuffer = new byte[TOTAL_BUFF_SIZE * breakup];
            System.out.println("Total Length of File=" + fileSizeBytes + " breakup=" + breakup);
            recvPktCount = 0;
        } else if (StartUp == 1) {
            if (dstbuffer != null) {
                
                System.arraycopy(recvBuff, 0, dstbuffer, recvPktCount, recvBuff.length);
                recvPktCount += recvBuff.length;
                System.out.println("recvPktCount=" + recvPktCount + " fileSizeBytes=" + fileSizeBytes);
                if (recvPktCount > fileSizeBytes) {
                    WritingZipFile(fileName, dstbuffer, fileSizeBytes);
                    dstbuffer = null;
                    recvPktCount = 0;
                    fileSizeBytes = 0;
                }
            }
        }
    }

    private void WritingZipFile(String fileName, byte[] buff, int len) {
        try {
            String fileWithPath = new StringBuilder(downloadFtpPath).append(fileName).toString();
            System.out.println("RECVVVVVVVVVVVVVvv WritingZipFile filenam=" + fileWithPath + " len= " + len + " buffLen=" + buff.length);
//            for (int i = 0; i < len; i++) {
//                System.out.println("buffer[" + i + "]=" + buff[i]);
//            }
            FileOutputStream fileOutputStream = new FileOutputStream(fileWithPath);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            bufferedOutputStream.write(buff, 0, len);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        } catch (IOException ex) {
            Logger.getLogger(ftpConn_Processing.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int byteToInteger(byte[] b, int offset) {
        int num = 0;
        for (int i = 0; i < 4; i++) {
            num <<= 8;
            num |= b[offset + i] & 0xFF;

        }
        return num;
    }

}
