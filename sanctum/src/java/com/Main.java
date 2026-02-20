package com;

import com.Modules.User.SanctumAuthPresenter;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.scene.layout.*;
import javafx.fxml.FXMLLoader;
import javafx.application.Application;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class Main extends Application {
    private Stage primaryStage;
    private SanctumAuthPresenter authenticationController;
    private HBox layout = new HBox();
    private boolean firstLogin = true;
    private StringProperty changeMainScene = new SimpleStringProperty("login");
    
    // Backend API configuration
    private static final String BACKEND_URL = "https://sanctum.co.ke/backend/api";
    private static final String WEB_APP_URL = "https://sanctum.co.ke";

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.authenticationController = new SanctumAuthPresenter(changeMainScene);
        this.applyTheme();
        this.listenSceneChanging();
        this.testBackendConnection();
    }
    
    private void applyTheme() {
        // Apply the new Sanctum theme
        Scene scene = new Scene(new StackPane(), 850, 650);
        scene.getStylesheets().add(getClass().getResource("/css/sanctum_theme.css").toExternalForm());
        this.primaryStage.setScene(scene);
    }
    
    private void testBackendConnection() {
        // Test backend connectivity
        new Thread(() -> {
            try {
                URL url = new URL(BACKEND_URL + "/health/");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    System.out.println("✅ Backend connection successful");
                } else {
                    System.out.println("⚠️ Backend returned: " + responseCode);
                }
            } catch (Exception e) {
                System.out.println("❌ Backend connection failed: " + e.getMessage());
                System.out.println("📱 Falling back to local mode");
            }
        }).start();
    }

    private void listenSceneChanging() throws IOException {
        this.changeMainScene.addListener((observableValue, oldValue, newValue) -> {
            try {
                this.layout.getChildren().clear();

                if (newValue.equals("login") || newValue.equals("sign_up")||newValue.equals("confirm")) {
                    this.primaryStage.setMinWidth(400);
                    this.primaryStage.setMinHeight(500);

                    if (newValue.equals("sign_up")) {
                        Scene signUpScene = this.authenticationController.loadSignUpForm();
                        signUpScene.getStylesheets().add(getClass().getResource("/css/sanctum_theme.css").toExternalForm());
                        this.primaryStage.setScene(signUpScene);
                        this.primaryStage.setTitle("Sanctum - Sign Up");
                        this.primaryStage.getIcons().add(new Image("/image/Other/sign_up.png"));
                        this.primaryStage.show();
                    }
                    else if(newValue.equals("confirm")){
                        Scene confirmScene = this.authenticationController.loadConfirmForm();
                        confirmScene.getStylesheets().add(getClass().getResource("/css/sanctum_theme.css").toExternalForm());
                        this.primaryStage.setScene(confirmScene);
                    }
                    else {
                        Scene loginScene = this.authenticationController.loadLoginForm();
                        loginScene.getStylesheets().add(getClass().getResource("/css/sanctum_theme.css").toExternalForm());
                        this.primaryStage.setScene(loginScene);
                        this.primaryStage.setTitle("Sanctum - Sign In");
                        this.primaryStage.getIcons().add(new Image("/image/Other/sign_in.png"));
                        this.primaryStage.show();
                    }
                } else {
                    this.loadMainScene();
                    firstLogin = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        
        Scene loginScene = this.authenticationController.loadLoginForm();
        loginScene.getStylesheets().add(getClass().getResource("/css/sanctum_theme.css").toExternalForm());
        this.primaryStage.setScene(loginScene);
        this.primaryStage.setTitle("Sanctum - Church Management System");
        this.primaryStage.getIcons().add(new Image("/image/Other/sign_in.png"));
        this.primaryStage.setMinWidth(400);
        this.primaryStage.setMinHeight(500);
        this.primaryStage.show();
    }

    private void loadMainScene() throws IOException {
        if (!firstLogin) {
            this.layout = new HBox();
        }
        try {
            FXMLLoader sidebarLoader = new FXMLLoader(Main.class.getResource("/fxml/sidebar.fxml"));
            Parent sidebar = sidebarLoader.load();

            this.layout.getChildren().add(sidebar);
            this.changeMainView(sidebarLoader.getController());
            this.primaryStage.setTitle("Sanctum - Church Management");
            this.primaryStage.getIcons().add(new Image("/image/Other/home_icon.png"));
            
            Scene mainScene = new Scene(this.layout, 1200, 800);
            mainScene.getStylesheets().add(getClass().getResource("/css/sanctum_theme.css").toExternalForm());
            this.primaryStage.setScene(mainScene);
            this.primaryStage.setMinWidth(800);
            this.primaryStage.setMinHeight(600);
            this.primaryStage.show();
        }catch (Exception e){
            System.out.println(e);
        }
    }

    private void changeMainView(MainPresenter mainPresenter) {
        mainPresenter.getChangeScene().addListener((observableValue, oldValue, newValue) -> {
            if (newValue) {
                if (this.layout.getChildren().size() == 2) {
                    this.layout.getChildren().set(1, mainPresenter.getMainView());
                } else {
                    this.layout.getChildren().add(mainPresenter.getMainView());
                }

                mainPresenter.setChangeScene(false);
            }
        });
        mainPresenter.setChangeScene(true);
        mainPresenter.setChangeMainScene(this.changeMainScene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}



