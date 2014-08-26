package gov.noaa.pmel.socat.dashboard.nc;

import gov.noaa.pmel.socat.dashboard.handlers.DsgNcFileHandler;
import gov.noaa.pmel.socat.dashboard.ome.OmeMetadata;
import gov.noaa.pmel.socat.dashboard.shared.DashboardCruiseWithData;
import gov.noaa.pmel.socat.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.socat.dashboard.shared.DataLocation;
import gov.noaa.pmel.socat.dashboard.shared.SocatCruiseData;
import gov.noaa.pmel.socat.dashboard.shared.SocatMetadata;
import gov.noaa.pmel.socat.dashboard.shared.SocatQCEvent;
import gov.noaa.pmel.socat.dashboard.shared.SocatWoceEvent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

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
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;


public class CruiseDsgNcFile extends File {

	private static final long serialVersionUID = 419168141850194424L;

	private static final String VERSION = "CruiseDsgNcFile 1.2";
	private static final Calendar BASE_CALENDAR = Calendar.proleptic_gregorian;
	private static final CalendarDate BASE_DATE = CalendarDate.of(BASE_CALENDAR, 1970, 1, 1, 0, 0, 0);

	private SocatMetadata metadata;
	private ArrayList<SocatCruiseData> dataList;

	/**
	 * See {@link java.io.File#File(java.lang.String)}
	 * The internal metadata and data list references are set null.
	 */
	public CruiseDsgNcFile(String filename) {
		super(filename);
		metadata = null;
		dataList = null;
	}

