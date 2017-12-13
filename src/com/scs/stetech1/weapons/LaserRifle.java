package com.scs.stetech1.weapons;

import java.util.HashMap;
import java.util.LinkedList;

import com.scs.stetech1.components.IRequiresAmmoCache;
import com.scs.stetech1.entities.AbstractAvatar;
import com.scs.stetech1.shared.EntityTypes;
import com.scs.stetech1.shared.IAbility;
import com.scs.stetech1.shared.IEntityController;
import com.scs.testgame.entities.Grenade;
import com.scs.testgame.entities.LaserBullet;

/*
 * This gun shoots physical laser bolts
 */
public class LaserRifle extends AbstractMagazineGun implements IAbility, IRequiresAmmoCache<LaserBullet> {

	private LinkedList<LaserBullet> ammoCache = new LinkedList<LaserBullet>(); 
	
	public LaserRifle(IEntityController game, int id, AbstractAvatar owner, int num) {
		super(game, id, EntityTypes.LASER_RIFLE, owner, num, "Laser Rifle", .2f, 2, 10);

		if (game.isServer()) {
			creationData = new HashMap<String, Object>();
			creationData.put("ownerid", owner.id);
			creationData.put("num", num);
		}

	}


	@Override
	public void launchBullet() {
		LaserBullet g = ammoCache.remove();
		g.launch();
	}




	@Override
	public int getAmmoType() {
		return EntityTypes.LASER_BULLET;
	}


	@Override
	public boolean requiresAmmo() {
		return this.ammoCache.size() <= 2;
	}


	@Override
	public HashMap<String, Object> getCreationData() {
		return super.creationData;
	}


	@Override
	public void addToCache(LaserBullet o) {
		this.ammoCache.add(o);
	}


}
