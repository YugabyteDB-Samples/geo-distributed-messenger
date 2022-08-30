package com.yugabyte.app.messenger.attachments;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FileUploadController {

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestBody byte[] file,
            @RequestParam("fileName") String fileName) {

        System.out.println("Receiving new file: " + fileName);

        File targetFile = new File("/tmp/" + fileName);

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(file);
            System.out.println("Succesfully stored file locally: " + targetFile.getAbsolutePath());
        } catch (IOException e) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.ok(targetFile.getAbsolutePath());
    }

    @GetMapping("/ping")
    public ResponseEntity<String> uploaderPingRequest() {
        return ResponseEntity.ok("The Attachments Service is Running!");
    }
}
