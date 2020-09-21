package client;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Client client = new Client(args[0], args[1], Integer.parseInt(args[2]));
        try {
            client.sendFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
