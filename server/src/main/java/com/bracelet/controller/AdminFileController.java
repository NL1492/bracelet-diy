package com.bracelet.controller;

import com.bracelet.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;

/**
 * 管理端文件上传 — 灵感演示视频
 * <p>存储目录：E:/over/image/uploads/video/（vite publicDir=image，dev 模式经 /uploads/video/** 直接可访问）
 * <p>权限：位于 /api/bracelet/admin/** 下，由 AdminInterceptor 校验管理员身份
 */
@Slf4j
@RestController
@RequestMapping("/api/bracelet/admin")
public class AdminFileController {

    /** 允许的视频扩展名 */
    private static final Set<String> ALLOWED_EXT = Set.of("mp4", "webm", "mov", "avi", "mkv");

    /** 单个视频上限 300 MB（与前端提示一致） */
    private static final long MAX_SIZE = 300L * 1024 * 1024;

    private static final SecureRandom RANDOM = new SecureRandom();

    @PostMapping("/upload/video")
    public Result<Map<String, String>> uploadVideo(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择视频文件");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.fail("视频不能超过 300 MB，请压缩后再上传");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot >= 0 ? original.substring(dot + 1).toLowerCase() : "";
        if (!ALLOWED_EXT.contains(ext)) {
            return Result.fail("仅支持 mp4 / webm / mov / avi / mkv 格式视频");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("video/")) {
            return Result.fail("文件内容不是视频，请重新选择");
        }

        // 存储目录（image/uploads/video/，与 vite publicDir 对齐）
        File dir = new File("E:/over/image/uploads/video");
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("创建视频上传目录失败: {}", dir.getAbsolutePath());
            return Result.fail("服务器创建上传目录失败");
        }

        // 文件名：video_{时间戳}_{4位随机}.{ext}，避免冲突与路径穿越
        String filename = "video_" + System.currentTimeMillis() + "_"
                + String.format("%04x", RANDOM.nextInt(0x10000)) + "." + ext;
        try {
            file.transferTo(new File(dir, filename));
        } catch (Exception e) {
            log.error("视频保存失败", e);
            return Result.fail("视频保存失败，请重试");
        }

        String url = "/uploads/video/" + filename;
        log.info("演示视频已上传: {} ({} KB) → {}", original, file.getSize() / 1024, url);
        return Result.ok("上传成功", Map.of("url", url));
    }
}
