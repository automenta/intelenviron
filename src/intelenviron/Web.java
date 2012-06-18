/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
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

    final static int staticBufferSize = 1024 * 64;
    final static ObjectPool<byte[]> buffers = new ObjectPool() {

        @Override
        protected Object create() {
            return new byte[staticBufferSize];
        }

        @Override
        public boolean validate(Object o) {
            return true;
        }

        @Override
        public void expire(Object o) {
        }

        
    };

    public static void getStaticBinaryFile(String path, Response rspns) throws IOException {
        getStaticBinaryFile(path, rspns, null);
    }



    public static void getStatic(final String path, final String contentPath) {
        get(new Route(path) {
            @Override
            public Object handle(Request rqst, Response rspns) {
                htmlHeader(rspns);
                try {
                    getStaticBinaryFile(contentPath, rspns);
                } catch (IOException ex) {
                    Logger.getLogger(KBWeb.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }            
        });
    }
    
    public static void setCacheable(Response r) {
        final Calendar inTwoMonths = new GregorianCalendar();
        inTwoMonths.setTime(new Date());
        inTwoMonths.add(Calendar.MONTH, 2);
        r.raw().setDateHeader("Expires", inTwoMonths.getTimeInMillis());        
    }
    
    public static void getStaticBinaryFile(final String path, final Response rspns, final String append) throws IOException {
        
        ServletOutputStream os = rspns.raw().getOutputStream();
        final File f = new File("./web/" + path);
        if (!f.exists()) {
            return;
        }
        
        
        byte[] buf = buffers.checkOut();
        
        final FileInputStream in = new FileInputStream(f);

        int count = 0;
        while ((count = in.read(buf)) >= 0) {
            os.write(buf, 0, count);
        }
        
        if (append!=null)
            os.write(UTF8.encode(append));
        
        in.close();
        os.close();
        
        buffers.checkIn(buf);
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
    
    public static void jsonHeader(final Response rspns) {
        rspns.header("Content-type", "application/json");        
    }
    public static void htmlHeader(final Response rspns) {
        rspns.header("Content-type", "text/html");        
        setCacheable(rspns);       
    }
    public static void jsHeader(final Response rspns) {
        rspns.header("Content-type", "application/javascript");        
        setCacheable(rspns);       
    }
    public static void imageHeader(final Response rspns, final String type) {
        rspns.header("Content-type", "image/" + type);
        setCacheable(rspns);       
    }
    public static void cssHeader(final Response rspns) {
        rspns.header("Content-type", "text/css");
        setCacheable(rspns);
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

//        get(new Route("/") {
//
//            @Override
//            public Object handle(Request rqst, Response rspns) {
//                //rspns.header("Content-type", "text/html");
//                        
//                return "welcome";
//            }
//            
//        });
        
        get(new Route("/log") {

            @Override
            public Object handle(Request rqst, Response rspns) {
                rspns.header("Content-type", "text/html");
                
                String result = Intelenviron.exec("tail -n 32 data/log");
                               
                return "<html><pre>" + result + "</pre></html>";
            }
            
        });
        get(new Route("/favicon.ico") {
            @Override public Object handle(Request rqst, Response rspns) {
                return "";
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
                        imageHeader(rspns, "jpg");
                        getStaticBinaryFile(page, rspns);
                        return null;
                    } else if (page.endsWith(".png")) {
                        imageHeader(rspns, "png");
                        getStaticBinaryFile(page, rspns);
                        return null;
                    } else if (page.endsWith(".gif")) {
                        imageHeader(rspns, "gif");
                        getStaticBinaryFile(page, rspns);
                        return null;
                    } else if (page.endsWith(".css")) {
                        cssHeader(rspns);
                        getStaticBinaryFile(page, rspns);
                        return null;
                    } else if (page.endsWith(".js")) {
                        jsHeader(rspns);
                        getStaticBinaryFile(page, rspns);
                        return null;
                    } else {
                        htmlHeader(rspns);
                        getStaticBinaryFile(page, rspns);
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
