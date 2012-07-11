/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

import java.io.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.python.util.PythonInterpreter;

/**
 *
 * @author me
 */
public class RunPython {
    
    public static String exec(String cmd) {
        try {
            StringBuffer r = new StringBuffer();
            Process p = Runtime.getRuntime().exec(cmd);
            InputStream i = p.getInputStream();
            p.waitFor();
            BufferedReader in = new BufferedReader(new InputStreamReader(i));  
            String line = null;  
            while ((line = in.readLine()) != null) {  
                r.append(line + "\n");
            }  
            in.close();        
            return r.toString();
        }
        catch (Exception e) {
            return e.toString();
        }
    }

    public static void log(String s) {
        FileWriter outFile = null;
        try {
            outFile = new FileWriter("data/log", true);            
            outFile.append(new Date().toString() + " " + s.replaceAll("\'", "\\\'") + "\n"); //writes to file
            outFile.close();
        } catch (IOException ex) {
            Logger.getLogger(RunPython.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    public static void main(String[] args) {
        log("Starting");                       
        
        final PythonInterpreter interp = new PythonInterpreter();
        String scriptFile = "conf/default.py";
        interp.execfile(scriptFile);

         Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override public void run() {
                log("Stopping");
            }            
        }));
        
    }
}
