/**
 * 
 */
package gov.noaa.pmel.socat.dashboard.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Karl Smith
 */
public class DashboardCruiseUploadPage extends Composite {

	protected static String welcomeIntro = "Logged in as: ";
	protected static String logoutText = "Logout";
	protected static String introHtmlMsg = 
			"Select a cruise file to upload, and select the character set " +
			"encoding for that file.  Standard ASCII text files can use " +
			"either of the ISO, or the UTF-8, encodings.  Only use UTF-16 " +
			"if you know your file is in that encoding, but be aware that " +
			"only Western European characters can be properly handled.  " +
			"Use the Window encoding for files produced by older Window " +
			"programs.  Finally upload the file (or cancel) using the buttons " +
			"at the bottom of the page. " +
			"<br /><br /> " +
			"If you are unsure of the encoding, use the preview button to " +
			"show the beginning of the file as it will be used for SOCAT " +
			"ingestion.  Note that this uploads the entire file only for " +
			"the purpose of creating the preview. ";
	protected static String encodingText = "File encoding:";
	protected static String[] knownEncodings = {
		"ISO-8859-1", "ISO-8859-15", "UTF-8", "UTF-16", "Windows-1252"
	};
	protected static String previewText = "Preview Cruise File";
	protected static String uploadText = "Upload Cruise File";
	protected static String cancelText = "Return to Cruise List";
	protected static String noFileErrorMsg = 
			"Please select a cruise data file to upload";
	protected static String noPreviewMsg = "<p>(No file previewed)</p>";

	interface DashboardNewCruisePageUiBinder extends
			UiBinder<Widget, DashboardCruiseUploadPage> {
	}

	private static DashboardNewCruisePageUiBinder uiBinder = 
			GWT.create(DashboardNewCruisePageUiBinder.class);

	@UiField Label userInfoLabel;
	@UiField Button logoutButton;
	@UiField HTML introhtml;
	@UiField FormPanel uploadForm;
	@UiField FileUpload cruiseUpload;
	@UiField Label encodingLabel;
	@UiField ListBox encodingListBox;
	@UiField Button previewButton;
	@UiField Hidden usernameToken;
	@UiField Hidden userhashToken;
	@UiField Hidden passhashToken;
	@UiField Hidden previewToken;
	@UiField HTML previewHtml;
	@UiField Button uploadButton;
	@UiField Button cancelButton;

	DashboardCruiseUploadPage() {
		initWidget(uiBinder.createAndBindUi(this));

		logoutButton.setText(logoutText);

		introhtml.setHTML(introHtmlMsg);

		uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		uploadForm.setMethod(FormPanel.METHOD_POST);
		uploadForm.setAction(GWT.getModuleBaseURL() + "cruiseUploadService");

		encodingLabel.setText(encodingText);

		encodingListBox.setVisibleItemCount(1);
		for ( String encoding : knownEncodings ) {
			encodingListBox.addItem(encoding);
		}

		previewButton.setText(previewText);

		uploadButton.setText(uploadText);
		cancelButton.setText(cancelText);
	}

	void updatePageContents() {
		userInfoLabel.setText(welcomeIntro + 
				SafeHtmlUtils.htmlEscape(DashboardPageFactory.getUsername()));
		usernameToken.setValue(DashboardPageFactory.getUsername());
		userhashToken.setValue(DashboardPageFactory.getUserhash());
		passhashToken.setValue(DashboardPageFactory.getPasshash());
		previewHtml.setHTML(noPreviewMsg);
	}

	@UiHandler("logoutButton")
	void logoutOnClick(ClickEvent event) {
		DashboardLogout logoutPage = 
				DashboardPageFactory.getPage(DashboardLogout.class);
		RootLayoutPanel.get().remove(this);
		RootLayoutPanel.get().add(logoutPage);
		logoutPage.doLogout();
	}

	@UiHandler("previewButton") 
	void previewButtonOnClick(ClickEvent event) {
		previewToken.setValue("true");
		uploadForm.submit();
	}

	@UiHandler("uploadButton") 
	void uploadButtonOnClick(ClickEvent event) {
		previewToken.setValue("false");
		uploadForm.submit();
	}

	@UiHandler("cancelButton")
	void cancelButtonOnClick(ClickEvent event) {
		RootLayoutPanel.get().remove(this);
		DashboardCruiseListPage page = 
				DashboardPageFactory.getPage(DashboardCruiseListPage.class);
		RootLayoutPanel.get().add(page);
		// Current content should be correct; no need to update
	}

	@UiHandler("uploadForm")
	void uploadFormOnSubmit(SubmitEvent event) {
		// Make sure a file was selected
		String cruiseFilename = cruiseUpload.getFilename();
		if ( (cruiseFilename == null) || cruiseFilename.trim().isEmpty() ) {
			Window.alert(noFileErrorMsg);
			event.cancel();
		}
	}

	@UiHandler("uploadForm")
	void uploadFormOnSubmitComplete(SubmitCompleteEvent event) {
		String resultMsg = event.getResults();
		if ( resultMsg != null ) {
			// If the response was from a request to preview, display the preview
			previewHtml.setHTML("<pre>" + SafeHtmlUtils.htmlEscape(resultMsg) + "</pre>");
		}
		else {
			Window.alert("Unexpected null result from submit complete");
		}
	}

}