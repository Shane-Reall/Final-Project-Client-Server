module com.example.tripletriadnew {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.tripletriadnew to javafx.fxml;
    exports com.example.tripletriadnew;
}