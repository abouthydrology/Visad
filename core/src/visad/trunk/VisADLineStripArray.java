 
//
// VisADLineStripArray.java
//
 
/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 1998 Bill Hibbard, Curtis Rueden, Tom
Rink and Dave Glowacki.
 
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
 
package visad;
 
/**
   VisADLineStripArray stands in for j3d.LineStripArray
   and is Serializable.<P>
*/
public class VisADLineStripArray extends VisADGeometryArray {
  public int[] stripVertexCounts;

  public static VisADLineStripArray
                merge(VisADLineStripArray[] arrays)
         throws VisADException {
    if (arrays == null || arrays.length == 0) return null;
    VisADLineStripArray array = new VisADLineStripArray();
    merge(arrays, array);
    int n = arrays.length;
    int nstrips = 0;
    for (int i=0; i<n; i++) {
      if (arrays[i] != null) {
        nstrips += arrays[i].stripVertexCounts.length;
      }
    }
    int[] stripVertexCounts = new int[nstrips];
    nstrips = 0;
    for (int i=0; i<n; i++) {
      if (arrays[i] != null) {
        int incnstrips = arrays[i].stripVertexCounts.length;
        for (int j=0; j<incnstrips; j++) {
          stripVertexCounts[nstrips + j] = arrays[i].stripVertexCounts[j];
        }
        nstrips += incnstrips;
      }
    }
    array.stripVertexCounts = stripVertexCounts;
    return array;
  }

  public Object clone() {
    VisADLineStripArray array = new VisADLineStripArray();
    copy(array);
    if (stripVertexCounts != null) {
      array.stripVertexCounts = new int[stripVertexCounts.length];
      System.arraycopy(stripVertexCounts, 0, array.stripVertexCounts, 0,
                       stripVertexCounts.length);
    }
    return array;
  }

}

