/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.candlepin.java.extractor;

import groovyx.net.http.HTTPBuilder;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 *
 * @author asaleh
 */
public class CandlepinConsume {

    static Random r = new Random();
    String key;
    String cer;
    String uri;
    String port;
            
    String uuid = "";
    

    public static String randstring() {
        int randnum = 1000000 + r.nextInt(100000);
        return String.valueOf(randnum);
    }

    public CandlepinConsume(String uri,String port,String key, String cer) {
      
            this.key = key;
            this.cer = cer;
            this.port = port;
            this.uri = uri;

           
    }
    
    private void setupClient(DefaultHttpClient httpclient){
          try {
            KeyCertConfig kcc = new KeyCertConfig(new HTTPBuilder());
            KeyStore trustStore = kcc.pemToPKCS12(key, cer, "asdf");
            uuid = kcc.getUuid();
            SSLSocketFactory socketFactory = new SSLSocketFactory(trustStore, "asdf");
            Scheme sch = new Scheme("https", socketFactory, 8443);
            httpclient.getConnectionManager().getSchemeRegistry().register(sch);
        } catch (KeyManagementException ex) {
            Logger.getLogger(CandlepinConsume.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnrecoverableKeyException ex) {
            Logger.getLogger(CandlepinConsume.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CandlepinConsume.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeyStoreException ex) {
            Logger.getLogger(CandlepinConsume.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(CandlepinConsume.class.getName()).log(Level.SEVERE, null, ex);
        } catch (CertificateException ex) {
            Logger.getLogger(CandlepinConsume.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void sslClientConsumePool(String pool, String quantity) {
                DefaultHttpClient httpclient = new DefaultHttpClient();

        try {
                setupClient(httpclient);

            HttpGet httpget = new HttpGet("https://"+uri+":"+port+"/candlepin/consumers/" + uuid + "/entitlements?pool=" + pool + "&quantity=" + quantity);

            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();


        } catch (IOException ex) {
            Logger.getLogger(CandlepinConsume.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
    }

    String sslClientExportPool( String destDir) {
       DefaultHttpClient httpclient = new DefaultHttpClient();
       String fname="";
        try {
                setupClient(httpclient);

            HttpGet httpget = new HttpGet("https://"+uri+":"+port+"/candlepin/consumers/" + uuid + "/export");

            HttpResponse response = httpclient.execute(httpget);
            InputStream inputStream = response.getEntity().getContent();

            try {

                // write the inputStream to a FileOutputStream
                fname = destDir + "/export-" + randstring()+".zip";
                OutputStream out = new FileOutputStream(new File(fname));

                int read = 0;
                byte[] bytes = new byte[1024];

                while ((read = inputStream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }

                inputStream.close();
                out.flush();
                out.close();

                System.out.println("New file created: "+fname);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

        } catch (IOException ex) {
            Logger.getLogger(ManifestExtractor.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
        return fname;
    }
}
