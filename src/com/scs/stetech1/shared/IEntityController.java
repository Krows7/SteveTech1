package com.scs.stetech1.shared;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.scene.Node;
import com.scs.stetech1.components.IEntity;

public interface IEntityController {

	AssetManager getAssetManager();
	
	BulletAppState getBulletAppState();
	
	Node getRootNode();
	
	void addEntity(IEntity e);
	
	void removeEntity(IEntity e);
	
}
