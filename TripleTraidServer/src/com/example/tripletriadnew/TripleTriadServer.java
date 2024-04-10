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

        while (clientCount < 2) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            // Start a new thread to handle the client connection
            ClientHandler handler = new ClientHandler(clientSocket);
            handler.start();
            clientCount++;
        }

        System.out.println("Max Number of Clients Reached");
    }
}

class ClientHandler extends Thread {
    private static int clientCount = 0;
    private Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private int clientId;
    private int fileCounter;
    private CardClass[][] boardStatus = new CardClass[3][3];

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

                if (received instanceof String && received.equals("Ready")) {
                    if (clientCount == 2) {
                        // Both clients are ready; send a readiness signal back to each client
                        output.writeObject("Ready");
                        output.flush();

                        System.out.println("Sent");
                    }
                }
                else if (received instanceof byte[]) {
                    // Handle file transfer
                    FileOutputStream fileOutputStream;
                    byte[] receivedBytes = (byte[]) received;
                    String fileName = (++fileCounter) + ".ser";
                    if (clientCount == 1) {
                        fileOutputStream = new FileOutputStream("PlayerOne/" + fileName);
                    }
                    else {
                        fileOutputStream = new FileOutputStream("PlayerTwo/" +fileName);
                    }
                    fileOutputStream.write(receivedBytes);
                    fileOutputStream.close();

                    if (clientCount == 2) {
                        sendFilesBack();
                    }
                } else if (received instanceof CardClass[][]) {
                    Object[][] receivedArray = (Object[][]) received;
                    boardStatus = (CardClass[][]) receivedArray;
                    System.out.println("Received array of " + receivedArray.length + " objects.");
                    System.out.println(Arrays.deepToString(boardStatus));
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

    private void sendFilesBack() {
        try {
            for (int i = 0; i < 5; i++) {
                File playerOneFile = new File("PlayerOne/" + i + ".ser");
                if (playerOneFile.exists()) {
                    FileInputStream playerOneInputStream = new FileInputStream(playerOneFile);
                    byte[] playerOneBytes = playerOneInputStream.readAllBytes();
                    playerOneInputStream.close();

                    FileOutputStream playerTwoOutputStream = new FileOutputStream("PlayerOne/" + i + ".ser");
                    playerTwoOutputStream.write(playerOneBytes);
                    playerTwoOutputStream.close();

                    System.out.println("File sent back to PlayerTwo");
                }
            }

            for (int i = 0; i < 5; i++) {
                File playerTwoFile = new File("PlayerTwo/" + i + ".ser");
                if (playerTwoFile.exists()) {
                    FileInputStream playerTwoInputStream = new FileInputStream(playerTwoFile);
                    byte[] playerTwoBytes = playerTwoInputStream.readAllBytes();
                    playerTwoInputStream.close();

                    FileOutputStream playerOneOutputStream = new FileOutputStream("PlayerTwo/" + i + ".ser");
                    playerOneOutputStream.write(playerTwoBytes);
                    playerOneOutputStream.close();

                    System.out.println("File sent back to PlayerOne");
                }
            }
        } catch (Exception e) {
            System.exit(1205);
        }
    }
}
