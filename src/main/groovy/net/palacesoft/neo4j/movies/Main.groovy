package net.palacesoft.neo4j.movies

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext

class Main {

    public static void main(String[] args) {
        /*String webPort = System.getenv("PORT");
        if (webPort == null || webPort.isEmpty()) {
            webPort = "8080";
        }
        Server server = new Server(webPort.toInteger());
        Context root = new Context(server, "/resources", Context.SESSIONS);
        ServletContainer servletContainer = new ServletContainer(new PackagesResourceConfig("net.palacesoft.neo4j.movies"))
        root.addServlet(new ServletHolder(servletContainer), "*//*");
        server.start()*/

        String webappDirLocation = "src/main/webapp/";
                String webPort = System.getenv("PORT");
                if(webPort == null || webPort.isEmpty()) {
                    webPort = "8080";
                }
                Server server = new Server(Integer.valueOf(webPort));
                WebAppContext root = new WebAppContext();
                root.setContextPath("/");
                root.setDescriptor(webappDirLocation+"/WEB-INF/web.xml");
                root.setResourceBase(webappDirLocation);
                root.setParentLoaderPriority(true);
                server.setHandler(root);
                server.start();
                server.join();
    }
}
