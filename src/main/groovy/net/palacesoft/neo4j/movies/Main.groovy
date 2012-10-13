package net.palacesoft.neo4j.movies

import org.mortbay.jetty.Server
import org.mortbay.jetty.servlet.Context
import com.sun.jersey.spi.container.servlet.ServletContainer
import com.sun.jersey.api.core.PackagesResourceConfig
import org.mortbay.jetty.servlet.ServletHolder


class Main {

    public static void main(String[] args) {
        String webPort = System.getenv("PORT");
        if (webPort == null || webPort.isEmpty()) {
            webPort = "8080";
        }
        Server server = new Server(webPort.toInteger());
        Context root = new Context(server, "/resources", Context.SESSIONS);
        ServletContainer servletContainer = new ServletContainer(new PackagesResourceConfig("net.palacesoft.neo4j.movies"))
        root.addServlet(new ServletHolder(servletContainer), "/*");
        server.start()
    }
}
