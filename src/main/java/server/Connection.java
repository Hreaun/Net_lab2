package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Connection implements Runnable {
    private final Socket clientSocket;
    private File outFile;
    FileMetadata fileMetadata;
    InputStream socketInputStream;
    ObjectInputStream objectInputStream;
    OutputStream fileOutputStream;
    OutputStream socketOutputStream;


    public Connection(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    private void nameCheck(String clientsFilename) {
        outFile = new File("./uploads/" + clientsFilename);
        outFile.getParentFile().mkdirs();
        int counter = 1;
        try {
            while (!outFile.createNewFile()) {
                outFile = new File("./uploads/" + counter++ + clientsFilename);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            close();
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
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void run() {
        try {
            socketInputStream = clientSocket.getInputStream();
            socketOutputStream = clientSocket.getOutputStream();
            objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException ex) {
            System.out.println("Can't get socket input/output stream");
            return;
        }

        try {
            fileMetadata = (FileMetadata) objectInputStream.readObject();
            if (fileMetadata.filename == null) {
                System.out.println("Filename is not provided");
                close();
                return;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }

        nameCheck(fileMetadata.filename);

        try {
            fileOutputStream = new FileOutputStream(outFile);
        } catch (FileNotFoundException ex) {
            System.out.println("File not found");
            close();
            return;
        }

        byte[] buffer = new byte[8 * 1024];

        int count;
        try {
            clientSocket.setSoTimeout(2000);
            while ((count = socketInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, count);
            }
        } catch (SocketTimeoutException ignored) {
        } catch (IOException e) {
            System.out.println(e.getMessage());
            close();
        }

        try {
            if (outFile.length() == fileMetadata.fileSize) {
                socketOutputStream.write("Got the file successfully".getBytes());
            } else {
                socketOutputStream.write("The file transfer failed".getBytes());
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            close();
        }

        close();


    }
}
