/**
 * 
 */
package gov.noaa.pmel.socat.dashboard.shared;

import java.io.Serializable;
import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Base class for SocatQCEvent and SocatWoceEvent. 
 * 
 * @author Karl Smith
 */
public class SocatEvent implements Serializable, IsSerializable {

	private static final long serialVersionUID = -7397909715673215928L;

	Date flagDate;
	String expocode;
	String socatVersion;
	String username;
	String realname;
	String comment;

	/**
	 * Creates an empty flag
	 */
	public SocatEvent() {
		flagDate = SocatMetadata.DATE_MISSING_VALUE;
		expocode = "";
		socatVersion = "";
		username = "";
		realname = "";
		comment = "";
	}

	/**
	 * @return 
	 * 		the date of the flag; never null 
	 * 		but may be {@link SocatMetadata#DATE_MISSING_VALUE}
	 */
	public Date getFlagDate() {
		return flagDate;
	}

	/**
	 * @param flagDate 
	 * 		the date of the flag to set; if null, {@link SocatMetadata#DATE_MISSING_VALUE}
	 */
	public void setFlagDate(Date flagDate) {
		if ( flagDate == null )
			this.flagDate = SocatMetadata.DATE_MISSING_VALUE;
		else
			this.flagDate = flagDate;
	}

	/**
	 * @return 
	 * 		the expocode; never null but may be empty
	 */
	public String getExpocode() {
		return expocode;
	}

	/**
	 * @param expocode 
	 * 		the expocode to set; if null, a empty string is assigned
	 */
	public void setExpocode(String expocode) {
		if ( expocode == null )
			this.expocode = "";
		else
			this.expocode = expocode;
	}

	/**
	 * @return 
	 * 		the SOCAT version; never null but may be empty
	 */
	public String getSocatVersion() {
		return socatVersion;
	}

	/**
	 * @param socatVersion 
	 * 		the SOCAT version to set; if null, an empty string is assigned
	 */
	public void setSocatVersion(String socatVersion) {
		if ( socatVersion == null )
			this.socatVersion = "";
		else
			this.socatVersion = socatVersion;
	}

	/**
	 * @return 
	 * 		the reviewer username; never null but may be empty
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username 
	 * 		the reviewer username to set; if null, an empty string is assigned
	 */
	public void setUsername(String username) {
		if ( username == null )
			this.username = "";
		else
			this.username = username;
	}

	/**
	 * @return 
	 * 		the reviewer's actual name; never null but may be empty
	 */
	public String getRealname() {
		return realname;
	}

	/**
	 * @param realname 
	 * 		the reviewer's actual name to set; if null, an empty string is assigned
	 */
	public void setRealname(String realname) {
		if ( realname == null )
			this.realname = "";
		else
			this.realname = realname;
	}

	/**
	 * @return 
	 * 		the comment; never null but may be empty
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment 
	 * 		the comment to set; if null an empty string is assigned
	 */
	public void setComment(String comment) {
		if ( comment == null )
			this.comment = "";
		else
			this.comment = comment;
	}

	@Override
	public int hashCode() {
		final int prime = 37;
		int result = flagDate.hashCode();
		result = result * prime + expocode.hashCode();
		result = result * prime + socatVersion.hashCode();
		result = result * prime + username.hashCode();
		result = result * prime + realname.hashCode();
		result = result * prime + comment.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;

		if ( ! (obj instanceof SocatEvent) )
			return false;
		SocatEvent other = (SocatEvent) obj;

		if ( ! flagDate.equals(other.flagDate) )
			return false;
		if ( ! expocode.equals(other.expocode) )
			return false;
		if ( ! socatVersion.equals(other.socatVersion) )
			return false;
		if ( ! username.equals(other.username) )
			return false;
		if ( ! realname.equals(other.realname) )
			return false;
		if ( ! comment.equals(other.comment) )
			return false;

		return true;
	}

	@Override
	public String toString() {
		return "SocatEvent" +
				"[\n    flagDate=" + flagDate.toString() + 
				",\n    expocode=" + expocode + 
				",\n    socatVersion=" + socatVersion.toString() + 
				",\n    username=" + username + 
				",\n    realname=" + realname + 
				",\n    comment=" + comment + 
				"]";
	}

}
