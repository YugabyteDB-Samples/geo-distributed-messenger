package com.yugabyte.app.messenger.attachments.service;

import java.util.Optional;

public interface ObjectStorage {
    Optional<String> storeFile(String filePath, String fileName, String contentType);

    
}