	/**
	 * Creates this NetCDF DSG file with the contents of the given 
	 * SocatMetadata object and list of SocatCruiseData objects.
	 * The internal metadata and data list references are set to 
	 * the given arguments. 
	 * 
	 * @param mdata
	 * 		metadata for the cruise
	 * @param data
	 * 		list of data for the cruise
	 * @throws IllegalArgumentException
	 * 		if either argument is null, or 
	 * 		if the list of SocatCruiseData objects is empty
	 * @throws IOException
	 * 		if creating the NetCDF file throws one
	 * @throws InvalidRangeException
	 * 		if creating the NetCDF file throws one
	 * @throws IllegalAccessException
	 * 		if creating the NetCDF file throws one
	 */
	public void create(SocatMetadata mdata, ArrayList<SocatCruiseData> data) 
			throws IllegalArgumentException, IOException, InvalidRangeException, IllegalAccessException {
		metadata = mdata;
		dataList = data;

		if ( metadata == null )
			throw new IllegalArgumentException("SocatMetadata given to create cannot be null");
		if ( (dataList == null) || (dataList.size() < 1) )
			throw new IllegalArgumentException("SocatCruiseData list given to create cannot be null");

		NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(Version.netcdf3, getPath());
		try {
			// According to the CF standard if a file only has one trajectory, 
			// then the trajectory dimension is not necessary.
			// However, who knows what would break downstream from this process without it...

			Dimension traj = ncfile.addDimension(null, "trajectory", 1);

			// There will be a number of trajectory variables of type character from the metadata.
			// Which is the longest?
			int maxchar = metadata.getMaxStringLength();
			Dimension stringlen = ncfile.addDimension(null, "string_length", maxchar);
			List<Dimension> trajStringDims = new ArrayList<Dimension>();
			trajStringDims.add(traj);
			trajStringDims.add(stringlen);

			List<Dimension> trajDims = new ArrayList<Dimension>();
			trajDims.add(traj);

			Dimension obslen = ncfile.addDimension(null, "obs", dataList.size());
			List<Dimension> dataDims = new ArrayList<Dimension>();
			dataDims.add(obslen);

			Dimension charlen = ncfile.addDimension(null, "char_length", 1);
			List<Dimension> charDataDims = new ArrayList<Dimension>();
			charDataDims.add(obslen);
			charDataDims.add(charlen);

			Field[] metaFields = SocatMetadata.class.getDeclaredFields();
			for ( Field f : metaFields )
				f.setAccessible(true);
			Field[] dataFields = SocatCruiseData.class.getDeclaredFields();
			for ( Field f : dataFields )
				f.setAccessible(true);

			ncfile.addGroupAttribute(null, new Attribute("featureType", "Trajectory"));
			ncfile.addGroupAttribute(null, new Attribute("Conventions", "CF-1.6"));
			ncfile.addGroupAttribute(null, new Attribute("history", VERSION));

			Variable var = ncfile.addVariable(null, "num_obs", DataType.DOUBLE, trajDims);
			ncfile.addVariableAttribute(var, new Attribute("sample_dimension", "obs"));
			ncfile.addVariableAttribute(var, new Attribute("long_name", "Number of Observations"));

			String name;
			String varName;

			// Make netCDF variables of all the metadata.
			for ( Field f : metaFields ) {
				if ( ! Modifier.isStatic(f.getModifiers()) ) {
					name = f.getName();
					varName = Constants.SHORT_NAMES.get(name);
					if ( varName == null )
						throw new RuntimeException("Unexpected missing short name for " + name);
					var = null;
					Number missVal = null;
					Class<?> type = f.getType();
					if ( type.equals(String.class) ) {
						var = ncfile.addVariable(null, varName, DataType.CHAR, trajStringDims);
						missVal = null;
					} 
					else if ( type.equals(Double.class) || type.equals(Double.TYPE) ) {
						var = ncfile.addVariable(null, varName, DataType.DOUBLE, trajDims);
						missVal = SocatCruiseData.FP_MISSING_VALUE;
					} 
					else if ( type.equals(Date.class) ) {
						var = ncfile.addVariable(null, varName, DataType.DOUBLE, trajDims);
						missVal = Double.valueOf(SocatMetadata.DATE_MISSING_VALUE.getTime() / 1000.0);
					}
					else
						throw new RuntimeException("Unexpected metadata field type " + 
								type.getSimpleName() + " for variable " + name);
					if ( var == null )
						throw new RuntimeException("Unexpected failure to add the variable " + 
								varName + " for metadata field " + name);

					if ( missVal != null ) {
						ncfile.addVariableAttribute(var, new Attribute("missing_value", missVal));
						ncfile.addVariableAttribute(var, new Attribute("_FillValue", missVal));
					}
					String units = Constants.UNITS.get(name);
					if ( units != null ) {
						ncfile.addVariableAttribute(var, new Attribute("units", units));
					}
					String longName = Constants.LONG_NAMES.get(name);
					if ( longName == null )
						throw new RuntimeException("Unexpected missing long name for " + name);
					ncfile.addVariableAttribute(var, new Attribute("long_name", longName));
					if ( name.equals("expocode")) {
						ncfile.addVariableAttribute(var, new Attribute("cf_role", "trajectory_id"));
					}
					String stdName = Constants.STANDARD_NAMES.get(name);
					if ( stdName != null ) {
						ncfile.addVariableAttribute(var, new Attribute("standard_name", stdName));
					}
					String category = Constants.IOOS_CATEGORIES.get(name);
					if ( category != null ) {
						ncfile.addVariableAttribute(var, new Attribute("ioos_category", category));
					}
				}
			}

			// Make netCDF variables of all the data.
			for ( Field f : dataFields ) {
				if ( ! Modifier.isStatic(f.getModifiers()) ) {
					name = f.getName();
					varName = Constants.SHORT_NAMES.get(name);
					if ( varName == null )
						throw new RuntimeException("Unexpected missing short name for " + name);
					var = null;
					Number missVal = null;
					Class<?> type = f.getType();
					if ( type.equals(Double.class) || type.equals(Double.TYPE) ) {
						var = ncfile.addVariable(null, varName, DataType.DOUBLE, dataDims);
						missVal = SocatCruiseData.FP_MISSING_VALUE;
					} 
					else if ( type.equals(Integer.class) || type.equals(Integer.TYPE) ) {
						var = ncfile.addVariable(null, varName, DataType.INT, dataDims);
						missVal = SocatCruiseData.INT_MISSING_VALUE;
					} 
					else if ( type.equals(Character.class) || type.equals(Character.TYPE) ) {
						var = ncfile.addVariable(null, varName, DataType.CHAR, charDataDims);
						missVal = null;
					} 
					else
						throw new RuntimeException("Unexpected data field type " + 
								type.getSimpleName() + " for variable " + name);
					if ( var == null )
						throw new RuntimeException("Unexpected failure to add the variable " + 
								varName + " for data field " + name);

					if ( missVal != null ) {
						ncfile.addVariableAttribute(var, new Attribute("missing_value", missVal));
						ncfile.addVariableAttribute(var, new Attribute("_FillValue", missVal));
					}
					String units = Constants.UNITS.get(name);
					if ( units != null ) {
						ncfile.addVariableAttribute(var, new Attribute("units", units));
					}
					String longName = Constants.LONG_NAMES.get(name);
					if ( longName == null )
						throw new RuntimeException("Unexpected missing long name for " + name);
					ncfile.addVariableAttribute(var, new Attribute("long_name", longName));
					if ( name.endsWith("Depth") ) {
						ncfile.addVariableAttribute(var, new Attribute("positive", "down"));
					}
					String stdName = Constants.STANDARD_NAMES.get(name);
					if ( stdName != null ) {
						ncfile.addVariableAttribute(var, new Attribute("standard_name", stdName));
					}
					String category = Constants.IOOS_CATEGORIES.get(name);
					if ( category != null ) {
						ncfile.addVariableAttribute(var, new Attribute("ioos_category", category));
					}
				}
			}

			name = Constants.time_VARNAME;
			varName = Constants.SHORT_NAMES.get(name);
			var = ncfile.addVariable(null, varName, DataType.DOUBLE, dataDims);
			ncfile.addVariableAttribute(var, new Attribute("missing_value", SocatCruiseData.FP_MISSING_VALUE));
			ncfile.addVariableAttribute(var, new Attribute("_FillValue", SocatCruiseData.FP_MISSING_VALUE));
			ncfile.addVariableAttribute(var, new Attribute("units", Constants.UNITS.get(name)));
			ncfile.addVariableAttribute(var, new Attribute("long_name", Constants.LONG_NAMES.get(name)));
			ncfile.addVariableAttribute(var, new Attribute("standard_name", Constants.STANDARD_NAMES.get(name)));
			ncfile.addVariableAttribute(var, new Attribute("ioos_category", Constants.IOOS_CATEGORIES.get(name)));

			ncfile.create();

			// The header has been created.  Now let's fill it up.

			var = ncfile.findVariable("num_obs");
			if ( var == null )
				throw new RuntimeException("Unexpected failure to find ncfile variable num_obs");
			ArrayDouble.D1 obscount = new ArrayDouble.D1(1);
			obscount.set(0, (double) dataList.size());
			ncfile.write(var, obscount);

			for ( Field f : metaFields ) {
				if ( ! Modifier.isStatic(f.getModifiers()) ) {
					varName = Constants.SHORT_NAMES.get(f.getName());
					var = ncfile.findVariable(varName);
					if ( var == null )
						throw new RuntimeException("Unexpected failure to find ncfile variable " + varName);
					Class<?> type = f.getType();
					if ( type.equals(String.class) ) {
						ArrayChar.D2 mvar = new ArrayChar.D2(1, maxchar);
						mvar.setString(0, (String) f.get(metadata));
						ncfile.write(var, mvar);
					}
					else if ( type.equals(Double.class) || type.equals(Double.TYPE) ) {
						ArrayDouble.D1 mvar = new ArrayDouble.D1(1);
						Double dvalue = (Double) f.get(metadata);
						if ( dvalue.isNaN() )
							dvalue = SocatCruiseData.FP_MISSING_VALUE;
						mvar.set(0, dvalue);
						ncfile.write(var, mvar);
					}
					else if ( type.equals(Date.class) ) {
						ArrayDouble.D1 mvar = new ArrayDouble.D1(1);
						Date dateVal = (Date) f.get(metadata);
						mvar.set(0, Double.valueOf(dateVal.getTime() / 1000.0));
						ncfile.write(var, mvar);
					}
					else
						throw new RuntimeException("Unexpected metadata field type " + 
								type.getSimpleName() + " for variable " + varName);
				}
			}

			for ( Field f : dataFields ) {
				if ( ! Modifier.isStatic(f.getModifiers()) ) {
					varName = Constants.SHORT_NAMES.get(f.getName());
					var = ncfile.findVariable(varName);
					if ( var == null )
						throw new RuntimeException("Unexpected failure to find ncfile variable " + varName);
					Class<?> type = f.getType();
					if ( type.equals(Double.class) || type.equals(Double.TYPE) ) {
						ArrayDouble.D1 dvar = new ArrayDouble.D1(dataList.size());
						for (int index = 0; index < dataList.size(); index++) {
							SocatCruiseData datarow = (SocatCruiseData) dataList.get(index);
							Double dvalue = (Double) f.get(datarow);
							if ( dvalue.isNaN() )
								dvalue = SocatCruiseData.FP_MISSING_VALUE;
							dvar.set(index, dvalue);
						}
						ncfile.write(var, dvar);
					}
					else if ( type.equals(Integer.class) || type.equals(Integer.TYPE) ) {
						ArrayInt.D1 dvar = new ArrayInt.D1(dataList.size());
						for (int index = 0; index < dataList.size(); index++) {
							SocatCruiseData datarow = (SocatCruiseData) dataList.get(index);
							Integer dvalue = (Integer) f.get(datarow);
							dvar.set(index, dvalue);
						}
						ncfile.write(var, dvar);
					}
					else if ( type.equals(Character.class) || type.equals(Character.TYPE) ) {
						ArrayChar.D2 dvar = new ArrayChar.D2(dataList.size(), 1);
						for (int index = 0; index < dataList.size(); index++) {
							SocatCruiseData datarow = (SocatCruiseData) dataList.get(index);
							Character dvalue = (Character) f.get(datarow);
							dvar.set(index, 0, dvalue);
						}
						ncfile.write(var, dvar);
					}
					else
						throw new RuntimeException("Unexpected data field type " + 
								type.getSimpleName() + " for variable " + varName);
				}
			}

			varName = Constants.SHORT_NAMES.get(Constants.time_VARNAME);
			var = ncfile.findVariable(varName);
			if ( var == null )
				throw new RuntimeException("Unexpected failure to find " +
						"ncfile variable '" + varName + "'");
			ArrayDouble.D1 values = new ArrayDouble.D1(dataList.size());
			for (int index = 0; index < dataList.size(); index++) {
				SocatCruiseData datarow = dataList.get(index);
				Integer year = datarow.getYear();
				Integer month = datarow.getMonth();
				Integer day = datarow.getDay();
				Integer hour = datarow.getHour();
				Integer minute = datarow.getMinute();
				Double second = datarow.getSecond();
				Integer sec;
				if ( second.isNaN() || (second == SocatCruiseData.FP_MISSING_VALUE) ) {
					sec = 0;
				}
				else {
					sec = (int) Math.round(second);
				}
				if ( (year != SocatCruiseData.INT_MISSING_VALUE) && 
						(month != SocatCruiseData.INT_MISSING_VALUE) && 
						(day != SocatCruiseData.INT_MISSING_VALUE) && 
						(hour != SocatCruiseData.INT_MISSING_VALUE) && 
						(minute != SocatCruiseData.INT_MISSING_VALUE) ) {
					try {
						CalendarDate date = CalendarDate.of(BASE_CALENDAR, year, month, day, hour, minute, sec);
						double value = date.getDifferenceInMsecs(BASE_DATE) / 1000.0;
						values.set(index, value);
					} catch (Exception ex) {
						values.set(index, SocatCruiseData.FP_MISSING_VALUE);
					}
				}
				else {
					values.set(index, SocatCruiseData.FP_MISSING_VALUE);
				}
				ncfile.write(var, values);
			}
		} finally {
			ncfile.close();
		}
	}

