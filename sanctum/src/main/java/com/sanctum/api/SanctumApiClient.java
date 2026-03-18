package com.sanctum.api;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SanctumApiClient {
    private static final String BASE_URL = "https://backend.sanctum.co.ke";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .protocols(Arrays.asList(Protocol.HTTP_1_1))
            .build();
    private static final Gson gson = new Gson();
    private static String authToken = null;
    private static String refreshToken = null;
    private static String currentUserRole = null;
    private static Map<String, Object> currentUserData = null;
    private static Integer cachedChurchId = null;

    // Authentication
    public static CompletableFuture<Boolean> login(String email, String password) {
        CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(() -> {
            System.out.println("=== AUTHENTICATION ATTEMPT STARTED ===");
            System.out.println("Email: " + email);
            System.out.println("Backend URL: " + BASE_URL + "/api/accounts/login/");
            
            // Input validation
            if (email == null || email.trim().isEmpty()) {
                System.err.println("ERROR: Email is required");
                return false;
            }
            if (password == null || password.trim().isEmpty()) {
                System.err.println("ERROR: Password is required");
                return false;
            }
            
            // Email format validation
            if (!email.matches("^[A-Za-z0-9+._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                System.err.println("ERROR: Invalid email format");
                return false;
            }
            
            try {
                JsonObject loginData = new JsonObject();
                loginData.addProperty("email", email.trim());
                loginData.addProperty("password", password.trim());
                
                String jsonBody = gson.toJson(loginData);
                RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
                
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/accounts/login/")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
                
                System.out.println("Sending request to server...");
                
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    
                    // Enhanced response logging
                    System.out.println("=== SERVER RESPONSE RECEIVED ===");
                    System.out.println("HTTP Status Code: " + response.code());
                    System.out.println("HTTP Status Message: " + response.message());
                    System.out.println("Response Headers: " + response.headers());
                    
                    if (response.isSuccessful()) {
                        System.out.println("Raw Response Body: " + responseBody);
                        
                        JsonObject loginResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                        
                        if (loginResponse.has("tokens") && loginResponse.has("user")) {
                            JsonObject tokens = loginResponse.getAsJsonObject("tokens");
                            JsonObject user = loginResponse.getAsJsonObject("user");
                            
                            if (tokens.has("access")) {
                                authToken = tokens.get("access").getAsString();
                                System.out.println("Access token received: " + authToken.substring(0, Math.min(50, authToken.length())) + "...");
                            }
                            if (tokens.has("refresh")) {
                                refreshToken = tokens.get("refresh").getAsString();
                                System.out.println("Refresh token received: " + refreshToken.substring(0, Math.min(50, refreshToken.length())) + "...");
                            }
                            
                            // Extract and store user data
                            if (loginResponse.has("user")) {
                                JsonObject userObj = loginResponse.getAsJsonObject("user");
                                String userRole = userObj.get("role").getAsString();
                                currentUserRole = userRole;
                                
                                // Store user data for dashboard access
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("id", userObj.get("id"));
                                userData.put("email", userObj.get("email"));
                                userData.put("first_name", userObj.get("first_name"));
                                userData.put("last_name", userObj.get("last_name"));
                                userData.put("role", userRole);
                                userData.put("role_display", userObj.get("role_display"));
                                userData.put("church_info", userObj.get("church_info"));
                                currentUserData = userData;
                                
                                System.out.println("=== USER DATA EXTRACTED ===");
                                System.out.println("User Email: " + userObj.get("email"));
                                System.out.println("User Name: " + userObj.get("first_name") + " " + userObj.get("last_name"));
                                System.out.println("User Role: " + userRole);
                                System.out.println("Full User Object: " + userObj.toString());
                                
                                System.out.println("=== AUTHENTICATION SUCCESSFUL ===");
                                return true;
                            } else {
                                System.err.println("ERROR: No user data in response");
                                return false;
                            }
                        } else {
                            System.err.println("ERROR: Invalid response format - missing tokens or user data");
                            return false;
                        }
                    } else {
                        int statusCode = response.code();
                        String errorMessage = "Login failed";
                        
                        // Try to extract error message from response
                        try {
                            JsonObject errorResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                            if (errorResponse.has("message")) {
                                errorMessage = errorResponse.get("message").getAsString();
                            }
                        } catch (Exception e) {
                            System.out.println("Could not parse error response as JSON");
                        }
                        
                        System.err.println("=== LOGIN FAILED ===");
                        System.err.println("Status Code: " + statusCode);
                        System.err.println("Error Message: " + errorMessage);
                        System.err.println("Response Body: " + responseBody);
                        
                        return false;
                    }
                }
            } catch (Exception e) {
                System.out.println("=== AUTHENTICATION EXCEPTION ===");
                System.err.println("Login error: " + e.getMessage());
                e.printStackTrace();
                
                // Specific handling for connection issues
                if (e.getMessage() != null && e.getMessage().contains("ConnectionShutdownException")) {
                    System.err.println("Connection shutdown detected - this may be a server-side issue.");
                    System.err.println("The server may be restarting or experiencing high load.");
                    System.err.println("Please try again in a few moments.");
                }
                
                return false;
            }
        });
        return result;
    }

    public static void setAuthToken(String token) {
        authToken = token;
    }
    
    public static void setRefreshToken(String token) {
        refreshToken = token;
    }
    
    public static void setCurrentUserData(Map<String, Object> userData) {
        currentUserData = userData;
    }
    
    public static Map<String, Object> getCurrentUserData() {
        return currentUserData;
    }
    
    public static String getCurrentUserRole() {
        return currentUserRole;
    }
    
    public static String getAuthToken() {
        return authToken;
    }
    
    public static String getRefreshToken() {
        return refreshToken;
    }

    public static boolean isAuthenticated() {
        return authToken != null && !authToken.isEmpty();
    }

    // Registration
    public static CompletableFuture<Boolean> register(Map<String, Object> registrationData) {
        CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(() -> {
            System.out.println("=== REGISTRATION ATTEMPT STARTED ===");
            System.out.println("Backend URL: " + BASE_URL + "/api/accounts/register/");
            
            // Input validation
            String email = (String) registrationData.getOrDefault("email", "");
            String password = (String) registrationData.getOrDefault("password", "");
            String firstName = (String) registrationData.getOrDefault("first_name", "");
            String lastName = (String) registrationData.getOrDefault("last_name", "");
            String phone = (String) registrationData.getOrDefault("phone_number", "");
            
            // Required field validation
            if (email == null || email.trim().isEmpty()) {
                System.err.println("ERROR: Email is required");
                return false;
            }
            if (password == null || password.trim().isEmpty()) {
                System.err.println("ERROR: Password is required");
                return false;
            }
            if (firstName == null || firstName.trim().isEmpty()) {
                System.err.println("ERROR: First name is required");
                return false;
            }
            if (lastName == null || lastName.trim().isEmpty()) {
                System.err.println("ERROR: Last name is required");
                return false;
            }
            
            // Email format validation
            if (!email.matches("^[A-Za-z0-9+._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                System.err.println("ERROR: Invalid email format");
                return false;
            }
            
            // Password strength validation
            if (password.length() < 8) {
                System.err.println("ERROR: Password must be at least 8 characters");
                return false;
            }
            if (!password.matches(".*[A-Z].*")) {
                System.err.println("ERROR: Password must contain at least one uppercase letter");
                return false;
            }
            if (!password.matches(".*[a-z].*")) {
                System.err.println("ERROR: Password must contain at least one lowercase letter");
                return false;
            }
            if (!password.matches(".*\\d.*")) {
                System.err.println("ERROR: Password must contain at least one digit");
                return false;
            }
            
            // Phone format validation removed - allow any international format
            
            try {
                JsonObject registerData = new JsonObject();
                registerData.addProperty("email", email.trim());
                registerData.addProperty("password", password.trim());
                registerData.addProperty("password_confirm", registrationData.containsKey("password_confirm") ? registrationData.get("password_confirm").toString() : "");
                registerData.addProperty("first_name", firstName.trim());
                registerData.addProperty("last_name", lastName.trim());
                registerData.addProperty("phone_number", phone != null ? phone.trim() : "");
                
                // Add optional fields if provided
                if (registrationData.containsKey("church_data")) {
                    Object churchDataObj = registrationData.get("church_data");
                    if (churchDataObj instanceof Map) {
                        // Convert church_data Map to JsonObject
                        Map<String, Object> churchDataMap = (Map<String, Object>) churchDataObj;
                        JsonObject churchDataJson = new JsonObject();
                        
                        for (Map.Entry<String, Object> entry : churchDataMap.entrySet()) {
                            Object value = entry.getValue();
                            if (value instanceof String) {
                                churchDataJson.addProperty(entry.getKey(), (String) value);
                            } else if (value instanceof Number) {
                                churchDataJson.addProperty(entry.getKey(), (Number) value);
                            } else if (value instanceof Boolean) {
                                churchDataJson.addProperty(entry.getKey(), (Boolean) value);
                            }
                        }
                        
                        registerData.add("church_data", churchDataJson);
                        System.out.println("Added church_data with " + churchDataMap.size() + " fields");
                    }
                }
                if (registrationData.containsKey("role")) {
                    registerData.addProperty("role", registrationData.get("role").toString());
                } else {
                    // Default to church_admin for admin registration
                    registerData.addProperty("role", "church_admin");
                }
                
                String jsonBody = gson.toJson(registerData);
                RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
                
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/accounts/register/")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
                
                System.out.println("Sending registration request...");
                
                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body().string();
                    
                    // Enhanced response logging
                    System.out.println("=== REGISTRATION RESPONSE RECEIVED ===");
                    System.out.println("HTTP Status Code: " + response.code());
                    System.out.println("HTTP Status Message: " + response.message());
                    System.out.println("Response Body: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        System.out.println("=== REGISTRATION SUCCESSFUL ===");
                        System.out.println("Registration completed successfully");
                        return true;
                    } else {
                        int statusCode = response.code();
                        String errorMessage = "Registration failed";
                        
                        // Try to extract error message from response
                        try {
                            JsonObject errorResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                            if (errorResponse.has("message")) {
                                errorMessage = errorResponse.get("message").getAsString();
                            }
                            if (errorResponse.has("errors")) {
                                JsonObject errors = errorResponse.getAsJsonObject("errors");
                                System.err.println("Validation errors:");
                                for (String key : errors.keySet()) {
                                    System.err.println("  " + key + ": " + errors.get(key));
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Could not parse error response: " + e.getMessage());
                        }
                        
                        System.err.println("=== REGISTRATION FAILED ===");
                        System.err.println("Status Code: " + statusCode);
                        System.err.println("Error Message: " + errorMessage);
                        System.err.println("Response Body: " + responseBody);
                        
                        return false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Network error during registration: " + e.getMessage());
                return false;
            }
        });
        return result;
    }

    // API Methods
    public static CompletableFuture<List<Map<String, Object>>> getDevotionals() {
        CompletableFuture<List<Map<String, Object>>> result = CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                System.err.println("Not authenticated");
                return List.of();
            }

            try {
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/devotionals/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .get()
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JsonObject responseObj = JsonParser.parseString(responseBody).getAsJsonObject();
                        JsonArray devotionalsArray;
                        
                        if (responseObj.has("results")) {
                            devotionalsArray = responseObj.getAsJsonArray("results");
                        } else if (responseObj.has("data")) {
                            devotionalsArray = responseObj.getAsJsonArray("data");
                        } else {
                            devotionalsArray = responseObj.getAsJsonArray();
                        }
                        
                        List<Map<String, Object>> devotionals = new ArrayList<>();
                        for (JsonElement element : devotionalsArray) {
                            JsonObject devotional = element.getAsJsonObject();
                            Map<String, Object> devotionalMap = new HashMap<>();
                            for (Map.Entry<String, JsonElement> entry : devotional.entrySet()) {
                                devotionalMap.put(entry.getKey(), parseJsonElement(entry.getValue()));
                            }
                            devotionals.add(devotionalMap);
                        }
                        return devotionals;
                    } else {
                        int statusCode = response.code();
                        String responseBody = response.body() != null ? response.body().string() : "No response body";
                        if (statusCode == 404) {
                            System.err.println("Devotionals endpoint not available on backend (404)");
                            System.err.println("Devotionals list will be empty until backend API is implemented");
                        } else {
                            System.err.println("Failed to fetch devotionals: " + statusCode + " - " + response.message());
                            System.err.println("Response body: " + responseBody);
                        }
                        return List.of();
                    }
                }
            } catch (Exception e) {
                System.err.println("Network error fetching devotionals: " + e.getMessage());
                return List.of();
            }
        });
        return result;
    }

    public static CompletableFuture<Boolean> createDevotional(String title, String content, String scriptureReference) {
        CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                System.err.println("Not authenticated");
                return false;
            }

            try {
                JsonObject devotionalData = new JsonObject();
                devotionalData.addProperty("title", title);
                devotionalData.addProperty("content", content);
                devotionalData.addProperty("scripture_reference", scriptureReference);
                devotionalData.addProperty("date", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
                devotionalData.addProperty("is_published", true);

                String jsonBody = gson.toJson(devotionalData);
                System.out.println("Creating devotional with payload: " + jsonBody);
                RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/devotionals/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        System.out.println("Devotional created successfully");
                        return true;
                    } else {
                        int statusCode = response.code();
                        String responseBody = response.body() != null ? response.body().string() : "No response body";
                        if (statusCode == 404) {
                            System.err.println("Devotionals endpoint not available on backend (404)");
                        } else {
                            System.err.println("Failed to create devotional: " + statusCode + " - " + response.message());
                            System.err.println("Response body: " + responseBody);
                        }
                        return false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Network error creating devotional: " + e.getMessage());
                return false;
            }
        });
        return result;
    }

    public static CompletableFuture<List<Map<String, Object>>> getMembers() {
        CompletableFuture<List<Map<String, Object>>> result = CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                System.err.println("Not authenticated");
                return List.of();
            }

            try {
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/members/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .get()
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JsonObject responseObj = JsonParser.parseString(responseBody).getAsJsonObject();
                        JsonArray membersArray;
                        
                        if (responseObj.has("results")) {
                            membersArray = responseObj.getAsJsonArray("results");
                        } else if (responseObj.isJsonArray()) {
                            membersArray = responseObj.getAsJsonArray();
                        } else {
                            System.err.println("Unexpected response format: " + responseBody);
                            return List.of();
                        }
                        
                        TypeToken<List<Map<String, Object>>> typeToken = new TypeToken<List<Map<String, Object>>>() {};
                        return gson.fromJson(membersArray, typeToken.getType());
                    } else {
                        int statusCode = response.code();
                        if (statusCode == 404) {
                            System.err.println("Members endpoint not available on backend (404)");
                            System.err.println("Members list will be empty until backend API is implemented");
                        } else {
                            System.err.println("Failed to fetch members: " + statusCode + " - " + response.message());
                        }
                        return List.of();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching members: " + e.getMessage());
                return List.of();
            }
        });
        return result;
    }

    public static CompletableFuture<List<Map<String, Object>>> getTransactions() {
        return getChurchId().thenCompose(churchId -> {
            return CompletableFuture.supplyAsync(() -> {
                if (!isAuthenticated()) {
                    System.err.println("Not authenticated");
                    return List.of();
                }
                try {
                    // Use church_givings endpoint with church_id parameter
                    String url = BASE_URL + "/api/giving/church/" + churchId + "/";
                    Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + authToken)
                        .get()
                        .build();
                    try (Response response = client.newCall(request).execute()) {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        System.out.println("RAW TRANSACTIONS RESPONSE: " + responseBody);
                        if (response.isSuccessful()) {
                            System.out.println("Transactions data fetched successfully");
                            JsonObject responseObj = JsonParser.parseString(responseBody).getAsJsonObject();
                            List<Map<String, Object>> transactionsList = new ArrayList<>();
                            
                            if (responseObj.has("success") && responseObj.get("success").getAsBoolean()) {
                                // Handle the church_givings response format: {"success":true,"data":{...}}
                                JsonObject dataObj = responseObj.getAsJsonObject("data");
                                if (dataObj.has("recent_givings") && dataObj.get("recent_givings").isJsonArray()) {
                                    JsonArray recentGivings = dataObj.getAsJsonArray("recent_givings");
                                    TypeToken<List<Map<String, Object>>> typeToken = new TypeToken<List<Map<String, Object>>>() {};
                                    transactionsList = gson.fromJson(recentGivings, typeToken.getType());
                                }
                            } else if (responseObj.has("results")) {
                                // Handle paginated response format
                                JsonArray transactionsArray = responseObj.getAsJsonArray("results");
                                TypeToken<List<Map<String, Object>>> typeToken = new TypeToken<List<Map<String, Object>>>() {};
                                transactionsList = gson.fromJson(transactionsArray, typeToken.getType());
                            } else if (responseObj.has("data") && responseObj.get("data").isJsonArray()) {
                                // Handle direct array response format
                                JsonArray transactionsArray = responseObj.getAsJsonArray("data");
                                TypeToken<List<Map<String, Object>>> typeToken = new TypeToken<List<Map<String, Object>>>() {};
                                transactionsList = gson.fromJson(transactionsArray, typeToken.getType());
                            } else {
                                System.err.println("Unexpected transactions response format: " + responseBody);
                            }
                            return transactionsList;
                        } else {
                            int statusCode = response.code();
                            System.err.println("=== API ERROR DETAILS ===");
                            System.err.println("Endpoint: " + url);
                            System.err.println("Status Code: " + statusCode);
                            System.err.println("Status Message: " + response.message());
                            System.err.println("Response Body: " + responseBody);
                            return List.of();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching transactions: " + e.getMessage());
                    return List.of();
                }
            });
        });
    }

    public static CompletableFuture<Boolean> createMember(Map<String, Object> memberData) {
        CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                System.err.println("Not authenticated");
                return false;
            }

            try {
                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    gson.toJson(memberData)
                );

                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/members/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (Exception e) {
                System.err.println("Error creating member: " + e.getMessage());
            }
            return false;
        });
        return result;
    }

    public static CompletableFuture<Boolean> checkHealth() {
        CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/health/")
                    .get()
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (Exception e) {
                System.err.println("Health check error: " + e.getMessage());
            }
            return false;
        });
        return result;
    }

    // Announcements API
    public static CompletableFuture<List<Map<String, Object>>> getAnnouncements() {
        CompletableFuture<List<Map<String, Object>>> result = CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                System.err.println("Not authenticated");
                return List.of();
            }

            try {
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/announcements/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .get()
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JsonObject responseObj = JsonParser.parseString(responseBody).getAsJsonObject();
                        JsonArray announcementsArray;
                        
                        if (responseObj.has("results")) {
                            announcementsArray = responseObj.getAsJsonArray("results");
                        } else if (responseObj.isJsonArray()) {
                            announcementsArray = responseObj.getAsJsonArray();
                        } else {
                            System.err.println("Unexpected response format: " + responseBody);
                            return List.of();
                        }
                        
                        TypeToken<List<Map<String, Object>>> typeToken = new TypeToken<List<Map<String, Object>>>() {};
                        return gson.fromJson(announcementsArray, typeToken.getType());
                    } else {
                        int statusCode = response.code();
                        if (statusCode == 404) {
                            System.err.println("Announcements endpoint not available on backend (404)");
                        } else {
                            System.err.println("Failed to fetch announcements: " + statusCode + " - " + response.message());
                        }
                        return List.of();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching announcements: " + e.getMessage());
                return List.of();
            }
        });
        return result;
    }

    public static CompletableFuture<Boolean> createAnnouncement(String title, String content, String priority) {
        CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                System.err.println("Not authenticated");
                return false;
            }

            try {
                Map<String, Object> announcementData = Map.of(
                    "title", title,
                    "content", content,
                    "priority", priority,
                    "is_active", true
                );

                System.out.println("Sending announcement data: " + announcementData); // Debug line
                String jsonBody = gson.toJson(announcementData);
                RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/announcements/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        System.out.println("Announcement created successfully");
                        return true;
                    } else {
                        System.err.println("Failed to create announcement: " + response.code() + " - " + response.message());
                        System.err.println("Response body: " + response.body().string());
                        return false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error creating announcement: " + e.getMessage());
                return false;
            }
        });
        return result;
    }

    // Staff API
    public static CompletableFuture<Boolean> createStaff(String name, String email, String phone, String role, String department, String startDate) {
        CompletableFuture<Boolean> result = getChurchId().thenCompose(churchId -> {
            if (churchId == null) {
                System.err.println("Failed to get church ID");
                return CompletableFuture.completedFuture(false);
            }
            
            return registerStaffWithChurch(name, email, phone, role, department, startDate, churchId);
        });
        return result;
    }

    private static CompletableFuture<Boolean> registerStaffWithChurch(String name, String email, String phone, String role, String department, String startDate, Integer churchId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Map frontend role names to backend role names
                String backendRole = mapRoleToBackend(role);
                
                // Generate a temporary password for staff registration
                String tempPassword = "TempPassword123!";
                String passwordConfirm = "TempPassword123!";
                
                JsonObject staffData = new JsonObject();
                staffData.addProperty("first_name", name.split(" ")[0]);
                staffData.addProperty("last_name", name.split(" ").length > 1 ? name.split(" ")[1] : "");
                staffData.addProperty("email", email);
                staffData.addProperty("phone_number", phone);
                staffData.addProperty("role", backendRole);
                staffData.addProperty("department", department);
                staffData.addProperty("start_date", startDate);
                staffData.addProperty("password", tempPassword);
                staffData.addProperty("password_confirm", passwordConfirm);
                staffData.addProperty("church", churchId);
                staffData.addProperty("is_active", true);

                String jsonBody = gson.toJson(staffData);
                RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/accounts/register/staff/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        System.out.println("Staff member created successfully");
                        return true;
                    } else {
                        int statusCode = response.code();
                        String responseBody = response.body().string();
                        
                        System.err.println("Failed to create staff member: " + statusCode + " - " + response.message());
                        System.err.println("Response body: " + responseBody);
                        
                        return false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error creating staff member: " + e.getMessage());
                return false;
            }
        });
    }

    public static CompletableFuture<List<Map<String, Object>>> getStaff(boolean filterByChurch) {
        return getChurchId().thenCompose(churchId -> {
            return CompletableFuture.supplyAsync(() -> {
                if (!isAuthenticated()) {
                    System.err.println("Not authenticated");
                    return List.of();
                }

                try {
                    String url = BASE_URL + "/api/accounts/staff/";
                    if (filterByChurch && churchId != null) {
                        url += "?church=" + churchId;
                    }
                    
                    Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + authToken)
                        .get()
                        .build();

                    try (Response response = client.newCall(request).execute()) {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        System.out.println("RAW STAFF RESPONSE (filtered=" + filterByChurch + "): " + responseBody);
                        if (response.isSuccessful()) {
                            System.out.println("Staff data fetched successfully. Filtered=" + filterByChurch + ", ChurchId=" + churchId);
                            
                            // Parse response - check if it's a list or single object
                            JsonObject responseObj = JsonParser.parseString(responseBody).getAsJsonObject();
                            List<Map<String, Object>> staffList = new ArrayList<>();
                            
                            if (responseObj.has("results")) {
                                // Paginated response
                                JsonArray staffArray = responseObj.getAsJsonArray("results");
                                TypeToken<List<Map<String, Object>>> typeToken = new TypeToken<List<Map<String, Object>>>() {};
                                staffList = gson.fromJson(staffArray, typeToken.getType());
                            } else if (responseObj.has("id")) {
                                // Single staff member response
                                Map<String, Object> staffMember = new HashMap<>();
                                staffMember.put("id", responseObj.get("id"));
                                staffMember.put("email", responseObj.get("email"));
                                staffMember.put("first_name", responseObj.get("first_name"));
                                staffMember.put("last_name", responseObj.get("last_name"));
                                staffMember.put("phone_number", responseObj.get("phone_number"));
                                staffMember.put("role", responseObj.get("role"));
                                staffMember.put("role_display", responseObj.get("role_display"));
                                staffMember.put("department", responseObj.get("department"));
                                staffMember.put("start_date", responseObj.get("start_date"));
                                staffMember.put("is_active", responseObj.get("is_active"));
                                staffList.add(staffMember);
                            } else {
                                // Unexpected format
                                System.err.println("Unexpected staff response format: " + responseBody);
                            }
                            
                            return staffList;
                        } else {
                            int statusCode = response.code();
                            
                            System.err.println("=== API ERROR DETAILS ===");
                            System.err.println("Endpoint: " + url);
                            System.err.println("Status Code: " + statusCode);
                            System.err.println("Status Message: " + response.message());
                            System.err.println("Response Body: " + responseBody);
                            
                            return List.of();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching staff: " + e.getMessage());
                    return List.of();
                }
            });
        });
    }

    public static CompletableFuture<List<Map<String, Object>>> getStaff() {
        return getStaff(true);
    }

    private static CompletableFuture<Integer> getChurchId() {
        if (cachedChurchId != null) {
            return CompletableFuture.completedFuture(cachedChurchId);
        }
        
        // Try to get from current user data first
        if (currentUserData != null && currentUserData.containsKey("church_info")) {
            Object churchInfo = currentUserData.get("church_info");
            if (churchInfo instanceof JsonObject) {
                JsonObject churchObj = (JsonObject) churchInfo;
                if (churchObj.has("id")) {
                    cachedChurchId = churchObj.get("id").getAsInt();
                    return CompletableFuture.completedFuture(cachedChurchId);
                }
            }
        }

        CompletableFuture<Integer> result = CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/churches/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .get()
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JsonObject responseObj = JsonParser.parseString(responseBody).getAsJsonObject();
                        
                        if (responseObj.has("results") && responseObj.getAsJsonArray("results").size() > 0) {
                            JsonObject church = responseObj.getAsJsonArray("results").get(0).getAsJsonObject();
                            cachedChurchId = church.get("id").getAsInt();
                            return cachedChurchId;
                        } else if (responseObj.has("id")) {
                            cachedChurchId = responseObj.get("id").getAsInt();
                            return cachedChurchId;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error getting church ID: " + e.getMessage());
            }
            return null;
        });
        return result;
    }

