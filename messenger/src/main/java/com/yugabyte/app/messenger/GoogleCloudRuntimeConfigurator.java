package com.yugabyte.app.messenger;

import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
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
import com.yugabyte.app.messenger.data.DynamicDataSource;

@Service
@Scope("singleton")
@DependsOn({ "dynamicDataSource", "dataGenerator" })
public class GoogleCloudRuntimeConfigurator {
    private static final String CONFIG_NAME = "messaging-microservice-settings";
    private final static String DB_URL_VARIABLE = "spring.datasource.url";
    private final static String DB_USER_VARIABLE = "spring.datasource.username";
    private final static String DB_PWD_VARIABLE = "spring.datasource.password";
    private final static String YUGABYTEDB_CONNECTION_TYPE = "yugabytedb.connection.type";

    private final static String CONFIGURATOR_URL = "projects/%s/configs/%s/variables/%s/";

    private CloudRuntimeConfig runtimeConfig;
    private String apiEndpoint;

    private String currentDBUrl;
    private String currentDBUser;
    private String currentDBPassword;
    private String yugabyteConnType;

    @Autowired
    private DynamicDataSource dynamicDataSource;

    public GoogleCloudRuntimeConfigurator(
            @Value("${spring.datasource.url}") String currentDBUrl,
            @Value("${spring.datasource.username}") String currentDBUser,
            @Value("${spring.datasource.password}") String currentDBPassword,
            @Value("${yugabytedb.connection.type:standard}") String yugabyteDBConnectionType) {
        this.currentDBUrl = currentDBUrl;
        this.currentDBUser = currentDBUser;
        this.currentDBPassword = currentDBPassword;
        this.yugabyteConnType = yugabyteDBConnectionType;

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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkDbUrlChanged(Variable dbUrlVar) throws IOException {
        if (dbUrlVar.getText() != null) {
            String newDBUrl = dbUrlVar.getText();

            if (!newDBUrl.equals(currentDBUrl)) {
                System.out.printf("Database URL changed [old=%s, new=%s]%n", currentDBUrl, newDBUrl);
                currentDBUrl = newDBUrl;

                try {
                    String newUserName = runtimeConfig.projects().configs().variables()
                            .get(apiEndpoint + DB_USER_VARIABLE)
                            .execute().getText();

                    if (!newUserName.equals(currentDBUser)) {
                        System.out.printf("Database username changed [old=%s, new=%s]%n", currentDBUser, newUserName);
                        currentDBUser = newUserName;
                    }
                } catch (GoogleJsonResponseException e) {
                    if (e.getStatusCode() == 404) {
                        System.out.printf("The username is not set, using the old one %s%n", currentDBUser);
                    }
                }

                try {
                    String newPassword = runtimeConfig.projects().configs().variables()
                            .get(apiEndpoint + DB_PWD_VARIABLE)
                            .execute().getText();

                    if (!newPassword.equals(currentDBPassword)) {
                        System.out.println("Database password changed");
                        currentDBPassword = newPassword;
                    }
                } catch (GoogleJsonResponseException e) {
                    if (e.getStatusCode() == 404) {
                        System.out.println("The password is not set, using the old one");
                    }
                }

                try {
                    String newDBConnectionType = runtimeConfig.projects().configs().variables()
                            .get(apiEndpoint + YUGABYTEDB_CONNECTION_TYPE)
                            .execute().getText();

                    switch (newDBConnectionType) {
                        case "standard":
                        case "replica":
                        case "geo":
                            System.out.printf("YugabyteDB connection type changed [old=%s, new=%s]%n",
                                    yugabyteConnType,
                                    newDBConnectionType);
                            yugabyteConnType = newDBConnectionType;
                            break;
                        default:
                            System.err.printf("Unrecognized yugabytedb connection type %s, " +
                                    "using the old one %s%n", newDBConnectionType, yugabyteConnType);
                    }

                    dynamicDataSource.createNewDataSource(currentDBUrl, currentDBUser, currentDBPassword,
                            yugabyteConnType);

                } catch (GoogleJsonResponseException e) {
                    if (e.getStatusCode() == 404) {
                        System.out.printf("The YugabyteDB connection type is not set, using the old one %s%n",
                                yugabyteConnType);
                    }
                }
            }
        }
    }
}
