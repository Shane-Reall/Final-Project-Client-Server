import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TripleTriadServer {

    private static final int PORT = 5555;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server Started. Waiting for clients...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            // Start a new thread to handle the client connection
            ClientHandler handler = new ClientHandler(clientSocket);
            handler.start();
        }
    }
}

class ClientHandler extends Thread {
    private static int clientCount = 0;
    private Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private int clientId;
    private int fileCounter;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.clientId = ++clientCount; // Assign a unique ID to each client
        this.fileCounter = 0;
        try {
            input = new ObjectInputStream(clientSocket.getInputStream());
            output = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object received = input.readObject();

                if (received instanceof byte[]) {
                    // Handle file transfer
                    byte[] receivedBytes = (byte[]) received;
                    String fileName = "received_file_" + (++fileCounter) + ".ser";
                    FileOutputStream fileOutputStream = new FileOutputStream(fileName);
                    fileOutputStream.write(receivedBytes);
                    fileOutputStream.close();
                    System.out.println("File received and saved: " + fileName);
                } else if (received instanceof String) {
                    String message = (String) received;
                    if (message.equalsIgnoreCase("QUIT")) {
                        break;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) {
                    System.out.println("Closing connection for Client " + clientId);
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
