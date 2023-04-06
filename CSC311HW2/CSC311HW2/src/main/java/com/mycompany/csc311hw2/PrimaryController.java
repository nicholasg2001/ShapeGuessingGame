package com.mycompany.csc311hw2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import javafx.animation.FadeTransition;
import javafx.animation.FillTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import static javafx.scene.paint.Color.GREEN;
import static javafx.scene.paint.Color.RED;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.util.Duration;

public class PrimaryController {
    
    @FXML
    private BorderPane borderPane; //root container of the GUI
    
    @FXML
    private ListView guessInfo; 
   
    @FXML
    private Button guessShapeButton; 
    
    @FXML
    private Circle circle;
    @FXML
    private Rectangle rectangle;
    
    @FXML
    ToggleGroup tgChooseShape; //prevents user from being able to select both radio buttons
    
    @FXML
    private TextField totalGuessCount;
    @FXML
    private TextField correctGuessCount;
    
    private int guessCount = 0; //global variable that corresponds to the totalGuessCount text field. Multiple methods manipulate this number.
    
    
    
    /**
     * Initialize function called when project is run.
     * All it does is disable the text fields and set them to 0
     */
    @FXML
    public void initialize(){
        totalGuessCount.setText("0");
        totalGuessCount.setEditable(false);
        correctGuessCount.setText("0");
        correctGuessCount.setEditable(false);
    }
    
    /**
     * Calls the getShape() method to get the corresponding shape to the radio button the user guessed
     * Calls the getColor() method to determine whether the shape should be red or green
     * Calls the animateShape() method to move the shape right, change it's color, and fade it into vision
     * This method animates the shape guessed, inserts the guess information into the DB, increments the
     * totalGuessCount text field by 1, and increments the correctGuessCount text field by 1 if the guess was correct.
     */
    
    @FXML
    public void handleGuessShape(){
        guessShapeButton.setDisable(true);
        Shape shape = getShape(); //get the shape the user guessed
        Color color = getColor(); //change the color of the shape if the user was right or wrong.
        String shapeAsString;
        if(shape == rectangle){ //convert the shape value to a string so it can be inserted into the DB.
            shapeAsString = "RECTANGLE";
        }
        else{
            shapeAsString = "CIRCLE";
        }
        try{
            ParallelTransition playAnimations = animateShape(shape, color);
            playAnimations.setOnFinished(e -> {
                guessShapeButton.setDisable(false); //while animation is running, stop user from being able to guess again.
                insertGuessIntoDB(guessCount, color, shapeAsString); //add guessing info to the DB
                totalGuessCount.setText(String.format("%d", guessCount)); //update the total guess count in the GUI
                if(color == GREEN){
                    correctGuessCount.setText(String.format("%d", Integer.parseInt(correctGuessCount.getText())+1)); //update correct guess count in the GUI
                }
            });
            playAnimations.play();
            guessCount++; 
        }
        catch(Exception e){
            System.out.println("Error occured");
        }
    }
    /**
     * Closes the GUI when menu button "Close" is pressed.
     */
    @FXML
    public void handleCloseApp(){
        System.out.println("Menu closed");
        Stage stage = (Stage) borderPane.getScene().getWindow();
        stage.close();
    }
    /**
     * Resets the game in the GUI when menu button "New Game" is pressed.
     * Sets the guessing fields to 0 and clears the DB of all previous guesses.
     */
    @FXML
    public void handleNewGame(){
        totalGuessCount.setText("0");
        correctGuessCount.setText("0");
        guessCount = 0;
        clearDB();
        guessInfo.getItems().clear();
    }
    /**
     * Calls the openConnection() method to open a connection with the DB
     * Reads guessing information from the DB and inserts it into the listView
     * Clears previous data from the listView (but not the DB) before inserting.
     */
    @FXML
    public void handleShowGuessesFromDB(){
        Connection conn = openConnection(); 
        guessInfo.getItems().clear();
        ObservableList<String> guesses = guessInfo.getItems();
        try{
            String tableName = "playerGuesses";
            Statement guessResult = conn.createStatement();
            ResultSet result = guessResult.executeQuery("select * from " + tableName);
            
            while(result.next()){
                int numGuess = result.getInt("guessNumber");
                String correctGuess = result.getString("correctGuess");
                String playerGuess = result.getString("playerGuess");
                guesses.add(String.format("%d, %s, %s", numGuess, correctGuess, playerGuess));
            }
        }
        catch(SQLException e){
               
        }
    }
    /**
     * Helper method that getColor() calls in order to determine a correct shape the user needs to guess.
     * @return an int corresponding to the shape generated. 0 = circle, 1 = rectangle.
     */
    @FXML
    public int generateRandomShape(){
        Random randomNum = new Random();
        int randomShape = randomNum.nextInt() % 2; //random value is either a 0 or 1. 0 = circle and 1 = rectangle.
        return randomShape;
    }
    
