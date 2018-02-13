package com.scs.stevetech1.netmessages;

import com.jme3.network.serializing.Serializable;
import com.scs.stevetech1.entities.AbstractAvatar;

@Serializable
public class AvatarStartedMessage extends MyAbstractMessage {
	
	public int entityID;
	
	public AvatarStartedMessage() {
		super();
	}

	public AvatarStartedMessage(AbstractAvatar _avatar) {
		super(true, true);
		
		entityID = _avatar.getID();
	}

}
