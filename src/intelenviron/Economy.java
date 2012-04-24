/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

import twitter4j.Tweet;

/**
 *
 * @author me
 */
public class Economy extends TwitterUpdater {

    public Economy(String currency) {
        super(currency, 180);
        
        //load balances

         Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override public void run() {
                save();
            }            
        }));
        
    }
      
    
    public synchronized void save() {
        //save log
        //save balances
        //save map        
    }
    
    public double getBalance(String agent, String currency) {
        return 0;
    }
    
    public synchronized void setBalance(String agent, String c, double newAmount, String reason) {
        
    }
    
    public synchronized void transferBalance(String agentFrom, String agentTo, String c, double transferAmount, String reason) {
        
    }
    
    public synchronized void setAgentLocation(double lat, double lng) {
        
    }

    @Override
    public void onTweet(Tweet t) {
    }
    
    
    
}
