/*
 * Copyright 1998, University Corporation for Atmospheric Research
 * All Rights Reserved.
 * See file LICENSE for copying and redistribution conditions.
 *
 * $Id: NcTuple.java,v 1.4 1998-04-02 20:49:48 visad Exp $
 */

package visad.data.netcdf.in;

import java.io.IOException;
import visad.DataImpl;
import visad.MathType;
import visad.RealTupleType;
import visad.RealType;
import visad.Tuple;
import visad.TupleType;
import visad.VisADException;


/**
 * The NcTuple class adapts a tuple of netCDF data objects to a VisAD Tuple.
 */
class
NcTuple
    extends	NcData
{
    /**
     * The netCDF data objects.
     */
    protected final NcData[]	ncDatas;


    /**
     * Construct from an array of netCDF data objects.
     *
     * @param ncDatas	The netCDF data objects constituting the tuple.
     * @precondition	<code>ncDatas.length >= 2</code>.
     * @exception VisADException
     *			Problem in core VisAD.  Probably some VisAD object
     *			couldn't be created.
     */
    NcTuple(NcData[] ncDatas)
	throws VisADException
    {
	super(getTupleType(ncDatas));
	this.ncDatas = ncDatas;
    }


    /**
     * Return the VisAD MathType of the given, netCDF data objects.
     *
     * @param ncDatas	The netCDF data objects.
     * @prcondition	<code>ncDatas.length >= 2</code>.
     * @return		The VisAD MathType of the collection of netCDF data
     *			objects.
     */
    private static TupleType
    getTupleType(NcData[] ncDatas)
	throws VisADException
    {
	int		numComponents = ncDatas.length;
	MathType[]	mathTypes = new MathType[numComponents];
	boolean		allRealTypes = true;

	for (int i = 0; i < numComponents; ++i)
	{
	    mathTypes[i] = ncDatas[i].getMathType();
	    if (allRealTypes && !(mathTypes[i] instanceof RealType))
		allRealTypes = false;
	}

	return allRealTypes
		    ? new RealTupleType((RealType[])mathTypes)
		    : new TupleType(mathTypes);
    }


    /**
     * Return the VisAD data object corresponding to this netCDF data object.
     *
     * @return		The VisAD data object corresponding to the netCDF
     *			data object.
     * @exception VisADException
     *			Problem in core VisAD.  Probably some VisAD object
     *			couldn't be created.
     * @exception IOException
     *			Data access I/O failure.
     */
    DataImpl
    getData()
	throws VisADException, IOException
    {
	DataImpl[]	datas = new DataImpl[ncDatas.length];

	for (int i = 0; i < ncDatas.length; ++i)
	    datas[i] = ncDatas[i].getData();

	return new Tuple((TupleType)mathType, datas, /*copy=*/false);
    }


    /**
     * Return a proxy for the VisAD data object corresponding to this 
     * netCDF data object.
     *
     * @return		The VisAD data object corresponding to the netCDF
     *			data object.
     * @exception VisADException
     *			Problem in core VisAD.  Probably some VisAD object
     *			couldn't be created.
     * @exception IOException
     *			Data access I/O failure.
     */
    DataImpl
    getProxy()
	throws VisADException, IOException
    {
	DataImpl[]	datas = new DataImpl[ncDatas.length];

	for (int i = 0; i < ncDatas.length; ++i)
	    datas[i] = ncDatas[i].getProxy();

	return new Tuple((TupleType)mathType, datas, /*copy=*/false);
    }
}
