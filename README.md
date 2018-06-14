# FTP_APP
FTP Application to transfer files using TCP method.
Download FTP_APP project. 
Code is developed using netbeans-8.0.2 IDE.
Code language is java and compilation is done in jdk-1.8.
This ocde is giving library and need to compile with your code to use it with end applications.
To test this library, Open the complete project in netbeans.
Uncomment the main() for two files: 1. MainftpServer_TCP.java  2. MainftpClient_TCP.java
Compile the complete project "FTP_APP"
First run "java -ajr MainftpServer_TCP.java <ftpPort> <ftpPath>"
After that run "java -jar MainftpClient_TCP <FTP_serverIPAddr>, <ftpFileNameOnly>  <ftpFileNameWithPath> <ftpPath>"
