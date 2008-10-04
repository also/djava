package com.ryanberdeen.djava;

public class TargetNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 1;

	public TargetNotFoundException(Integer id, String methodName) {
		super("Could not invoke '" + methodName + "'. Target " + id + " not found.");
	}
}
