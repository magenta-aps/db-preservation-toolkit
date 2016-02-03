package com.databasepreservation.modules.siard.out.content.lobs;

/**
 * @author Andreas Kring <andreas@magenta.dk>
 *
 */
public interface WriteLinesStrategy {

  /**
   * Writes lines to image
   * 
   * @param characters
   *          Characters from CLOB to write to tiff image
   * @param image
   *          Image to write to
   * @return Remainder to string that could fit into the image
   */
  public char[] writeLines(char[] characters, TiffPage tiffPage);

}
