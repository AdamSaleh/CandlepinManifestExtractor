/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.candlepin.java.extractor;
import com.trilead.ssh2.Connection;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author asaleh
 */
public class ResetCandlepin {
    Connection c = null;
    String username;
    String password;
    public ResetCandlepin(String hostname,int port,String username,String password) throws IOException {
        c = new Connection(hostname, port);
        this.username=username;
        this.password=password;
        c.connect();
                c.authenticateWithPassword(username, password);

    }
 
    void setSudoSu() throws IOException, InterruptedException{
       runcommand("sudo -n su");
    }
    
    private void runcommand(String command) throws IOException, InterruptedException{
        OutputStream s = new ByteArrayOutputStream();
        int exit = c.exec(command, s);
        if(exit!=0){
            throw new InterruptedException(s.toString());
        }
        s.close();
    }
    
    public String clearDatabase() throws IOException, InterruptedException{
        String folder ="/tmp/saved_katello_databases";
        runcommand("mkdir -p "+ folder);

        return folder;
    }
    
    public void turnKatelloOff() throws IOException, InterruptedException{
        runcommand("cp  /etc/candlepin/candlepin.conf  /tmp/candlepin.conf");
        runcommand("cat /tmp/candlepin.conf| sed s/^module.config.katello/#module.config.katello/ | sed s/^candlepin/#candlepin/ > /etc/candlepin/candlepin.conf");
        runcommand("chown tomcat:tomcat  /etc/candlepin/candlepin.conf");
        runcommand("service tomcat6 restart");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ManifestExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
        runcommand("curl -k  https://localhost:8443/candlepin/admin/init/");

    }
    public void turnKatelloBackOn() throws IOException, InterruptedException{
        runcommand("mv -f   /tmp/candlepin.conf  /etc/candlepin/candlepin.conf");
        runcommand("chown tomcat:tomcat  /etc/candlepin/candlepin.conf");
        runcommand("service tomcat6 restart");
        try {
            Thread.sleep(4000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ManifestExtractor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
