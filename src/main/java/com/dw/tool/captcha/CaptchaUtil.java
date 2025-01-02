package com.dw.tool.captcha;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yanggj
 * @version 1.0.0
 * @date 2025/1/2 16:28
 */
public class CaptchaUtil {

    // 存储验证码，键是用户ID，值是验证码
    private static Map<String, String> captchaStore = new HashMap<>();

    // 用于生成随机数的安全随机数生成器
    private static final SecureRandom random = new SecureRandom();

    /**
     * 生成验证码字符串
     *
     * @param length 验证码的长度
     * @param useLetters 是否使用字母
     * @param useNumbers 是否使用数字
     * @return 生成的验证码字符串
     * @throws IllegalArgumentException 如果没有选择字母或数字
     */
    public static String generateCaptchaString(int length, boolean useLetters, boolean useNumbers) {
        // 定义可用字符集
        // 字母
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        // 数字
        String numbers = "0123456789";
        StringBuilder allowedChars = new StringBuilder();

        // 根据参数决定可用字符集
        if (useLetters) {
            allowedChars.append(letters);  // 启用字母
        }
        if (useNumbers) {
            allowedChars.append(numbers);  // 启用数字
        }

        // 如果没有启用任何字符集，则抛出异常
        if (allowedChars.length() == 0) {
            throw new IllegalArgumentException("At least one type of characters must be enabled.");
        }

        // 生成指定长度的验证码
        StringBuilder captcha = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(allowedChars.length());  // 随机选择字符
            captcha.append(allowedChars.charAt(index));  // 添加到验证码字符串
        }

        return captcha.toString();
    }

    /**
     * 生成验证码图片
     *
     * @param captchaText 需要显示在图片上的验证码文本
     * @return 生成的验证码图片
     */
    public static BufferedImage generateCaptchaImage(String captchaText) {
        // 设置图片的宽度和高度
        int width = 160;
        int height = 60;

        // 创建一个空的图片对象
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // 获取 Graphics2D 对象，用于绘制图片
        Graphics2D graphics = image.createGraphics();

        // 设置背景色为浅灰色
        graphics.setColor(new Color(245, 245, 245));  // light gray background
        graphics.fillRect(0, 0, width, height);  // 填充整个背景

        // 设置字体，大小为40，使用加粗字体
        Font font = new Font("Arial", Font.BOLD, 40);
        graphics.setFont(font);

        // 设置文本颜色为黑色，并绘制验证码文本
        graphics.setColor(new Color(0, 0, 0));  // black text
        graphics.drawString(captchaText, 30, 45);  // 在图片上绘制验证码文本

        // 添加干扰线，增加验证码的难度
        addNoise(graphics, width, height);

        // 完成绘制，释放资源
        graphics.dispose();

        return image;
    }

    /**
     * 添加干扰线到验证码图片上
     *
     * @param graphics Graphics2D对象，用于绘制干扰线
     * @param width 图片的宽度
     * @param height 图片的高度
     */
    private static void addNoise(Graphics2D graphics, int width, int height) {
        // 设置干扰线颜色
        graphics.setColor(new Color(100, 100, 100));  // gray lines

        // 生成5条随机的干扰线
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(width);  // 随机起点x坐标
            int y1 = random.nextInt(height);  // 随机起点y坐标
            int x2 = random.nextInt(width);  // 随机终点x坐标
            int y2 = random.nextInt(height);  // 随机终点y坐标
            graphics.drawLine(x1, y1, x2, y2);  // 绘制干扰线
        }
    }

    /**
     * 生成并存储验证码，供用户验证
     *
     * @param userId 用户ID，用于存储验证码
     * @param length 验证码的长度
     * @return 生成的验证码字符串
     */
    public static String createCaptcha(String userId, int length) {
        // 生成一个随机验证码字符串
        String captcha = generateCaptchaString(length, true, true);

        // 存储验证码，用户ID作为键，验证码作为值
        captchaStore.put(userId, captcha);  // 存储验证码

        return captcha;
    }

    /**
     * 校验用户输入的验证码是否正确
     *
     * @param userId 用户ID，用于查找存储的验证码
     * @param userInputCaptcha 用户输入的验证码
     * @return 验证码是否正确
     */
    public static boolean validateCaptcha(String userId, String userInputCaptcha) {
        // 从存储中获取验证码
        String storedCaptcha = captchaStore.get(userId);

        // 如果验证码不存在，返回false
        if (storedCaptcha == null) {
            return false;
        }

        // 比较存储的验证码与用户输入的验证码，忽略大小写
        return storedCaptcha.equalsIgnoreCase(userInputCaptcha);
    }

    /**
     * 将验证码图片转换为字节数组，方便传输或存储
     *
     * @param image 生成的验证码图片
     * @param formatName 图片格式名称，如"PNG"、"JPEG"
     * @return 转换后的字节数组
     * @throws IOException 图片转换过程中可能抛出的异常
     */
    public static byte[] imageToByteArray(BufferedImage image, String formatName) throws IOException {
        // 使用ByteArrayOutputStream来将图片内容写入字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 将图片写入字节数组输出流
        ImageIO.write(image, formatName, baos);

        // 返回字节数组
        return baos.toByteArray();
    }

    public static void main(String[] args) {
        try {
            // 示例生成验证码
            String captchaText = generateCaptchaString(6, true, true);  // 生成6位字母+数字验证码
            BufferedImage captchaImage = generateCaptchaImage(captchaText);  // 生成验证码图片
            byte[] imageBytes = imageToByteArray(captchaImage, "PNG");  // 将图片转为字节数组

            // 输出验证码文本和图片的字节数组大小
            System.out.println("验证码文本: " + captchaText);
            System.out.println("验证码图片大小: " + imageBytes.length + " bytes");

            // 校验验证码
            String userId = "user123";  // 假设用户ID为user123
            createCaptcha(userId, 6);  // 为该用户生成并存储验证码
            boolean isValid = validateCaptcha(userId, captchaText);
            System.out.println("验证码校验结果: " + isValid);
        } catch (IOException e) {
            e.printStackTrace();  // 处理IO异常
        }
    }
}
