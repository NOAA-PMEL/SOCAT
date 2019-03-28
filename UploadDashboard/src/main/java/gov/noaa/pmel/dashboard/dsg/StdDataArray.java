/**
 *
 */
package gov.noaa.pmel.dashboard.dsg;

import gov.noaa.pmel.dashboard.datatype.DashDataType;
import gov.noaa.pmel.dashboard.datatype.DoubleDashDataType;
import gov.noaa.pmel.dashboard.datatype.IntDashDataType;
import gov.noaa.pmel.dashboard.datatype.KnownDataTypes;
import gov.noaa.pmel.dashboard.datatype.StringDashDataType;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;
import gov.noaa.pmel.dashboard.shared.DataColumnType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

/**
 * A 2-D array of objects corresponding to the standardized values in a dataset,
 * as well as 1-D arrays of information describing each data column.
 *
 * @author Karl Smith
 */
public class StdDataArray {

    protected int numSamples;
    protected int numDataCols;
    protected DashDataType<?>[] dataTypes;
    protected Object[][] stdObjects;
    // Often-used indices
    protected int longitudeIndex;
    protected int latitudeIndex;
    protected int sampleDepthIndex;
    protected int timestampIndex;
    protected int dateIndex;
    protected int yearIndex;
    protected int monthOfYearIndex;
    protected int dayOfMonthIndex;
    protected int timeOfDayIndex;
    protected int hourOfDayIndex;
    protected int minuteOfHourIndex;
    protected int secondOfMinuteIndex;
    protected int dayOfYearIndex;
    protected int secondOfDayIndex;
    // Indices of all types keyed on the variable name for the type
    protected HashMap<String,Integer> typeNameIndexMap;
    // Indices used for compute sample time by getSampleTime; used for flagging data misordered in time
    protected int[] indicesForTime;

    /**
     * Create and assign the 1-D arrays of data column types from the given user's descriptions
     * of the data column.  Appends the non-user type {@link DashboardServerUtils#SAMPLE_NUMBER}.
     * The 2-D array of standard data objects is not created.
     *
     * @param dataColumnTypes
     *         user's description of the data columns in each sample
     * @param knownTypes
     *         all known user data types
     *
     * @throws IllegalArgumentException
     *         if there are no user data column descriptions,
     *         if there are no known user data types, or
     *         if a data column description is not a known user data type
     */
    protected StdDataArray(List<DataColumnType> dataColumnTypes, KnownDataTypes knownTypes)
            throws IllegalArgumentException {
        if ( (dataColumnTypes == null) || dataColumnTypes.isEmpty() )
            throw new IllegalArgumentException("no data column types given");
        if ( (knownTypes == null) || knownTypes.isEmpty() )
            throw new IllegalArgumentException("no known user data types given");
        numDataCols = dataColumnTypes.size();
        numSamples = 0;

        dataTypes = new DashDataType<?>[numDataCols + 1];
        stdObjects = null;

        for (int k = 0; k < numDataCols; k++) {
            DataColumnType dataColType = dataColumnTypes.get(k);
            dataTypes[k] = knownTypes.getDataType(dataColType);
            if ( dataTypes[k] == null )
                throw new IllegalArgumentException("unknown data column type: " + dataColType.getDisplayName());
        }
        dataTypes[numDataCols] = DashboardServerUtils.SAMPLE_NUMBER;
        numDataCols++;

        // UNKNOWN and duplicates can be present so ignore the return value of the following method
        assignColumnIndicesOfInterest();
    }

