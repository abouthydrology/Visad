/*
 * Copyright 1998, University Corporation for Atmospheric Research
 * All Rights Reserved.
 * See file LICENSE for copying and redistribution conditions.
 *
 * $Id: SkewTDisplay.java,v 1.9 1998-08-24 15:24:47 steve Exp $
 */

package visad.meteorology;

import com.sun.java.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.rmi.RemoteException;
import visad.ConstantMap;
import visad.ContourControl;
import visad.CoordinateSystem;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.Display;
import visad.DisplayRealType;
import visad.FlatField;
import visad.FunctionType;
import visad.Linear2DSet;
import visad.MathType;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarMap;
import visad.SI;
import visad.Set;
import visad.Unit;
import visad.VisADException;
import visad.data.netcdf.Plain;
import visad.data.netcdf.QuantityMap;
import visad.data.netcdf.units.ParseException;
import visad.java2d.DisplayImplJ2D;


/**
 * VisAD display bean for a Skew T, Log P Diagram (alias "Skew-T Chart").
 *
 * @author Steven R. Emmerson
 */
public class
SkewTDisplay
    implements	Serializable
{
    /**
     * The VisAD display.
     */
    private final DisplayImplJ2D		display;

    /**
     * The sounding property.
     */
    private Sounding				sounding;

    /**
     * The sounding data-reference.
     */
    private final DataReference			soundingRef;

    /**
     * Whether or not the display has been configured.
     */
    private boolean				displayInitialized;

    /**
     * Supports property changes.
     */
    private final PropertyChangeSupport		changes;

    /**
     * The Skew T, log p coordinate system.
     */
    private final SkewTCoordinateSystem		skewTCoordSys;

    /**
     * The potential temperature coordinate system.
     */
    private final ThetaCoordinateSystem		thetaCoordSys;

    /**
     * The saturation equivalent potential temperature coordinate system.
     */
    private final ThetaESCoordinateSystem	thetaESCoordSys;

    /**
     * The interval between isotherms.
     */
    private final float				deltaTemperature = 10f;

    /**
     * The interval between potential isotherms.
     */
    private final float				deltaTheta = 10f;

    /**
     * The base isotherm.
     */
    private final float				baseIsotherm = 0f;

    /**
     * The DisplayRenderer.
     */
    private final SkewTDisplayRenderer		displayRenderer;

    /**
     * Temperature sounding constant maps.
     */
    private ConstantMap[]			soundingConstantMaps;

    /**
     * Temperature field constant maps
     */
    private ConstantMap[]			temperatureConstantMaps =
	new ConstantMap[0];

    /**
     * Potential temperature field constant maps
     */
    private ConstantMap[]			thetaConstantMaps =
	new ConstantMap[0];

    /**
     * Saturation equivalent potential temperature constant maps.
     */
    private ConstantMap[]			thetaESConstantMaps;


    /**
     * Constructs from nothing.
     */
    public
    SkewTDisplay()
	throws	VisADException, RemoteException, ParseException
    {
	displayRenderer = new SkewTDisplayRenderer();

	sounding = null;
	soundingRef = new DataReferenceImpl("soundingRef");
	displayInitialized = false;
	changes = new PropertyChangeSupport(this);
	display = new DisplayImplJ2D("Skew T, Log P Diagram",
				     displayRenderer);
	skewTCoordSys = displayRenderer.skewTCoordSys;
	thetaCoordSys = displayRenderer.thetaCoordSys;
	thetaESCoordSys = displayRenderer.thetaESCoordSys;

	JFrame jframe = new JFrame("Skew-T Chart");
	jframe.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {System.exit(0);}
	});
	jframe.getContentPane().add(display.getComponent());
	jframe.setSize(256, 256);
	jframe.setVisible(true);

	soundingConstantMaps = new ConstantMap[] {
	    new ConstantMap(1., Display.Red),
	    new ConstantMap(0., Display.Blue),
	    new ConstantMap(0., Display.Green),
	    new ConstantMap(3.0, Display.LineWidth)};

	thetaESConstantMaps = new ConstantMap[] {
	    new ConstantMap(0., Display.Red),
	    new ConstantMap(0., Display.Blue),
	    new ConstantMap(1., Display.Green)
	};
    }


    /**
     * Displays a given sounding.
     */
    protected void
    display(Sounding sounding)
	throws	RemoteException, VisADException
    {
	soundingRef.setData(sounding);

	if (!displayInitialized)
	{
	    /*
	     * Map the X and Y coordinates of the various background fields
	     * to the display X and Y coordinates.  This is necessary for
	     * contouring of the various fields -- though it does have the
	     * unfortunate effect of making XAxis and YAxis value readouts
	     * appear.
	     */
	    ScalarMap	xMap = new ScalarMap(RealType.XAxis, Display.XAxis);
	    ScalarMap	yMap = new ScalarMap(RealType.YAxis, Display.YAxis);
	    display.addMap(xMap);
	    display.addMap(yMap);

	    /*
	     * Map the sounding types to display types.  NB: Because of this
	     * mapping, the display will have a value readout for regular
	     * temperature values.  Consequently, we don't need to create
	     * one for the regular temperature field.
	     */
	    ScalarMap	soundingPressureMap = new ScalarMap(
		sounding.getPressureType(), displayRenderer.pressure);
	    soundingPressureMap.setRangeByUnits();
	    display.addMap(soundingPressureMap);

	    ScalarMap	soundingTemperatureMap = new ScalarMap(
		sounding.getTemperatureType(), displayRenderer.temperature);
	    soundingTemperatureMap.setRangeByUnits();
	    display.addMap(soundingTemperatureMap);

	    /*
	     * Define the temperature type for the various
	     * temperature fields which will be contoured.
	     */
	    RealType	isothermType = new RealType("isotherm");

	    /*
	     * Map the temperature parameter of the various temperature
	     * fields to display contours.
	     */
	    ScalarMap	contourMap = new ScalarMap(isothermType,
		Display.IsoContour);
	    display.addMap(contourMap);
	    ContourControl	control =
		(ContourControl)contourMap.getControl();
	    Unit	temperatureUnit = skewTCoordSys.getTemperatureUnit();
	    control.setContourInterval(deltaTemperature,
		(float)temperatureUnit.toThis(0.f, SI.kelvin),
		Float.POSITIVE_INFINITY, 0f);

	    /*
	     * Establish value readouts for the non-sounding temperature
	     * parameters.
	     */
	    RealType	type = new RealType("potential_temperature");
	    ScalarMap	fieldTemperatureMap = new ScalarMap(type, 
		displayRenderer.theta);
	    display.addMap(fieldTemperatureMap);

	    type = new RealType("saturation_equivalent_potential_temperature");
	    fieldTemperatureMap = new ScalarMap(type, displayRenderer.thetaES);
	    display.addMap(fieldTemperatureMap);

	    /*
	     * Create the temperature fields.
	     */
	    DataReferenceImpl	temperatureRef = 
		createTemperatureField("temperature", isothermType,
		    skewTCoordSys.viewport, 2, 2, skewTCoordSys);
	    DataReferenceImpl	thetaRef =
		createTemperatureField("potential_temperature", isothermType,
		    thetaCoordSys.viewport, 10, 10, thetaCoordSys);
	    DataReferenceImpl	thetaESRef = createTemperatureField(
		"saturation_equivalent_potential_temperature", isothermType,
		thetaESCoordSys.viewport, 20, 20, thetaESCoordSys);

	    /*
	     * Add the temperature fields to the display.
	     */
	    display.addReference(thetaESRef, thetaESConstantMaps);
	    display.addReference(thetaRef, thetaConstantMaps);
	    display.addReference(temperatureRef, temperatureConstantMaps);
	    display.addReference(soundingRef, soundingConstantMaps);

	    displayInitialized = true;
	}
    }


    /**
     * Creates a particular temperature field.  Maps display points to
     * coordinate system points and stores the temperature values in the
     * field's range.
     *
     * @param name		The name of the parameter (e.g. 
     *				"potential_temperature") for the purpose of
     *				naming the returned DdataReference.
     * @param rangeType		The RealType of the parameter.
     * @param viewport		The display viewport.
     * @param nx		The number of samples in the X dimension.
     * @param ny		The number of samples in the Y dimension.
     * @param coordSys		The coordinate system transform.
     * @return			Data reference to created field.
     * @exception VisADException	Can't create necessary VisAD object.
     * @exception RemoteException	Remote access failure.
     */
    protected DataReferenceImpl
    createTemperatureField(String name, RealType rangeType,
	    Rectangle2D viewport, int nx, int ny, CoordinateSystem coordSys)
	throws	VisADException, RemoteException
    {
	MathType	domainType = 
	    new RealTupleType(new RealType[] {RealType.XAxis, RealType.YAxis});
	FunctionType	funcType = new FunctionType(domainType, rangeType);
	Linear2DSet	set = new Linear2DSet(domainType,
	    viewport.getX(), viewport.getX() + viewport.getWidth(), nx,
	    viewport.getY(), viewport.getY() + viewport.getHeight(), ny);
	FlatField	temperature = new FlatField(funcType, set);
	float[][]	xyCoords = set.getSamples();
	float[][]	ptCoords = coordSys.fromReference(
	    new float[][] {xyCoords[0], xyCoords[1], null});

	temperature.setSamples(new float[][] {ptCoords[1]}, /*copy=*/false);

	/*
	 * Create a data-reference for the temperature field.
	 */
	DataReferenceImpl	temperatureRef =
	    new DataReferenceImpl(name + "_ref");
	temperatureRef.setData(temperature);

	return temperatureRef;
    }


    /**
     * Sets the sounding property.
     */
    public synchronized void
    setSounding(Sounding sounding)
    {
	try
	{
	    Sounding	oldSounding = this.sounding;

	    display(sounding);
	    this.sounding = sounding;
	    changes.firePropertyChange("sounding", oldSounding, sounding);
	}
	catch (Exception e)
	{
	    String	reason = e.getMessage();

	    System.err.println("Couldn't display sounding {" +
		sounding + "}" + (reason == null ? "" : (": " + reason)));
	}
    }


    /**
     * Tests this class.
     */
    public static void
    main(String[] args)
	throws Exception
    {
	/*
	 * Create and display a Skew T, Log P Diagram.
	 */
	SkewTDisplay	display = new SkewTDisplay();

	QuantityMap.push(MetQuantityDB.instance());

	display.setSounding(
	    new Sounding((FlatField)new Plain().open("sounding.nc")));
    }
}
