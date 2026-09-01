package com.edusync;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GoogleDriveSyncTest {

    @Test
    public void testDriveConnection() throws Exception {
        Path credPath = Path.of("credentials.json");
        System.out.println("Checking credentials at: " + credPath.toAbsolutePath());
        
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credPath.toFile()))
                .createScoped(Collections.singletonList(DriveScopes.DRIVE));
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Drive driveService = new Drive.Builder(httpTransport, GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                .setApplicationName("EduSync-Campus-AI")
                .build();

        String folderId = "1BFI1zXYH9gaFGlXsrfjSm_WLki8vpA1p";
        System.out.println("Listing files in Drive folder: " + folderId);
        String q = "'" + folderId + "' in parents and trashed = false";
        FileList fileList = driveService.files().list()
                .setQ(q)
                .setSupportsAllDrives(true)
                .setIncludeItemsFromAllDrives(true)
                .setFields("files(id, name, mimeType, modifiedTime)")
                .execute();

        System.out.println("Files found in folder (" + (fileList.getFiles() != null ? fileList.getFiles().size() : 0) + "):");
        if (fileList.getFiles() != null) {
            fileList.getFiles().forEach(f -> System.out.println(" - " + f.getName() + " (ID: " + f.getId() + ", Mime: " + f.getMimeType() + ")"));
        }
        assertNotNull(fileList);
    }
}