    /**
     * Create with the given data file data types for each column and the given standardized data objects
     * for each data column value (second index) in each sample (first index).  The data types given must
     * be known subclasses of DashDataType valid for data files: {@link StringDashDataType},
     * {@link IntDashDataType}, or {@link DoubleDashDataType}.
     *
     * @param dataColumnTypes
     *         types for the data columns in each sample
     * @param stdDataValues
     *         standard values; the value at stdDataValues[j][k] is the appropriate object for the value
     *         of the k-th data column in the j-th sample. Missing values correspond to null objects.
     *
     * @throws IllegalArgumentException
     *         if not data column types are given, if a data column type is not a known subclass type,
     *         if no data values are given, if the number of data columns in the array of data values
     *         does not match the number of data column types, if a data value object is not an
     *         appropriate object for the data column type, if a data column type is the "UNKNOWN"
     *         type or occurs more that one.
     */
    public StdDataArray(DashDataType<?>[] dataColumnTypes, Object[][] stdDataValues)
            throws IllegalArgumentException {
        if ( (dataColumnTypes == null) || (dataColumnTypes.length == 0) )
            throw new IllegalArgumentException("no data column types given");
        numDataCols = dataColumnTypes.length;
        if ( (stdDataValues == null) || (stdDataValues.length == 0) )
            throw new IllegalArgumentException("no standardized data values given");
        numSamples = stdDataValues.length;
        if ( stdDataValues[0].length != numDataCols )
            throw new IllegalArgumentException("Different number of data column values (" +
                    stdDataValues[0].length + ") and types (" + numDataCols + ")");

        dataTypes = new DashDataType<?>[numDataCols];
        stdObjects = new Object[numSamples][numDataCols];

        for (int k = 0; k < numDataCols; k++) {
            DashDataType<?> dtype = dataColumnTypes[k];
            if ( dtype == null )
                throw new IllegalArgumentException(
                        "no data type for column number" + Integer.toString(k + 1));
            dataTypes[k] = dtype;

            // Catch invalid data column types and invalid data objects
            // while assigning the standard data values
            if ( dtype instanceof StringDashDataType ) {
                for (int j = 0; j < numSamples; j++) {
                    try {
                        stdObjects[j][k] = (String) stdDataValues[j][k];
                    } catch ( Exception ex ) {
                        throw new IllegalArgumentException("standard data object for sample number " +
                                Integer.toString(j + 1) + ", column number " + Integer.toString(j + 1) +
                                " is invalid: " + ex.getMessage());
                    }
                }
            }
            else if ( dtype instanceof IntDashDataType ) {
                for (int j = 0; j < numSamples; j++) {
                    try {
                        stdObjects[j][k] = (Integer) stdDataValues[j][k];
                    } catch ( Exception ex ) {
                        throw new IllegalArgumentException("standard data object for sample number " +
                                Integer.toString(j + 1) + ", column number " + Integer.toString(j + 1) +
                                " is invalid: " + ex.getMessage());
                    }
                }
            }
            else if ( dtype instanceof DoubleDashDataType ) {
                for (int j = 0; j < numSamples; j++) {
                    try {
                        stdObjects[j][k] = (Double) stdDataValues[j][k];
                    } catch ( Exception ex ) {
                        throw new IllegalArgumentException("standard data object for sample number " +
                                Integer.toString(j + 1) + ", column number " + Integer.toString(j + 1) +
                                " is invalid: " + ex.getMessage());
                    }
                }
            }
            else {
                throw new IllegalArgumentException("unknown data class type for " +
                        dtype.getDisplayName() + " (" + dtype.getDataClassName() + ")");
            }
        }

        // Get column indices and check for UNKNOWN (return null) and duplicates (return true)
        if ( !Boolean.FALSE.equals(assignColumnIndicesOfInterest()) )
            throw new IllegalArgumentException("unknown or duplicated data column type");
    }

