package com.yugabyte.app.messenger.attachments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.yugabyte.app.messenger.attachments.service.ObjectStorageService;

@Controller
public class FileUploadController {
    @Autowired
    private ObjectStorageService storage;

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestBody byte[] file,
            @RequestHeader("Content-Type") String contentType,
            @RequestParam("fileName") String fileName) {

        System.out.println("Receiving new file: " + fileName);

        File targetFile = new File("/tmp/" + fileName);

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(file);
            System.out.println("Succesfully stored file locally: " + targetFile.getAbsolutePath());

            Optional<String> objectUrl = storage.uploadFile(targetFile.getAbsolutePath(), fileName, contentType);

            if (objectUrl.isPresent()) {
                return ResponseEntity.ok(objectUrl.get());
            }
        } catch (IOException e) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            e.printStackTrace();
        } finally {
            targetFile.delete();
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload the file" +
                "to the object store");
    }

    @GetMapping("/ping")
    public ResponseEntity<String> uploaderPingRequest() {
        return ResponseEntity.ok("The Attachments Service is Running!");
    }
}
