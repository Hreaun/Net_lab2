package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server extends Thread {
    private final int CORE_POOL_SIZE = 5;
    private final int MAX_POOL_SIZE = 40;
    private final int KEEP_ALIVE_TIME = 10;
    int port;
    ServerSocket s;

    public Server(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        ThreadPoolExecutor executorPool = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(10_000));
        try {
            s = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Port is busy");
            e.printStackTrace();
            return;
        }

        while (!isInterrupted()) {
            Socket clientSocket;
            try {
                clientSocket = s.accept();
            } catch (IOException e) {
                System.out.println("Server is closed");
                return;
            }
            System.out.println("Client " + clientSocket.getInetAddress().toString()
                    + "  " + clientSocket.getPort());
            executorPool.execute(new Connection(clientSocket));
        }
    }

    public void close() {

    }
}
