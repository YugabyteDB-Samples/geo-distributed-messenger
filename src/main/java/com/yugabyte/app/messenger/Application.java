package com.yugabyte.app.messenger;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URL;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * The entry point of the Spring Boot application.
 *
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 *
 */
@SpringBootApplication
@Theme(value = "geo-distributed-messenger", variant = Lumo.DARK)
@PWA(name = "geo-distributed-messenger", shortName = "geo-distributed-messenger", offlineResources = {})
@NpmPackage(value = "line-awesome", version = "1.3.0")
@NpmPackage(value = "@vaadin-component-factory/vcf-nav", version = "1.0.6")
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

    public static void main(String[] args) {
        enableFixieProxy();
        SpringApplication.run(Application.class, args);
    }

    private static void enableFixieProxy() {
        if (System.getenv("USE_FIXIE_SOCKS") != null) {
            boolean useFixie = Boolean.valueOf(System.getenv("USE_FIXIE_SOCKS"));

            if (useFixie) {
                System.out.println("Setting up Fixie Socks Proxy");

                String[] fixieData = System.getenv("FIXIE_SOCKS_HOST").split("@");
                String[] fixieCredentials = fixieData[0].split(":");
                String[] fixieUrl = fixieData[1].split(":");

                String fixieHost = fixieUrl[0];
                String fixiePort = fixieUrl[1];
                String fixieUser = fixieCredentials[0];
                String fixiePassword = fixieCredentials[1];

                DatabaseProxySelector proxySelector = new DatabaseProxySelector(fixieHost, Integer.parseInt(fixiePort));
                ProxySelector.setDefault(proxySelector);

                Authenticator.setDefault(new ProxyAuthenticator(fixieUser, fixiePassword));

                System.out.println("Enabled Fixie Socks Proxy:" + fixieHost);
            }
        }
    }

    private static class ProxyAuthenticator extends Authenticator {
        private final PasswordAuthentication passwordAuthentication;

        private ProxyAuthenticator(String user, String password) {
            passwordAuthentication = new PasswordAuthentication(user, password.toCharArray());
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return passwordAuthentication;
        }
    }
}
