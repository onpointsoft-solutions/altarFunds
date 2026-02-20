package com.Modules.User;

import com.Infrastructure.BackendService;
import com.Infrastructure.Base.BaseViewPresenter;
import com.Infrastructure.Handle.MyTooltip;
import com.Infrastructure.Handle.HandleError;
import com.Other.ConfirmEmail.ConfirmController;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Service;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Hyperlink;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Map;

/**
 * Sanctum Authentication Presenter
 * Integrates with Django Backend API
 */
public class SanctumAuthPresenter extends BaseViewPresenter implements Initializable {
    private StringProperty changeMainScene;
    private BackendService backendService;
    private String currentUserRole;
    
    @FXML
    private JFXTextField emailTextField;
    @FXML
    private JFXPasswordField passwordTextField;
    @FXML
    private JFXPasswordField confirmPasswordTextField;
    @FXML
    private JFXTextField firstNameTextField;
    @FXML
    private JFXTextField lastNameTextField;
    @FXML
    private Label errorLabel;
    @FXML
    private Label successLabel;
    @FXML
    private JFXButton loginButton;
    @FXML
    private JFXButton signUpButton;
    @FXML
    private JFXButton switchToSignUpButton;
    @FXML
    private JFXButton switchToLoginButton;
    @FXML
    private JFXProgressBar progressBar;
    @FXML
    private Hyperlink webAppLink;
    @FXML
    private VBox loginContainer;
    @FXML
    private VBox signUpContainer;
    
    private boolean isLoginMode = true;

    public SanctumAuthPresenter(StringProperty changeMainScene) {
        this.changeMainScene = changeMainScene;
        this.backendService = new BackendService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize UI components
        setupUI();
        setupEventHandlers();
        applyTheme();
    }
    
    private void setupUI() {
        // Set initial visibility
        loginContainer.setVisible(true);
        signUpContainer.setVisible(false);
        
        // Setup web app link
        webAppLink.setText("Access Web App →");
        webAppLink.setOnAction(e -> {
            getHostServices().showDocument(backendService.getWebAppUrl());
        });
        
        // Hide progress bar initially
        progressBar.setVisible(false);
        
        // Clear messages
        clearMessages();
    }
    
    private void setupEventHandlers() {
        // Login button
        loginButton.setOnAction(e -> handleLogin());
        
        // Sign up button  
        signUpButton.setOnAction(e -> handleSignUp());
        
        // Switch mode buttons
        switchToSignUpButton.setOnAction(e -> switchToSignUpMode());
        switchToLoginButton.setOnAction(e -> switchToLoginMode());
        
        // Enter key handlers
        emailTextField.setOnKeyPressed(e -> {
            if (e.getCode().getName().equals("Enter")) {
                if (isLoginMode) {
                    handleLogin();
                } else {
                    passwordTextField.requestFocus();
                }
            }
        });
        
        passwordTextField.setOnKeyPressed(e -> {
            if (e.getCode().getName().equals("Enter")) {
                if (isLoginMode) {
                    handleLogin();
                } else {
                    confirmPasswordTextField.requestFocus();
                }
            }
        });
        
        confirmPasswordTextField.setOnKeyPressed(e -> {
            if (e.getCode().getName().equals("Enter") && !isLoginMode) {
                handleSignUp();
            }
        });
    }
    
    private void applyTheme() {
        // Apply modern styling to components
        loginContainer.getStyleClass().add("login-container");
        signUpContainer.getStyleClass().add("login-container");
        
        loginButton.getStyleClass().addAll("primary-button", "animated");
        signUpButton.getStyleClass().addAll("primary-button", "animated");
        switchToSignUpButton.getStyleClass().addAll("accent-button", "animated");
        switchToLoginButton.getStyleClass().addAll("accent-button", "animated");
        
        emailTextField.getStyleClass().add("text-field");
        passwordTextField.getStyleClass().add("text-field");
        confirmPasswordTextField.getStyleClass().add("text-field");
        firstNameTextField.getStyleClass().add("text-field");
        lastNameTextField.getStyleClass().add("text-field");
        
        errorLabel.getStyleClass().add("caption-label");
        successLabel.getStyleClass().add("caption-label");
    }
    
    private void switchToSignUpMode() {
        isLoginMode = false;
        loginContainer.setVisible(false);
        signUpContainer.setVisible(true);
        clearMessages();
        clearFields();
        firstNameTextField.requestFocus();
    }
    
