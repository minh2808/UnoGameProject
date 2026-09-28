package com.ptit.uno.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;

/**
 * Khởi chạy ứng dụng UNO Game với giao diện JavaFX hiện đại (JDK 21).
 */
public class UnoFXApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Tìm file FXML qua Classpath hoặc File System fallback
            URL fxmlUrl = getClass().getResource("/resource/fxml/game_board.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("/fxml/game_board.fxml");
            }
            if (fxmlUrl == null) {
                File f = new File("src/resource/fxml/game_board.fxml");
                if (f.exists()) {
                    fxmlUrl = f.toURI().toURL();
                }
            }

            if (fxmlUrl == null) {
                throw new RuntimeException("Không tìm thấy file FXML game_board.fxml trong classpath hay thư mục dự án!");
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            Scene scene = new Scene(root, 1150, 680);

            primaryStage.setTitle("UNO Multiplayer Online - PTIT LTM");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(620);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi khởi chạy giao diện JavaFX: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