    /**
     * Creates with the standardized data file types and values in the given user standard data array.
     * The methods {@link #getSampleLongitudes()}, {@link #getSampleLatitudes()}, {@link #getSampleDepths()},
     * and {@link #calcSampleTimes()} on the standardized user data must succeed and return arrays with no null
     * (missing) values. No data column can be the type {@link DashboardServerUtils#UNKNOWN}.  Only those data
     * columns matching one of the given known data files types is copied from the standardized user data.
     * The following data columns will be added and assigned if not already present:
     * <ul>
     * <li>{@link DashboardServerUtils#YEAR}</li>
     * <li>{@link DashboardServerUtils#MONTH_OF_YEAR}</li>
     * <li>{@link DashboardServerUtils#DAY_OF_MONTH}</li>
     * <li>{@link DashboardServerUtils#HOUR_OF_DAY}</li>
     * <li>{@link DashboardServerUtils#MINUTE_OF_HOUR}</li>
     * <li>{@link DashboardServerUtils#SECOND_OF_MINUTE}</li>
     * <li>{@link DashboardServerUtils#TIME}</li>
     * </ul>
     * (TIME should always be added and assigned since it is not a user provided type.) If the time to the seconds
     * is not provided, the seconds values are all set to zero and the added SECOND_OF_MINUTE column added will be
     * all zeros.
     *
     * @param userStdData
     *         standardized user data values
     * @param dataFileTypes
     *         known data file column types
     *
     * @throws IllegalArgumentException
     *         if no standard user data values are given,
     *         if any of the user data types is {@link DashboardServerUtils#UNKNOWN},
     *         if any sample longitude or latitude is missing, or
     *         if any sample time cannot be computed.
     */
    public StdDataArray(StdUserDataArray userStdData, KnownDataTypes dataFileTypes)
            throws IllegalArgumentException {
        // StdUserDataArray has to have data columns, but could be missing the data values
        numSamples = userStdData.getNumSamples();
        if ( numSamples <= 0 )
            throw new IllegalArgumentException("no data values given");
        int numUserColumns = userStdData.getNumDataCols();

        // Check that sample longitude, latitude, and time are present and all valid;
        // hang onto the time values for adding to this standardized data
        Double[] timeVals;
        try {
            for (Double value : userStdData.getSampleLongitudes()) {
                if ( value == null )
                    throw new IllegalArgumentException("a longitude value is missing");
            }
            for (Double value : userStdData.getSampleLatitudes()) {
                if ( value == null )
                    throw new IllegalArgumentException("a latitude value is missing");
            }
            timeVals = userStdData.calcSampleTimes();
            for (Double value : timeVals) {
                if ( value == null )
                    throw new IllegalArgumentException("a sample date/time value is missing");
            }
        } catch ( IllegalStateException ex ) {
            throw new IllegalArgumentException(ex);
        }

        // Get the list of data file types present in the standardized user data
        ArrayList<DashDataType<?>> userDataTypes = new ArrayList<DashDataType<?>>(numUserColumns + 7);
        ArrayList<Integer> userColIndices = new ArrayList<Integer>(numUserColumns + 7);
        List<DashDataType<?>> userTypes = userStdData.getDataTypes();
        for (int k = 0; k < numUserColumns; k++) {
            DashDataType<?> dtype = userTypes.get(k);
            if ( DashboardServerUtils.UNKNOWN.typeNameEquals(dtype) )
                throw new IllegalArgumentException("user column number " + Integer.toString(k + 1) +
                        " is type " + DashboardServerUtils.UNKNOWN.getDisplayName());
            if ( userStdData.isUsableIndex(k) && dataFileTypes.containsTypeName(dtype.getVarName()) ) {
                // OTHER and metadata column types are not added
                userColIndices.add(k);
                userDataTypes.add(dtype);
            }
        }
        // Add required data columns if not present
        if ( !userStdData.hasYear() ) {
            userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
            userDataTypes.add(DashboardServerUtils.YEAR);
        }
        if ( !userStdData.hasMonthOfYear() ) {
            userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
            userDataTypes.add(DashboardServerUtils.MONTH_OF_YEAR);
        }
        if ( !userStdData.hasDayOfMonth() ) {
            userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
            userDataTypes.add(DashboardServerUtils.DAY_OF_MONTH);
        }
        if ( !userStdData.hasHourOfDay() ) {
            userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
            userDataTypes.add(DashboardServerUtils.HOUR_OF_DAY);
        }
        if ( !userStdData.hasMinuteOfHour() ) {
            userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
            userDataTypes.add(DashboardServerUtils.MINUTE_OF_HOUR);
        }
        if ( !userStdData.hasSecondOfMinute() ) {
            userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
            userDataTypes.add(DashboardServerUtils.SECOND_OF_MINUTE);
        }
        if ( !userDataTypes.contains(DashboardServerUtils.TIME) ) {
            userColIndices.add(DashboardUtils.INT_MISSING_VALUE);
            userDataTypes.add(DashboardServerUtils.TIME);
        }

        numDataCols = userDataTypes.size();
        dataTypes = new DashDataType<?>[numDataCols];
        stdObjects = new Object[numSamples][numDataCols];
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setLenient(false);
        for (int k = 0; k < numDataCols; k++) {
            dataTypes[k] = userDataTypes.get(k);
            int userIdx = userColIndices.get(k);
            if ( DashboardUtils.INT_MISSING_VALUE.equals(userIdx) ) {
                if ( DashboardServerUtils.YEAR.typeNameEquals(dataTypes[k]) ) {
                    for (int j = 0; j < numSamples; j++) {
                        cal.setTimeInMillis(Double.valueOf(timeVals[j] * 1000.0).longValue());
                        stdObjects[j][k] = Integer.valueOf(cal.get(GregorianCalendar.YEAR));
                    }
                }
                else if ( DashboardServerUtils.MONTH_OF_YEAR.typeNameEquals(dataTypes[k]) ) {
                    for (int j = 0; j < numSamples; j++) {
                        cal.setTimeInMillis(Double.valueOf(timeVals[j] * 1000.0).longValue());
                        stdObjects[j][k] = Integer.valueOf(cal.get(GregorianCalendar.MONTH) -
                                GregorianCalendar.JANUARY + 1);
                    }
                }
                else if ( DashboardServerUtils.DAY_OF_MONTH.typeNameEquals(dataTypes[k]) ) {
                    for (int j = 0; j < numSamples; j++) {
                        cal.setTimeInMillis(Double.valueOf(timeVals[j] * 1000.0).longValue());
                        stdObjects[j][k] = Integer.valueOf(cal.get(GregorianCalendar.DAY_OF_MONTH));
                    }
                }
                else if ( DashboardServerUtils.HOUR_OF_DAY.typeNameEquals(dataTypes[k]) ) {
                    for (int j = 0; j < numSamples; j++) {
                        cal.setTimeInMillis(Double.valueOf(timeVals[j] * 1000.0).longValue());
                        stdObjects[j][k] = Integer.valueOf(cal.get(GregorianCalendar.HOUR_OF_DAY));
                    }
                }
                else if ( DashboardServerUtils.MINUTE_OF_HOUR.typeNameEquals(dataTypes[k]) ) {
                    for (int j = 0; j < numSamples; j++) {
                        cal.setTimeInMillis(Double.valueOf(timeVals[j] * 1000.0).longValue());
                        stdObjects[j][k] = Integer.valueOf(cal.get(GregorianCalendar.MINUTE));
                    }
                }
                else if ( DashboardServerUtils.SECOND_OF_MINUTE.typeNameEquals(dataTypes[k]) ) {
                    for (int j = 0; j < numSamples; j++) {
                        cal.setTimeInMillis(Double.valueOf(timeVals[j] * 1000.0).longValue());
                        Double second = (1000.0 * cal.get(GregorianCalendar.SECOND) +
                                cal.get(GregorianCalendar.MILLISECOND)) / 1000.0;
                        stdObjects[j][k] = second;
                    }
                }
                else if ( DashboardServerUtils.TIME.typeNameEquals(dataTypes[k]) ) {
                    for (int j = 0; j < numSamples; j++) {
                        stdObjects[j][k] = timeVals[j];
                    }
                }
                else {
                    throw new IllegalArgumentException("Unexpected error: unknown data column type with missing index");
                }
            }
            else {
                for (int j = 0; j < numSamples; j++) {
                    // Because isValidIndex was true, this should not throw any exceptions
                    stdObjects[j][k] = userStdData.getStdVal(j, userIdx);
                }
            }
        }

        // Get column indices and check for UNKNOWN (return null) and duplicates (return true)
        if ( !Boolean.FALSE.equals(assignColumnIndicesOfInterest()) )
            throw new IllegalArgumentException("unknown or duplicated data column type");
    }

