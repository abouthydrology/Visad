//
// AreaDirectory.java
//

/*
The code in this file is Copyright(C) 1999 by Don
Murray.  It is designed to be used with the VisAD system for 
interactive analysis and visualization of numerical data.  
 
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 1, or (at your option)
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License in file NOTICE for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*/

package edu.wisc.ssec.mcidas;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

/** 
 * AreaDirectory interface for the metadata of McIDAS 'area' file format 
 * image data.
 *
 * @author Don Murray
 * 
 */
public class AreaDirectory 
{

  private int[] dir = new int[AreaFile.AD_DIRSIZE];   // single directory
  private Date nominalTime;      // time of the image
  private Date startTime;      // start time of the image
  private int[] bands;         // array of the band numbers
  private double centerLatitude;
  private double centerLongitude;
  private double centerLatitudeResolution;
  private double centerLongitudeResolution;
  private Vector[] calInfo = null;
  private String calType;
  private String memo;
  private String[] sensors = {"derived data",
                "test patterns",
                "graphics",
                "miscellaneous",
                "PDUS Meteosat visible",
                "PDUS Meteosat infrared",
                "PDUS Meteosat water vapor",
                "radar",
                "miscellaneous aircraft data",
                "raw Meteosat",
                "composite image",
                "topography image",
                "GMS visible",
                "GMS infrared",
                "ATS 6 visible",
                "ATS 6 infrared",
                "SMS-1 visible",
                "SMS-1 infrared",
                "SMS-2 visible",
                "SMS-2 infrared",
                "GOES-1 visible",
                "GOES-1 infrared",
                "GOES-2 visible",
                "GOES-2 infrared",
                "GOES-3 visible",
                "GOES-3 infrared",
                "GOES-4 visible (VAS)",
                "GOES-4 infrared and water vapor (VAS)",
                "GOES-5 visible (VAS)",
                "GOES-5 infrared and water vapor (VAS)",
                "GOES-6 visible",
                "GOES-6 infrared",
                "GOES-7 visible, Block 1 supplemental data",
                "GOES-7 infrared",
                "FY-2B",
                "FY-2C",
                "FY-2D",
                "FY-2E",
                "FY-2F",
                "FY-2G",
                "FY-2H",
                "TIROS-N (POES)",
                "NOAA-6",
                "NOAA-7",
                "NOAA-8",
                "NOAA-9",
                "Venus",
                "Voyager 1",
                "Voyager 2",
                "Galileo",
                "Hubble space telescope",
                "",
                "",
                "",
                "Meteosat-3",
                "Meteosat-4",
                "Meteosat-5",
                "Meteosat-6",
                "Meteosat-7",
                "",
                "NOAA-10",
                "NOAA-11",
                "NOAA-12",
                "NOAA-13",
                "NOAA-14",
                "NOAA-15",
                "NOAA-16",
                "NOAA-17",
                "NOAA-18",
                "NOAA-19",
                "GOES 8 imager",
                "GOES 8 sounder",
                "GOES 9 imager",
                "GOES 9 sounder",
                "GOES 10 imager",
                "GOES 10 sounder",
                "GOES 11 imager",
                "GOES 11 sounder",
                "GOES 12 imager",
                "GOES 12 sounder",
                "ERBE",
                "",
                "GMS-4",
                "GMS-5",
                "GMS-6",
                "GMS-7",
                "GMS-8",
                "DMSP F-8",
                "DMSP F-9",
                "DMSP F-10",
                "DMSP F-11",
                "DMSP F-12",
                "DMSP F-13",
                "DMSP F-14",
                "DMSP F-15", // 94
                "FY-1B",
                "FY-1C",
                "FY-1D",
                "", 
                "",
                "",
                "MODIS 1KM", // 100
                "MODIS",
                "MODIS",
                "MODIS",
                "MODIS"};

