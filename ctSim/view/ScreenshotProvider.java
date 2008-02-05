package ctSim.view;

import java.awt.image.BufferedImage;

/**
 * Screenshot-Klasse 
 */
public interface ScreenshotProvider {
    /**
     * @return Image vom Screenshot
     */
    public BufferedImage getScreenshot();
}
