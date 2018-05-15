package gov.noaa.pmel.dashboard.dsg;

import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.DoubleDashDataType;
import gov.noaa.pmel.dashboard.datatype.IntDashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.datatype.StringDashDataType;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;


public class DsgNcFile extends File {

    private static final long serialVersionUID = -7695491814772713480L;

    private static final String DSG_VERSION = "DsgNcFile 2.0";
    private static final String TIME_ORIGIN_ATTRIBUTE = "01-JAN-1970 00:00:00";

    private DsgMetadata metadata;
    private StdDataArray stddata;

    /**
     * See {@link java.io.File#File(java.lang.String)} The internal metadata and data array references are set null.
     */
    public DsgNcFile(String filename) {
        super(filename);
        metadata = null;
        stddata = null;
    }

    /**
     * See {@link java.io.File#File(java.io.File, java.lang.String)} The internal metadata and data array references are
     * set null.
     */
    public DsgNcFile(File parent, String child) {
        super(parent, child);
        metadata = null;
        stddata = null;
    }

    /**
     * Adds the missing_value, _FillValue, long_name, standard_name, ioos_category, and units attributes to the given
     * variables in the given NetCDF file.
     *
     * @param ncfile
     *         NetCDF file being written containing the variable
     * @param var
     *         the variables to add attributes to
     * @param missVal
     *         if not null, the value for the missing_value and _FillValue attributes
     * @param longName
     *         if not null and not {@link DashboardUtils#STRING_MISSING_VALUE}, the value for the long_name attribute
     * @param standardName
     *         if not null and not {@link DashboardUtils#STRING_MISSING_VALUE}, the value for the standard_name
     *         attribute
     * @param ioosCategory
     *         if not null and not {@link DashboardUtils#STRING_MISSING_VALUE}, the value for the ioos_category
     *         attribute
     * @param units
     *         if not null and not {@link DashboardUtils#STRING_MISSING_VALUE}, the value for the units attribute
     */
    private void addAttributes(NetcdfFileWriter ncfile, Variable var, Number missVal,
            String longName, String standardName, String ioosCategory, String units) {
        if ( missVal != null ) {
            ncfile.addVariableAttribute(var, new Attribute("missing_value", missVal));
            ncfile.addVariableAttribute(var, new Attribute("_FillValue", missVal));
        }
        if ( (longName != null) && !DashboardUtils.STRING_MISSING_VALUE.equals(longName) ) {
            ncfile.addVariableAttribute(var, new Attribute("long_name", longName));
        }
        if ( (standardName != null) && !DashboardUtils.STRING_MISSING_VALUE.equals(standardName) ) {
            ncfile.addVariableAttribute(var, new Attribute("standard_name", standardName));
        }
        if ( (ioosCategory != null) && !DashboardUtils.STRING_MISSING_VALUE.equals(ioosCategory) ) {
            ncfile.addVariableAttribute(var, new Attribute("ioos_category", ioosCategory));
        }
        if ( (units != null) && !DashboardUtils.STRING_MISSING_VALUE.equals(units) ) {
            ncfile.addVariableAttribute(var, new Attribute("units", units));
        }
    }

    /**
     * Creates this NetCDF DSG file with the given metadata and standardized user provided data.  The internal metadata
     * reference is updated to the given DsgMetadata object and the internal data array reference is updated to a new
     * standardized data array object created from the appropriate user provided data. Every data sample must have a
     * valid longitude, latitude, sample depth, and complete date and time specification, to at least the minute.  If
     * the seconds of the time is not provided, zero seconds will be used.
     *
     * @param metaData
     *         metadata for the dataset
     * @param userStdData
     *         standardized user-provided data
     * @param dataFileTypes
     *         known data types for data files
     *
     * @throws IllegalArgumentException
     *         if any argument is null, if any of the data types in userStdData is {@link DashboardServerUtils#UNKNOWN}
     *         if any sample longitude, latitude, sample depth is missing, if any sample time cannot be computed
     * @throws IOException
     *         if creating the NetCDF file throws one
     * @throws InvalidRangeException
     *         if creating the NetCDF file throws one
     * @throws IllegalAccessException
     *         if creating the NetCDF file throws one
     */
    public void create(DsgMetadata metaData, StdUserDataArray userStdData, KnownDataTypes dataFileTypes)
            throws IllegalArgumentException, IOException, InvalidRangeException, IllegalAccessException {
        if ( metaData == null )
            throw new IllegalArgumentException("no metadata given");
        metadata = metaData;
        if ( userStdData == null )
            throw new IllegalArgumentException("no data given");

        // The following verifies lon, lat, depth, and time
        // Adds time and, if not already present, year, month, day, hour, minute, and second.
        stddata = new StdDataArray(userStdData, dataFileTypes);

        create(metadata, stddata);
    }

