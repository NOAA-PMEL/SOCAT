package uk.ac.uea.socat.sanitychecker.metadata;

import java.text.ParseException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import uk.ac.uea.socat.sanitychecker.config.MetadataConfigItem;
import uk.ac.uea.socat.sanitychecker.data.SocatDataRecord;
import uk.ac.uea.socat.sanitychecker.data.datetime.DateTimeHandler;


/**
 * Implementation of the {@code MetadataItem} class
 * to extract dates from the data in the file
 */
public class DataDateMetadataItem extends MetadataItem {

	DateTime itsDate = null;
	
	/**
	 * Constructs a metadata item object.
	 * @param config The configuration for the metadata item
	 * @param value The value of the metadata item
	 * @throws ParseException If the supplied in value could not be parsed into the correct data type
	 */
	public DataDateMetadataItem(MetadataConfigItem config, int line, Logger logger) throws ParseException {
		super(config, line, logger);
		itCanGenerate = true;
		itCanGenerateFromOneRecord = false;
	}

	@Override
	public void generateValue(DateTimeHandler dateTimeHandler) throws MetadataException {
		setValue(itsDate);
	}

	@Override
	public void processRecordForValue(Map<String, MetadataItem> metadataSet, SocatDataRecord record) throws MetadataException {
		
		// Get the record's date
		if (!record.getColumn(SocatDataRecord.YEAR_COLUMN_NAME).getValue().equalsIgnoreCase("NaN")) {
		
			int year = Integer.parseInt(record.getColumn(SocatDataRecord.YEAR_COLUMN_NAME).getValue());
			int month = Integer.parseInt(record.getColumn(SocatDataRecord.MONTH_COLUMN_NAME).getValue());
			int day = Integer.parseInt(record.getColumn(SocatDataRecord.DAY_COLUMN_NAME).getValue());
			DateTime newDate = new DateTime(year, month, day, 0, 0, 0).withTimeAtStartOfDay();
			
			// If no date is currently set, then this record's date is be recorded
			if (null == itsDate) {
				itsDate = newDate;
			} else {
				
				/* If we're looking for the start date, only store this date if it's before
				   what we already have */ 
				if (itsConfigItem.getGeneratorParameter().equalsIgnoreCase("start")) {
					if (newDate.isBefore(itsDate)) {
						itsDate = newDate;
					}
				/* If we're looking for the end date, only store this date if it's after
				   what we already have */ 
				} else if (itsConfigItem.getGeneratorParameter().equalsIgnoreCase("end")) {
					if (newDate.isAfter(itsDate)) {
						itsDate = newDate;
					}
				}
			}
		}
	}
}
