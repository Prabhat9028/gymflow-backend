package com.gymflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.UUID;

@Service @Slf4j
public class StorageService {

    @Value("${app.storage.mode:local}") private String storageMode;
    @Value("${app.storage.s3.bucket:}") private String s3Bucket;
    @Value("${app.storage.s3.region:ap-south-1}") private String s3Region;
    @Value("${app.storage.s3.access-key:}") private String s3AccessKey;
    @Value("${app.storage.s3.secret-key:}") private String s3SecretKey;

    private S3Client s3Client;
    private static final String LOCAL_DIR = "/app/uploads/signage";

    public record UploadResult(String fileUrl, String checksum, long fileSize) {}

    public UploadResult upload(MultipartFile file, String contentType) throws IOException {
        byte[] bytes = file.getBytes();
        String checksum = calculateMD5(bytes);
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : (contentType.equalsIgnoreCase("IMAGE") ? ".jpg" : ".mp4");
        String storedName = UUID.randomUUID() + ext;

        String fileUrl;
        if (isS3Enabled()) {
            fileUrl = uploadToS3(storedName, bytes, file.getContentType());
        } else {
            fileUrl = uploadToLocal(storedName, bytes);
        }

        log.info("Uploaded {} ({} bytes) -> {}", originalName, bytes.length, fileUrl);
        return new UploadResult(fileUrl, checksum, bytes.length);
    }

    public void delete(String fileUrl) {
        if (fileUrl == null) return;
        try {
            if (fileUrl.contains("amazonaws.com") || fileUrl.contains("s3.")) {
                deleteFromS3(fileUrl);
            } else if (fileUrl.startsWith("/api/signage/media/")) {
                String fn = fileUrl.replace("/api/signage/media/", "");
                File f = new File(LOCAL_DIR, fn);
                if (f.exists()) { f.delete(); log.info("Deleted local file: {}", fn); }
            }
        } catch (Exception e) {
            log.warn("Failed to delete file: {}", e.getMessage());
        }
    }

    private String uploadToLocal(String storedName, byte[] bytes) throws IOException {
        File dir = new File(LOCAL_DIR);
        dir.mkdirs();
        File dest = new File(dir, storedName);
        try (FileOutputStream fos = new FileOutputStream(dest)) { fos.write(bytes); }
        return "/api/signage/media/" + storedName;
    }

    private String uploadToS3(String key, byte[] bytes, String contentType) {
        S3Client client = getS3Client();
        String s3Key = "signage/" + key;

        client.putObject(PutObjectRequest.builder()
            .bucket(s3Bucket)
            .key(s3Key)
            .contentType(contentType)
            .build(), RequestBody.fromBytes(bytes));

        // Make object public readable
        try {
            client.putObjectAcl(PutObjectAclRequest.builder()
                .bucket(s3Bucket).key(s3Key)
                .acl(ObjectCannedACL.PUBLIC_READ).build());
        } catch (Exception e) {
            log.warn("Could not set public ACL (bucket may use policy instead): {}", e.getMessage());
        }

        String url = String.format("https://%s.s3.%s.amazonaws.com/%s", s3Bucket, s3Region, s3Key);
        log.info("Uploaded to S3: {}", url);
        return url;
    }

    private void deleteFromS3(String fileUrl) {
        try {
            // Extract key from URL
            String key = fileUrl.substring(fileUrl.indexOf("signage/"));
            getS3Client().deleteObject(DeleteObjectRequest.builder()
                .bucket(s3Bucket).key(key).build());
            log.info("Deleted from S3: {}", key);
        } catch (Exception e) {
            log.warn("S3 delete failed: {}", e.getMessage());
        }
    }

    private boolean isS3Enabled() {
        return "s3".equalsIgnoreCase(storageMode)
            && s3AccessKey != null && !s3AccessKey.isBlank()
            && s3SecretKey != null && !s3SecretKey.isBlank()
            && s3Bucket != null && !s3Bucket.isBlank();
    }

    private S3Client getS3Client() {
        if (s3Client == null) {
            s3Client = S3Client.builder()
                .region(Region.of(s3Region))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3AccessKey, s3SecretKey)))
                .build();
        }
        return s3Client;
    }

    private String calculateMD5(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(bytes);
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) { return UUID.randomUUID().toString(); }
    }
}
