package com.diozero.devices.oled;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

public interface MonochromePixelBuffer {

    default void draw(BufferedImage image) {
        draw(image, 1);
    }

    default void draw(BufferedImage image, int threshold) {
        if (image.getWidth() != getWidth() || image.getHeight() != getHeight()) {
            throw new IllegalArgumentException("Invalid input image dimensions, must be " + getWidth() + "x" + getHeight());
        }

        Raster r = image.getRaster();
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                //int[] pixel = r.getPixel(x, y, new int[] {});
                setPixel(x, y, r.getSample(x, y, 0) >= threshold);
            }
        }
    }

    int getHeight();

    int getWidth();

    void setPixel(int x, int y, boolean on);
}
