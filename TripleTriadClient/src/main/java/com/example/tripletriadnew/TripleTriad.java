package com.example.tripletriadnew;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
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

public class TripleTriad extends Application {

    Image icon = new Image("file:icon.png");
    Stage primaryStage;
    Socket socket;
    CardClass[] playerCards = new CardClass[5];
    CardClass[] enemyCards = new CardClass[5];
    Color playerColor = Color.BLUE;
    Color enemyColor = Color.RED;
    ImageView selectedTile = null;
    CardClass selectedCard = null;
    GridPane board = new GridPane();
    Rectangle[][] boardGrid = new Rectangle[3][3];
    CardClass[][] boardStatus = new CardClass[3][3];
    GridPane arrowLeft = new GridPane();
    GridPane arrowRight = new GridPane();
    GridPane enemy = new GridPane();
    GridPane player = new GridPane();
    ObjectOutputStream objectOutputStream;
    ObjectInputStream objectInputStream;
    Thread gameThread;
    Scene menuScene;
    Scene gameScene;
    Scene cardScene;
    Scene creditScene;
    Scene endScene;
    Scene loadingScene;
    Boolean playState = true;
    Boolean currentTurn = false;
    Boolean victory = false;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;

        //Scene Setups
        menuScreenCreation(primaryStage);
        playScreenCreation();
        cardScreenCreation(primaryStage);
        loadingSceneCreation();
        creditSceneCreation();

