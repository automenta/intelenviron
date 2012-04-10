/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Cookie;
import static spark.Spark.*;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;

public class Web {
    long authenticationTimeout; //in milliseconds
    
    private Date adminAuthenticated = null;
    final private String adminCookieKey = "auth";
    private String adminCookie = "";
    private final String adminUser;
    private final String adminPassword;

    public boolean isAdmin(String user, String pass) {
        if (user.equals(adminUser))
            if (pass.equals(adminPassword))
                return true;
        return false;
    }
    
    public Cookie setAdmin(String ip) {
        this.adminCookie = UUID.randomUUID().toString();
        this.adminAuthenticated = new Date();
        Cookie c = new Cookie(adminCookieKey, adminCookie);
        c.setMaxAge((int)(authenticationTimeout/1000));
        return c;
    }
    
    public void requireAuthentication(String path) {
        before(new Filter(path) {

            @Override
            public void handle(Request request, Response response) {
                boolean authenticated = false;

                if (adminAuthenticated!=null) {
                    final Date now = new Date();
                    if (now.getTime() - adminAuthenticated.getTime() < authenticationTimeout) {
                        for (Cookie c : request.raw().getCookies()) {
                            if (c.getName().equals(adminCookieKey)) {
                                String v = c.getValue();
                                if (v.equals(adminCookie))
                                    authenticated = true;
                            }

                        }
                    }
                }
                
                if (!authenticated) {
                    String authHeader = request.headers("Authorization");
                    if ((authHeader != null) && (authHeader.startsWith("Basic"))) {
                        authHeader = authHeader.substring("Basic".length()).trim();
                        try {
                            authHeader = new String(Base64.decode(authHeader));
                            String user = authHeader.split(":")[0];
                            String password = authHeader.split(":")[1];
                            if (isAdmin(user, password)) {
                                Cookie c = setAdmin(request.ip());
                                response.raw().addCookie(c);
                                authenticated = true;
                            }

                        } catch (Base64DecodingException ex) {
                            Logger.getLogger(Web.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                }
                
                if (!authenticated) {
                    response.header("WWW-Authenticate", "Basic");
                    halt(401, "Please login");
                }

            }
        });
        
        
    }
    
    //http://blog.stevensanderson.com/2008/08/25/using-the-browsers-native-login-prompt/
    /**
     * 
     * @param adminUser
     * @param adminPassword
     * @param authTimeout  in milliseconds
     */
    public Web(String adminUser, String adminPassword, long authTimeout) {
        
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
        this.authenticationTimeout = authTimeout;
        
        setPort(9090); // Spark will run on port 9090
        
        requireAuthentication("/");

        get(new Route("/") {

            @Override
            public Object handle(Request rqst, Response rspns) {
                //rspns.header("Content-type", "text/html");
                        
                return "welcome";
            }
            
        });
        
    }
}
