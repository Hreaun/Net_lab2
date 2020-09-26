package server;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class Connection implements Runnable {
    private final int WAIT_TIME = 3_000;
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

    private void printSpeed(long bytesPerLastSecs, long bytesReceived, Instant lastMeasureTime, Instant startRecvTime) {
        System.out.println(clientSocket.getInetAddress() + "  " + clientSocket.getPort() +
                " Speed: " + (bytesPerLastSecs / (Duration.between(lastMeasureTime, Instant.now()).toMillis())) / (long) 1e3 +
                "MB/s, Avg speed: " + (bytesReceived / (Duration.between(startRecvTime, Instant.now()).toMillis())) / (long) 1e3 + "MB/s");
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

            byte[] fileSize = new byte[Long.BYTES];
            byte[] nameSize = new byte[Integer.BYTES];
            byte[] fileName;


            if (socketInputStream.read(fileSize, 0, Long.BYTES) != Long.BYTES) {
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

            byte[] buffer = new byte[4 * 1024];
            long fileSizeLong = ByteBuffer.wrap(fileSize).getLong();

            int count;
            int timeoutCounter = 0;
            long bytesReceived = 0;
            long bytesPerLastSecs = 0;

            Instant startRecvTime = Instant.now();
            Instant lastMeasureTime = Instant.now();

            clientSocket.setSoTimeout(WAIT_TIME);


            while (bytesReceived < fileSizeLong) {
                try {
                    count = checkedInputStream.read(buffer);
                    bytesReceived += count;
                    bytesPerLastSecs += count;
                    if (Duration.between(lastMeasureTime, Instant.now()).toMillis() >= 3e3) {
                        printSpeed(bytesPerLastSecs, bytesReceived, lastMeasureTime, startRecvTime);
                        bytesPerLastSecs = 0;
                        lastMeasureTime = Instant.now();
                    }
                    timeoutCounter = 0;
                    clientSocket.setSoTimeout(WAIT_TIME);
                    fileOutputStream.write(buffer, 0, count);

                } catch (SocketTimeoutException e) {
                    printSpeed(bytesPerLastSecs, bytesReceived, lastMeasureTime, startRecvTime);
                    bytesPerLastSecs = 0;
                    lastMeasureTime = Instant.now();
                    timeoutCounter++;
                    if (timeoutCounter >= 10) {
                        break;
                    }
                }

            }

            if (Duration.between(startRecvTime, Instant.now()).toMillis() <= 3e3) {
                printSpeed(bytesPerLastSecs, bytesReceived, lastMeasureTime, startRecvTime);
            }


            byte[] clientChecksumBytes = new byte[Long.BYTES];

            socketOutputStream.write("OK".getBytes());
            clientSocket.setSoTimeout(WAIT_TIME * 10);
            if (socketInputStream.read(clientChecksumBytes, 0, Long.BYTES) != Long.BYTES) {
                socketOutputStream.write("The file transfer failed".getBytes());
            }

            long clientChecksum = ByteBuffer.wrap(clientChecksumBytes).getLong();
            long serverChecksum = checkedInputStream.getChecksum().getValue();


            if ((outFile.length() == fileSizeLong) && (clientChecksum == serverChecksum)) {
                socketOutputStream.write("The file uploaded successfully".getBytes());
                System.out.println("Got file from " + clientSocket.getInetAddress() + "  " + clientSocket.getPort() + " successfully");
            } else {
                socketOutputStream.write("The file transfer failed".getBytes());
                System.out.println("Got corrupted file from " + clientSocket.getInetAddress() + "  " + clientSocket.getPort());
            }
        } catch (SocketTimeoutException e) {
            System.out.println(clientSocket.getInetAddress() + "  " + clientSocket.getPort() + " disconnected");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            close();
        }
    }
}
