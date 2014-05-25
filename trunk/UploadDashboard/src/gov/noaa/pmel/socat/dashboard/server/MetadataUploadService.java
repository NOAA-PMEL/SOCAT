/**
 * 
 */
package gov.noaa.pmel.socat.dashboard.server;

import gov.noaa.pmel.socat.dashboard.ome.OmeMetadata;
import gov.noaa.pmel.socat.dashboard.shared.DashboardCruise;
import gov.noaa.pmel.socat.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.socat.dashboard.shared.DashboardUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.TreeSet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

/**
 * Service to receive the uploaded metadata file from the client
 * @author Karl Smith
 */
public class MetadataUploadService extends HttpServlet {

	private static final long serialVersionUID = 6620559111563840485L;

	private ServletFileUpload metadataUpload;

	public MetadataUploadService() {
		File servletTmpDir;
		try {
			// Get the temporary directory used by the servlet
			servletTmpDir = (File) getServletContext().getAttribute(
					"javax.servlet.context.tempdir");
		} catch (Exception ex) {
			// Just use the default system temp dir (less secure)
			servletTmpDir = null;
		}
		// Create a disk file item factory for processing requests
		DiskFileItemFactory factory = new DiskFileItemFactory();
		if ( servletTmpDir != null ) {
			// Use the temporary directory for the servlet for large files
			factory.setRepository(servletTmpDir);
		}
		// Create the file uploader using this factory
		metadataUpload = new ServletFileUpload(factory);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
																throws IOException {
		// Verify the post has the correct encoding
		if ( ! ServletFileUpload.isMultipartContent(request) ) {
			sendErrMsg(response, "Invalid request contents format for this service.");
			return;
		}

		// Get the contents from the post request
		String username = null;
		String passhash = null;
		String expocodes = null;
		String uploadTimestamp = null;
		String omeIndicator = null;
		FileItem metadataItem = null;
		try {
			// Go through each item in the request
			for ( FileItem item : metadataUpload.parseRequest(request) ) {
				String itemName = item.getFieldName();
				if ( "username".equals(itemName) ) {
					username = item.getString();
					item.delete();
				}
				else if ( "passhash".equals(itemName) ) {
					passhash = item.getString();
					item.delete();
				}
				else if ( "expocodes".equals(itemName) ) {
					expocodes = item.getString();
					item.delete();
				}
				else if ( "timestamp".equals(itemName) ) {
					uploadTimestamp = item.getString();
					item.delete();
				}
				else if ( "ometoken".equals(itemName) ) {
					omeIndicator = item.getString();
					item.delete();
				}
				else if ( "metadataupload".equals(itemName) ) {
					metadataItem = item;
				}
				else {
					item.delete();
				}
			}
		} catch (Exception ex) {
			if ( metadataItem != null )
				metadataItem.delete();
			sendErrMsg(response, "Error processing the request\n" + ex.getMessage());
			return;
		}

		// Verify page contents seem okay
		DashboardDataStore dataStore = DashboardDataStore.get();
		if ( (username == null) || (passhash == null) || 
			 (expocodes == null) || (uploadTimestamp == null) ||
			 (omeIndicator == null) || (metadataItem == null) || 
			 ( ! (omeIndicator.equals("false") || omeIndicator.equals("true")) ) || 
			 ! dataStore.validateUser(username, passhash) ) {
			metadataItem.delete();
			sendErrMsg(response, "Invalid request contents for this service.");
			return;
		}
		// Extract the cruise expocodes from the expocodes string
		TreeSet<String> cruiseExpocodes = new TreeSet<String>(); 
		try {
			cruiseExpocodes.addAll(
					DashboardUtils.decodeStringArrayList(expocodes));
			if ( cruiseExpocodes.size() < 1 )
				throw new IllegalArgumentException();
		} catch ( IllegalArgumentException ex ) {
			metadataItem.delete();
			sendErrMsg(response, "Invalid request contents for this service.");
			return;
		}

		MetadataFileHandler metadataHandler = dataStore.getMetadataFileHandler();
		CruiseFileHandler cruiseHandler = dataStore.getCruiseFileHandler();
		String uploadFilename;
		if ( omeIndicator.equals("true") ) {
			uploadFilename = OmeMetadata.OME_FILENAME;
		}
		else {
			uploadFilename = DashboardUtils.baseName(metadataItem.getName());
			if ( uploadFilename.equals(OmeMetadata.OME_FILENAME) ) {
				metadataItem.delete();
				sendErrMsg(response, "Name of the uploaded file cannot be " + 
						OmeMetadata.OME_FILENAME + "\nPlease rename the file and try again.");
			}
		}

		DashboardMetadata metadata = null;
		for ( String expo : cruiseExpocodes ) {
			try {
				// Save the metadata document for this cruise
				if ( metadata == null ) {
					metadata = metadataHandler.saveMetadataFile(expo, 
							username, uploadTimestamp, uploadFilename, metadataItem);
				}
				else {
					metadata = metadataHandler.copyMetadataFile(expo, metadata);
				}
				// Update the metadata documents associated with this cruise
				DashboardCruise cruise = cruiseHandler.getCruiseFromInfoFile(expo);
				if ( cruise == null )
					throw new IllegalArgumentException(
							"Cruise " + expo + " does not exist");
				if ( omeIndicator.equals("true") ) {
					// Make sure the contents are valid OME XML
					OmeMetadata omedata;
					try {
						omedata = new OmeMetadata(metadata);
					} catch ( IllegalArgumentException ex ) {
						// Problems with the file - delete it
						metadataHandler.removeMetadata(username, expo, 
														metadata.getFilename());
						throw new IllegalArgumentException(
								"Invalid OME metadata file:\n   " + ex.getMessage());
					}
					// Assign the OME metadata timestamp for this cruise and save
					if ( ! cruise.getOmeTimestamp().equals(omedata.getUploadTimestamp()) ) {
						cruise.setOmeTimestamp(omedata.getUploadTimestamp());
						cruiseHandler.saveCruiseInfoToFile(cruise, 
								"Assigned new OME metadata file timestamp " + 
								cruise.getOmeTimestamp() + " to cruise " + expo);
					}
				}
				else {
					// Work directly on the additional documents list in the cruise object
					TreeSet<String> addlDocTitles = cruise.getAddlDocs();
					String titleToDelete = null;
					for ( String title : addlDocTitles ) {
						if ( uploadFilename.equals(
								(DashboardMetadata.splitAddlDocsTitle(title))[0]) ) {
							titleToDelete = title;
							break;
						}
					}
					String commitMsg; 
					if ( titleToDelete != null ) {
						addlDocTitles.remove(titleToDelete);
						commitMsg = "Update additional document " + uploadFilename + 
									" (" + uploadTimestamp + ") for cruise " + expo;
					}
					else {
						commitMsg = "Add additional document " + uploadFilename + 
									" (" + uploadTimestamp + ") to cruise " + expo;
					}
					addlDocTitles.add(metadata.getAddlDocsTitle());
					cruiseHandler.saveCruiseInfoToFile(cruise, commitMsg);
				}
			} catch ( Exception ex ) {
				metadataItem.delete();
				sendErrMsg(response, ex.getMessage());
				return;
			}
		}

		// Send the success response
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter respWriter = response.getWriter();
		respWriter.println(DashboardUtils.FILE_CREATED_HEADER_TAG);
		response.flushBuffer();
	}

	/**
	 * Returns an error message in the given Response object.  
	 * The response number is still 200 (SC_OK) so the message 
	 * goes through cleanly.
	 * 
	 * @param response
	 * 		write the error message here
	 * @param errMsg
	 * 		error message to return
	 * @throws IOException 
	 * 		if writing to the response object throws one
	 */
	private void sendErrMsg(HttpServletResponse response, String errMsg) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter respWriter = response.getWriter();
		respWriter.println(errMsg);
		response.flushBuffer();
	}

}
