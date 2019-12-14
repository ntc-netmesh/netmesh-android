package net.pregi.networking.speedtest;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SocketUtil {
    public static X509TrustManager createTrustManager() {
        try {
            // Get default factory
            // https://stackoverflow.com/questions/19005318/implementing-x509trustmanager-passing-on-part-of-the-verification-to-existing
            TrustManagerFactory tmf = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            // Using null here initialises the TMF with the default trust store.
            tmf.init((KeyStore) null);

            // Get hold of the default trust manager
            // Only the first trust manager is used.
            X509TrustManager x509Tm = null;
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    x509Tm = (X509TrustManager) tm;
                    break;
                }
            }

            return new X509TrustManager() {
                X509TrustManager existingTrustManager;

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return existingTrustManager != null ? existingTrustManager.getAcceptedIssuers() : null;
                }

                @Override
                public void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString)
                        throws CertificateException {
                    try {
                        if (existingTrustManager != null) {
                            existingTrustManager.checkServerTrusted(paramArrayOfX509Certificate, paramString);
                        }
                    } catch (CertificateException e) {
                        // TODO Add certificate exceptions here.
                        //      Normally, if a certificate fails verification,
                        //      a cascade of exceptions happen that would eventually prevent
                        //      communication with the target resource.
                        //      Browsers typically warn users
                        //          and allow them to consent to add exceptions for them.
                        System.out.println("A certificate failed to verify.");
                        e.printStackTrace();
                    }
                }

                @Override
                public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString)
                        throws CertificateException {
                    if (existingTrustManager != null) {
                        existingTrustManager.checkClientTrusted(paramArrayOfX509Certificate, paramString);
                    }
                }

                X509TrustManager init(X509TrustManager existingTrustManager) {
                    this.existingTrustManager = existingTrustManager;
                    return this;
                }
            }.init(x509Tm);
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            // TODO: scenarios that trip these must be handled gracefully.
            throw new RuntimeException(e);
        }
    }

    /** <p>Create a socket factory. This is needed to reach HTTPS servers. </p>
     *
     * */
    public static SSLContext createSSLContext(X509TrustManager trustManager) {
        try {
            // Create our sslContext.
            // It's tempting to just allow everything through,
            //		but that would be a serious vulnerability.
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore)null);

            SSLContext sslcontext;
            try {
                sslcontext = SSLContext.getInstance("TLS");
                sslcontext.init(null,
                        // trustManagerFactory.getTrustManagers()
                        new X509TrustManager[] {
                                trustManager
                        }
                        , new SecureRandom());
            } catch (NoSuchAlgorithmException e) {
                sslcontext = SSLContext.getDefault();
            }

            return sslcontext;
        } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException e) {
            // TODO: scenarios that trip these must be handled gracefully.
            throw new RuntimeException(e);
        }
    }
}
