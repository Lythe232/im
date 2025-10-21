package com.lythe.media.im.messager.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * 安全存储管理器
 * 功能：
 * 1. 使用Android Keystore加密敏感数据
 * 2. 安全的SharedPreferences存储
 * 3. 数据完整性验证
 * 4. 密钥管理
 */
public class SecureStorage {
    private static final String TAG = "SecureStorage";
    private static final String KEYSTORE_ALIAS = "IM_SECURE_KEY";
    private static final String PREFS_NAME = "secure_prefs";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    private static volatile SecureStorage instance;
    private final Context context;
    private final SharedPreferences securePrefs;
    private final KeyStore keyStore;
    
    private SecureStorage(Context context) {
        this.context = context.getApplicationContext();
        this.securePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.keyStore = initKeyStore();
    }
    
    public static SecureStorage getInstance(Context context) {
        if (instance == null) {
            synchronized (SecureStorage.class) {
                if (instance == null) {
                    instance = new SecureStorage(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化KeyStore
     */
    private KeyStore initKeyStore() {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            return ks;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize KeyStore", e);
            return null;
        }
    }
    
    /**
     * 生成或获取密钥
     */
    private SecretKey getOrCreateSecretKey() throws Exception {
        if (keyStore == null) {
            throw new IllegalStateException("KeyStore not initialized");
        }
        
        SecretKey secretKey = (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
        if (secretKey == null) {
            // 生成新密钥
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .setRandomizedEncryptionRequired(true)
                    .build();
            
            keyGenerator.init(keyGenParameterSpec);
            secretKey = keyGenerator.generateKey();
        }
        
        return secretKey;
    }
    
    /**
     * 加密数据
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        
        try {
            SecretKey secretKey = getOrCreateSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
            
            // 将IV和密文组合
            byte[] encryptedData = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, encryptedData, GCM_IV_LENGTH, ciphertext.length);
            
            return Base64.encodeToString(encryptedData, Base64.NO_WRAP);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to encrypt data", e);
            return null;
        }
    }
    
    /**
     * 解密数据
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }
        
        try {
            byte[] encryptedBytes = Base64.decode(encryptedData, Base64.NO_WRAP);
            
            if (encryptedBytes.length < GCM_IV_LENGTH) {
                Log.e(TAG, "Invalid encrypted data length");
                return null;
            }
            
            // 提取IV和密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[encryptedBytes.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedBytes, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedBytes, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            
            SecretKey secretKey = getOrCreateSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, "UTF-8");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to decrypt data", e);
            return null;
        }
    }
    
    /**
     * 安全存储字符串
     */
    public boolean putSecureString(String key, String value) {
        try {
            String encryptedValue = encrypt(value);
            if (encryptedValue != null) {
                securePrefs.edit().putString(key, encryptedValue).apply();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to store secure string", e);
        }
        return false;
    }
    
    /**
     * 安全获取字符串
     */
    public String getSecureString(String key, String defaultValue) {
        try {
            String encryptedValue = securePrefs.getString(key, null);
            if (encryptedValue != null) {
                String decryptedValue = decrypt(encryptedValue);
                return decryptedValue != null ? decryptedValue : defaultValue;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve secure string", e);
        }
        return defaultValue;
    }
    
    /**
     * 安全存储整数
     */
    public boolean putSecureInt(String key, int value) {
        return putSecureString(key, String.valueOf(value));
    }
    
    /**
     * 安全获取整数
     */
    public int getSecureInt(String key, int defaultValue) {
        try {
            String value = getSecureString(key, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse secure int", e);
            return defaultValue;
        }
    }
    
    /**
     * 安全存储长整数
     */
    public boolean putSecureLong(String key, long value) {
        return putSecureString(key, String.valueOf(value));
    }
    
    /**
     * 安全获取长整数
     */
    public long getSecureLong(String key, long defaultValue) {
        try {
            String value = getSecureString(key, String.valueOf(defaultValue));
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse secure long", e);
            return defaultValue;
        }
    }
    
    /**
     * 安全存储布尔值
     */
    public boolean putSecureBoolean(String key, boolean value) {
        return putSecureString(key, String.valueOf(value));
    }
    
    /**
     * 安全获取布尔值
     */
    public boolean getSecureBoolean(String key, boolean defaultValue) {
        String value = getSecureString(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 删除安全存储的数据
     */
    public boolean removeSecureData(String key) {
        try {
            securePrefs.edit().remove(key).apply();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove secure data", e);
            return false;
        }
    }
    
    /**
     * 清空所有安全存储的数据
     */
    public boolean clearAllSecureData() {
        try {
            securePrefs.edit().clear().apply();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear secure data", e);
            return false;
        }
    }
    
    /**
     * 检查密钥是否存在
     */
    public boolean isKeyAvailable() {
        try {
            return keyStore != null && keyStore.containsAlias(KEYSTORE_ALIAS);
        } catch (Exception e) {
            Log.e(TAG, "Failed to check key availability", e);
            return false;
        }
    }
    
    /**
     * 删除密钥
     */
    public boolean deleteKey() {
        try {
            if (keyStore != null && keyStore.containsAlias(KEYSTORE_ALIAS)) {
                keyStore.deleteEntry(KEYSTORE_ALIAS);
                Log.d(TAG, "Key deleted successfully");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete key", e);
        }
        return false;
    }
    
    /**
     * 验证数据完整性
     */
    public boolean verifyDataIntegrity(String key, String expectedValue) {
        try {
            String storedValue = getSecureString(key, null);
            return expectedValue != null && expectedValue.equals(storedValue);
        } catch (Exception e) {
            Log.e(TAG, "Failed to verify data integrity", e);
            return false;
        }
    }
    
    /**
     * 获取存储统计信息
     */
    public StorageStats getStorageStats() {
        return new StorageStats(
            securePrefs.getAll().size(),
            isKeyAvailable()
        );
    }
    
    /**
     * 释放资源
     */
    public void release() {
        instance = null;
    }
    
    /**
     * 存储统计信息
     */
    public static class StorageStats {
        public final int entryCount;
        public final boolean keyAvailable;
        
        public StorageStats(int entryCount, boolean keyAvailable) {
            this.entryCount = entryCount;
            this.keyAvailable = keyAvailable;
        }
    }
}
