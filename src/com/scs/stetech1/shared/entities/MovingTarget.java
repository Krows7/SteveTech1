package com.scs.stetech1.shared.entities;

import java.util.HashMap;

import com.jme3.asset.TextureKey;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.JmeContext;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.scs.stetech1.components.IAffectedByPhysics;
import com.scs.stetech1.components.ICollideable;
import com.scs.stetech1.server.ServerMain;
import com.scs.stetech1.server.Settings;
import com.scs.stetech1.shared.EntityTypes;
import com.scs.stetech1.shared.IEntityController;

public class MovingTarget extends PhysicalEntity implements IAffectedByPhysics, ICollideable {// Need ICollideable so lasers don't bounce off it

	private static final float SPEED = 7;

	private Vector3f currDir = new Vector3f(0, 1f, 0);

	public MovingTarget(IEntityController _game, int id, float x, float y, float z, float w, float h, float d, String tex, float rotDegrees) {
		super(_game, id, EntityTypes.MOVING_TARGET, "MovingTarget");

		if (_game.isServer()) {
			creationData = new HashMap<String, Object>();
			creationData.put("id", id);
			creationData.put("size", new Vector3f(w, h, d));
			creationData.put("tex", tex);
			//creationData.put("rot", rotDegrees); No, since chances are it will have moved anyway
		}

		Box box1 = new Box(w/2, h/2, d/2);
		//box1.scaleTextureCoordinates(new Vector2f(WIDTH, HEIGHT));
		Geometry geometry = new Geometry("Crate", box1);
		if (_game.getJmeContext() != JmeContext.Type.Headless) { // !_game.isServer()) { // Not running in server
			TextureKey key3 = new TextureKey(tex);//Settings.getCrateTex());//"Textures/boxes and crates/" + i + ".png");
			key3.setGenerateMips(true);
			Texture tex3 = game.getAssetManager().loadTexture(key3);
			tex3.setWrap(WrapMode.Repeat);

			Material floor_mat = null;
			if (Settings.LIGHTING) {
				floor_mat = new Material(game.getAssetManager(),"Common/MatDefs/Light/Lighting.j3md");  // create a simple material
				floor_mat.setTexture("DiffuseMap", tex3);
			} else {
				floor_mat = new Material(game.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
				floor_mat.setTexture("ColorMap", tex3);
			}

			geometry.setMaterial(floor_mat);
			floor_mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
			geometry.setQueueBucket(Bucket.Transparent);
		}

		this.main_node.attachChild(geometry);
		float rads = (float)Math.toRadians(rotDegrees);
		main_node.rotate(0, rads, 0);
		main_node.setLocalTranslation(x+(w/2), y+(h/2), z+(d/2));

		rigidBodyControl = new RigidBodyControl(1f);
		//rigidBodyControl.setGravity(Vector3f.ZERO); // Floats
		rigidBodyControl = new RigidBodyControl(1f);
		if (_game.isServer() || Settings.CLIENT_SIDE_PHYSICS) {
		} else {
			rigidBodyControl.setKinematic(true);
		}
		main_node.addControl(rigidBodyControl);

		game.getBulletAppState().getPhysicsSpace().add(rigidBodyControl);
		game.getRootNode().attachChild(this.main_node);

		geometry.setUserData(Settings.ENTITY, this);
		main_node.setUserData(Settings.ENTITY, this);
		rigidBodyControl.setUserObject(this);

		game.addEntity(this);

	}


	@Override
	public void process(ServerMain server, float tpf) {
		//super.process(tpf);
		//Settings.p("Pos: " + this.getWorldTranslation());
		
		// move around
		//todo - move randomly if (Settings.r)
		this.rigidBodyControl.applyCentralForce(currDir.mult(SPEED));

	}


	@Override
	public void collidedWith(ICollideable other) {
		// Do nothing
	}

}
