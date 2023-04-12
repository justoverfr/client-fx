package com.example.clientfx;

import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {
    private static final String ipServer = "localhost";
    private static final int portServer = 1234;

    private Socket socket;
    private PrintWriter printWriter;
    private StringProperty messages = new SimpleStringProperty("");

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        Label usernameLabel = new Label("Veuillez entrer votre pseudo :");
        TextField usernameTextField = new TextField();
        Button connectButton = new Button("Se connecter");
        connectButton.disableProperty().bind(Bindings.isEmpty(usernameTextField.textProperty()));

        TextArea chatArea = new TextArea();
        chatArea.textProperty().bind(messages);
        chatArea.setEditable(false);

        TextField messageTextField = new TextField();
        Button sendButton = new Button("Envoyer");
        sendButton.disableProperty().bind(Bindings.isEmpty(messageTextField.textProperty()));

        connectButton.setOnAction(event -> {
            String username = usernameTextField.getText().trim();
            if (!username.isEmpty()) {
                connectToServer(username);
                usernameTextField.clear();
            }
        });

        sendButton.setOnAction(event -> {
            String message = messageTextField.getText().trim();
            if (!message.isEmpty() && socket != null && !socket.isClosed()) {
                printWriter.println(message);
                printWriter.flush();
                messageTextField.clear();
            }
        });

        HBox messageBox = new HBox(10, messageTextField, sendButton);
        messageBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(usernameLabel, usernameTextField, connectButton, chatArea, messageBox);

        primaryStage.setOnCloseRequest(event -> {
            closeResources();
            Platform.exit();
            System.exit(0);
        });

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Chat Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void connectToServer(String username) {
        try {
            socket = new Socket(ipServer, portServer);
            System.out.println("Connexion établie avec le serveur");

            printWriter = new PrintWriter(socket.getOutputStream());

            printWriter.println(username);

            // Créer un objet Runnable pour lire les messages du serveur
            Runnable readMessages = () -> {
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message;
                    while ((message = bufferedReader.readLine()) != null) {
                        messages.set(messages.get() + "\n" + message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            // Démarrer un thread pour lire les messages du serveur
            new Thread(readMessages).start();
        } catch (IOException e) {
            System.out.println("Impossible de se connecter au serveur");
            e.printStackTrace();
        }
    }

    private void closeResources() {
        if (printWriter != null) {
            printWriter.close();
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
