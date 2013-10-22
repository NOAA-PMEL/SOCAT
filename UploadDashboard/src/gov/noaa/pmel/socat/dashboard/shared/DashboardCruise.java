/**
 * 
 */
package gov.noaa.pmel.socat.dashboard.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Represents an uploaded cruise and its current status.
 * 
 * @author Karl Smith
 */
public class DashboardCruise implements Serializable {

	private static final long serialVersionUID = -2373783036245133660L;

	boolean selected;
	String owner;
	String expocode;
	String dataCheckStatus;
	String metadataCheckStatus;
	String qcStatus;
	String archiveStatus;
	String uploadFilename;
	int numDataRows;
	ArrayList<CruiseDataColumnType> dataColTypes;
	ArrayList<Integer> userColIndices;
	ArrayList<String> userColNames;
	ArrayList<String> dataColUnits;
	ArrayList<String> dataColDescriptions;

	public DashboardCruise() {
		selected = false;
		owner = "";
		expocode = "";
		dataCheckStatus = "";
		metadataCheckStatus = "";
		qcStatus = "";
		archiveStatus = "";
		uploadFilename = "";
		numDataRows = 0;
		dataColTypes = new ArrayList<CruiseDataColumnType>();
		userColIndices = new ArrayList<Integer>();
		userColNames = new ArrayList<String>();
		dataColUnits = new ArrayList<String>();
		dataColDescriptions = new ArrayList<String>();
	}

	/**
	 * @return
	 * 		if the cruise is selected
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * @param selected
	 * 		set if the cruise is selected
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * @return 
	 * 		the owner for this cruise; never null
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @param owner 
	 * 		the cruise owner (after trimming) to set;
	 * 		if null, sets to an empty string
	 */
	public void setOwner(String owner) {
		if ( owner == null )
			this.owner = "";
		else
			this.owner = owner.trim();
	}

	/**
	 * @return 
	 * 		the cruise expocode; never null
	 */
	public String getExpocode() {
		return expocode;
	}

	/**
	 * @param cruiseExpocode 
	 * 		the cruise expocode to set (after trimming 
	 * 		and converting to upper-case) to set;
	 * 		if null, sets to an empty string
	 */
	public void setExpocode(String expocode) {
		if ( expocode == null )
			this.expocode = "";
		else
			this.expocode = expocode.trim().toUpperCase();
	}

	/**
	 * @return 
	 * 		the data check status; never null
	 */
	public String getDataCheckStatus() {
		return dataCheckStatus;
	}

	/**
	 * @param dataCheckStatus 
	 * 		the data check status to set;
	 * 		if null, sets to an empty string
	 */
	public void setDataCheckStatus(String dataCheckStatus) {
		if ( dataCheckStatus == null )
			this.dataCheckStatus = "";
		else
			this.dataCheckStatus = dataCheckStatus;
	}

	/**
	 * @return 
	 * 		the metadata check status; never null
	 */
	public String getMetadataCheckStatus() {
		return metadataCheckStatus;
	}

	/**
	 * @param metadataCheckDate
	 * 		the metadata check status to set;
	 * 		if null, sets to an empty string
	 */
	public void setMetadataCheckStatus(String metadataCheckStatus) {
		if ( metadataCheckStatus == null )
			this.metadataCheckStatus = "";
		else
			this.metadataCheckStatus = metadataCheckStatus;
	}

	/**
	 * @return 
	 * 		the QC submission status; never null
	 */
	public String getQcStatus() {
		return qcStatus;
	}

	/**
	 * @param qcStatus 
	 * 		the  QC submission status (after trimming) to set;
	 * 		if null, sets to an empty string
	 */
	public void setQcStatus(String qcStatus) {
		if ( qcStatus == null )
			this.qcStatus = "";
		else
			this.qcStatus = qcStatus.trim();
	}

	/**
	 * @return 
	 * 		the archive submission status; never null
	 */
	public String getArchiveStatus() {
		return archiveStatus;
	}

