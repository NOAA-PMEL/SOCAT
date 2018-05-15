/**
 *
 */
package gov.noaa.pmel.dashboard.handlers;

import gov.noaa.pmel.dashboard.server.DashboardConfigStore;
import gov.noaa.pmel.dashboard.server.DashboardServerUtils;
import gov.noaa.pmel.dashboard.shared.DashboardMetadata;
import gov.noaa.pmel.dashboard.shared.DashboardUtils;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Bundles files for sending out to be archived.
 *
 * @author Karl Smith
 */
public class ArchiveFilesBundler extends VersionedFileHandler {

    private static final String BUNDLE_NAME_EXTENSION = "_bundle.zip";
    private static final String MAILED_BUNDLE_NAME_ADDENDUM = "_from_SOCAT";

    private static final String EMAIL_SUBJECT_MSG_START =
            "Request for OCADS archival of dataset ";
    private static final String EMAIL_SUBJECT_MSG_MIDDLE =
            " from SOCAT dashboard user ";
    private static final String EMAIL_MSG_START =
            "Dear OCADS Archival Team, \n" +
                    "\n" +
                    "As part of submitting dataset ";
    private static final String EMAIL_MSG_MIDDLE =
            " to SOCAT for QC, \nthe SOCAT Upload Dashboard user ";
    private static final String EMAIL_MSG_END =
            " \nhas requested immediate OCADS archival of the attached data and metadata. \n" +
                    "The attached file is a ZIP file of the data and metadata, but \"" +
                    MAILED_BUNDLE_NAME_ADDENDUM + "\" \n" +
                    "has been appended to the name for sending as an email attachment. \n" +
                    "\n" +
                    "Best regards, \n" +
                    "SOCAT Team \n";

    private String[] toEmails;
    private String[] ccEmails;
    private String smtpHost;
    private String smtpPort;
    private PasswordAuthentication auth;
    private boolean debugIt;

    /**
     * A file bundler that saves the file bundles under the given directory and sends an email with the bundle to the
     * given email addresses.
     *
     * @param outputDirname
     *         save the file bundles under this directory
     * @param svnUsername
     *         username for SVN authentication; if null, the directory is not checked for version control and no version
     *         control is performed
     * @param svnPassword
     *         password for SVN authentication
     * @param toEmailAddresses
     *         e-mail addresses to send bundles to for archival
     * @param ccEmailAddresses
     *         e-mail addresses to be cc'd on the archival request
     * @param smtpHostAddress
     *         address of the SMTP host to use for email; if null or empty, "localhost" is used
     * @param smtpHostPort
     *         port number of the SMTP host to use for email; if null or empty, the appropriate default port is used
     * @param smtpUsername
     *         username for SMTPS authentication; if null or empty, SMTP is used without authentication
     * @param smtpPassword
     *         password for SMTPS authentication; if null or empty, SMTP is used without authentication
     * @param setDebug
     *         debug the SMTP connection?
     *
     * @throws IllegalArgumentException
     *         if the outputDirname directory does not exist, is not a directory, or is not under version control
     */
    public ArchiveFilesBundler(String outputDirname, String svnUsername, String svnPassword,
            String[] toEmailAddresses, String[] ccEmailAddresses, String smtpHostAddress,
            String smtpHostPort, String smtpUsername, String smtpPassword, boolean setDebug)
            throws IllegalArgumentException {
        super(outputDirname, svnUsername, svnPassword);
        if ( toEmailAddresses != null )
            toEmails = toEmailAddresses.clone();
        else
            toEmails = null;
        if ( ccEmailAddresses != null )
            ccEmails = ccEmailAddresses.clone();
        else
            ccEmails = null;
        smtpHost = smtpHostAddress;
        smtpPort = smtpHostPort;
        if ( (smtpUsername == null) || smtpUsername.isEmpty() ||
                (smtpPassword == null) || smtpPassword.isEmpty() ) {
            auth = null;
        }
        else {
            auth = new PasswordAuthentication(smtpUsername, smtpPassword);
        }
        debugIt = setDebug;
    }

