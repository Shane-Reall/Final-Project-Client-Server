package com.example.tripletriadnew;

import java.io.*;
import java.net.*;
import java.util.Random;

public class TripleTriadServer {

    private static final int PORT = 5555;
    public static int clientCount = 0;

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server Started. Waiting for clients...");

        //Variables
        boolean clientCheck = false;
        Socket clientSocketOne = null;
        ObjectInputStream inputOne = null;
        ObjectOutputStream outputOne = null;
        ObjectInputStream playerOneInput = null;
        ObjectOutputStream playerOneOutput = null;
        Socket clientSocketTwo = null;
        ObjectInputStream inputTwo = null;
        ObjectOutputStream outputTwo = null;
        ObjectInputStream playerTwoInput = null;
        ObjectOutputStream playerTwoOutput = null;
        Socket playerOneSocket = null;
        Socket playerTwoSocket = null;

        while (true) { //Loops through Threads to creates two connects and then use these connection. Once these connections disconnect it searches for new Clients
            if (clientCount == 0) {
                clientSocketOne = serverSocket.accept();
                System.out.println("Client connected: " + clientSocketOne.getInetAddress());

                inputOne = new ObjectInputStream(clientSocketOne.getInputStream());
                outputOne = new ObjectOutputStream(clientSocketOne.getOutputStream());

                playerOneInput = inputOne;
                playerOneOutput = outputOne;
                playerOneSocket = clientSocketOne;

                clientCount++;

            } else if (clientCount == 1) {
                clientSocketTwo = serverSocket.accept();
                System.out.println("Client connected: " + clientSocketTwo.getInetAddress());

                inputTwo = new ObjectInputStream(clientSocketTwo.getInputStream());
                outputTwo = new ObjectOutputStream(clientSocketTwo.getOutputStream());

                playerTwoInput = inputTwo;
                playerTwoOutput = outputTwo;
                playerTwoSocket = clientSocketTwo;

                clientCount++;
                clientCheck = true;
            } else if (clientCheck) {
                ClientHandler handlerOne = new ClientHandler(clientSocketOne, playerOneInput, playerOneOutput, playerTwoInput, playerTwoOutput, inputOne, outputOne, playerOneSocket, playerTwoSocket);
                handlerOne.start();

                ClientHandler handlerTwo = new ClientHandler(clientSocketTwo, playerOneInput, playerOneOutput, playerTwoInput, playerTwoOutput, inputTwo, outputTwo, playerOneSocket, playerTwoSocket);
                handlerTwo.start();

                System.out.println("Max Number of Clients Reached");
                clientCheck = false;
            } else if (clientCount >= 2) { //Resets All important needed Client Variables
                clientSocketOne = null;
                inputOne = null;
                outputOne = null;
                playerOneInput = null;
                playerOneOutput = null;
                clientSocketTwo = null;
                inputTwo = null;
                outputTwo = null;
                playerTwoInput = null;
                playerTwoOutput = null;
                playerOneSocket = null;
                playerTwoSocket = null;
            }
            Thread.sleep(100); //Easies up on continues quick looping of loop
        }
    }
}

class ClientHandler extends Thread {
    private static int clientCount = 0;
    private Socket clientSocket;
    private final Object lock = new Object();
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
        this.clientId = ++clientCount; //Assign a unique ID to each client
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
            while (true) { //Continues check for inputs
                Object received = input.readObject();

                if (received instanceof String && received.equals("Ready")) { //Responds to the Clients all Connects are ready
                    if (clientId == 1) {

                        playerOneOutput.writeObject("Ready");
                        playerOneOutput.flush();

                        playerTwoOutput.writeObject("Ready");
                        playerTwoOutput.flush();
                    }
                }
                else if (received instanceof byte[]) { //Checks for incoming .ser files
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
                } else if (received instanceof CardClass[][]) { //Checks for incoming boardArrays
                    Object[][] receivedArray = (Object[][]) received;
                    boardStatus = (CardClass[][]) receivedArray;

                    playerOneOutput.writeObject(boardStatus);
                    playerOneOutput.flush();

                    playerTwoOutput.writeObject(boardStatus);
                    playerTwoOutput.flush();

                    if (boardChecking()) {
                        playerOneOutput.writeObject(false);
                        playerOneOutput.flush();

                        playerTwoOutput.writeObject(false);
                        playerTwoOutput.flush();
                    }
                } else if (received instanceof String && received.equals("Flip") && clientId == 2) { //Checks to see if boards needs to be exchanged
                    synchronized (ClientHandler.class) {
                        sendFilesBack(playerOneSocket, playerTwoSocket);
                        currentPlayer();
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) { //Closes Clients when They disconnect
            TripleTriadServer.clientCount--;
            try {
                variableReset();
                clientSocket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } finally {
            if (clientSocket != null) {
                System.out.println("Closing connection for Client " + clientId);
            }
        }
    }

    private void variableReset() { //Resets Variables Important for Client Reset
        clientCount = 0;
        clientId = 0;
        fileCounter = 0;
        boardStatus = new CardClass[3][3];
    }

    private void currentPlayer() {
        synchronized (lock) { //Syncs the Threads to decide on who PlayerOne is
            Random rand = new Random();
            int coinFlip = rand.nextInt(0,2);

            try {
                if (coinFlip == 0) {
                    playerOneOutput.writeObject(true);
                    playerOneOutput.flush();
                    playerTwoOutput.writeObject(false);
                    playerTwoOutput.flush();
                }
                else {
                    playerTwoOutput.writeObject(true);
                    playerTwoOutput.flush();
                    playerOneOutput.writeObject(false);
                    playerOneOutput.flush();
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private void sendFilesBack(Socket playerOneSocket, Socket playerTwoSocket) {
        try {

            // Sending files from PlayerOne to PlayerTwo
            for (int i = 1; i <= 5; i++) {
                String fileName = "PlayerOne/" + i + ".ser";
                FileInputStream fileInputStream = new FileInputStream(fileName);
                byte[] fileData = fileInputStream.readAllBytes();

                OutputStream out = playerTwoSocket.getOutputStream();
                out.write(fileData);
                out.flush();

                Thread.sleep(500);

                fileInputStream.close();
            }

            // Sending files from PlayerTwo to PlayerOne
            for (int i = 1; i <= 5; i++) {
                String fileName = "PlayerTwo/" + i + ".ser";
                FileInputStream fileInputStream = new FileInputStream(fileName);
                byte[] fileData = fileInputStream.readAllBytes();

                OutputStream out = playerOneSocket.getOutputStream();
                out.write(fileData);
                out.flush();

                Thread.sleep(500);

                fileInputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean boardChecking() { //Checks for end Game Condition
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (boardStatus[i][j] == null) {
                    return false;
                }
            }
        }
        return true;
    }
}