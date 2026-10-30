package com.fenglin.springboottest.service.impl;

import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.service.CaptchaService;
import com.fenglin.springboottest.vo.CaptchaVO;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码实现：纯内存、无外部依赖（JDK 自带 BufferedImage / ImageIO）。
 * <p>
 * 设计要点：
 * 1. 令牌为服务端随机 UUID，前端无法伪造，可作为"服务端可信标识"；
 * 2. 答案只存服务端内存，5 分钟过期；
 * 3. verify 命中后立刻删除令牌，保证一次性使用（防重放）。
 *
 * @author fenglin
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    /** 图片尺寸与字符数 */
    private static final int WIDTH = 120;
    private static final int HEIGHT = 42;
    private static final int LENGTH = 4;

    /** 过期时间（秒），与下发验证码的 300s 同级 */
    private static final long TTL_SECONDS = 300;

    /** 去掉易混淆字符：0/O、1/l/I、8/B 等 */
    private static final String CHAR_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public CaptchaVO generate() {
        purgeExpired();
        String token = UUID.randomUUID().toString().replace("-", "");
        String code = randomText();
        store.put(token, new Entry(code, Instant.now().plusSeconds(TTL_SECONDS)));
        return new CaptchaVO(token, render(code));
    }

    @Override
    public boolean verify(String token, String code) {
        if (token == null || code == null) {
            return false;
        }
        // 原子取出并删除：保证同一令牌只能成功校验一次
        Entry entry = store.remove(token);
        if (entry == null) {
            return false;
        }
        if (entry.expireAt.isBefore(Instant.now())) {
            return false;
        }
        return entry.code.equalsIgnoreCase(code.trim());
    }

    /** 生成指定长度的随机字符 */
    private String randomText() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(CHAR_POOL.charAt(random.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    /** 把验证码渲染成 base64 PNG（无显示环境亦可运行，ImageIO 走纯软件渲染） */
    private String render(String code) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        // 背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线：降低 OCR 自动识别成功率
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                    random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }

        // 文字（轻微随机基线抖动 + 深色，便于人眼辨认）
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(new Color(20, 20, 20));
        FontMetrics fm = g.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(code)) / 2;
        int y = (HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(code, x, y);
        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new BizException("验证码图片生成失败");
        }
    }

    /** 惰性清理过期条目，避免内存无限增长 */
    private void purgeExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> e.getValue().expireAt.isBefore(now));
    }

    /** 一条验证码记录：答案 + 过期时刻 */
    private static class Entry {
        final String code;
        final Instant expireAt;

        Entry(String code, Instant expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
