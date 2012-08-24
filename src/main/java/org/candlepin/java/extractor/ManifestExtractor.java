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
import java.util.Map;
import java.util.Random;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.candlepin.groovy.CandlepinClient;

public class ManifestExtractor {

    static Random r = new Random();

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
    public static String extractManifest(String ADMIN_USERNAME,
            String ADMIN_PASSWORD,
            String HOST,
            String PORT) {
        Boolean use_ssl = Boolean.TRUE;

        CandlepinClient cp = new CandlepinClient(ADMIN_USERNAME, ADMIN_PASSWORD,
                null, null,
                HOST, PORT,
                "candlepin", use_ssl);

        Map owner = (Map) cp.create_owner("owner" + randstring());

        Map product1;
        try {
            product1 = (Map) cp.get_product("ZOO");
        } catch (HttpResponseException ex) {
            product1 = (Map) cp.create_product("ZOO", "Zoo Enterprise 24/7");
        }

        Map product2;
        try {
            product2 = (Map) cp.get_product("NATURE");
        } catch (HttpResponseException ex) {
            product2 = (Map) cp.create_product("NATURE", "Nature Enterprise 8/5");
        }

        Map subscription1 = (Map) cp.create_subscription(owner.get("key"), product1.get("id"), 100, "", "123456", null, null);
        Map subscription2 = (Map) cp.create_subscription(owner.get("key"), product2.get("id"), 100, "", "765432", null, null);
        Map statuspath = (Map) cp.refresh_pools(owner.get("key"), Boolean.FALSE, Boolean.FALSE);
        cp.async_control(statuspath);

        Map pool1 = (Map) ((List) cp.list_owner_pools(owner.get("id"), product1.get("id"))).get(0);
        Map pool2 = (Map) ((List) cp.list_owner_pools(owner.get("id"), product2.get("id"))).get(0);


        String ORG_ADMIN_USERNAME = "orgadmin-" + randstring();
        String ORG_ADMIN_PASSWORD = "password";

        cp.create_user(ORG_ADMIN_USERNAME, ORG_ADMIN_PASSWORD, Boolean.TRUE);

        CandlepinClient org_admin_cp = new CandlepinClient(ORG_ADMIN_USERNAME, ORG_ADMIN_PASSWORD,
                null, null,
                HOST, PORT,
                "candlepin", use_ssl);

        Map consumer = (Map) org_admin_cp.register("dummyconsumer" + randstring(), "candlepin", owner.get("key"));

        CandlepinConsume consumer_cp = new CandlepinConsume(HOST, PORT,
                (String) ((Map) consumer.get("idCert")).get("key"),
                (String) ((Map) consumer.get("idCert")).get("cert"),
                (String) (consumer.get("uuid")));

        consumer_cp.sslClientConsumePool((String) pool1.get("id"), "100");
        consumer_cp.sslClientConsumePool((String) pool2.get("id"), "100");

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
        extractManifest("admin", "admin", "candlepin.localdomain", "8443");
        // rcp.turnKatelloBackOn();
    }
}
