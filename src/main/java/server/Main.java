package server;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Enter port of the server");
            return;
        }
        Server server;
        try {
            server = new Server(Integer.parseInt(args[0]));
        } catch (NumberFormatException e) {
            System.out.println("Enter port of the server");
            return;
        }

        server.start();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Press enter to close the server");
        scanner.nextLine();
        server.close();
        server.interrupt();
        try {
            server.join();
        } catch (InterruptedException ignored) {
        }
    }
}
