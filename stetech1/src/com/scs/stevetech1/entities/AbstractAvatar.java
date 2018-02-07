package com.scs.stevetech1.entities;

import java.util.HashMap;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.scs.simplephysics.SimpleCharacterControl;
import com.scs.stevetech1.client.AbstractGameClient;
import com.scs.stevetech1.components.IAffectedByPhysics;
import com.scs.stevetech1.components.IAnimatedAvatarModel;
import com.scs.stevetech1.components.ICalcHitInPast;
import com.scs.stevetech1.components.ICanShoot;
import com.scs.stevetech1.components.IPlayerControlled;
import com.scs.stevetech1.components.IProcessByServer;
import com.scs.stevetech1.input.IInputDevice;
import com.scs.stevetech1.server.AbstractGameServer;
import com.scs.stevetech1.server.Globals;
import com.scs.stevetech1.shared.IAbility;
import com.scs.stevetech1.shared.IEntityController;

public abstract class AbstractAvatar extends PhysicalEntity implements IPlayerControlled, IProcessByServer, ICanShoot, IAffectedByPhysics {

	// Animation Codes
	public static final String ANIM_IDLE = "Idle";
	public static final String ANIM_WALKING = "Walking";
	public static final String ANIM_SHOOTING = "Shooting";
	public static final String ANIM_DIED = "Died";

	private final Vector3f walkDirection = new Vector3f(); // Need sep walkDir as we set y=0 on this one, but not the one in RigidBody
	public final float moveSpeed = Globals.PLAYER_MOVE_SPEED;
	protected IInputDevice input;

	//Temporary vectors used on each frame.
	private final Vector3f camDir = new Vector3f();
	private final Vector3f camLeft = new Vector3f();

	public final int playerID;
	public Spatial playerGeometry;
	protected float health;
	public IAbility[] ability = new IAbility[2];
	public int side = -1;
	protected IAnimatedAvatarModel avatarModel;

	protected boolean alive = true;
	protected float restartTimeSecs, invulnerableTimeSecs;
	protected long lastMoveTime = System.currentTimeMillis() + 5000;

	public AbstractAvatar(IEntityController _game, int _playerID, IInputDevice _input, int eid, int _side, IAnimatedAvatarModel _anim) {
		super(_game, eid, 1, "Player", true);

		if (game.isServer()) {
			creationData = new HashMap<String, Object>();
			creationData.put("id", eid); this.getID();
			creationData.put("playerID", _playerID);
			creationData.put("side", _side);
		}

		playerID = _playerID;
		input = _input;
		side =_side;
		avatarModel = _anim;

		playerGeometry = avatarModel.getModel(!game.isServer());// getPlayersModel(game, playerID);
		playerGeometry.setCullHint(CullHint.Always); // Don't draw ourselves - yet?

		this.getMainNode().attachChild(playerGeometry);

		this.simpleRigidBody = new SimpleCharacterControl<PhysicalEntity>(this.mainNode, game.getPhysicsController(), this);

		//game.getRootNode().attachChild(this.mainNode);

		playerGeometry.setUserData(Globals.ENTITY, this);
		this.getMainNode().setUserData(Globals.ENTITY, this);

	}


