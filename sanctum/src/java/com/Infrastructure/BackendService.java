package com.Infrastructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Backend Service for connecting to Sanctum Django API
 */
public class BackendService {
    private static final String BASE_URL = "https://sanctum.co.ke/backend/api";
    private static final String WEB_APP_URL = "https://sanctum.co.ke";
    private String authToken = null;
    private String refreshToken = null;
    private final Gson gson = new Gson();
    
    public BackendService() {
        // Test connection on initialization
        testConnection();
    }
    
    /**
     * Core HTTP request helper that mirrors the web ApiClient behaviour.
     * Automatically prefixes the BASE_URL and attaches Authorization header when needed.
     */
    private String request(String endpoint, String method, String bodyJson, boolean requiresAuth) throws IOException {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("Content-Type", "application/json");
        
        if (requiresAuth && authToken != null) {
            connection.setRequestProperty("Authorization", "Bearer " + authToken);
        }
        
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            connection.setDoOutput(true);
            if (bodyJson != null && !bodyJson.isEmpty()) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = bodyJson.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
        }
        
        int responseCode = connection.getResponseCode();
        BufferedReader br = null;
        try {
            if (responseCode >= 200 && responseCode < 300) {
                br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                );
            } else if (connection.getErrorStream() != null) {
                br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8)
                );
            }
            
            String responseBody = null;
            if (br != null) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                responseBody = response.toString();
            }
            
            if (responseCode >= 200 && responseCode < 300) {
                return responseBody;
            } else {
                System.out.println("❌ Request failed (" + method + " " + endpoint + "): " + responseCode + " " + responseBody);
                return null;
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }
    
    /**
     * Test backend connectivity
     */
    public void testConnection() {
        try {
            String response = request("/health/", "GET", null, false);
            if (response != null) {
                System.out.println("✅ Backend connection successful");
            } else {
                System.out.println("⚠️ Backend health check failed");
            }
        } catch (Exception e) {
            System.out.println("❌ Backend connection failed: " + e.getMessage());
            System.out.println("📱 Operating in offline mode");
        }
    }
    
    /**
     * Login to backend and get auth token
     */
    public boolean login(String email, String password) {
        try {
            // Create login request matching the React web client: POST /auth/token/
            Map<String, String> loginData = new HashMap<>();
            loginData.put("email", email);
            loginData.put("password", password);
            
            String jsonInputString = gson.toJson(loginData);
            String responseBody = request("/auth/token/", "POST", jsonInputString, false);
            
            if (responseBody != null) {
                // Parse response and extract top-level access/refresh tokens
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                if (jsonResponse.has("access")) {
                    this.authToken = jsonResponse.get("access").getAsString();
                    if (jsonResponse.has("refresh")) {
                        this.refreshToken = jsonResponse.get("refresh").getAsString();
                    }
                    System.out.println("✅ Login successful");
                    return true;
                } else {
                    System.out.println("❌ Login response missing access token");
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Login error: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Register new user
     */
    public boolean register(String firstName, String lastName, String email, String password, String role) {
        try {
            // Create registration request
            Map<String, Object> registerData = new HashMap<>();
            registerData.put("first_name", firstName);
            registerData.put("last_name", lastName);
            registerData.put("email", email);
            registerData.put("password", password);
            registerData.put("role", role);
            registerData.put("agree_to_terms", true);
            
            String jsonInputString = gson.toJson(registerData);
            String responseBody = request("/auth/register/", "POST", jsonInputString, false);
            
            if (responseBody != null) {
                System.out.println("✅ Registration successful");
                return true;
            } else {
                System.out.println("❌ Registration failed");
            }
        } catch (Exception e) {
            System.out.println("❌ Registration error: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Get user profile
     */
    public Map<String, Object> getUserProfile() {
        if (authToken == null) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            // Align with React apiService.getCurrentUser(): GET /accounts/profile/
            String responseBody = request("/accounts/profile/", "GET", null, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            } else {
                System.out.println("❌ Failed to get profile");
            }
        } catch (Exception e) {
            System.out.println("❌ Profile error: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get church list
     */
    public Map<String, Object> getChurches() {
        if (authToken == null) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String responseBody = request("/churches/", "GET", null, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            } else {
                System.out.println("❌ Failed to get churches");
            }
        } catch (Exception e) {
            System.out.println("❌ Churches error: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get members for a church
     */
    public Map<String, Object> getMembers(int churchId) {
        if (authToken == null) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String responseBody = request("/accounts/members/?church=" + churchId, "GET", null, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            } else {
                System.out.println("❌ Failed to get members");
            }
        } catch (Exception e) {
            System.out.println("❌ Members error: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Get giving/transactions
     */
    public Map<String, Object> getTransactions(int churchId) {
        if (authToken == null) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String responseBody = request("/giving/transactions/?church=" + churchId, "GET", null, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            } else {
                System.out.println("❌ Failed to get transactions");
            }
        } catch (Exception e) {
            System.out.println("❌ Transactions error: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * =========================
     *  Dashboard endpoints
     * =========================
     */
    public Map<String, Object> getFinancialSummary() {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String responseBody = request("/dashboard/financial-summary/", "GET", null, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ getFinancialSummary error: " + e.getMessage());
        }
        return null;
    }
    
    public String getMonthlyTrend() {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            return request("/dashboard/monthly-trend/", "GET", null, true);
        } catch (Exception e) {
            System.out.println("❌ getMonthlyTrend error: " + e.getMessage());
            return null;
        }
    }
    
    public String getIncomeBreakdown() {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            return request("/dashboard/income-breakdown/", "GET", null, true);
        } catch (Exception e) {
            System.out.println("❌ getIncomeBreakdown error: " + e.getMessage());
            return null;
        }
    }
    
    public String getExpenseBreakdown() {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            return request("/dashboard/expense-breakdown/", "GET", null, true);
        } catch (Exception e) {
            System.out.println("❌ getExpenseBreakdown error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Helper to build URL query strings similar to the web client.
     */
    private String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || value == null) {
                continue;
            }
            if (!first) {
                sb.append("&");
            }
            try {
                sb.append(URLEncoder.encode(key, StandardCharsets.UTF_8.name()))
                  .append("=")
                  .append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
            } catch (Exception e) {
                // Fallback to raw value if encoding fails
                sb.append(key).append("=").append(value);
            }
            first = false;
        }
        return sb.toString();
    }
    
    /**
     * =========================
     *  Donations endpoints
     * =========================
     */
    public Map<String, Object> getDonations(Map<String, String> params) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String query = buildQueryString(params);
            String endpoint = query.isEmpty() ? "/donations/" : "/donations/?" + query;
            String responseBody = request(endpoint, "GET", null, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ getDonations error: " + e.getMessage());
        }
        return null;
    }
    
    public Map<String, Object> createDonation(Map<String, Object> donation) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String jsonBody = gson.toJson(donation);
            String responseBody = request("/donations/", "POST", jsonBody, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ createDonation error: " + e.getMessage());
        }
        return null;
    }
    
    public Map<String, Object> updateDonation(String id, Map<String, Object> donation) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String jsonBody = gson.toJson(donation);
            String responseBody = request("/donations/" + id + "/", "PUT", jsonBody, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ updateDonation error: " + e.getMessage());
        }
        return null;
    }
    
    public boolean deleteDonation(String id) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return false;
        }
        
        try {
            String responseBody = request("/donations/" + id + "/", "DELETE", null, true);
            // Successful delete typically returns 204 No Content
            return responseBody != null || true;
        } catch (Exception e) {
            System.out.println("❌ deleteDonation error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * =========================
     *  Expenses endpoints
     * =========================
     */
    public Map<String, Object> getExpenses(Map<String, String> params) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String query = buildQueryString(params);
            String endpoint = query.isEmpty() ? "/expenses/" : "/expenses/?" + query;
            String responseBody = request(endpoint, "GET", null, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ getExpenses error: " + e.getMessage());
        }
        return null;
    }
    
    public Map<String, Object> createExpense(Map<String, Object> expense) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String jsonBody = gson.toJson(expense);
            String responseBody = request("/expenses/", "POST", jsonBody, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ createExpense error: " + e.getMessage());
        }
        return null;
    }
    
    public Map<String, Object> updateExpense(String id, Map<String, Object> expense) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String jsonBody = gson.toJson(expense);
            String responseBody = request("/expenses/" + id + "/", "PUT", jsonBody, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ updateExpense error: " + e.getMessage());
        }
        return null;
    }
    
    public Map<String, Object> approveExpense(String id) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String responseBody = request("/expenses/" + id + "/approve/", "POST", "{}", true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ approveExpense error: " + e.getMessage());
        }
        return null;
    }
    
    public Map<String, Object> rejectExpense(String id, String reason) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("reason", reason);
            String jsonBody = gson.toJson(body);
            String responseBody = request("/expenses/" + id + "/reject/", "POST", jsonBody, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ rejectExpense error: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * =========================
     *  Budgets endpoints
     * =========================
     */
    public Map<String, Object> getBudgets(Map<String, String> params) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String query = buildQueryString(params);
            String endpoint = query.isEmpty() ? "/budgets/" : "/budgets/?" + query;
            String responseBody = request(endpoint, "GET", null, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ getBudgets error: " + e.getMessage());
        }
        return null;
    }
    
    public Map<String, Object> createBudget(Map<String, Object> budget) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String jsonBody = gson.toJson(budget);
            String responseBody = request("/budgets/", "POST", jsonBody, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ createBudget error: " + e.getMessage());
        }
        return null;
    }
    
    public Map<String, Object> updateBudget(String id, Map<String, Object> budget) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String jsonBody = gson.toJson(budget);
            String responseBody = request("/budgets/" + id + "/", "PUT", jsonBody, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ updateBudget error: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * =========================
     *  Members endpoints
     * =========================
     */
    public Map<String, Object> getMembersPaged(Map<String, String> params) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String query = buildQueryString(params);
            String endpoint = query.isEmpty() ? "/members/" : "/members/?" + query;
            String responseBody = request(endpoint, "GET", null, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ getMembersPaged error: " + e.getMessage());
        }
        return null;
    }
    
    public Map<String, Object> createMember(Map<String, Object> member) {
        if (!isAuthenticated()) {
            System.out.println("❌ Not authenticated");
            return null;
        }
        
        try {
            String jsonBody = gson.toJson(member);
            String responseBody = request("/members/", "POST", jsonBody, true);
            if (responseBody != null) {
                return gson.fromJson(responseBody, Map.class);
            }
        } catch (Exception e) {
            System.out.println("❌ createMember error: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Logout and clear token
     */
    public void logout() {
        try {
            if (authToken != null) {
                // Mirror React apiService.logout(): POST /accounts/logout/
                request("/accounts/logout/", "POST", "{}", true);
            }
        } catch (Exception e) {
            System.out.println("❌ Logout error: " + e.getMessage());
        } finally {
            this.authToken = null;
            this.refreshToken = null;
            System.out.println("✅ Logged out successfully");
        }
    }
    
    /**
     * Check if authenticated
     */
    public boolean isAuthenticated() {
        return authToken != null;
    }
    
    public String getAuthToken() {
        return authToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    /**
     * Get web app URL for external links
     */
    public String getWebAppUrl() {
        return WEB_APP_URL;
    }
    
    /**
     * Get backend URL
     */
    public String getBackendUrl() {
        return BASE_URL;
    }
}
