package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class Connection implements Runnable {
    private final int WAIT_TIME = 30_000;
    private final Socket clientSocket;
    private File outFile;
    InputStream socketInputStream;
    OutputStream fileOutputStream;
    OutputStream socketOutputStream;
    CheckedInputStream checkedInputStream;


    public Connection(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    private void nameCheck(String clientsFilename) throws IOException {
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
            throw e;
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
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            try {
                socketInputStream = clientSocket.getInputStream();
                socketOutputStream = clientSocket.getOutputStream();
                checkedInputStream = new CheckedInputStream(socketInputStream, new CRC32());
            } catch (IOException ex) {
                System.out.println("Can't get socket input/output stream");
                return;
            }

            byte[] fileSize = new byte[Integer.BYTES];
            byte[] nameSize = new byte[Integer.BYTES];
            byte[] fileName;


            if (socketInputStream.read(fileSize, 0, Integer.BYTES) != Integer.BYTES) {
                System.out.println("Didn't get file size");
                return;
            }
            if (socketInputStream.read(nameSize, 0, Integer.BYTES) != Integer.BYTES) {
                System.out.println("Didn't get filename size");
                return;
            }
            fileName = new byte[ByteBuffer.wrap(nameSize).getInt()];
            if (socketInputStream.read(fileName) != fileName.length) {
                System.out.println("Didn't get file name");
                return;
            }

            nameCheck(new String(fileName, StandardCharsets.UTF_8));

            try {
                fileOutputStream = new FileOutputStream(outFile);
            } catch (FileNotFoundException ex) {
                System.out.println("File not found");
                return;
            }

            byte[] buffer = new byte[8 * 1024];
            int fileSizeInt = ByteBuffer.wrap(fileSize).getInt();

            int count;
            int sizeCounter = 0;

            clientSocket.setSoTimeout(WAIT_TIME);
            while ((count = checkedInputStream.read(buffer)) > 0) {
                clientSocket.setSoTimeout(WAIT_TIME);
                fileOutputStream.write(buffer, 0, count);
                sizeCounter += count;
                if (sizeCounter >= fileSizeInt) {
                    break;
                }
            }

            byte[] clientChecksumBytes = new byte[Long.BYTES];

            socketOutputStream.write("OK".getBytes());
            clientSocket.setSoTimeout(WAIT_TIME);
            if (socketInputStream.read(clientChecksumBytes, 0, Long.BYTES) != Long.BYTES) {
                socketOutputStream.write("The file transfer failed".getBytes());
            }

            long clientChecksum = ByteBuffer.wrap(clientChecksumBytes).getLong();
            long serverChecksum = checkedInputStream.getChecksum().getValue();


            if ((outFile.length() == fileSizeInt) && (clientChecksum == serverChecksum)) {
                socketOutputStream.write("The file uploaded successfully".getBytes());
            } else {
                socketOutputStream.write("The file transfer failed".getBytes());
            }

        } catch (SocketTimeoutException ignored) {
            System.out.println("Disconnected");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            close();
        }
    }
}
