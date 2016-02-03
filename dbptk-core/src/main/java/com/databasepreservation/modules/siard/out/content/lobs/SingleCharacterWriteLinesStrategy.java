package com.databasepreservation.modules.siard.out.content.lobs;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * @author Andreas Kring <andreas@magenta.dk>
 *
 */
public class SingleCharacterWriteLinesStrategy implements WriteLinesStrategy {

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.databasepreservation.modules.siard.out.content.lobs.WriteLinesStrategy
   * #writeLines(java.lang.String,
   * com.databasepreservation.modules.siard.out.content.lobs.TiffPage)
   */
  @Override
  public char[] writeLines(char[] characters, TiffPage tiffPage) {

    BufferedImage image = tiffPage.getImage();
    Graphics2D graphics = (Graphics2D) image.getGraphics();
    graphics.setBackground(Color.WHITE);
    graphics.clearRect(0, 0, tiffPage.getWidth(), tiffPage.getHeight());
    graphics.setColor(Color.BLACK);
    graphics.setFont(tiffPage.getFont());
    FontMetrics fontMetrics = graphics.getFontMetrics();

    int currentXPosition = tiffPage.getxMargin();
    int currentYPosition = fontMetrics.getHeight() + tiffPage.getyMargin();
    int maximumAllowedXPosition = tiffPage.getWidth() - tiffPage.getxMargin();
    int maximumAllowedYPosition = tiffPage.getHeight() - tiffPage.getyMargin();
    int characterCounter = 0;

    while (currentYPosition < maximumAllowedYPosition && characterCounter < characters.length) {
      graphics.drawChars(characters, characterCounter, 1, currentXPosition, currentYPosition);
      currentXPosition += fontMetrics.charWidth(characters[characterCounter]);
      characterCounter += 1;
      if (characterCounter < characters.length
        && currentXPosition + fontMetrics.charWidth(characters[characterCounter]) > maximumAllowedXPosition) {
        currentXPosition = tiffPage.getxMargin();
        currentYPosition += fontMetrics.getHeight();
      }
    }

    graphics.dispose();

    return Arrays.copyOfRange(characters, characterCounter, characters.length);
  }
}
