package com.example;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Main class to launch the embedded Jetty server.
 */
public class JettyLauncher {

    /**
     * Launch the embedded Jetty server and also the JAX-RS services specified in the web.xml file.
     * @param args not required
     */
    public static void main(String[] args) throws Exception {
        Server server = createServer();
        server.start();
    }

    public static Server createServer() throws Exception {

        if(System.getenv("MONGODB_URI") == null) {
            throw new IllegalArgumentException("MONGODB_URI environment variable must be set.");
        }

        if(System.getenv("REDIS_URL") == null) {
            throw new IllegalArgumentException("REDIS_URL environment variable must be set.");
        }

        String webappDirLocation = "src/main/webapp/";
        String webPort = System.getenv("PORT");
        if (webPort == null || webPort.isEmpty()) {
            webPort = "8080";
        }
        Server server = new Server(Integer.valueOf(webPort));
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setDescriptor(webappDirLocation + "/WEB-INF/web.xml");
        root.setResourceBase(webappDirLocation);
        server.setHandler(root);
        return server;
    }
}
