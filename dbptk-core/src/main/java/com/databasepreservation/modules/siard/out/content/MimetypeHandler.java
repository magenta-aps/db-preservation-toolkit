package com.databasepreservation.modules.siard.out.content;

import java.security.InvalidParameterException;

/**
 * @author Andreas Kring <andreas@magenta.dk>
 *
 */
public interface MimetypeHandler {

  /**
   * Checks if mimetype is allowed by the SIARDDK specification
   * 
   * @param mimetype
   * @return True if mimetype is allowed and false otherwise
   */
  public boolean isMimetypeAllowed(String mimetype);

  /**
   * Gets the filename extension for the given mimetype (overkill to use Apache
   * Tika for this)
   * 
   * @param mimetype
   * @return Extension of the given mimetype, e.g. "tif" for "image/tiff" etc.
   * @throws InvalidParameterException
   */
  public String getFileExtension(String mimetype) throws InvalidParameterException;
}
