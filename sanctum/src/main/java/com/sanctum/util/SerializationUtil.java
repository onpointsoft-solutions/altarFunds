package com.sanctum.util;

import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for Java serialization operations
 */
public class SerializationUtil {
    private static final Logger logger = Logger.getLogger(SerializationUtil.class.getName());
    
    /**
     * Serialize object to file
     */
    public static <T extends Serializable> boolean serialize(T object, String filePath) {
        try (FileOutputStream fileOut = new FileOutputStream(filePath);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            
            out.writeObject(object);
            logger.info("Object serialized successfully to: " + filePath);
            return true;
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error serializing object to: " + filePath, e);
            return false;
        }
    }
    
    /**
     * Deserialize object from file
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserialize(String filePath, Class<T> clazz) {
        File file = new File(filePath);
        if (!file.exists()) {
            logger.warning("Serialization file does not exist: " + filePath);
            return null;
        }
        
        try (FileInputStream fileIn = new FileInputStream(filePath);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            
            Object obj = in.readObject();
            
            if (clazz.isInstance(obj)) {
                logger.info("Object deserialized successfully from: " + filePath);
                return (T) obj;
            } else {
                logger.warning("Deserialized object is not of expected type: " + clazz.getName());
                return null;
            }
            
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Error deserializing object from: " + filePath, e);
            return null;
        }
    }
    
    /**
     * Serialize list of objects to file
     */
    public static <T extends Serializable> boolean serializeList(List<T> objects, String filePath) {
        return serialize((Serializable) objects, filePath);
    }
    
    /**
     * Deserialize list of objects from file
     */
    public static <T extends Serializable> List<T> deserializeList(String filePath, Class<T> clazz) {
        Object obj = deserialize(filePath, Serializable.class);
        if (obj == null) {
            return null;
        }
        
        if (!(obj instanceof List)) {
            logger.warning("Deserialized object is not a List");
            return null;
        }
        
        List<?> list = (List<?>) obj;
        
        // Validate all elements are of correct type
        for (Object element : list) {
            if (!clazz.isInstance(element)) {
                logger.warning("List contains object of wrong type: " + element.getClass().getName());
                return null;
            }
        }
        
        return (List<T>) list;
    }
    
    /**
     * Check if serialization file exists and is readable
     */
    public static boolean fileExists(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.canRead();
    }
    
    /**
     * Delete serialization file
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        boolean deleted = file.delete();
        if (deleted) {
            logger.info("Serialization file deleted: " + filePath);
        } else {
            logger.warning("Failed to delete serialization file: " + filePath);
        }
        return deleted;
    }
    
    /**
     * Get file size in bytes
     */
    public static long getFileSize(String filePath) {
        File file = new File(filePath);
        return file.exists() ? file.length() : 0;
    }
    
    /**
     * Ensure directory exists for file path
     */
    public static boolean ensureDirectoryExists(String filePath) {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                logger.info("Directory created: " + parentDir.getAbsolutePath());
            }
            return created;
        }
        
        return true;
    }
    
    /**
     * Backup serialization file
     */
    public static boolean backupFile(String originalPath, String backupPath) {
        File original = new File(originalPath);
        if (!original.exists()) {
            return false;
        }
        
        try (FileInputStream in = new FileInputStream(original);
             FileOutputStream out = new FileOutputStream(backupPath)) {
            
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            
            logger.info("Serialization file backed up to: " + backupPath);
            return true;
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error backing up serialization file", e);
            return false;
        }
    }
    
    /**
     * Get last modified time of file
     */
    public static long getLastModified(String filePath) {
        File file = new File(filePath);
        return file.exists() ? file.lastModified() : 0;
    }
}