  /**
   * Create an AreaDirectory from the raw block of data of
   * an AreaFile.  Byte-flipping will be handled.
   *
   * @param  dirblock   the integer block
   *
   * @exception  AreaFileException   not a valid directory
   */
  public AreaDirectory(int[] dirblock)
    throws AreaFileException
  {
    centerLatitude =  Double.NaN;
    centerLongitude =  Double.NaN;
    centerLatitudeResolution =  Double.NaN;
    centerLongitudeResolution =  Double.NaN;

    if (dirblock.length != AreaFile.AD_DIRSIZE)
      throw new AreaFileException("Directory is not the right size");
    dir = dirblock;
    // see if the directory needs to be byte-flipped
    if (dir[AreaFile.AD_VERSION] != AreaFile.VERSION_NUMBER) 
    {
      McIDASUtil.flip(dir,0,19);
      // check again to make sure we got the right type of file
      if (dir[AreaFile.AD_VERSION] != AreaFile.VERSION_NUMBER)
        throw new AreaFileException(
          "Invalid version number - probably not an AREA file");
      // word 20 may contain characters -- if small int, flip it
      if ( (dir[20] & 0xffff) == 0) McIDASUtil.flip(dir,20,20);
      McIDASUtil.flip(dir,21,23);
      // words 24-31 contain memo field
      McIDASUtil.flip(dir,32,50);
      // words 51-2 contain cal info
      McIDASUtil.flip(dir,53,55);
      // word 56 contains original source type (ascii)
      McIDASUtil.flip(dir,57,63);
    }
  /*   Debug
    for (int i = 0; i < AreaFile.AD_DIRSIZE; i++)
    {
      System.out.println("dir[" + i +"] = " + dir[i]);
    }
  */
    // Pull out some of the important information
    nominalTime = new Date(1000* McIDASUtil.mcDayTimeToSecs(
          dir[AreaFile.AD_IMGDATE], 
            dir[AreaFile.AD_IMGTIME]));

    if (dir[AreaFile.AD_STARTDATE] == 0 &&
        dir[AreaFile.AD_STARTTIME] == 0)
                             startTime = nominalTime;
    else
      startTime = new Date( 1000* 
              McIDASUtil.mcDayTimeToSecs(
                dir[AreaFile.AD_STARTDATE],
                dir[AreaFile.AD_STARTTIME]));
    
    int numbands = dir[AreaFile.AD_NUMBANDS];
    bands = new int[numbands];
    int j = 0;
    for (int i = 0; i < 32; i++)
    {
      int bandmask = 1 << i;
      if ( (bandmask & dir[AreaFile.AD_BANDMAP]) == bandmask)
      {
        bands[j] = i+1 ;
        j++;
      }
      if (j > numbands) break;
    }
    if (numbands > 32) {
      for (int i=0; i<32; i++) {
        int bandmask = 1 << i;
        if ( (bandmask & dir[AreaFile.AD_BANDMAP+1]) == bandmask)
        {
          bands[j] = i+33 ;
          j++;
        }
        if (j > numbands) break;
      }
    }
    // get memo field
    int[] memoArray = new int[8];
    System.arraycopy(dir, 24, memoArray, 0, memoArray.length);
    memo = McIDASUtil.intBitsToString(memoArray);
    calType = McIDASUtil.intBitsToString(dir[AreaFile.AD_CALTYPE]);

  }

  /**
   * Create an AreaDirectory from another AreaDirectory object.
   *
   * @param  directory   the source AreaDirectory
   *
   * @exception  AreaFileException   not a valid directory
   */
  public AreaDirectory(AreaDirectory directory)
    throws AreaFileException
  {
    this(directory.getDirectoryBlock());
  }
  
  /**
   * Return a specific value from the directory
   *
   * @param  pointer   part of the directory you want returned.  
   *           Use AreaFile static fields as pointers.
   *
   * @exception  AreaFileException  invalid pointer
   */
  public int getValue(int pointer)
    throws AreaFileException
  {
    if (pointer < 0 || pointer > AreaFile.AD_DIRSIZE)
      throw new AreaFileException("Invalid pointer " + pointer);
    return dir[pointer];
  }

  /**
   * Get the raw directory block
   *
   * @return integer array of the raw directory values
   */
  public int[] getDirectoryBlock()
  {
    return dir;
  }

  /**
   * returns the nominal time of the image
   *
   * @return the nominal time as a Date
   */
  public Date getNominalTime()
  {
    return nominalTime;
  }

