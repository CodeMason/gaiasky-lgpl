package gaia.cu9.ari.gaiaorbit.screenshot;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;

import gaia.cu9.ari.gaiaorbit.util.format.INumberFormat;
import gaia.cu9.ari.gaiaorbit.util.format.NumberFormatFactory;

/**
 * Utility class to render the current frame buffer to images.
 * 
 * @author Toni Sagrista
 *
 */
public class ImageRenderer {
    private static int sequenceNumber = 0;

    enum ImageType {
        PNG, JPG;
    }

    public static String renderToImageGl20(String absoluteLocation, String baseFileName, int w, int h) {
        return renderToImageGl20(absoluteLocation, baseFileName, w, h, ImageType.JPG);
    }

    /**
     * Saves the current screen as an image to the given directory using the
     * given file name. The sequence number is added automatically to the file
     * name. This method works with OpenGL 2.0
     * 
     * @param absoluteLocation
     * @param baseFileName
     * @param w
     * @param h
     */
    public static String renderToImageGl20(String absoluteLocation, String baseFileName, int w, int h, ImageType type) {
        Pixmap pixmap = getScreenshot(0, 0, w, h, true);

        String file = writePixmapToImage(absoluteLocation, baseFileName, pixmap, type);
        pixmap.dispose();
        return file;
    }

    public static Pixmap renderToPixmap(int w, int h) {
        return getScreenshot(0, 0, w, h, true);
    }

    public static String writePixmapToImage(String absoluteLocation, String baseFileName, Pixmap pixmap, ImageType type) {
        /** Make sure the directory exists **/
        FileHandle dir = Gdx.files.absolute(absoluteLocation);
        dir.mkdirs();

        /** Save to file **/
        FileHandle fh = getTarget(absoluteLocation, baseFileName, type);
        switch (type) {
        case PNG:
            PixmapIO.writePNG(fh, pixmap);
            break;
        case JPG:
            JPGWriter.write(fh, pixmap);
            break;
        }
        return fh.path();
    }

    private static Pixmap getScreenshot(int x, int y, int w, int h, boolean flipY) {
        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);

        final Pixmap pixmap = new Pixmap(w, h, Format.RGBA8888);
        ByteBuffer pixels = pixmap.getPixels();
        Gdx.gl.glReadPixels(x, y, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);

        final int numBytes = w * h * 4;
        byte[] lines = new byte[numBytes];
        if (flipY) {
            final int numBytesPerLine = w * 4;
            for (int i = 0; i < h; i++) {
                pixels.position((h - i - 1) * numBytesPerLine);
                pixels.get(lines, i * numBytesPerLine, numBytesPerLine);

                for (int j = 3; j < w * 4; j += 4) {
                    lines[j + i * w * 4] = (byte) 255;
                }
            }
            pixels.clear();
            pixels.put(lines);
        } else {
            pixels.clear();
            pixels.get(lines);
        }

        return pixmap;
    }

    private static FileHandle getTarget(String absoluteLocation, String baseFileName, ImageType type) {
        FileHandle fh = Gdx.files.absolute(absoluteLocation + File.separator + baseFileName + getNextSeqNumSuffix() + "." + type.toString().toLowerCase());
        while (fh.exists()) {
            fh = Gdx.files.absolute(absoluteLocation + File.separator + baseFileName + getNextSeqNumSuffix() + "." + type.toString().toLowerCase());
        }
        return fh;
    }

    private static String getNextSeqNumSuffix() {
        return "_" + intToString(sequenceNumber++, 5);
    }

    private static String intToString(int num, int digits) {
        assert digits > 0 : "Invalid number of digits";

        // create variable length array of zeros
        char[] zeros = new char[digits];
        Arrays.fill(zeros, '0');
        // format number as String
        INumberFormat df = NumberFormatFactory.getFormatter(String.valueOf(zeros));

        return df.format(num);
    }
}
