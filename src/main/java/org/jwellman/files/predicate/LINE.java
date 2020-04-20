package org.jwellman.files.predicate;

public class LINE implements Predicate<String> {

	protected String value;
	
	protected LINE(String value) {
		this.value = value;
	}
	
	@Override
	public boolean evaluate(String s) {
		return false;
	}
	
	public static LINE beginsWith(String s) {
		return new BEGINSWITH(s);
	}
	
}