    /**
     * Helper method that handleGuessShape() calls in order to obtain the shape the user guessed corresponding to the radio button.
     * @return The shape the user guessed.
     */
    @FXML
    public Shape getShape(){
        RadioButton buttonSelected = (RadioButton) tgChooseShape.getSelectedToggle();
        String selectedShape = buttonSelected.getText();
        Shape shape = null;
        if(selectedShape.equals("Circle")){
            shape = circle;
        }
        else{
            shape = rectangle;
        }
        return shape;
    }
    /**
     * Helper method that handleGuessShape() calls in order to obtain the color needed for animating the shape guessed.
     * @return The color corresponding to whether or not the shape guessed was correct. GREEN = correct, RED = incorrect.
     */
    @FXML
    public Color getColor(){
        int correctShapeNum = generateRandomShape(); //0 = circle, 1 = rectangle.
        int guess;
        RadioButton buttonSelected = (RadioButton) tgChooseShape.getSelectedToggle();
        if(buttonSelected.getText().equals("Circle")){
            guess = 0;
        }
        else{
            guess = 1;
        }
        if(guess == correctShapeNum){
            return GREEN;
        }
        else{
            return RED;
        }
    }
    /**
     * Helper method that handleGuessShape() calls in order to animate the shape guessed. This method fades the shape into vision,
     * changes its color, and shifts it right. It then reverses all these animations. 
     * @param shape the shape the user guessed
     * @param color the color the shape will be changed to, according to whether or not the guess was correct
     * @return a parallelTransition of the 3 animations needed for showing the shape guessed.
     */
    @FXML
    public ParallelTransition animateShape(Shape shape, Color color){
        FillTransition changeColor = new FillTransition(Duration.seconds(2), shape);
        changeColor.setToValue(color);
        changeColor.setCycleCount(2);
        changeColor.setAutoReverse(true);

        TranslateTransition moveRight = new TranslateTransition(Duration.seconds(2), shape);
        moveRight.setToX(100);
        moveRight.setCycleCount(2);
        moveRight.setAutoReverse(true);

        FadeTransition fadeIntoVision = new FadeTransition(Duration.seconds(2), shape);
        fadeIntoVision.setToValue(1.0);
        fadeIntoVision.setCycleCount(2);
        fadeIntoVision.setAutoReverse(true);

        ParallelTransition playAnimations = new ParallelTransition(changeColor, moveRight, fadeIntoVision);
        return playAnimations;
    }
    /**
     * Formats the guessing information and inserts it into the database.
     * Called by handleGuessShape()
     * @param numGuess the number corresponding to how many times the user has guessed a shape
     * @param color Color of the shape guessed, used to determine whether the guess was right or wrong.
     * @param shapeGuessed shape the user guessed
     */
    public void insertGuessIntoDB(int numGuess, Color color, String shapeGuessed){
        Connection conn = openConnection();
        String correctShape = "";
        if(color == GREEN){ //guess was correct
            correctShape = shapeGuessed;
        }
        else if(color == RED && "CIRCLE".equals(shapeGuessed)){ //user guessed circle and it was rectangle
            correctShape = "RECTANGLE";
        }
        else if(color == RED && "RECTANGLE".equals(shapeGuessed)){ //user guessed rectangle and it was circle
            correctShape = "CIRCLE";
        }
        try{
            String sql = "INSERT INTO playerGuesses (guessNumber, correctGuess, playerGuess) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(sql);
            insertStmt.setInt(1, numGuess);
            insertStmt.setString(2, String.format("correct: %s", correctShape));
            insertStmt.setString(3, String.format("guessed: %s", shapeGuessed));
            insertStmt.executeUpdate();
        }
        catch(SQLException e){
            
        }
    }
    
    /**
     * Clears the database 
     */
    public void clearDB(){
        Connection conn = openConnection();
        try{
            String sql = "DELETE FROM playerGuesses";
            PreparedStatement clear = conn.prepareStatement(sql);
            clear.executeUpdate();
        }
        catch(SQLException e){
                System.out.println("Table not cleared");
        }
    }
    /**
     * Establishes a connection with the MS access database
     * @return the connection
     */
    public static Connection openConnection(){
       Connection conn = null;
       try{
           String databaseURL = "jdbc:ucanaccess://.//shapeGuessing.accdb";
           conn = DriverManager.getConnection(databaseURL);
       }
       catch (SQLException ex){
           System.out.println("Error occured");
       }
       return conn;
   }
    
}
