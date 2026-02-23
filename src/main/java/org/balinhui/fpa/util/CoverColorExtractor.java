package org.balinhui.fpa.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CoverColorExtractor {
    private static final Logger logger = LogManager.getLogger(CoverColorExtractor.class);

    private CoverColorExtractor() {}

    public static String extractOneRGBColor(byte[] data) {
        Color color = extractColors(data, 1).getFirst();
        String colorStr = color.getRed() + ", " +
                color.getGreen() + ", " +
                color.getBlue();
        if (color.getRed() > 180 && color.getGreen() > 180 && color.getBlue() > 180)
            colorStr = "131, 131, 131";
        logger.trace("提取封面颜色: ({})", colorStr);
        return colorStr;
    }

    public static List<Color> extractColors(byte[] data, int colorCount) {
        try(ByteArrayInputStream imageStream = new ByteArrayInputStream(data)) {
            BufferedImage image = ImageIO.read(imageStream);
            return medianCut(getPixels(image), colorCount);
        } catch (IOException e) {
            logger.fatal(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static List<Color> getPixels(BufferedImage image) {
        List<Color> pixels = new ArrayList<>();
        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = 0; x < width; x += 5) {
            for (int y = 0; y < height; y += 5) {
                int rgb = image.getRGB(x, y);
                pixels.add(new Color(rgb));
            }
        }
        return pixels;
    }

    private static List<Color> medianCut(List<Color> pixels, int colorCount) {
        List<List<Color>> buckets = new ArrayList<>();
        buckets.add(pixels);

        while (buckets.size() < colorCount) {
            List<List<Color>> newBuckets = new ArrayList<>();

            for (List<Color> bucket : buckets) {
                if (bucket.isEmpty()) continue;

                // 找到颜色范围最大的通道
                int[] range = getColorRange(bucket);
                int maxRangeChannel = 0; // 0: red, 1: green, 2: blue
                int maxRange = range[0];

                if (range[1] > maxRange) {
                    maxRange = range[1];
                    maxRangeChannel = 1;
                }
                if (range[2] > maxRange) {
                    maxRangeChannel = 2;
                }

                // 按该通道排序并切分
                final int channel = maxRangeChannel;
                bucket.sort((c1, c2) -> {
                    int v1 = getChannelValue(c1, channel);
                    int v2 = getChannelValue(c2, channel);
                    return Integer.compare(v1, v2);
                });

                int medianIndex = bucket.size() / 2;
                newBuckets.add(bucket.subList(0, medianIndex));
                newBuckets.add(bucket.subList(medianIndex, bucket.size()));
            }

            buckets = newBuckets;
        }

        // 计算每个桶的平均颜色
        List<Color> result = new ArrayList<>();
        for (List<Color> bucket : buckets) {
            if (!bucket.isEmpty()) {
                result.add(calculateAverageColor(bucket));
            }
        }

        return result;
    }

    private static int[] getColorRange(List<Color> colors) {
        int minRed = 255, maxRed = 0;
        int minGreen = 255, maxGreen = 0;
        int minBlue = 255, maxBlue = 0;

        for (Color color : colors) {
            minRed = Math.min(minRed, color.getRed());
            maxRed = Math.max(maxRed, color.getRed());
            minGreen = Math.min(minGreen, color.getGreen());
            maxGreen = Math.max(maxGreen, color.getGreen());
            minBlue = Math.min(minBlue, color.getBlue());
            maxBlue = Math.max(maxBlue, color.getBlue());
        }

        return new int[]{
                maxRed - minRed,
                maxGreen - minGreen,
                maxBlue - minBlue
        };
    }

    private static int getChannelValue(Color color, int channel) {
        return switch (channel) {
            case 0 -> color.getRed();
            case 1 -> color.getGreen();
            case 2 -> color.getBlue();
            default -> 0;
        };
    }

    private static Color calculateAverageColor(List<Color> colors) {
        long totalRed = 0, totalGreen = 0, totalBlue = 0;

        for (Color color : colors) {
            totalRed += color.getRed();
            totalGreen += color.getGreen();
            totalBlue += color.getBlue();
        }

        int size = colors.size();
        return new Color(
                (int) (totalRed / size),
                (int) (totalGreen / size),
                (int) (totalBlue / size)
        );
    }
}
