package com.scs.undercoveragent.entities;

import java.util.HashMap;

import com.jme3.collision.Collidable;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Spatial;
import com.scs.simplephysics.SimpleRigidBody;
import com.scs.stevetech1.entities.PhysicalEntity;
import com.scs.stevetech1.jme.JMEModelFunctions;
import com.scs.stevetech1.server.Globals;
import com.scs.stevetech1.shared.IEntityController;
import com.scs.undercoveragent.UndercoverAgentClientEntityCreator;

public class SnowHill2 extends PhysicalEntity {

	public SnowHill2(IEntityController _game, int id, float x, float y, float z, Quaternion q) {
		super(_game, id, UndercoverAgentClientEntityCreator.SNOW_HILL_2, "SnowHill2", false, true);

		if (_game.isServer()) {
			creationData = new HashMap<String, Object>();
			//creationData.put("q", q);
		}

		Spatial model = game.getAssetManager().loadModel("Models/Holiday/Terrain2.blend");
		if (_game.isServer()) {
			model.setShadowMode(ShadowMode.CastAndReceive);
		}
		JMEModelFunctions.moveYOriginTo(model, -.5f);
		this.mainNode.attachChild(model);
		
		mainNode.setLocalRotation(q);
		
		mainNode.setLocalTranslation(x, y, z);

		this.simpleRigidBody = new SimpleRigidBody<PhysicalEntity>(this, game.getPhysicsController(), false, this);
		simpleRigidBody.setModelComplexity(3);
		simpleRigidBody.setNeverMoves(true);

		model.setUserData(Globals.ENTITY, this);
		mainNode.setUserData(Globals.ENTITY, this);

		//game.getRootNode().attachChild(this.mainNode);
		//game.addEntity(this);

	}


	@Override
	public Collidable getCollidable() {
		return this.mainNode;
	}


}
