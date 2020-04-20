package org.jwellman.files.predicate;

public interface Predicate<T extends Object> {
	
	boolean evaluate(T evaluateMe);

}
