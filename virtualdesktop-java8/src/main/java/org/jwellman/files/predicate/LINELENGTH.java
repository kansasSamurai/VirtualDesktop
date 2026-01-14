package org.jwellman.files.predicate;

public class LINELENGTH implements Predicate<String> {

	protected Integer value;
	
	protected LINELENGTH(Integer value) {
		this.value = value;
	}
	
	@Override
	public boolean evaluate(String s) {
		return false;
	}
	
	public static LINELENGTH equals(Integer i) {
		return new EQUALS(i);
	}
	
}