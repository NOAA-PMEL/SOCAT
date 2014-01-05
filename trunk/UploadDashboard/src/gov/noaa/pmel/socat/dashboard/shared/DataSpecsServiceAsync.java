/**
 * 
 */
package gov.noaa.pmel.socat.dashboard.shared;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Client side specification of data column specification services
 * @author Karl Smith
 */
public interface DataSpecsServiceAsync {

	/**
	 * Reads the saved cruise file and returns the current data
	 * column specifications as well as some initial cruise data
	 * to assist in identifying cruise data columns.
	 *  
	 * @param username
	 * 		authenticate using this username
	 * @param passhash
	 * 		authenticate using this password hash
	 * @param expocode
	 * 		generate report for this cruise
	 * @param callback
	 * 		callback to make with the current cruise data 
	 * 		column specifications and initial (partial) cruise data.
	 * 		The fail method is invoked if authentication fails, 
	 * 		if expocode is invalid, if the cruise does not exist, 
	 * 		or if there are problems obtaining the data for the cruise
	 */
	void getCruiseDataColumnSpecs(String username, String passhash,
			String expocode, AsyncCallback<DashboardCruiseWithData> callback);

	/**
	 * Reads the saved cruise file and returns the specified
	 * rows of cruise data.  The outer list contains the rows 
	 * of cruise data; the inner list contains the columns of
	 * cruise data for that row.  (Thus, each row is all data 
	 * measured for a given sample, and each column is data 
	 * of a given type measured for all samples.)
	 * 
	 * @param username
	 * 		authenticate using this username
	 * @param passhash
	 * 		authenticate using this password hash
	 * @param expocode
	 * 		get data for this cruise
	 * @param firstRow
	 * 		index of the first row of data to return
	 * @param numRows
	 * 		number of rows of data to return
	 * @param callback
	 * 		callback to make with rows of data for a cruise.
	 * 		The fail method is invoked if authentication fails, 
	 * 		if expocode is invalid, if the cruise does not exist, 
	 * 		or if there are problems obtaining the specified data 
	 * 		for the cruise
	 */
	void getCruiseData(String username, String passhash, String expocode,
			int firstRow, int numRows,
			AsyncCallback<ArrayList<ArrayList<String>>> callback);

	/**
	 * Updates the data column specifications for a cruise to those 
	 * provided.  This triggers the SanityChecker to run using the 
	 * new data column specifications.
	 * 
	 * @param username
	 * 		authenticate using this username
	 * @param passhash
	 * 		authenticate using this password hash
	 * @param newSpecs
	 * 		cruise data column types to assign.  The expocode in this 
	 * 		object specifies the cruise to update.  Any cruise data in 
	 * 		this object is ignored.
	 * @param callback
	 * 		callback to make with the the updated cruise with  
	 * 		(abbreviated) data after processing through the SanityChecker
 after processing through the 
	 * 		SanityChecker.  The fail method is invoked if authentication 
	 * 		fails, if expocode is invalid, if the cruise does not exist, 
	 * 		or if there are problems obtaining or evaluating the data for 
	 * 		the cruise
	 */
	void updateCruiseDataColumnSpecs(String username, String passhash,
			DashboardCruise newSpecs, 
			AsyncCallback<DashboardCruiseWithData> callback);

}