    /**
     * Assigns the data column indices of interest (longitude, latitude, sample depth, and various time types) from the
     * assigned types of the data columns.
     *
     * @return null if the data column type "UNKNOWN" is present; otherwise,
     *         if there are any duplicate data column types other than "OTHER".
     */
    private Boolean assignColumnIndicesOfInterest() {
        Boolean hasDups = false;
        typeNameIndexMap = new HashMap<String,Integer>(64);
        for (int k = 0; k < numDataCols; k++) {
            // Ignore UNKNOWN type other that resetting the return value
            if ( DashboardServerUtils.UNKNOWN.typeNameEquals(dataTypes[k]) )
                hasDups = null;
            // Completely ignore datatype OTHER
            if ( DashboardServerUtils.OTHER.typeNameEquals(dataTypes[k]) )
                continue;
            // Otherwise map the data type variable name to data column index
            if ( typeNameIndexMap.put(dataTypes[k].getVarName(), k) != null )
                hasDups = true;
        }

        // explicitly save the indices of some commonly referenced data column types
        Integer value = typeNameIndexMap.get(DashboardServerUtils.LONGITUDE.getVarName());
        if ( value != null )
            longitudeIndex = value;
        else
            longitudeIndex = DashboardUtils.INT_MISSING_VALUE;

        value = typeNameIndexMap.get(DashboardServerUtils.LATITUDE.getVarName());
        if ( value != null )
            latitudeIndex = value;
        else
            latitudeIndex = DashboardUtils.INT_MISSING_VALUE;

        value = typeNameIndexMap.get(DashboardServerUtils.SAMPLE_DEPTH.getVarName());
        if ( value != null )
            sampleDepthIndex = value;
        else
            sampleDepthIndex = DashboardUtils.INT_MISSING_VALUE;

        value = typeNameIndexMap.get(DashboardServerUtils.TIMESTAMP.getVarName());
        if ( value != null )
            timestampIndex = value;
        else
            timestampIndex = DashboardUtils.INT_MISSING_VALUE;

        value = typeNameIndexMap.get(DashboardServerUtils.DATE.getVarName());
        if ( value != null )
            dateIndex = value;
        else
            dateIndex = DashboardUtils.INT_MISSING_VALUE;

        value = typeNameIndexMap.get(DashboardServerUtils.YEAR.getVarName());
        if ( value != null )
            yearIndex = value;
        else
            yearIndex = DashboardUtils.INT_MISSING_VALUE;

        value = typeNameIndexMap.get(DashboardServerUtils.MONTH_OF_YEAR.getVarName());
        if ( value != null )
            monthOfYearIndex = value;
        else
            monthOfYearIndex = DashboardUtils.INT_MISSING_VALUE;

        value = typeNameIndexMap.get(DashboardServerUtils.DAY_OF_MONTH.getVarName());
        if ( value != null )
            dayOfMonthIndex = value;
        else
            dayOfMonthIndex = DashboardUtils.INT_MISSING_VALUE;

        value = typeNameIndexMap.get(DashboardServerUtils.TIME_OF_DAY.getVarName());
        if ( value != null )
            timeOfDayIndex = value;
        else
            timeOfDayIndex = DashboardUtils.INT_MISSING_VALUE;

        value = typeNameIndexMap.get(DashboardServerUtils.HOUR_OF_DAY.getVarName());
        if ( value != null )
            hourOfDayIndex = value;
        else
            hourOfDayIndex = DashboardUtils.INT_MISSING_VALUE;

        value = typeNameIndexMap.get(DashboardServerUtils.MINUTE_OF_HOUR.getVarName());
        if ( value != null )
            minuteOfHourIndex = value;
        else
            minuteOfHourIndex = DashboardUtils.INT_MISSING_VALUE;

        value = typeNameIndexMap.get(DashboardServerUtils.SECOND_OF_MINUTE.getVarName());
        if ( value != null )
            secondOfMinuteIndex = value;
        else
            secondOfMinuteIndex = DashboardUtils.INT_MISSING_VALUE;

        value = typeNameIndexMap.get(DashboardServerUtils.DAY_OF_YEAR.getVarName());
        if ( value != null )
            dayOfYearIndex = value;
        else
            dayOfYearIndex = DashboardUtils.INT_MISSING_VALUE;

        value = typeNameIndexMap.get(DashboardServerUtils.SECOND_OF_DAY.getVarName());
        if ( value != null )
            secondOfDayIndex = value;
        else
            secondOfDayIndex = DashboardUtils.INT_MISSING_VALUE;

        // indices for time assigned when calcSampleTimes called
        indicesForTime = null;

        return hasDups;
    }

    /**
     * @return the number of samples (rows) in the current standardized data
     */
    public int getNumSamples() {
        return numSamples;
    }

    /**
     * @return the number of data columns
     */
    public int getNumDataCols() {
        return numDataCols;
    }