    private void switchToLoginMode() {
        isLoginMode = true;
        signUpContainer.setVisible(false);
        loginContainer.setVisible(true);
        clearMessages();
        clearFields();
        emailTextField.requestFocus();
    }
    
    private void handleLogin() {
        String email = emailTextField.getText().trim();
        String password = passwordTextField.getText();
        
        // Validation
        if (email.isEmpty()) {
            showError("Please enter your email");
            emailTextField.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            showError("Please enter your password");
            passwordTextField.requestFocus();
            return;
        }
        
        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            emailTextField.requestFocus();
            return;
        }
        
        // Show loading
        showLoading(true);
        clearMessages();
        
        // Perform login in background
        Service<Boolean> loginService = new Service<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                boolean success = backendService.login(email, password);
                if (success) {
                    // Load current user profile and key dashboard data in the background
                    Map<String, Object> profile = backendService.getUserProfile();
                    if (profile != null && profile.get("role") != null) {
                        currentUserRole = profile.get("role").toString();
                    }
                    backendService.getFinancialSummary();
                }
                return success;
            }
        };
        
        loginService.setOnSucceeded(e -> {
            showLoading(false);
            if (loginService.getValue()) {
                showSuccess("Login successful! Loading dashboard...");
                // Determine dashboard key based on role (e.g., dashboard:treasurer)
                String dashboardKey = "dashboard";
                if (currentUserRole != null && !currentUserRole.isEmpty()) {
                    dashboardKey = "dashboard:" + currentUserRole.toLowerCase();
                }
                // Load main scene after successful login
                changeMainScene.set(dashboardKey);
            } else {
                showError("Invalid email or password");
            }
        });
        
        loginService.setOnFailed(e -> {
            showLoading(false);
            showError("Login failed. Please check your connection.");
        });
        
        loginService.start();
    }
    
    private void handleSignUp() {
        String firstName = firstNameTextField.getText().trim();
        String lastName = lastNameTextField.getText().trim();
        String email = emailTextField.getText().trim();
        String password = passwordTextField.getText();
        String confirmPassword = confirmPasswordTextField.getText();
        
        // Validation
        if (firstName.isEmpty()) {
            showError("Please enter your first name");
            firstNameTextField.requestFocus();
            return;
        }
        
        if (lastName.isEmpty()) {
            showError("Please enter your last name");
            lastNameTextField.requestFocus();
            return;
        }
        
        if (email.isEmpty()) {
            showError("Please enter your email");
            emailTextField.requestFocus();
            return;
        }
        
        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            emailTextField.requestFocus();
            return;
        }
        
        if (password.isEmpty()) {
            showError("Please enter a password");
            passwordTextField.requestFocus();
            return;
        }
        
        if (password.length() < 8) {
            showError("Password must be at least 8 characters");
            passwordTextField.requestFocus();
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            confirmPasswordTextField.requestFocus();
            return;
        }
        
        // Show loading
        showLoading(true);
        clearMessages();
        
        // Perform registration in background
        Service<Boolean> signUpService = new Service<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return backendService.register(firstName, lastName, email, password, "pastor");
            }
        };
        
        signUpService.setOnSucceeded(e -> {
            showLoading(false);
            if (signUpService.getValue()) {
                showSuccess("Registration successful! Please login.");
                // Switch to login mode after successful registration
                switchToLoginMode();
            } else {
                showError("Registration failed. Email may already be registered.");
            }
        });
        
        signUpService.setOnFailed(e -> {
            showLoading(false);
            showError("Registration failed. Please check your connection.");
        });
        
        signUpService.start();
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisible(show);
        loginButton.setDisable(show);
        signUpButton.setDisable(show);
        switchToSignUpButton.setDisable(show);
        switchToLoginButton.setDisable(show);
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }
    
    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }
    
    private void clearMessages() {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        errorLabel.setText("");
        successLabel.setText("");
    }
    
    private void clearFields() {
        emailTextField.clear();
        passwordTextField.clear();
        confirmPasswordTextField.clear();
        firstNameTextField.clear();
        lastNameTextField.clear();
    }

    public Scene loadLoginForm() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/login.fxml"));
        loader.setController(this);
        return new Scene(loader.load());
    }

    public Scene loadSignUpForm() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/signup.fxml"));
        loader.setController(this);
        return new Scene(loader.load());
    }

    public Scene loadConfirmForm() throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/fxml/confirm.fxml"));
        return new Scene(loader.load());
    }
}
