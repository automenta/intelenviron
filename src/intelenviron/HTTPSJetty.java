/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

/**
 *
 * @author me
 */
public class HTTPSJetty {
//    //keytool -alias jetty -genkey -keyalg RSA
//    //https://github.com/perwendel/spark/blob/master/src/test/java/spark/servlet/ServletTest.java
//    //https://github.com/perwendel/spark/blob/master/src/main/java/spark/webserver/SparkServerImpl.java
////    http://docs.codehaus.org/display/JETTY/Embedding+Jetty
//
//    public static class JettyHandler2 extends AbstractHandler {
//
//        private Filter filter;
//
//        public JettyHandler2(Filter filter) {
//            this.filter = filter;
//        }
//
//        @Override
//        public void handle(String target, Request baseRequest, HttpServletRequest request,
//                HttpServletResponse response) throws IOException, ServletException {
//            Log.debug("jettyhandler, handle();");
//            filter.doFilter(request, response, null);
//            baseRequest.setHandled(true);
//        }
//    }
//
//    public static void main(String[] args) throws Exception {
//        
//        Server server = new Server();
//        SslSocketConnector connector = new SslSocketConnector();
//        connector.setPort(9191);
//        connector.setKeyPassword("abcxyz");
////        if (keystoreFile != "") {
////                connector.setKeystore(keystoreFile);
////        }
//        server.setConnectors(new Connector[]{connector});
//        
//          MatcherFilter matcherFilter = new MatcherFilter(RouteMatcherFactory.get(), false);
//        matcherFilter.init(null);
//       server.setHandler(new JettyHandler2(matcherFilter));
//
//
////        MatcherFilter matcherFilter = new MatcherFilter(RouteMatcherFactory.get(), true);
////        matcherFilter.init(null);
////        
////        server.setHandler(new JettyHandler2(matcherFilter));
//        	//WebAppContext bb = new WeAppContext();
////		bb.setServer(server);
////		//bb.setContextPath(SOMEPATH);
////		//bb.setWar("src/test/webapp");
////                //bb.addServlet(server, null)
//                //bb.addFilter(
//                
////
//		//server.setHandler(bb);
////        
//
//        server.start();
//    }
}
