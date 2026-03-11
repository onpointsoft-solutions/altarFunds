package com.sanctum.util;

import com.sanctum.model.*;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cache manager for offline storage and synchronization
 */
public class CacheManager {
    private static final Logger logger = Logger.getLogger(CacheManager.class.getName());
    
    private static final String CACHE_DIR = "cache";
    private static final String MEMBERS_CACHE = CACHE_DIR + "/members.ser";
    private static final String DONATIONS_CACHE = CACHE_DIR + "/donations.ser";
    private static final String EXPENSES_CACHE = CACHE_DIR + "/expenses.ser";
    private static final String EVENTS_CACHE = CACHE_DIR + "/events.ser";
    private static final String FUNDS_CACHE = CACHE_DIR + "/funds.ser";
    
    // In-memory cache
    private static final ConcurrentHashMap<String, Object> memoryCache = new ConcurrentHashMap<>();
    
    // Scheduled executor for cache cleanup
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    static {
        // Ensure cache directory exists
        SerializationUtil.ensureDirectoryExists(MEMBERS_CACHE);
        
        // Schedule periodic cache cleanup (every hour)
        scheduler.scheduleAtFixedRate(CacheManager::cleanupExpiredCache, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * Cache members for offline use
     */
    public static boolean cacheMembers(List<Member> members) {
        memoryCache.put("members", new ArrayList<>(members));
        return SerializationUtil.serializeList(members, MEMBERS_CACHE);
    }
    
    /**
     * Get cached members
     */
    public static List<Member> getCachedMembers() {
        // Try memory cache first
        @SuppressWarnings("unchecked")
        List<Member> memoryMembers = (List<Member>) memoryCache.get("members");
        if (memoryMembers != null) {
            return new ArrayList<>(memoryMembers);
        }
        
        // Try file cache
        List<Member> fileMembers = SerializationUtil.deserializeList(MEMBERS_CACHE, Member.class);
        if (fileMembers != null) {
            memoryCache.put("members", new ArrayList<>(fileMembers));
            return new ArrayList<>(fileMembers);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Cache donations for offline use
     */
    public static boolean cacheDonations(List<Donation> donations) {
        memoryCache.put("donations", new ArrayList<>(donations));
        return SerializationUtil.serializeList(donations, DONATIONS_CACHE);
    }
    
    /**
     * Get cached donations
     */
    public static List<Donation> getCachedDonations() {
        @SuppressWarnings("unchecked")
        List<Donation> memoryDonations = (List<Donation>) memoryCache.get("donations");
        if (memoryDonations != null) {
            return new ArrayList<>(memoryDonations);
        }
        
        List<Donation> fileDonations = SerializationUtil.deserializeList(DONATIONS_CACHE, Donation.class);
        if (fileDonations != null) {
            memoryCache.put("donations", new ArrayList<>(fileDonations));
            return new ArrayList<>(fileDonations);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Cache expenses for offline use
     */
    public static boolean cacheExpenses(List<Expense> expenses) {
        memoryCache.put("expenses", new ArrayList<>(expenses));
        return SerializationUtil.serializeList(expenses, EXPENSES_CACHE);
    }
    
    /**
     * Get cached expenses
     */
    public static List<Expense> getCachedExpenses() {
        @SuppressWarnings("unchecked")
        List<Expense> memoryExpenses = (List<Expense>) memoryCache.get("expenses");
        if (memoryExpenses != null) {
            return new ArrayList<>(memoryExpenses);
        }
        
        List<Expense> fileExpenses = SerializationUtil.deserializeList(EXPENSES_CACHE, Expense.class);
        if (fileExpenses != null) {
            memoryCache.put("expenses", new ArrayList<>(fileExpenses));
            return new ArrayList<>(fileExpenses);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Cache events for offline use
     */
    public static boolean cacheEvents(List<Event> events) {
        memoryCache.put("events", new ArrayList<>(events));
        return SerializationUtil.serializeList(events, EVENTS_CACHE);
    }
    
    /**
     * Get cached events
     */
    public static List<Event> getCachedEvents() {
        @SuppressWarnings("unchecked")
        List<Event> memoryEvents = (List<Event>) memoryCache.get("events");
        if (memoryEvents != null) {
            return new ArrayList<>(memoryEvents);
        }
        
        List<Event> fileEvents = SerializationUtil.deserializeList(EVENTS_CACHE, Event.class);
        if (fileEvents != null) {
            memoryCache.put("events", new ArrayList<>(fileEvents));
            return new ArrayList<>(fileEvents);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Cache funds for offline use
     */
    public static boolean cacheFunds(List<Fund> funds) {
        memoryCache.put("funds", new ArrayList<>(funds));
        return SerializationUtil.serializeList(funds, FUNDS_CACHE);
    }
    
    /**
     * Get cached funds
     */
    public static List<Fund> getCachedFunds() {
        @SuppressWarnings("unchecked")
        List<Fund> memoryFunds = (List<Fund>) memoryCache.get("funds");
        if (memoryFunds != null) {
            return new ArrayList<>(memoryFunds);
        }
        
        List<Fund> fileFunds = SerializationUtil.deserializeList(FUNDS_CACHE, Fund.class);
        if (fileFunds != null) {
            memoryCache.put("funds", new ArrayList<>(fileFunds));
            return new ArrayList<>(fileFunds);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Add single item to cache
     */
    public static <T> void addToCache(String cacheKey, T item) {
        @SuppressWarnings("unchecked")
        List<T> cache = (List<T>) memoryCache.get(cacheKey);
        if (cache == null) {
            cache = new ArrayList<>();
            memoryCache.put(cacheKey, cache);
        }
        cache.add(item);
    }
    
    /**
     * Remove item from cache
     */
    public static <T> boolean removeFromCache(String cacheKey, T item) {
        @SuppressWarnings("unchecked")
        List<T> cache = (List<T>) memoryCache.get(cacheKey);
        if (cache != null) {
            boolean removed = cache.remove(item);
            if (removed) {
                // Persist changes to file
                persistCacheToFile(cacheKey);
            }
            return removed;
        }
        return false;
    }
    
    /**
     * Clear all cache
     */
    public static void clearAllCache() {
        memoryCache.clear();
        
        // Delete cache files
        SerializationUtil.deleteFile(MEMBERS_CACHE);
        SerializationUtil.deleteFile(DONATIONS_CACHE);
        SerializationUtil.deleteFile(EXPENSES_CACHE);
        SerializationUtil.deleteFile(EVENTS_CACHE);
        SerializationUtil.deleteFile(FUNDS_CACHE);
        
        logger.info("All cache cleared");
    }
    
    /**
     * Clear specific cache
     */
    public static void clearCache(String cacheKey) {
        memoryCache.remove(cacheKey);
        
        String filePath = getCacheFilePath(cacheKey);
        if (filePath != null) {
            SerializationUtil.deleteFile(filePath);
        }
        
        logger.info("Cache cleared: " + cacheKey);
    }
    
    /**
     * Get cache statistics
     */
    public static CacheStats getCacheStats() {
        long totalSize = 0;
        int fileCount = 0;
        
        String[] cacheFiles = {MEMBERS_CACHE, DONATIONS_CACHE, EXPENSES_CACHE, EVENTS_CACHE, FUNDS_CACHE};
        
        for (String filePath : cacheFiles) {
            long size = SerializationUtil.getFileSize(filePath);
            if (size > 0) {
                totalSize += size;
                fileCount++;
            }
        }
        
        return new CacheStats(memoryCache.size(), fileCount, totalSize);
    }
    
    /**
     * Backup all cache files
     */
    public static boolean backupCache(String backupDir) {
        boolean success = true;
        String timestamp = LocalDateTime.now().toString().replace(":", "-");
        
        String[] cacheFiles = {
            MEMBERS_CACHE, DONATIONS_CACHE, EXPENSES_CACHE, EVENTS_CACHE, FUNDS_CACHE
        };
        
        for (String filePath : cacheFiles) {
            if (SerializationUtil.fileExists(filePath)) {
                String fileName = new File(filePath).getName();
                String backupPath = backupDir + "/" + timestamp + "_" + fileName;
                success &= SerializationUtil.backupFile(filePath, backupPath);
            }
        }
        
        return success;
    }
    
    /**
     * Persist memory cache to file
     */
    private static <T> void persistCacheToFile(String cacheKey) {
        @SuppressWarnings("unchecked")
        List<T> cache = (List<T>) memoryCache.get(cacheKey);
        if (cache != null) {
            String filePath = getCacheFilePath(cacheKey);
            if (filePath != null) {
                SerializationUtil.serializeList((List<Serializable>) cache, filePath);
            }
        }
    }
    
    /**
     * Get file path for cache key
     */
    private static String getCacheFilePath(String cacheKey) {
        switch (cacheKey) {
            case "members": return MEMBERS_CACHE;
            case "donations": return DONATIONS_CACHE;
            case "expenses": return EXPENSES_CACHE;
            case "events": return EVENTS_CACHE;
            case "funds": return FUNDS_CACHE;
            default: return null;
        }
    }
    
    /**
     * Cleanup expired cache entries
     */
    private static void cleanupExpiredCache() {
        // This could be enhanced to check timestamps and remove old entries
        logger.info("Cache cleanup completed");
    }
    
    /**
     * Shutdown cache manager
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Cache manager shutdown");
    }
    
    /**
     * Cache statistics class
     */
    public static class CacheStats {
        private final int memoryCacheSize;
        private final int fileCount;
        private final long totalFileSize;
        
        public CacheStats(int memoryCacheSize, int fileCount, long totalFileSize) {
            this.memoryCacheSize = memoryCacheSize;
            this.fileCount = fileCount;
            this.totalFileSize = totalFileSize;
        }
        
        public int getMemoryCacheSize() { return memoryCacheSize; }
        public int getFileCount() { return fileCount; }
        public long getTotalFileSize() { return totalFileSize; }
        
        public String getFormattedFileSize() {
            if (totalFileSize < 1024) {
                return totalFileSize + " B";
            } else if (totalFileSize < 1024 * 1024) {
                return String.format("%.1f KB", totalFileSize / 1024.0);
            } else {
                return String.format("%.1f MB", totalFileSize / (1024.0 * 1024));
            }
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{memory=%d, files=%d, size=%s}", 
                memoryCacheSize, fileCount, getFormattedFileSize());
        }
    }
}
