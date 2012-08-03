/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.candlepin.java.extractor;

import groovyx.net.http.AuthConfig;
import groovyx.net.http.HTTPBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;

import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;

/**
 *
 * @author asaleh
 */
public class KeyCertConfig extends AuthConfig {

    String uuid="";

    public String getUuid() {
        return uuid;
    }
    
    public KeyCertConfig(HTTPBuilder builder) {
        super(builder);
    }

    /*
     * Sets a certificate to be used for SSL authentication. See
     * {@link Class#getResource(String)} for how to get a URL from a resource on
     * the classpath.
     *
     * @param certURL URL to a JKS keystore where the certificate is stored.
     * @param password password to decrypt the keystore
     */
    public void certificate(String key, String cer, final String password)
            throws GeneralSecurityException, IOException {

        KeyStore keyStore =pemToPKCS12(key, cer, password);
        

        SSLSocketFactory ssl = new SSLSocketFactory(keyStore, password);
        ssl.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);

        builder.getClient().getConnectionManager().getSchemeRegistry().register(new Scheme("https", ssl, 8443));
    }

    public KeyStore pemToPKCS12(Reader keyFile, Reader cerFile, final String password) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
   
        PEMReader pem = new PEMReader(keyFile);

        PrivateKey key = ((KeyPair) pem.readObject()).getPrivate();

        pem.close();
        keyFile.close();

        // Get the certificate      

        pem = new PEMReader(cerFile);

        X509Certificate cert = (X509Certificate) pem.readObject();

        pem.close();
        cerFile.close();

        uuid = cert.getSubjectDN().getName().substring(3); //"Begins \w CN= and I don't want that

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null);
        ks.setKeyEntry("alias", (Key) key, password.toCharArray(), new java.security.cert.Certificate[]{cert});
        return ks;
    }

    public KeyStore pemToPKCS12(String key, String cer, final String password) throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException  {
        StringReader keyReader = new StringReader(key);
        StringReader cerReader = new StringReader(cer);

        return pemToPKCS12(keyReader, cerReader, password);
    }
}
