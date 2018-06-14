    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ftplibtcp_linux;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

/**
 *
 * @author root
 */
public class MainftpServer_TCP extends Thread {

//    public static void main(String args[]) {
//        MainftpServer_TCP server_mail = new MainftpServer_TCP(26002, "/tmp/");
//        server_mail.start();
//    }
    
    byte MAX_FTP_CLIENT = 10;    
    byte FTP_CLEINT = 1;
    byte FTP_SERVER = 2;
    byte TCP_HEADER_SIZE = 3;
    byte CONN_AVAIL = 1;
    byte NO_CONN_AVAIL = 2;
    int ftpServerPort;
    private final String ftpDownLoadPath;
    ftpConn_Processing[] clientThread = new ftpConn_Processing[10];

    public MainftpServer_TCP(int ftpPort, String ftpPath) {
        ftpServerPort = ftpPort;
        ftpDownLoadPath = ftpPath;
    }

    public void run() {

        try {
            ServerSocket srvrSocket = new ServerSocket(ftpServerPort);

            while (true) {
                Socket sktClient = srvrSocket.accept();

                if (checkTcpConnAvailability()) {
                    System.out.println("More Connection Available $$$$$$$$$$$");
                    for (byte i = 0; i < MAX_FTP_CLIENT; i++) {
                        if (clientThread[i] == null || !clientThread[i].isAlive()) {
                            String connClientIP = sktClient.getInetAddress().getHostAddress();
                            System.out.println("value of i " + i + " connClientIP " + connClientIP);
                            (clientThread[i] = new ftpConn_Processing(sktClient, i, connClientIP, ftpDownLoadPath)).start();
                            break;
                        }
                    }
                } else {
                    sendConnCloseMsgHeader(sktClient);
                }

                Thread.sleep(1000);
            }
        } catch (IOException e) {
            System.out.println("Whoops! It didn't work!" + e.toString());
        } catch (InterruptedException e) {
            System.out.println("Whoops! It didn't work!" + e.toString());
        }
    }

    private boolean checkTcpConnAvailability() {
        for (byte i = 0; i < MAX_FTP_CLIENT; i++) {
            if (clientThread[i] == null || !clientThread[i].isAlive()) {
                return true;
            }
        }
        return false;
    }

    private void sendConnCloseMsgHeader(Socket sktClient) {
        try {
            byte[] buffer = new byte[TCP_HEADER_SIZE];
            buffer[0] = FTP_SERVER;
            buffer[1] = FTP_CLEINT;
            buffer[2] = NO_CONN_AVAIL;
            OutputStream out = sktClient.getOutputStream();
            DataOutputStream dos = new DataOutputStream(out);
            dos.write(buffer);
            dos.flush();
            sktClient.close();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(ftpConn_Processing.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
