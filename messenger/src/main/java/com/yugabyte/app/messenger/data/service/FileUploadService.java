package com.yugabyte.app.messenger.data.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileUploadService {

    @Value("${kong.attachments.gateway:kong_gateway_not_configured}")
    private String uploaderUrl;

    public Optional<String> uploadAttachment(String fileName, String mimeType,
            InputStream inputStream) {

        HttpClient httpClient = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploaderUrl + "?fileName=" + fileName))
                .header("Content-Type", mimeType)
                .POST(HttpRequest.BodyPublishers.ofInputStream(new Supplier<InputStream>() {
                    @Override
                    public InputStream get() {
                        return inputStream;
                    }

                }))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpStatus.SC_OK) {
                String fileURL = response.body();
                System.out.println("Succesfully loaded file: " + fileURL);

                return Optional.of(fileURL);
            }

            System.err.println("File uploading failed [code=" + response.statusCode() +
                    ", body=" + response.body());

            return Optional.empty();
        } catch (IOException | InterruptedException e1) {
            e1.printStackTrace();
        }

        return Optional.empty();
    }
}