	/**
	 * @param submitStatus 
	 * 		the archive submission status (after trimming) to set;
	 * 		if null, sets to an empty string
	 */
	public void setArchiveStatus(String archiveStatus) {
		if ( archiveStatus == null )
			this.archiveStatus = "";
		else
			this.archiveStatus = archiveStatus.trim();
	}

	/**
	 * @return 
	 * 		the uploaded data filename; never null
	 */
	public String getUploadFilename() {
		return uploadFilename;
	}

	/**
	 * @param uploadFilename 
	 * 		the uploaded data filename (after trimming) to set;
	 * 		if null, sets to an empty string
	 */
	public void setUploadFilename(String uploadFilename) {
		if ( uploadFilename == null )
			this.uploadFilename = "";
		else
			this.uploadFilename = uploadFilename.trim();
	}

	/**
	 * @return 
	 * 		the total number of data measurements (data rows) 
	 * 		for the cruise
	 */
	public int getNumDataRows() {
		return numDataRows;
	}

	/**
	 * @param numDataRows 
	 * 		the total number of data measurements (data rows) 
	 * 		to set for the cruise 
	 */
	public void setNumDataRows(int numDataRows) {
		this.numDataRows = numDataRows;
	}

	/**
	 * @return 
	 * 		the list of data column types for this cruise; may be empty 
	 * 		but never null.  The actual list in this object is returned.
	 */
	public ArrayList<CruiseDataColumnType> getDataColTypes() {
		return dataColTypes;
	}

	/**
	 * @param dataColTypes 
	 * 		the list of data column types for this cruise.  The list in 
	 * 		this object is cleared and all the contents of the given list, 
	 * 		if not null, are added. 
	 */
	public void setDataColTypes(ArrayList<CruiseDataColumnType> dataColTypes) {
		this.dataColTypes.clear();
		if ( dataColTypes != null )
			this.dataColTypes.addAll(dataColTypes);
	}

	/**
	 * @return 
	 * 		the list of data column indices as they appeared in the 
	 * 		original user-provided data file for this cruise; may be 
	 * 		empty but never null.  The actual list in this object is 
	 * 		returned.
	 */
	public ArrayList<Integer> getUserColIndices() {
		return userColIndices;
	}

	/**
	 * @param userColIndices 
	 * 		the list of data column indices as they appeared in the 
	 * 		original user-provided data file for this cruise.  The list 
	 * 		in this object is cleared and all the contents of the given 
	 * 		list, if not null, are added. 
	 */
	public void setUserColIndices(ArrayList<Integer> userColIndices) {
		this.userColIndices.clear();
		if ( userColIndices != null )
			this.userColIndices.addAll(userColIndices);
	}

	/**
	 * @return the userColNames
	 * 		the list of data column header names as they appeared in 
	 * 		the original user-provided data file for this cruise; may 
	 * 		be empty but never null.  The actual list in this object 
	 * 		is returned. 
	 */
	public ArrayList<String> getUserColNames() {
		return userColNames;
	}

	/**
	 * @param userColNames 
	 * 		the list of data column header names as they appeared in 
	 * 		the original user-provided data file for this cruise.  The 
	 * 		list in this object is cleared and all the contents of the  
	 * 		given list, if not null, are added. 
	 */
	public void setUserColNames(ArrayList<String> userColNames) {
		this.userColNames.clear();
		if ( userColNames != null )
			this.userColNames.addAll(userColNames);
	}

	/**
	 * @return 
	 * 		the list of data column units for this cruise; may be empty 
	 * 		but never null.  The actual list in this object is returned. 
	 */
	public ArrayList<String> getDataColUnits() {
		return dataColUnits;
	}

	/**
	 * @param dataColUnits 
	 * 		the list of data column units for this cruise.  The list 
	 * 		in this object is cleared and all the contents of the given 
	 * 		list, if not null, are added. 
	 */
	public void setDataColUnits(ArrayList<String> dataColUnits) {
		this.dataColUnits.clear();
		if ( dataColUnits != null )
			this.dataColUnits.addAll(dataColUnits);
	}

