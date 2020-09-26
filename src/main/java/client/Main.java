package client;

public class Main {
    public static void main(String[] args) {
        try {
            Client client = new Client(args[0], args[1], Integer.parseInt(args[2]));
            client.sendFile();
        } catch (NumberFormatException e) {
            System.out.println("Enter server's port");
        }
    }
}
