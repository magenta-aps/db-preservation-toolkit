package com.databasepreservation.modules.siard.out.content.lobs;

/**
 * @author Andreas Kring <andreas@magenta.dk>
 *
 */
public class TiffPageDriver {

  /**
   * @param args
   */
  public static void main(String[] args) {
    TiffPage tiffPage = new TiffPage(300, 300, 0, 0, new SentenceWriteLinesStrategy());
    String clob = "Dette er en meget meget lang streng, som skal skrives til det dynamiske og farverige billede";
    clob = "Dette er en streng";

    tiffPage.writeLines(clob);
    tiffPage.saveTiff();

  }

}
