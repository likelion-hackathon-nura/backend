package org.example.nura.global.infra.s3;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile multipartFile, String dirName) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BaseException(ErrorCode.INVALID_INPUT_VALUE, "업로드할 파일이 없습니다.");
        }

        String extension = extractExtension(multipartFile.getOriginalFilename());
        String storeFileName = dirName + "/" + UUID.randomUUID() + extension;

        try (InputStream inputStream = multipartFile.getInputStream()) {
            ObjectMetadata metadata = ObjectMetadata.builder()
                    .contentType(multipartFile.getContentType())
                    .build();

            var resource = s3Template.upload(bucket, storeFileName, inputStream, metadata);
            return resource.getURL().toString();
        } catch (IOException e) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "S3 파일 업로드에 실패했습니다.");
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return ".jpg"; // 기본 확장자
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}