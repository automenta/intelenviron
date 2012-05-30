/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.Cookie;
import org.neo4j.helpers.UTF8;
import spark.*;
import static spark.Spark.*;

public class Web {
    long authenticationTimeout; //in milliseconds
    
    private Date adminAuthenticated = null;
    final private String adminCookieKey = "auth";
    private String adminCookie = "";
    private final String adminUser;
    private final String adminPassword;

    public static Map<String, Object> staticpages = new HashMap();
        final static byte[] buf = new byte[128000];
        //final static char[] chr = new char[4096];

//    public static String getLocalTextFile(File file) throws IOException {
//        int len;
//        final StringBuffer buffer = new StringBuffer();
//        final FileReader reader = new FileReader(file);
//        try {
//            while ((len = reader.read(chr)) > 0) {
//                buffer.append(chr, 0, len);
//            }
//        } finally {
//            reader.close();
//        }
//        return buffer.toString();
//    }


//    public static String getStaticTextFile(String path) throws IOException {
//        if (staticpages.containsKey(path)) {
//            return (String) staticpages.get(path);
//        }
//        String content = getLocalTextFile(new File("./web/" + path));
//        staticpages.put(path, content);
//        return content;
//    }

    public static void getStaticBinaryFile(String path, OutputStream os) throws IOException {
        getStaticBinaryFile(path, os, "");
    }

    //TODO should this be synchronized or a threadpool?
    public static void getStaticBinaryFile(String path, OutputStream os, String append) throws IOException {
        FileInputStream in = new FileInputStream(new File("./web/" + path));

        int count = 0;
        while ((count = in.read(buf)) >= 0) {
            os.write(buf, 0, count);
        }
        os.write(UTF8.encode(append));
        
        in.close();
        os.close();
    }
    
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
    
    public static void htmlHeader(final Response rspns) {
        rspns.header("Content-type", "text/html");        
    }
    public static void jsonHeader(final Response rspns) {
        rspns.header("Content-type", "application/json");        
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
        get(new Route("/log") {

            @Override
            public Object handle(Request rqst, Response rspns) {
                rspns.header("Content-type", "text/html");
                
                String result = Intelenviron.exec("tail -n 32 data/log");
                               
                return "<html><pre>" + result + "</pre></html>";
            }
            
        });
        get(new Route("/static/*") {

            @Override
            public Object handle(Request rqst, Response rspns) {


                String url = rqst.pathInfo();
                String page = "index.html";
                if (!url.equals("/")) {
                    String xpage = url.substring(url.indexOf("/", 1) + 1);
                    if (page.length() > 0) {
                        page = xpage;
                    }
                }                

                try {
                    if (page.endsWith(".jpg") || page.endsWith(".jpeg")) {
                        rspns.header("Content-type", "image/jpg");
                        getStaticBinaryFile(page, rspns.raw().getOutputStream());
                        return null;
                    } else if (page.endsWith(".png")) {
                        rspns.header("Content-type", "image/png");
                        getStaticBinaryFile(page, rspns.raw().getOutputStream());
                        return null;
                    } else if (page.endsWith(".gif")) {
                        rspns.header("Content-type", "image/gif");
                        getStaticBinaryFile(page, rspns.raw().getOutputStream());
                        return null;
                    } else if (page.endsWith(".css")) {
                        rspns.header("Content-type", "text/css");
                        getStaticBinaryFile(page, rspns.raw().getOutputStream());
                        return null;
                    } else if (page.endsWith(".js")) {
                        rspns.header("Content-type", "text/javascript");
                        getStaticBinaryFile(page, rspns.raw().getOutputStream());
                        return null;
                    } else {
                        rspns.header("Content-type", "text/html");
                        getStaticBinaryFile(page, rspns.raw().getOutputStream());
                        return null;
                    }
                } catch (IOException ex) {
//                    try {
//                        getStaticBinaryFile("404.html", rspns.raw().getOutputStream());
//                        halt(404);
//                    } catch (IOException ex1) {
//                        Logger.getLogger(Web.class.getName()).log(Level.SEVERE, null, ex1);
//                    }
                }
                return null;
            }
        });
        
    }
}
