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
    FontMetrics fontMetrics = graphics.getFontMetrics();

    int currentXPosition = tiffPage.getxMargin();
    int currentYPosition = fontMetrics.getHeight() + tiffPage.getyMargin();
    int maximumAllowedXPosition = tiffPage.getWidth() - tiffPage.getxMargin();
    int maximumAllowedYPosition = tiffPage.getHeight() - tiffPage.getyMargin();
    int characterCounter = 0;

    // char[] characters = new char[clob.length()];
    // clob.getChars(0, clob.length(), characters, 0);

    while (currentYPosition < maximumAllowedYPosition && characterCounter < characters.length) {
      graphics.drawChars(characters, characterCounter, 1, currentXPosition, currentYPosition);
      currentXPosition += fontMetrics.charWidth(characters[characterCounter]);
      characterCounter += 1;
      if (currentXPosition + fontMetrics.charWidth(characters[characterCounter]) > maximumAllowedXPosition) {
        currentXPosition = tiffPage.getxMargin();
        currentYPosition += fontMetrics.getHeight();
      }
      // Write character
      // update counter
      // advance x pos
      // if over maxXpos - change line
      //
    }

    graphics.dispose();

    return Arrays.copyOfRange(characters, characterCounter, characters.length);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.databasepreservation.modules.siard.out.content.lobs.WriteLinesStrategy
   * #stringWidth(java.lang.String)
   */
  @Override
  public int stringWidth(String s) {
    // TODO Auto-generated method stub
    return 0;
  }

}
