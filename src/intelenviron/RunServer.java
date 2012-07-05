/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

/**
 *
 * @author me
 */
public class RunServer {
    
    public static void main(String[] args) {
        new Web("admin", "password", 1000 * 60 * 60);
        new KBWeb();
    }
}
