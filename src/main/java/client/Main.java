package client;

public class Main {
    public static void main(String[] args) {
        Client client = new Client(args[0], args[1], Integer.parseInt(args[2]));
        client.sendFile();
    }
}
