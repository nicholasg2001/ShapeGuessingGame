module com.mycompany.csc311hw2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.mycompany.csc311hw2 to javafx.fxml;
    exports com.mycompany.csc311hw2;
}
