package com.yugabyte.app.messenger.attachments.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;

import com.google.api.gax.paging.Page;
import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.StorageRoles;

import static com.yugabyte.app.messenger.attachments.service.ObjectStorageService.*;

public class ObjectStorageGoogleCloudImpl implements ObjectStorage {
    private volatile Storage client;

    private String projectId;

    private String bucketName;

    public ObjectStorageGoogleCloudImpl(String projectId) {
        this.projectId = projectId;
    }

    @Override
    public Optional<String> storeFile(String filePath, String fileName, String contentType) {
        if (client == null) {
            initClient();
        }

        String objectName = generateUniqueObjectName(fileName);

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        try {
            client.create(blobInfo, Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            System.err.println("Failed to load the file:" + fileName);
            e.printStackTrace();

            return Optional.empty();
        }

        System.out.printf(
                "File %s uploaded to bucket %s as %s %n", filePath, bucketName, objectName);

        String objectFullAddress = "http://storage.googleapis.com/" + bucketName + "/" + objectName;

        System.out.println("Picture public address: " + objectFullAddress);

        return Optional.of(objectFullAddress);
    }

    private synchronized void initClient() {
        if (client != null)
            return;

        client = StorageOptions.newBuilder().setProjectId(projectId).build().getService();

        System.out.printf("Succesfully connected to Google Storage, project %s %n", projectId);

        Page<Bucket> buckets = client.list();

        for (Bucket bucket : buckets.iterateAll()) {
            if (bucket.getName().startsWith(PICTURES_BUCKET)) {
                bucketName = bucket.getName();
                System.out.printf("Bucket %s already exists. Skipping creation %n", bucketName);
                return;
            }
        }

        String bucketRandName = RandomStringUtils.randomAlphanumeric(25).toLowerCase();

        bucketName = PICTURES_BUCKET + "-" + bucketRandName;

        Bucket bucket = client.create(BucketInfo.of(bucketName));

        System.out.printf("Bucket %s created.%n", bucket.getName());

        Policy originalPolicy = client.getIamPolicy(bucketName);
        client.setIamPolicy(
                bucketName,
                originalPolicy
                        .toBuilder()
                        .addIdentity(StorageRoles.objectViewer(), Identity.allUsers())
                        .build());

        System.out.printf("Bucket %s is now publicly readable %n", bucketName);
    }
}
