package com.starshop.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.starshop.config.CloudinaryProperties;
import com.starshop.dto.UploadResult;
import com.starshop.entity.enums.MediaType;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.InvalidFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Kiểm tra FileStorageServiceImpl với Cloudinary giả lập (không gọi mạng).
 */
class FileStorageServiceImplTest {

    private static final byte[] JPEG = header(0xFF, 0xD8, 0xFF, 0xE0);
    private static final byte[] PNG = header(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
    private static final byte[] WEBP = header('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P');
    private static final byte[] MP4 = header(0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2');

    private Uploader uploader;
    private FileStorageServiceImpl service;

    @BeforeEach
    void setUp() throws IOException {
        Cloudinary cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/v1/starshop/products/abc.jpg",
                "public_id", "starshop/products/abc"));
        service = new FileStorageServiceImpl(cloudinary, configured());
    }

    // ---------------------------------------------------------------- hợp lệ

    @Test
    void uploadImage_acceptsJpgPngWebp() throws IOException {
        assertThat(service.uploadImage(file("hoa.jpg", "image/jpeg", JPEG), "products").publicId())
                .isEqualTo("starshop/products/abc");
        service.uploadImage(file("hoa.JPEG", "image/jpeg", JPEG), "products");
        service.uploadImage(file("hoa.png", "image/png", PNG), "products");
        service.uploadImage(file("hoa.webp", "image/webp", WEBP), "products");

        verify(uploader, times(4)).upload(any(), argThat(params ->
                "starshop/products".equals(params.get("folder")) && "image".equals(params.get("resource_type"))));
    }

    @Test
    void uploadVideo_acceptsMp4AndUsesVideoResourceType() throws IOException {
        UploadResult result = service.uploadVideo(file("review.mp4", "video/mp4", MP4), "reviews/videos");

        assertThat(result.mediaType()).isEqualTo(MediaType.VIDEO);
        verify(uploader).upload(any(), argThat(params ->
                "starshop/reviews/videos".equals(params.get("folder")) && "video".equals(params.get("resource_type"))));
    }

    // ------------------------------------------------------------ không hợp lệ

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> service.uploadImage(file("hoa.jpg", "image/jpeg", new byte[0]), "products"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("chọn file");
    }

    @Test
    void rejectsWrongExtension() {
        assertThatThrownBy(() -> service.uploadImage(file("hoa.gif", "image/gif", JPEG), "products"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("jpg, png, webp");
        assertThatThrownBy(() -> service.uploadVideo(file("clip.mov", "video/quicktime", MP4), "reviews"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("mp4");
    }

    @Test
    void rejectsImageUploadedAsVideoAndViceVersa() {
        assertThatThrownBy(() -> service.uploadVideo(file("hoa.jpg", "image/jpeg", JPEG), "reviews"))
                .isInstanceOf(InvalidFileException.class);
        assertThatThrownBy(() -> service.uploadImage(file("clip.mp4", "video/mp4", MP4), "reviews"))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsWrongContentType() {
        assertThatThrownBy(() -> service.uploadImage(file("hoa.jpg", "application/octet-stream", JPEG), "products"))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsRenamedFile_whenSignatureDoesNotMatch() {
        byte[] exe = header('M', 'Z', 0x90, 0x00);
        assertThatThrownBy(() -> service.uploadImage(file("virus.jpg", "image/jpeg", exe), "products"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("đổi đuôi");
        assertThatThrownBy(() -> service.uploadImage(file("hoa.png", "image/png", JPEG), "products"))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void rejectsOversizedFiles() {
        byte[] bigImage = Arrays.copyOf(JPEG, (int) FileStorageServiceImpl.MAX_IMAGE_BYTES + 1);
        assertThatThrownBy(() -> service.uploadImage(file("big.jpg", "image/jpeg", bigImage), "products"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("5MB");

        byte[] bigVideo = Arrays.copyOf(MP4, (int) FileStorageServiceImpl.MAX_VIDEO_BYTES + 1);
        assertThatThrownBy(() -> service.uploadVideo(file("big.mp4", "video/mp4", bigVideo), "reviews"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("30MB");
    }

    @Test
    void acceptsFileExactlyAtLimit() {
        byte[] image = Arrays.copyOf(JPEG, (int) FileStorageServiceImpl.MAX_IMAGE_BYTES);
        assertThat(service.uploadImage(file("max.jpg", "image/jpeg", image), "products")).isNotNull();
    }

    @Test
    void rejectsInvalidFolder() {
        assertThatThrownBy(() -> service.uploadImage(file("hoa.jpg", "image/jpeg", JPEG), "../secret"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------- cấu hình

    @Test
    void failsClearlyWhenCloudinaryNotConfigured() {
        Cloudinary cloudinary = mock(Cloudinary.class);
        FileStorageServiceImpl notConfigured = new FileStorageServiceImpl(cloudinary,
                new CloudinaryProperties("", "", "", "starshop"));

        assertThatThrownBy(() -> notConfigured.uploadImage(file("hoa.jpg", "image/jpeg", JPEG), "products"))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("chưa cấu hình Cloudinary");
        verifyNoInteractions(cloudinary);
    }

    @Test
    void wrapsCloudinaryIoError() throws IOException {
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("timeout"));

        assertThatThrownBy(() -> service.uploadImage(file("hoa.jpg", "image/jpeg", JPEG), "products"))
                .isInstanceOf(FileStorageException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    // ------------------------------------------------------------------ xóa

    @Test
    void delete_usesResourceTypeOfMedia() throws IOException {
        when(uploader.destroy(eq("starshop/reviews/v1"), anyMap())).thenReturn(Map.of("result", "ok"));

        service.delete("starshop/reviews/v1", MediaType.VIDEO);

        verify(uploader).destroy(eq("starshop/reviews/v1"), argThat(params -> "video".equals(params.get("resource_type"))));
    }

    @Test
    void delete_ignoresBlankIdAndMissingFile() throws IOException {
        service.delete("  ", MediaType.IMAGE);
        verify(uploader, never()).destroy(any(), anyMap());

        when(uploader.destroy(eq("gone"), anyMap())).thenReturn(Map.of("result", "not found"));
        service.delete("gone", MediaType.IMAGE);
    }

    @Test
    void delete_throwsWhenCloudinaryRefuses() throws IOException {
        when(uploader.destroy(eq("x"), anyMap())).thenReturn(Map.of("result", "error"));

        assertThatThrownBy(() -> service.delete("x", MediaType.IMAGE))
                .isInstanceOf(FileStorageException.class);
    }

    // --------------------------------------------------------------- helpers

    private static CloudinaryProperties configured() {
        return new CloudinaryProperties("demo", "key", "secret", "starshop");
    }

    private static MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    private static byte[] header(int... bytes) {
        byte[] data = new byte[Math.max(bytes.length, 16)];
        for (int i = 0; i < bytes.length; i++) {
            data[i] = (byte) bytes[i];
        }
        return data;
    }
}
