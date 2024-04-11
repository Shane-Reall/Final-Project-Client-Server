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

        while (clientCount < 2) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());

            if (clientCount == 0) {
                playerOneInput = input;
                playerOneOutput = output;
            } else {
                playerTwoInput = input;
                playerTwoOutput = output;
            }

            // Start a new thread to handle the client connection
            ClientHandler handler = new ClientHandler(clientSocket, playerOneInput, playerOneOutput, playerTwoInput, playerTwoOutput, input, output);
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
    private ObjectInputStream playerOneInput;
    private ObjectInputStream playerTwoInput;
    private ObjectOutputStream playerOneOutput;
    private ObjectOutputStream playerTwoOutput;
    private final Object filesSentLock = new Object();
    private int clientId;
    private int fileCounter;
    private static boolean filesSent = false;
    private CardClass[][] boardStatus = new CardClass[3][3];

    public ClientHandler(Socket socket, ObjectInputStream playerOneInput, ObjectOutputStream playerOneOutput, ObjectInputStream playerTwoInput, ObjectOutputStream playerTwoOutput, ObjectInputStream input, ObjectOutputStream output) {
        this.clientSocket = socket;
        this.clientId = ++clientCount; // Assign a unique ID to each client
        this.fileCounter = 0;

        this.playerOneInput = playerOneInput;
        this.playerOneOutput = playerOneOutput;
        this.playerTwoInput = playerTwoInput;
        this.playerTwoOutput = playerTwoOutput;

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
                    if (clientCount == 1) {
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
                } else if (received instanceof String && received.equals("Flip")) {
                    synchronized (ClientHandler.class) {
                        if (!filesSent) {
                            filesSent = true;
                            sendFilesBack();
                        }
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

    private void sendFilesBack() {
        System.out.println("Cactus");
        try {
            // Sending files from PlayerOne to PlayerTwo
            for (int i = 0; i <= 5; i++) {
                File playerOneFile = new File("PlayerOne/" + i + ".ser");
                if (playerOneFile.exists()) {
                    FileInputStream playerOneInputStream = new FileInputStream(playerOneFile);
                    byte[] playerOneBytes = playerOneInputStream.readAllBytes();
                    playerOneInputStream.close();

                    FileOutputStream playerTwoOutputStream = new FileOutputStream("PlayerTwo/" + i + ".ser");
                    playerTwoOutputStream.write(playerOneBytes);
                    playerTwoOutputStream.flush();
                    playerTwoOutputStream.close();

                    System.out.println("File " + i + ".ser sent from PlayerOne to PlayerTwo");
                }
            }

            // Sending files from PlayerTwo to PlayerOne
            for (int i = 0; i <= 5; i++) {
                File playerTwoFile = new File("PlayerTwo/" + i + ".ser");
                if (playerTwoFile.exists()) {
                    FileInputStream playerTwoInputStream = new FileInputStream(playerTwoFile);
                    byte[] playerTwoBytes = playerTwoInputStream.readAllBytes();
                    playerTwoInputStream.close();

                    FileOutputStream playerOneOutputStream = new FileOutputStream("PlayerOne/" + i + ".ser");
                    playerOneOutputStream.write(playerTwoBytes);
                    playerOneOutputStream.flush();
                    playerOneOutputStream.close();

                    System.out.println("File " + i + ".ser sent from PlayerTwo to PlayerOne");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
