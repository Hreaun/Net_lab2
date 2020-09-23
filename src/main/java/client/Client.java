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
    ObjectOutputStream objectOutputStream;
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
            if (objectOutputStream != null) {
                objectOutputStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    void sendFile() throws IOException {
        File file;
        try {
            file = new File(filePath);
        } catch (NullPointerException e) {
            System.out.println("Cannot find the file with given name");
            return;
        }
        if (file.length() > 1e12) {
            System.out.println("The file size must not exceed 1TB");
            close();
            return;
        }


        fileInputStream = new FileInputStream(file);
        socketOutputStream = socket.getOutputStream();
        socketInputStream = socket.getInputStream();
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
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
            close();
            return;
        }

        buffer = new byte[8 * 1024];
        int count;
        while ((count = checkedInputStream.read(buffer)) > 0) {
            socketOutputStream.write(buffer, 0, count);
        }

        socketInputStream.read(buffer);
        if (!"Got filesize bytes".equals(new String(buffer, StandardCharsets.UTF_8).replaceAll("\u0000.*", ""))) {
            System.out.println("Didn't get confirmation of receipt of the file");
        }


        socketOutputStream.write(ByteBuffer.allocate(Long.BYTES).putLong(checkedInputStream.getChecksum().getValue()).array());


        if (socketInputStream.read(buffer) > 0) {
            System.out.println(new String(buffer, StandardCharsets.UTF_8).replaceAll("\u0000.*", ""));
        } else {
            System.out.println("Didn't get confirmation of receipt of the file");
        }


        close();
    }
}
