package jetty12;

import java.util.Set;

import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

@Component
public class JettyCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

    @Override
    public void customize(JettyServletWebServerFactory factory) {
        factory.addServerCustomizers(this::customizeUriCompliance);
        factory.addServerCustomizers(this::bypassServlet6UriRules);
    }

    private void customizeUriCompliance(Server server) {
        for (Connector connector : server.getConnectors()) {
            connector.getConnectionFactories().stream()
                    .filter(factory -> factory instanceof HttpConnectionFactory)
                    .forEach(factory -> {
                        HttpConfiguration httpConfig = ((HttpConnectionFactory) factory).getHttpConfiguration();
                        httpConfig.setUriCompliance(UriCompliance.from(Set.of(
                                UriCompliance.Violation.AMBIGUOUS_PATH_SEPARATOR,
                                UriCompliance.Violation.AMBIGUOUS_PATH_ENCODING)));
                    });
        }
    }

    /**
     * Bypass Servlet 6 spec rules on ambiguous URIs.
     * <p>
     *     <em>WARNING</em> this is not recommended and will result
     *     in other libraries (like Jersey and Spring) misbehaving in odd,
     *     unusual, and unpredictable ways (this is a violation of the Servlet 6 spec rules).
     * </p>
     *
     * @see <a href="https://github.com/jakartaee/servlet/issues/18">Servlet 6 changes to getRequestURI / getRequestContextPath / getServletPath / getPathInfo</a>
     */
    private void bypassServlet6UriRules(Server server) {
        server.getContainedBeans(ServletHandler.class)
            .forEach(handler -> handler.setDecodeAmbiguousURIs(true));
    }
}

