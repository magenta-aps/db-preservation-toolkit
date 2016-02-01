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
public class SentenceWriteLinesStrategy implements WriteLinesStrategy {

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.databasepreservation.modules.siard.out.content.lobs.WriteLinesStrategy
   * #writeLines(java.lang.String,
   * com.databasepreservation.modules.siard.out.content.lobs.TiffPage)
   */
  @Override
  public Object writeLines(String clob, TiffPage tiffPage) {

    String[] words = clob.trim().split("\\s+");

    BufferedImage image = tiffPage.getImage();
    Graphics2D graphics = (Graphics2D) image.getGraphics();
    graphics.setBackground(Color.WHITE);
    graphics.clearRect(0, 0, tiffPage.getWidth(), tiffPage.getHeight());
    graphics.setColor(Color.BLACK);
    FontMetrics fontMetrics = graphics.getFontMetrics();

    // graphics.drawString("Dette er en streng", 0, fontMetrics.getHeight());

    int currentYPosition = fontMetrics.getHeight();
    int wordIdx = 0;

    while (currentYPosition < tiffPage.getEffectiveHeight()) {

      int widthOfLineSoFar = 0;
      StringBuilder builder = new StringBuilder();

      while (widthOfLineSoFar < tiffPage.getEffectiveWidth()) {
        System.out.println("Inside second while...");

        if (wordIdx == words.length) {

          String line = builder.toString();
          System.out.println(line + "x");
          graphics.drawString(line, 0, currentYPosition);
          System.out.println("Last word written!");
          return null;
        }

        int wordWidth = fontMetrics.stringWidth(words[wordIdx] + " ");
        widthOfLineSoFar += wordWidth;
        if (widthOfLineSoFar > tiffPage.getEffectiveWidth()) {
          if (wordWidth > tiffPage.getEffectiveWidth()) {
            // word too long for line
          } else {
            // Write to line image
            System.out.println("Line done - go to next line");
            String line = builder.toString();
            graphics.drawString(line, 0, currentYPosition);
            // Go to next line
            currentYPosition += fontMetrics.getHeight();

          }

        } else {
          // word can be appended to line
          System.out.println(words[wordIdx] + " appended to builder");
          builder.append(words[wordIdx] + " ");
          // graphics.drawString(builder.toString(), 20, currentYPosition);
          wordIdx += 1;
        }

      }
    }

    return Arrays.copyOfRange(words, wordIdx, words.length);
    // return null;
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

    return 0;
  }

}