    /**
     * Determines is this data column is an appropriate index.  This version of the method just checks that the value is
     * in the appropriate range. Subclasses should override this method if further validation is required.
     *
     * @param idx
     *         index to test
     *
     * @return if the index is valid
     */
    public boolean isUsableIndex(int idx) {
        if ( idx < 0 )
            return false;
        if ( idx >= numDataCols )
            return false;
        return true;
    }

    /**
     * @return the maximum length of all String data values; never less than one even if there are no valid Strings
     */
    public int getMaxStringLength() {
        int maxStrLen = 1;
        for (int k = 0; k < numDataCols; k++) {
            if ( dataTypes[k] instanceof StringDashDataType ) {
                for (int j = 0; j < numSamples; j++) {
                    String strval = (String) stdObjects[j][k];
                    if ( strval != null ) {
                        if ( maxStrLen < strval.length() )
                            maxStrLen = strval.length();
                    }
                }
            }
        }
        return maxStrLen;
    }

    /**
     * @return an array containing the standardized longitudes; missing values are null
     *
     * @throws IllegalStateException
     *         if there are no standardized longitudes
     */
    public Double[] getSampleLongitudes() throws IllegalStateException {
        if ( !isUsableIndex(longitudeIndex) )
            throw new IllegalStateException("no valid longitude data column");
        Double[] sampleLongitudes = new Double[numSamples];
        for (int j = 0; j < numSamples; j++) {
            sampleLongitudes[j] = (Double) stdObjects[j][longitudeIndex];
        }
        return sampleLongitudes;
    }

    /**
     * @return an array containing the standardized latitudes; missing values are null
     *
     * @throws IllegalStateException
     *         if there are no standardized latitudes
     */
    public Double[] getSampleLatitudes() throws IllegalStateException {
        if ( !isUsableIndex(latitudeIndex) )
            throw new IllegalStateException("no valid latitude data column");
        Double[] sampleLatitudes = new Double[numSamples];
        for (int j = 0; j < numSamples; j++) {
            sampleLatitudes[j] = (Double) stdObjects[j][latitudeIndex];
        }
        return sampleLatitudes;
    }

    /**
     * @return an array containing the standardized sample depths; missing values are null
     *
     * @throws IllegalStateException
     *         if there are no standardized sample depths
     */
    public Double[] getSampleDepths() throws IllegalStateException {
        if ( !isUsableIndex(sampleDepthIndex) )
            throw new IllegalStateException("no valid sample depth data column");
        Double[] sampleDepths = new Double[numSamples];
        for (int j = 0; j < numSamples; j++) {
            sampleDepths[j] = (Double) stdObjects[j][sampleDepthIndex];
        }
        return sampleDepths;
    }

