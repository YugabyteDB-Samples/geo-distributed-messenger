package com.yugabyte.app.messenger;

import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.runtimeconfig.v1beta1.CloudRuntimeConfig;
import com.google.api.services.runtimeconfig.v1beta1.CloudRuntimeConfigScopes;
import com.google.api.services.runtimeconfig.v1beta1.model.Variable;
import com.google.api.services.runtimeconfig.v1beta1.model.WatchVariableRequest;

@Service
@Scope("singleton")
public class GoogleCloudRuntimeConfigurator {
    private static final String CONFIG_NAME = "messaging-microservice-settings";

    private final String CONFIGURATOR_URL = "projects/%s/configs/%s/variables/%s/";

    private final String DB_URL_VARIABLE = "spring.datasource.url";
    private final String DB_USER_VARIABLE = "spring.datasource.username";
    private final String DB_PWD_VARIABLE = "spring.datasource.password";

    private CloudRuntimeConfig runtimeConfig;
    private String apiEndpoint;

    private String currentDBUrl;
    private String currentDBUser;
    private String currentDBPassword;

    public GoogleCloudRuntimeConfigurator(
            @Value("${spring.datasource.url}") String currentDBUrl,
            @Value("${spring.datasource.username}") String currentDBUser,
            @Value("${spring.datasource.password}") String currentDBPassword) {
        this.currentDBUrl = currentDBUrl;
        this.currentDBUser = currentDBUser;
        this.currentDBPassword = currentDBPassword;

        if (System.getenv("enable.runtime.configurator") != null &&
                Boolean.valueOf(System.getenv("enable.runtime.configurator"))) {
            enable();
        }
    }

    private void init() throws Exception {
        String regionName = System.getenv("REGION");
        String projectId = System.getenv("PROJECT_ID");

        if (regionName == null || regionName.isBlank() || projectId == null || projectId.isBlank())
            throw new RuntimeException(String.format("Cloud region or project ID parameter is" +
                    "not set for the Runtime Configurer." +
                    "The feature is disabled [region=%s, project_id=%s]", regionName, projectId));

        apiEndpoint = String.format(CONFIGURATOR_URL, projectId, CONFIG_NAME, regionName);

        System.out.printf("Prepared Google Runtime Configurator's URL: %s%n", apiEndpoint);

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JacksonFactory jacksonFactory = JacksonFactory.getDefaultInstance();

        GoogleCredential credential = GoogleCredential
                .getApplicationDefault(httpTransport, jacksonFactory)
                .createScoped(Collections.singleton(CloudRuntimeConfigScopes.CLOUDRUNTIMECONFIG));

        runtimeConfig = new CloudRuntimeConfig(httpTransport, jacksonFactory, credential);
    }

    public void enable() {
        try {
            init();

            new Thread(new Runnable() {

                @Override
                public void run() {
                    System.out.println("Started a thread to watch for the configuration changes");

                    while (true) {
                        try {
                            try {
                                Thread.sleep(10 * 1000);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }

                            Variable dbUrlVar = runtimeConfig.projects().configs().variables()
                                    .get(apiEndpoint + DB_URL_VARIABLE)
                                    .execute();

                            checkDbUrlChanged(dbUrlVar);
                        } catch (GoogleJsonResponseException ex) {
                            if (ex.getStatusCode() == 404) {
                                System.out.println(ex.getMessage());
                                continue;
                            }
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                }
            }).start();

        } catch (

        Exception e) {
            e.printStackTrace();
        }
    }

    private void checkDbUrlChanged(Variable dbUrlVar) throws IOException {
        if (dbUrlVar.getText() != null) {
            String newDBUrl = dbUrlVar.getText();

            if (!newDBUrl.equals(currentDBUrl)) {
                System.out.printf("Database URL changed [old=%s, new=%s]%n", currentDBUrl, newDBUrl);
                System.out.println("Applying new username and password");

                currentDBUrl = newDBUrl;
                currentDBUser = runtimeConfig.projects().configs().variables().get(apiEndpoint + DB_USER_VARIABLE)
                        .execute().getText();
                currentDBPassword = runtimeConfig.projects().configs().variables()
                        .get(apiEndpoint + DB_PWD_VARIABLE)
                        .execute().getText();
            }
        }
    }
}