	/**
	 * @return 
	 * 		the list of data column descriptions for this cruise; may 
	 * 		be empty but never null.  The actual list in this object is 
	 * 		returned. 
	 */
	public ArrayList<String> getDataColDescriptions() {
		return dataColDescriptions;
	}

	/**
	 * @param dataColDescriptions 
	 * 		the list of data column descriptions for this cruise.  The 
	 * 		list in this object is cleared and all the contents of the 
	 * 		given list, if not null, are added. 
	 */
	public void setDataColDescriptions(ArrayList<String> dataColDescriptions) {
		this.dataColDescriptions.clear();
		if ( dataColDescriptions != null )
			this.dataColDescriptions.addAll(dataColDescriptions);
	}

	@Override
	public int hashCode() {
		final int prime = 37;
		int result = Boolean.valueOf(selected).hashCode();
		result = result * prime + owner.hashCode();
		result = result * prime + expocode.hashCode();
		result = result * prime + dataCheckStatus.hashCode();
		result = result * prime + metadataCheckStatus.hashCode();
		result = result * prime + qcStatus.hashCode();
		result = result * prime + archiveStatus.hashCode();
		result = result * prime + uploadFilename.hashCode();
		result = result * prime + numDataRows;
		result = result * prime + dataColTypes.hashCode();
		result = result * prime + userColIndices.hashCode();
		result = result * prime + userColNames.hashCode();
		result = result * prime + dataColUnits.hashCode();
		result = result * prime + dataColDescriptions.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;

		if ( ! (obj instanceof DashboardCruise) )
			return false;
		DashboardCruise other = (DashboardCruise) obj;

		if ( selected != other.selected )
			return false;
		if ( ! owner.equals(other.owner) )
			return false;
		if ( ! expocode.equals(other.expocode) )
			return false;
		if ( ! dataCheckStatus.equals(other.dataCheckStatus) )
			return false;
		if ( ! metadataCheckStatus.equals(other.metadataCheckStatus) )
			return false;
		if ( ! qcStatus.equals(other.qcStatus) )
			return false;
		if ( ! archiveStatus.equals(other.archiveStatus) )
			return false;
		if ( ! uploadFilename.equals(other.uploadFilename) )
			return false;
		if ( numDataRows != other.numDataRows )
			return false;
		if ( ! dataColTypes.equals(other.dataColTypes) )
			return false;
		if ( ! userColIndices.equals(other.userColIndices) )
			return false;
		if ( ! userColNames.equals(other.userColNames) )
			return false;
		if ( ! dataColUnits.equals(other.dataColUnits) )
			return false;
		if ( ! dataColDescriptions.equals(other.dataColDescriptions) )
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DashboardCruise" +
				"[ selected=" + Boolean.toString(selected) + 
				",\n    owner=" + owner + 
				",\n    expocode=" + expocode + 
				",\n    dataCheckStatus=" + dataCheckStatus +
				",\n    metadataCheckStatus=" + metadataCheckStatus +
				",\n    qcStatus=" + qcStatus + 
				",\n    archiveStatus=" + archiveStatus + 
				",\n    uploadFilename=" + uploadFilename +
				",\n    numDataRows=" + Integer.toString(numDataRows) +
				",\n    dataColTypes=" + dataColTypes.toString() +
				",\n    userColIndices=" + userColIndices.toString() +
				",\n    userColNames=" + userColNames.toString() +
				",\n    dataColUnits=" + dataColUnits.toString() +
				",\n    dataColDescriptions=" + dataColDescriptions.toString() +
				" ]";
	}

	/**
	 * Compare using the "selected" property of cruises.
	 * Note that this is inconsistent with DashboardCruise.equals.
	 */
	public static Comparator<DashboardCruise> selectedComparator =
			new Comparator<DashboardCruise>() {
		@Override
		public int compare(DashboardCruise c1, DashboardCruise c2) {
			if ( c1 == c2 )
				return 0;
			if ( c1 == null )
				return -1;
			if ( c2 == null )
				return 1;
			Boolean s1 = c1.isSelected();
			return s1.compareTo(c2.isSelected());
		}
	};

