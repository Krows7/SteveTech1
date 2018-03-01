package com.scs.moonbaseassault.server;

import java.io.IOException;

import com.jme3.math.Vector3f;
import com.scs.moonbaseassault.MoonbaseAssaultStaticData;
import com.scs.moonbaseassault.abilities.LaserRifle;
import com.scs.moonbaseassault.entities.Floor;
import com.scs.moonbaseassault.entities.SoldierServerAvatar;
import com.scs.moonbaseassault.entities.MoonbaseWall;
import com.scs.simplephysics.SimpleRigidBody;
import com.scs.stevetech1.data.GameOptions;
import com.scs.stevetech1.entities.AbstractAvatar;
import com.scs.stevetech1.entities.AbstractServerAvatar;
import com.scs.stevetech1.entities.PhysicalEntity;
import com.scs.stevetech1.server.AbstractGameServer;
import com.scs.stevetech1.server.ClientData;
import com.scs.stevetech1.server.Globals;
import com.scs.stevetech1.shared.IAbility;

import ssmith.util.MyProperties;

public class MoonbaseAssaultServer extends AbstractGameServer {

	public static void main(String[] args) {
		try {
			MyProperties props = null;
			if (args.length > 0) {
				props = new MyProperties(args[0]);
			} else {
				props = new MyProperties();
				Globals.p("Warning: No config file specified");
			}
			String gameIpAddress = props.getPropertyAsString("gameIpAddress", "localhost");
			int gamePort = props.getPropertyAsInt("gamePort", 6145);
			String lobbyIpAddress = null;//props.getPropertyAsString("lobbyIpAddress", "localhost");
			int lobbyPort = props.getPropertyAsInt("lobbyPort", 6146);

			int tickrateMillis = props.getPropertyAsInt("tickrateMillis", 25);
			int sendUpdateIntervalMillis = props.getPropertyAsInt("sendUpdateIntervalMillis", 40);
			int clientRenderDelayMillis = props.getPropertyAsInt("clientRenderDelayMillis", 200);
			int timeoutMillis = props.getPropertyAsInt("timeoutMillis", 100000);
			float gravity = props.getPropertyAsFloat("gravity", -5);
			float aerodynamicness = props.getPropertyAsFloat("aerodynamicness", 0.99f);

			//startLobbyServer(lobbyPort, timeoutMillis); // Start the lobby in the same process, why not?  Feel from to comment this line out and run it seperately.  If you want a lobby.

			new MoonbaseAssaultServer(gameIpAddress, gamePort, lobbyIpAddress, lobbyPort, 
					tickrateMillis, sendUpdateIntervalMillis, clientRenderDelayMillis, timeoutMillis, gravity, aerodynamicness);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

/*
	private static void startLobbyServer(int lobbyPort, int timeout) {
		Thread r = new Thread("LobbyServer") {

			@Override
			public void run() {
				try {
					new MoonbaseAssaultLobbyServer(lobbyPort, timeout);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		r.start();


	}
*/

	public MoonbaseAssaultServer(String gameIpAddress, int gamePort, String lobbyIpAddress, int lobbyPort, 
			int tickrateMillis, int sendUpdateIntervalMillis, int clientRenderDelayMillis, int timeoutMillis, float gravity, float aerodynamicness) throws IOException {
		super(new GameOptions(MoonbaseAssaultStaticData.NAME, 1, 999, 10*1000, 60*1000, 10*1000, 
				gameIpAddress, gamePort, lobbyIpAddress, lobbyPort, 
				10, 5), tickrateMillis, sendUpdateIntervalMillis, clientRenderDelayMillis, timeoutMillis, gravity, aerodynamicness);

	}


	@Override
	public void moveAvatarToStartPosition(AbstractAvatar avatar) {
		float startHeight = .1f;
		avatar.setWorldTranslation(new Vector3f(3f, startHeight, 3f + (avatar.playerID*2)));
		Globals.p("Player starting at " + avatar.getWorldTranslation());
	}


	protected void createGame() {
		float mapSize = 20f;
		float ceilingHeight = 1.5f;

		MoonbaseWall wall = new MoonbaseWall(this, getNextEntityID(), 0, 0, 0, mapSize, ceilingHeight, "Textures/spacewall2.png", 0);
		this.actuallyAddEntity(wall);

		// Place floor & ceiling last
		Floor floor = new Floor(this, getNextEntityID(), 0, 0, 0, mapSize, .5f, mapSize, "Textures/floor0041.png");
		this.actuallyAddEntity(floor);

		Floor ceiling = new Floor(this, getNextEntityID(), 0, ceilingHeight, 0, mapSize, .5f, mapSize, "Textures/bluemetal.png");
		this.actuallyAddEntity(ceiling);
	}


	@Override
	protected AbstractServerAvatar createPlayersAvatarEntity(ClientData client, int entityid) {
		SoldierServerAvatar avatar = new SoldierServerAvatar(this, client, client.getPlayerID(), client.remoteInput, entityid);
		//avatar.getMainNode().lookAt(new Vector3f(15, avatar.avatarModel.getCameraHeight(), 15), Vector3f.UNIT_Y); // Look towards the centre

		IAbility abilityGun = new LaserRifle(this, getNextEntityID(), avatar, 0);
		this.actuallyAddEntity(abilityGun);

		return avatar;
	}


	@Override
	public void collisionOccurred(SimpleRigidBody<PhysicalEntity> a, SimpleRigidBody<PhysicalEntity> b, Vector3f point) {
		super.collisionOccurred(a, b, point);

	}


	@Override
	public float getAvatarStartHealth(AbstractAvatar avatar) {
		return 100;
	}


	@Override
	protected int getWinningSide() {
		int highestScore = -1;
		int winningSide = -1;
		boolean draw = false;
		for(ClientData c : super.clients.values()) {
			if (c.getScore() > highestScore) {
				winningSide = c.side;
				highestScore = c.getScore();
				draw = false;
			} else if (c.getScore() == highestScore) {
				draw = true;
			}
		}
		if (draw) {
			return -1;
		}
		return winningSide;
	}


	@Override
	public float getAvatarMoveSpeed(AbstractAvatar avatar) {
		return 3f;
	}


	@Override
	public float getAvatarJumpForce(AbstractAvatar avatar) {
		return 2f;
	}


}