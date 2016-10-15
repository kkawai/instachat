package com.instachat.android.util;

import com.instachat.android.MyApp;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by kevin on 10/14/2015.
 */
public class HttpMessageHelper {
    public static void initSSL() throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
// From https://www.washington.edu/itconnect/security/ca/load-der.crt
        InputStream caInput = new BufferedInputStream(MyApp.getInstance().getAssets().open("public.crt"));
        Certificate ca;
        try {
            ca = cf.generateCertificate(caInput);
            System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
        } finally {
            caInput.close();
        }

// Create a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

// Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

// Create an SSLContext that uses our TrustManager
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);

        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

// Tell the URLConnection to use a SocketFactory from our SSLContext
        URL url = new URL("https://api.instachat.us/ih/settings");
        //URL url = new URL("https://certs.cac.washington.edu/CAtest/");
        HttpsURLConnection urlConnection =
                (HttpsURLConnection) url.openConnection();
        //urlConnection.setSSLSocketFactory(context.getSocketFactory());

        InputStream in = urlConnection.getInputStream();
        final StringBuilder sb = new StringBuilder(16000);
        final byte buf[] = new byte[16000];
        int read = 0;
        while ((read = in.read(buf)) != -1) {
            sb.append(new String(buf, 0, read, "UTF-8"));
        }
        System.out.println("yyyy response: " + sb.toString());
        in.close();

    }
}