    /**
     * Creates this NetCDF DSG file with the given metadata and standardized data for data files.  The internal metadata
     * and stddata references are updated to the given DsgMetadata and StdDataArray object.  Every data sample should
     * have a valid longitude, latitude, sample depth, year, month of year, day of month, hour of day, minute of hour,
     * second of minute, time, sample number, and WOCE autocheck value, although this is not fully verified.
     *
     * @param metaData
     *         metadata for the dataset
     * @param fileData
     *         standardized data appropriate for data files
     *
     * @throws IllegalArgumentException
     *         if any argument is null, or if there is no longitude, latitude, sample depth, year, month of year, day of
     *         month, hour of day, minute of hour, or second of minute, or time data column
     * @throws IOException
     *         if creating the NetCDF file throws one
     * @throws InvalidRangeException
     *         if creating the NetCDF file throws one
     * @throws IllegalAccessException
     *         if creating the NetCDF file throws one
     */
    public void create(DsgMetadata metaData, StdDataArray fileData)
            throws IllegalArgumentException, IOException, InvalidRangeException, IllegalAccessException {
        if ( metaData == null )
            throw new IllegalArgumentException("no metadata given");
        metadata = metaData;
        if ( fileData == null )
            throw new IllegalArgumentException("no data given");
        stddata = fileData;
        // Quick check of data column indices already assigned in StdDataArray
        if ( !stddata.hasLongitude() )
            throw new IllegalArgumentException("no longitude data column");
        if ( !stddata.hasLatitude() )
            throw new IllegalArgumentException("no latitude data column");
        if ( !stddata.hasSampleDepth() )
            throw new IllegalArgumentException("no sample depth data column");
        if ( !stddata.hasYear() )
            throw new IllegalArgumentException("no year data column");
        if ( !stddata.hasMonthOfYear() )
            throw new IllegalArgumentException("no month of year data column");
        if ( !stddata.hasDayOfMonth() )
            throw new IllegalArgumentException("no day of month data column");
        if ( !stddata.hasHourOfDay() )
            throw new IllegalArgumentException("no hour of day data column");
        if ( !stddata.hasMinuteOfHour() )
            throw new IllegalArgumentException("no minute of hour data column");
        if ( !stddata.hasSecondOfMinute() )
            throw new IllegalArgumentException("no second of minute data column");

        NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(Version.netcdf3, getPath());
        try {
            // According to the CF standard if a file only has one trajectory,
            // then the trajectory dimension is not necessary.
            // However, who knows what would break downstream from this process without it...
            Dimension traj = ncfile.addDimension(null, "trajectory", 1);

            // There will be a number of trajectory variables of type character from the metadata.
            // Which is the longest?
            int maxMetaChar = metadata.getMaxStringLength();
            Dimension metaStringLen = ncfile.addDimension(null, "metadata_string_length", maxMetaChar);
            List<Dimension> metaStringDims = new ArrayList<Dimension>();
            metaStringDims.add(traj);
            metaStringDims.add(metaStringLen);

            List<Dimension> trajDims = new ArrayList<Dimension>();
            trajDims.add(traj);

            int numSamples = stddata.getNumSamples();
            Dimension obslen = ncfile.addDimension(null, "obs", numSamples);
            List<Dimension> dataDims = new ArrayList<Dimension>();
            dataDims.add(obslen);

            int maxDataChar = stddata.getMaxStringLength();
            Dimension dataStringLen = ncfile.addDimension(null, "data_string_length", maxDataChar);
            List<Dimension> dataStringDims = new ArrayList<Dimension>();
            dataStringDims.add(obslen);
            dataStringDims.add(dataStringLen);

            ncfile.addGroupAttribute(null, new Attribute("featureType", "Trajectory"));
            ncfile.addGroupAttribute(null, new Attribute("Conventions", "CF-1.6"));
            ncfile.addGroupAttribute(null, new Attribute("history", DSG_VERSION));

            // Add the "num_obs" variable which will be assigned using the number of data points
            Variable var = ncfile.addVariable(null, "num_obs", DataType.INT, trajDims);
            ncfile.addVariableAttribute(var, new Attribute("sample_dimension", "obs"));
            ncfile.addVariableAttribute(var, new Attribute("long_name", "Number of Observations"));
            ncfile.addVariableAttribute(var, new Attribute("missing_value", DashboardUtils.INT_MISSING_VALUE));
            ncfile.addVariableAttribute(var, new Attribute("_FillValue", DashboardUtils.INT_MISSING_VALUE));

            String varName;
            // Make netCDF variables of all the metadata and data variables
            for (DashDataType<?> dtype : metadata.valuesMap.keySet()) {
                varName = dtype.getVarName();
                if ( dtype instanceof StringDashDataType ) {
                    // Metadata Strings
                    var = ncfile.addVariable(null, varName, DataType.CHAR, metaStringDims);
                    // No missing_value, _FillValue, or units for strings
                    addAttributes(ncfile, var, null, dtype.getDescription(),
                            dtype.getStandardName(), dtype.getCategoryName(), DashboardUtils.STRING_MISSING_VALUE);
                    if ( DashboardServerUtils.DATASET_ID.typeNameEquals(dtype) ) {
                        ncfile.addVariableAttribute(var, new Attribute("cf_role", "trajectory_id"));
                    }
                }
                else if ( dtype instanceof IntDashDataType ) {
                    // Metadata Integers
                    var = ncfile.addVariable(null, varName, DataType.INT, trajDims);
                    addAttributes(ncfile, var, DashboardUtils.INT_MISSING_VALUE, dtype.getDescription(),
                            dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
                }
                else if ( dtype instanceof DoubleDashDataType ) {
                    // Metadata Doubles
                    var = ncfile.addVariable(null, varName, DataType.DOUBLE, trajDims);
                    addAttributes(ncfile, var, DashboardUtils.FP_MISSING_VALUE, dtype.getDescription(),
                            dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
                    if ( DashboardServerUtils.TIME_UNITS.get(0).equals(dtype.getUnits().get(0)) ) {
                        // Additional attribute giving the time origin (although also mentioned in the units)
                        ncfile.addVariableAttribute(var, new Attribute("time_origin", TIME_ORIGIN_ATTRIBUTE));
                    }
                }
                else {
                    throw new IllegalArgumentException("unexpected unknown metadata type: " + dtype.toString());
                }
            }

            boolean timeFound = false;
            List<DashDataType<?>> dataTypes = stddata.getDataTypes();
            for (DashDataType<?> dtype : dataTypes) {
                varName = dtype.getVarName();
                if ( dtype instanceof StringDashDataType ) {
                    // Data Strings
                    var = ncfile.addVariable(null, varName, DataType.CHAR, dataStringDims);
                    // No missing_value, _FillValue, or units for characters
                    addAttributes(ncfile, var, null, dtype.getDescription(),
                            dtype.getStandardName(), dtype.getCategoryName(), DashboardUtils.STRING_MISSING_VALUE);
                }
                else if ( dtype instanceof IntDashDataType ) {
                    // Data Integers
                    var = ncfile.addVariable(null, varName, DataType.INT, dataDims);
                    addAttributes(ncfile, var, DashboardUtils.INT_MISSING_VALUE, dtype.getDescription(),
                            dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
                }
                else if ( dtype instanceof DoubleDashDataType ) {
                    // Data Doubles
                    var = ncfile.addVariable(null, varName, DataType.DOUBLE, dataDims);
                    addAttributes(ncfile, var, DashboardUtils.FP_MISSING_VALUE, dtype.getDescription(),
                            dtype.getStandardName(), dtype.getCategoryName(), dtype.getFileStdUnit());
                    if ( DashboardServerUtils.TIME.typeNameEquals(dtype) ) {
                        // Additional attribute giving the time origin (although also mentioned in the units)
                        ncfile.addVariableAttribute(var, new Attribute("time_origin", TIME_ORIGIN_ATTRIBUTE));
                        timeFound = true;
                    }
                    if ( dtype.getStandardName().endsWith("depth") ) {
                        ncfile.addVariableAttribute(var, new Attribute("positive", "down"));
                    }
                }
                else {
                    throw new IllegalArgumentException("unexpected unknown data type: " + dtype.toString());
                }
            }
            if ( !timeFound )
                throw new IllegalArgumentException("no time data column");

            ncfile.create();

            // The header has been created.  Now let's fill it up.
            var = ncfile.findVariable("num_obs");
            if ( var == null )
                throw new RuntimeException("Unexpected failure to find ncfile variable num_obs");
            ArrayInt.D1 obscount = new ArrayInt.D1(1);
            obscount.set(0, numSamples);
            ncfile.write(var, obscount);

            for (Entry<DashDataType<?>,Object> entry : metadata.getValuesMap().entrySet()) {
                DashDataType<?> dtype = entry.getKey();
                varName = dtype.getVarName();
                var = ncfile.findVariable(varName);
                if ( var == null )
                    throw new RuntimeException("Unexpected failure to find ncfile variable " + varName);

                if ( dtype instanceof StringDashDataType ) {
                    // Metadata Strings
                    String dvalue = (String) entry.getValue();
                    if ( dvalue == null )
                        dvalue = DashboardUtils.STRING_MISSING_VALUE;
                    ArrayChar.D2 mvar = new ArrayChar.D2(1, maxMetaChar);
                    mvar.setString(0, dvalue);
                    ncfile.write(var, mvar);
                }
                else if ( dtype instanceof IntDashDataType ) {
                    // Metadata Integers
                    Integer dvalue = (Integer) entry.getValue();
                    if ( dvalue == null )
                        dvalue = DashboardUtils.INT_MISSING_VALUE;
                    ArrayInt.D1 mvar = new ArrayInt.D1(1);
                    mvar.set(0, dvalue);
                    ncfile.write(var, mvar);
                }
                else if ( dtype instanceof DoubleDashDataType ) {
                    // Metadata Doubles
                    Double dvalue = (Double) entry.getValue();
                    if ( (dvalue == null) || dvalue.isNaN() || dvalue.isInfinite() )
                        dvalue = DashboardUtils.FP_MISSING_VALUE;
                    ArrayDouble.D1 mvar = new ArrayDouble.D1(1);
                    mvar.set(0, dvalue);
                    ncfile.write(var, mvar);
                }
                else {
                    throw new IllegalArgumentException("unexpected unknown metadata type: " + dtype.toString());
                }
            }

            for (int k = 0; k < stddata.getNumDataCols(); k++) {
                DashDataType<?> dtype = dataTypes.get(k);
                varName = dtype.getVarName();
                var = ncfile.findVariable(varName);
                if ( var == null )
                    throw new RuntimeException("Unexpected failure to find ncfile variable " + varName);

                if ( dtype instanceof StringDashDataType ) {
                    // Data Stings
                    ArrayChar.D2 dvar = new ArrayChar.D2(numSamples, maxDataChar);
                    for (int j = 0; j < numSamples; j++) {
                        String dvalue = (String) stddata.getStdVal(j, k);
                        if ( dvalue == null )
                            dvalue = DashboardUtils.STRING_MISSING_VALUE;
                        dvar.setString(j, dvalue);
                    }
                    ncfile.write(var, dvar);
                }
                else if ( dtype instanceof IntDashDataType ) {
                    // Data Integers
                    ArrayInt.D1 dvar = new ArrayInt.D1(numSamples);
                    for (int j = 0; j < numSamples; j++) {
                        Integer dvalue = (Integer) stddata.getStdVal(j, k);
                        if ( dvalue == null )
                            dvalue = DashboardUtils.INT_MISSING_VALUE;
                        dvar.set(j, dvalue);
                    }
                    ncfile.write(var, dvar);
                }
                else if ( dtype instanceof DoubleDashDataType ) {
                    // Data Doubles
                    ArrayDouble.D1 dvar = new ArrayDouble.D1(numSamples);
                    for (int j = 0; j < numSamples; j++) {
                        Double dvalue = (Double) stddata.getStdVal(j, k);
                        if ( (dvalue == null) || dvalue.isNaN() || dvalue.isInfinite() )
                            dvalue = DashboardUtils.FP_MISSING_VALUE;
                        dvar.set(j, dvalue);
                    }
                    ncfile.write(var, dvar);
                }
                else {
                    throw new IllegalArgumentException("unexpected unknown data type: " + dtype.toString());
                }
            }

        } finally {
            ncfile.close();
        }
    }

    /**
     * Creates and assigns the internal metadata reference from the contents of this netCDF DSG file.
     *
     * @param metadataTypes
     *         metadata file types to read
     *
     * @return variable names of the metadata fields not assigned from this netCDF file (will have its default/missing
     * value)
     *
     * @throws IllegalArgumentException
     *         if there are no metadata types given, or if an invalid type for metadata is encountered
     * @throws IOException
     *         if there are problems opening or reading from the netCDF file
     */
    public ArrayList<String> readMetadata(KnownDataTypes metadataTypes)
            throws IllegalArgumentException, IOException {
        if ( (metadataTypes == null) || metadataTypes.isEmpty() )
            throw new IllegalArgumentException("no metadata file types given");
        ArrayList<String> namesNotFound = new ArrayList<String>();
        NetcdfFile ncfile = NetcdfFile.open(getPath());
        try {
            // Create the metadata with default (missing) values
            metadata = new DsgMetadata(metadataTypes);

            for (DashDataType<?> dtype : metadataTypes.getKnownTypesSet()) {
                String varName = dtype.getVarName();
                Variable var = ncfile.findVariable(varName);
                if ( var == null ) {
                    namesNotFound.add(varName);
                    continue;
                }
                if ( var.getShape(0) != 1 )
                    throw new IOException("more than one value for a metadata type");
                if ( dtype instanceof StringDashDataType ) {
                    ArrayChar.D2 mvar = (ArrayChar.D2) var.read();
                    String strval = mvar.getString(0);
                    if ( !DashboardUtils.STRING_MISSING_VALUE.equals(strval) )
                        metadata.setValue(dtype, strval);
                }
                else if ( dtype instanceof IntDashDataType ) {
                    ArrayInt.D1 mvar = (ArrayInt.D1) var.read();
                    Integer intval = mvar.getInt(0);
                    if ( !DashboardUtils.INT_MISSING_VALUE.equals(intval) )
                        metadata.setValue(dtype, intval);
                }
                else if ( dtype instanceof DoubleDashDataType ) {
                    ArrayDouble.D1 mvar = (ArrayDouble.D1) var.read();
                    Double dblval = mvar.getDouble(0);
                    if ( !DashboardUtils.closeTo(DashboardUtils.FP_MISSING_VALUE, dblval,
                            0.0, DashboardUtils.MAX_ABSOLUTE_ERROR) )
                        metadata.setValue(dtype, dblval);
                }
                else {
                    throw new IllegalArgumentException("invalid metadata file type " + dtype.getVarName());
                }
            }
        } finally {
            ncfile.close();
        }
        return namesNotFound;
    }

    /**
     * Creates and assigns the internal standard data array reference from the contents of this netCDF DSG file.
     *
     * @param dataTypes
     *         data files types to read
     *
     * @return variable names of the data types not assigned from this netCDF file (will have its default/missing value)
     *
     * @throws IllegalArgumentException
     *         if no known data types are given, or if an invalid type for data files is encountered
     * @throws IOException
     *         if the netCDF file is invalid: it must have a 'time' variable and all data variables must have the same
     *         number of values as the 'time' variable, or if there are problems opening or reading from the netCDF
     *         file
     */
    public ArrayList<String> readData(KnownDataTypes dataTypes)
            throws IllegalArgumentException, IOException {
        if ( (dataTypes == null) || dataTypes.isEmpty() )
            throw new IllegalArgumentException("no data file types given");
        int numColumns;
        DashDataType<?>[] dataTypesArray;
        {
            TreeSet<DashDataType<?>> dataTypesSet = dataTypes.getKnownTypesSet();
            numColumns = dataTypesSet.size();
            dataTypesArray = new DashDataType<?>[numColumns];
            int idx = -1;
            for (DashDataType<?> dtype : dataTypesSet) {
                idx++;
                dataTypesArray[idx] = dtype;
            }
        }

        ArrayList<String> namesNotFound = new ArrayList<String>();
        NetcdfFile ncfile = NetcdfFile.open(getPath());
        try {
            // Get the number of samples from the length of the time 1D array
            String varName = DashboardServerUtils.TIME.getVarName();
            Variable var = ncfile.findVariable(varName);
            if ( var == null )
                throw new IOException("unable to find variable 'time' in " + getName());
            int numSamples = var.getShape(0);

            // Create the array of data values
            Object[][] dataArray = new Object[numSamples][numColumns];

            for (int k = 0; k < numColumns; k++) {
                DashDataType<?> dtype = dataTypesArray[k];
                varName = dtype.getVarName();
                var = ncfile.findVariable(varName);
                if ( var == null ) {
                    namesNotFound.add(varName);
                    for (int j = 0; j < numSamples; j++) {
                        dataArray[j][k] = null;
                    }
                    continue;
                }

                if ( var.getShape(0) != numSamples )
                    throw new IOException("number of values for '" + varName +
                            "' (" + Integer.toString(var.getShape(0)) + ") does not match " +
                            "the number of values for 'time' (" + Integer.toString(numSamples) + ")");

                if ( dtype instanceof StringDashDataType ) {
                    ArrayChar.D2 dvar = (ArrayChar.D2) var.read();
                    for (int j = 0; j < numSamples; j++) {
                        String strval = dvar.getString(j);
                        if ( DashboardUtils.STRING_MISSING_VALUE.equals(strval) )
                            dataArray[j][k] = null;
                        else
                            dataArray[j][k] = strval;
                    }
                }
                else if ( dtype instanceof IntDashDataType ) {
                    ArrayInt.D1 dvar = (ArrayInt.D1) var.read();
                    for (int j = 0; j < numSamples; j++) {
                        Integer intval = dvar.get(j);
                        if ( DashboardUtils.INT_MISSING_VALUE.equals(intval) )
                            dataArray[j][k] = null;
                        else
                            dataArray[j][k] = intval;
                    }
                }
                else if ( dtype instanceof DoubleDashDataType ) {
                    ArrayDouble.D1 dvar = (ArrayDouble.D1) var.read();
                    for (int j = 0; j < numSamples; j++) {
                        Double dblval = dvar.get(j);
                        if ( DashboardUtils.closeTo(DashboardUtils.FP_MISSING_VALUE, dblval,
                                0.0, DashboardUtils.MAX_ABSOLUTE_ERROR) )
                            dataArray[j][k] = null;
                        else
                            dataArray[j][k] = dblval;
                    }
                }
                else {
                    throw new IllegalArgumentException("invalid data file type " + dtype.toString());
                }
            }
            stddata = new StdDataArray(dataTypesArray, dataArray);
        } finally {
            ncfile.close();
        }
        return namesNotFound;
    }

    /**
     * @return the internal metadata reference; may be null
     */
    public DsgMetadata getMetadata() {
        return metadata;
    }

    /**
     * @return the internal standard data array reference; may be null
     */
    public StdDataArray getStdDataArray() {
        return stddata;
    }

    /**
     * Reads and returns the array of data values for the specified variable contained in this DSG file.  The variable
     * must be saved in the DSG file as Strings.  For some variables, this DSG file must have been processed by Ferret
     * for the data values to be meaningful.
     *
     * @param varName
     *         name of the variable to read
     *
     * @return array of values for the specified variable
     *
     * @throws IOException
     *         if there is a problem opening or reading from this DSG file
     * @throws IllegalArgumentException
     *         if the variable name is invalid, or if the variable is not a String array variable
     */
    public String[] readStringVarDataValues(String varName)
            throws IOException, IllegalArgumentException {
        String[] dataVals;
        NetcdfFile ncfile = NetcdfFile.open(getPath());
        try {
            Variable var = ncfile.findVariable(varName);
            if ( var == null )
                throw new IllegalArgumentException("Unable to find variable '" + varName + "' in " + getName());
            ArrayChar.D2 cvar = (ArrayChar.D2) var.read();
            int numVals = var.getShape(0);
            dataVals = new String[numVals];
            for (int k = 0; k < numVals; k++) {
                dataVals[k] = cvar.getString(k);
            }
        } finally {
            ncfile.close();
        }
        return dataVals;
    }

    /**
     * Reads and returns the array of data values for the specified variable contained in this DSG file.  The variable
     * must be saved in the DSG file as integers.  For some variables, this DSG file must have been processed by Ferret
     * for the data values to be meaningful.
     *
     * @param varName
     *         name of the variable to read
     *
     * @return array of values for the specified variable
     *
     * @throws IOException
     *         if there is a problem opening or reading from this DSG file
     * @throws IllegalArgumentException
     *         if the variable name is invalid
     */
    public int[] readIntVarDataValues(String varName)
            throws IOException, IllegalArgumentException {
        int[] dataVals;
        NetcdfFile ncfile = NetcdfFile.open(getPath());
        try {
            Variable var = ncfile.findVariable(varName);
            if ( var == null )
                throw new IllegalArgumentException("Unable to find variable '" +
                        varName + "' in " + getName());
            ArrayInt.D1 dvar = (ArrayInt.D1) var.read();
            int numVals = var.getShape(0);
            dataVals = new int[numVals];
            for (int k = 0; k < numVals; k++) {
                dataVals[k] = dvar.get(k);
            }
        } finally {
            ncfile.close();
        }
        return dataVals;
    }

    /**
     * Reads and returns the array of data values for the specified variable contained in this DSG file.  The variable
     * must be saved in the DSG file as doubles. NaN and infinite values are changed to {@link
     * DashboardUtils#FP_MISSING_VALUE}. For some variables, this DSG file must have been processed by Ferret for the
     * data values to be meaningful.
     *
     * @param varName
     *         name of the variable to read
     *
     * @return array of values for the specified variable
     *
     * @throws IOException
     *         if there is a problem opening or reading from this DSG file
     * @throws IllegalArgumentException
     *         if the variable name is invalid
     */
    public double[] readDoubleVarDataValues(String varName)
            throws IOException, IllegalArgumentException {
        double[] dataVals;
        NetcdfFile ncfile = NetcdfFile.open(getPath());
        try {
            Variable var = ncfile.findVariable(varName);
            if ( var == null )
                throw new IllegalArgumentException("Unable to find variable '" +
                        varName + "' in " + getName());
            ArrayDouble.D1 dvar = (ArrayDouble.D1) var.read();
            int numVals = var.getShape(0);
            dataVals = new double[numVals];
            for (int k = 0; k < numVals; k++) {
                double value = dvar.get(k);
                if ( Double.isNaN(value) || Double.isInfinite(value) )
                    value = DashboardUtils.FP_MISSING_VALUE;
                dataVals[k] = value;
            }
        } finally {
            ncfile.close();
        }
        return dataVals;
    }

    // TODO: re-add any functions that might be needed

}