	/**
	 * Creates and assigns the internal metadata and data list
	 * references from the contents of this netCDF DSG file.
	 * 
	 * @param onlyMetadata
	 * 		only read the metadata?
	 * @return
	 * 		names of metadata and data fields not assigned from this 
	 * 		netCDF file (will have its default/missing value)
	 * @throws IOException
	 * 		if there are problems opening or reading from the netCDF file
	 * @throws IllegalArgumentException
	 * 		if the netCDF file is invalid.  Currently it must have a
	 * 		'time' variable and all data variables must have the same
	 * 		number of values as this variable.
	 */
	public ArrayList<String> read(boolean onlyMetadata) 
								throws IOException, IllegalArgumentException {
		ArrayList<String> namesNotFound = new ArrayList<String>();
		NetcdfFile ncfile = NetcdfFile.open(getPath());
		try {
			// Get the number of data points from the length of the time 1D array
			String name = Constants.time_VARNAME;
			String varName = Constants.SHORT_NAMES.get(name);
			Variable var = ncfile.findVariable(varName);
			if ( var == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			int numData = var.getShape(0);

			// Create the metadata with default (missing) values
			metadata = new SocatMetadata();

			// Get all the metadata fields to be assigned
			Field[] metaFields = SocatMetadata.class.getDeclaredFields();
			for ( Field f : metaFields )
				f.setAccessible(true);

			// Assign the metadata values from the netCDF file.
			for ( Field f : metaFields ) {
				if ( ! Modifier.isStatic(f.getModifiers()) ) {
					name = f.getName();
					varName = Constants.SHORT_NAMES.get(name);
					if ( varName == null )
						throw new RuntimeException(
								"Unexpected missing short name for " + name);
					var = ncfile.findVariable(varName);
					if ( var == null ) {
						namesNotFound.add(name);
						continue;
					}
					Class<?> type = f.getType();
					try {
						if ( type.equals(String.class) ) {
							ArrayChar.D2 mvar = (ArrayChar.D2) var.read();
							f.set(metadata, mvar.getString(0));
						} 
						else if ( type.equals(Double.class) || type.equals(Double.TYPE) ) {
							ArrayDouble.D1 mvar = (ArrayDouble.D1) var.read();
							f.set(metadata, mvar.getDouble(0));
						} 
						else if ( type.equals(Date.class) ) {
							ArrayDouble.D1 mvar = (ArrayDouble.D1) var.read();
							f.set(metadata, new Date(Math.round(mvar.getDouble(0) * 1000.0)));
						}
						else
							throw new RuntimeException("Unexpected metadata field type " + 
									type.getSimpleName() + " for variable " + name);
					} catch (IllegalArgumentException | IllegalAccessException ex) {
						throw new RuntimeException("Unexpected failure to assign " +
								"metadata field " + name + ": " + ex.getMessage());
					}
				}
			}

			if ( onlyMetadata )
				return namesNotFound;

			// Create the complete list of data values, 
			// all with default (missing) values
			dataList = new ArrayList<SocatCruiseData>(numData);
			for (int k = 0; k < numData; k++)
				dataList.add(new SocatCruiseData());

			// Get all the data fields to be assigned
			Field[] dataFields = SocatCruiseData.class.getDeclaredFields();
			for ( Field f : dataFields )
				f.setAccessible(true);
			
			// Assign the data values from the netCDF file.
			for ( Field f : dataFields ) {
				if ( ! Modifier.isStatic(f.getModifiers()) ) {
					name = f.getName();
					varName = Constants.SHORT_NAMES.get(name);
					if ( varName == null )
						throw new RuntimeException("Unexpected missing short name for " + name);
					var = ncfile.findVariable(varName);
					if ( var == null ) {
						namesNotFound.add(name);
						continue;
					}
					if ( var.getShape(0) != numData )
						throw new IllegalArgumentException("Number of values for '" + varName + 
								"' (" + Integer.toString(var.getShape(0)) + ") does not match " +
								"the number of values for 'time' (" + Integer.toString(numData) + ")");
					Class<?> type = f.getType();
					try {
						if ( type.equals(Double.class) || type.equals(Double.TYPE) ) {
							ArrayDouble.D1 dvar = (ArrayDouble.D1) var.read();
							for (int k = 0; k < numData; k++)
								f.set(dataList.get(k), Double.valueOf(dvar.get(k)));
						}
						else if ( type.equals(Integer.class) || type.equals(Integer.TYPE) ) {
							ArrayInt.D1 dvar = (ArrayInt.D1) var.read();
							for (int k = 0; k < numData; k++)
								f.set(dataList.get(k), Integer.valueOf(dvar.get(k)));
						} 
						else if ( type.equals(Character.class) || type.equals(Character.TYPE) ) {
							ArrayChar.D2 dvar = (ArrayChar.D2) var.read();
							for (int k = 0; k < numData; k++)
								f.set(dataList.get(k), Character.valueOf(dvar.get(k,0)));
						} 
						else
							throw new RuntimeException("Unexpected data field type " + 
									type.getSimpleName() + " for variable " + name);
					} catch (IllegalArgumentException | IllegalAccessException ex) {
						throw new RuntimeException("Unexpected failure to assign " +
								"data field " + name + ": " + ex.getMessage());
					}
				}
			}
		} finally {
			ncfile.close();
		}
		return namesNotFound;
	}

	/**
	 * @return
	 * 		the internal metadata reference; may be null
	 */
	public SocatMetadata getMetadata() {
		return metadata;
	}

	/**
	 * @return
	 * 		the internal data list reference; may be null
	 */
	public ArrayList<SocatCruiseData> getDataList() {
		return dataList;
	}

	/**
	 * Reads and returns the array of data values for the specified variable
	 * contained in this DSG file.  The variable must be saved in the DSG file
	 * as characters.  Empty strings are changed to a single blank character.
	 * For some variables, this DSG file must have been processed by Ferret, 
	 * such as when saved using 
	 * {@link DsgNcFileHandler#saveCruise(OmeMetadata, DashboardCruiseWithData, String)}
	 * for the data values to be meaningful.
	 * 
	 * @param varName
	 * 		name of the variable to read
	 * @return
	 * 		array of values for the specified variable
	 * @throws IOException
	 * 		if there is a problem opening or reading from this DSG file
	 * @throws IllegalArgumentException
	 * 		if the variable name is invalid, or
	 * 		if the variable is not a single-character array variable
	 */
	public char[] readCharVarDataValues(String varName) 
								throws IOException, IllegalArgumentException {
		char[] dataVals;
		NetcdfFile ncfile = NetcdfFile.open(getPath());
		try {
			Variable var = ncfile.findVariable(varName);
			if ( var == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayChar.D2 cvar = (ArrayChar.D2) var.read();
			if ( var.getShape(1) != 1 ) 
				throw new IllegalArgumentException("Variable '" + varName + 
						"' is not a single-character array variable in " + getName());
			int numVals = var.getShape(0);
			dataVals = new char[numVals];
			for (int k = 0; k < numVals; k++) {
				char value = cvar.get(k,0);
				if ( value == (char) 0 )
					value = ' ';
				dataVals[k] = value;
			}
		} finally {
			ncfile.close();
		}
		return dataVals;
	}

	/**
	 * Reads and returns the array of data values for the specified variable
	 * contained in this DSG file.  The variable must be saved in the DSG file
	 * as integers.  For some variables, this DSG file must have been processed 
	 * by Ferret, such as when saved using 
	 * {@link DsgNcFileHandler#saveCruise(OmeMetadata, DashboardCruiseWithData, String)}
	 * for the data values to be meaningful.
	 * 
	 * @param varName
	 * 		name of the variable to read
	 * @return
	 * 		array of values for the specified variable
	 * @throws IOException
	 * 		if there is a problem opening or reading from this DSG file
	 * @throws IllegalArgumentException
	 * 		if the variable name is invalid
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
	 * Reads and returns the array of data values for the specified variable
	 * contained in this DSG file.  The variable must be saved in the DSG file
	 * as doubles.  NaN and infinite values are changed to 
	 * {@link SocatCruiseData#FP_MISSING_VALUE}.  For some variables, this 
	 * DSG file must have been processed by Ferret, such as when saved using 
	 * {@link DsgNcFileHandler#saveCruise(OmeMetadata, DashboardCruiseWithData, String)}
	 * for the data values to be meaningful.
	 * 
	 * @param varName
	 * 		name of the variable to read
	 * @return
	 * 		array of values for the specified variable
	 * @throws IOException
	 * 		if there is a problem opening or reading from this DSG file
	 * @throws IllegalArgumentException
	 * 		if the variable name is invalid
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
					value = SocatCruiseData.FP_MISSING_VALUE;
				dataVals[k] = value;
			}
		} finally {
			ncfile.close();
		}
		return dataVals;
	}

	/**
	 * Updates this DSG file with the given expocode.
	 * 
	 * @param newExpocode
	 * 		expocode to assign in the DSG file
	 * @throws IllegalArgumentException
	 * 		if the DSG file is not valid
	 * @throws IOException
	 * 		if opening or writing to the DSG file throws one
	 * @throws InvalidRangeException 
	 * 		if writing the updated expocode to the DSG file throws one 
	 */
	public void updateExpocode(String newExpocode) 
			throws IllegalArgumentException, IOException, InvalidRangeException {
		NetcdfFileWriter ncfile = NetcdfFileWriter.openExisting(getPath());
		try {
			String varName = Constants.SHORT_NAMES.get(Constants.expocode_VARNAME);
			Variable var = ncfile.findVariable(varName);
			if ( var == null ) 
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayChar.D2 flagArray = new ArrayChar.D2(1, var.getShape(1));
			flagArray.setString(0, newExpocode);
			ncfile.write(var, flagArray);
		} finally {
			ncfile.close();
		}
	}

	/**
	 * @return
	 * 		the QC flag contained in this DSG file
	 * @throws IllegalArgumentException
	 * 		if this DSG file is not valid
	 * @throws IOException
	 * 		if opening or reading from the DSG file throws one
	 */
	public char getQCFlag() throws IllegalArgumentException, IOException {
		char flag;
		NetcdfFileWriter ncfile = NetcdfFileWriter.openExisting(getPath());
		try {
			String varName = Constants.SHORT_NAMES.get(Constants.qcFlag_VARNAME);
			Variable var = ncfile.findVariable(varName);
			if ( var == null ) 
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayChar.D2 flagArray = (ArrayChar.D2) var.read();
			flag = flagArray.get(0, 0);
		} finally {
			ncfile.close();
		}
		return flag;
	}

	/**
	 * Updates this DSG file with the given QC flag.
	 * 
	 * @param qcEvent
	 * 		get the expocode and the QC flag from here
	 * @throws IllegalArgumentException
	 * 		if this DSG file is not valid
	 * @throws IOException
	 * 		if opening or writing to the DSG file throws one
	 * @throws InvalidRangeException 
	 * 		if writing the updated QC flag to the DSG file throws one 
	 */
	public void updateQCFlag(SocatQCEvent qcEvent) 
			throws IllegalArgumentException, IOException, InvalidRangeException {
		NetcdfFileWriter ncfile = NetcdfFileWriter.openExisting(getPath());
		try {
			String varName = Constants.SHORT_NAMES.get(Constants.qcFlag_VARNAME);
			Variable var = ncfile.findVariable(varName);
			if ( var == null ) 
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayChar.D2 flagArray = new ArrayChar.D2(1, var.getShape(1));
			flagArray.setString(0, qcEvent.getFlag().toString());
			ncfile.write(var, flagArray);
		} finally {
			ncfile.close();
		}
	}

	/**
	 * Assigns the given complete WOCE flags in this DSG file.  In particular, 
	 * the row numbers in the WOCE flag locations are used to identify the row 
	 * for the WOCE flag; however, the latitude, longitude, and timestamp in 
	 * these WOCE flag locations are checked that they match those in this DSG 
	 * file.  The data values, if given, is also checked that they roughly match 
	 * those in this DSG file.  If there is a mismatch in any of these values, 
	 * a message is added to the list returned.
	 * 
	 * @param woceEvent
	 * 		WOCE flags to set
	 * @return
	 * 		list of data mismatch messages; never null but may be empty
	 * @throws IllegalArgumentException
	 * 		if the DSG file or the WOCE flags are not valid, including
	 * 		if the WOCE flag values are not close to the DSG values for
	 * 		the indicated row in the WOCE flag
	 * @throws IOException
	 * 		if opening, reading from, or writing to the DSG file throws one
	 */
	public ArrayList<String> assignWoceFlags(SocatWoceEvent woceEvent) 
								throws IllegalArgumentException, IOException {
		ArrayList<String> issues = new ArrayList<String>();
		NetcdfFileWriter ncfile = NetcdfFileWriter.openExisting(getPath());
		try {

			String varName = Constants.SHORT_NAMES.get(Constants.longitude_VARNAME);
			Variable var = ncfile.findVariable(varName);
			if ( var == null ) 
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayDouble.D1 longitudes = (ArrayDouble.D1) var.read();

			varName = Constants.SHORT_NAMES.get(Constants.latitude_VARNAME);
			var = ncfile.findVariable(varName);
			if ( var == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayDouble.D1 latitudes = (ArrayDouble.D1) var.read();

			varName = Constants.SHORT_NAMES.get(Constants.time_VARNAME);
			var = ncfile.findVariable(varName);
			if ( var == null ) 
				throw new IllegalArgumentException("Unable to find variable '" +
						varName + "' in " + getName());
			ArrayDouble.D1 times = (ArrayDouble.D1) var.read();

			varName = Constants.SHORT_NAMES.get(Constants.regionID_VARNAME);
			var = ncfile.findVariable(varName);
			if ( var == null )
				throw new IllegalArgumentException("Unable to find variable '" +
						varName + "' in " + getName());
			ArrayChar.D2 regionIDs = (ArrayChar.D2) var.read(); 

			String dataname = woceEvent.getDataVarName();
			ArrayDouble.D1 datavalues;
			if ( Constants.geoposition_VARNAME.equals(dataname) ) {
				// WOCE based on longitude/latitude/time
				datavalues = null;
			}
			else {
				var = ncfile.findVariable(dataname);
				if ( var == null )
					throw new IllegalArgumentException("Unable to find variable '" + 
							dataname + "' in " + getName());
				datavalues = (ArrayDouble.D1) var.read(); 
			}

			// WOCE flags - currently only WOCE_CO2_water
			varName = Constants.SHORT_NAMES.get(Constants.woceCO2Water_VARNAME);
			Variable wocevar = ncfile.findVariable(varName);
			if ( wocevar == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayChar.D2 wocevalues = (ArrayChar.D2) wocevar.read();

			char newFlag = woceEvent.getFlag();
			for ( DataLocation dataloc : woceEvent.getLocations() ) {
				int idx = dataloc.getRowNumber() - 1;

				// Check the values are close (data value roughly close)
				if ( ! dataMatches(dataloc, longitudes, latitudes, times, 
									datavalues, idx, 0.001, 0.1) ) {
					DataLocation dsgLoc = new DataLocation();
					dsgLoc.setRowNumber(idx + 1);
					dsgLoc.setRegionID(regionIDs.get(idx, 0));
					dsgLoc.setLongitude(longitudes.get(idx));
					dsgLoc.setLatitude(latitudes.get(idx));
					dsgLoc.setDataDate(new Date(Math.round(times.get(idx) * 1000.0)));
					if ( datavalues != null )
						dsgLoc.setDataValue(datavalues.get(idx));
					issues.add("Values for the DSG row (first) different from WOCE " +
							"flag location (second): \n    " + dsgLoc.toString() + 
							"\n    " + dataloc.toString());
				}

				wocevalues.set(idx, 0, newFlag);
			}

			// Save the updated WOCE flags to the DSG file
			try {
				ncfile.write(wocevar, wocevalues);
			} catch (InvalidRangeException ex) {
				throw new IOException(ex);
			}
		} finally {
			ncfile.close();
		}
		return issues;
	}

	/**
	 * Updates this DSG file with the given WOCE flags.  Optionally will 
	 * also update the region ID and row number in the WOCE flags from 
	 * the data in this DSG file. 
	 * 
	 * @param woceEvent
	 * 		WOCE flags to set
	 * @param updateWoceEvent
	 * 		if true, update the WOCE flags from data in this DSG file
	 * @return
	 * 		list of the WOCEEvent data locations not found 
	 * 		in this DSG file; never null but may be empty
	 * @throws IllegalArgumentException
	 * 		if the DSG file or the WOCE flags are not valid
	 * @throws IOException
	 * 		if opening, reading from, or writing to the DSG file throws one
	 * @throws InvalidRangeException 
	 * 		if writing the update WOCE flags to the DSG file throws one 
	 */
	public ArrayList<DataLocation> updateWoceFlags(SocatWoceEvent woceEvent, 
			boolean updateWoceEvent) 
			throws IllegalArgumentException, IOException, InvalidRangeException {
		ArrayList<DataLocation> unidentified = new ArrayList<DataLocation>();
		NetcdfFileWriter ncfile = NetcdfFileWriter.openExisting(getPath());
		try {

			String varName = Constants.SHORT_NAMES.get(Constants.longitude_VARNAME);
			Variable var = ncfile.findVariable(varName);
			if ( var == null ) 
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayDouble.D1 longitudes = (ArrayDouble.D1) var.read();

			varName = Constants.SHORT_NAMES.get(Constants.latitude_VARNAME);
			var = ncfile.findVariable(varName);
			if ( var == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayDouble.D1 latitudes = (ArrayDouble.D1) var.read();

			varName = Constants.SHORT_NAMES.get(Constants.time_VARNAME);
			var = ncfile.findVariable(varName);
			if ( var == null ) 
				throw new IllegalArgumentException("Unable to find variable '" +
						varName + "' in " + getName());
			ArrayDouble.D1 times = (ArrayDouble.D1) var.read();

			ArrayChar.D2 regionIDs;
			if ( updateWoceEvent ) {
				varName = Constants.SHORT_NAMES.get(Constants.regionID_VARNAME);
				var = ncfile.findVariable(varName);
				if ( var == null )
					throw new IllegalArgumentException("Unable to find variable '" +
							varName + "' in " + getName());
				regionIDs = (ArrayChar.D2) var.read(); 
			}
			else {
				regionIDs = null;
			}

			String dataname = woceEvent.getDataVarName();
			ArrayDouble.D1 datavalues;
			if ( Constants.geoposition_VARNAME.equals(dataname) ) {
				// WOCE based on longitude/latitude/time
				datavalues = null;
			}
			else {
				var = ncfile.findVariable(dataname);
				if ( var == null )
					throw new IllegalArgumentException("Unable to find variable '" + 
							dataname + "' in " + getName());
				datavalues = (ArrayDouble.D1) var.read(); 
			}

			// WOCE flags - currently only WOCE_CO2_water
			varName = Constants.SHORT_NAMES.get(Constants.woceCO2Water_VARNAME);
			Variable wocevar = ncfile.findVariable(varName);
			if ( wocevar == null )
				throw new IllegalArgumentException("Unable to find variable '" + 
						varName + "' in " + getName());
			ArrayChar.D2 wocevalues = (ArrayChar.D2) wocevar.read();

			char newFlag = woceEvent.getFlag();

			// Identify the data points using a round-robin search 
			// just in case there is more than one matching point
			int startIdx = 0;
			int arraySize = (int) times.getSize();
			HashSet<Integer> assignedRowIndices = new HashSet<Integer>(); 
			for ( DataLocation dataloc : woceEvent.getLocations() ) {
				boolean valueFound = false;
				int idx;
				for (idx = startIdx; idx < arraySize; idx++) {
					if ( dataMatches(dataloc, longitudes, latitudes, times, 
							datavalues, idx, 1.0E-5, 1.0E-5) ) {
						if ( assignedRowIndices.add(idx) ) {
							valueFound = true;
							break;
						}
					}
				}
				if ( idx >= arraySize ) {
					for (idx = 0; idx < startIdx; idx++) {
						if ( dataMatches(dataloc, longitudes, latitudes, times, 
								datavalues, idx, 1.0E-5, 1.0E-5) ) {
							if ( assignedRowIndices.add(idx) ) {
								valueFound = true;
								break;
							}
						}
					}
				}
				if ( valueFound ) {
					wocevalues.set(idx, 0, newFlag);
					if ( updateWoceEvent ) {
						dataloc.setRowNumber(idx + 1);
						dataloc.setRegionID(regionIDs.get(idx, 0));
					}
					// Start the next search from the next data point
					startIdx = idx + 1;
				}
				else {
					unidentified.add(dataloc);
				}
			}

			// Save the updated WOCE flags to the DSG file
			ncfile.write(wocevar, wocevalues);
		} finally {
			ncfile.close();
		}
		return unidentified;
	}

	/**
	 * Compares the data location information given in a DataLocation with the
	 * longitude, latitude, time, and (if applicable) data value at a given 
	 * index into arrays of these values.
	 * 
	 * @param dataloc
	 * 		data location to compare
	 * @param longitudes
	 * 		array of longitudes to use
	 * @param latitudes
	 * 		array of latitudes to use
	 * @param times
	 * 		array of times (seconds since 1970-01-01 00:00:00) to use
	 * @param datavalues
	 * 		if not null, array of data values to use
	 * @param idx
	 * 		index into the arrays of the values to compare
	 * @param dataRelTol
	 * 		relative tolerance for (only) the data value;
	 * 		see {@link DashboardUtils#closeTo(Double, Double, double, double)}
	 * @param dataAbsTol
	 * 		absolute tolerance for (only) the data value
	 * 		see {@link DashboardUtils#closeTo(Double, Double, double, double)}
	 * @return
	 * 		true if the data locations match
	 */
	private boolean dataMatches(DataLocation dataloc, ArrayDouble.D1 longitudes,
			ArrayDouble.D1 latitudes, ArrayDouble.D1 times, ArrayDouble.D1 datavalues, 
			int idx, double dataRelTol, double dataAbsTol) {

		Double arrLongitude = longitudes.get(idx);
		Double arrLatitude = latitudes.get(idx);
		Double arrTime = times.get(idx);
		Double arrValue;
		if ( datavalues != null )
			arrValue = datavalues.get(idx);
		else
			arrValue = Double.NaN;

		Double datLongitude = dataloc.getLongitude();
		Double datLatitude = dataloc.getLatitude();
		Double datTime = dataloc.getDataDate().getTime() / 1000.0;
		Double datValue = dataloc.getDataValue(); 

		// Check if longitude is within 0.001 degrees of each other
		if ( ! DashboardUtils.longitudeCloseTo(datLongitude, arrLongitude, 0.0, 0.001) ) {
			return false;
		}

		// Check if latitude is within 0.0001 degrees of each other
		if ( ! DashboardUtils.closeTo(datLatitude, arrLatitude, 0.0, 0.0001) ) {
			return false;
		}

		// Check if times are within a second of each other
		if ( ! DashboardUtils.closeTo(datTime, arrTime, 0.0, 1.0) ) {
			return false;
		}

		// If given, check if data values are close to each other
		if ( datavalues != null ) {
			if ( ! DashboardUtils.closeTo(datValue, arrValue, dataRelTol, dataAbsTol) ) {
				return false;
			}
		}

		return true;
	}

}
