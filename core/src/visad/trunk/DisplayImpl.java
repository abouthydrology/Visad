//
// DisplayImpl.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 1999 Bill Hibbard, Curtis Rueden, Tom
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

package visad;

import java.util.*;
import java.rmi.*;
import java.io.*;

import java.awt.*;
import java.awt.image.*;
import java.net.*;

import javax.swing.*;
import visad.util.*;

/**
   DisplayImpl is the abstract VisAD superclass for display
   implementations.  It is runnable.<P>

   DisplayImpl is not Serializable and should not be copied
   between JVMs.<P>
*/
public abstract class DisplayImpl extends ActionImpl implements LocalDisplay {

  /** instance variables */
  /** a Vector of ScalarMap objects;
      does not include ConstantMap objects */
  private Vector MapVector = new Vector();

  /** a Vector of ConstantMap objects */
  private Vector ConstantMapVector = new Vector();

  /** a Vector of RealType (and TextType) objects occuring
      in MapVector */
  private Vector RealTypeVector = new Vector();

  /** a Vector of DisplayRealType objects occuring in MapVector */
  private Vector DisplayRealTypeVector = new Vector();

  /** list of Control objects linked to ScalarMap objects in MapVector;
      the Control objects may be linked to UI widgets, or just computed */
  private Vector ControlVector = new Vector();

  /** ordered list of DataRenderer objects that render Data objects */
  private Vector RendererVector = new Vector();

  /** DisplayRenderer object for background and metadata rendering */
  private DisplayRenderer displayRenderer;

  /** Component where data depictions are rendered;
      must be set by concrete subclass constructor */
  Component component;


  /** set to indicate need to compute ranges of RealType-s
      and sampling for Animation */
  private boolean initialize = true;

  /** set to indicate that ranges should be auto-scaled
      every time data are displayed */
  private boolean always_initialize = false;

  /** set to re-display all linked Data */
  private boolean redisplay_all = false;

  /** length of ValueArray of distinct DisplayRealType values;
      one per Single DisplayRealType that occurs in a ScalarMap,
      plus one per ScalarMap per non-Single DisplayRealType;
      ScalarMap.valueIndex is an index into ValueArray */
  private int valueArrayLength;

  /** mapping from ValueArray to DisplayScalar */
  private int[] valueToScalar;

  /** mapping from ValueArray to MapVector */
  private int[] valueToMap;

  /** Vector of DisplayListeners */
  private transient Vector ListenerVector = new Vector();

  private Object mapslock = new Object();

  // WLH 16 March 99
  private MouseBehavior mouse = null;

  // objects which monitor and synchronize with remote displays
  private DisplayMonitor displayMonitor = null;
  private DisplaySync displaySync = null;

  /** constructor with non-default DisplayRenderer */
  public DisplayImpl(String name, DisplayRenderer renderer)
         throws VisADException, RemoteException {
    super(name);
    // put system intrinsic DisplayRealType-s in DisplayRealTypeVector
    for (int i=0; i<DisplayRealArray.length; i++) {
      DisplayRealTypeVector.addElement(DisplayRealArray[i]);
    }

    displayMonitor = new DisplayMonitorImpl(this);
    displaySync = new DisplaySyncImpl(this);

    if (renderer != null) {
      displayRenderer = renderer;
    } else {
      displayRenderer = getDefaultDisplayRenderer();
    }
    displayRenderer.setDisplay(this);

    // initialize ScalarMap's, ShadowDisplayReal's and Control's
    clearMaps();
  }

  /** construct DisplayImpl from RemoteDisplay */
  public DisplayImpl(RemoteDisplay rmtDpy, DisplayRenderer renderer)
         throws VisADException, RemoteException {
    super(rmtDpy.getName());

    // put system intrinsic DisplayRealType-s in DisplayRealTypeVector
    for (int i=0; i<DisplayRealArray.length; i++) {
      DisplayRealTypeVector.addElement(DisplayRealArray[i]);
    }

    displayMonitor = new DisplayMonitorImpl(this);
    displaySync = new DisplaySyncImpl(this);

    if (renderer != null) {
      displayRenderer = renderer;
    } else {
      try {
	String name = rmtDpy.getDisplayRendererClassName();
	Object obj = Class.forName(name).newInstance();
	displayRenderer = (DisplayRenderer )obj;
      } catch (Exception e) {
	displayRenderer = getDefaultDisplayRenderer();
      }
    }
    displayRenderer.setDisplay(this);

    // initialize ScalarMap's, ShadowDisplayReal's and Control's
    clearMaps();
  }

