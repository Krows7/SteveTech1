package com.scs.stetech1.components;

import java.util.HashMap;

public interface IEntity {

	String getName();
	
	int getID();
	
	int getType();
	
	HashMap<String, Object> getCreationData();
	
	void remove();
	
}
