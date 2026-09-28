package com.ptit.uno.client;

import javafx.application.Platform;
import javafx.stage.Stage;

public class UnoFXLauncher {
    public static void main(String[] args) {
        try {
            Platform.startup(() -> {
                try {
                    UnoFXApp app = new UnoFXApp();
                    Stage stage = new Stage();
                    app.start(stage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
