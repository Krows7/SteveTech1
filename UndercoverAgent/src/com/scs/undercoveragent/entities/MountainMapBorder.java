package com.scs.undercoveragent.entities;

import java.util.HashMap;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.scs.stevetech1.entities.PhysicalEntity;
import com.scs.stevetech1.jme.JMEFunctions;
import com.scs.stevetech1.server.Globals;
import com.scs.stevetech1.shared.IEntityController;
import com.scs.undercoveragent.UndercoverAgentClientEntityCreator;

import ssmith.lang.NumberFunctions;

/*
 * The origin for this should be left/bottom/front
 *
 */
public class MountainMapBorder extends PhysicalEntity {

	public MountainMapBorder(IEntityController _game, int id, float x, float y, float z, float size, Vector3f dir) {
		super(_game, id, UndercoverAgentClientEntityCreator.MOUNTAIN_MAP_BORDER, "MountainMapBorder", false);

		if (_game.isServer()) {
			creationData = new HashMap<String, Object>();
			creationData.put("size", size);
			creationData.put("dir", dir);
		}

		if (!_game.isServer()) { // Not running in server
			Node container = new Node("MountainContainer");
			// Add mountain models
			for (float i=(InvisibleMapBorder.BORDER_WIDTH/2) ; i<size ; i+=InvisibleMapBorder.BORDER_WIDTH/2) { 
				Spatial model = game.getAssetManager().loadModel("Models/Holiday/Terrain2.blend");
				JMEFunctions.setTextureOnSpatial(game.getAssetManager(), model, "Textures/snow.jpg");
				JMEFunctions.scaleModelToWidth(model, InvisibleMapBorder.BORDER_WIDTH+1); // Since we rotate it, needs to be slightly wider
				model.setLocalTranslation(-InvisibleMapBorder.BORDER_WIDTH/2, 0, i);//-(InvisibleMapBorder.BORDER_WIDTH/2));
				JMEFunctions.rotateToDirection(model, NumberFunctions.rnd(0, 359));
				container.attachChild(model);
			}
			mainNode.attachChild(container);
			JMEFunctions.rotateToDirection(mainNode, dir);
		} else {
			// Do nothing on server
		}

		mainNode.setLocalTranslation(x, y, z);
		
		// Note we don't create a SimpleRigidBody, since this doesn't collide

	}

}
