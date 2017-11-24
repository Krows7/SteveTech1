package com.scs.stetech1.jme;

import java.util.Collection;
import java.util.Iterator;

public interface ISimplePhysicsController {

	Collection<Object> getEntities(); // todo - use T?

	void collisionOccurred(SimpleRigidBody a, Object b);
	
}
