package com.sanctum.api;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import java.util.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Sanctum API Client
 * Covers: auth, members, staff, donations, budgets, transactions (giving),
 *         expenses, accounts, financial overview, reports, announcements,
 *         devotionals, church management, attendance.
 */
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

    // ─── Auth State ──────────────────────────────────────────────────────────
    private static String authToken       = null;
    private static String storedRefreshToken = null;
    private static String currentUserRole = null;
    private static Map<String, Object> currentUserData = null;
    private static Integer cachedChurchId = null;

    // ─── Auth Getters / Setters ──────────────────────────────────────────────
    public static void setAuthToken(String token)                      { authToken = token; }
    public static void setRefreshToken(String token)                   { storedRefreshToken = token; }
    public static void setCurrentUserData(Map<String,Object> data)     { currentUserData = data; }
    public static Map<String, Object> getCurrentUserData()             { return currentUserData; }
    public static String getCurrentUserRole()                          { return currentUserRole; }
    public static String getAuthToken()                                { return authToken; }
    public static String getRefreshToken()                             { return storedRefreshToken; }
    public static boolean isAuthenticated()                            { return authToken != null && !authToken.isEmpty(); }

    // ════════════════════════════════════════════════════════════════════════
    //  AUTHENTICATION
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<Boolean> login(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            if (email == null || email.trim().isEmpty()) return false;
            if (password == null || password.trim().isEmpty()) return false;
            if (!email.matches("^[A-Za-z0-9+._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) return false;
            try {
                JsonObject body = new JsonObject();
                body.addProperty("email", email.trim());
                body.addProperty("password", password.trim());
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/accounts/login/")
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body().string();
                    System.out.println("LOGIN response ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return false;
                    JsonObject root = JsonParser.parseString(rb).getAsJsonObject();
                    if (!root.has("tokens") || !root.has("user")) return false;
                    JsonObject tokens = root.getAsJsonObject("tokens");
                    JsonObject user   = root.getAsJsonObject("user");
                    if (tokens.has("access"))  authToken = tokens.get("access").getAsString();
                    if (tokens.has("refresh")) storedRefreshToken = tokens.get("refresh").getAsString();
                    currentUserRole = user.get("role").getAsString();
                    Map<String,Object> ud = new HashMap<>();
                    ud.put("id",           user.has("id")           ? user.get("id")           : null);
                    ud.put("email",        user.has("email")        ? user.get("email")        : null);
                    ud.put("first_name",   user.has("first_name")   ? user.get("first_name").getAsString() : "User");
                    ud.put("last_name",    user.has("last_name")    ? user.get("last_name").getAsString()  : "");
                    ud.put("role",         currentUserRole);
                    ud.put("role_display", user.has("role_display") ? user.get("role_display") : null);
                    ud.put("church_info",  user.has("church_info")  ? user.get("church_info")  : null);
                    currentUserData = ud;
                    return true;
                }
            } catch (Exception e) { System.err.println("Login error: "+e.getMessage()); return false; }
        });
    }

    public static CompletableFuture<Boolean> register(Map<String,Object> data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("email",            data.getOrDefault("email","").toString());
                body.addProperty("password",         data.getOrDefault("password","").toString());
                body.addProperty("password_confirm", data.getOrDefault("password_confirm","").toString());
                body.addProperty("first_name",       data.getOrDefault("first_name","").toString());
                body.addProperty("last_name",        data.getOrDefault("last_name","").toString());
                body.addProperty("phone_number",     data.getOrDefault("phone_number","").toString());
                body.addProperty("role",             data.getOrDefault("role","church_admin").toString());
                if (data.containsKey("church_data") && data.get("church_data") instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String,Object> cd = (Map<String,Object>) data.get("church_data");
                    JsonObject cj = new JsonObject();
                    cd.forEach((k,v) -> { if (v instanceof String) cj.addProperty(k,(String)v); else if (v instanceof Number) cj.addProperty(k,(Number)v); });
                    body.add("church_data", cj);
                }
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/accounts/register/")
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    System.out.println("REGISTER response ["+resp.code()+"]: "+resp.body().string());
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("Register error: "+e.getMessage()); return false; }
        });
    }

    public static CompletableFuture<Boolean> refreshAccessToken() {
        return CompletableFuture.supplyAsync(() -> {
            if (storedRefreshToken == null) return false;
            try {
                JsonObject body = new JsonObject();
                body.addProperty("refresh", storedRefreshToken);
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/auth/token/refresh/")
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    if (!resp.isSuccessful()) return false;
                    JsonObject root = JsonParser.parseString(resp.body().string()).getAsJsonObject();
                    if (root.has("access")) { authToken = root.get("access").getAsString(); return true; }
                    return false;
                }
            } catch (Exception e) { System.err.println("Token refresh error: "+e.getMessage()); return false; }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASSWORD RESET
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Simple result carrier returned by the two password-reset API calls.
     * {@code success} is true when the server returned a 2xx response.
     * {@code message} contains either the server's human-readable message or
     * a first validation-error string on failure.
     */
    public static final class PasswordResetResult {
        public final boolean success;
        public final String  message;
        public PasswordResetResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    /**
     * Change the currently-authenticated user's password.
     * POST /api/accounts/password/change/
     * Requires a valid Bearer token (user must be logged in).
     *
     * Body: { "current_password": "...",
     *         "new_password": "...",
     *         "new_password_confirm": "..." }
     */
    public static CompletableFuture<PasswordResetResult> changePassword(
            String currentPassword, String newPassword, String newPasswordConfirm) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) {
                return new PasswordResetResult(false, "You are not logged in.");
            }
            try {
                JsonObject body = new JsonObject();
                body.addProperty("current_password",    currentPassword);
                body.addProperty("new_password",         newPassword);
                body.addProperty("new_password_confirm", newPasswordConfirm);

                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/accounts/password/change/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();

                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("CHANGE PASSWORD [" + resp.code() + "]: " + rb);

                    if (resp.isSuccessful()) {
                        String msg = extractMessage(rb, "Password changed successfully");
                        return new PasswordResetResult(true, msg);
                    }
                    String errMsg = extractFirstError(rb);
                    return new PasswordResetResult(false, errMsg);
                }
            } catch (Exception e) {
                System.err.println("changePassword error: " + e.getMessage());
                return new PasswordResetResult(false, null);
            }
        });
    }

    /**
     * Step 1 – POST /api/accounts/password/reset/
     * <p>Body: {@code {"email": "<email>"}}</p>
     * Success: server returns 200 and sends a token to the user's email.
     */
    public static CompletableFuture<PasswordResetResult> requestPasswordReset(String email) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("email", email.trim().toLowerCase());

                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/accounts/password/reset/")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();

                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("PASSWORD RESET REQUEST [" + resp.code() + "]: " + rb);

                    if (resp.isSuccessful()) {
                        String msg = extractMessage(rb, "Password reset email sent");
                        return new PasswordResetResult(true, msg);
                    }
                    // Extract a human-readable error from the response JSON
                    String errMsg = extractFirstError(rb);
                    return new PasswordResetResult(false, errMsg);
                }
            } catch (Exception e) {
                System.err.println("requestPasswordReset error: " + e.getMessage());
                return new PasswordResetResult(false, null);
            }
        });
    }

    /**
     * Step 2 – POST /api/accounts/password/reset/confirm/
     * <p>Body: {@code {"token": "<uuid>", "new_password": "...", "new_password_confirm": "..."}}</p>
     * Success: server returns 200 and the password has been updated.
     */
    public static CompletableFuture<PasswordResetResult> confirmPasswordReset(
            String token, String newPassword, String newPasswordConfirm) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("token",                token.trim());
                body.addProperty("new_password",         newPassword);
                body.addProperty("new_password_confirm", newPasswordConfirm);

                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/accounts/password/reset/confirm/")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();

                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("PASSWORD RESET CONFIRM [" + resp.code() + "]: " + rb);

                    if (resp.isSuccessful()) {
                        String msg = extractMessage(rb, "Password reset successful");
                        return new PasswordResetResult(true, msg);
                    }
                    String errMsg = extractFirstError(rb);
                    return new PasswordResetResult(false, errMsg);
                }
            } catch (Exception e) {
                System.err.println("confirmPasswordReset error: " + e.getMessage());
                return new PasswordResetResult(false, null);
            }
        });
    }

    /** Pull the "message" field from a JSON response, falling back to {@code fallback}. */
    private static String extractMessage(String json, String fallback) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("message")) return obj.get("message").getAsString();
        } catch (Exception ignored) { }
        return fallback;
    }

    /**
     * Pull the first human-readable validation error from a DRF error response.
     * DRF returns errors as {@code {"field": ["msg"]} } or {@code {"detail": "msg"}}
     * or {@code {"non_field_errors": ["msg"]}}.
     */
    private static String extractFirstError(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            // "detail" is used by DRF for non-field errors
            if (obj.has("detail")) return obj.get("detail").getAsString();
            // "non_field_errors" for cross-field validation failures
            if (obj.has("non_field_errors")) {
                JsonElement nfe = obj.get("non_field_errors");
                if (nfe.isJsonArray() && nfe.getAsJsonArray().size() > 0)
                    return nfe.getAsJsonArray().get(0).getAsString();
            }
            // first field error
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                JsonElement val = entry.getValue();
                if (val.isJsonArray() && val.getAsJsonArray().size() > 0)
                    return val.getAsJsonArray().get(0).getAsString();
                if (val.isJsonPrimitive()) return val.getAsString();
            }
        } catch (Exception ignored) { }
        return null;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CHURCH HELPERS
    // ════════════════════════════════════════════════════════════════════════
    private static CompletableFuture<Integer> getChurchId() {
        if (cachedChurchId != null) return CompletableFuture.completedFuture(cachedChurchId);
        if (currentUserData != null && currentUserData.containsKey("church_info")) {
            Object ci = currentUserData.get("church_info");
            if (ci instanceof JsonObject) {
                JsonObject jo = (JsonObject) ci;
                if (jo.has("id")) { cachedChurchId = jo.get("id").getAsInt(); return CompletableFuture.completedFuture(cachedChurchId); }
            }
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/churches/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .get().build();
                try (Response resp = client.newCall(req).execute()) {
                    if (!resp.isSuccessful()) return null;
                    JsonObject root = JsonParser.parseString(resp.body().string()).getAsJsonObject();
                    if (root.has("results")) {
                        JsonArray arr = root.getAsJsonArray("results");
                        if (arr.size()>0) { cachedChurchId = arr.get(0).getAsJsonObject().get("id").getAsInt(); return cachedChurchId; }
                    } else if (root.has("id")) {
                        cachedChurchId = root.get("id").getAsInt(); return cachedChurchId;
                    }
                }
            } catch (Exception e) { System.err.println("getChurchId error: "+e.getMessage()); }
            return null;
        });
    }

    public static CompletableFuture<Boolean> checkHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request req = new Request.Builder().url(BASE_URL+"/api/health/").get().build();
                try (Response resp = client.newCall(req).execute()) { return resp.isSuccessful(); }
            } catch (Exception e) { return false; }
        });
    }

    public static CompletableFuture<Boolean> checkUserChurchAssociation() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/accounts/check-church-association/")
                    .addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    if (!resp.isSuccessful()) return false;
                    JsonObject root = JsonParser.parseString(resp.body().string()).getAsJsonObject();
                    return root.has("has_church") && root.get("has_church").getAsBoolean();
                }
            } catch (Exception e) { return false; }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FINANCIAL OVERVIEW  (KPIs for dashboard)
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<Map<String,Object>> getFinancialOverview() {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return new HashMap<>();
            try {
                String url = BASE_URL+"/api/reports/financial-summary/";
                if (churchId != null) url += "?church_id="+churchId;
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("FINANCIAL OVERVIEW ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return fallbackFinancialData();
                    JsonObject root = JsonParser.parseString(rb).getAsJsonObject();
                    Map<String,Object> result = new HashMap<>();
                    
                    // Extract data from the nested "data" object if it exists
                    JsonObject data = root.has("data") ? root.getAsJsonObject("data") : root;
                    
                    for (Map.Entry<String,JsonElement> entry : data.entrySet()) {
                        result.put(entry.getKey(), parseJsonElement(entry.getValue()));
                    }
                    return result;
                }
            } catch (Exception e) { System.err.println("getFinancialOverview error: "+e.getMessage()); return fallbackFinancialData(); }
        }));
    }

    private static Map<String,Object> fallbackFinancialData() {
        Map<String,Object> m = new HashMap<>();
        m.put("total_donations","0"); m.put("monthly_donations","0");
        m.put("total_expenses","0");  m.put("net_balance","0");
        m.put("total_assets","0");    m.put("total_income","0");
        return m;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FINANCIAL REPORT  (for Reports page)
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<Map<String,Object>> getFinancialReport() {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return new HashMap<>();
            try {
                String url = BASE_URL+"/api/reports/financial-summary/";
                if (churchId != null) url += "?church_id="+churchId;
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("FINANCIAL REPORT ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return new HashMap<>();
                    JsonObject root = JsonParser.parseString(rb).getAsJsonObject();
                    Map<String,Object> result = new HashMap<>();
                    for (Map.Entry<String,JsonElement> e : root.entrySet()) result.put(e.getKey(), parseJsonElement(e.getValue()));
                    return result;
                }
            } catch (Exception e) { System.err.println("getFinancialReport error: "+e.getMessage()); return new HashMap<>(); }
        }));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DONATIONS
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<List<Map<String,Object>>> getDonations() {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                String url = BASE_URL+"/api/donations/";
                if (churchId != null) url += "?church="+churchId;
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("DONATIONS ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getDonations error: "+e.getMessage()); return List.<Map<String,Object>>of(); }
        }));
    }

    public static CompletableFuture<Boolean> addDonation(String amount, String donorName, String donationType, String notes) {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                // Add required fields
                String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                
                JsonObject body = new JsonObject();
                body.addProperty("amount", amount);
                body.addProperty("donor_name", donorName);
                body.addProperty("donation_type", donationType);
                body.addProperty("notes", notes);
                body.addProperty("transaction_date", today);
                body.addProperty("created_by", 1); // Required field - user ID
                body.addProperty("updated_by", 1); // Required field - user ID  
                body.addProperty("member", 1); // Required field - member ID (using default)
                body.addProperty("category", 1); // Required field - category ID (default to tithe)
                if (churchId != null) body.addProperty("church", churchId);
                
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/donations/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("ADD DONATION ["+resp.code()+"]: "+rb);
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("addDonation error: "+e.getMessage()); return false; }
        }));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GIVING TRANSACTIONS  (church giving ledger / transaction history)
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<List<Map<String,Object>>> getGivingTransactions() {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                String url = BASE_URL + "/api/giving/church/" + (churchId != null ? churchId : "0") + "/";
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("GIVING TRANSACTIONS ["+resp.code()+"]: "+rb);

                    if (!resp.isSuccessful()) {
                        // Fallback: return plain donations list instead
                        System.out.println("Giving transactions endpoint failed — falling back to donations endpoint");
                        return List.<Map<String,Object>>of();
                    }

                    JsonElement root = JsonParser.parseString(rb);
                    if (root.isJsonObject()) {
                        JsonObject obj = root.getAsJsonObject();
                        // {"success":true,"data":{"recent_givings":[...]}}
                        if (obj.has("success") && obj.get("success").getAsBoolean() && obj.has("data")) {
                            JsonObject data = obj.getAsJsonObject("data");
                            if (data.has("recent_givings") && data.get("recent_givings").isJsonArray()) {
                                return parseJsonArray(data.getAsJsonArray("recent_givings"));
                            }
                        }
                        if (obj.has("results")) return parseJsonArray(obj.getAsJsonArray("results"));
                        if (obj.has("data") && obj.get("data").isJsonArray()) return parseJsonArray(obj.getAsJsonArray("data"));
                    } else if (root.isJsonArray()) {
                        return parseJsonArray(root.getAsJsonArray());
                    }
                    return List.<Map<String,Object>>of();
                }
            } catch (Exception e) {
                System.err.println("getGivingTransactions error: "+e.getMessage());
                return List.<Map<String,Object>>of();
            }
        })).thenCompose(result -> {
            // If the giving-church endpoint returned nothing, fall back to donations
            if (result.isEmpty()) {
                System.out.println("Transaction data loaded from individual endpoint.");
                return getDonations();
            }
            return CompletableFuture.completedFuture(result);
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BUDGETS
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<List<Map<String,Object>>> getBudgets() {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                String url = BASE_URL+"/api/budgets/";
                if (churchId != null) url += "?church="+churchId;
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("BUDGETS ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getBudgets error: "+e.getMessage()); return List.<Map<String,Object>>of(); }
        }));
    }

    public static CompletableFuture<Boolean> addBudget(String name, String department, double allocatedAmount, String period, int year, Integer month) {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                JsonObject body = new JsonObject();
                body.addProperty("name", name);
                body.addProperty("department", department);
                body.addProperty("allocated_amount", allocatedAmount);
                body.addProperty("spent_amount", 0);
                body.addProperty("period", period);
                body.addProperty("year", year);
                if (month != null) body.addProperty("month", month);
                if (churchId != null) body.addProperty("church", churchId);
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/budgets/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    System.out.println("ADD BUDGET ["+resp.code()+"]: "+resp.body().string());
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("addBudget error: "+e.getMessage()); return false; }
        }));
    }

    public static CompletableFuture<Boolean> updateBudget(String budgetId, String name, String department, double allocatedAmount, String period, int year, Integer month) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                JsonObject body = new JsonObject();
                body.addProperty("name", name); body.addProperty("department", department);
                body.addProperty("allocated_amount", allocatedAmount); body.addProperty("period", period); body.addProperty("year", year);
                if (month != null) body.addProperty("month", month);
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/budgets/"+budgetId+"/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .patch(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    System.out.println("UPDATE BUDGET ["+resp.code()+"]: "+resp.body().string());
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("updateBudget error: "+e.getMessage()); return false; }
        });
    }

    public static CompletableFuture<Boolean> deleteBudget(String budgetId) {        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/budgets/"+budgetId+"/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .delete()
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    System.out.println("DELETE BUDGET ["+resp.code()+"]");
                    // 204 No Content is the expected success response for DELETE
                    return resp.code() == 204 || resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("deleteBudget error: "+e.getMessage()); return false; }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  EXPENSES  ← NEW
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<List<Map<String,Object>>> getExpenses() {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                String url = BASE_URL+"/api/expenses/";
                if (churchId != null) url += "?church="+churchId;
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("EXPENSES ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getExpenses error: "+e.getMessage()); return List.<Map<String,Object>>of(); }
        }));
    }

    public static CompletableFuture<Boolean> addExpense(String amount, String title, String category, String vendor, String date) {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                JsonObject body = new JsonObject();
                body.addProperty("amount", amount);
                body.addProperty("title", title);
                body.addProperty("category", category);
                body.addProperty("vendor", vendor);
                body.addProperty("date", date);
                body.addProperty("status", "pending");
                if (churchId != null) body.addProperty("church", churchId);
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/expenses/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("ADD EXPENSE ["+resp.code()+"]: "+rb);
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("addExpense error: "+e.getMessage()); return false; }
        }));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ACCOUNTS  ← NEW
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<List<Map<String,Object>>> getAccounts() {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                String url = BASE_URL+"/api/churches/bank-accounts/";
                if (churchId != null) url += "?church="+churchId;
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("ACCOUNTS ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getAccounts error: "+e.getMessage()); return List.<Map<String,Object>>of(); }
        }));
    }

    public static CompletableFuture<Boolean> addAccount(String name, String type, String bank, String accountNumber, String openingBalance) {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                JsonObject body = new JsonObject();
                body.addProperty("name", name);
                body.addProperty("account_type", type);
                body.addProperty("bank_name", bank);
                body.addProperty("account_number", accountNumber);
                body.addProperty("balance", openingBalance.isEmpty() ? "0" : openingBalance);
                body.addProperty("currency", "KES");
                body.addProperty("is_active", true);
                if (churchId != null) body.addProperty("church", churchId);
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/accounts/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("ADD ACCOUNT ["+resp.code()+"]: "+rb);
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("addAccount error: "+e.getMessage()); return false; }
        }));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BUDGET ACCESS PINs
    // ════════════════════════════════════════════════════════════════════════

    /** GET /api/budgets/pins/ — list all PINs created by the treasurer */
    public static CompletableFuture<List<Map<String,Object>>> getBudgetPins() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/budgets/pins/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("BUDGET PINS [" + resp.code() + "]: " + rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) {
                System.err.println("getBudgetPins error: " + e.getMessage());
                return List.<Map<String,Object>>of();
            }
        });
    }

    /**
     * POST /api/budgets/pins/ — generate a new access PIN.
     * @param label   human-readable description (e.g. "Q2 Budget Review")
     * @param hours   validity window in hours (1–720)
     * @param maxUses maximum number of uses, or -1 for unlimited
     * @return the created PIN object map, or empty map on failure
     */
    public static CompletableFuture<Map<String,Object>> createBudgetPin(
            String label, int hours, int maxUses) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return new HashMap<String,Object>();
            try {
                JsonObject body = new JsonObject();
                body.addProperty("label", label);
                body.addProperty("hours", hours);
                if (maxUses > 0) body.addProperty("max_uses", maxUses);
                // else omit → unlimited
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/budgets/pins/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("CREATE PIN [" + resp.code() + "]: " + rb);
                    if (!resp.isSuccessful()) return new HashMap<String,Object>();
                    Map<String,Object> result = new HashMap<>();
                    JsonObject obj = JsonParser.parseString(rb).getAsJsonObject();
                    obj.entrySet().forEach(e -> result.put(e.getKey(), parseJsonElement(e.getValue())));
                    return result;
                }
            } catch (Exception e) {
                System.err.println("createBudgetPin error: " + e.getMessage());
                return new HashMap<String,Object>();
            }
        });
    }

    /** PATCH /api/budgets/pins/<id>/revoke/ — deactivate a PIN immediately */
    public static CompletableFuture<Boolean> revokeBudgetPin(int pinId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/budgets/pins/" + pinId + "/revoke/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .patch(RequestBody.create("{}", MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    System.out.println("REVOKE PIN [" + resp.code() + "]");
                    return resp.isSuccessful();
                }
            } catch (Exception e) {
                System.err.println("revokeBudgetPin error: " + e.getMessage());
                return false;
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GIVING CATEGORIES
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<List<Map<String,Object>>> getGivingCategories() {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                String url = BASE_URL+"/api/giving/categories/";
                if (churchId != null) url += "?church="+churchId;
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("GIVING CATEGORIES ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getGivingCategories error: "+e.getMessage()); return List.<Map<String,Object>>of(); }
        }));
    }

    public static CompletableFuture<Boolean> addGivingCategory(String name, String description, boolean hasTarget, double monthlyTarget, double yearlyTarget) {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                JsonObject body = new JsonObject();
                body.addProperty("name", name); body.addProperty("description", description);
                body.addProperty("is_active", true); body.addProperty("has_target", hasTarget);
                body.addProperty("monthly_target", monthlyTarget); body.addProperty("yearly_target", yearlyTarget);
                body.addProperty("is_tax_deductible", true); body.addProperty("display_order", 0);
                if (churchId != null) body.addProperty("church", churchId);
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/giving/categories/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    System.out.println("ADD GIVING CAT ["+resp.code()+"]: "+resp.body().string());
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("addGivingCategory error: "+e.getMessage()); return false; }
        }));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MEMBERS
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<List<Map<String,Object>>> getMembers() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/members/")
                    .addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("MEMBERS ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    // Parse either array root or paginated object
                    JsonElement root = JsonParser.parseString(rb);
                    if (root.isJsonArray()) return parseJsonArray(root.getAsJsonArray());
                    if (root.isJsonObject()) {
                        JsonObject obj = root.getAsJsonObject();
                        if (obj.has("results")) return parseJsonArray(obj.getAsJsonArray("results"));
                    }
                    return List.<Map<String,Object>>of();
                }
            } catch (Exception e) { System.err.println("getMembers error: "+e.getMessage()); return List.<Map<String,Object>>of(); }
        });
    }

    public static CompletableFuture<Boolean> createMember(Map<String,Object> memberData) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/members/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(memberData), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) { return resp.isSuccessful(); }
            } catch (Exception e) { System.err.println("createMember error: "+e.getMessage()); return false; }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  STAFF
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<List<Map<String,Object>>> getStaff() { return getStaff(true); }

    public static CompletableFuture<List<Map<String,Object>>> getStaff(boolean filterByChurch) {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                String url = BASE_URL+"/api/accounts/staff/";
                if (filterByChurch && churchId != null) url += "?church="+churchId;
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("STAFF ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getStaff error: "+e.getMessage()); return List.<Map<String,Object>>of(); }
        }));
    }

    public static CompletableFuture<Boolean> createStaff(String name, String email, String phone, String role, String department, String startDate) {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated() || churchId == null) return false;
            try {
                String[] parts = name.split(" ", 2);
                JsonObject body = new JsonObject();
                body.addProperty("first_name",   parts[0]);
                body.addProperty("last_name",    parts.length > 1 ? parts[1] : "");
                body.addProperty("email",        email);
                body.addProperty("phone_number", phone);
                body.addProperty("role",         mapRoleToBackend(role));
                body.addProperty("department",   department);
                body.addProperty("start_date",   startDate);
                body.addProperty("password",     "TempPassword123!");
                body.addProperty("password_confirm","TempPassword123!");
                body.addProperty("church",       churchId);
                body.addProperty("is_active",    true);
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/accounts/register/staff/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    System.out.println("CREATE STAFF ["+resp.code()+"]: "+resp.body().string());
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("createStaff error: "+e.getMessage()); return false; }
        }));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ANNOUNCEMENTS
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<List<Map<String,Object>>> getAnnouncements() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/announcements/")
                    .addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("ANNOUNCEMENTS ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getAnnouncements error: "+e.getMessage()); return List.<Map<String,Object>>of(); }
        });
    }

    public static CompletableFuture<Boolean> createAnnouncement(String title, String content, String priority) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                // Map display values to backend choices: low | medium | high | urgent
                String backendPriority = mapAnnouncementPriority(priority);

                JsonObject body = new JsonObject();
                body.addProperty("title", title);
                body.addProperty("content", content);
                body.addProperty("priority", backendPriority);
                body.addProperty("target_audience", "all");
                body.addProperty("is_active", true);
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/announcements/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    System.out.println("CREATE ANNOUNCEMENT ["+resp.code()+"]: "+resp.body().string());
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("createAnnouncement error: "+e.getMessage()); return false; }
        });
    }

    /**
     * Maps UI display strings to the backend priority choices.
     * Backend accepts: "low" | "medium" | "high" | "urgent"
     */
    private static String mapAnnouncementPriority(String displayValue) {
        if (displayValue == null) return "medium";
        switch (displayValue.trim().toLowerCase()) {
            case "low":    return "low";
            case "high":   return "high";
            case "urgent": return "urgent";
            case "normal": // legacy — treat as medium
            case "medium":
            default:       return "medium";
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DEVOTIONALS
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<List<Map<String,Object>>> getDevotionals() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/devotionals/")
                    .addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("DEVOTIONALS ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getDevotionals error: "+e.getMessage()); return List.<Map<String,Object>>of(); }
        });
    }

    public static CompletableFuture<List<Map<String,Object>>> getDevotionalReactions(int devotionalId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/devotionals/"+devotionalId+"/reactions/")
                    .addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("REACTIONS ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getDevotionalReactions error: "+e.getMessage()); return List.<Map<String,Object>>of(); }
        });
    }

    public static CompletableFuture<Boolean> reactToDevotional(int devotionalId, String reactionType) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                JsonObject body = new JsonObject();
                body.addProperty("reaction_type", reactionType);
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/devotionals/"+devotionalId+"/react/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("REACT ["+resp.code()+"]: "+rb);
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("reactToDevotional error: "+e.getMessage()); return false; }
        });
    }

    public static CompletableFuture<Boolean> removeDevotionalReaction(int devotionalId, String reactionType) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/devotionals/"+devotionalId+"/react/?reaction_type="+reactionType)
                    .addHeader("Authorization","Bearer "+authToken)
                    .delete()
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("REMOVE REACT ["+resp.code()+"]: "+rb);
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("removeDevotionalReaction error: "+e.getMessage()); return false; }
        });
    }

    public static CompletableFuture<Boolean> createDevotional(String title, String content, String scriptureRef) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                JsonObject body = new JsonObject();
                body.addProperty("title", title); body.addProperty("content", content);
                body.addProperty("scripture_reference", scriptureRef);
                body.addProperty("date", java.time.LocalDate.now().toString());
                body.addProperty("is_published", true);
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/devotionals/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    System.out.println("CREATE DEVOTIONAL ["+resp.code()+"]: "+resp.body().string());
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("createDevotional error: "+e.getMessage()); return false; }
        });
    }

    public static CompletableFuture<List<Map<String,Object>>> getDevotionalComments(int devotionalId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/devotionals/"+devotionalId+"/comments/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .get()
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("DEVOTIONAL COMMENTS ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getDevotionalComments error: "+e.getMessage()); return List.<Map<String,Object>>of(); }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ATTENDANCE
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<Map<String,Object>> getAttendanceData() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return fallbackAttendanceData();
            try {
                // /api/attendance/records/ returns a list — derive summary from it
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/attendance/records/")
                    .addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("ATTENDANCE ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return fallbackAttendanceData();
                    List<Map<String,Object>> records = parseListFromResponse(rb);
                    String today = java.time.LocalDate.now().toString();
                    long checkedIn = records.stream()
                        .filter(r -> {
                            Object t = r.get("check_in_time");
                            return t != null && t.toString().startsWith(today);
                        }).count();
                    long visitors = records.stream()
                        .filter(r -> Boolean.TRUE.equals(r.get("is_visitor"))).count();
                    Map<String,Object> result = new HashMap<>();
                    result.put("total_checked_in", checkedIn);
                    result.put("today_attendance", checkedIn);
                    result.put("active_services",  0);
                    result.put("new_visitors",     visitors);
                    return result;
                }
            } catch (Exception e) { System.err.println("getAttendanceData error: "+e.getMessage()); return fallbackAttendanceData(); }
        });
    }

    private static Map<String,Object> fallbackAttendanceData() {
        Map<String,Object> m = new HashMap<>();
        m.put("total_checked_in",0); m.put("today_attendance",0); m.put("active_services",0); m.put("new_visitors",0);
        return m;
    }

    public static CompletableFuture<List<Map<String,Object>>> getAttendanceRecords() {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                String url = BASE_URL+"/api/attendance/records/";
                if (churchId != null) url += "?church="+churchId;
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("ATTENDANCE RECORDS ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getAttendanceRecords error: "+e.getMessage()); return List.<Map<String,Object>>of(); }
        }));
    }

    /**
     * GET /api/attendance/members/?member=<userId>&page_size=N
     * Returns all attendance records for a specific member.
     */
    public static CompletableFuture<List<Map<String,Object>>> getMemberAttendanceHistory(int userId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                String url = BASE_URL + "/api/attendance/members/?member=" + userId + "&page_size=" + limit;
                Request req = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization","Bearer " + authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("MEMBER_ATTENDANCE_HISTORY [" + resp.code() + "]: " + rb.substring(0,Math.min(200,rb.length())));
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) {
                System.err.println("getMemberAttendanceHistory: " + e.getMessage());
                return List.<Map<String,Object>>of();
            }
        });
    }

    /**
     * GET /api/giving/transactions-list/?member__user=<userId>
     * Returns giving history for a specific member (using GivingTransactionViewSet list).
     */
    public static CompletableFuture<List<Map<String,Object>>> getMemberGivingHistory(int userId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                String url = BASE_URL + "/api/giving/transactions-list/?member__user=" + userId + "&page_size=" + limit;
                Request req = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization","Bearer " + authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("MEMBER_GIVING_HISTORY [" + resp.code() + "]");
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) {
                System.err.println("getMemberGivingHistory: " + e.getMessage());
                return List.<Map<String,Object>>of();
            }
        });
    }
     /* member_name, arrival_time, is_present, is_visitor, notes.
     * GET /api/attendance/members/?service_date=YYYY-MM-DD
     */
    public static CompletableFuture<List<Map<String,Object>>> getMemberAttendances(String serviceDate) {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String,Object>>of();
            try {
                String url = BASE_URL + "/api/attendance/members/?service_date=" + serviceDate;
                if (churchId != null) url += "&church=" + churchId;
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("MEMBER ATTENDANCES ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) {
                System.err.println("getMemberAttendances error: "+e.getMessage());
                return List.<Map<String,Object>>of();
            }
        }));
    }

    public static CompletableFuture<Boolean> checkInMember(String memberId, String serviceType) {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                JsonObject body = new JsonObject();
                // Attempt to parse as integer; backend expects member ID
                try {
                    body.addProperty("member", Integer.parseInt(memberId.trim()));
                } catch (NumberFormatException e) {
                    // memberId is a name, not an ID — cannot send to API
                    System.err.println("checkInMember: memberId is not an integer: " + memberId);
                    return false;
                }
                body.addProperty("service_type", serviceType);
                body.addProperty("check_in_time", java.time.LocalDateTime.now().toString());
                body.addProperty("status", "present");
                if (churchId != null) body.addProperty("church", churchId);
                
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/attendance/records/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("CHECK IN ["+resp.code()+"]: "+rb);
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("checkInMember error: "+e.getMessage()); return false; }
        }));
    }

    /**
     * Mark a member as present on an existing attendance record.
     * POST /api/attendance/records/{attendanceRecordId}/mark_member_present/
     * attendanceRecordId is a UUID string stored in lastAttendanceRecordId.
     * This overload accepts an int sentinel (1 = success from getOrCreateAttendanceRecord)
     * and reads the real UUID from lastAttendanceRecordId.
     */
    public static CompletableFuture<Boolean> markMemberPresent(int sentinel, int memberId, String notes) {
        String recordId = lastAttendanceRecordId;
        if (sentinel < 0 || recordId == null) return CompletableFuture.completedFuture(false);
        return markMemberPresentByUuid(recordId, memberId, notes);
    }

    public static CompletableFuture<Boolean> markMemberPresentByUuid(String attendanceRecordId, int memberId, String notes) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                JsonObject body = new JsonObject();
                body.addProperty("member_id", memberId);
                body.addProperty("notes", notes != null ? notes : "Marked by usher");
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/attendance/records/" + attendanceRecordId + "/mark_member_present/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("MARK PRESENT [" + resp.code() + "]: " + rb);
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("markMemberPresent error: " + e.getMessage()); return false; }
        });
    }

    /**
     * Get or create today's attendance record for the given service name.
     *
     * Flow:
     *  1. Fetch ChurchService list → find one whose name matches serviceType (case-insensitive).
     *     If no exact match, use the first available service.
     *  2. Try to find an existing AttendanceRecord for today + that service.
     *  3. If none, create one.
     *
     * Returns the attendance record ID (UUID string from backend) cast to a synthetic int
     * by hashing, OR we store the UUID string in a thread-local.
     *
     * NOTE: The AttendanceRecord PK is a UUID. We return -1 on failure and store the
     * raw UUID string in lastAttendanceRecordId for use by markMemberPresent.
     */
    private static volatile String lastAttendanceRecordId = null;

    /**
     * Get or create today's attendance record for the given ChurchService integer ID.
     * Returns 1 (success sentinel) and stores the UUID in lastAttendanceRecordId,
     * or -1 on failure.
     */
    public static CompletableFuture<Integer> getOrCreateAttendanceRecord(int serviceId) {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated() || churchId == null || serviceId <= 0) return -1;
            try {
                String today = java.time.LocalDate.now().toString();

                // ── Step 1: look for an existing record today ─────────
                String getUrl = BASE_URL + "/api/attendance/records/?service_date=" + today
                    + "&service=" + serviceId + "&church=" + churchId;
                Request getReq = new Request.Builder()
                    .url(getUrl)
                    .addHeader("Authorization", "Bearer " + authToken)
                    .get().build();
                try (Response getResp = client.newCall(getReq).execute()) {
                    if (getResp.isSuccessful()) {
                        String rb = getResp.body() != null ? getResp.body().string() : "";
                        List<Map<String, Object>> existing = parseListFromResponse(rb);
                        if (!existing.isEmpty()) {
                            Object id = existing.get(0).get("id");
                            if (id != null) {
                                lastAttendanceRecordId = id.toString();
                                System.out.println("Found existing record: " + lastAttendanceRecordId);
                                return 1;
                            }
                        }
                    }
                }

                // ── Step 2: create a new record ───────────────────────
                JsonObject body = new JsonObject();
                body.addProperty("service",             serviceId);
                body.addProperty("service_date",        today);
                body.addProperty("church",              churchId);
                body.addProperty("total_attendance",    0);
                body.addProperty("male_attendance",     0);
                body.addProperty("female_attendance",   0);
                body.addProperty("children_attendance", 0);
                body.addProperty("youth_attendance",    0);
                body.addProperty("visitors_count",      0);
                body.addProperty("new_converts",        0);
                Request postReq = new Request.Builder()
                    .url(BASE_URL + "/api/attendance/records/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response postResp = client.newCall(postReq).execute()) {
                    String rb = postResp.body() != null ? postResp.body().string() : "";
                    System.out.println("CREATE ATTENDANCE RECORD [" + postResp.code() + "]: " + rb);
                    if (postResp.isSuccessful()) {
                        JsonObject obj = JsonParser.parseString(rb).getAsJsonObject();
                        String newId = obj.has("id") ? obj.get("id").getAsString() : null;
                        if (newId != null) {
                            lastAttendanceRecordId = newId;
                            return 1;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("getOrCreateAttendanceRecord error: " + e.getMessage());
            }
            return -1;
        }));
    }

    /**
     * Convenience overload: resolves a service name → ID via getChurchServices(),
     * then delegates to the int-ID overload.
     */
    public static CompletableFuture<Integer> getOrCreateAttendanceRecord(String serviceTypeName) {
        return getChurchServices().thenCompose(services -> {
            int resolvedId = -1;
            for (Map<String, Object> svc : services) {
                String name = svc.getOrDefault("name", "").toString();
                if (name.equalsIgnoreCase(serviceTypeName)) {
                    Object id = svc.get("id");
                    if (id instanceof Number) { resolvedId = ((Number) id).intValue(); break; }
                }
            }
            if (resolvedId < 0 && !services.isEmpty()) {
                Object id = services.get(0).get("id");
                if (id instanceof Number) resolvedId = ((Number) id).intValue();
            }
            if (resolvedId < 0) {
                System.err.println("getOrCreateAttendanceRecord: no ChurchService found — add services via Admin");
                return CompletableFuture.completedFuture(-1);
            }
            return getOrCreateAttendanceRecord(resolvedId);
        });
    }

    /**
     * Register a walk-in visitor and mark them on today's attendance record.
     * Creates a MemberAttendance entry with is_visitor=true linked to the
     * most-recently obtained attendance record.
     *
     * POST /api/attendance/members/
     * Body: { "attendance_record": "<uuid>", "is_visitor": true,
     *         "notes": "<name> — <purpose> — Host: <host>" }
     *
     * Note: visitor does not have a User account, so we store their details
     * in the notes field and set member to null by omitting it (the backend
     * allows null member for visitors if the model permits, otherwise we
     * increment the attendance_record.visitors_count via a PATCH).
     */
    public static CompletableFuture<Boolean> registerVisitor(
            String name, String phone, String purpose, String host) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            String recordId = lastAttendanceRecordId;
            if (recordId == null) {
                System.err.println("registerVisitor: no attendance record — call getOrCreateAttendanceRecord first");
                return false;
            }
            try {
                String notes = name
                    + (phone.isEmpty()   ? "" : " | " + phone)
                    + (purpose.isEmpty() ? "" : " | " + purpose)
                    + (host.isEmpty()    ? "" : " | Host: " + host);

                // Try to create a MemberAttendance row with is_visitor=true
                JsonObject body = new JsonObject();
                body.addProperty("attendance_record", recordId);
                body.addProperty("is_visitor",        true);
                body.addProperty("notes",             notes);
                // member field is intentionally omitted — visitor has no account

                Request postReq = new Request.Builder()
                    .url(BASE_URL + "/api/attendance/members/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(postReq).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("REGISTER VISITOR [" + resp.code() + "]: " + rb);
                    if (resp.isSuccessful()) return true;
                }

                // Fallback: PATCH the visitors_count on the attendance record
                JsonObject patch = new JsonObject();
                patch.addProperty("visitors_count", 1); // backend should increment, not replace
                patch.addProperty("notes", notes);
                Request patchReq = new Request.Builder()
                    .url(BASE_URL + "/api/attendance/records/" + recordId + "/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .patch(RequestBody.create(gson.toJson(patch), MediaType.parse("application/json")))
                    .build();
                try (Response patchResp = client.newCall(patchReq).execute()) {
                    String rb = patchResp.body() != null ? patchResp.body().string() : "";
                    System.out.println("VISITOR PATCH [" + patchResp.code() + "]: " + rb);
                    return patchResp.isSuccessful();
                }
            } catch (Exception e) {
                System.err.println("registerVisitor error: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Quick bulk attendance mark.
     * POST /api/attendance/records/quick_mark_attendance/
     */
    public static CompletableFuture<Map<String, Object>> quickMarkAttendance(
            int serviceId, String serviceDate, List<Integer> memberIds, String notes) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return new HashMap<>();
            try {
                JsonObject body = new JsonObject();
                body.addProperty("service_id", serviceId);
                body.addProperty("service_date", serviceDate);
                body.addProperty("notes", notes != null ? notes : "");
                JsonArray ids = new JsonArray();
                memberIds.forEach(ids::add);
                body.add("member_ids", ids);
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/attendance/records/quick_mark_attendance/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("QUICK ATTENDANCE [" + resp.code() + "]: " + rb);
                    if (resp.isSuccessful()) {
                        Map<String, Object> result = new HashMap<>();
                        JsonObject obj = JsonParser.parseString(rb).getAsJsonObject();
                        obj.entrySet().forEach(e -> result.put(e.getKey(), parseJsonElement(e.getValue())));
                        return result;
                    }
                }
            } catch (Exception e) { System.err.println("quickMarkAttendance error: " + e.getMessage()); }
            return new HashMap<>();
        });
    }

    /**
     * Get service types for the current church.
     * GET /api/attendance/service-types/
     */
    /**
     * Fetch ChurchService objects for the current user's church.
     * Returns a list of maps with keys: id (int), name (string), start_time, is_active.
     * Used to populate service dropdowns in the Usher dashboard.
     * GET /api/churches/services/
     */
    /**
     * Create a new ChurchService for the current user's church.
     * POST /api/churches/services/
     * Body: { "name", "service_type", "day_of_week", "start_time", "end_time", "location" }
     * Returns the new service ID on success, or -1 on failure.
     */
    public static CompletableFuture<Integer> createChurchService(
            String name, String serviceType, String dayOfWeek,
            String startTime, String endTime, String location) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return -1;
            try {
                JsonObject body = new JsonObject();
                body.addProperty("name",         name.trim());
                body.addProperty("service_type", serviceType.isEmpty() ? "other" : serviceType);
                body.addProperty("day_of_week",  dayOfWeek);
                body.addProperty("start_time",   startTime);   // HH:MM format
                body.addProperty("end_time",     endTime);     // HH:MM format
                body.addProperty("location",     location);
                body.addProperty("is_active",    true);

                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/churches/services/")
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("CREATE SERVICE [" + resp.code() + "]: " + rb);
                    if (resp.isSuccessful()) {
                        JsonObject obj = JsonParser.parseString(rb).getAsJsonObject();
                        if (obj.has("id")) return obj.get("id").getAsInt();
                    }
                }
            } catch (Exception e) {
                System.err.println("createChurchService error: " + e.getMessage());
            }
            return -1;
        });
    }

    public static CompletableFuture<List<Map<String, Object>>> getChurchServices() {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String, Object>>of();
            try {
                String url = BASE_URL + "/api/churches/services/";
                if (churchId != null) url += "?church=" + churchId;
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization", "Bearer " + authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("CHURCH SERVICES [" + resp.code() + "]: " + rb);
                    if (!resp.isSuccessful()) return List.<Map<String, Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) {
                System.err.println("getChurchServices error: " + e.getMessage());
                return List.<Map<String, Object>>of();
            }
        }));
    }

    public static CompletableFuture<List<Map<String, Object>>> getServiceTypes() {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.<Map<String, Object>>of();
            try {
                String url = BASE_URL + "/api/attendance/service-types/";
                if (churchId != null) url += "?church=" + churchId;
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization", "Bearer " + authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("SERVICE TYPES [" + resp.code() + "]: " + rb);
                    if (!resp.isSuccessful()) return List.<Map<String, Object>>of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getServiceTypes error: " + e.getMessage()); return List.<Map<String, Object>>of(); }
        }));
    }

    /**
     * Fetch attendance counts for a given date by querying the records list
     * and computing counts locally.
     * Backend exposes: GET /api/attendance/records/?service_date=YYYY-MM-DD
     * The per-record detail action (attendance_summary) requires a record PK
     * and cannot be used as a global summary endpoint.
     */
    public static CompletableFuture<Map<String, Object>> getAttendanceSummaryForDate(String date) {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return fallbackAttendanceData();
            try {
                String url = BASE_URL + "/api/attendance/records/?service_date=" + date;
                if (churchId != null) url += "&church=" + churchId;
                Request req = new Request.Builder()
                    .url(url).addHeader("Authorization", "Bearer " + authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("ATTENDANCE SUMMARY [" + resp.code() + "]: " + rb);
                    if (!resp.isSuccessful()) return fallbackAttendanceData();

                    // The records list endpoint may return MemberAttendance sub-records
                    // or AttendanceRecord objects — handle both shapes.
                    List<Map<String, Object>> records = parseListFromResponse(rb);

                    long present  = 0, absent = 0, late = 0, visitors = 0;
                    for (Map<String, Object> r : records) {
                        // MemberAttendance shape: { is_present, is_visitor, ... }
                        if (r.containsKey("is_present")) {
                            boolean isPresent = Boolean.TRUE.equals(r.get("is_present"));
                            boolean isVisitor = Boolean.TRUE.equals(r.get("is_visitor"));
                            if (isVisitor)      visitors++;
                            else if (isPresent) present++;
                            else                absent++;
                        } else {
                            // AttendanceRecord shape: { total_attendance, visitors_count, ... }
                            Object ta = r.get("total_attendance");
                            Object vc = r.get("visitors_count");
                            if (ta instanceof Number) present  += ((Number) ta).longValue();
                            if (vc instanceof Number) visitors += ((Number) vc).longValue();
                        }
                    }

                    Map<String, Object> result = new HashMap<>();
                    result.put("present_count",  present);
                    result.put("absent_count",   absent);
                    result.put("late_count",     late);
                    result.put("visitor_count",  visitors);
                    result.put("total_checked_in", present + visitors);
                    result.put("today_attendance", present + visitors);
                    return result;
                }
            } catch (Exception e) {
                System.err.println("getAttendanceSummaryForDate error: " + e.getMessage());
                return fallbackAttendanceData();
            }
        }));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DASHBOARD STATS
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<Map<String,Object>> getDashboardData() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return new HashMap<>();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/dashboard/comprehensive/")
                    .addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("DASHBOARD STATS ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return new HashMap<>();
                    return gson.fromJson(rb, Map.class);
                }
            } catch (Exception e) { System.err.println("getDashboardData error: "+e.getMessage()); return new HashMap<>(); }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CHURCH MANAGEMENT
    // ════════════════════════════════════════════════════════════════════════
    //  CHURCH MANAGEMENT
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<Map<String,Object>> getChurchDetails() {
        // First resolve the church ID, then fetch the specific church record
        return getChurchId().thenCompose(churchId ->
            CompletableFuture.supplyAsync(() -> {
                if (!isAuthenticated()) return new HashMap<String,Object>();
                try {
                    // If we have a specific church ID, use the detail endpoint
                    // so we get THIS church's branding/logo fields reliably.
                    String url = (churchId != null && churchId > 0)
                        ? BASE_URL + "/api/churches/" + churchId + "/"
                        : BASE_URL + "/api/churches/";

                    System.out.println("getChurchDetails → " + url);
                    Request req = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + authToken)
                        .get().build();

                    try (Response resp = client.newCall(req).execute()) {
                        String rb = resp.body() != null ? resp.body().string() : "";
                        System.out.println("CHURCH DETAILS [" + resp.code() + "]: " + rb);
                        if (!resp.isSuccessful()) return new HashMap<String,Object>();

                        JsonObject root = JsonParser.parseString(rb).getAsJsonObject();

                        // Detail endpoint returns the object directly
                        // List endpoint wraps in { "results": [...] }
                        JsonObject church;
                        if (root.has("results") && root.getAsJsonArray("results").size() > 0) {
                            church = root.getAsJsonArray("results").get(0).getAsJsonObject();
                        } else if (root.has("id")) {
                            church = root;
                        } else {
                            return new HashMap<String,Object>();
                        }

                        Map<String,Object> m = new HashMap<>();
                        for (Map.Entry<String, JsonElement> e : church.entrySet())
                            m.put(e.getKey(), parseJsonElement(e.getValue()));

                        // Cache the church ID for subsequent calls
                        if (church.has("id"))
                            cachedChurchId = church.get("id").getAsInt();

                        System.out.println("Church loaded: id=" + m.get("id")
                            + " primary=" + m.get("primary_color")
                            + " logo=" + m.get("logo"));
                        return m;
                    }
                } catch (Exception e) {
                    System.err.println("getChurchDetails error: " + e.getMessage());
                    return new HashMap<String,Object>();
                }
            })
        );
    }

    public static CompletableFuture<Boolean> createChurch(Map<String,Object> churchData) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                JsonObject body = new JsonObject();
                churchData.forEach((k,v) -> { if (v != null && !v.toString().isEmpty()) body.addProperty(k, v.toString()); });
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/churches/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    System.out.println("CREATE CHURCH ["+resp.code()+"]: "+resp.body().string());
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("createChurch error: "+e.getMessage()); return false; }
        });
    }

    public static CompletableFuture<Map<String,Object>> updateChurchSettings(Map<String,Object> settings) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return Map.of("success",false);
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/churches/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .put(RequestBody.create(gson.toJson(settings), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    return Map.of("success", resp.isSuccessful());
                }
            } catch (Exception e) { return Map.of("success",false,"error",e.getMessage()); }
        });
    }

    public static CompletableFuture<Map<String,Object>> updateChurchBranding(int churchId, String primaryColor, String secondaryColor, String accentColor) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return Map.of("success", false, "error", "Not authenticated");
            if (churchId <= 0)      return Map.of("success", false, "error", "Church ID is 0 — church not loaded yet");
            try {
                JsonObject body = new JsonObject();
                if (primaryColor   != null) body.addProperty("primary_color",   primaryColor);
                if (secondaryColor != null) body.addProperty("secondary_color", secondaryColor);
                if (accentColor    != null) body.addProperty("accent_color",    accentColor);

                String url = BASE_URL + "/api/churches/" + churchId + "/branding/";
                System.out.println("BRANDING PATCH → " + url + " body=" + body);

                Request req = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json")
                    .patch(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();

                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("BRANDING [" + resp.code() + "]: " + rb);

                    if (resp.isSuccessful()) {
                        // Parse the full response back so the dialog can show actual new values
                        try {
                            JsonObject root = JsonParser.parseString(rb).getAsJsonObject();
                            Map<String, Object> result = new java.util.HashMap<>();
                            result.put("success", true);
                            if (root.has("data")) {
                                JsonObject data = root.getAsJsonObject("data");
                                for (Map.Entry<String, JsonElement> e : data.entrySet())
                                    result.put(e.getKey(), parseJsonElement(e.getValue()));
                            }
                            return result;
                        } catch (Exception ex) {
                            return Map.of("success", true);
                        }
                    } else {
                        // Extract meaningful error from DRF response
                        String errMsg = rb;
                        try {
                            JsonObject errJson = JsonParser.parseString(rb).getAsJsonObject();
                            if (errJson.has("message")) errMsg = errJson.get("message").getAsString();
                            else if (errJson.has("detail")) errMsg = errJson.get("detail").getAsString();
                            else if (errJson.has("errors")) errMsg = errJson.get("errors").toString();
                        } catch (Exception ignored) {}
                        System.err.println("BRANDING update failed [" + resp.code() + "]: " + errMsg);
                        return Map.of("success", false, "error", "[" + resp.code() + "] " + errMsg);
                    }
                }
            } catch (Exception e) {
                System.err.println("updateChurchBranding error: " + e.getMessage());
                return Map.of("success", false, "error", e.getMessage());
            }
        });
    }

    public static CompletableFuture<Map<String,Object>> uploadChurchLogo(int churchId, java.io.File logoFile) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return Map.of("success", false, "error", "Not authenticated");
            if (churchId <= 0)      return Map.of("success", false, "error", "Church ID is 0 — church not loaded yet");
            try {
                String mimeType = logoFile.getName().toLowerCase().endsWith(".png") ? "image/png"
                    : logoFile.getName().toLowerCase().endsWith(".webp") ? "image/webp"
                    : "image/jpeg";

                MultipartBody.Builder mb = new MultipartBody.Builder().setType(MultipartBody.FORM);
                mb.addFormDataPart("logo", logoFile.getName(),
                    RequestBody.create(logoFile, MediaType.parse(mimeType)));

                String url = BASE_URL + "/api/churches/" + churchId + "/upload-logo/";
                System.out.println("LOGO UPLOAD → " + url);

                Request req = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + authToken)
                    .post(mb.build())
                    .build();

                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("LOGO UPLOAD [" + resp.code() + "]: " + rb);

                    if (resp.isSuccessful()) {
                        try {
                            JsonObject root = JsonParser.parseString(rb).getAsJsonObject();
                            Map<String, Object> result = new java.util.HashMap<>();
                            result.put("success", true);
                            if (root.has("logo_url")) result.put("logo_url", root.get("logo_url").getAsString());
                            if (root.has("data") && root.get("data").isJsonObject()) {
                                JsonObject data = root.getAsJsonObject("data");
                                if (data.has("logo_url")) result.put("logo_url", data.get("logo_url").getAsString());
                            }
                            return result;
                        } catch (Exception ex) {
                            return Map.of("success", true);
                        }
                    } else {
                        String errMsg = rb;
                        try {
                            JsonObject errJson = JsonParser.parseString(rb).getAsJsonObject();
                            if (errJson.has("message")) errMsg = errJson.get("message").getAsString();
                            else if (errJson.has("detail")) errMsg = errJson.get("detail").getAsString();
                        } catch (Exception ignored) {}
                        System.err.println("Logo upload failed [" + resp.code() + "]: " + errMsg);
                        return Map.of("success", false, "error", "[" + resp.code() + "] " + errMsg);
                    }
                }
            } catch (Exception e) {
                System.err.println("uploadChurchLogo error: " + e.getMessage());
                return Map.of("success", false, "error", e.getMessage());
            }
        });
    }

    public static CompletableFuture<Map<String,Object>> exportChurchData() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return Map.of("success",false);
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/churches/export/")
                    .addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    return Map.of("success", resp.isSuccessful(), "data", rb);
                }
            } catch (Exception e) { return Map.of("success",false,"error",e.getMessage()); }
        });
    }

    public static CompletableFuture<Map<String,Object>> importChurchData(java.io.File dataFile) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return Map.of("success",false);
            try {
                String content = new String(java.nio.file.Files.readAllBytes(dataFile.toPath()));
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/churches/import/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(content, MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    return Map.of("success", resp.isSuccessful());
                }
            } catch (Exception e) { return Map.of("success",false,"error",e.getMessage()); }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SYSTEM ADMIN — dedicated endpoints (financials, users, giving)
    // ════════════════════════════════════════════════════════════════════════

    public static CompletableFuture<Map<String,Object>> getSystemFinancials() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return new HashMap<>();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/system/financials/")
                    .addHeader("Authorization","Bearer " + authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("SYSTEM_FINANCIALS [" + resp.code() + "]: " + rb.substring(0, Math.min(200, rb.length())));
                    if (!resp.isSuccessful()) return new HashMap<>();
                    JsonObject root = JsonParser.parseString(rb).getAsJsonObject();
                    JsonObject data = root.has("data") ? root.getAsJsonObject("data") : root;
                    Map<String,Object> m = new HashMap<>();
                    for (Map.Entry<String,JsonElement> e : data.entrySet())
                        m.put(e.getKey(), parseJsonElement(e.getValue()));
                    return m;
                }
            } catch (Exception e) { System.err.println("getSystemFinancials: " + e.getMessage()); return new HashMap<>(); }
        });
    }

    public static CompletableFuture<Map<String,Object>> getSystemUsers(String search, String role, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return new HashMap<>();
            try {
                StringBuilder url = new StringBuilder(BASE_URL + "/api/system/users/?page_size=" + pageSize);
                if (search != null && !search.isEmpty()) url.append("&search=").append(java.net.URLEncoder.encode(search, "UTF-8"));
                if (role   != null && !role.isEmpty())   url.append("&role=").append(role);
                Request req = new Request.Builder()
                    .url(url.toString())
                    .addHeader("Authorization","Bearer " + authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("SYSTEM_USERS [" + resp.code() + "]");
                    if (!resp.isSuccessful()) return new HashMap<>();
                    JsonObject root = JsonParser.parseString(rb).getAsJsonObject();
                    Map<String,Object> m = new HashMap<>();
                    for (Map.Entry<String,JsonElement> e : root.entrySet())
                        m.put(e.getKey(), parseJsonElement(e.getValue()));
                    return m;
                }
            } catch (Exception e) { System.err.println("getSystemUsers: " + e.getMessage()); return new HashMap<>(); }
        });
    }

    public static CompletableFuture<Map<String,Object>> getSystemGiving(String churchId, String txStatus, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return new HashMap<>();
            try {
                StringBuilder url = new StringBuilder(BASE_URL + "/api/system/giving/?page_size=" + pageSize);
                if (churchId != null && !churchId.isEmpty()) url.append("&church=").append(churchId);
                if (txStatus != null && !txStatus.isEmpty()) url.append("&status=").append(txStatus);
                Request req = new Request.Builder()
                    .url(url.toString())
                    .addHeader("Authorization","Bearer " + authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("SYSTEM_GIVING [" + resp.code() + "]");
                    if (!resp.isSuccessful()) return new HashMap<>();
                    JsonObject root = JsonParser.parseString(rb).getAsJsonObject();
                    Map<String,Object> m = new HashMap<>();
                    for (Map.Entry<String,JsonElement> e : root.entrySet())
                        m.put(e.getKey(), parseJsonElement(e.getValue()));
                    return m;
                }
            } catch (Exception e) { System.err.println("getSystemGiving: " + e.getMessage()); return new HashMap<>(); }
        });
    }
    //                  approveChurch, rejectChurch, getAllUsers, getAuditLogs
    // ════════════════════════════════════════════════════════════════════════

    public static CompletableFuture<Map<String,Object>> getSystemOverview() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return new HashMap<>();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/reports/system-overview/")
                    .addHeader("Authorization","Bearer " + authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("SYSTEM_OVERVIEW [" + resp.code() + "]: " + rb);
                    if (!resp.isSuccessful()) return new HashMap<>();
                    JsonObject root = JsonParser.parseString(rb).getAsJsonObject();
                    JsonObject data = root.has("data") ? root.getAsJsonObject("data") : root;
                    Map<String,Object> m = new HashMap<>();
                    for (Map.Entry<String,JsonElement> e : data.entrySet())
                        m.put(e.getKey(), parseJsonElement(e.getValue()));
                    return m;
                }
            } catch (Exception e) { System.err.println("getSystemOverview: "+e.getMessage()); return new HashMap<>(); }
        });
    }

    public static CompletableFuture<List<Map<String,Object>>> getPendingChurches() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.of();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/churches/pending-approval/")
                    .addHeader("Authorization","Bearer " + authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("PENDING_CHURCHES [" + resp.code() + "]: " + rb);
                    if (!resp.isSuccessful()) return List.of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getPendingChurches: "+e.getMessage()); return List.of(); }
        });
    }

    public static CompletableFuture<List<Map<String,Object>>> getAllChurches() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.of();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/churches/?page_size=500")
                    .addHeader("Authorization","Bearer " + authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    if (!resp.isSuccessful()) return List.of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getAllChurches: "+e.getMessage()); return List.of(); }
        });
    }

    public static CompletableFuture<Boolean> approveChurch(int churchId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/churches/" + churchId + "/approve/")
                    .addHeader("Authorization","Bearer " + authToken)
                    .post(RequestBody.create("{}", MediaType.parse("application/json"))).build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("APPROVE_CHURCH [" + resp.code() + "]: " + rb);
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("approveChurch: "+e.getMessage()); return false; }
        });
    }

    public static CompletableFuture<Boolean> rejectChurch(int churchId, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                JsonObject body = new JsonObject();
                body.addProperty("reason", reason);
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/churches/" + churchId + "/reject/")
                    .addHeader("Authorization","Bearer " + authToken)
                    .addHeader("Content-Type","application/json")
                    .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json"))).build();
                try (Response resp = client.newCall(req).execute()) {
                    System.out.println("REJECT_CHURCH [" + resp.code() + "]: " + resp.body().string());
                    return resp.isSuccessful();
                }
            } catch (Exception e) { System.err.println("rejectChurch: "+e.getMessage()); return false; }
        });
    }

    public static CompletableFuture<List<Map<String,Object>>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.of();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/accounts/users/?page_size=500")
                    .addHeader("Authorization","Bearer " + authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    if (!resp.isSuccessful()) return List.of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getAllUsers: "+e.getMessage()); return List.of(); }
        });
    }

    public static CompletableFuture<List<Map<String,Object>>> getAuditLogs(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return List.of();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL + "/api/audit/logs/?page_size=" + limit)
                    .addHeader("Authorization","Bearer " + authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("AUDIT_LOGS [" + resp.code() + "]");
                    if (!resp.isSuccessful()) return List.of();
                    return parseListFromResponse(rb);
                }
            } catch (Exception e) { System.err.println("getAuditLogs: "+e.getMessage()); return List.of(); }
        });
    }
    // ════════════════════════════════════════════════════════════════════════
    private static List<Map<String,Object>> parseListFromResponse(String responseBody) {
        try {
            JsonElement root = JsonParser.parseString(responseBody);
            if (root.isJsonArray()) return parseJsonArray(root.getAsJsonArray());
            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("results")) return parseJsonArray(obj.getAsJsonArray("results"));
                if (obj.has("data") && obj.get("data").isJsonArray()) return parseJsonArray(obj.getAsJsonArray("data"));
            }
        } catch (Exception e) { System.err.println("parseListFromResponse error: "+e.getMessage()); }
        return new ArrayList<>();
    }

    private static List<Map<String,Object>> parseJsonArray(JsonArray arr) {
        List<Map<String,Object>> list = new ArrayList<>();
        for (JsonElement el : arr) {
            if (el.isJsonObject()) {
                Map<String,Object> m = new HashMap<>();
                for (Map.Entry<String,JsonElement> entry : el.getAsJsonObject().entrySet())
                    m.put(entry.getKey(), parseJsonElement(entry.getValue()));
                list.add(m);
            }
        }
        return list;
    }

    private static Object parseJsonElement(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isNumber())  return p.getAsNumber();
            return p.getAsString();
        }
        if (el.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement item : el.getAsJsonArray()) list.add(parseJsonElement(item));
            return list;
        }
        if (el.isJsonObject()) {
            Map<String,Object> m = new HashMap<>();
            for (Map.Entry<String,JsonElement> e : el.getAsJsonObject().entrySet()) m.put(e.getKey(), parseJsonElement(e.getValue()));
            return m;
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ROLE MAPPING
    // ════════════════════════════════════════════════════════════════════════
    private static String mapRoleToBackend(String frontendRole) {
        if (frontendRole == null) return "staff";
        switch (frontendRole.toLowerCase()) {
            case "pastor": case "senior pastor": case "associate pastor": case "youth pastor": return "pastor";
            case "treasurer": case "financial secretary": return "treasurer";
            case "usher": return "usher";
            case "admin": case "church administrator": return "church_admin";
            default: return "staff";
        }
    }
}