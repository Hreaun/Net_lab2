package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server extends Thread {
    private final int CORE_POOL_SIZE = 1;
    private final int MAX_POOL_SIZE = 40;
    private final int KEEP_ALIVE_TIME = 10;
    int port;
    ServerSocket s;
    ThreadPoolExecutor executorPool;
    ArrayList<Socket> clients = new ArrayList<>();

    public Server(int port) throws IOException {
        this.port = port;
        try {
            s = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Port is busy");
            throw e;
        }
    }

    @Override
    public void run() {
        executorPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));

        while (!isInterrupted()) {
            Socket clientSocket;
            try {
                clientSocket = s.accept();
            } catch (IOException e) {
                System.out.println("Server is closed");
                return;
            }
            clients.add(clientSocket);
            System.out.println("Client " + clientSocket.getInetAddress().toString()
                    + "  " + clientSocket.getPort() + " connected");
            executorPool.execute(new Connection(clientSocket));
        }
    }

    public void close() {
        try {
            clients.forEach(e -> {
                try {
                    e.close();
                } catch (IOException ioException) {
                    System.out.println("Cannot close client " + e.getInetAddress() + "  " + e.getPort());
                }
            });
            s.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        executorPool.shutdownNow();
    }
}
