package com.example.tripletriadnew;

//Cactus

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class TripleTriad extends Application {

    Socket socket;
    CardClass[] playerCards = new CardClass[5];
    CardClass[] enemyCards = new CardClass[5];
    Color currentColor = Color.BLUE;
    ImageView selectedTile = null;
    CardClass selectedCard = null;
    Rectangle[][] boardGrid = new Rectangle[3][3];
    CardClass[][] boardStatus = new CardClass[3][3];
    int enemyCardCount = 0;
    GridPane enemy = new GridPane();
    GridPane player = new GridPane();
    ObjectOutputStream objectOutputStream;
    ObjectInputStream objectInputStream;
    Scene menuScene;
    Scene gameScene;
    Scene cardScene;
    Boolean currentTurn = false;

    @Override
    public void start(Stage primaryStage) throws Exception {
        VBox menuLayout = new VBox(20);
        menuLayout.setAlignment(Pos.CENTER);
        Button startButton = new Button("Start Game");
        startButton.setOnAction(e -> {
            primaryStage.setScene(gameScene);
            serverSetup();
        });
        Button cardButton = new Button("Cards");
        cardButton.setOnAction(e -> primaryStage.setScene(cardScene));
        Button exitButton = new Button("Exit Game");
        exitButton.setOnAction(e -> primaryStage.close());
        menuLayout.getChildren().add(startButton);
        menuLayout.getChildren().add(cardButton);
        menuLayout.getChildren().add(exitButton);
        menuScene = new Scene(menuLayout, 300, 200);

        playScreenCreation();
        cardScreenCreation(primaryStage);

        primaryStage.setTitle("Triple Triad");
        primaryStage.setScene(menuScene);
        primaryStage.show();
    }

    private void serverSetup() {
        try {
            socket = new Socket("localhost", 5555);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            objectOutputStream.writeObject("Ready");
            objectOutputStream.flush();

            // Wait for the server's readiness signal
            String serverResponse = (String) objectInputStream.readObject();
            while (!serverResponse.equals("Ready")) {
                serverResponse = (String) objectInputStream.readObject();
                Thread.sleep(5000);
            }
            for (int i = 1; i <= 5; i++) {
                File file = new File("hand/" + i + ".ser");

                if (file.exists()) {
                    // Read file content into a byte array
                    byte[] fileBytes = readFileToBytes(file);

                    // Send the byte array to the server
                    objectOutputStream.writeObject(fileBytes);
                    objectOutputStream.flush();

                    System.out.println("Sent" + i);
                } else {
                    System.out.println("File not found: " + file.getName());
                    System.exit(1205);
                }
            }

            objectOutputStream.writeObject("Flip");
            objectOutputStream.flush();

            getEnemyFiles("enemy/" + 1 + ".ser", 1);

            System.out.println(Arrays.toString(enemyCards));

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void getEnemyFiles(String filePath, int i) throws Exception {
        folderEmpty();
        byte[] fileBytes = new byte[1024];
        InputStream inputStream = socket.getInputStream();
        FileOutputStream fileOutputStream;

        int bytesRead;
        while ((bytesRead = inputStream.read(fileBytes)) != -1) {
            fileOutputStream = new FileOutputStream(filePath);
            fileOutputStream.write(fileBytes, 0, bytesRead);
            fileOutputStream.close();
            deserialized(i-1, filePath, enemyCards);
            i++;
            filePath = "enemy/" + i + ".ser";
            if (i >= 6) {
                break;
            }
        }
    }

    private void folderEmpty() {
        File folder = new File("enemy/");
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    // Delete the file
                    boolean deleted = file.delete();
                    if (deleted) {
                        System.out.println("Deleted file: " + file.getAbsolutePath());
                    } else {
                        System.out.println("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }

    private static byte[] readFileToBytes(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileBytes = new byte[(int) file.length()];
        fileInputStream.read(fileBytes);
        fileInputStream.close();
        return fileBytes;
    }

    private void cardScreenCreation(Stage primaryStage) throws Exception {
        HBox buttonLayout = new HBox(10);
        HBox cardLayoutH = new HBox(90);
        VBox cardLayout = new VBox(20);
        buttonLayout.setAlignment(Pos.CENTER);
        cardLayout.setAlignment(Pos.CENTER);
        cardLayoutH.setAlignment(Pos.CENTER);

        ListView<String> availableNamesListView = new ListView<>();
        ListView<String> selectedNamesListView = new ListView<>();

        ObservableList<String> fileNamesList = FXCollections.observableArrayList();
        File folder = new File("Serialize");
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        fileNamesList.add(file.getName());
                    }
                }
            }
        }
        availableNamesListView.setItems(fileNamesList);

        TextArea currentCards = new TextArea("Current Cards:");
        currentCards.setEditable(false);
        Button addButton = new Button("Add");
        addButton.setOnAction(event -> {
            String selectedName = availableNamesListView.getSelectionModel().getSelectedItem();
            if (selectedName != null && !selectedNamesListView.getItems().contains(selectedName) && selectedNamesListView.getItems().size() < 5) {
                selectedNamesListView.getItems().add(selectedName);
            }
        });
        Button clearButton = new Button("Clear Hand");
        clearButton.setOnAction(e -> {
            selectedNamesListView.getItems().remove(0,selectedNamesListView.getItems().size());
        });
        Button saveButton = new Button("Save Hand");
        saveButton.setOnAction(e -> {
            if (selectedNamesListView.getItems().size() == 5) {
                try {
                    updateHand(selectedNamesListView);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                selectedNamesListView.getItems().remove(0,5);
            }
        });
        Button backButton = new Button("Return Menu");
        backButton.setOnAction(e -> primaryStage.setScene(menuScene));
        availableNamesListView.setMaxSize(350,150);
        selectedNamesListView.setMaxSize(350,150);
        cardLayout.getChildren().add(availableNamesListView);
        cardLayout.getChildren().add(selectedNamesListView);
        cardLayoutH.getChildren().add(cardLayout);
        buttonLayout.getChildren().add(addButton);
        buttonLayout.getChildren().add(clearButton);
        buttonLayout.getChildren().add(saveButton);
        buttonLayout.getChildren().add(backButton);
        cardLayout.getChildren().add(buttonLayout);
        cardScene = new Scene(cardLayoutH,500,300);
    }

    private void playScreenCreation() throws Exception {
        GridPane board = new GridPane();
        GridPane placeBoard = new GridPane();

        for (int i = 0; i < 5; i++) {
            deserialized(i, "hand/" + (i+1) + ".ser", playerCards);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Rectangle rectangle = new Rectangle(150, 150, Color.WHITE);
                rectangle.setStroke(Color.BLACK);
                rectangle.setStrokeWidth(2);
                boardGrid[col][row] = rectangle;
                board.add(rectangle, col, row);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                ImageView imageView = new ImageView("file:WhiteSpace.png");
                int finalCol = col;
                int finalRow = row;
                imageView.setOnMouseClicked(e -> boardClick(imageView, finalCol, finalRow));
                imageView.setFitWidth(150);
                imageView.setFitHeight(150);
                board.add(imageView, col, row);
            }
        }

        for (int i = 0; i < 5; i++) {
            Image card = new Image("file:card.png");
            ImageView tile = new ImageView(card);
            tile.setFitHeight(75);
            tile.setFitWidth(75);
            enemy.add(tile, i, 0);
        }

        for (int i = 0; i < 5; i++) {
            Image card = new Image(playerCards[i].getImage());
            ImageView tile = new ImageView(card);
            tile.setFitHeight(75);
            tile.setFitWidth(75);
            int finalI = i;
            tile.setOnMouseClicked(e -> handClick(tile, finalI));
            tile.setOnMouseEntered(e -> tile.setCursor(Cursor.OPEN_HAND));
            tile.setOnMouseExited(e -> tile.setCursor(Cursor.DEFAULT));
            player.add(tile, i, 0);
        }

        StackPane stackPane = new StackPane(placeBoard, board);

        board.setAlignment(Pos.CENTER);
        enemy.setAlignment(Pos.CENTER);
        player.setAlignment(Pos.CENTER);
        VBox boardV = new VBox(25, enemy, stackPane, player);
        boardV.setAlignment(Pos.CENTER);
        gameScene = new Scene(boardV, 750, 750);
    }

    private void updateHand(ListView<String> list) throws Exception {
        File from = new File("Serialize");

        for (int i = 0; i < 5; i++) {
            if (from.isDirectory()) {
                File[] files = from.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().equals(list.getItems().get(i))) {
                            copyFiles(list.getItems().get(i), i);
                        }
                    }
                }
            }
        }
        player.getChildren().clear();
        playScreenCreation();
    }

    private void copyFiles(String file, int num) throws Exception {
        num += 1;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream("Serialize/" + file);
            fos = new FileOutputStream("hand/" + num + ".ser");
            int c;

            while ((c = fis.read()) != -1) {
                fos.write(c);
            }
            System.out.println("copied the file successfully");
        }
        finally {
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }

    private void handClick(ImageView tile, int i) {

        if (selectedTile != null) {
            selectedTile.setFitWidth(75);
            selectedTile.setFitHeight(75);
        }

        if (tile == selectedTile){
            tile.setFitWidth(75);
            tile.setFitHeight(75);
            selectedTile = null;
            selectedCard = null;
        } else {
            tile.setFitHeight(100);
            tile.setFitWidth(100);
            selectedTile = tile;
            selectedCard = playerCards[i];
        }
    }

    private void boardClick(ImageView imageView, int x, int y) {
        if (selectedTile != null && boardStatus[x][y] == null && currentTurn) {
            imageView.setImage(selectedTile.getImage());
            player.getChildren().remove(selectedTile);
            boardGrid[x][y].setFill(currentColor);
            boardStatus[x][y] = selectedCard;
            colorSwap();
            selectedTile = null;
            selectedCard = null;
            boardUpdate(x,y);

            try {
                // Read file content into a byte array
                CardClass[][] boardServerUpdate = boardStatus;

                // Send the byte array to the server
                objectOutputStream.writeObject(boardServerUpdate);
                objectOutputStream.flush();
            } catch (Exception e) {
                System.out.println(e);
                System.exit(1205);
            }
        }
    }

    private void colorSwap() {
        if (currentColor == Color.BLUE) {
            currentColor = Color.RED;
        }
        else {
            currentColor = Color.BLUE;
        }
    }

    private void deserialized(int i, String filePath, CardClass[] array) throws Exception {
        FileInputStream fis = new FileInputStream(filePath);
        ObjectInputStream ois = new ObjectInputStream(fis);
        CardClass deserialized = (CardClass) ois.readObject();
        ois.close();
        fis.close();

        array[i] = deserialized;
    }

    private void boardUpdate(int x, int y) {
        CardClass[] checkingLocations = new CardClass[4];

        try {
            checkingLocations[0] = boardStatus[x][y-1];
        } catch (Exception e) {
            checkingLocations[0] = null;
        }

        try {
            checkingLocations[1] = boardStatus[x][y+1];
        } catch (Exception e) {
            checkingLocations[1] = null;
        }

        try {
            checkingLocations[2] = boardStatus[x-1][y];
        } catch (Exception e) {
            checkingLocations[2] = null;
        }

        try {
            checkingLocations[3] = boardStatus[x+1][y];
        } catch (Exception e) {
            checkingLocations[3] = null;
        }

        if (checkingLocations[0] != null && checkingLocations[0].getDown() > boardStatus[x][y].getUp()) {
            boardGrid[x][y].setFill(boardGrid[x][y-1].getFill());
        }
        if (checkingLocations[1] != null && checkingLocations[1].getUp() > boardStatus[x][y].getDown()) {
            boardGrid[x][y].setFill(boardGrid[x][y+1].getFill());
        }
        if (checkingLocations[2] != null && checkingLocations[2].getRight() > boardStatus[x][y].getLeft()) {
            boardGrid[x][y].setFill(boardGrid[x-1][y].getFill());
        }
        if (checkingLocations[3] != null && checkingLocations[3].getLeft() > boardStatus[x][y].getRight()) {
            boardGrid[x][y].setFill(boardGrid[x+1][y].getFill());
        }

        if (checkingLocations[0] != null && checkingLocations[0].getDown() < boardStatus[x][y].getUp()) {
            boardGrid[x][y-1].setFill(boardGrid[x][y].getFill());
        }
        if (checkingLocations[1] != null && checkingLocations[1].getUp() < boardStatus[x][y].getDown()) {
            boardGrid[x][y+1].setFill(boardGrid[x][y].getFill());
        }
        if (checkingLocations[2] != null && checkingLocations[2].getRight() < boardStatus[x][y].getLeft()) {
            boardGrid[x-1][y].setFill(boardGrid[x][y].getFill());
        }
        if (checkingLocations[3] != null && checkingLocations[3].getLeft() < boardStatus[x][y].getRight()) {
            boardGrid[x+1][y].setFill(boardGrid[x][y].getFill());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
