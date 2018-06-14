/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ftplibtcp_linux;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author sonia
 */
public class MainftpClient_TCP extends Thread {

    Socket skt;
    InputStream in;
    DataInputStream dis;
    OutputStream out;
    DataOutputStream dos;

    byte FTP_CLEINT = 1;
    byte FTP_SERVER = 2;
    byte TCP_HEADER_SIZE = 3;
    byte INIT_CONN = 1;
    byte NO_CONN_AVAIL = 2;
    byte MESG_ID_INDEX = 2;

    int ftpServerPort;
    String ftpServerIP;
    String fileName;
    String fileNameWithPath;
    int TOTAL_BUFF_SIZE = 1500;

    /*
     * Packet Header
     * byte src_csci
     * byte dst_csci
     * byte MsgId
     */
    public MainftpClient_TCP(String ftpIP, String fileTxt, String fullFileName, int ftpPort) {
        ftpServerIP = ftpIP;
        ftpServerPort = ftpPort;
        fileName = fileTxt;
        fileNameWithPath = fullFileName;
    }

    public void run() {
        try {
            RetryLogic();
            try {
                byte[] buffer = new byte[TOTAL_BUFF_SIZE];
                int len = dis.read(buffer);

                if (len > 0) {
                    if (buffer[MESG_ID_INDEX] == NO_CONN_AVAIL) {
                        skt.close();
                        in.close();
                        dis.close();
                        dos.close();
                        out.close();
                        createPopupMessage("No Connection Available at Server!!");
                    } else if (buffer[MESG_ID_INDEX] == INIT_CONN) {
                        transmitFileOnConnectionAvail();
                        skt.close();
                        in.close();
                        dis.close();
                        dos.close();
                        out.close();
                    }
                } else if (len == -1) {
                    skt.close();
                    in.close();
                    dis.close();
                    dos.close();
                    out.close();
                    RetryLogic();
                }
            } catch (IOException e) {
                skt.close();
                in.close();
                dis.close();
                dos.close();
                out.close();
                RetryLogic();
            }

        } catch (UnknownHostException ex) {

        } catch (IOException ex) {

        }
    }

    private void transmitFileOnConnectionAvail() {
        byte[] buffer = convertZipIntoByteArray();
        int len = buffer.length;

        sendFTPSessionStartUpMsg(fileName, len);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(MainftpClient_TCP.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (len <= TOTAL_BUFF_SIZE) {
            toSendTCPPacket(buffer, len);
        } else {

            int breakup = len / TOTAL_BUFF_SIZE;
            if (len % TOTAL_BUFF_SIZE != 0) {
                breakup++;
            }
            System.out.println("Total Length =" + len + " breakup=" + breakup);
            int i = 0;
            for (int j = 0; j < breakup; j++) {
               
                    byte[] sendBuffer = new byte[TOTAL_BUFF_SIZE];
                    try {
                        for (int k = 0; k < TOTAL_BUFF_SIZE && i < len; k++) {
                            sendBuffer[k] = buffer[i++];
//                            System.out.println("kkk" + k);
                        }
                    } catch (Exception ex) {
                        break;
                    }
                    toSendTCPPacket(sendBuffer, sendBuffer.length);                
            }
        }
        
    }

    private byte[] convertZipIntoByteArray() {
        File file = new File(fileNameWithPath);

        int length = 1024;
        int offset;
        byte[] buffer = new byte[length];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            //convert the file content into a byte array
            FileInputStream fileInuptStream = new FileInputStream(file);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInuptStream);

            while ((offset = bufferedInputStream.read(buffer, 0, length)) != -1) {
                byteArrayOutputStream.write(buffer, 0, offset);
            }
            bufferedInputStream.close();

            buffer = byteArrayOutputStream.toByteArray();

            byteArrayOutputStream.flush();
            byteArrayOutputStream.close();

        } catch (FileNotFoundException fileNotFoundException) {
            //fileNotFoundException.printStackTrace();
        } catch (IOException ioException) {
            //ioException.printStackTrace();
        }
        return buffer;
    }

    void toSendTCPPacket(byte[] buffer, int len) {
        if (dos != null && skt.isConnected()) {
            try {
                dos.write(buffer, 0, len); //index would show length of buffer
                dos.flush();
            } catch (IOException ex) {
            }
        }
    }

    private void sendFTPSessionStartUpMsg(String fileName, int fileContentSize) {

        byte[] buffer = new byte[TOTAL_BUFF_SIZE];
        byte index = 0;
        buffer[index++] = 1;
        byte b[] = integerToByte(fileContentSize);
        System.arraycopy(b, 0, buffer, index, b.length);
        index = (byte) (index + 4);
        byte[] tmpBytes = fileName.getBytes();
        System.out.println("######################## fileName Length=" + tmpBytes.length
                + " index=" + index + " FileName=" + fileName);
        System.arraycopy(tmpBytes, 0, buffer, index, tmpBytes.length);
        toSendTCPPacket(buffer, buffer.length);

    }

    private void RetryLogic() {
        while (true) {
            try {
                try {
                    skt = new Socket(ftpServerIP, ftpServerPort);

                    if (skt.isConnected() == true) {

                        in = skt.getInputStream();
                        dis = new DataInputStream(in);

                        out = skt.getOutputStream();
                        dos = new DataOutputStream(out);

                        return;
                    }
                } catch (IOException ex) {

                }
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(MainftpClient_TCP.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void createPopupMessage(String Desc) {
        JOptionPane.showMessageDialog(new JPanel(), Desc, "Message", JOptionPane.INFORMATION_MESSAGE);
    }

    public byte[] integerToByte(int val) {

        int MASK = 0xff;
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            int offset = (result.length - 1 - i) * 8;
            result[i] = (byte) ((val >>> offset) & MASK);
        }
        return result;
    }

//    public static void main(String[] args) {
//        new MainftpClient_TCP("192.168.101.128", "hello.txt", "/tmp/hello.txt", 26002).start();
//    }
}
