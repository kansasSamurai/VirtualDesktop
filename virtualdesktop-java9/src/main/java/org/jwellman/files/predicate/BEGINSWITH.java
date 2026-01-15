package org.jwellman.files.predicate;

public class BEGINSWITH extends LINE {
	
	protected BEGINSWITH(String value) {
		super(value);
	}
	
	@Override
	public boolean evaluate(String s) {
		return s.startsWith(value);
	}
	
}