	/**
	 * Compare using the owner of cruises
	 * Note that this is inconsistent with DashboardCruise.equals.
	 */
	public static Comparator<DashboardCruise> ownerComparator =
			new Comparator<DashboardCruise>() {
		@Override
		public int compare(DashboardCruise c1, DashboardCruise c2) {
			if ( c1 == c2 )
				return 0;
			if ( c1 == null )
				return -1;
			if ( c2 == null )
				return 1;
			return c1.getOwner().compareTo(c2.getOwner());
		}
	};

	/**
	 * Compare using the expocode of the cruises
	 * Note that this is inconsistent with DashboardCruise.equals.
	 */
	public static Comparator<DashboardCruise> expocodeComparator = 
			new Comparator<DashboardCruise>() {
		@Override
		public int compare(DashboardCruise c1, DashboardCruise c2) {
			if ( c1 == c2 )
				return 0;
			if ( c1 == null )
				return -1;
			if ( c2 == null )
				return 1;
			return c1.getExpocode().compareTo(c2.getExpocode());
		}
	};

	/**
	 * Compare using the data check status of the cruises
	 * Note that this is inconsistent with DashboardCruise.equals.
	 */
	public static Comparator<DashboardCruise> dataCheckComparator = 
			new Comparator<DashboardCruise>() {
		@Override
		public int compare(DashboardCruise c1, DashboardCruise c2) {
			if ( c1 == c2 )
				return 0;
			if ( c1 == null )
				return -1;
			if ( c2 == null )
				return 1;
			return c1.getDataCheckStatus().compareTo(c2.getDataCheckStatus());
		}
	};

	/**
	 * Compare using the metadata check status of the cruises
	 * Note that this is inconsistent with DashboardCruise.equals.
	 */
	public static Comparator<DashboardCruise> metadataCheckComparator = 
			new Comparator<DashboardCruise>() {
		@Override
		public int compare(DashboardCruise c1, DashboardCruise c2) {
			if ( c1 == c2 )
				return 0;
			if ( c1 == null )
				return -1;
			if ( c2 == null )
				return 1;
			return c1.getMetadataCheckStatus().compareTo(c2.getMetadataCheckStatus());
		}
	};

	/**
	 * Compare using the QC status string of the cruises
	 * Note that this is inconsistent with DashboardCruise.equals.
	 */
	public static Comparator<DashboardCruise> qcStatusComparator = 
			new Comparator<DashboardCruise>() {
		@Override
		public int compare(DashboardCruise c1, DashboardCruise c2) {
			if ( c1 == c2 )
				return 0;
			if ( c1 == null )
				return -1;
			if ( c2 == null )
				return 1;
			return c1.getQcStatus().compareTo(c2.getQcStatus());
		}
	};

	/**
	 * Compare using the archive status of the cruises
	 * Note that this is inconsistent with DashboardCruise.equals.
	 */
	public static Comparator<DashboardCruise> archiveStatusComparator = 
			new Comparator<DashboardCruise>() {
		@Override
		public int compare(DashboardCruise c1, DashboardCruise c2) {
			if ( c1 == c2 )
				return 0;
			if ( c1 == null )
				return -1;
			if ( c2 == null )
				return 1;
			return c1.getArchiveStatus().compareTo(c2.getArchiveStatus());
		}
	};

	/**
	 * Compare using the upload filename of the cruises
	 * Note that this is inconsistent with DashboardCruise.equals.
	 */
	public static Comparator<DashboardCruise> filenameComparator = 
			new Comparator<DashboardCruise>() {
		@Override
		public int compare(DashboardCruise c1, DashboardCruise c2) {
			if ( c1 == c2 )
				return 0;
			if ( c1 == null )
				return -1;
			if ( c2 == null )
				return 1;
			return c1.getUploadFilename().compareTo(c2.getUploadFilename());
		}
	};

}