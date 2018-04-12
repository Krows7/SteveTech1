package twoweeks.entities;

import com.scs.stevetech1.entities.AbstractServerAvatar;
import com.scs.stevetech1.input.IInputDevice;
import com.scs.stevetech1.server.ClientData;
import com.scs.stevetech1.shared.IAbility;

import twoweeks.abilities.LaserRifle;
import twoweeks.models.SoldierModel;
import twoweeks.server.TwoWeeksServer;

public class MercServerAvatar extends AbstractServerAvatar { //implements IUnit {
	
	public MercServerAvatar(TwoWeeksServer _module, ClientData client, int _playerID, IInputDevice _input, int eid) {
		super(_module, client, _playerID, _input, eid, new SoldierModel(_module.getAssetManager()));
		
		IAbility abilityGun = new LaserRifle(_module, _module.getNextEntityID(), this, 0, client);
		_module.actuallyAddEntity(abilityGun);
		
		//IAbility abilityGrenades = new GrenadeLauncher(_module, _module.getNextEntityID(), this, 1, client);
		//_module.actuallyAddEntity(abilityGrenades);

	}
	
}