package com.example.tripletriadnew;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.*;
import java.util.Scanner;

public class CardObjectMaker {

    public static void main(String[] args) throws Exception {
        String name;
        int up;
        int right;
        int down;
        int left;
        String image;

        Scanner scan = new Scanner(System.in);

        System.out.print("Name: ");
        name = scan.nextLine();
        System.out.print("Up: ");
        up = scan.nextInt();
        System.out.print("Down: ");
        down = scan.nextInt();
        System.out.print("Left: ");
        left = scan.nextInt();
        System.out.print("Right: ");
        right = scan.nextInt();
        image = "file:cards/" + name + ".png";

        CardClass card = new CardClass(name, up, right, down, left, image);

        String fileLocation = "Serialize/"  + name + ".ser";

        FileOutputStream fos = new FileOutputStream(fileLocation);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(card);
        oos.close();
        fos.close();

        fileLocation = "Serialize/" + name + ".ser";

        FileInputStream fis = new FileInputStream(fileLocation);
        ObjectInputStream ois = new ObjectInputStream(fis);
        CardClass deserialized = (CardClass) ois.readObject();
        ois.close();
        fis.close();

        System.out.println(
                deserialized.getName() + deserialized.getUp() + deserialized.getDown() + deserialized.getLeft() + deserialized.getRight() + deserialized.getImage()
        );
    }
}