    /**
     * Computes the fully-specified time, in units of "seconds since 1970-01-01T00:00:00Z" from the standardized
     * date and time data values that can be found in the data.  One of the following combinations of date/time
     * columns must be given; if more than one time specification is found, the first specification in this list
     * is used.
     * <ul>
     * <li>YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE</li>
     * <li>YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR</li>
     * <li>YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, TIME_OF_DAY</li>
     * <li>YEAR, DAY_OF_YEAR, SECOND_OF_DAY</li>
     * <li>TIMESTAMP</li>
     * <li>DATE, TIME_OF_DAY</li>
     * <li>DATE, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE</li>
     * <li>DATE, HOUR_OF_DAY, MINUTE_OF_HOUR</li>
     * <li>YEAR, DAY_OF_YEAR</li>
     * </ul>
     * In the formats without seconds, or TIME_OF_DAY values without seconds, the seconds are set to zero.
     * The logic in this ordering is the most likely mistake is with the interpretation of a date string
     * (year-month-day, day-month-year, month-day-year), especially if the user gave years with only the
     * last two digits.
     *
     * @return an array containing the sample times; missing values are null
     *
     * @throws IllegalStateException
     *         if specification of the sample date and time is incomplete
     */
    public Double[] calcSampleTimes() throws IllegalStateException {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setLenient(false);
        Double[] sampleTimes = new Double[numSamples];
        indicesForTime = null;

        if ( isUsableIndex(yearIndex) && isUsableIndex(monthOfYearIndex) &&
                isUsableIndex(dayOfMonthIndex) && isUsableIndex(hourOfDayIndex) &&
                isUsableIndex(minuteOfHourIndex) ) {
            // Get time using year, month, day, hour, minute, and (if available) second
            boolean hasSec = isUsableIndex(secondOfMinuteIndex);
            if ( hasSec )
                indicesForTime = new int[] { yearIndex, monthOfYearIndex, dayOfMonthIndex,
                        hourOfDayIndex, minuteOfHourIndex, secondOfMinuteIndex };
            else
                indicesForTime = new int[] { yearIndex, monthOfYearIndex, dayOfMonthIndex,
                        hourOfDayIndex, minuteOfHourIndex };
            for (int j = 0; j < numSamples; j++) {
                try {
                    int year = ((Integer) stdObjects[j][yearIndex]).intValue();
                    int month = ((Integer) stdObjects[j][monthOfYearIndex]).intValue();
                    int day = ((Integer) stdObjects[j][dayOfMonthIndex]).intValue();
                    int hour = ((Integer) stdObjects[j][hourOfDayIndex]).intValue();
                    int min = ((Integer) stdObjects[j][minuteOfHourIndex]).intValue();
                    int sec = 0;
                    int millisec = 0;
                    if ( hasSec ) {
                        try {
                            Double value = (Double) stdObjects[j][secondOfMinuteIndex];
                            sec = value.intValue();
                            value -= sec;
                            value *= 1000.0;
                            millisec = value.intValue();
                        } catch ( Exception ex ) {
                            sec = 0;
                            millisec = 0;
                        }
                    }
                    cal.set(year, GregorianCalendar.JANUARY + month - 1, day, hour, min, sec);
                    cal.set(GregorianCalendar.MILLISECOND, millisec);
                    sampleTimes[j] = Double.valueOf(cal.getTimeInMillis() / 1000.0);
                } catch ( Exception ex ) {
                    sampleTimes[j] = null;
                }
            }
        }
        else if ( isUsableIndex(yearIndex) && isUsableIndex(monthOfYearIndex) &&
                isUsableIndex(dayOfMonthIndex) && isUsableIndex(timeOfDayIndex) ) {
            // Use year, month, day, and time string
            // Standard format of time string is HH:mm:ss.SSS
            indicesForTime = new int[] { yearIndex, monthOfYearIndex, dayOfMonthIndex, timeOfDayIndex };

            for (int j = 0; j < numSamples; j++) {
                try {
                    int year = ((Integer) stdObjects[j][yearIndex]).intValue();
                    int month = ((Integer) stdObjects[j][monthOfYearIndex]).intValue();
                    int day = ((Integer) stdObjects[j][dayOfMonthIndex]).intValue();
                    String[] hms = ((String) stdObjects[j][timeOfDayIndex]).split(":");
                    if ( hms.length != 3 )
                        throw new Exception();
                    int hour = Integer.parseInt(hms[0]);
                    int min = Integer.parseInt(hms[1]);
                    Double value = Double.parseDouble(hms[2]);
                    int sec = value.intValue();
                    value -= sec;
                    value *= 1000.0;
                    int millisec = value.intValue();
                    cal.set(year, GregorianCalendar.JANUARY + month - 1, day, hour, min, sec);
                    cal.set(GregorianCalendar.MILLISECOND, millisec);
                    sampleTimes[j] = Double.valueOf(cal.getTimeInMillis() / 1000.0);
                } catch ( Exception ex ) {
                    sampleTimes[j] = null;
                }
            }
        }
        else if ( isUsableIndex(yearIndex) && isUsableIndex(dayOfYearIndex) && isUsableIndex(secondOfDayIndex) ) {
            // Use year, day of year (an integer), and second of day
            indicesForTime = new int[] { yearIndex, dayOfYearIndex, secondOfDayIndex };
            for (int j = 0; j < numSamples; j++) {
                try {
                    int year = ((Integer) stdObjects[j][yearIndex]).intValue();
                    Double value = (Double) stdObjects[j][dayOfYearIndex];
                    int dayOfYear = value.intValue();
                    if ( Math.abs(value - dayOfYear) > DashboardUtils.MAX_ABSOLUTE_ERROR )
                        throw new Exception();
                    value = ((Double) stdObjects[j][secondOfDayIndex]).doubleValue();
                    value /= 3600.0;
                    int hour = value.intValue();
                    value -= hour;
                    value *= 60.0;
                    int minute = value.intValue();
                    value -= minute;
                    value *= 60.0;
                    int sec = value.intValue();
                    value -= sec;
                    value *= 1000.0;
                    int millisec = value.intValue();
                    cal.clear(GregorianCalendar.MONTH);
                    cal.clear(GregorianCalendar.DAY_OF_MONTH);
                    cal.set(GregorianCalendar.YEAR, year);
                    cal.set(GregorianCalendar.DAY_OF_YEAR, dayOfYear);
                    cal.set(GregorianCalendar.HOUR_OF_DAY, hour);
                    cal.set(GregorianCalendar.MINUTE, minute);
                    cal.set(GregorianCalendar.SECOND, sec);
                    cal.set(GregorianCalendar.MILLISECOND, millisec);
                    sampleTimes[j] = Double.valueOf(cal.getTimeInMillis() / 1000.0);
                } catch ( Exception ex ) {
                    sampleTimes[j] = null;
                }
            }
        }
        else if ( isUsableIndex(timestampIndex) ) {
            // Use full timestamp
            // Standard format of the timestamp is yyyy-MM-dd HH:mm:sss.SSS
            indicesForTime = new int[] { timestampIndex };
            for (int j = 0; j < numSamples; j++) {
                try {
                    String[] dateTime = ((String) stdObjects[j][timestampIndex]).split(" ");
                    if ( dateTime.length != 2 )
                        throw new Exception();
                    String[] ymd = dateTime[0].split("-");
                    if ( ymd.length != 3 )
                        throw new Exception();
                    int year = Integer.parseInt(ymd[0]);
                    int month = Integer.parseInt(ymd[1]);
                    int day = Integer.parseInt(ymd[2]);
                    String[] hms = dateTime[1].split(":");
                    if ( hms.length != 3 )
                        throw new Exception();
                    int hour = Integer.parseInt(hms[0]);
                    int min = Integer.parseInt(hms[1]);
                    Double value = Double.parseDouble(hms[2]);
                    int sec = value.intValue();
                    value -= sec;
                    value *= 1000.0;
                    int millisec = value.intValue();
                    cal.set(year, GregorianCalendar.JANUARY + month - 1, day, hour, min, sec);
                    cal.set(GregorianCalendar.MILLISECOND, millisec);
                    sampleTimes[j] = Double.valueOf(cal.getTimeInMillis() / 1000.0);
                } catch ( Exception ex ) {
                    sampleTimes[j] = null;
                }
            }
        }
        else if ( isUsableIndex(dateIndex) && isUsableIndex(timeOfDayIndex) ) {
            // Use date string and time string
            // Standard format of the date is yyyy-MM-dd
            // Standard format of time string is HH:mm:ss.SSS
            indicesForTime = new int[] { dateIndex, timeOfDayIndex };
            for (int j = 0; j < numSamples; j++) {
                try {
                    String[] ymd = ((String) stdObjects[j][dateIndex]).split("-");
                    if ( ymd.length != 3 )
                        throw new Exception();
                    int year = Integer.parseInt(ymd[0]);
                    int month = Integer.parseInt(ymd[1]);
                    int day = Integer.parseInt(ymd[2]);
                    String[] hms = ((String) stdObjects[j][timeOfDayIndex]).split(":");
                    if ( hms.length != 3 )
                        throw new Exception();
                    int hour = Integer.parseInt(hms[0]);
                    int min = Integer.parseInt(hms[1]);
                    Double value = Double.parseDouble(hms[2]);
                    int sec = value.intValue();
                    value -= sec;
                    value *= 1000.0;
                    int millisec = value.intValue();
                    cal.set(year, GregorianCalendar.JANUARY + month - 1, day, hour, min, sec);
                    cal.set(GregorianCalendar.MILLISECOND, millisec);
                    sampleTimes[j] = Double.valueOf(cal.getTimeInMillis() / 1000.0);
                } catch ( Exception ex ) {
                    sampleTimes[j] = null;
                }
            }
        }
        else if ( isUsableIndex(dateIndex) && isUsableIndex(hourOfDayIndex) && isUsableIndex(minuteOfHourIndex) ) {
            // Use date string, hour, minute, and (if available) second
            // Standard format of the date is yyyy-MM-dd
            boolean hasSec = isUsableIndex(secondOfMinuteIndex);
            if ( hasSec )
                indicesForTime = new int[] { dateIndex, hourOfDayIndex, minuteOfHourIndex, secondOfMinuteIndex };
            else
                indicesForTime = new int[] { dateIndex, hourOfDayIndex, minuteOfHourIndex };
            for (int j = 0; j < numSamples; j++) {
                try {
                    String[] ymd = ((String) stdObjects[j][dateIndex]).split("-");
                    if ( ymd.length != 3 )
                        throw new Exception();
                    int year = Integer.parseInt(ymd[0]);
                    int month = Integer.parseInt(ymd[1]);
                    int day = Integer.parseInt(ymd[2]);
                    int hour = ((Integer) stdObjects[j][hourOfDayIndex]).intValue();
                    int min = ((Integer) stdObjects[j][minuteOfHourIndex]).intValue();
                    int sec = 0;
                    int millisec = 0;
                    if ( hasSec ) {
                        try {
                            Double value = (Double) stdObjects[j][secondOfMinuteIndex];
                            sec = value.intValue();
                            value -= sec;
                            value *= 1000.0;
                            millisec = value.intValue();
                        } catch ( Exception ex ) {
                            sec = 0;
                            millisec = 0;
                        }
                    }
                    cal.set(year, GregorianCalendar.JANUARY + month - 1, day, hour, min, sec);
                    cal.set(GregorianCalendar.MILLISECOND, millisec);
                    sampleTimes[j] = Double.valueOf(cal.getTimeInMillis() / 1000.0);
                } catch ( Exception ex ) {
                    sampleTimes[j] = null;
                }
            }
        }
        else if ( isUsableIndex(yearIndex) && isUsableIndex(dayOfYearIndex) ) {
            // Use year and day of year (floating-point)
            indicesForTime = new int[] { yearIndex, dayOfYearIndex };
            for (int j = 0; j < numSamples; j++) {
                try {
                    int year = ((Integer) stdObjects[j][yearIndex]).intValue();
                    Double value = (Double) stdObjects[j][dayOfYearIndex];
                    int dayOfYear = value.intValue();
                    value -= dayOfYear;
                    value *= 24.0;
                    int hour = value.intValue();
                    value -= hour;
                    value *= 60.0;
                    int minute = value.intValue();
                    value -= minute;
                    value *= 60.0;
                    int sec = value.intValue();
                    value -= sec;
                    value *= 1000.0;
                    int millisec = value.intValue();
                    cal.clear(GregorianCalendar.MONTH);
                    cal.clear(GregorianCalendar.DAY_OF_MONTH);
                    cal.set(GregorianCalendar.YEAR, year);
                    cal.set(GregorianCalendar.DAY_OF_YEAR, dayOfYear);
                    cal.set(GregorianCalendar.HOUR_OF_DAY, hour);
                    cal.set(GregorianCalendar.MINUTE, minute);
                    cal.set(GregorianCalendar.SECOND, sec);
                    cal.set(GregorianCalendar.MILLISECOND, millisec);
                    sampleTimes[j] = Double.valueOf(cal.getTimeInMillis() / 1000.0);
                } catch ( Exception ex ) {
                    sampleTimes[j] = null;
                }
            }
        }
        else
            throw new IllegalStateException("Incomplete specification of sample time");

        return sampleTimes;
    }