  // suck in any remote ScalarMaps
  void copyScalarMaps(RemoteDisplay rmtDpy)
  {
    Vector m;
    try {
      m = rmtDpy.getMapVector();
    } catch (Exception e) {
      System.err.println("Couldn't copy ScalarMaps");
      return;
    }

    Enumeration me = m.elements();
    while (me.hasMoreElements()) {
      ScalarMap sm = (ScalarMap )me.nextElement();
      try {
        addMap(sm);
      } catch (DisplayException de) {
        try {
          addMap(new ScalarMap(sm.getScalar(), sm.getDisplayScalar()));
        } catch (Exception e) {
          System.err.println("Couldn't copy remote ScalarMap " + sm);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  // suck in any remote ConstantMaps
  void copyConstantMaps(RemoteDisplay rmtDpy)
  {
    Vector c;
    try {
      c = rmtDpy.getConstantMapVector();
    } catch (Exception e) {
      System.err.println("Couldn't copy ConstantMaps");
      return;
    }

    Enumeration ce = c.elements();
    while (ce.hasMoreElements()) {
      ConstantMap cm = (ConstantMap )ce.nextElement();
      try {
        addMap(cm);
      } catch (DisplayException de) {
        try {
          addMap(new ConstantMap(cm.getConstant(), cm.getDisplayScalar()));
        } catch (Exception e) {
          System.err.println("Couldn't copy remote ConstantMap " + cm);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  // suck in remote GraphicsModeControl settings
  void copyGraphicsModeControl(RemoteDisplay rmtDpy)
  {
    GraphicsModeControl rc;
    try {
      getGraphicsModeControl().syncControl(rmtDpy.getGraphicsModeControl());
    } catch (UnmarshalException ue) {
      System.err.println("Couldn't copy remote GraphicsModeControl");
      return;
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

  }

  // suck in any remote DataReferences
  void copyRefLinks(RemoteDisplay rmtDpy)
  {
    Vector ml;
    try {
      ml = rmtDpy.getReferenceLinks();
    } catch (UnmarshalException ue) {
      System.err.println("Couldn't copy remote DataReferences");
      return;
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    Enumeration mle = ml.elements();
    if (mle.hasMoreElements()) {

      DataRenderer dr = displayRenderer.makeDefaultRenderer();
      String defaultClass = dr.getClass().getName();

      while (mle.hasMoreElements()) {
        RemoteReferenceLink link = (RemoteReferenceLink )mle.nextElement();

        // build array of ConstantMap values
        ConstantMap[] cm = null;
        try {
          Vector v = link.getConstantMapVector();
          int len = v.size();
          if (len > 0) {
            cm = new ConstantMap[len];
            for (int i = 0; i < len; i++) {
              cm[i] = (ConstantMap )v.elementAt(i);
            }
          }
        } catch (Exception e) {
          System.err.println("Couldn't copy ConstantMaps" +
                             " for remote DataReference");
        }

        // get reference to Data object
        RemoteDataReference ref;
        try {
          ref = link.getReference();
        } catch (Exception e) {
          System.err.println("Couldn't copy remote DataReference");
          ref = null;
        }

        if (ref != null) {

          // get proper DataRenderer
          DataRenderer renderer;
          try {
            String newClass = link.getRendererClassName();
            if (newClass == defaultClass) {
              renderer = null;
            } else {
              Object obj = Class.forName(newClass).newInstance();
              renderer = (DataRenderer )obj;
            }
          } catch (Exception e) {
            System.err.println("Couldn't copy remote DataRenderer name" +
                               "; using " + defaultClass);
            renderer = null;
          }

          // build RemoteDisplayImpl to which reference is attached
          try {
            RemoteDisplayImpl rd = new RemoteDisplayImpl(this);

            // if this reference uses the default renderer...
            if (renderer == null) {
              rd.addReference(ref, cm);
            } else {
              rd.addReferences(renderer, ref, cm);
            }
          } catch (Exception e) {
            System.err.println("Couldn't add remote DataReference " + ref);
          }
        }
      }
    }
  }

  // suck in any remote data associated with this Display
  protected void syncRemoteData(RemoteDisplay rmtDpy)
    throws VisADException, RemoteException
  {
    copyScalarMaps(rmtDpy);
    copyConstantMaps(rmtDpy);
    copyGraphicsModeControl(rmtDpy);
    copyRefLinks(rmtDpy);

    notifyAction();
    waitForTasks();

    // only add remote display as listener *after* we've synced
    displayMonitor.addRemoteListener(rmtDpy);
    displayMonitor.syncControls();
  }

  /** RemoteDisplayImpl to this for use with
      Remote DisplayListeners */
  private RemoteDisplayImpl rd = null;

  public void notifyListeners(int id, int x, int y)
         throws VisADException, RemoteException {
    notifyListeners(new DisplayEvent(this, id, x, y));
  }

  public void notifyListeners(DisplayEvent evt)
         throws VisADException, RemoteException {
    if (ListenerVector != null) {
      synchronized (ListenerVector) {
        Enumeration listeners = ListenerVector.elements();
        while (listeners.hasMoreElements()) {
          DisplayListener listener =
            (DisplayListener) listeners.nextElement();
          if (listener instanceof Remote) {
            if (rd == null) {
              rd = new RemoteDisplayImpl(this);
            }
            listener.displayChanged(evt.cloneButDisplay(rd));
          } else {
            listener.displayChanged(evt.cloneButDisplay(this));
          }
        }
      }
    }
  }

  /** add a DisplayListener */
  public void addDisplayListener(DisplayListener listener) {
    ListenerVector.addElement(listener);
  }
 
  /** remove a DisplayListener */
  public void removeDisplayListener(DisplayListener listener) {
    if (listener != null) {
      ListenerVector.removeElement(listener);
    }
  }

  /** return the java.awt.Component (e.g., JPanel or AppletPanel)
      this DisplayImpl uses; returns null for an offscreen DisplayImpl */
  public Component getComponent() {
    return component;
  }

  public void setComponent(Component c) {
    component = c;
  }

  /** re-apply auto-scaling of ScalarMap ranges next time
      Display is triggered */
  public void reAutoScale() {
    initialize = true;
// printStack("reAutoScale");
  }

  /** if auto is true, re-apply auto-scaling of ScalarMap ranges
      every time Display is triggered */
  public void setAlwaysAutoScale(boolean a) {
    always_initialize = a;
  }

  public void reDisplayAll() {
    redisplay_all = true;
// printStack("reDisplayAll");
    notifyAction();
  }


  /* CTR 21 Sep 1999 - begin code for slaved displays */

  private Vector Slaves = new Vector();

  /** links a slave display to this display */
  void addSlave(RemoteSlaveDisplay display) {
    if (!Slaves.contains(display)) Slaves.add(display);
  }

  /** removes a link between a slave display and this display */
  void removeSlave(RemoteSlaveDisplay display) {
    if (Slaves.contains(display)) Slaves.remove(display);
  }

  /** removes all links between slave displays and this display */
  void removeAllSlaves() {
    Slaves.removeAllElements();
  }

  /** whether there are any slave displays linked to this display */
  public boolean hasSlaves() {
    return (!Slaves.isEmpty());
  }

  /** updates all linked slave displays with the given image */
  public void updateSlaves(BufferedImage img) {
    RemoteSlaveDisplay d;
    int width = img.getWidth();
    int height = img.getHeight();
    int type = img.getType();
    int[] pixels = new int[width*height];
    img.getRGB(0, 0, width, height, pixels, 0, width);
    for (int i=0; i<Slaves.size(); i++) {
      d = (RemoteSlaveDisplay) Slaves.elementAt(i);
      try {
        d.sendImage(pixels, width, height, type);
      }
      catch (RemoteException e) {
      }
    }
  }

  /* CTR 21 Sep 1999 - end code for slaved displays */


  /** link ref to this Display; this method may only be invoked
      after all links to ScalarMaps have been made */
  public void addReference(ThingReference ref)
         throws VisADException, RemoteException {
    if (!(ref instanceof DataReference)) {
      throw new ReferenceException("DisplayImpl.addReference: ref " +
                                   "must be DataReference");
    }
    addReference((DataReference) ref, null);
  }

  /** add a link to a DataReference object */
  void addLink(DataDisplayLink link)
        throws VisADException, RemoteException
  {
    super.addLink((ReferenceActionLink )link);
    notifyListeners(new DisplayReferenceEvent(this,
                                             DisplayEvent.REFERENCE_ADDED,
                                             link));
  }

  /** link ref to this Display; must be local DataReferenceImpl; this
      method may only be invoked after all links to ScalarMaps have
      been made; the ConstantMap array applies only to rendering ref */
  public void addReference(DataReference ref,
         ConstantMap[] constant_maps) throws VisADException, RemoteException {
    if (!(ref instanceof DataReferenceImpl)) {
      throw new RemoteVisADException("DisplayImpl.addReference: requires " +
                                     "DataReferenceImpl");
    }
    if (findReference(ref) != null) {
      throw new TypeException("DisplayImpl.addReference: link already exists");
    }
    DataRenderer renderer = displayRenderer.makeDefaultRenderer();
    DataDisplayLink[] links = {new DataDisplayLink(ref, this, this, constant_maps,
                                                   renderer, getLinkId())};
    addLink(links[0]);
    renderer.setLinks(links, this);
    synchronized (mapslock) {
      RendererVector.addElement(renderer);
    }
    initialize = true;
// printStack("addReference");
    notifyAction();
  }

  /** method for use by RemoteActionImpl.addReference that adapts this
      ActionImpl */
  void adaptedAddReference(RemoteDataReference ref, RemoteDisplay display,
       ConstantMap[] constant_maps) throws VisADException, RemoteException {
    if (findReference(ref) != null) {
      throw new TypeException("DisplayImpl.adaptedAddReference: " +
                              "link already exists");
    }
    DataRenderer renderer = displayRenderer.makeDefaultRenderer();
    DataDisplayLink[] links = {new DataDisplayLink(ref, this, display, constant_maps,
                                                   renderer, getLinkId())};
    addLink(links[0]);
    renderer.setLinks(links, this);
    synchronized (mapslock) {
      RendererVector.addElement(renderer);
    }
    initialize = true;
// printStack("adaptedAddReference");
    notifyAction();
  }

  /** link ref to this Display using the non-default renderer;
      must be local DataRendererImpls;
      this method may only be invoked after all links to ScalarMaps
      have been made;
      this is a method of DisplayImpl and RemoteDisplayImpl rather
      than Display - see Section 6.1 of the Developer's Guide for
      more information */
  public void addReferences(DataRenderer renderer, DataReference ref)
         throws VisADException, RemoteException {
    addReferences(renderer, new DataReference[] {ref}, null);
  }

  /** link ref to this Display using the non-default renderer;
      must be local DataRendererImpls;
      this method may only be invoked after all links to ScalarMaps
      have been made;
      the maps array applies only to rendering ref;
      this is a method of DisplayImpl and RemoteDisplayImpl rather
      than Display - see Section 6.1 of the Developer's Guide for
      more information */
  public void addReferences(DataRenderer renderer, DataReference ref,
                            ConstantMap[] constant_maps)
         throws VisADException, RemoteException {
    addReferences(renderer, new DataReference[] {ref},
                  new ConstantMap[][] {constant_maps});
  }

  /** link refs to this Display using the non-default renderer;
      must be local DataRendererImpls;
      this method may only be invoked after all links to ScalarMaps
      have been made; this is a method of DisplayImpl and
      RemoteDisplayImpl rather than Display - see Section 6.1 of the
      Developer's Guide for more information */
  public void addReferences(DataRenderer renderer, DataReference[] refs)
         throws VisADException, RemoteException {
    addReferences(renderer, refs, null);
  }

  /** link refs to this Display using the non-default renderer;
      must be local DataRendererImpls;
      this method may only be invoked after all links to ScalarMaps
      have been made;
      the maps[i] array applies only to rendering refs[i];
      this is a method of DisplayImpl and RemoteDisplayImpl rather
      than Display - see Section 6.1 of the Developer's Guide for
      more information */
  public void addReferences(DataRenderer renderer, DataReference[] refs,
                            ConstantMap[][] constant_maps)
         throws VisADException, RemoteException {
    if (refs.length < 1) {
      throw new DisplayException("DisplayImpl.addReferences: must have at " +
                                 "least one DataReference");
    }
    if (constant_maps != null && refs.length != constant_maps.length) {
      throw new DisplayException("DisplayImpl.addReferences: constant_maps " +
                                 "length must match refs length");
    }
    if (!displayRenderer.legalDataRenderer(renderer)) {
      throw new DisplayException("DisplayImpl.addReferences: illegal " +
                                 "DataRenderer class");
    }
    DataDisplayLink[] links = new DataDisplayLink[refs.length];
    for (int i=0; i< refs.length; i++) {
      if (!(refs[i] instanceof DataReferenceImpl)) {
        throw new RemoteVisADException("DisplayImpl.addReferences: requires " +
                                       "DataReferenceImpl");
      }
      if (findReference(refs[i]) != null) {
        throw new TypeException("DisplayImpl.addReferences: link already exists");
      }
      if (constant_maps == null) {
        links[i] = new DataDisplayLink(refs[i], this, this, null,
                                       renderer, getLinkId());
      }
      else {
        links[i] = new DataDisplayLink(refs[i], this, this, constant_maps[i],
                                       renderer, getLinkId());
      }
      addLink(links[i]);
    }
    renderer.setLinks(links, this);
    synchronized (mapslock) {
      RendererVector.addElement(renderer);
    }
    initialize = true;
// printStack("addReferences");
    notifyAction();
  }

  /** method for use by RemoteActionImpl.addReferences that adapts this
      ActionImpl; this allows a mix of local and remote refs */
  void adaptedAddReferences(DataRenderer renderer, DataReference[] refs,
       RemoteDisplay display, ConstantMap[][] constant_maps)
       throws VisADException, RemoteException {
    if (refs.length < 1) {
      throw new DisplayException("DisplayImpl.addReferences: must have at " +
                                 "least one DataReference");
    }
    if (constant_maps != null && refs.length != constant_maps.length) {
      throw new DisplayException("DisplayImpl.addReferences: constant_maps " +
                                 "length must match refs length");
    }
    if (!displayRenderer.legalDataRenderer(renderer)) {
      throw new DisplayException("DisplayImpl.addReferences: illegal " +
                                 "DataRenderer class");
    }
    DataDisplayLink[] links = new DataDisplayLink[refs.length];
    for (int i=0; i< refs.length; i++) {
      if (findReference(refs[i]) != null) {
        throw new TypeException("DisplayImpl.addReferences: link already exists");
      }
      if (refs[i] instanceof DataReferenceImpl) {
        // refs[i] is local
        if (constant_maps == null) {
          links[i] = new DataDisplayLink(refs[i], this, this, null,
                                         renderer, getLinkId());
        }   
        else {
          links[i] = new DataDisplayLink(refs[i], this, this, constant_maps[i],
                                         renderer, getLinkId());
        }
      }
      else {
        // refs[i] is remote
        if (constant_maps == null) {
          links[i] = new DataDisplayLink(refs[i], this, display, null,
                                         renderer, getLinkId());
        }   
        else {
          links[i] = new DataDisplayLink(refs[i], this, display, constant_maps[i],
                                         renderer, getLinkId());
        }
      }
      addLink(links[i]);
    }
    renderer.setLinks(links, this);
    synchronized (mapslock) {
      RendererVector.addElement(renderer);
    }
    initialize = true;
// printStack("adaptedAddReferences");
    notifyAction();
  }

  /** remove link to ref; must be local DataReferenceImpl; if ref was
      added as part of a DataReference  array passed to addReferences,
      remove links to all of them */
  public void removeReference(ThingReference ref)
         throws VisADException, RemoteException {
    if (!(ref instanceof DataReferenceImpl)) {
      throw new RemoteVisADException("ActionImpl.removeReference: requires " +
                                     "DataReferenceImpl");
    }
    adaptedDisplayRemoveReference((DataReference) ref);
  }

  /** remove link to a DataReference;
      method for use by RemoteActionImpl.removeReference that adapts this
      ActionImpl;
      because DataReference array input to adaptedAddReferences may be a
      mix of local and remote, we tolerate either here */
  void adaptedDisplayRemoveReference(DataReference ref)
       throws VisADException, RemoteException {
    DataDisplayLink link = (DataDisplayLink) findReference(ref);
    // don't throw an Exception if link is null: users may try to
    // remove all DataReferences added by a call to addReferences
    if (link == null) return;
    DataRenderer renderer = link.getRenderer();
    DataDisplayLink[] links = renderer.getLinks();
    synchronized (mapslock) {
      renderer.clearScene();
      RendererVector.removeElement(renderer);
    }
    removeLinks(links);

/* WLH 22 April 99
    initialize = true;
*/
  }

  /** remove all DataReference links */
  public void removeAllReferences()
         throws VisADException, RemoteException {
    synchronized (mapslock) {
      synchronized (RendererVector) {
        Iterator renderers = RendererVector.iterator();
        while (renderers.hasNext()) {
          DataRenderer renderer = (DataRenderer) renderers.next();
          renderer.clearScene();
          DataDisplayLink[] links = renderer.getLinks();
          renderers.remove();
          removeLinks(links);
        }
      }
      initialize = true;
// printStack("removeAllReferences");
    }
  }

  /** return a Vector containing all DataReferences */
  /** used by Control-s to notify this DisplayImpl that
      they have changed */
  public void controlChanged() {
    notifyAction();
/* WLH 29 Aug 98
    synchronized (this) {
      notify();
    }
*/
  }

  /** DisplayImpl always runs doAction to find out if there is
      work to do */
  public boolean checkTicks() {
    return true;
  }

  /** a Display is runnable;
      doAction is invoked by any event that requires a re-transform */
  public void doAction() throws VisADException, RemoteException {
    if (mapslock == null) return;
    synchronized (mapslock) {
      if (RendererVector == null || displayRenderer == null) {
        return;
      }
      displayRenderer.setWaitFlag(true);
      // set tickFlag-s in changed Control-s
      // clone MapVector to avoid need for synchronized access
      Vector tmap = (Vector) MapVector.clone();
      Enumeration maps = tmap.elements();
      while (maps.hasMoreElements()) {
        ScalarMap map = (ScalarMap) maps.nextElement();
        map.setTicks();
      }
  
      // set ScalarMap.valueIndex-s and valueArrayLength
      int n = getDisplayScalarCount();
      int[] scalarToValue = new int[n];
      for (int i=0; i<n; i++) scalarToValue[i] = -1;
      valueArrayLength = 0;
      maps = tmap.elements();
      while (maps.hasMoreElements()) {
        ScalarMap map = ((ScalarMap) maps.nextElement());
        DisplayRealType dreal = map.getDisplayScalar();
        if (dreal.isSingle()) {
          int j = getDisplayScalarIndex(dreal);
          if (scalarToValue[j] < 0) {
            scalarToValue[j] = valueArrayLength;
            valueArrayLength++;
          }
          map.setValueIndex(scalarToValue[j]);
        }
        else {
          map.setValueIndex(valueArrayLength);
          valueArrayLength++;
        }
      }
   
      // set valueToScalar and valueToMap arrays
      valueToScalar = new int[valueArrayLength];
      valueToMap = new int[valueArrayLength];
      maps = tmap.elements();
      while (maps.hasMoreElements()) {
        ScalarMap map = ((ScalarMap) maps.nextElement());
        DisplayRealType dreal = map.getDisplayScalar();
        valueToScalar[map.getValueIndex()] = getDisplayScalarIndex(dreal);
        valueToMap[map.getValueIndex()] = tmap.indexOf(map);
      }

      DataShadow shadow = null;
      // invoke each DataRenderer (to prepare associated Data objects
      // for transformation)
      // clone RendererVector to avoid need for synchronized access
      Vector temp = ((Vector) RendererVector.clone());
      Enumeration renderers;
      boolean go = false;
      if (initialize) {
        renderers = temp.elements();
        while (!go && renderers.hasMoreElements()) {
          DataRenderer renderer = (DataRenderer) renderers.nextElement();
          go |= renderer.checkAction();
        }
// System.out.println("initialize = " + initialize + " go = " + go);
      }
      if (redisplay_all) {
        go = true;
// System.out.println("redisplay_all = " + redisplay_all + " go = " + go);
        redisplay_all = false;
      }

      if (!initialize || go) {
        renderers = temp.elements();
        while (renderers.hasMoreElements()) {
          DataRenderer renderer = (DataRenderer) renderers.nextElement();
          shadow = renderer.prepareAction(go, initialize, shadow);
        }

        if (shadow != null) {
          // apply RealType ranges and animationSampling
          maps = tmap.elements();
          while(maps.hasMoreElements()) {
            ScalarMap map = ((ScalarMap) maps.nextElement());
            map.setRange(shadow);
          }
        }

        ScalarMap.equalizeFlow(tmap, Display.DisplayFlow1Tuple);
        ScalarMap.equalizeFlow(tmap, Display.DisplayFlow2Tuple);

        renderers = temp.elements();
        boolean badScale = false;
        while (renderers.hasMoreElements()) {
          DataRenderer renderer = (DataRenderer) renderers.nextElement();
          badScale |= renderer.getBadScale();
        }
        initialize = badScale;
        if (always_initialize) initialize = true;
/*
if (initialize) {
  System.out.println("badScale = " + badScale +
                     " always_initialize = " + always_initialize);
}
*/
        boolean transform_done = false;

        renderers = temp.elements();
        while (renderers.hasMoreElements()) {
          DataRenderer renderer = (DataRenderer) renderers.nextElement();
          transform_done |= renderer.doAction();
        }
        if (transform_done) {
          AnimationControl control =
            (AnimationControl) getControl(AnimationControl.class);
          if (control != null) {
            control.init();
          }

          synchronized (ControlVector) {
            Enumeration controls = ControlVector.elements();
            while(controls.hasMoreElements()) {
              Control cont = (Control) controls.nextElement();
              if (ValueControl.class.isInstance(cont)) {
                ((ValueControl) cont).init();
              }
            }
          }

          notifyListeners(DisplayEvent.TRANSFORM_DONE, 0, 0);
        }

      }

      // clear tickFlag-s in Control-s
      maps = tmap.elements();
      while(maps.hasMoreElements()) {
        ScalarMap map = (ScalarMap) maps.nextElement();
        map.resetTicks();
      }
      displayRenderer.setWaitFlag(false);
    } // end synchronized (mapslock)
  }

  /** return the default DisplayRenderer for this DisplayImpl */
  protected abstract DisplayRenderer getDefaultDisplayRenderer();

  /** return the DisplayRenderer associated with this DisplayImpl */
  public DisplayRenderer getDisplayRenderer() {
    return displayRenderer;
  }

  /** get a clone of RendererVector to avoid
      concurrent access by Display thread */
  public Vector getRendererVector() {
    return (Vector) RendererVector.clone();
  }

  public int getDisplayScalarCount() {
    return DisplayRealTypeVector.size();
  }

  public DisplayRealType getDisplayScalar(int index) {
    return (DisplayRealType) DisplayRealTypeVector.elementAt(index);
  }

  public int getDisplayScalarIndex(DisplayRealType dreal) {
    int dindex;
    synchronized (DisplayRealTypeVector) {
      DisplayTupleType tuple = dreal.getTuple();
      if (tuple != null) {
        int n = tuple.getDimension();
        for (int i=0; i<n; i++) {
          try {
            DisplayRealType ereal =
              (DisplayRealType) tuple.getComponent(i);
            int eindex = DisplayRealTypeVector.indexOf(ereal);
            if (eindex < 0) {
              DisplayRealTypeVector.addElement(ereal);
            }
          }
          catch (VisADException e) {
          }
        }
      }
      dindex = DisplayRealTypeVector.indexOf(dreal);
      if (dindex < 0) {
        DisplayRealTypeVector.addElement(dreal);
        dindex = DisplayRealTypeVector.indexOf(dreal);
      }
    }
    return dindex;
  }

  public int getScalarCount() {
    return RealTypeVector.size();
  }

  public ScalarType getScalar(int index) {
    return (ScalarType) RealTypeVector.elementAt(index);
  }

  public int getScalarIndex(ScalarType real) throws RemoteException {
    return RealTypeVector.indexOf(real);
  }

  /** add a ScalarMap to this Display;
      can only be invoked when no DataReference-s are
      linked to this Display */
  public void addMap(ScalarMap map)
         throws VisADException, RemoteException {
    synchronized (mapslock) {
      int index;
      if (!RendererVector.isEmpty()) {
        throw new DisplayException("DisplayImpl.addMap: RendererVector " +
                                   "must be empty");
      }
      DisplayRealType type = map.getDisplayScalar();
      if (!displayRenderer.legalDisplayScalar(type)) {
        throw new BadMappingException("DisplayImpl.addMap: " +
              map.getDisplayScalar() + " illegal for this DisplayRenderer");
      }
      if ((Display.LineWidth.equals(type) || Display.PointSize.equals(type))
          && !(map instanceof ConstantMap)) {
        throw new BadMappingException("DisplayImpl.addMap: " +
              map.getDisplayScalar() + " for ConstantMap only");
      }
      map.setDisplay(this);
  
      if (map instanceof ConstantMap) {
        synchronized (ConstantMapVector) {
          Enumeration maps = ConstantMapVector.elements();
          while(maps.hasMoreElements()) {
            ConstantMap map2 = (ConstantMap) maps.nextElement();
            if (map2.getDisplayScalar().equals(map.getDisplayScalar())) {
              throw new BadMappingException("Display.addMap: two ConstantMaps " +
                                "have the same DisplayScalar");
            }
          }
          ConstantMapVector.addElement(map);
        }
      }
      else { // !(map instanceof ConstantMap)
        // add to RealTypeVector and set ScalarIndex
        ScalarType real = map.getScalar();
        DisplayRealType dreal = map.getDisplayScalar();
        synchronized (MapVector) {
          Enumeration maps = MapVector.elements();
          while(maps.hasMoreElements()) {
            ScalarMap map2 = (ScalarMap) maps.nextElement();
            if (real.equals(map2.getScalar()) &&
                dreal.equals(map2.getDisplayScalar()) &&
                !dreal.equals(Display.Shape)) {
              throw new BadMappingException("Display.addMap: two ScalarMaps " +
                                     "with the same RealType & DisplayRealType");
            }
            if (dreal.equals(Display.Animation) &&
                map2.getDisplayScalar().equals(Display.Animation)) {
              throw new BadMappingException("Display.addMap: two RealTypes " +
                                            "are mapped to Animation");
            }
          }
          MapVector.addElement(map);
          needWidgetRefresh = true;
        }
        synchronized (RealTypeVector) {
          index = RealTypeVector.indexOf(real);
          if (index < 0) {
            RealTypeVector.addElement(real);
            index = RealTypeVector.indexOf(real);
          }
        }
        map.setScalarIndex(index);
        map.setControl();
      } // end !(map instanceof ConstantMap)
      addDisplayScalar(map);
      notifyListeners(new DisplayMapEvent(this, DisplayEvent.MAP_ADDED, map));

      // make sure we monitor all changes to this ScalarMap
      map.addScalarMapListener(displayMonitor);
    }
  }

  void addDisplayScalar(ScalarMap map) {
    int index;

    DisplayRealType dreal = map.getDisplayScalar();
    synchronized (DisplayRealTypeVector) {
      DisplayTupleType tuple = dreal.getTuple();
      if (tuple != null) {
        int n = tuple.getDimension();
        for (int i=0; i<n; i++) {
          try {
            DisplayRealType ereal =
              (DisplayRealType) tuple.getComponent(i);
            int eindex = DisplayRealTypeVector.indexOf(ereal);
            if (eindex < 0) {
              DisplayRealTypeVector.addElement(ereal);
            }
          }
          catch (VisADException e) {
          }
        }
      }
      index = DisplayRealTypeVector.indexOf(dreal);
      if (index < 0) {
        DisplayRealTypeVector.addElement(dreal);
        index = DisplayRealTypeVector.indexOf(dreal);
      }
    }
    map.setDisplayScalarIndex(index);
  }

  /** clear set of ScalarMap-s associated with this display;
      can only be invoked when no DataReference-s are
      linked to this Display */
  public void clearMaps() throws VisADException, RemoteException {
    synchronized (mapslock) {
      if (!RendererVector.isEmpty()) {
        throw new DisplayException("DisplayImpl.clearMaps: RendererVector " +
                                   "must be empty");
      }

      Enumeration maps;

      synchronized (MapVector) {
        maps = MapVector.elements();
        while(maps.hasMoreElements()) {
          ScalarMap map = (ScalarMap) maps.nextElement();
          map.nullDisplay();
          map.removeScalarMapListener(displayMonitor);
        }
        MapVector.removeAllElements();
        needWidgetRefresh = true;
      }
      synchronized (ConstantMapVector) {
        maps = ConstantMapVector.elements();
        while(maps.hasMoreElements()) {
          ConstantMap map = (ConstantMap) maps.nextElement();
          map.nullDisplay();
          map.removeScalarMapListener(displayMonitor);
        }
        ConstantMapVector.removeAllElements();
      }

      synchronized (ControlVector) {
        // clear Control-s associated with this Display
        maps = ControlVector.elements();
        while(maps.hasMoreElements()) {
          Control ctl = (Control )maps.nextElement();
          ctl.removeControlListener((ControlListener )displayMonitor);
        }
        ControlVector.removeAllElements();
        // one each GraphicsModeControl and ProjectionControl always exists
        Control control = (Control) getGraphicsModeControl();
        if (control != null) addControl(control);
        control = (Control) getProjectionControl();
        if (control != null) addControl(control);
        // don't forget RendererControl
        control = (Control) displayRenderer.getRendererControl();
        if (control != null) addControl(control);
      }
      // clear RealType-s from RealTypeVector
      // removeAllElements is synchronized
      RealTypeVector.removeAllElements();
      synchronized (DisplayRealTypeVector) {
        // clear DisplayRealType-s from DisplayRealTypeVector
        DisplayRealTypeVector.removeAllElements();
        // put system intrinsic DisplayRealType-s in DisplayRealTypeVector
        for (int i=0; i<DisplayRealArray.length; i++) {
          DisplayRealTypeVector.addElement(DisplayRealArray[i]);
        }
      }
      displayRenderer.clearAxisOrdinals();
      displayRenderer.setAnimationString(new String[] {null, null});
    }

    notifyListeners(new DisplayEvent(this, DisplayEvent.MAPS_CLEARED));
  }

  public Vector getMapVector() {
    return (Vector) MapVector.clone();
  }

  public Vector getConstantMapVector() {
    return (Vector) ConstantMapVector.clone();
  }

  public void addControl(Control control) {
    if (control != null && !ControlVector.contains(control)) {
      ControlVector.addElement(control);
      control.setIndex(ControlVector.indexOf(control));
      control.addControlListener((ControlListener )displayMonitor);
    }
  }

  /** only called for Control objects associated with 'single'
      DisplayRealType-s */
  public Control getControl(Class c) {
    synchronized (ControlVector) {
      Enumeration controls = ControlVector.elements();
      while(controls.hasMoreElements()) {
        Control control = (Control) controls.nextElement();
/* WLH 19 March 99
        if (c.equals(control.getClass())) return control;
*/
        if (c.isInstance(control)) return control;
      }
    }
    return null;
  }

  public Vector getControlVector() {
    return (Vector) ControlVector.clone();
  }


  /* CTR 4 October 1999 - begin code for this Display's Control widgets */

  /** whether the Control widget panel needs to be reconstructed */
  private boolean needWidgetRefresh = true;

  /** this Display's associated panel of Control widgets */
  private JPanel widgetPanel = null;

  /** get a GUI component containing this Display's Control widgets,
      creating the widgets as necessary */
  public Container getWidgetPanel() {
    if (needWidgetRefresh) {
      synchronized (MapVector) {
        // construct widget panel if needed
        if (widgetPanel == null) {
          widgetPanel = new JPanel();
          widgetPanel.setLayout(new BoxLayout(widgetPanel, BoxLayout.Y_AXIS));
        }
        else widgetPanel.removeAll();

        if (getLinks().size() > 0) {
          // GraphicsModeControl widget
          GMCWidget gmcw = new GMCWidget(getGraphicsModeControl());
          addToWidgetPanel(gmcw, false);
        }

        for (int i=0; i<MapVector.size(); i++) {
          ScalarMap sm = (ScalarMap) MapVector.elementAt(i);

          DisplayRealType drt = sm.getDisplayScalar();
          try {
            double[] a = new double[2];
            double[] b = new double[2];
            double[] c = new double[2];
            boolean scale = sm.getScale(a, b, c);
            if (scale) {
              // ScalarMap range widget
              RangeWidget rw = new RangeWidget(sm);
              addToWidgetPanel(rw, true);
            }
          }
          catch (VisADException exc) { }
          try {
            if (drt.equals(Display.RGB) || drt.equals(Display.RGBA)) {
              // ColorControl widget
              try {
                LabeledColorWidget lw = new LabeledColorWidget(sm);
                addToWidgetPanel(lw, true);
              }
              catch (VisADException exc) { }
              catch (RemoteException exc) { }
            }
            else if (drt.equals(Display.SelectValue)) {
              // ValueControl widget
              VisADSlider vs = new VisADSlider(sm);
              vs.setAlignmentX(JPanel.CENTER_ALIGNMENT);
              addToWidgetPanel(vs, true);
            }
            else if (drt.equals(Display.SelectRange)) {
              // RangeControl widget
              SelectRangeWidget srw = new SelectRangeWidget(sm);
              addToWidgetPanel(srw, true);
            }
            else if (drt.equals(Display.IsoContour)) {
              // ContourControl widget
              ContourWidget cw = new ContourWidget(sm);
              addToWidgetPanel(cw, true);
            }
            else if (drt.equals(Display.Animation)) {
              // AnimationControl widget
              AnimationWidget aw = new AnimationWidget(sm);
              addToWidgetPanel(aw, true);
            }
          }
          catch (VisADException exc) { }
          catch (RemoteException exc) { }
        }
      }
      needWidgetRefresh = false;
    }
    return widgetPanel;
  }

  /** add a component to the widget panel */
  private void addToWidgetPanel(Component c, boolean divide) {
    // add a thin, horizontal divider for separating widget panel components
    if (divide) widgetPanel.add(new JComponent() {
      public void paint(Graphics g) {
        int w = getSize().width;
        g.setColor(Color.white);
        g.drawRect(0, 0, w-2, 6);
        g.drawRect(2, 2, w-4, 2);
        g.setColor(Color.black);
        g.drawRect(1, 1, w-3, 3);
      }

      public Dimension getMinimumSize() {
        return new Dimension(0, 6);
      }

      public Dimension getPreferredSize() {
        return new Dimension(0, 6);
      }

      public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 6);
      }
    });
    widgetPanel.add(c);
  }

  /* CTR 4 October 1999 - end code for this Display's Control widgets */


  public int getValueArrayLength() {
    return valueArrayLength;
  }

  public int[] getValueToScalar() {
    return valueToScalar;
  }

  public int[] getValueToMap() {
    return valueToMap;
  }

  /** return the ProjectionControl associated with this DisplayImpl */
  public abstract ProjectionControl getProjectionControl();
 
  /** return the GraphicsModeControl associated with this DisplayImpl */
  public abstract GraphicsModeControl getGraphicsModeControl(); 

  /** wait for millis milliseconds
   *  @deprecated Use <CODE>new visad.util.Delay(millis)</CODE> instead.
   */
  public static void delay(int millis) {
    new visad.util.Delay(millis);
  }

  /** print a stack dump */
  public static void printStack(String message) {
    try {
      throw new DisplayException("printStack: " + message);
    }
    catch (DisplayException e) {
      e.printStackTrace();
    }
  }

  /** given their complexity, its reasonable that DisplayImpl
      objects are only equal to themselves */
  public boolean equals(Object obj) {
    return (obj == this);
  }

  public Vector getRenderers()
  {
    return (Vector )RendererVector.clone();
  }

  public int getAPI()
	throws VisADException
  {
    throw new VisADException("No API specified");
  }

  /**
   * Returns the <CODE>DisplayMonitor</CODE> associated with this
   * <CODE>Display</CODE>.
   */
  public DisplayMonitor getDisplayMonitor()
  {
    return displayMonitor;
  }

  /**
   * Returns the <CODE>DisplaySync</CODE> associated with this
   * <CODE>Display</CODE>.
   */
  public DisplaySync getDisplaySync()
  {
    return displaySync;
  }

  public void setMouseBehavior(MouseBehavior m) {
    mouse = m;
  }

  // CTR 18 Oct 1999
  public MouseBehavior getMouseBehavior() {
    return mouse;
  }

  public double[] make_matrix(double rotx, double roty, double rotz,
         double scale, double transx, double transy, double transz) {
    if (mouse != null) {
      return mouse.make_matrix(rotx, roty, rotz, scale, transx, transy, transz);
    }
    else {
      return null;
    }
  }

  public double[] multiply_matrix(double[] a, double[] b) {
    if (mouse != null && a != null && b != null) {
      return mouse.multiply_matrix(a, b);
    }
    else {
      return null;
    }
  }

  /** return a captured image of the display */
  public BufferedImage getImage() {
    return displayRenderer.getImage();
  }

  /** return a captured image of the display;
      synchronize if sync */
  public BufferedImage getImage(boolean sync) {
    if (sync) new Syncher(this);
    return displayRenderer.getImage();
  }

  public String toString() {
    return toString("");
  }

  public String toString(String pre) {
    String s = pre + "Display\n";
    Enumeration maps = MapVector.elements();
    while(maps.hasMoreElements()) {
      ScalarMap map = (ScalarMap) maps.nextElement();
      s = s + map.toString(pre + "    ");
    }
    maps = ConstantMapVector.elements();
    while(maps.hasMoreElements()) {
      ConstantMap map = (ConstantMap) maps.nextElement();
      s = s + map.toString(pre + "    ");
    }
    return s;
  }

  private class Syncher extends Object implements DisplayListener {

    private ProjectionControl control;
    int count;

    Syncher(DisplayImpl display) {
      control = display.getProjectionControl();
      count = -1;
      display.disableAction();
      display.addDisplayListener(this);
      display.reDisplayAll();
      display.enableAction();
      try {
        synchronized (this) {
          this.wait();
        }
      }
      catch(InterruptedException e) {
      }
      display.removeDisplayListener(this);
    }

    public void displayChanged(DisplayEvent e)
           throws VisADException, RemoteException {
      if (e.getId() == DisplayEvent.TRANSFORM_DONE) {
        count = 2;
        control.setMatrix(control.getMatrix());
      }
      else if (e.getId() == DisplayEvent.FRAME_DONE) {
        if (count > 0) {
          control.setMatrix(control.getMatrix());
          count--;
        }
        else if (count == 0) {
          synchronized (this) {
            this.notify();
          }
          count--;
        }
      }
    }
  }

}

