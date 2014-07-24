package gov.noaa.pmel.socat.dashboard.ome;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

class OMECompositeVariable {
	private Path itsPath;
	private List<String> itsIdFields;
	private List<Value> itsValues = new ArrayList<Value>();
	
	protected OMECompositeVariable(Path parentPath, String idElement) {
		itsPath = parentPath;
		itsIdFields = new ArrayList<String>();
		itsIdFields.add(idElement);
	}
	
	protected OMECompositeVariable(Path parentPath, List<String> idElements) {
		itsPath = parentPath;
		itsIdFields = idElements;
	}
	
	private OMECompositeVariable(Path parentPath) {
		itsPath = parentPath;
	}

	protected void addValue(String name, Element element) throws IllegalArgumentException {
		String valueText = null;
		if (null != element) {
			valueText = element.getChildTextTrim(name);
			if (null != valueText) {
				addValue(name, valueText);
			}
		}
	}
	
	protected void addValue(String name, String valueText) throws IllegalArgumentException {
		
		boolean foundValue = false;
		for (Value searchValue : itsValues) {
			if (searchValue.name.equals(name)) {
				
				if (itsIdFields.contains(name) && searchValue.getValueCount() > 0) {
					throw new IllegalArgumentException("Cannot add multiple values to an identifier in a composite variable");
				} else {
					searchValue.addValue(valueText);
					foundValue = true;
					break;
				}
			}
		}
		
		if (!foundValue) {
			itsValues.add(new Value(name, valueText));
		}
	}
	
	protected void addValues(List<Value> values) {
		
		for (Value value : values) {
			
			boolean valueStored = false;
			for (Value existingValue : itsValues) {
				if (existingValue.name.equals(value.name)) {
					existingValue.addValues(value.values);
					valueStored = true;
					break;
				}
			}
			
			if (!valueStored) {
				itsValues.add(value);
			}
			
			
		}
	}
	
	protected static List<OMECompositeVariable> mergeVariables(List<OMECompositeVariable> dest, List<OMECompositeVariable> newValues) {
		
		// Copy the dest list to the output.
		List<OMECompositeVariable> merged = new ArrayList<OMECompositeVariable>();
		
		// Now copy in the new values
		for (OMECompositeVariable newValue : newValues) {
			
			OMECompositeVariable destVar = findById(dest, newValue);
			if (null == destVar) {
				merged.add((OMECompositeVariable) newValue.clone());
			} else {
				
				OMECompositeVariable mergedVar = (OMECompositeVariable) destVar.clone();
				mergedVar.addValues(newValue.getAllValues());
				merged.add(mergedVar);
			}
		}
		
		// Anything in dest but not in new can now be added
		for (OMECompositeVariable destValue : dest) {
			OMECompositeVariable matchingNew = findById(newValues, destValue);
			if (null == matchingNew) {
				merged.add((OMECompositeVariable) destValue.clone());
			}
		}
		
		return merged;
	}
	
	private List<Value> getAllValues() {
		return itsValues;
	}
	
	private static OMECompositeVariable findById(List<OMECompositeVariable> variables, OMECompositeVariable criteria) {
		OMECompositeVariable found = null;
		
		for (OMECompositeVariable variable : variables) {
			
			boolean match = true;
			for (String idField : variable.itsIdFields) {
				if (!valuesEqual(variable.getValue(idField), criteria.getValue(idField))) {
					match = false;
				}
			}
			
			if (match) {
				found = variable;
				break;
			}
		}
		
		return found;
	}
	
	private static boolean valuesEqual(String val1, String val2) {
		boolean result = false;
		
		if (null == val1 && null == val2) {
			result = true;
		} else if (null == val1 && null != val2 && val2.equals("")) {
			result = true;
		} else if (null != val1 && val1.equals("") && null == val2) {
			result = true;
		} else if (val1.equals(val2)) {
			result = true;
		}
		
		return result;
	}
		
	private Element getElement() {
		Element element = new Element(itsPath.getElementName());
		for (Value subValue : itsValues) {
			Element subElement = new Element(subValue.name);
			subElement.setText(subValue.getValue());
			
			element.addContent(subElement);
		}
		
		return element;
	}
	
	protected void generateXMLContent(Element parent, ConflictElement conflictParent) {
		parent.addContent(getElement());
		if (hasConflict()) {
			conflictParent.addContent(generateConflictElement());
		}
	}

	private Element generateConflictElement() {
		Element conflictElement = null;
		
		if (hasConflict()) {
			
			Element variableElement = new Element(itsPath.getElementName());
			for (String id : itsIdFields) {
				String value = getValue(id);
				if (null == value) {
					value = "";
				}
				
				variableElement.setAttribute(id, value);
			}
			
			for (Value value : itsValues) {
				if (value.getValueCount() > 1) {
					Element valuesElement = new Element(value.name);
					
					for (String valueString : value.getAllValues()) {
						Element valueElement = new Element("VALUE");
						valueElement.setText(valueString);
						valuesElement.addContent(valueElement);
					}
					
					variableElement.addContent(valuesElement);
				}
			}
			
			conflictElement = itsPath.buildElementTree("Conflict", variableElement);
		}
		
		return conflictElement;
	}
	
	protected boolean hasConflict() {
		boolean result = false;
		
		for (Value value : itsValues) {
			if (value.getValueCount() > 1) {
				result = true;
				break;
			}
		}
		
		return result;
	}
	
	protected String getValue(String valueName) {
		String result = "";
		
		for (Value value : itsValues) {
			if (value.name.equals(valueName)) {
				result = value.getValue();
				break;
			}
		}
		
		return result;
	}
	
	public Object clone() {
		OMECompositeVariable clone = new OMECompositeVariable((Path) itsPath.clone());
		clone.itsIdFields = new ArrayList<String>();
		for (String id : itsIdFields) {
			clone.itsIdFields.add(id);
		}
		
		for (Value value : itsValues) {
			clone.itsValues.add((Value) value.clone());
		}
		
		return clone;
	}
	
	private class Value {
		private String name;
		private List<String> values;
		
		private Value(String name, String value) {
			this.name = name;
			values = new ArrayList<String>();
			values.add(value);
		}
		
		private Value(String name) {
			this.name = name;
			values = new ArrayList<String>();
		}
		
		private void addValue(String value) {
			if (!values.contains(value)) {
				values.add(value);
			}
		}
		
		private void addValues(List<String> values) {
			for (String value: values) {
				addValue(value);
			}
		}
		
		private int getValueCount() {
			return values.size();
		}

		
		
		/**
		 * Gets the value of this variable for placing in an XML document.
		 * If the variable has no values, an empty string is returned.
		 * If the variable has exactly one value, that value is returned.
		 * If the variable has more than one value, this represents a conflict
		 * and {@link OmeMetadata#CONFLICT_STRING} is returned.
		 * 
		 * @return The value of the variable
		 */
		private String getValue() {
			
			String result;
			
			switch (values.size()) {
			case 0:
			{
				result = "";
				break;
			}
			case 1:
			{
				result = values.get(0);
				break;
			}
			default:
			{
				result = OmeMetadata.CONFLICT_STRING;
			}
			}
			
			return result;
		}
		
		private List<String> getAllValues() {
			return values;
		}
		
		public Object clone() {
			Value clone = new Value(name);
			for (String value : values) {
				clone.values.add(value);
			}
			
			return clone;
		}
	}
}