    /**
     * @return if there is a valid {@link DashboardServerUtils#LONGITUDE} data column
     */
    public boolean hasLongitude() {
        return isUsableIndex(longitudeIndex);
    }

    /**
     * @return if there is a valid {@link DashboardServerUtils#LATITUDE} data column
     */
    public boolean hasLatitude() {
        return isUsableIndex(latitudeIndex);
    }

    /**
     * @return if there is a valid {@link DashboardServerUtils#SAMPLE_DEPTH} data column
     */
    public boolean hasSampleDepth() {
        return isUsableIndex(sampleDepthIndex);
    }

    /**
     * @return if there is a valid {@link DashboardServerUtils#YEAR} data column
     */
    public boolean hasYear() {
        return isUsableIndex(yearIndex);
    }

    /**
     * @return if there is a valid {@link DashboardServerUtils#MONTH_OF_YEAR} data column
     */
    public boolean hasMonthOfYear() {
        return isUsableIndex(monthOfYearIndex);
    }

    /**
     * @return if there is a valid {@link DashboardServerUtils#DAY_OF_MONTH} data column
     */
    public boolean hasDayOfMonth() {
        return isUsableIndex(dayOfMonthIndex);
    }

    /**
     * @return if there is a valid {@link DashboardServerUtils#HOUR_OF_DAY} data column
     */
    public boolean hasHourOfDay() {
        return isUsableIndex(hourOfDayIndex);
    }

