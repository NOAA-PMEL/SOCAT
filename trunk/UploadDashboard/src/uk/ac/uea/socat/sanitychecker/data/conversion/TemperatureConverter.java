package uk.ac.uea.socat.sanitychecker.data.conversion;

import java.util.ArrayList;

/**
 * Performs temperature conversions
 */
public class TemperatureConverter extends SpecifiedUnitsConverter {

	/**
	 * Initialise the list of supported temperature units
	 */
	public TemperatureConverter() {
		itsSupportedUnits = new ArrayList<String>();
		itsSupportedUnits.add("degc");
		itsSupportedUnits.add("celsius");
		itsSupportedUnits.add("degk");
		itsSupportedUnits.add("kelvin");
	}
	
	@Override
	public String convert(String value, String units) throws ConversionException {
		String result = value;
		
		if (units.equalsIgnoreCase("degk") || units.equalsIgnoreCase("kelvin")) {
			result = convertKelvin(value); 
		}
		
		return result;
	}
	
	/**
	 * Convert from Kelvin to Celsius
	 * @param value The input temperature (in degrees Kelvin)
	 * @return The temperature converted to degrees Celsius
	 * @throws ConversionException If the value cannot be converted
	 */
	private String convertKelvin(String value) throws ConversionException {
		String result = value;
		
		try {
			result = Double.toString(Double.parseDouble(value) + 273.15);
		} catch (NumberFormatException e) {
			throw new ConversionException("Cannot parse value");
		}
		
		return result;
	}
}
