package org.jwellman.files.predicate;

public class EQUALS extends LINELENGTH {

	protected EQUALS(Integer value) {
		super(value);
	}
	
	@Override
	public boolean evaluate(String s) {
		return s.length() == this.value;
	}
	
}
