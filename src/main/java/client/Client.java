package client;

import server.FileMetadata;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    String filePath;
    Socket socket;
    String serverAddress;
    int serverPort;
    InputStream fileInputStream;
    OutputStream socketOutputStream;
    InputStream socketInputStream;
    ObjectOutputStream objectOutputStream;

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

        byte[] buffer = new byte[8 * 1024];

        fileInputStream = new FileInputStream(file);
        socketOutputStream = socket.getOutputStream();
        socketInputStream = socket.getInputStream();
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

        if (file.getName().length() < 4 * 1024) {
            objectOutputStream.writeObject(new FileMetadata(file.length(), file.getName()));
        } else {
            System.out.println("Filename must not exceed 4096 bytes");
            close();
            return;
        }


        int count;
        while ((count = fileInputStream.read(buffer)) > 0) {
            socketOutputStream.write(buffer, 0, count);
        }

        if (socketInputStream.read(buffer) > 0) {
            System.out.println(new String(buffer, StandardCharsets.UTF_8).replaceAll("\u0000.*", ""));
        } else {
            System.out.println("Didn't get confirmation of receipt of the file");
        }


        close();
    }
}