	protected void serverAndClientProcess(AbstractGameServer server, AbstractGameClient client, float tpf_secs, long serverTime) {
		this.resetWalkDir();

		avatarModel.setAnimationForCode(ANIM_IDLE); // Default

		// Check for any abilities/guns being fired
		for (int i=0 ; i< this.ability.length ; i++) {
			if (this.ability[i] != null) {
				if (input.isAbilityPressed(i)) { // Must be before we set the walkDirection & moveSpeed, as this method may affect it
					//Settings.p("Using " + this.ability.toString());
					avatarModel.setAnimationForCode(ANIM_SHOOTING); // Default
					this.ability[i].activate();
				}
			}
		}

		camDir.set(input.getDirection()).multLocal(moveSpeed, 0.0f, moveSpeed);
		camLeft.set(input.getLeft()).multLocal(moveSpeed);
		if (input.getFwdValue()) {
			//Settings.p("fwd=" + input.getFwdValue());
			walkDirection.addLocal(camDir);  //this.getMainNode().getWorldTranslation();
			avatarModel.setAnimationForCode(ANIM_WALKING);
			lastMoveTime = System.currentTimeMillis();
		} else if (input.getBackValue()) {
			walkDirection.addLocal(camDir.negate());
			avatarModel.setAnimationForCode(ANIM_WALKING);
			lastMoveTime = System.currentTimeMillis();
		}
		if (input.getStrafeLeftValue()) {		
			walkDirection.addLocal(camLeft);
			avatarModel.setAnimationForCode(ANIM_WALKING);
			lastMoveTime = System.currentTimeMillis();
		} else if (input.getStrafeRightValue()) {		
			walkDirection.addLocal(camLeft.negate());
			avatarModel.setAnimationForCode(ANIM_WALKING);
			lastMoveTime = System.currentTimeMillis();
		}
		if (input.isJumpPressed()){
			this.jump();
		}

		if (this.walkDirection.length() != 0) {
			if (!this.game.isServer() || Globals.STOP_SERVER_AVATAR_MOVING == false) {
				SimpleCharacterControl<PhysicalEntity> simplePlayerControl = (SimpleCharacterControl<PhysicalEntity>)this.simpleRigidBody; 
				simplePlayerControl.getAdditionalForce().addLocal(walkDirection);
				if (Globals.SHOW_AVATAR_WALK_DIR) {
					Globals.p("time=" + serverTime + ",   pos=" + this.getWorldTranslation());// + "  walkDirection=" + walkDirection);
				}
			}
		}

		simpleRigidBody.process(tpf_secs);

		// Point us in the right direction
		/*if (this.game.isServer()) {
			Vector3f lookAtPoint = camLeft.add(camDir.mult(10));
			lookAtPoint.y = this.getMainNode().getWorldTranslation().y; // Look horizontal!
			this.getMainNode().lookAt(lookAtPoint, Vector3f.UNIT_Y); // need this in order to send the avatar's rotation to other players
		}*/
	}


	public void resetWalkDir() {
		this.walkDirection.set(0, 0, 0);
	}


	public void jump() {
		//Globals.p("Jumping!");
		SimpleCharacterControl<PhysicalEntity> simplePlayerControl = (SimpleCharacterControl<PhysicalEntity>)this.simpleRigidBody; 
		if (simplePlayerControl.jump()) {
			lastMoveTime = System.currentTimeMillis();
		}
	}


	/*@Override
	public boolean canMove() {
		return true; // Always calc for avatars
	}
	 */

	@Override
	public boolean sendUpdates() {
		return true; // Always send for avatars
	}

	/*
	public boolean isShooting() {
		return this.input.isShootPressed();
	}
	 */

	/*
	 * Need this since we can't warp a player to correct their position, as they may warp into walls!
	 * Also, we're adjusting their position based on the past, so we want to offset them, rather than move them to
	 * a specific point
	 */
	@Override
	public void adjustWorldTranslation(Vector3f offset) { // Adjust avatars differently to normal entities
		SimpleCharacterControl<PhysicalEntity> simplePlayerControl = (SimpleCharacterControl<PhysicalEntity>)this.simpleRigidBody;
		simplePlayerControl.getAdditionalForce().addLocal(offset);
	}


	//	@Override
	public int getSide() {
		return side;
	}


	public void addAbility(IAbility a, int num) {
		this.ability[num] = a;
		/*if (num == 0) {
			this.abilityGun = a;
		} else if (num == 1) {
			this.abilityOther = a;
		} else {
			throw new RuntimeException("Unknown ability num: " + num);
		}*/
	}


	@Override
	public void resetPlayerInput() {
		SimpleCharacterControl<PhysicalEntity> simplePlayerControl = (SimpleCharacterControl<PhysicalEntity>)this.simpleRigidBody;
		simplePlayerControl.getAdditionalForce().set(0, 0, 0);
	}


	@Override
	public Vector3f getBulletStartPos() {
		return this.getWorldTranslation().add(0, avatarModel.getBulletStartHeight(), 0);
	}


	@Override
	public void remove() {
		for (int i=0 ; i< this.ability.length ; i++) {
			if (this.ability[i] != null) {
				this.ability[i].remove();
			}
		}
		super.remove();
	}


	public ICalcHitInPast getAnyAbilitiesShootingInPast() {
		for (int i=0 ; i< this.ability.length ; i++) {
			if (this.ability[i] != null) {
				if (this.ability[i] instanceof ICalcHitInPast) {
					return (ICalcHitInPast)this.ability[i];
				}
			}
		}
		return null;
	}


	public void setAlive(boolean a) {
		if (this.alive != a) {
			this.alive = a;
			// Note that the client has been told they have died, but the player shouldn't know until the client render time has caught up!
		}
	}


	public boolean isAlive() {
		return alive;
	}


	public float getHealth() {
		return this.health;
	}


	public void setHealth(float h) {
		this.health = h;
	}
}
