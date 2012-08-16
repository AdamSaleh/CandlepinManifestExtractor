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
    
    public void turnKatelloOff() throws IOException, InterruptedException{
        runcommand("cp  /etc/candlepin/candlepin.conf  /tmp/candlepin.conf");
        runcommand("cat /tmp/candlepin.conf| sed s/^module.config.katello/#module.config.katello/ | sed s/^candlepin/#candlepin/ > /etc/candlepin/candlepin.conf");
        runcommand("service tomcat6 restart");
    }
    public void turnKatelloBackOn() throws IOException, InterruptedException{
        runcommand("mv -f   /tmp/candlepin.conf  /etc/candlepin/candlepin.conf");
        runcommand("service tomcat6 restart");
    }
    
}
