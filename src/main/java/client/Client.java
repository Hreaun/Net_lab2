package client;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class Client {
    String filePath;
    Socket socket;
    String serverAddress;
    int serverPort;

    InputStream fileInputStream;
    OutputStream socketOutputStream;
    InputStream socketInputStream;
    CheckedInputStream checkedInputStream;

    public Client(String filePath, String serverAddress, int serverPort) {
        this.filePath = filePath;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        try {
            socket = new Socket(serverAddress, serverPort);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    private void close() {
        try {
            if (socketInputStream != null) {
                socketInputStream.close();
            }
            if (socketOutputStream != null) {
                socketOutputStream.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    void sendFile() {
        File file;
        try {
            try {
                file = new File(filePath);
            } catch (NullPointerException e) {
                System.out.println("Cannot find a file with the given name");
                return;
            }
            if (file.length() > 1e12) {
                System.out.println("The file size must not exceed 1TB");
                return;
            }

            fileInputStream = new FileInputStream(file);
            socketOutputStream = socket.getOutputStream();
            socketInputStream = socket.getInputStream();
            checkedInputStream = new CheckedInputStream(fileInputStream, new CRC32());

            byte[] buffer;

            if (file.getName().length() <= 4 * 1024) {
                buffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.BIG_ENDIAN).putInt((int) file.length()).array();
                socketOutputStream.write(buffer, 0, Integer.BYTES);
                buffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.BIG_ENDIAN).putInt(file.getName().length()).array();
                socketOutputStream.write(buffer, 0, Integer.BYTES);
                buffer = file.getName().getBytes();
                socketOutputStream.write(buffer);
            } else {
                System.out.println("Filename must not exceed 4096 bytes");
                return;
            }

            buffer = new byte[8 * 1024];
            int count;
            while ((count = checkedInputStream.read(buffer)) > 0) {
                socketOutputStream.write(buffer, 0, count);
            }

            byte[] okMessage = new byte[2];
            socketInputStream.read(okMessage);
            if (!"OK".equals(new String(okMessage, StandardCharsets.UTF_8))){
                System.out.println("Didn't get confirmation of receipt of the file");
                return;
            }

            socketOutputStream.write(ByteBuffer.allocate(Long.BYTES).putLong(checkedInputStream.getChecksum().getValue()).array());


            buffer = new byte[40];
            if (socketInputStream.read(buffer) > 0) {
                System.out.println(new String(buffer, StandardCharsets.UTF_8).replaceAll("\u0000.*", ""));
            } else {
                System.out.println("Didn't get confirmation of receipt of the file");
            }
        } catch (NullPointerException e) {
            System.out.println("Server is down");
        } catch (IOException e) {
            System.out.println("Connection closed");
            System.out.println(e.getMessage());
        } finally {
            close();
        }

    }
}
