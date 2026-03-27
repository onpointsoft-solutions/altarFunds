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
                    if (!resp.isSuccessful()) return List.<Map<String,Object>>of();

                    JsonElement root = JsonParser.parseString(rb);
                    if (root.isJsonObject()) {
                        JsonObject obj = root.getAsJsonObject();
                        // Format: {"success":true,"data":{"recent_givings":[...]}}
                        if (obj.has("success") && obj.get("success").getAsBoolean() && obj.has("data")) {
                            JsonObject data = obj.getAsJsonObject("data");
                            if (data.has("recent_givings") && data.get("recent_givings").isJsonArray()) {
                                return parseJsonArray(data.getAsJsonArray("recent_givings"));
                            }
                        }
                        // Format: {"results":[...]}
                        if (obj.has("results")) return parseJsonArray(obj.getAsJsonArray("results"));
                        // Format: {"data":[...]}
                        if (obj.has("data") && obj.get("data").isJsonArray()) return parseJsonArray(obj.getAsJsonArray("data"));
                    } else if (root.isJsonArray()) {
                        return parseJsonArray(root.getAsJsonArray());
                    }
                    return List.<Map<String,Object>>of();
                }
            } catch (Exception e) { System.err.println("getGivingTransactions error: "+e.getMessage()); return List.<Map<String,Object>>of(); }
        }));
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
                JsonObject body = new JsonObject();
                body.addProperty("title", title); body.addProperty("content", content);
                body.addProperty("priority", priority); body.addProperty("is_active", true);
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

    // ════════════════════════════════════════════════════════════════════════
    //  ATTENDANCE
    // ════════════════════════════════════════════════════════════════════════
    public static CompletableFuture<Map<String,Object>> getAttendanceData() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return fallbackAttendanceData();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/attendance/summary/")
                    .addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    System.out.println("ATTENDANCE ["+resp.code()+"]: "+rb);
                    if (!resp.isSuccessful()) return fallbackAttendanceData();
                    JsonObject root = JsonParser.parseString(rb).getAsJsonObject();
                    Map<String,Object> result = new HashMap<>();
                    for (Map.Entry<String,JsonElement> e : root.entrySet()) result.put(e.getKey(), parseJsonElement(e.getValue()));
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

    public static CompletableFuture<Boolean> checkInMember(String memberId, String serviceType) {
        return getChurchId().thenCompose(churchId -> CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return false;
            try {
                JsonObject body = new JsonObject();
                body.addProperty("member", memberId);
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
    public static CompletableFuture<Map<String,Object>> getChurchDetails() {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return new HashMap<>();
            try {
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/churches/")
                    .addHeader("Authorization","Bearer "+authToken).get().build();
                try (Response resp = client.newCall(req).execute()) {
                    String rb = resp.body() != null ? resp.body().string() : "";
                    if (!resp.isSuccessful()) return new HashMap<>();
                    JsonObject root = JsonParser.parseString(rb).getAsJsonObject();
                    if (root.has("results") && root.getAsJsonArray("results").size()>0) {
                        JsonObject ch = root.getAsJsonArray("results").get(0).getAsJsonObject();
                        Map<String,Object> m = new HashMap<>();
                        for (Map.Entry<String,JsonElement> e : ch.entrySet()) m.put(e.getKey(), parseJsonElement(e.getValue()));
                        return m;
                    }
                    return new HashMap<>();
                }
            } catch (Exception e) { System.err.println("getChurchDetails error: "+e.getMessage()); return new HashMap<>(); }
        });
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

    public static CompletableFuture<Map<String,Object>> uploadChurchLogo(int churchId, java.io.File logoFile) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return Map.of("success",false);
            try {
                MultipartBody.Builder mb = new MultipartBody.Builder().setType(MultipartBody.FORM);
                mb.addFormDataPart("logo", logoFile.getName(),
                    RequestBody.create(logoFile, MediaType.parse("image/*")));
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/churches/"+churchId+"/upload-logo/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .post(mb.build()).build();
                try (Response resp = client.newCall(req).execute()) {
                    return Map.of("success", resp.isSuccessful());
                }
            } catch (Exception e) { return Map.of("success",false,"error",e.getMessage()); }
        });
    }

    public static CompletableFuture<Map<String,Object>> updateChurchBranding(int churchId, String primaryColor, String secondaryColor, String accentColor) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAuthenticated()) return Map.of("success",false);
            try {
                JsonObject body = new JsonObject();
                if (primaryColor   != null) body.addProperty("primary_color",   primaryColor);
                if (secondaryColor != null) body.addProperty("secondary_color", secondaryColor);
                if (accentColor    != null) body.addProperty("accent_color",    accentColor);
                Request req = new Request.Builder()
                    .url(BASE_URL+"/api/churches/"+churchId+"/branding/")
                    .addHeader("Authorization","Bearer "+authToken)
                    .addHeader("Content-Type","application/json")
                    .patch(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
                    .build();
                try (Response resp = client.newCall(req).execute()) {
                    return Map.of("success", resp.isSuccessful());
                }
            } catch (Exception e) { return Map.of("success",false,"error",e.getMessage()); }
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
    //  PARSE HELPERS
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