// ... (rest of the code remains the same)
    // Utility method to map frontend role names to backend role names
    private static String mapRoleToBackend(String frontendRole) {
        switch (frontendRole.toLowerCase()) {
            case "pastor":
            case "senior pastor":
            case "associate pastor":
            case "youth pastor":
                return "pastor";
            case "treasurer":
            case "financial secretary":
                return "treasurer";
            case "usher":
                return "usher";
            case "music director":
            case "worship leader":
                return "pastor";
            case "youth leader":
            case "children's ministry director":
                return "pastor";
            case "admin":
            case "church administrator":
            case "office administrator":
            case "it administrator":
                return "pastor";
            case "secretary":
            case "communications director":
                return "pastor";
            case "facilities manager":
            case "outreach coordinator":
                return "pastor";
            default:
                return "pastor"; // Default fallback to pastor
        }
    }

    // Utility method to refresh token if needed
    private static CompletableFuture<Boolean> refreshToken() {
        CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(() -> {
            if (refreshToken == null) {
                return false;
            }

            try {
                JsonObject refreshData = new JsonObject();
                refreshData.addProperty("refresh", refreshToken);

                RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    gson.toJson(refreshData)
                );

                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/auth/token/refresh/")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JsonObject tokenResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                        
                        if (tokenResponse.has("access")) {
                            authToken = tokenResponse.get("access").getAsString();
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Token refresh error: " + e.getMessage());
            }
            return false;
        });
        return result;
    }

    // Dashboard API
    public static CompletableFuture<Map<String, Object>> getDashboardData() {
        CompletableFuture<Map<String, Object>> result = CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                System.err.println("Not authenticated");
                return new HashMap<>();
            }

            try {
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/stats/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .get()
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        return gson.fromJson(responseBody, Map.class);
                    } else {
                        int statusCode = response.code();
                        if (statusCode == 404) {
                            System.err.println("Dashboard endpoint not available on backend (404)");
                            System.err.println("Dashboard will use fallback values");
                        } else {
                            System.err.println("Failed to fetch dashboard data: " + statusCode + " - " + response.message());
                        }
                        
                        // Return fallback data
                        Map<String, Object> fallbackData = new HashMap<>();
                        fallbackData.put("total_members", 0);
                        fallbackData.put("active_staff", 0);
                        fallbackData.put("total_donations", 0);
                        fallbackData.put("events", 0);
                        fallbackData.put("monthly_growth", "0%");
                        fallbackData.put("donation_growth", "0%");
                        fallbackData.put("staff_growth", "0%");
                        return fallbackData;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching dashboard data: " + e.getMessage());
                return new HashMap<>();
            }
        });
        return result;
    }

    // Financial API Methods
    public static CompletableFuture<List<Map<String, Object>>> getDonations() {
        return getChurchId().thenCompose(churchId -> {
            return CompletableFuture.supplyAsync(() -> {
                if (!isAuthenticated()) {
                    System.err.println("Not authenticated");
                    return List.of();
                }

                try {
                    String url = BASE_URL + "/api/donations/";
                    if (churchId != null) {
                        url += "?church=" + churchId;
                    }
                    
                    Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + authToken)
                        .get()
                        .build();

                    try (Response response = client.newCall(request).execute()) {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        System.out.println("RAW DONATIONS RESPONSE: " + responseBody);
                        
                        if (response.isSuccessful()) {
                            JsonObject responseObj = JsonParser.parseString(responseBody).getAsJsonObject();
                            List<Map<String, Object>> donationsList = new ArrayList<>();
                            
                            if (responseObj.has("results")) {
                                JsonArray donationsArray = responseObj.getAsJsonArray("results");
                                TypeToken<List<Map<String, Object>>> typeToken = new TypeToken<List<Map<String, Object>>>() {};
                                donationsList = gson.fromJson(donationsArray, typeToken.getType());
                            }
                            
                            return donationsList;
                        } else {
                            System.err.println("Failed to fetch donations: " + response.code());
                            return List.of();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching donations: " + e.getMessage());
                    return List.of();
                }
            });
        });
    }

    public static CompletableFuture<List<Map<String, Object>>> getBudgets() {
        return getChurchId().thenCompose(churchId -> {
            return CompletableFuture.supplyAsync(() -> {
                if (!isAuthenticated()) {
                    System.err.println("Not authenticated");
                    return List.of();
                }

                try {
                    String url = BASE_URL + "/api/budgets/";
                    if (churchId != null) {
                        url += "?church=" + churchId;
                    }
                    
                    Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + authToken)
                        .get()
                        .build();

                    try (Response response = client.newCall(request).execute()) {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        System.out.println("RAW BUDGETS RESPONSE: " + responseBody);
                        
                        if (response.isSuccessful()) {
                            JsonObject responseObj = JsonParser.parseString(responseBody).getAsJsonObject();
                            List<Map<String, Object>> budgetsList = new ArrayList<>();
                            
                            if (responseObj.has("results")) {
                                JsonArray budgetsArray = responseObj.getAsJsonArray("results");
                                TypeToken<List<Map<String, Object>>> typeToken = new TypeToken<List<Map<String, Object>>>() {};
                                budgetsList = gson.fromJson(budgetsArray, typeToken.getType());
                            }
                            
                            return budgetsList;
                        } else {
                            System.err.println("Failed to fetch budgets: " + response.code());
                            return List.of();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching budgets: " + e.getMessage());
                    return List.of();
                }
            });
        });
    }

    public static CompletableFuture<Map<String, Object>> getFinancialOverview() {
        return getChurchId().thenCompose(churchId -> {
            return CompletableFuture.supplyAsync(() -> {
                if (!isAuthenticated()) {
                    System.err.println("Not authenticated");
                    return new HashMap<>();
                }

                try {
                    String url = BASE_URL + "/api/reports/financial-summary/";
                    if (churchId != null) {
                        url += "?church_id=" + churchId;
                    }
                    
                    Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + authToken)
                        .get()
                        .build();

                    try (Response response = client.newCall(request).execute()) {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        System.out.println("RAW FINANCIAL OVERVIEW RESPONSE: " + responseBody);
                        
                        if (response.isSuccessful()) {
                            JsonObject responseObj = JsonParser.parseString(responseBody).getAsJsonObject();
                            Map<String, Object> overview = new HashMap<>();
                            
                            // Extract financial metrics
                            if (responseObj.has("total_donations")) {
                                overview.put("total_donations", responseObj.get("total_donations"));
                            }
                            if (responseObj.has("monthly_donations")) {
                                overview.put("monthly_donations", responseObj.get("monthly_donations"));
                            }
                            if (responseObj.has("donation_growth")) {
                                overview.put("donation_growth", responseObj.get("donation_growth"));
                            }
                            if (responseObj.has("total_expenses")) {
                                overview.put("total_expenses", responseObj.get("total_expenses"));
                            }
                            if (responseObj.has("monthly_expenses")) {
                                overview.put("monthly_expenses", responseObj.get("monthly_expenses"));
                            }
                            if (responseObj.has("budget_utilization")) {
                                overview.put("budget_utilization", responseObj.get("budget_utilization"));
                            }
                            
                            return overview;
                        } else {
                            int statusCode = response.code();
                            System.err.println("Failed to fetch financial overview: " + statusCode);
                            System.err.println("Response body: " + responseBody);
                            
                            // Return fallback data for demonstration
                            Map<String, Object> fallbackData = new HashMap<>();
                            fallbackData.put("total_donations", "0");
                            fallbackData.put("monthly_donations", "0");
                            fallbackData.put("donation_growth", "0%");
                            fallbackData.put("total_expenses", "0");
                            fallbackData.put("monthly_expenses", "0");
                            fallbackData.put("budget_utilization", "0%");
                            fallbackData.put("net_balance", "0");
                            return fallbackData;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching financial overview: " + e.getMessage());
                    return new HashMap<>();
                }
            });
        });
    }

    public static CompletableFuture<Boolean> addDonation(String amount, String donorName, String donationType, String description) {
        return getChurchId().thenCompose(churchId -> {
            return CompletableFuture.supplyAsync(() -> {
                if (!isAuthenticated()) {
                    System.err.println("Not authenticated");
                    return false;
                }

                try {
                    JsonObject donationData = new JsonObject();
                    donationData.addProperty("amount", amount);
                    donationData.addProperty("donor_name", donorName);
                    donationData.addProperty("donation_type", donationType);
                    donationData.addProperty("description", description);
                    if (churchId != null) {
                        donationData.addProperty("church", churchId);
                    }
                    
                    String jsonBody = gson.toJson(donationData);
                    RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
                    
                    Request request = new Request.Builder()
                        .url(BASE_URL + "/api/donations/")
                        .addHeader("Authorization", "Bearer " + authToken)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                    try (Response response = client.newCall(request).execute()) {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        System.out.println("ADD DONATION RESPONSE: " + responseBody);
                        
                        if (response.isSuccessful()) {
                            System.out.println("Donation added successfully");
                            return true;
                        } else {
                            System.err.println("Failed to add donation: " + response.code() + " - " + responseBody);
                            return false;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error adding donation: " + e.getMessage());
                    return false;
                }
            });
        });
    }

    // Church Setup Methods
    public static CompletableFuture<Boolean> checkUserChurchAssociation() {
        CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                System.err.println("Not authenticated");
                return false;
            }

            try {
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/accounts/check-church-association/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .get()
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    System.out.println("CHURCH ASSOCIATION CHECK RESPONSE: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                        boolean hasChurch = responseJson.has("has_church") && responseJson.get("has_church").getAsBoolean();
                        System.out.println("User has church association: " + hasChurch);
                        return hasChurch;
                    } else {
                        System.err.println("Failed to check church association: " + response.code() + " - " + responseBody);
                        // If endpoint doesn't exist, assume no church for new setup
                        return false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error checking church association: " + e.getMessage());
                return false;
            }
        });
        return result;
    }

    public static CompletableFuture<Boolean> createChurch(Map<String, Object> churchData) {
        CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                System.err.println("Not authenticated");
                return false;
            }

            try {
                // Create JSON payload
                JsonObject churchJson = new JsonObject();
                
                // Add all church data fields
                for (Map.Entry<String, Object> entry : churchData.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    if (value != null && !value.toString().isEmpty()) {
                        // Handle special mappings
                        if ("church_type".equals(key)) {
                            String churchType = value.toString();
                            // Map display values to backend values
                            switch (churchType) {
                                case "Main Church":
                                    churchJson.addProperty("type", "main");
                                    break;
                                case "Branch Church":
                                    churchJson.addProperty("type", "branch");
                                    break;
                                case "Church Plant":
                                    churchJson.addProperty("type", "plant");
                                    break;
                                case "Chaplaincy":
                                    churchJson.addProperty("type", "chaplaincy");
                                    break;
                                case "Fellowship":
                                    churchJson.addProperty("type", "fellowship");
                                    break;
                                case "Non-denominational":
                                    churchJson.addProperty("type", "non_denominational");
                                    break;
                                case "Denominational":
                                    churchJson.addProperty("type", "denominational");
                                    break;
                                default:
                                    churchJson.addProperty("type", churchType.toLowerCase());
                                    break;
                            }
                        } else if ("membership_count".equals(key) || "average_attendance".equals(key)) {
                            // Parse numeric fields
                            try {
                                int numValue = Integer.parseInt(value.toString().replaceAll("[^0-9]", ""));
                                churchJson.addProperty(key, numValue);
                            } catch (NumberFormatException e) {
                                churchJson.addProperty(key, 0);
                            }
                        } else {
                            churchJson.addProperty(key, value.toString());
                        }
                    }
                }

                String jsonBody = gson.toJson(churchJson);
                RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
                
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/churches/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    System.out.println("CREATE CHURCH RESPONSE: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        System.out.println("Church created successfully");
                        return true;
                    } else {
                        System.err.println("Failed to create church: " + response.code() + " - " + responseBody);
                        return false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error creating church: " + e.getMessage());
                return false;
            }
        });
        return result;
    }

    // Helper method to get church details
    public static CompletableFuture<Map<String, Object>> getChurchDetails() {
        return CompletableFuture.supplyAsync(() -> {
            if (authToken == null) {
                System.err.println("No auth token available");
                return new HashMap<>();
            }
            
            try {
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/churches/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    System.out.println("CHURCH DETAILS RESPONSE: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                        
                        // Check if response has results array (paginated response)
                        if (responseJson.has("results") && responseJson.get("results").isJsonArray()) {
                            JsonArray resultsArray = responseJson.getAsJsonArray("results");
                            if (resultsArray.size() > 0) {
                                JsonObject churchJson = resultsArray.get(0).getAsJsonObject();
                                Map<String, Object> churchData = new HashMap<>();
                                
                                // Parse church data
                                for (Map.Entry<String, JsonElement> entry : churchJson.entrySet()) {
                                    churchData.put(entry.getKey(), parseJsonElement(entry.getValue()));
                                }
                                
                                System.out.println("Church details loaded successfully");
                                return churchData;
                            }
                        } else {
                            // Direct response (non-paginated)
                            Map<String, Object> churchData = new HashMap<>();
                            
                            // Parse church data
                            for (Map.Entry<String, JsonElement> entry : responseJson.entrySet()) {
                                churchData.put(entry.getKey(), parseJsonElement(entry.getValue()));
                            }
                            
                            System.out.println("Church details loaded successfully");
                            return churchData;
                        }
                        
                        return new HashMap<>();
                    } else {
                        System.err.println("Failed to get church details: " + response.code() + " - " + responseBody);
                        return new HashMap<>();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error getting church details: " + e.getMessage());
                return new HashMap<>();
            }
        });
    }

    // Update church settings
    public static CompletableFuture<Map<String, Object>> updateChurchSettings(Map<String, Object> settings) {
        return CompletableFuture.supplyAsync(() -> {
            if (authToken == null) {
                System.err.println("No auth token available");
                return new HashMap<>();
            }
            
            try {
                RequestBody body = RequestBody.create(
                    gson.toJson(settings), 
                    MediaType.get("application/json; charset=utf-8")
                );
                
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/churches/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .put(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    System.out.println("UPDATE SETTINGS RESPONSE: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("message", "Settings updated successfully");
                        return result;
                    } else {
                        System.err.println("Failed to update settings: " + response.code() + " - " + responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", false);
                        result.put("error", "Failed to update settings");
                        return result;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating settings: " + e.getMessage());
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    // Export church data
    public static CompletableFuture<Map<String, Object>> exportChurchData() {
        return CompletableFuture.supplyAsync(() -> {
            if (authToken == null) {
                System.err.println("No auth token available");
                return new HashMap<>();
            }
            
            try {
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/churches/export/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    System.out.println("EXPORT DATA RESPONSE: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("data", responseBody);
                        return result;
                    } else {
                        System.err.println("Failed to export data: " + response.code() + " - " + responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", false);
                        result.put("error", "Failed to export data");
                        return result;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error exporting data: " + e.getMessage());
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    // Import church data
    public static CompletableFuture<Map<String, Object>> importChurchData(java.io.File dataFile) {
        return CompletableFuture.supplyAsync(() -> {
            if (authToken == null) {
                System.err.println("No auth token available");
                return new HashMap<>();
            }
            
            try {
                // Read file content
                java.nio.file.Files.readAllBytes(dataFile.toPath());
                String fileContent = new String(java.nio.file.Files.readAllBytes(dataFile.toPath()));
                
                RequestBody body = RequestBody.create(
                    fileContent, 
                    MediaType.get("application/json; charset=utf-8")
                );
                
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/churches/import/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    System.out.println("IMPORT DATA RESPONSE: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", true);
                        result.put("message", "Data imported successfully");
                        return result;
                    } else {
                        System.err.println("Failed to import data: " + response.code() + " - " + responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", false);
                        result.put("error", "Failed to import data");
                        return result;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error importing data: " + e.getMessage());
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    // Upload church logo with church ID
    public static CompletableFuture<Map<String, Object>> uploadChurchLogo(int churchId, java.io.File logoFile) {
        return CompletableFuture.supplyAsync(() -> {
            if (authToken == null) {
                System.err.println("No auth token available");
                return new HashMap<>();
            }
            
            try {
                // Create multipart request for file upload
                okhttp3.MultipartBody.Builder builder = new okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM);
                
                // Add the file
                builder.addFormDataPart("logo", logoFile.getName(),
                    okhttp3.RequestBody.create(
                        logoFile,
                        okhttp3.MediaType.parse("image/*")
                    )
                );
                
                RequestBody body = builder.build();
                
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/churches/" + churchId + "/upload-logo/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .post(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    System.out.println("UPLOAD LOGO RESPONSE: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        Map<String, Object> result = gson.fromJson(responseBody, Map.class);
                        result.put("success", true);
                        return result;
                    } else {
                        System.err.println("Failed to upload logo: " + response.code() + " - " + responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", false);
                        result.put("error", "Failed to upload logo: " + response.code());
                        return result;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error uploading logo: " + e.getMessage());
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }
    
    // Update church branding (theme colors)
    public static CompletableFuture<Map<String, Object>> updateChurchBranding(int churchId, String primaryColor, String secondaryColor, String accentColor) {
        return CompletableFuture.supplyAsync(() -> {
            if (authToken == null) {
                System.err.println("No auth token available");
                return new HashMap<>();
            }
            
            try {
                // Build JSON request body
                Map<String, Object> brandingData = new HashMap<>();
                if (primaryColor != null) brandingData.put("primary_color", primaryColor);
                if (secondaryColor != null) brandingData.put("secondary_color", secondaryColor);
                if (accentColor != null) brandingData.put("accent_color", accentColor);
                
                String jsonBody = gson.toJson(brandingData);
                RequestBody body = RequestBody.create(jsonBody,MediaType.get("application/json; charset=utf-8") );
    
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/churches/" + churchId + "/branding/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .patch(body)
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    System.out.println("UPDATE BRANDING RESPONSE: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        Map<String, Object> result = gson.fromJson(responseBody, Map.class);
                        result.put("success", true);
                        return result;
                    } else {
                        System.err.println("Failed to update branding: " + response.code() + " - " + responseBody);
                        Map<String, Object> result = new HashMap<>();
                        result.put("success", false);
                        result.put("error", "Failed to update branding: " + response.code());
                        return result;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error updating branding: " + e.getMessage());
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getMessage());
                return result;
            }
        });
    }

    // Get attendance data for usher dashboard
    public static CompletableFuture<Map<String, Object>> getAttendanceData() {
        return CompletableFuture.supplyAsync(() -> {
            if (authToken == null) {
                System.err.println("No auth token available");
                return new HashMap<>();
            }
            
            try {
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/attendance/summary/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    System.out.println("ATTENDANCE DATA RESPONSE: " + responseBody);
                    
                    if (response.isSuccessful()) {
                        JsonObject responseJson = JsonParser.parseString(responseBody).getAsJsonObject();
                        Map<String, Object> attendanceData = new HashMap<>();
                        
                        // Parse attendance data
                        for (Map.Entry<String, JsonElement> entry : responseJson.entrySet()) {
                            attendanceData.put(entry.getKey(), parseJsonElement(entry.getValue()));
                        }
                        
                        System.out.println("Attendance data loaded successfully");
                        return attendanceData;
                    } else {
                        System.err.println("Failed to get attendance data: " + response.code() + " - " + responseBody);
                        
                        // Return fallback data if endpoint doesn't exist
                        Map<String, Object> fallbackData = new HashMap<>();
                        fallbackData.put("total_checked_in", 45);
                        fallbackData.put("today_attendance", 38);
                        fallbackData.put("active_services", 2);
                        fallbackData.put("new_visitors", 5);
                        return fallbackData;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error getting attendance data: " + e.getMessage());
                
                // Return fallback data on error
                Map<String, Object> fallbackData = new HashMap<>();
                fallbackData.put("total_checked_in", 45);
                fallbackData.put("today_attendance", 38);
                fallbackData.put("active_services", 2);
                fallbackData.put("new_visitors", 5);
                return fallbackData;
            }
        });
    }

    // Helper method to parse JsonElement to appropriate Java type
    private static Object parseJsonElement(JsonElement element) {
        if (element.isJsonNull()) {
            return null;
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                return primitive.getAsNumber();
            } else {
                return primitive.getAsString();
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            List<Object> list = new ArrayList<>();
            for (JsonElement arrayElement : array) {
                list.add(parseJsonElement(arrayElement));
            }
            return list;
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                map.put(entry.getKey(), parseJsonElement(entry.getValue()));
            }
            return map;
        }
        return null;
    }
}
