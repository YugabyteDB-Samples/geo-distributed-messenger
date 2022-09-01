package com.yugabyte.app.messenger.attachments.service;

import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ObjectStorageService {
    final static String PICTURES_BUCKET = "pictures";

    private String storageImpl;

    private String storageHost;

    private int storagePort;

    private String storageUser;

    private String storagePassword;

    private String googleStorageProjectId;

    private ObjectStorage storage;

    public ObjectStorageService(
            @Value("${object-storage.impl:minio}") String storageImpl,
            @Value("${object-storage.minio.host:}") String storageHost,
            @Value("${object-storage.minio.port:0}") int storagePort,
            @Value("${object-storage.minio.user:}") String storageUser,
            @Value("${object-storage.minio.password:}") String storagePassword,
            @Value("${object-storage.google-storage.project-id:}") String googleStorageProjectId) {
        this.storageImpl = storageImpl;
        this.storageHost = storageHost;
        this.storagePort = storagePort;
        this.storageUser = storageUser;
        this.storagePassword = storagePassword;
        this.googleStorageProjectId = googleStorageProjectId;

        initStorageImpl();
    }

    public Optional<String> uploadFile(String filePath, String fileName, String contentType) {
        return storage.storeFile(filePath, fileName, contentType);
    }

    private void initStorageImpl() {
        if (storage != null)
            return;

        storageImpl = storageImpl.trim().toLowerCase();

        switch (storageImpl) {
            case "minio":
                storage = new ObjectStorageMinioImpl(storageHost, storagePort,
                        storageUser, storagePassword);
                System.out.println("Using Minio storage");
                break;
            case "google-storage":
                storage = new ObjectStorageGoogleCloudImpl(googleStorageProjectId);
                System.out.println("Using Google Cloud Storage");
                break;
            default:
                throw new IllegalArgumentException("Unsupported object storage type: "
                        + storageImpl);
        }
    }

    static String generateUniqueObjectName(String fileName) {
        String[] fileStructure = fileName.split("\\.");

        String objectName = fileStructure[0] + "-" +
                RandomStringUtils.randomAlphanumeric(15) +
                "." + fileStructure[1];

        return objectName;
    }

}
