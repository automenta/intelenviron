/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

/**
 *
 * @author me
 */
public class TwitterCurrency {

    public final Economy econ;
    public final Currency currency;

    /**
     * 
     * @param econ
     * @param currency which currency to watch for
     */
    public TwitterCurrency(Economy econ, Currency currency) {
        this.econ = econ;
        this.currency = currency;
    }
    
    public void update() {
        
    }
    
    
}
