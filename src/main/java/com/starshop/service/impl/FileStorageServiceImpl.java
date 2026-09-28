package com.starshop.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.starshop.config.CloudinaryProperties;
import com.starshop.dto.UploadResult;
import com.starshop.entity.enums.MediaType;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.InvalidFileException;
import com.starshop.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Upload/xóa file trên Cloudinary.
 * File được kiểm tra 3 lớp trước khi upload: đuôi file, Content-Type và chữ ký byte đầu file
 * (magic number) — để chặn trường hợp đổi tên file .exe thành .jpg.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    static final long MAX_IMAGE_BYTES = DataSize.ofMegabytes(5).toBytes();
    static final long MAX_VIDEO_BYTES = DataSize.ofMegabytes(30).toBytes();

    private static final Pattern FOLDER_PATTERN = Pattern.compile("[a-z0-9-]+(/[a-z0-9-]+)*");
    private static final int SIGNATURE_LENGTH = 12;

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    @Override
    public UploadResult uploadImage(MultipartFile file, String folder) {
        validate(file, MediaType.IMAGE, MAX_IMAGE_BYTES);
        return upload(file, folder, MediaType.IMAGE);
    }

    @Override
    public UploadResult uploadVideo(MultipartFile file, String folder) {
        validate(file, MediaType.VIDEO, MAX_VIDEO_BYTES);
        return upload(file, folder, MediaType.VIDEO);
    }

    @Override
    public void delete(String publicId, MediaType mediaType) {
        if (!StringUtils.hasText(publicId)) {
            return;
        }
        ensureConfigured();
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", resourceType(mediaType),
                    "invalidate", true));
            Object status = result.get("result");
            if ("not found".equals(status)) {
                log.warn("Xóa file Cloudinary: không tìm thấy {}", publicId);
            } else if (!"ok".equals(status)) {
                throw new FileStorageException("Không xóa được file trên Cloudinary (kết quả: " + status + ").");
            }
        } catch (IOException e) {
            throw new FileStorageException("Không kết nối được Cloudinary để xóa file.", e);
        }
    }

    // ------------------------------------------------------------------ upload

    private UploadResult upload(MultipartFile file, String folder, MediaType mediaType) {
        ensureConfigured();
        String targetFolder = buildFolder(folder);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", targetFolder,
                    "resource_type", resourceType(mediaType),
                    "unique_filename", true,
                    "overwrite", false));
            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            if (url == null || publicId == null) {
                throw new FileStorageException("Cloudinary không trả về đường dẫn file.");
            }
            return new UploadResult(url, publicId, mediaType);
        } catch (IOException e) {
            throw new FileStorageException("Tải file lên thất bại, vui lòng thử lại.", e);
        }
    }

    private void ensureConfigured() {
        if (!properties.isConfigured()) {
            throw new FileStorageException("Hệ thống chưa cấu hình Cloudinary, chưa thể lưu file.");
        }
    }

    private String buildFolder(String folder) {
        if (folder == null || !FOLDER_PATTERN.matcher(folder).matches()) {
            throw new IllegalArgumentException("Tên thư mục upload không hợp lệ: " + folder);
        }
        String root = StringUtils.hasText(properties.folder()) ? properties.folder() : "starshop";
        return root + "/" + folder;
    }

    private static String resourceType(MediaType mediaType) {
        return mediaType == MediaType.VIDEO ? "video" : "image";
    }

    // -------------------------------------------------------------- validation

    private void validate(MultipartFile file, MediaType mediaType, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Vui lòng chọn file.");
        }
        String allowedNames = AllowedFile.describe(mediaType);
        if (file.getSize() > maxBytes) {
            throw new InvalidFileException("File \"" + file.getOriginalFilename() + "\" vượt quá "
                    + DataSize.ofBytes(maxBytes).toMegabytes() + "MB.");
        }

        AllowedFile allowed = AllowedFile.byExtension(extensionOf(file.getOriginalFilename()), mediaType);
        if (allowed == null) {
            throw new InvalidFileException("Chỉ chấp nhận file " + allowedNames + ".");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!allowed.contentTypes.contains(contentType)) {
            throw new InvalidFileException("Định dạng file không hợp lệ, chỉ chấp nhận " + allowedNames + ".");
        }
        if (!allowed.matchesSignature(readHeader(file))) {
            throw new InvalidFileException("Nội dung file không đúng định dạng " + allowedNames
                    + " (file có thể bị đổi đuôi).");
        }
    }

    private static String extensionOf(String filename) {
        String ext = StringUtils.getFilenameExtension(filename);
        return ext == null ? "" : ext.toLowerCase(Locale.ROOT);
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(SIGNATURE_LENGTH);
        } catch (IOException e) {
            throw new InvalidFileException("Không đọc được file, vui lòng thử lại.");
        }
    }

    /**
     * Các loại file được phép và chữ ký byte đầu (magic number) tương ứng.
     */
    enum AllowedFile {
        JPEG(MediaType.IMAGE, Set.of("jpg", "jpeg"), Set.of("image/jpeg", "image/jpg")) {
            @Override
            boolean matchesSignature(byte[] h) {
                return startsWith(h, 0, 0xFF, 0xD8, 0xFF);
            }
        },
        PNG(MediaType.IMAGE, Set.of("png"), Set.of("image/png")) {
            @Override
            boolean matchesSignature(byte[] h) {
                return startsWith(h, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
            }
        },
        WEBP(MediaType.IMAGE, Set.of("webp"), Set.of("image/webp")) {
            @Override
            boolean matchesSignature(byte[] h) {
                return startsWith(h, 0, 'R', 'I', 'F', 'F') && startsWith(h, 8, 'W', 'E', 'B', 'P');
            }
        },
        MP4(MediaType.VIDEO, Set.of("mp4"), Set.of("video/mp4")) {
            @Override
            boolean matchesSignature(byte[] h) {
                return startsWith(h, 4, 'f', 't', 'y', 'p');
            }
        };

        final MediaType mediaType;
        final Set<String> extensions;
        final Set<String> contentTypes;

        AllowedFile(MediaType mediaType, Set<String> extensions, Set<String> contentTypes) {
            this.mediaType = mediaType;
            this.extensions = extensions;
            this.contentTypes = contentTypes;
        }

        abstract boolean matchesSignature(byte[] header);

        static AllowedFile byExtension(String extension, MediaType mediaType) {
            return Arrays.stream(values())
                    .filter(f -> f.mediaType == mediaType && f.extensions.contains(extension))
                    .findFirst()
                    .orElse(null);
        }

        /** Ví dụ "jpg, png, webp" hoặc "mp4" – dùng trong thông báo lỗi. */
        static String describe(MediaType mediaType) {
            return mediaType == MediaType.VIDEO ? "mp4" : "jpg, png, webp";
        }

        private static boolean startsWith(byte[] data, int offset, int... expected) {
            if (data.length < offset + expected.length) {
                return false;
            }
            for (int i = 0; i < expected.length; i++) {
                if ((data[offset + i] & 0xFF) != expected[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
