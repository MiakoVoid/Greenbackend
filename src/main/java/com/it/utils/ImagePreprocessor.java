package com.it.utils;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Locale;

public class ImagePreprocessor {
    private ImagePreprocessor() {
    }

    public static byte[] preprocessForOcr(byte[] inputBytes, String contentTypeOrFilename) {
        if (inputBytes == null || inputBytes.length == 0) {
            return inputBytes;
        }
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(inputBytes));
        } catch (Exception e) {
            return inputBytes;
        }
        if (image == null) {
            return inputBytes;
        }

        BufferedImage scaled = scaleToMaxWidth(image, 1600);
        BufferedImage gray = toGrayscale(scaled);
        BufferedImage enhanced = contrastStretch(gray);
        BufferedImage sharpened = sharpen(enhanced);

        String format = inferFormat(contentTypeOrFilename);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(sharpened, format, out);
            return out.toByteArray();
        } catch (Exception e) {
            return inputBytes;
        }
    }

    private static BufferedImage scaleToMaxWidth(BufferedImage src, int maxWidth) {
        if (src.getWidth() <= maxWidth) {
            return src;
        }
        double scale = maxWidth * 1.0 / src.getWidth();
        int newW = maxWidth;
        int newH = Math.max(1, (int) Math.round(src.getHeight() * scale));
        BufferedImage dst = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = dst.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(src, 0, 0, newW, newH, null);
        g2d.dispose();
        return dst;
    }

    private static BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(src, gray);
        return gray;
    }

    private static BufferedImage contrastStretch(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, src.getType());

        int min = 255;
        int max = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = src.getRaster().getSample(x, y, 0);
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }
        if (max <= min) {
            return src;
        }
        double scale = 255.0 / (max - min);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = src.getRaster().getSample(x, y, 0);
                int nv = (int) Math.round((v - min) * scale);
                if (nv < 0) nv = 0;
                if (nv > 255) nv = 255;
                out.getRaster().setSample(x, y, 0, nv);
            }
        }
        return out;
    }

    private static BufferedImage sharpen(BufferedImage src) {
        float[] kernel = new float[]{
                0f, -1f, 0f,
                -1f, 5f, -1f,
                0f, -1f, 0f
        };
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        op.filter(src, dst);
        return dst;
    }

    private static String inferFormat(String contentTypeOrFilename) {
        if (contentTypeOrFilename == null) {
            return "jpg";
        }
        String s = contentTypeOrFilename.toLowerCase(Locale.ROOT);
        if (s.contains("png") || s.endsWith(".png")) {
            return "png";
        }
        return "jpg";
    }
}

