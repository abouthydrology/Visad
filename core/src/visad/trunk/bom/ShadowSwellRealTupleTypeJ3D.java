//
// ShadowSwellRealTupleTypeJ3D.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2000 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

package visad.bom;

import visad.*;
import visad.java3d.*;

import java.util.*;
import java.rmi.*;

/**
   The ShadowSwellRealTupleTypeJ3D class shadows the RealTupleType class
   for SwellManipulationRendererJ3D and SwellRendererJ3D, within a
   DataDisplayLink, under Java3D.<P>
*/
public class ShadowSwellRealTupleTypeJ3D extends ShadowRealTupleTypeJ3D {

  public ShadowSwellRealTupleTypeJ3D(MathType t, DataDisplayLink link,
                                ShadowType parent)
         throws VisADException, RemoteException {
    super(t, link, parent);
  }

  public VisADGeometryArray[] makeFlow(int which, float[][] flow_values,
                float flowScale, float[][] spatial_values,
                byte[][] color_values, boolean[][] range_select)
         throws VisADException {

    DataRenderer renderer = getLink().getRenderer();
    boolean direct = renderer.getIsDirectManipulation();
    if (direct && renderer instanceof SwellManipulationRendererJ3D) {
      return ShadowSwellRealTupleTypeJ3D.staticMakeFlow(getDisplay(), which,
                 flow_values, flowScale, spatial_values, color_values,
                 range_select, renderer, true);
    }
    else {
      return ShadowSwellRealTupleTypeJ3D.staticMakeFlow(getDisplay(), which,
                 flow_values, flowScale, spatial_values, color_values,
                 range_select, renderer, false);
    }
  }


  private static final int NUM = 256;

