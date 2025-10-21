package com.lythe.media.im.net;

import android.content.Context;
import android.util.Log;

import com.lythe.media.R;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class SSLUtils {
    private static final String TAG = "SSLUtils";

    // 创建信任自定义证书的OkHttpClient
    public static OkHttpClient getSSLOkHttpClient(Context context) {
        try {
            // 加载证书
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            InputStream inputStream = context.getResources().openRawResource(R.raw.lythe);

            // 创建密钥库
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
//            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null);
            keyStore.setCertificateEntry("custom_cert", certificateFactory.generateCertificate(inputStream));

            if(inputStream != null) {
                inputStream.close();
            }

            // 创建信任管理器
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            // 创建SSL上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new java.security.SecureRandom());

            // 创建OkHttpClient并配置SSL
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(),
                            (X509TrustManager) trustManagerFactory.getTrustManagers()[0])
                    .build();
        } catch (CertificateException | IOException | KeyStoreException |
                 NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(TAG, "SSL配置失败: " + e.getMessage());
            e.printStackTrace();
        }
        return new OkHttpClient();
    }

    public static OkHttpClient getUnsafeOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] {
                   new X509TrustManager() {
                       @Override
                       public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                       }

                       @Override
                       public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                       }

                       @Override
                       public X509Certificate[] getAcceptedIssuers() {
                           return new X509Certificate[0];
                       }
                   }
            } ;
            // 安装信任管理器
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            //创建SSL套接字工厂
            final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier(((hostname, session) -> true));

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