        //primaryStage Setup
        primaryStage.getIcons().add(icon);
        primaryStage.setTitle("Triple Triad");
        primaryStage.setScene(menuScene);
        primaryStage.show();
    }

    private void creditSceneCreation() {
        TextArea credits = new TextArea();

        try {
            BufferedReader reader = new BufferedReader(new FileReader("credits.txt"));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            credits.setText(content.toString());
        } catch (Exception e) {
            System.exit(1205);
        }

        credits.setEditable(false);
        credits.setWrapText(true);

        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> primaryStage.setScene(menuScene));

        VBox vbox = new VBox(credits, exitButton);
        vbox.setAlignment(Pos.CENTER);

        creditScene = new Scene(vbox, 800, 300);
    }

    private void menuScreenCreation(Stage primaryStage) {
        VBox menuLayout = new VBox(20);
        menuLayout.setAlignment(Pos.CENTER);
        Button startButton = new Button("Start Game");
        startButton.setOnAction(e -> {
            primaryStage.setScene(loadingScene);
            Platform.runLater(() -> { //Needed to have a thread run anything to do with JavaFX
                try {
                    Thread.sleep(100); //Allows menuScene to switch over to loadingScene
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                serverSetup();
            });
        });
        Button cardButton = new Button("Cards");
        cardButton.setOnAction(e -> primaryStage.setScene(cardScene));
        Button creditButton = new Button("Credits");
        creditButton.setOnAction(e -> primaryStage.setScene(creditScene));
        Button exitButton = new Button("Exit Game");
        exitButton.setOnAction(e -> primaryStage.close());
        menuLayout.getChildren().add(startButton);
        menuLayout.getChildren().add(cardButton);
        menuLayout.getChildren().add(creditButton);
        menuLayout.getChildren().add(exitButton);
        menuScene = new Scene(menuLayout, 300, 200);
    }

    private void loadingSceneCreation() {
        VBox layout = new VBox(35);
        layout.setAlignment(Pos.CENTER);
        Label message = new Label("Setting up game, Please wait");
        Image bufferWheel = new Image("file:spin.gif");

        ImageView bufferView = new ImageView(bufferWheel);

        layout.getChildren().add(message);
        layout.getChildren().add(bufferView);
        loadingScene = new Scene(layout, 300, 200);
    }

    private void endScreenCreation(Stage primaryStage) {
        Label winLose;
        Button menuButton = new Button("Close");
        menuButton.setOnAction(e -> primaryStage.setScene(menuScene));
        VBox layout = new VBox(25);
        layout.setAlignment(Pos.CENTER);

        if (victory) {
            winLose = new Label("You Win!");
        }
        else {
            winLose = new Label("You Lose!");
        }

        layout.getChildren().add(winLose);
        layout.getChildren().add(menuButton);
        endScene = new Scene(layout, 300, 200);
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
        GridPane placeBoard = new GridPane();

        ImageView currentArrowL = new ImageView(new Image("file:arrow.png"));
        currentArrowL.setFitWidth(32);
        currentArrowL.setFitHeight(32);

        ImageView currentArrowR = new ImageView(new Image("file:arrow.png"));
        currentArrowR.setFitWidth(32);
        currentArrowR.setFitHeight(32);

        arrowLeft.add(currentArrowL, 0, 0);
        arrowRight.add(currentArrowR, 0, 0);
        arrowLeft.setAlignment(Pos.CENTER);
        arrowRight.setAlignment(Pos.CENTER);

        for (int i = 0; i < 5; i++) {
            deserialized(i, "hand/" + (i+1) + ".ser", playerCards);
        }

        for (int row = 0; row < 3; row++) { //Board Creation
            for (int col = 0; col < 3; col++) {
                Rectangle rectangle = new Rectangle(150, 150, Color.WHITE);
                rectangle.setStroke(Color.BLACK);
                rectangle.setStrokeWidth(2);
                boardGrid[col][row] = rectangle;
                board.add(rectangle, col, row);
            }
        }

        for (int row = 0; row < 3; row++) { //Clickable Board Creation
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

        for (int i = 0; i < 5; i++) { //Enemy Hand Visual Creation
            Image card = new Image("file:card.png");
            ImageView tile = new ImageView(card);
            tile.setFitHeight(75);
            tile.setFitWidth(75);
            enemy.add(tile, i, 0);
        }

        for (int i = 0; i < 5; i++) { //Player Hand Creation
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
        HBox boardH = new HBox(50, arrowLeft, stackPane, arrowRight);
        boardH.setAlignment(Pos.CENTER);
        VBox boardV = new VBox(25, enemy, boardH, player);
        boardV.setAlignment(Pos.CENTER);
        gameScene = new Scene(boardV, 750, 750);
    }

    private void serverSetup() {
        try {

            socket = new Socket("localhost", 5555);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            objectOutputStream.writeObject("Ready");
            objectOutputStream.flush();

            String serverResponse = (String) objectInputStream.readObject();
            while (!serverResponse.equals("Ready")) { //Waits for a "Ready" response from the server, to tell the client it's ready for requests.
                serverResponse = (String) objectInputStream.readObject();
                Thread.sleep(5000); //Needed since it syncs up the input of the Client with the Server's output
            }
            for (int i = 1; i <= 5; i++) { //Sends the current hand to the server to be processed
                File file = new File("hand/" + i + ".ser");

                if (file.exists()) {
                    byte[] fileBytes = readFileToBytes(file);

                    objectOutputStream.writeObject(fileBytes);
                    objectOutputStream.flush();
                } else {
                    System.out.println("File not found: " + file.getName());
                    System.exit(1205);
                }
            }

            objectOutputStream.writeObject("Flip"); //Sent to the Server to tell it the client is finished sending files
            objectOutputStream.flush();

            getEnemyFiles("enemy/" + 1 + ".ser", 1); //Gets the enemy cards

            Object received = objectInputStream.readObject(); //Receives up if the user is PlayerOne or not

            currentTurn = (Boolean) received; //Sets the value for currentTurn

            if (currentTurn) {
                arrowFlip(); //Flips the current arrow position
            }

            primaryStage.setScene(gameScene);

            gameThread = new Thread(() -> { //Creates a new thread to continuously check for any incoming info
                try {
                    while (playState) {
                        Object receive = objectInputStream.readObject();

                        if (receive instanceof CardClass[][]) { //Checks for new Board State
                            boardStatus = (CardClass[][]) receive;

                            arrowFlip();

                            Platform.runLater(this::updateGameBoard);
                        } else if (receive instanceof Boolean) { //Checks for endGame
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.exit(1205);
                }
            });
            gameThread.start();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean boardChecking() { //Checks the board for endGame condition
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (boardStatus[i][j] == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private void swapCurrentPlayer() {
        currentTurn = !currentTurn;
    }

    private void updateHand(ListView<String> list) throws Exception { //Updates the current Player Hand
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

    private void updateGameBoard() { //Updates the visual GameBoard when any input is received from the Server
        ObservableList<Node> boardChildren = board.getChildren();
        for (Node node : boardChildren) {
            if (node instanceof ImageView imageView) {
                int col = GridPane.getRowIndex(node);
                int row = GridPane.getColumnIndex(node);

                if (row >= 0 && row < 3 && col >= 0 && col < 3) {
                    CardClass card = boardStatus[row][col];

                    if (card != null) {
                        Image cardImage = new Image(card.getImage());
                        imageView.setImage(cardImage);
                    } else {
                        imageView.setImage(new ImageView("file:WhiteSpace.png").getImage());
                    }
                }
            }
        }
        for (int i = 0; i < 3; i++) { //Setups the correct colors of the board
            for (int j = 0; j < 3; j++) {
                if (boardGrid[i][j].getFill() == Color.WHITE && boardStatus[i][j] != null) {
                    boardGrid[i][j].setFill(enemyColor);
                    boardUpdate(i, j);
                }
            }
        }
        swapCurrentPlayer();
        gameOverCheck();
    }

    private void boardUpdate(int x, int y) { //Updates the visual GameBoard when any input is received from the User in the Client

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

    private void handClick(ImageView tile, int i) { //Changes size of the clicked on card in the Player Hand

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

    private void boardClick(ImageView imageView, int x, int y) { //Checks to see if a card can be placed on a selected Board Tile
        if (selectedTile != null && boardStatus[x][y] == null && currentTurn) {
            imageView.setImage(selectedTile.getImage());
            player.getChildren().remove(selectedTile);
            boardGrid[x][y].setFill(playerColor);
            boardStatus[x][y] = selectedCard;
            selectedTile = null;
            selectedCard = null;
            boardUpdate(x,y);

            try {
                CardClass[][] boardServerUpdate = boardStatus;

                objectOutputStream.writeObject(boardServerUpdate);
                objectOutputStream.flush();
            } catch (Exception e) {
                System.out.println(e);
                System.exit(1205);
            }
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

    private void arrowFlip() { //Flips the current direction of the arrows
        if (arrowLeft.getRotate() == 0) {
            arrowLeft.setRotate(180);
            arrowRight.setRotate(180);
        }
        else {
            arrowLeft.setRotate(0);
            arrowRight.setRotate(0);
        }
    }

    private void gameOverCheck() { //Checks for Winning conditions
        try {
            int blue = 0;
            int red = 0;
            if (boardChecking()) {
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        if (boardGrid[i][j].getFill() == playerColor) {
                            blue++;
                        }
                        else {
                            red++;
                        }
                    }
                }
                victory = blue > red;
                variableResets();
                playScreenCreation();
                socket.close();

                endScreenCreation(primaryStage);
                primaryStage.setScene(endScene);
            }
        } catch (Exception e) {
            System.out.println("Error");
        }
    }

    private void variableResets() { //Resets all important variables for when the Clients reset for another game
        board = new GridPane();
        boardGrid = new Rectangle[3][3];
        boardStatus = new CardClass[3][3];
        playState = true;
        arrowLeft = new GridPane();
        arrowRight = new GridPane();
    }

    private void copyFiles(String file, int num) throws Exception { //Copying of Files from Serialized Folder to Hand Folder
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

    private void getEnemyFiles(String filePath, int i) throws Exception { //Receives enemy files and loads them into the enemy folder
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

    private void folderEmpty() { //Empties the enemy folder before loading in new enemy Files
        File folder = new File("enemy/");
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    boolean deleted = file.delete();
                }
            }
        }
    }

    private static byte[] readFileToBytes(File file) throws IOException { //Reads Bytes into Files
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileBytes = new byte[(int) file.length()];
        fileInputStream.read(fileBytes);
        fileInputStream.close();
        return fileBytes;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
