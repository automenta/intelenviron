/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

/**
 *
 * @author me
 */
public class Economy {

    public Economy() {
            
    }
    
    public void load(String path) {
        //load balances
    }
    
    public void save(String path) {
        //save log
        //save balances
        //save map        
    }
    
    public double getBalance(String agent, Currency c) {
        return 0;
    }
    
    public void setBalance(String agent, Currency c, double newAmount, String reason) {
        
    }
    
    public void transferBalance(String agentFrom, String agentTo, Currency c, double transferAmount, String reason) {
        
    }
    
    public void setAgentLocation(double lat, double lng) {
        
    }
    
    
}
