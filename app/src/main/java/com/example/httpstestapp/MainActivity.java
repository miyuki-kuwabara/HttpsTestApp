package com.example.httpstestapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


public class MainActivity extends AppCompatActivity {

    private final ExecutorService _executorService;

    public MainActivity() {
        _executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id._buttonSubmit).setOnClickListener(view -> {
            final Handler handler = new Handler();

            OkHttpClient httpClient = createOkHttpClient();
            Request request = new Request.Builder()
                    .url("https://192.168.0.2:3000/")
                    .build();
            _executorService.execute(() -> httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    handler.post(() -> Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    handler.post(()->Toast.makeText(MainActivity.this, response.toString(), Toast.LENGTH_LONG).show());
                }
            }));
        });
    }

    @NonNull
    private OkHttpClient createOkHttpClient() {
        try {
            final KeyStore caStore = KeyStore.getInstance("AndroidCAStore");
            caStore.load(null);
            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(caStore);
            final X509TrustManager defaultTrustManager = Arrays.stream(trustManagerFactory.getTrustManagers())
                    .filter(trustManager -> trustManager instanceof X509TrustManager)
                    .map(trustManager -> (X509TrustManager) trustManager)
                    .findFirst()
                    .orElse(null);
            X509TrustManager silentTrustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificate, String s) {
                    if (defaultTrustManager != null) {
                        try {
                            defaultTrustManager.checkClientTrusted(x509Certificate, s);
                        } catch (CertificateException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificate, String s) {
                    if (defaultTrustManager != null) {
                        try {
                            defaultTrustManager.checkServerTrusted(x509Certificate, s);
                        } catch (CertificateException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{silentTrustManager}, new SecureRandom());

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), silentTrustManager)
                    .build();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        return new OkHttpClient.Builder().build();
    }


}