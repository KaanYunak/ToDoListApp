package com.kaanyunak.todolistapp.ui;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AppIconLoader {
    private AppIconLoader() {
    }

    public static List<Image> loadApplicationIcons(String configuredIconPath) {
        for (Path candidate : iconCandidates(configuredIconPath)) {
            if (!Files.isRegularFile(candidate)) {
                continue;
            }
            try {
                List<Image> images = loadIconFile(candidate);
                if (!images.isEmpty()) {
                    return images;
                }
            } catch (IOException ignored) {
                // Try the next candidate.
            }
        }
        return List.of();
    }

    private static List<Path> iconCandidates(String configuredIconPath) {
        List<Path> candidates = new ArrayList<>();
        if (configuredIconPath != null && !configuredIconPath.isBlank()) {
            candidates.add(Path.of(configuredIconPath));
        }
        candidates.add(Path.of("Sisifos.png"));
        candidates.add(Path.of("sisifos.png"));
        candidates.add(Path.of("Sisifos.ico"));
        candidates.add(Path.of("sisifos.ico"));
        codeSourceDirectory().ifPresent(directory -> {
            candidates.add(directory.resolve("Sisifos.png"));
            candidates.add(directory.resolve("sisifos.png"));
            candidates.add(directory.resolve("Sisifos.ico"));
            candidates.add(directory.resolve("sisifos.ico"));
            candidates.add(directory.resolveSibling("Sisifos.png"));
            candidates.add(directory.resolveSibling("sisifos.png"));
            candidates.add(directory.resolveSibling("Sisifos.ico"));
            candidates.add(directory.resolveSibling("sisifos.ico"));
        });
        return candidates;
    }

    private static java.util.Optional<Path> codeSourceDirectory() {
        try {
            Path location = Path.of(AppIconLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return java.util.Optional.of(Files.isDirectory(location) ? location : location.getParent());
        } catch (URISyntaxException | RuntimeException ex) {
            return java.util.Optional.empty();
        }
    }

    private static List<Image> loadIconFile(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image != null) {
            return List.of(image);
        }
        return loadIco(path);
    }

    private static List<Image> loadIco(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length < 6) {
            return List.of();
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int reserved = Short.toUnsignedInt(buffer.getShort());
        int type = Short.toUnsignedInt(buffer.getShort());
        int count = Short.toUnsignedInt(buffer.getShort());
        if (reserved != 0 || type != 1 || count <= 0) {
            return List.of();
        }

        List<IconEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int entryOffset = 6 + i * 16;
            if (entryOffset + 16 > bytes.length) {
                break;
            }
            int width = Byte.toUnsignedInt(bytes[entryOffset]);
            int height = Byte.toUnsignedInt(bytes[entryOffset + 1]);
            int bitCount = Short.toUnsignedInt(ByteBuffer.wrap(bytes, entryOffset + 6, 2).order(ByteOrder.LITTLE_ENDIAN).getShort());
            int size = ByteBuffer.wrap(bytes, entryOffset + 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int imageOffset = ByteBuffer.wrap(bytes, entryOffset + 12, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            width = width == 0 ? 256 : width;
            height = height == 0 ? 256 : height;
            if (imageOffset >= 0 && size > 0 && imageOffset + size <= bytes.length) {
                entries.add(new IconEntry(width, height, bitCount, imageOffset, size));
            }
        }

        entries.sort(Comparator.comparingInt((IconEntry entry) -> entry.width * entry.height).reversed());
        List<Image> images = new ArrayList<>();
        for (IconEntry entry : entries) {
            byte[] imageData = java.util.Arrays.copyOfRange(bytes, entry.offset, entry.offset + entry.size);
            BufferedImage image = readImageData(imageData, entry);
            if (image != null) {
                images.add(image);
            }
        }
        return images;
    }

    private static BufferedImage readImageData(byte[] data, IconEntry entry) throws IOException {
        if (data.length >= 8 && data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return ImageIO.read(new ByteArrayInputStream(data));
        }
        return readDibImage(data, entry);
    }

    private static BufferedImage readDibImage(byte[] data, IconEntry entry) {
        if (data.length < 40) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int headerSize = buffer.getInt(0);
        int width = buffer.getInt(4);
        int storedHeight = buffer.getInt(8);
        int planes = Short.toUnsignedInt(buffer.getShort(12));
        int bitCount = Short.toUnsignedInt(buffer.getShort(14));
        int compression = buffer.getInt(16);
        if (headerSize < 40 || planes != 1 || compression != 0 || width <= 0 || storedHeight == 0) {
            return null;
        }

        int height = Math.abs(storedHeight) / 2;
        if (height <= 0) {
            height = entry.height;
        }
        int paletteColors = bitCount <= 8 ? 1 << bitCount : 0;
        int pixelOffset = headerSize + paletteColors * 4;
        if (pixelOffset >= data.length) {
            return null;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        if (bitCount == 32) {
            read32BitDib(data, pixelOffset, width, height, storedHeight > 0, image);
            return image;
        }
        if (bitCount == 24) {
            read24BitDib(data, pixelOffset, width, height, storedHeight > 0, image);
            return image;
        }
        return null;
    }

    private static void read32BitDib(byte[] data, int pixelOffset, int width, int height, boolean bottomUp, BufferedImage image) {
        int rowSize = width * 4;
        for (int y = 0; y < height; y++) {
            int sourceY = bottomUp ? height - 1 - y : y;
            int row = pixelOffset + sourceY * rowSize;
            for (int x = 0; x < width; x++) {
                int index = row + x * 4;
                if (index + 3 >= data.length) {
                    continue;
                }
                int blue = Byte.toUnsignedInt(data[index]);
                int green = Byte.toUnsignedInt(data[index + 1]);
                int red = Byte.toUnsignedInt(data[index + 2]);
                int alpha = Byte.toUnsignedInt(data[index + 3]);
                image.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
    }

    private static void read24BitDib(byte[] data, int pixelOffset, int width, int height, boolean bottomUp, BufferedImage image) {
        int rowSize = ((width * 3 + 3) / 4) * 4;
        for (int y = 0; y < height; y++) {
            int sourceY = bottomUp ? height - 1 - y : y;
            int row = pixelOffset + sourceY * rowSize;
            for (int x = 0; x < width; x++) {
                int index = row + x * 3;
                if (index + 2 >= data.length) {
                    continue;
                }
                int blue = Byte.toUnsignedInt(data[index]);
                int green = Byte.toUnsignedInt(data[index + 1]);
                int red = Byte.toUnsignedInt(data[index + 2]);
                image.setRGB(x, y, 0xFF000000 | (red << 16) | (green << 8) | blue);
            }
        }
    }

    private record IconEntry(int width, int height, int bitCount, int offset, int size) {
    }
}
