package com.databasepreservation.modules.siard.out.content.lobs;

/**
 * @author Andreas Kring <andreas@magenta.dk>
 *
 */
public interface WriteLinesStrategy {

  /**
   * Writes lines to image
   * 
   * @param clob
   *          String to write
   * @param image
   *          Image to write to
   * @return Remainder to string that could fit into the image
   */
  public Object writeLines(String clob, TiffPage tiffPage);

  /**
   * Get the pixel width of a string
   * 
   * @param s
   *          String to determine width of
   * @return Width of the string in pixels
   */
  public int stringWidth(String s);
}