    /**
     * The bundle virtual File for the given dataset. Creates the parent subdirectory, if it does not already exist, for
     * this File.
     *
     * @param datasetId
     *         return the virtual File for the dataset with this ID
     *
     * @return the bundle virtual File
     *
     * @throws IllegalArgumentException
     *         if the dataset is invalid, or if unable to generate the parent subdirectory if it does not already exist
     */
    public File getBundleFile(String datasetId) throws IllegalArgumentException {
        // Check and standardize the dataset
        String stdId = DashboardServerUtils.checkDatasetID(datasetId);
        // Create
        File parentFile = new File(filesDir, stdId.substring(0, 4));
        if ( !parentFile.isDirectory() ) {
            if ( parentFile.exists() )
                throw new IllegalArgumentException(
                        "File exists but is not a directory: " + parentFile.getPath());
            if ( !parentFile.mkdir() )
                throw new IllegalArgumentException(
                        "Problems creating the directory: " + parentFile.getPath());
        }
        // Generate the full path filename for this cruise metadata
        File bundleFile = new File(parentFile, stdId + BUNDLE_NAME_EXTENSION);
        return bundleFile;
    }

    /**
     * Creates the file bundle of original data and metadata, and emails this bundle, if appropriate, for archival. This
     * bundle is also committed to version control using the given message.
     * <p>
     * If the value of userRealName is {@link DashboardServerUtils#NOMAIL_USER_REAL_NAME} and the value of userEmail is
     * {@link DashboardServerUtils#NOMAIL_USER_EMAIL}, then the bundle is created but not emailed.
     *
     * @param datasetId
     *         create the bundle for the dataset with this ID
     * @param message
     *         version control commit message for the bundle file; if null or empty, the bundle file is not committed to
     *         version control
     * @param userRealName
     *         real name of the user make this archival request, or {@link DashboardServerUtils#NOMAIL_USER_REAL_NAME}
     * @param userEmail
     *         email address of the user making this archival request (and this address will be cc'd on the bundle email
     *         sent for archival), or {@link DashboardServerUtils#NOMAIL_USER_EMAIL}.
     *
     * @return an message indicating what was sent and to whom
     *
     * @throws IllegalArgumentException
     *         if the dataset is not valid, or if there is a problem sending the archival request email
     * @throws IOException
     *         if unable to read the default DashboardConfigStore, if the dataset is has no data or metadata files, if
     *         unable to create the bundle file, or if unable to commit the bundle to version control
     */
    public String sendOrigFilesBundle(String datasetId, String message, String userRealName,
            String userEmail) throws IllegalArgumentException, IOException {
        if ( (toEmails == null) || (toEmails.length == 0) )
            throw new IllegalArgumentException("no archival email address");
        if ( (ccEmails == null) || (ccEmails.length == 0) )
            throw new IllegalArgumentException("no cc email address");
        if ( (userRealName == null) || userRealName.isEmpty() )
            throw new IllegalArgumentException("no user name");
        if ( (userEmail == null) || userEmail.isEmpty() )
            throw new IllegalArgumentException("no user email address");
        String stdId = DashboardServerUtils.checkDatasetID(datasetId);
        DashboardConfigStore configStore = DashboardConfigStore.get(false);

        // Get the original data file for this dataset
        File dataFile = configStore.getDataFileHandler().datasetDataFile(stdId);
        if ( !dataFile.exists() )
            throw new IOException("No data file for " + stdId);

        // Get the list of metadata documents to be bundled with this data file
        ArrayList<File> addlDocs = new ArrayList<File>();
        MetadataFileHandler metadataHandler = configStore.getMetadataFileHandler();
        for (DashboardMetadata mdata : metadataHandler.getMetadataFiles(stdId)) {
            // Exclude the (dataset)/OME.xml document at this time;
            // do include the (dataset)/PI_OME.xml
            String filename = mdata.getFilename();
            if ( !filename.equals(DashboardUtils.OME_FILENAME) ) {
                addlDocs.add(metadataHandler.getMetadataFile(stdId, filename));
            }
        }
        if ( addlDocs.isEmpty() )
            throw new IOException("No metadata/supplemental documents for " + stdId);

        // Generate the bundle as a zip file
        File bundleFile = getBundleFile(stdId);
        String infoMsg = "Created files bundle " + bundleFile.getName() + " containing files:\n";
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(bundleFile));
        try {
            copyFileToBundle(zipOut, dataFile);
            infoMsg += "    " + dataFile.getName() + "\n";
            for (File metaFile : addlDocs) {
                copyFileToBundle(zipOut, metaFile);
                infoMsg += "    " + metaFile.getName() + "\n";
            }
        } finally {
            zipOut.close();
        }