    /**
     * @return if there is a valid {@link DashboardServerUtils#MINUTE_OF_HOUR} data column
     */
    public boolean hasMinuteOfHour() {
        return isUsableIndex(minuteOfHourIndex);
    }

    /**
     * @return if there is a valid {@link DashboardServerUtils#SECOND_OF_MINUTE} data column
     */
    public boolean hasSecondOfMinute() {
        return isUsableIndex(secondOfMinuteIndex);
    }

    /**
     * @return an unmodifiable list of types for the data columns.
     */
    public List<DashDataType<?>> getDataTypes() {
        return Collections.unmodifiableList(Arrays.asList(dataTypes));
    }

    /**
     * Gets the index of the data array column whose type matches the given data column type.
     * The data column type matching is performed using the data column variable name.
     * The data class name of the matched data columns is verified to be the same.
     *
     * @param dtype
     *         find the index of the data array column with this data type
     *
     * @return the index of the data array column with the matching type, or null if no match is found
     */
    public Integer getIndexOfType(DashDataType<?> dtype) {
        Integer colidx = typeNameIndexMap.get(dtype.getVarName());
        if ( colidx == null )
            return null;
        if ( !dtype.getDataClassName().equals(dataTypes[colidx].getDataClassName()) )
            return null;
        return colidx;
    }

    /**
     * Get the standard value object for the specified value (column index) of the specified sample (row index).
     *
     * @param sampleIdx
     *         index of the sample (row)
     * @param columnIdx
     *         index of the data column
     *
     * @return standard value object; null is returned for "missing value" or values that could not be interpreted
     *
     * @throws IndexOutOfBoundsException
     *         if the sample index or the data column index is invalid
     */
    public Object getStdVal(int sampleIdx, int columnIdx) throws IndexOutOfBoundsException {
        if ( (sampleIdx < 0) || (sampleIdx >= numSamples) )
            throw new IndexOutOfBoundsException("sample index is invalid: " + sampleIdx);
        if ( (columnIdx < 0) || (columnIdx >= numDataCols) )
            throw new IndexOutOfBoundsException("data column index is invalid: " + columnIdx);
        return stdObjects[sampleIdx][columnIdx];
    }

    @Override
    public int hashCode() {
        final int prime = 37;
        int result = Arrays.deepHashCode(stdObjects);
        result = prime * result + Arrays.hashCode(dataTypes);
        result = prime * result + numDataCols;
        result = prime * result + numSamples;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;

        if ( !(obj instanceof StdDataArray) )
            return false;
        StdDataArray other = (StdDataArray) obj;

        if ( numDataCols != other.numDataCols )
            return false;
        if ( numSamples != other.numSamples )
            return false;

        if ( !Arrays.equals(dataTypes, other.dataTypes) )
            return false;

        if ( !Arrays.deepEquals(stdObjects, other.stdObjects) )
            return false;

        return true;
    }

    @Override
    public String toString() {
        String repr = "StdDataArray[numSamples=" + numSamples + ", numDataCols=" + numDataCols +
                ",\n dataTypes=[";
        for (int k = 0; k < numDataCols; k++) {
            if ( k > 0 )
                repr += ",";
            repr += "\n    " + dataTypes[k].toString();
        }
        repr += "\n  ],\n  stdObjects=[";
        for (int j = 0; j < numSamples; j++) {
            if ( j > 0 )
                repr += ",";
            repr += "\n    " + Arrays.toString(stdObjects[j]);
        }
        repr += "\n  ]\n]";
        return repr;
    }

}
