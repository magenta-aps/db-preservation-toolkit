package com.databasepreservation.modules.siard.out.content.lobs;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import com.databasepreservation.modules.siard.constants.SIARDDKConstants;

/**
 * @author Andreas Kring <andreas@magenta.dk>
 *
 */
public class TiffPage {

  private int width;
  private int height;
  // private int currentYPosition;
  private int xMargin;
  private int yMargin;
  // private Font font;
  // private String[] wordBuffer;
  private BufferedImage image;
  private WriteLinesStrategy writeLinesStrategy;

  /**
   * Standard constructor - set width and height
   */
  public TiffPage(int width, int height) {
    this(width, height, SIARDDKConstants.XMARGIN, SIARDDKConstants.YMARGIN, new SentenceWriteLinesStrategy());
  }

  /**
   * Constructor for setting width, height and the margins for the tiff image
   */
  public TiffPage(int width, int height, int xMargin, int yMargin, WriteLinesStrategy writeLinesStrategy) {
    image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
    this.width = width;
    this.height = height;
    this.xMargin = xMargin;
    this.yMargin = yMargin;
    // currentYPosition = 0;
    this.writeLinesStrategy = writeLinesStrategy;
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
  public Object writeLines(String clob) {
    return writeLinesStrategy.writeLines(clob, this);
  }

  public void saveTiff() {
    ParameterBlock pb = new ParameterBlock().add(image);
    RenderedOp im = JAI.create("awtimage", pb);

    ParameterBlock fileParameters = new ParameterBlock();
    fileParameters.addSource(im).add("/tmp/img.tiff").add("TIFF").add(null);
    JAI.create("filestore", fileParameters);

  }

  public BufferedImage getImage() {
    return image;
  }

  /**
   * @return the width
   */
  public int getWidth() {
    return width;
  }

  /**
   * @return the height
   */
  public int getHeight() {
    return height;
  }

  /**
   * Get the effective writing width, i.e. image width - 2*xMargin
   * 
   * @return the effective width
   */
  public int getEffectiveWidth() {
    return width - 2 * xMargin;
  }

  /**
   * Get the effective writing height, i.e. image height - 2*yMargin
   * 
   * @return the effective height
   */
  public int getEffectiveHeight() {
    return height - 2 * yMargin;
  }
}
