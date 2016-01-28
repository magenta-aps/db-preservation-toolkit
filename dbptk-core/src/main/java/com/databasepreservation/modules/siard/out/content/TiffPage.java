package com.databasepreservation.modules.siard.out.content;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import com.databasepreservation.modules.siard.constants.SIARDDKConstants;

/**
 * @author Andreas Kring <andreas@magenta.dk>
 *
 */
public class TiffPage {

  private int currentYPosition;
  private int xMargin;
  private int yMargin;
  private Font font;
  private String[] wordBuffer;
  private RenderedImage image;

  /**
   * Standard constructor - set width and height
   */
  public TiffPage(int width, int height) {
    this(width, height, SIARDDKConstants.PAGE_WIDTH, SIARDDKConstants.PAGE_HEIGHT);
  }

  /**
   * Constructor for setting width, height and the margins for the tiff image
   */
  public TiffPage(int width, int height, int xMargin, int yMargin) {
    image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
    this.xMargin = xMargin;
    this.yMargin = yMargin;
    currentYPosition = 0;
  }

  public Font createFont(int size) {
    return new Font(Font.SANS_SERIF, Font.PLAIN, size);
  }

  /**
   * Write line to tiff image
   * 
   * @param words
   *          An array of words to be written to line in the image
   * @return Array of remaining words which could not fit into the line
   */
  public String[] writeLine(String[] words) {

    return null;
  }
}
