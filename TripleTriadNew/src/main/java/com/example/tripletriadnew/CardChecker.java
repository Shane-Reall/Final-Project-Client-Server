package com.example.tripletriadnew;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.Scanner;

public class CardChecker {
    public static void main(String[] args) throws Exception {
        Scanner scan = new Scanner(System.in);

        while (true) {
            System.out.print("Enter Monster to check: ");
            String name = scan.nextLine();

            if (name.equals("exit")) {
                System.exit(1205);
            }

            String fileLocation = "Serialize/"  + name + ".ser";

            FileInputStream fis = new FileInputStream(fileLocation);
            ObjectInputStream ois = new ObjectInputStream(fis);
            CardClass deserialized = (CardClass) ois.readObject();
            ois.close();
            fis.close();

            System.out.println("Name: " + deserialized.getName());
            System.out.println("Up: " + deserialized.getUp());
            System.out.println("Down: " + deserialized.getDown());
            System.out.println("Left: " + deserialized.getLeft());
            System.out.println("Right: " + deserialized.getRight());
        }
    }
}
