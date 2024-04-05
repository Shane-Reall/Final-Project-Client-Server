package com.example.tripletriadnew;

import java.io.Serializable;

public class CardClass implements Serializable {
    private String name;
    private int up;
    private int right;
    private int down;
    private int left;
    private String image;

    public CardClass(String name, int up, int right, int down, int left, String image) {
        this.name = name;
        this.up = up;
        this.right = right;
        this.down = down;
        this.left = left;
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public int getUp() {
        return up;
    }

    public int getDown() {
        return down;
    }

    public int getLeft() {
        return left;
    }

    public int getRight() {
        return right;
    }

    public String getImage() {
        return image;
    }
}