        // Commit the bundle to version control
        if ( (message != null) && !message.isEmpty() ) {
            try {
                commitVersion(bundleFile, message);
            } catch ( Exception ex ) {
                throw new IOException("Problems committing the archival file bundle for " +
                        stdId + ": " + ex.getMessage());
            }
        }

        // If userRealName is "nobody" and userEmail is "nobody@nowhere" then skip the email
        if ( DashboardServerUtils.NOMAIL_USER_REAL_NAME.equals(userRealName) &&
                DashboardServerUtils.NOMAIL_USER_EMAIL.equals(userEmail) ) {
            return "Data files archival bundle created but not emailed";
        }

        // Create a Session for sending out the email
        Properties props = System.getProperties();
        if ( debugIt )
            props.setProperty("mail.debug", "true");
        props.setProperty("mail.transport.protocol", "smtp");
        if ( (smtpHost != null) && !smtpHost.isEmpty() )
            props.put("mail.smtp.host", smtpHost);
        else
            props.put("mail.smtp.host", "localhost");
        if ( (smtpPort != null) && !smtpPort.isEmpty() )
            props.put("mail.smtp.port", smtpPort);
        Session sessn;
        if ( auth != null ) {
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            sessn = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return auth;
                }
            });
        }
        else {
            sessn = Session.getInstance(props, null);
        }

        // Parse all the email addresses, add the user's email as the first cc'd address
        InternetAddress[] ccAddresses = new InternetAddress[ccEmails.length + 1];
        try {
            ccAddresses[0] = new InternetAddress(userEmail);
        } catch ( MessagingException ex ) {
            String errmsg = getMessageExceptionMsgs(ex);
            throw new IllegalArgumentException("Invalid user email address: " + errmsg, ex);
        }
        for (int k = 0; k < ccEmails.length; k++) {
            try {
                ccAddresses[k + 1] = new InternetAddress(ccEmails[k]);
            } catch ( MessagingException ex ) {
                String errmsg = getMessageExceptionMsgs(ex);
                throw new IllegalArgumentException("Invalid 'CC:' email address: " + errmsg, ex);
            }
        }
        InternetAddress[] toAddresses = new InternetAddress[toEmails.length];
        for (int k = 0; k < toEmails.length; k++) {
            try {
                toAddresses[k] = new InternetAddress(toEmails[k]);
            } catch ( MessagingException ex ) {
                String errmsg = getMessageExceptionMsgs(ex);
                throw new IllegalArgumentException("Invalid 'To:' email address: " + errmsg, ex);
            }
        }

        // Create the email message with the renamed zip attachment
        MimeMessage msg = new MimeMessage(sessn);
        try {
            msg.setHeader("X-Mailer", "ArchiveFilesBundler");
            msg.setSubject(EMAIL_SUBJECT_MSG_START + stdId +
                    EMAIL_SUBJECT_MSG_MIDDLE + userRealName);
            msg.setSentDate(new Date());
            // Set the addresses
            // Mark as sent from the second cc'd address (the dashboard's);
            // the first cc address is the user and any others are purely supplemental
            msg.setFrom(ccAddresses[1]);
            msg.setReplyTo(ccAddresses);
            msg.setRecipients(Message.RecipientType.TO, toAddresses);
            msg.setRecipients(Message.RecipientType.CC, ccAddresses);
            // Create the text message part
            MimeBodyPart textMsgPart = new MimeBodyPart();
            textMsgPart.setText(EMAIL_MSG_START + stdId + EMAIL_MSG_MIDDLE + userRealName + EMAIL_MSG_END);
            // Create the attachment message part
            MimeBodyPart attMsgPart = new MimeBodyPart();
            attMsgPart.attachFile(bundleFile);
            attMsgPart.setFileName(bundleFile.getName() + MAILED_BUNDLE_NAME_ADDENDUM);
            // Create and add the multipart document to the message
            Multipart mp = new MimeMultipart();
            mp.addBodyPart(textMsgPart);
            mp.addBodyPart(attMsgPart);
            msg.setContent(mp);
            // Update the headers
            msg.saveChanges();
        } catch ( MessagingException ex ) {
            String errmsg = getMessageExceptionMsgs(ex);
            throw new IllegalArgumentException("Unexpected problems creating the archival request email: " + errmsg,
                    ex);
        }

        // Send the email
        try {
            Transport.send(msg);
        } catch ( MessagingException ex ) {
            String errmsg = getMessageExceptionMsgs(ex);
            throw new IllegalArgumentException("Problems sending the archival request email: " + errmsg, ex);
        }

        infoMsg += "Files bundle sent To: " + toEmails[0];
        for (int k = 1; k < toEmails.length; k++) {
            infoMsg += ", " + toEmails[k];
        }
        infoMsg += "; CC: " + userEmail + ", " + ccEmails[0];
        for (int k = 1; k < ccEmails.length; k++) {
            infoMsg += ", " + ccEmails[k];
        }
        infoMsg += "\n";
        return infoMsg;
    }

    /**
     * Returns all messages in a possibly-nested MessagingException. The messages are returned as a single String by
     * joining all the Exception messages together using a comma and space.
     *
     * @param ex
     *         get the error messages from this MessagingException
     *
     * @return all error messages concatenated together using a comma and a space; if no messages are present, an empty
     * String is returned
     */
    private String getMessageExceptionMsgs(MessagingException ex) {
        String fullErrMsg = null;
        Exception nextEx = ex;
        while ( nextEx != null ) {
            String errMsg = nextEx.getMessage();
            if ( errMsg != null ) {
                if ( fullErrMsg == null ) {
                    fullErrMsg = errMsg;
                }
                else {
                    fullErrMsg += ", " + errMsg;
                }
            }
            if ( nextEx instanceof MessagingException ) {
                nextEx = ((MessagingException) nextEx).getNextException();
            }
            else {
                nextEx = null;
            }
        }
        if ( fullErrMsg == null )
            fullErrMsg = "";
        return fullErrMsg;
    }

    /**
     * Copies the contents of the given data file to the bundle file.
     *
     * @param zipOut
     *         copy the contents of the given file to here
     * @param dataFile
     *         copy the contents of this file
     *
     * @throws IOException
     *         if reading from the data files throws one, or if writing to the bundle file throws one
     */
    private void copyFileToBundle(ZipOutputStream zipOut, File dataFile) throws IOException {
        // Create the entry in the zip file
        ZipEntry entry = new ZipEntry(dataFile.getName());
        entry.setTime(dataFile.lastModified());
        zipOut.putNextEntry(entry);

        // Copy the contents of the data file to the zip file
        FileInputStream dataIn = new FileInputStream(dataFile);
        try {
            byte[] data = new byte[4096];
            int numRead;
            while ( true ) {
                numRead = dataIn.read(data);
                if ( numRead < 0 )
                    break;
                zipOut.write(data, 0, numRead);
            }
        } finally {
            dataIn.close();
        }

        // End this entry
        zipOut.closeEntry();
    }

}