  public static VisADGeometryArray[] staticMakeFlow(DisplayImpl display,
               int which, float[][] flow_values, float flowScale,
               float[][] spatial_values, byte[][] color_values,
               boolean[][] range_select, DataRenderer renderer, boolean direct)
         throws VisADException {
    if (flow_values[0] == null) return null;
    if (spatial_values[0] == null) return null;

    int len = spatial_values[0].length;
    int flen = flow_values[0].length;
    int rlen = 0; // number of non-missing values
    if (range_select[0] == null) {
      rlen = len;
    }
    else {
      for (int j=0; j<range_select[0].length; j++) {
        if (range_select[0][j]) rlen++;
      }
    }
    if (rlen == 0) return null;

    // use default flowScale = 0.02f here, since flowScale for barbs is
    // just for barb size
    flow_values = adjustFlowToEarth(which, flow_values, spatial_values,
                                    0.02f, renderer);

    float[] vx = new float[NUM];
    float[] vy = new float[NUM];
    float[] vz = new float[NUM];
    byte[] vred = null;
    byte[] vgreen = null;
    byte[] vblue = null;
    if (color_values != null) {
      vred = new byte[NUM];
      vgreen = new byte[NUM];
      vblue = new byte[NUM];
    }
    int[] numv = {0};

    float scale = flowScale; // ????
    float pt_size = 0.25f * flowScale; // ????

    // flow vector
    float f0 = 0.0f, f1 = 0.0f, f2 = 0.0f;
    for (int j=0; j<len; j++) {
      if (range_select[0] == null || range_select[0][j]) {
// NOTE - must scale to knots
        if (flen == 1) {
          f0 = flow_values[0][0];
          f1 = flow_values[1][0];
          f2 = flow_values[2][0];
        }
        else {
          f0 = flow_values[0][j];
          f1 = flow_values[1][j];
          f2 = flow_values[2][j];
        }

        if (numv[0] + NUM/4 > vx.length) {
          float[] cx = vx;
          float[] cy = vy;
          float[] cz = vz;
          int l = 2 * vx.length;
          vx = new float[l];
          vy = new float[l];
          vz = new float[l];
          System.arraycopy(cx, 0, vx, 0, cx.length);
          System.arraycopy(cy, 0, vy, 0, cy.length);
          System.arraycopy(cz, 0, vz, 0, cz.length);
          if (color_values != null) {
            byte[] cred = vred;
            byte[] cgreen = vgreen;
            byte[] cblue = vblue;
            vred = new byte[l];
            vgreen = new byte[l];
            vblue = new byte[l];
            System.arraycopy(cred, 0, vred, 0, cred.length);
            System.arraycopy(cgreen, 0, vgreen, 0, cgreen.length);
            System.arraycopy(cblue, 0, vblue, 0, cblue.length);
          }
        }
        int oldnv = numv[0];
        float mbarb[] =
          makeSwell(spatial_values[0][j], spatial_values[1][j],
                   spatial_values[2][j], scale, pt_size, f0, f1, vx, vy, vz,
                   numv);
        if (direct) {
          ((SwellManipulationRendererJ3D) renderer).
            setSwellSpatialValues(mbarb, which);
        }
        int nv = numv[0];
        if (color_values != null) {
          if (color_values[0].length > 1) {
            for (int i=oldnv; i<nv; i++) {
              vred[i] = color_values[0][j];
              vgreen[i] = color_values[1][j];
              vblue[i] = color_values[2][j];
            }
          }
          else {  // if (color_values[0].length == 1)
            for (int i=oldnv; i<nv; i++) {
              vred[i] = color_values[0][0];
              vgreen[i] = color_values[1][0];
              vblue[i] = color_values[2][0];
            }
          }
        }
      } // end if (range_select[0] == null || range_select[0][j])
    } // end for (int j=0; j<len; j++)

    int nv = numv[0];
    if (nv == 0) return null;

    VisADGeometryArray[] arrays = null;
    VisADLineArray array = new VisADLineArray();
    array.vertexCount = nv;

    float[] coordinates = new float[3 * nv];

    int m = 0;
    for (int i=0; i<nv; i++) {
      coordinates[m++] = vx[i];
      coordinates[m++] = vy[i];
      coordinates[m++] = vz[i];
    }
    array.coordinates = coordinates;

    byte[] colors = null;
    if (color_values != null) {
      colors = new byte[3 * nv];
      m = 0;
      for (int i=0; i<nv; i++) {
        colors[m++] = vred[i];
        colors[m++] = vgreen[i];
        colors[m++] = vblue[i];
      }
      array.colors = colors;
    }
    arrays = new VisADGeometryArray[] {array};
    return arrays;
  }


