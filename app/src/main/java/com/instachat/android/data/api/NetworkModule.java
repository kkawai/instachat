package com.instachat.android.data.api;

import android.content.Context;

import com.instachat.android.BuildConfig;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.inject.Singleton;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
public class NetworkModule {

    @Provides
    @Singleton
    OkHttpClient provideOkhttpClient(Context context) {
        return createClient(context);
    }

    @Provides
    @Singleton
    Retrofit provideRetrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.DEBUG ? Constants.API_BASE_URL : Constants.API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client).build();
    }

    @Provides
    @Singleton
    APIInterface provideAPIInterface(Retrofit retrofit) {
        return retrofit.create(APIInterface.class);
    }

    private HostnameVerifier hostnameVerifier = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession sslSession) {
            return hostname.equals(Constants.API_BASE_URL.substring("https://".length()));
        }
    };

    private OkHttpClient createClient(Context context) {

        OkHttpClient client = null;

        CertificateFactory cf;
        InputStream cert;
        Certificate ca;
        SSLContext sslContext;
        try {
            cf = CertificateFactory.getInstance("X.509");
            cert = context.getResources().openRawResource(R.raw.mykeystore); // Place your 'my_cert.crt' file in
            // `res/raw`

            ca = cf.generateCertificate(cert);
            cert.close();

            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(Constants.IS_LOGGING_ENABLED
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

            client = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory())
                    .addInterceptor(interceptor)
                    .hostnameVerifier(hostnameVerifier)
                    .build();

        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException |
                KeyManagementException e) {
            e.printStackTrace();
        }

        return client;
    }
}
