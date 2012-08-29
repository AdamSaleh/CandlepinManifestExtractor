/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.candlepin.java.extractor;

/**
 *
 * @author asaleh
 */
import groovyx.net.http.HttpResponseException;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.candlepin.groovy.CandlepinClient;

public class ManifestExtractor {

    static Random r = new Random();
    static Boolean use_ssl = Boolean.TRUE;

    public static String randstring() {
        int randnum = 1000000 + r.nextInt(100000);
        return String.valueOf(randnum);
    }

    /**
     * Extracts manifest file from running candlepin server
     *
     * @return path to exported manifest (it saves to
     * /tmp/candlepin-XXXXXX/export-XXXXXX.zip ,where XXXXXX are random nubers)
     */
    public static String createOwner(String ADMIN_USERNAME,
            String ADMIN_PASSWORD,
            String HOST,String PORT,
            String ownerkey){
         CandlepinClient cp = new CandlepinClient(ADMIN_USERNAME, ADMIN_PASSWORD,
                null, null,
                HOST, PORT,
                "candlepin", use_ssl);
         Map nowner = (Map) cp.create_owner(ownerkey);
         
         return (String) nowner.get("key");
    }
    
     public static String createProduct(String ADMIN_USERNAME,
            String ADMIN_PASSWORD,
            String HOST,String PORT,
            String productid,String productname){
         CandlepinClient cp = new CandlepinClient(ADMIN_USERNAME, ADMIN_PASSWORD,
                null, null,
                HOST, PORT,
                "candlepin", use_ssl);
         Map nowner = (Map) cp.create_product(productid, productname);
         
         return (String) nowner.get("key");
    }
            
    public static String extractManifest(String ADMIN_USERNAME,
            String ADMIN_PASSWORD,
            String HOST,
            String PORT,String owner,List<String> products) throws HttpResponseException {
       
        CandlepinClient cp = new CandlepinClient(ADMIN_USERNAME, ADMIN_PASSWORD,
                null, null,
                HOST, PORT,
                "candlepin", use_ssl);

        Map nowner= (Map) cp.get_owner(owner);
       
        List<Map> productMap = new ArrayList<Map>();
        for(String product:products){
       
           Map product1 = (Map) cp.get_product(product);
           if(product1!=null) productMap.add(product1);
        }

        
        for(Map product:productMap){
             cp.create_subscription(nowner.get("key"), product.get("id"), 100, "", "123456", null, null);
        }
        Map statuspath = (Map) cp.refresh_pools(nowner.get("key"), Boolean.FALSE, Boolean.FALSE);
        cp.async_control(statuspath);

        List<Map> poolMap = new ArrayList<Map>();
        for(Map product:productMap){

            Map pool1 = (Map) ((List) cp.list_owner_pools(nowner.get("id"), product.get("id"))).get(0);
            if(pool1!=null) poolMap.add(pool1);
        }

        String ORG_ADMIN_USERNAME = "orgadmin-" + randstring();
        String ORG_ADMIN_PASSWORD = "password";

        cp.create_user(ORG_ADMIN_USERNAME, ORG_ADMIN_PASSWORD, Boolean.TRUE);

        CandlepinClient org_admin_cp = new CandlepinClient(ORG_ADMIN_USERNAME, ORG_ADMIN_PASSWORD,
                null, null,
                HOST, PORT,
                "candlepin", use_ssl);

        Map consumer = (Map) org_admin_cp.register("dummyconsumer" + randstring(), "candlepin", nowner.get("key"));

        CandlepinConsume consumer_cp = new CandlepinConsume(HOST, PORT,
                (String) ((Map) consumer.get("idCert")).get("key"),
                (String) ((Map) consumer.get("idCert")).get("cert"),
                (String) (consumer.get("uuid")));
        for(Map pool:poolMap){

            consumer_cp.sslClientConsumePool((String) pool.get("id"), "100");
        }
        File tmpdir = new File("/tmp/candlepin-" + randstring());
        tmpdir.mkdirs();
        try {
            String fname = consumer_cp.sslClientExportPool(tmpdir.getCanonicalPath());
            return fname;
        } catch (IOException ex) {
            Logger.getLogger(ManifestExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "";
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // ResetCandlepin rcp = new ResetCandlepin("katello.localdomain", 22, "root", "login");
        // rcp.turnKatelloOff();
        System.out.println("Downloading from admin,admin, candlepin.localdomain");
        List<String> products = new ArrayList<String>();
        products.add("ZOO");
        products.add("NATURE");

        extractManifest("admin", "admin", "candlepin.localdomain", "8443","export_owner",products);
        // rcp.turnKatelloBackOn();
    }
}