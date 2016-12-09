/**
 * 
 */
package gov.noaa.pmel.dashboard.programs;

import gov.noaa.pmel.dashboard.ferret.FerretConfig;
import gov.noaa.pmel.dashboard.ferret.SocatTool;
import gov.noaa.pmel.dashboard.handlers.CruiseFileHandler;
import gov.noaa.pmel.dashboard.handlers.DatabaseRequestHandler;
import gov.noaa.pmel.dashboard.handlers.DsgNcFileHandler;
import gov.noaa.pmel.dashboard.handlers.MetadataFileHandler;
import gov.noaa.pmel.dashboard.server.DsgNcFile;
import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DashboardOmeMetadata;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.server.KnownDataTypes;
import gov.noaa.pmel.dashboard.server.DsgCruiseData;
import gov.noaa.pmel.dashboard.server.DsgMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardCruise;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.TreeSet;

/**
 * Regenerates the full-data DSG files with the current data values 
 * in the DSG files but with the current metadata values in the OME 
 * XML files.  The decimated DSG files are then regenerated from the
 * full-data DSG file.
 *  
 * @author Karl Smith
 */
public class RegenerateDsgs {

	CruiseFileHandler cruiseHandler;
	DsgNcFileHandler dsgHandler;
	MetadataFileHandler metaHandler;
	KnownDataTypes knownMetadataTypes;
	KnownDataTypes knownDataFileTypes;
	DatabaseRequestHandler dbHandler;
	FerretConfig ferretConfig;

	/**
	 * Regenerate DSG files using the given configuration data.
	 * 
	 * @param configStore
	 * 		configuration data to use
	 */
	public RegenerateDsgs(DashboardConfigStore configStore) {
		cruiseHandler = configStore.getCruiseFileHandler();
		dsgHandler = configStore.getDsgNcFileHandler();
		metaHandler = configStore.getMetadataFileHandler();
		knownMetadataTypes = configStore.getKnownMetadataTypes();
		knownDataFileTypes = configStore.getKnownDataFileTypes();
		dbHandler = configStore.getDatabaseRequestHandler();
		ferretConfig = configStore.getFerretConfig();
	}

	/**
	 * Regenerate the DSG files for the given dataset.
	 * 
	 * @param expocode
	 * 		regenerate the DSG files the the dataset with this expocode
	 * @param forceIt
	 * 		if true, always regenerate the DSG files;
	 * 		if false, regenerate the DSG files only if the metadata has changed
	 * @return
	 * 		if the DSG files were regenerated
	 * @throws IllegalArgumentException
	 * 		if there was a problem regenerating the DSG files
	 */
	public boolean regenerateDsgFiles(String expocode, boolean forceIt) throws IllegalArgumentException {
		boolean updateIt = forceIt;
		String upperExpo = DashboardServerUtils.checkExpocode(expocode);
		DsgNcFile fullDataDsg;
		ArrayList<DsgCruiseData> dataVals;
		DsgMetadata updatedMeta;
		try {
			// Get just the filenames from the set of addition document
			DashboardCruise cruise = cruiseHandler.getCruiseFromInfoFile(upperExpo);

			// Read the current metadata in the full-data DSG file
			fullDataDsg = dsgHandler.getDsgNcFile(upperExpo);
			ArrayList<String> missing = fullDataDsg.readMetadata(knownMetadataTypes);
			if ( ! missing.isEmpty() )
				throw new IllegalArgumentException("Unexpected metadata fields missing from the DSG file: " + missing);
			missing = fullDataDsg.readData(knownDataFileTypes);
			if ( ! missing.isEmpty() )
				throw new IllegalArgumentException("Unexpected data fields missing from the DSG file: " + missing);
			DsgMetadata fullDataMeta = fullDataDsg.getMetadata();
			dataVals = fullDataDsg.getDataList();

			// Get the QC flag and SOCAT version from the database
			Character qcFlag = dbHandler.getQCFlag(upperExpo);
			String qcStatus = DashboardUtils.FLAG_STATUS_MAP.get(qcFlag);
			String versionStatus = dbHandler.getVersionStatus(upperExpo);
			if ( versionStatus.isEmpty() )
				throw new IllegalArgumentException("Unable to get the version and status from the database");
			String version = versionStatus.substring(0, versionStatus.length() - 1);

			// Update (but do not commit) the cruise info version number and QC status if not correct 
			if ( ! ( version.equals(cruise.getVersion()) && qcStatus.equals(cruise.getQcStatus()) ) ) {
				cruise.setVersion(version);
				cruise.setQcStatus(qcStatus);
				cruiseHandler.saveCruiseInfoToFile(cruise, null);
			}

			// Get the metadata in the OME XML file
			DashboardOmeMetadata omeMData = new DashboardOmeMetadata(
					metaHandler.getMetadataInfo(upperExpo, DashboardUtils.OME_FILENAME), metaHandler);
			// Update (but do not commit) the metadata info if not correct
			if ( ! version.equals(omeMData.getVersion()) ) {
				omeMData.setVersion(version);
				metaHandler.saveMetadataInfo(omeMData, null, false);
			}
			updatedMeta = omeMData.createSocatMetadata();

			if ( ! fullDataMeta.equals(updatedMeta) )
				updateIt = true;
		} catch (Exception ex) {
			throw new IllegalArgumentException("Problems reading the dataset " + upperExpo + ": " + ex.getMessage());
		}

		if ( updateIt ) {
			try {
				// Regenerate the DSG file with the updated metadata
				fullDataDsg.create(updatedMeta, dataVals);
				// Call Ferret to add lon360 and tmonth (calculated data should be the same)
				SocatTool tool = new SocatTool(ferretConfig);
				ArrayList<String> scriptArgs = new ArrayList<String>(1);
				scriptArgs.add(fullDataDsg.getPath());
				tool.init(scriptArgs, upperExpo, FerretConfig.Action.COMPUTE);
				tool.run();
				if ( tool.hasError() )
					throw new IllegalArgumentException("Failure in adding computed variables: " + 
							tool.getErrorMessage());

			} catch ( Exception ex ) {
				throw new IllegalArgumentException("Problems regenerating the full-data DSG files for " + 
							upperExpo + ": " + ex.getMessage());
			}
			try {
				// Regenerate the decimated-data DSG file 
				dsgHandler.decimateCruise(upperExpo);
			} catch ( Exception ex ) {
				throw new IllegalArgumentException("Problems regenerating the decimated-data DSG files for " + 
							upperExpo + ": " + ex.getMessage());
			}
		}
		return updateIt;		
	}

