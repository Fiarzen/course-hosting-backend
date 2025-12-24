package com.jeremy.courses;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${aws.s3.bucket-name:}")
    private String bucketName;

    @Value("${aws.s3.enabled:false}")
    private boolean s3Enabled;

    @Value("${aws.region:eu-west-1}")
    private String awsRegion;

    private S3Client s3Client;

    private S3Client getS3Client() {
        if (s3Client == null && s3Enabled) {
            try {
                s3Client = S3Client.builder()
                        .region(Region.of(awsRegion))
                        .build();
            } catch (Exception e) {
                System.err.println("Failed to initialize S3 client: " + e.getMessage());
            }
        }
        return s3Client;
    }

    public String uploadPdf(MultipartFile pdfFile) throws IOException {
        if (pdfFile == null || pdfFile.isEmpty()) {
            return null;
        }

        String filename = UUID.randomUUID() + "_" + pdfFile.getOriginalFilename();

        // Use S3 if enabled and configured, otherwise use local storage
        S3Client client = getS3Client();
        if (s3Enabled && bucketName != null && !bucketName.isEmpty() && client != null) {
            try {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key("pdfs/" + filename)
                        .contentType("application/pdf")
                        .build();

                client.putObject(putObjectRequest, RequestBody.fromInputStream(
                        pdfFile.getInputStream(), pdfFile.getSize()));

                // Return public S3 URL
                return String.format("https://%s.s3.%s.amazonaws.com/pdfs/%s", bucketName, awsRegion, filename);
            } catch (Exception e) {
                System.err.println("Failed to upload to S3, falling back to local storage: " + e.getMessage());
                // Fall through to local storage
            }
        }

        // Local storage fallback
        String uploadRoot = "uploads";
        Path pdfDir = Paths.get(uploadRoot, "pdfs");
        Files.createDirectories(pdfDir);
        Path pdfPath = pdfDir.resolve(filename);
        Files.copy(pdfFile.getInputStream(), pdfPath, StandardCopyOption.REPLACE_EXISTING);
        return "/files/pdfs/" + filename;
    }
}