  /** draw swell, f0 and f1 in meters */
  static float[] makeSwell(float x, float y, float z,
                          float scale, float pt_size, float f0, float f1,
                          float[] vx, float[] vy, float[] vz, int[] numv) {

    float d, xd, yd;
    float x0, y0, x1, y1, x2, y2, x3, y3, x4, y4;
    float sscale = 0.75f * scale;

    float[] mbarb = new float[4];
    mbarb[0] = x;
    mbarb[1] = y;

    float swell_height = (float) Math.sqrt(f0 * f0 + f1 * f1);

    int lenv = vx.length;
    int nv = numv[0];

    //determine the initial (minimum) length of the flag pole
    if (swell_height >= 0.1f) {
      // normalize direction
      x0 = -f0 / swell_height;
      y0 = -f1 / swell_height;

      float start_arrow = 0.9f * sscale;
      float end_arrow = 1.9f * sscale;
      float arrow_head = 0.3f * sscale;
      x1 = (x + x0 * start_arrow);
      y1 = (y + y0 * start_arrow);
      x2 = (x + x0 * end_arrow);
      y2 = (y + y0 * end_arrow);

      // draw arrow shaft
      vx[nv] = x1;
      vy[nv] = y1;
      vz[nv] = z;
      nv++;
      vx[nv] = x2;
      vy[nv] = y2;
      vz[nv] = z;
      nv++;

      mbarb[2] = x2;
      mbarb[3] = y2;

      xd = x2 - x1;
      yd = y2 - y1;

      x3 = x2 - 0.3f * (xd - yd);
      y3 = y2 - 0.3f * (yd + xd);
      x4 = x2 - 0.3f * (xd + yd);
      y4 = y2 - 0.3f * (yd - xd);

      // draw arrow head
      vx[nv] = x2;
      vy[nv] = y2;
      vz[nv] = z;
      nv++;
      vx[nv] = x3;
      vy[nv] = y3;
      vz[nv] = z;
      nv++;

      vx[nv] = x2;
      vy[nv] = y2;
      vz[nv] = z;
      nv++;
      vx[nv] = x4;
      vy[nv] = y4;
      vz[nv] = z;
      nv++;

      int shi = (int) (10.0f * (swell_height + 0.5f));
      float shf = 0.1f * shi;
      String sh_string = Float.toString(shf);
      int point = sh_string.indexOf('.');
      sh_string = sh_string.substring(0, point + 2);
      double[] start = {x, y - 0.25 * sscale, 0.0};
      double[] base = {0.5 * sscale, 0.0, 0.0};
      double[] up = {0.0, 0.5 * sscale, 0.0};
      VisADLineArray array =
        PlotText.render_label(sh_string, start, base, up, true);
      int nl = array.vertexCount;
      int k = 0;
      for (int i=0; i<nl; i++) {
        vx[nv] = array.coordinates[k++];
        vy[nv] = array.coordinates[k++];
        vz[nv] = array.coordinates[k++];
        nv++;
      }
    }
    else { // if (swell_height < 0.1)

      // wind < 2.5 kts.  Plot a circle
      float rad = (0.7f * pt_size);

      // draw 8 segment circle, center = (x, y), radius = rad
      // 1st segment
      vx[nv] = x - rad;
      vy[nv] = y;
      vz[nv] = z;
      nv++;
      vx[nv] = x - 0.7f * rad;
      vy[nv] = y + 0.7f * rad;
      vz[nv] = z;
      nv++;
      // 2nd segment
      vx[nv] = x - 0.7f * rad;
      vy[nv] = y + 0.7f * rad;
      vz[nv] = z;
      nv++;
      vx[nv] = x;
      vy[nv] = y + rad;
      vz[nv] = z;
      nv++;
      // 3rd segment
      vx[nv] = x;
      vy[nv] = y + rad;
      vz[nv] = z;
      nv++;
      vx[nv] = x + 0.7f * rad;
      vy[nv] = y + 0.7f * rad;
      vz[nv] = z;
      nv++;
      // 4th segment
      vx[nv] = x + 0.7f * rad;
      vy[nv] = y + 0.7f * rad;
      vz[nv] = z;
      nv++;
      vx[nv] = x + rad;
      vy[nv] = y;
      vz[nv] = z;
      nv++;
      // 5th segment
      vx[nv] = x + rad;
      vy[nv] = y;
      vz[nv] = z;
      nv++;
      vx[nv] = x + 0.7f * rad;
      vy[nv] = y - 0.7f * rad;
      vz[nv] = z;
      nv++;
      // 6th segment
      vx[nv] = x + 0.7f * rad;
      vy[nv] = y - 0.7f * rad;
      vz[nv] = z;
      nv++;
      vx[nv] = x;
      vy[nv] = y - rad;
      vz[nv] = z;
      nv++;
      // 7th segment
      vx[nv] = x;
      vy[nv] = y - rad;
      vz[nv] = z;
      nv++;
      vx[nv] = x - 0.7f * rad;
      vy[nv] = y - 0.7f * rad;
      vz[nv] = z;
      nv++;
      // 8th segment
      vx[nv] = x - 0.7f * rad;
      vy[nv] = y - 0.7f * rad;
      vz[nv] = z;
      nv++;
      vx[nv] = x - rad;
      vy[nv] = y;
      vz[nv] = z;
      nv++;
// System.out.println("circle " + x + " " + y + "" + rad);
      mbarb[2] = x;
      mbarb[3] = y;
    }

    numv[0] = nv;
    return mbarb;
  }

}

