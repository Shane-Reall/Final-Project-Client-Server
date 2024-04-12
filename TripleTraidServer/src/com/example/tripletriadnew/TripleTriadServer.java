package com.example.tripletriadnew;

import java.io.*;
import java.net.*;
import java.util.Arrays;

public class TripleTriadServer {

    private static final int PORT = 5555;

    public static void main(String[] args) throws IOException {
        int clientCount = 0;
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server Started. Waiting for clients...");

        ObjectInputStream playerOneInput = null;
        ObjectOutputStream playerOneOutput = null;
        ObjectInputStream playerTwoInput = null;
        ObjectOutputStream playerTwoOutput = null;
        Socket playerOneSocket = null;
        Socket playerTwoSocket = null;

        while (clientCount < 2) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());

            if (clientCount == 0) {
                playerOneInput = input;
                playerOneOutput = output;
                playerOneSocket = clientSocket;

            } else {
                playerTwoInput = input;
                playerTwoOutput = output;
                playerTwoSocket = clientSocket;
            }

            // Start a new thread to handle the client connection
            ClientHandler handler = new ClientHandler(clientSocket, playerOneInput, playerOneOutput, playerTwoInput, playerTwoOutput, input, output, playerOneSocket, playerTwoSocket);
            handler.start();
            clientCount++;
        }

        System.out.println("Max Number of Clients Reached");
    }
}

class ClientHandler extends Thread {
    private static int clientCount = 0;
    private Socket clientSocket;
    private final Socket playerOneSocket;
    private final Socket playerTwoSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private ObjectInputStream playerOneInput;
    private ObjectInputStream playerTwoInput;
    private ObjectOutputStream playerOneOutput;
    private ObjectOutputStream playerTwoOutput;
    private int clientId;
    private int fileCounter;
    private CardClass[][] boardStatus = new CardClass[3][3];

    public ClientHandler(Socket socket, ObjectInputStream playerOneInput, ObjectOutputStream playerOneOutput, ObjectInputStream playerTwoInput, ObjectOutputStream playerTwoOutput, ObjectInputStream input, ObjectOutputStream output, Socket playerOneSocket, Socket playerTwoSocket) {
        this.clientSocket = socket;
        this.clientId = ++clientCount; // Assign a unique ID to each client
        this.fileCounter = 0;

        this.playerOneInput = playerOneInput;
        this.playerOneOutput = playerOneOutput;
        this.playerOneSocket = playerOneSocket;
        this.playerTwoInput = playerTwoInput;
        this.playerTwoOutput = playerTwoOutput;
        this.playerTwoSocket = playerTwoSocket;

        this.input = input;
        this.output = output;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object received = input.readObject();

                if (received instanceof String && received.equals("Ready")) {
                    if (clientCount == 2) {

                        playerOneOutput.writeObject("Ready");
                        playerOneOutput.flush();

                        playerTwoOutput.writeObject("Ready");
                        playerTwoOutput.flush();

                        System.out.println("Sent");
                    }
                }
                else if (received instanceof byte[]) {
                    // Handle file transfer
                    FileOutputStream fileOutputStream;
                    byte[] receivedBytes = (byte[]) received;
                    String fileName = (++fileCounter) + ".ser";
                    if (clientId == 1) {
                        fileOutputStream = new FileOutputStream("PlayerOne/" + fileName);
                    }
                    else {
                        fileOutputStream = new FileOutputStream("PlayerTwo/" +fileName);
                    }
                    fileOutputStream.write(receivedBytes);
                    fileOutputStream.close();
                } else if (received instanceof CardClass[][]) {
                    Object[][] receivedArray = (Object[][]) received;
                    boardStatus = (CardClass[][]) receivedArray;
                    System.out.println("Received array of " + receivedArray.length + " objects.");
                    System.out.println(Arrays.deepToString(boardStatus));
                } else if (received instanceof String && received.equals("Flip") && clientId == 2) {
                    synchronized (ClientHandler.class) {
                        sendFilesBack(playerOneSocket, playerTwoSocket);
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

    private void sendFilesBack(Socket playerOneSocket, Socket playerTwoSocket) {
        try {

            // Sending files from PlayerTwo to PlayerOne
            for (int i = 1; i <= 5; i++) {
                String fileName = "PlayerOne/" + i + ".ser";
                FileInputStream fileInputStream = new FileInputStream(fileName);
                byte[] fileData = fileInputStream.readAllBytes();

                OutputStream out = playerTwoSocket.getOutputStream();
                out.write(fileData);
                out.flush();

                Thread.sleep(500);

                fileInputStream.close();

                System.out.println("Sent " + fileName);
            }

            // Sending files from PlayerOne to PlayerTwo
            for (int i = 1; i <= 5; i++) {
                String fileName = "PlayerTwo/" + i + ".ser";
                FileInputStream fileInputStream = new FileInputStream(fileName);
                byte[] fileData = fileInputStream.readAllBytes();

                OutputStream out = playerOneSocket.getOutputStream();
                out.write(fileData);
                out.flush();

                Thread.sleep(500);

                fileInputStream.close();

                System.out.println("Sent " + fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}