  /**
   * returns the nominal time of the image
   *
   * @return the nominal time as a Date
   */
  public Date getStartTime()
  {
    return startTime;
  }

  /**
   * returns the number of bands in the image
   *
   * @return number of bands
   */
  public int getNumberOfBands()
  {
    return dir[AreaFile.AD_NUMBANDS];
  }

  /**
   * returns the number of lines in the image
   *
   * @return line number
   */
  public int getLines()
  {
    return dir[AreaFile.AD_NUMLINES];
  }

  /**
   * returns the number of elements in the image
   *
   * @return number of elements
   */
  public int getElements()
  {
    return dir[AreaFile.AD_NUMELEMS];
  }

  /**
   * set the band calibration info (Vector)
   * array order is identical to bands array, each Vector
   * element is a pair of String values: first, the code value and
   * second the descriptive name.
   *
   * @param the list of calibration parameters
   */
   public void setCalInfo(Vector[] v) {
     calInfo = v;
   }

   /** get the valid band calibration information
   *
   * @return array of Vectors of Strings of calibration info
   */
   public Vector[] getCalInfo() {
     return calInfo;
   }


  /**
   * returns the bands in each of the images
   *
   * @return a array of bands 
   */
  public int[] getBands()
  {
    return bands;
  }

  /**
   * Returns memo field of the directory
   *
   * @return string representing the memo
   */
  public String getMemoField()
  {
    return memo;
  }

  /**
   * Returns the sensor type
   *
   * @return string representing the sensor type
   */
  public String getSensorType()
  {
    return sensors[dir[AreaFile.AD_SENSORID]];
  }

  /**
   * Returns the sensor type
   *
   * @return string representing the sensor type
   */
  public String getCalibrationType()
  {
    return calType;
  }

  /** get Latutide at center of image
  *
  * @return value of latitude; if not available, return Double.NaN
  */
  public double getCenterLatitude() {
    return centerLatitude;
  }

  /** set Latitude at center of image
  *
  * @param value of latitude at center point of image
  */
  public void setCenterLatitude(double lat) {
    centerLatitude = lat;
  }

  /** get longitude at center of image
  *
  * @return value of longitude; if not available, return Double.NaN.
  */
  public double getCenterLongitude() {
    return centerLongitude;
  }

  /** set Longitude at center of image
  *
  * @param value of Longitude at center point of image
  */
  public void setCenterLongitude(double lon) {
    centerLongitude = lon;
  }

  /** get Latutide-wise resolution at center of image
  *
  * @return value of resolution (usually in KM) .  If not available,
  * value is Double.NaN.
  */
  public double getCenterLatitudeResolution() {
    return centerLatitudeResolution;
  }

  /** set Latitude-wise resolution at center of image
  *
  * @param value of latitude-wise resolution at center point of image
  */
  public void setCenterLatitudeResolution(double res) {
    centerLatitudeResolution = res;
  }

  /** get longitude-wise resolution at center of image
  *
  * @return value of longitude-wise resolution (usually in KM)
  * return Double.NaN if not available.
  */
  public double getCenterLongitudeResolution() {
    return centerLongitudeResolution;
  }

  /** set Longitude-wise resolution at center of image
  *
  * @param value of Longitude-wise resolution at center point of image
  */
  public void setCenterLongitudeResolution(double res) {
    centerLongitudeResolution = res;
  }

  /**
   * Prints out a formatted listing of the directory info
   */
  public String toString()
  {
    StringBuffer buf = new StringBuffer();
    SimpleDateFormat sdf = new SimpleDateFormat();
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    sdf.applyPattern("yyyy-MMM-dd  HH:mm:ss");
    buf.append("  ");
    buf.append(sdf.format(nominalTime, 
      new StringBuffer(), new FieldPosition(0)).toString());
    buf.append("  ");
    buf.append(Integer.toString(getLines()));
    buf.append("  ");
    buf.append(Integer.toString(getElements()));
    buf.append("     ");
    //buf.append(" "+bands.length);
    for (int i = 0; i < bands.length; i++) buf.append(bands[i]);
    return buf.toString();
  }
}
