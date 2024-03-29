package com.yugabyte.app.messenger.attachments.service;

import java.util.Optional;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import io.minio.UploadObjectArgs;
import static com.yugabyte.app.messenger.attachments.service.ObjectStorageService.*;

class ObjectStorageMinioImpl implements ObjectStorage {
    private String storageHost;

    private int storagePort;

    private String storageUser;

    private String storagePassword;

    private volatile MinioClient client;

    public ObjectStorageMinioImpl(String storageHost, int storagePort, String storageUser, String storagePassword) {
        this.storageHost = storageHost;
        this.storagePort = storagePort;
        this.storageUser = storageUser;
        this.storagePassword = storagePassword;
    }

    @Override
    public Optional<String> storeFile(String filePath, String fileName, String contentType) {
        System.out.printf("Uploading file %s %n", fileName);

        if (client == null) {
            initClient();
        }

        String objectName = generateUniqueObjectName(fileName);

        try {
            client.uploadObject(UploadObjectArgs.builder()
                    .bucket(PICTURES_BUCKET)
                    .filename(filePath)
                    .contentType(contentType)
                    .object(objectName).build());
        } catch (Exception e) {
            System.err.println("Failed uploading file: " + fileName);
            e.printStackTrace();

            return Optional.empty();
        }

        String fullObjectName = storageHost + ":" + storagePort + "/" + PICTURES_BUCKET + "/" + objectName;

        System.out.printf("Succesfully uploaded file %s to %s %n", fileName, fullObjectName);

        return Optional.of(fullObjectName);
    }

    private synchronized boolean initClient() {
        if (client == null) {
            client = MinioClient.builder()
                    .endpoint(storageHost, storagePort, false)
                    .credentials(storageUser, storagePassword)
                    .build();

            try {
                if (!client.bucketExists(BucketExistsArgs.builder().bucket(PICTURES_BUCKET).build())) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(PICTURES_BUCKET).build());

                    StringBuilder builder = new StringBuilder();
                    builder.append("{\n");
                    builder.append("    \"Statement\": [\n");
                    builder.append("        {\n");
                    builder.append("            \"Action\": \"s3:GetObject\",\n");
                    builder.append("            \"Effect\": \"Allow\",\n");
                    builder.append("            \"Principal\": \"*\",\n");
                    builder.append("            \"Resource\": \"arn:aws:s3:::" + PICTURES_BUCKET + "/*\"\n");
                    builder.append("        }\n");
                    builder.append("    ],\n");
                    builder.append("    \"Version\": \"2012-10-17\"\n");
                    builder.append("}\n");

                    client.setBucketPolicy(
                            SetBucketPolicyArgs.builder().bucket(PICTURES_BUCKET).config(builder.toString()).build());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return false;
            }
        }

        return true;
    }

}
