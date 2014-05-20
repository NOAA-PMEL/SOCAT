package gov.noaa.pmel.socat.dashboard.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.UIObject;

public class SocatUploadDashboard implements EntryPoint, ValueChangeHandler<String> {

	public static final DashboardResources resources = 
			GWT.create(DashboardResources.class);

	/**
	 * Enumerated type to specify pages for browser history.
	 */
	public enum PagesEnum {
		/** History tag for DashboardLoginPage */
		LOGIN,
		/** History tag for CruiseListPage */
		SHOW_DATASETS,
		/** History tag for CruiseUploadPage */
		UPLOAD_DATASETS,
		/** History tag for DataColumnSpecsPage */
		IDENTIFY_COLUMNS,
		/** History tag for DataMessagesPage */
		SHOW_DATA_MESSAGES,
		/** History tag for OmeManagerPage */
		EDIT_METADATA,
		/** History tag for AddlDocsManagerPage */
		MANAGE_DOCUMENTS,
		/** History tag for AddToSocatPage */
		SUBMIT_FOR_QC,
		/** History tag for DashboardLogoutPage */
		LOGOUT
	}

	// Column widths in em's
	static final double CHECKBOX_COLUMN_WIDTH = 2.5;
	static final double NARROW_COLUMN_WIDTH = 5.0;
	static final double NORMAL_COLUMN_WIDTH = 9.0;
	static final double FILENAME_COLUMN_WIDTH = 16.0;

	// Data warning and error colors
	static final String WARNING_COLOR = "#FFCC33";
	static final String ERROR_COLOR = "#FF8888";

	// Singleton instance of this object
	private static SocatUploadDashboard singleton = null;

	// Keep a record of the currently displayed page
	private Composite currentPage;
	// PopupPanel for displaying messages 
	private DashboardInfoPopup msgPopup;

	/**
	 * Create the manager for the SocatUploadDashboard pages.
	 * Do not use this constructor; instead use the static
	 * methods provided to display pages and messages.
	 */
	SocatUploadDashboard() {
		// Just in case this gets called more than once, 
		// remove any recorded page in the previous instantiation
		if ( (singleton != null) && (singleton.currentPage != null) ) {
			RootLayoutPanel.get().remove(singleton.currentPage);
			singleton.currentPage = null;
		}
		currentPage = null;
		msgPopup = null;
		// Make sure singleton is assign to this instance since 
		// this constructor is probably called from GWT.
		singleton = this;
	}

	/**
	 * Shows the message in a popup panel centered on the page.
	 * 
	 * @param htmlMsg
	 * 		unchecked HTML message to show.
	 */
	public static void showMessage(String htmlMsg) {
		if ( singleton == null )
			singleton = new SocatUploadDashboard();
		if ( singleton.msgPopup == null )
			singleton.msgPopup = new DashboardInfoPopup();
		singleton.msgPopup.setInfoMessage(htmlMsg);
		singleton.msgPopup.showCentered();
	}

	/**
	 * Shows the message in a popup panel relative to the given UI obect. 
	 * See {@link PopupPanel#showRelativeTo(UIObject)}. 
	 * 
	 * @param htmlMsg
	 * 		unchecked HTML message to show.
	 * @param obj
	 * 		show the message relative to this object
	 * 		(usually underneath, left-aligned)
	 */
	public static void showMessageAt(String htmlMsg, UIObject obj) {
		if ( singleton == null )
			singleton = new SocatUploadDashboard();
		if ( singleton.msgPopup == null )
			singleton.msgPopup = new DashboardInfoPopup();
		singleton.msgPopup.setInfoMessage(htmlMsg);
		singleton.msgPopup.showRelativeTo(obj);
	}

	/**
	 * Shows an error message, along with the message from an
	 * exception, in a popup panel centered on the page.
	 * 
	 * @param htmlMsg
	 * 		unchecked HTML message to show before the exception message
	 * @param ex
	 * 		exception whose message is to be shown
	 */
	public static void showFailureMessage(String htmlMsg, Throwable ex) {
		String exceptMsg = ex.getMessage();
		if ( exceptMsg == null )
			exceptMsg = htmlMsg;
		else if ( exceptMsg.contains("</pre>") )
			exceptMsg = htmlMsg + "<br /><pre>" + 
					SafeHtmlUtils.htmlEscape(exceptMsg) + "</pre>";
		else
			exceptMsg = htmlMsg + "<br /><pre>" + exceptMsg + "</pre>";
		SocatUploadDashboard.showMessage(exceptMsg);
	}

	/**
	 * Updates the displayed page by removing any page 
	 * currently being shown and adding the given page.
	 * 
	 * @param newPage
	 * 		new page to be shown; if null, not page is shown
	 */
	public static void updateCurrentPage(Composite newPage) {
		if ( singleton == null )
			singleton = new SocatUploadDashboard();
		if ( singleton.currentPage != null )
			RootLayoutPanel.get().remove(singleton.currentPage);
		singleton.currentPage = newPage;
		if ( singleton.currentPage != null )
			RootLayoutPanel.get().add(singleton.currentPage);
	}

	/**
	 * Displays the wait cursor over the entire page
	 */
	public static void showWaitCursor() {
		RootLayoutPanel.get().getElement().getStyle().setCursor(Style.Cursor.WAIT);
	}

	/**
	 * Return the cursor to the automatically assigned one
	 */
	public static void showAutoCursor() {
		RootLayoutPanel.get().getElement().getStyle().setCursor(Style.Cursor.AUTO);
	}

	@Override
	public void onModuleLoad() {
		// setup history management
		History.addValueChangeHandler(this);
		// show the appropriate page - if new, then the login page
		History.fireCurrentHistoryState();
	}

	@Override
	public void onValueChange(ValueChangeEvent<String> event) {
		String token = event.getValue();
		if ( token != null )
			token = token.trim();
		if ( (token == null) || token.isEmpty() ) {
			// Initial history setup; show the login page
			DashboardLoginPage.showPage(true);
		}
		else if ( token.equals(PagesEnum.LOGIN.name()) ) {
			// Login page from history
			DashboardLoginPage.showPage(false);
		}
		else if ( token.equals(PagesEnum.SHOW_DATASETS.name()) ) {
			// Cruise list page from history
			CruiseListPage.redisplayPage(false);
		}
		else if ( token.equals(PagesEnum.UPLOAD_DATASETS.name()) ) {
			// Cruise upload page from history
			CruiseUploadPage.redisplayPage(false);
		}
		else if ( token.equals(PagesEnum.IDENTIFY_COLUMNS.name()) ) {
			// Data column specs page from history
			DataColumnSpecsPage.redisplayPage(false);
		}
		else if ( token.equals(PagesEnum.EDIT_METADATA.name()) ) {
			// OME metadata manager page from history
			OmeManagerPage.redisplayPage(false);
		}
		else if ( token.equals(PagesEnum.MANAGE_DOCUMENTS.name()) ) {
			// Additionl data manager page from history
			AddlDocsManagerPage.redisplayPage(false);
		}
		else if ( token.equals(PagesEnum.SUBMIT_FOR_QC.name()) ) {
			// Add to SOCAT page from history
			AddToSocatPage.redisplayPage(false);
		}
		else if ( token.equals(PagesEnum.LOGOUT.name()) ) {
			// Logout page from history
			DashboardLogoutPage.redisplayPage(false);
		}
		else {
			// Unknown page from the history; instead show the login page 
			DashboardLoginPage.showPage(true);
		}
	}

}
