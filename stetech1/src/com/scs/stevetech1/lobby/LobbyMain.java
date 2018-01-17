package com.scs.stevetech1.lobby;

import java.io.IOException;
import java.util.HashMap;

import com.scs.stevetech1.netmessages.MyAbstractMessage;
import com.scs.stevetech1.netmessages.lobby.ListOfGameServersMessage;
import com.scs.stevetech1.netmessages.lobby.RequestListOfGameServersMessage;
import com.scs.stevetech1.netmessages.lobby.UpdateLobbyMessage;
import com.scs.stevetech1.networking.IGameMessageServer;
import com.scs.stevetech1.networking.IMessageServerListener;
import com.scs.stevetech1.networking.KryonetGameServer;
import com.scs.stevetech1.server.Globals;

public class LobbyMain implements IMessageServerListener {

	private KryonetLobbyServer networkServer;

	private HashMap<String, GameServerDetails> gameServers = new HashMap<String, GameServerDetails>(); // game name::data

	public static void main(String[] args) {
		try {
			new LobbyMain();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	public LobbyMain() throws IOException {
		networkServer = new KryonetLobbyServer(Globals.LOBBY_PORT, Globals.LOBBY_PORT, this);
		// todo - loop through and remove game servers that we haven't heard of for a while
	}


	@Override
	public void connectionAdded(int id, Object net) {
		Globals.p("Game server connected to us");
	}


	@Override
	public void messageReceived(int clientid, MyAbstractMessage msg) {
		if (msg instanceof UpdateLobbyMessage) {
			UpdateLobbyMessage ulm = (UpdateLobbyMessage)msg;
			this.gameServers.remove(ulm.name);
			this.gameServers.put(ulm.name, new GameServerDetails(ulm));

			Globals.p("Updated details for game server '" + ulm.name + "'");
		} else if (msg instanceof RequestListOfGameServersMessage) {
			RequestListOfGameServersMessage rlogs = (RequestListOfGameServersMessage)msg;
			
			ListOfGameServersMessage logs = new ListOfGameServersMessage();
			for(GameServerDetails details : gameServers.values()) {
				logs.servers.add(details.ulm);
			}
			
			//todo this.networkServer.sendMessageToClient(client, msg);
		}

	}


	@Override
	public void connectionRemoved(int id) {
		Globals.p("Game server disconnected from us");

	}


}