	/**
	 * Flag ERDDAP that the full-data and decimated-data DSG files have changed
	 */
	public void flagErddap() {
		dsgHandler.flagErddap(true, true);
	}

	/**
	 * @param args
	 * 		ExpocodesFile - update DSG files of these cruises
	 */
	public static void main(String[] args) {
		if ( args.length != 2 ) {
			System.err.println("Arguments:  ExpocodesFile  Always");
			System.err.println();
			System.err.println("Regenerates the full-data DSG files with the current data values ");
			System.err.println("in the DSG files but with the current metadata values in the OME "); 
			System.err.println("XML files.  The decimated DSG files are then regenerated from the ");
			System.err.println("full-data DSG file.  The default dashboard configuration is used ");
			System.err.println("for this process.  If Always is T or True, this regeneration always "); 
			System.err.println("occurs; otherwise if only occurs if the metadata has changed. "); 
			System.err.println();
			System.exit(1);
		}

		String expocodesFilename = args[0];
		boolean always = false;
		if ( "T".equals(args[1]) || "True".equals(args[1]) )
			always = true;

		// Get the expocodes of the datasets to update
		TreeSet<String> allExpocodes = new TreeSet<String>();
		try {
			BufferedReader expoReader = new BufferedReader(new FileReader(expocodesFilename));
			try {
				String dataline = expoReader.readLine();
				while ( dataline != null ) {
					dataline = dataline.trim();
					if ( ! ( dataline.isEmpty() || dataline.startsWith("#") ) )
						allExpocodes.add(dataline);
					dataline = expoReader.readLine();
				}
			} finally {
				expoReader.close();
			}
		} catch (Exception ex) {
			System.err.println("Error getting expocodes from " + 
					expocodesFilename + ": " + ex.getMessage());
			ex.printStackTrace();
			System.exit(1);
		}

		// Get the default dashboard configuration
		DashboardConfigStore configStore = null;		
		try {
			configStore = DashboardConfigStore.get(false);
		} catch (Exception ex) {
			System.err.println("Problems reading the default dashboard configuration file: " + ex.getMessage());
			ex.printStackTrace();
			System.exit(1);
		}
		RegenerateDsgs regenerator = new RegenerateDsgs(configStore);

		boolean changed = false;
		boolean success = true;
		try {
			// update each of the datasets
			for ( String expocode : allExpocodes ) {
				try {
					if ( regenerator.regenerateDsgFiles(expocode, always) ) {
						System.err.println("Regenerated the DSG files for " + expocode);
						changed = true;
					}
				} catch (Exception ex) {
					System.err.println(ex.getMessage());
					success = false;
				}
			}
			if ( changed ) {
				regenerator.flagErddap();
			}
		} finally {
			DashboardConfigStore.shutdown();
		}

		if ( ! success )
			System.exit(1);
		System.exit(0);
	}

}
