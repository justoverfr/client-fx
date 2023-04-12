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

/**
 * Classe App qui sert de point d'entrée pour l'application du client
 */
public class App extends Application {
    private Socket socket;
    private PrintWriter printWriter;

    private StringProperty messages = new SimpleStringProperty("");

    /**
     * Fonction principale pour lancer l'application
     *
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    /**
     * Fonction qui sert à lancer l'application
     *
     * @param primaryStage Stage principal de l'application
     *
     * {@inheritDoc}
     */
    public void start(Stage primaryStage) {
        connectionPage(primaryStage);
    }

    /**
     * Fonction qui sert à afficher la page de connexion
     *
     * @param stage Stage principal de l'application
     */
    private void connectionPage(Stage stage) {
        // Crée le conteneur principal de la page de connexion
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        // Crée les composants de la page de connexion
        Label ipLabel = new Label("IP Address:");
        TextField ipTextField = new TextField();
        Label portLabel = new Label("Port:");
        TextField portTextField = new TextField();
        Label usernameLabel = new Label("Username:");
        TextField usernameTextField = new TextField();
        Button connectButton = new Button("Connect");

        // Désactive le bouton de connexion si un des champs est vide
        connectButton.disableProperty().bind(
                Bindings.isEmpty(ipTextField.textProperty())
                        .or(Bindings.isEmpty(portTextField.textProperty()))
                        .or(Bindings.isEmpty(usernameTextField.textProperty()))
        );

        // Lorsque le bouton de connexion est cliqué, on se connecte au serveur
        connectButton.setOnAction(event -> {
            // On récupère les valeurs des champs
            String ipAddress = ipTextField.getText().trim();
            int port = Integer.parseInt(portTextField.getText().trim());
            String username = usernameTextField.getText().trim();

            // On vérifie que les champs ne sont pas vides
            if (!ipAddress.isEmpty() && !username.isEmpty()) {
                // On se connecte au serveur et on affiche la page de chat
                connectToServer(ipAddress, port, username);
                chatPage(stage);
            }
        });

        // On ajoute les composants au conteneur principal
        root.getChildren().addAll(ipLabel, ipTextField, portLabel, portTextField, usernameLabel, usernameTextField, connectButton);

        // On affiche la page de connexion
        Scene scene = new Scene(root, 600, 400);
        stage.setTitle("Chat Client - Connection");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Fonction qui sert à afficher la page de chat
     *
     * @param stage Stage principal de l'application
     */
    private void chatPage(Stage stage) {
        // Crée le conteneur principal de la page de chat
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);

        // La zone de texte qui affiche les messages
        TextArea chatArea = new TextArea();
        chatArea.textProperty().bind(messages);
        chatArea.setEditable(false);

        // La zone de texte pour écrire un message et le bouton d'envoi
        TextField messageTextField = new TextField();
        Button sendButton = new Button("Send");
        sendButton.disableProperty().bind(Bindings.isEmpty(messageTextField.textProperty()));

        // Lorsque le bouton d'envoi est cliqué, on envoie le message au serveur
        sendButton.setOnAction(event -> {
            // On récupère le message
            String message = messageTextField.getText().trim();

            // On vérifie que le message n'est pas vide et que la connexion est ouverte
            if (!message.isEmpty() && socket != null && !socket.isClosed()) {
                // On envoie le message au serveur
                printWriter.println(message);
                printWriter.flush();
                messageTextField.clear();
            }
        });

        // Le bouton de déconnexion
        Button disconnectButton = new Button("Disconnect");
        disconnectButton.setOnAction(event -> {
            // On ferme la connexion et on affiche la page de connexion
            closeResources();
            connectionPage(stage);
        });

        // Un Hbox est un conteneur horizontal qui permet d'aligner les composants horizontalement
        HBox messageBox = new HBox(10, messageTextField, sendButton);
        messageBox.setAlignment(Pos.CENTER);

        // On ajoute les composants au conteneur principal
        root.getChildren().addAll(chatArea, messageBox, disconnectButton);

        // Lorsque la fenêtre est fermée, on ferme la connexion
        stage.setOnCloseRequest(event -> {
            closeResources();
            Platform.exit();
            System.exit(0);
        });

        // On affiche la page de chat
        Scene scene = new Scene(root, 600, 400);
        stage.setTitle("Chat Client - Chat");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Fonction qui sert à se connecter au serveur
     *
     * @param ipAddress Adresse IP du serveur
     * @param port      Port du serveur
     * @param username  Nom d'utilisateur
     */
    private void connectToServer(String ipAddress, int port, String username) {
        try {
            socket = new Socket(ipAddress, port);
            System.out.println("Connecté au serveur");

            // Point d'entrée pour envoyer des messages au serveur
            printWriter = new PrintWriter(socket.getOutputStream());

            // On envoie le nom d'utilisateur au serveur
            printWriter.println(username);

            // On crée un thread qui lit les messages du serveur
            Runnable readMessages = () -> {
                try {
                    // Point d'entrée pour lire les messages du serveur
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // On lit les messages du serveur
                    String message;
                    while ((message = bufferedReader.readLine()) != null) {
                        messages.set(messages.get() + "\n" + message);
                    }
                } catch (IOException e) {
                    // Affiche la pile d'appels de l'exception
                    e.printStackTrace();
                }
            };

            // On démarre le thread
            new Thread(readMessages).start();
        } catch (IOException e) {
            System.out.println("Erreur lors de la connexion au serveur");
            // Affiche la pile d'appels de l'exception
            e.printStackTrace();
        }
    }

    /**
     * Fonction qui sert à fermer les points d'entrée et la connexion.
     * Cette fonction est appelée lorsque la fenêtre est fermée.
     */
    private void closeResources() {
        // Vérifie que les points d'entrée ne sont pas null
        if (printWriter != null) {
            printWriter.close();
        }
        // Vérifie que la connexion n'est pas null
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
