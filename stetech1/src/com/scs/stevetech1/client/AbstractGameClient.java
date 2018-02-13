package com.scs.stevetech1.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.jme3.app.state.VideoRecorderAppState;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.font.BitmapFont;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import com.scs.simplephysics.ICollisionListener;
import com.scs.simplephysics.SimplePhysicsController;
import com.scs.simplephysics.SimpleRigidBody;
import com.scs.stevetech1.components.IClientSideAnimated;
import com.scs.stevetech1.components.IClientControlled;
import com.scs.stevetech1.components.IEntity;
import com.scs.stevetech1.components.ILaunchable;
import com.scs.stevetech1.components.IPlayerControlled;
import com.scs.stevetech1.components.IProcessByClient;
import com.scs.stevetech1.components.IRemoveOnContact;
import com.scs.stevetech1.data.SimpleGameData;
import com.scs.stevetech1.entities.AbstractAvatar;
import com.scs.stevetech1.entities.AbstractClientAvatar;
import com.scs.stevetech1.entities.AbstractEnemyAvatar;
import com.scs.stevetech1.entities.PhysicalEntity;
import com.scs.stevetech1.hud.HUD;
import com.scs.stevetech1.input.IInputDevice;
import com.scs.stevetech1.input.MouseAndKeyboardCamera;
import com.scs.stevetech1.lobby.KryonetLobbyClient;
import com.scs.stevetech1.netmessages.AbilityUpdateMessage;
import com.scs.stevetech1.netmessages.AvatarStartedMessage;
import com.scs.stevetech1.netmessages.AvatarStatusMessage;
import com.scs.stevetech1.netmessages.EntityKilledMessage;
import com.scs.stevetech1.netmessages.EntityLaunchedMessage;
import com.scs.stevetech1.netmessages.EntityUpdateMessage;
import com.scs.stevetech1.netmessages.GameSuccessfullyJoinedMessage;
import com.scs.stevetech1.netmessages.GeneralCommandMessage;
import com.scs.stevetech1.netmessages.MyAbstractMessage;
import com.scs.stevetech1.netmessages.NewEntityMessage;
import com.scs.stevetech1.netmessages.NewPlayerRequestMessage;
import com.scs.stevetech1.netmessages.PingMessage;
import com.scs.stevetech1.netmessages.PlayerInputMessage;
import com.scs.stevetech1.netmessages.PlayerLeftMessage;
import com.scs.stevetech1.netmessages.RemoveEntityMessage;
import com.scs.stevetech1.netmessages.SimpleGameDataMessage;
import com.scs.stevetech1.netmessages.WelcomeClientMessage;
import com.scs.stevetech1.netmessages.lobby.ListOfGameServersMessage;
import com.scs.stevetech1.netmessages.lobby.RequestListOfGameServersMessage;
import com.scs.stevetech1.networking.IGameMessageClient;
import com.scs.stevetech1.networking.IMessageClientListener;
import com.scs.stevetech1.networking.KryonetGameClient;
import com.scs.stevetech1.server.Globals;
import com.scs.stevetech1.shared.AbstractClientEntityCreator;
import com.scs.stevetech1.shared.AbstractGameController;
import com.scs.stevetech1.shared.AverageNumberCalculator;
import com.scs.stevetech1.shared.HistoricalAnimationData;
import com.scs.stevetech1.shared.IAbility;
import com.scs.stevetech1.shared.IEntityController;
import com.scs.stevetech1.systems.client.AnimationSystem;
import com.scs.stevetech1.systems.client.ClientEntityLauncherSystem;

import ssmith.util.RealtimeInterval;

public abstract class AbstractGameClient extends AbstractGameController implements IEntityController, ActionListener, IMessageClientListener, ICollisionListener<PhysicalEntity> { 

	// Statuses
	public static final int STATUS_NOT_CONNECTED = 0;
	public static final int STATUS_CONNECTED_TO_LOBBY = 1;
	public static final int STATUS_CONNECTED_TO_GAME = 2;
	public static final int STATUS_RCVD_WELCOME = 3;
	public static final int STATUS_SENT_JOIN_REQUEST = 4;
	public static final int STATUS_JOINED_GAME = 5; // About to be sent all the entities
	public static final int STATUS_STARTED = 6; // Have received all entities

	private static final String QUIT = "Quit";
	private static final String TEST = "Test";

	private HashMap<Integer, IEntity> clientOnlyEntities = new HashMap<>(100);

	public static BitmapFont guiFont_small;
	public static AppSettings settings;
	private KryonetLobbyClient lobbyClient;
	public IGameMessageClient networkClient;
	public HUD hud;
	public IInputDevice input;

	public AbstractClientAvatar currentAvatar;
	public int playerID = -1;
	public int side = -1;
	private AverageNumberCalculator pingCalc = new AverageNumberCalculator();
	public long pingRTT;
	private long clientToServerDiffTime; // Add to current time to get server time
	public int clientStatus = STATUS_NOT_CONNECTED;
	public SimpleGameData gameData;
	protected Node gameNode = new Node("GameNode");

	//private RealtimeInterval sendInputsInterval = new RealtimeInterval(Globals.SERVER_TICKRATE_MS);
	private RealtimeInterval showGameTimeInterval = new RealtimeInterval(1000);
	private List<MyAbstractMessage> unprocessedMessages = new LinkedList<>();

	public long serverTime, renderTime;
	private String gameServerIP, lobbyIP;
	private int gamePort, lobbyPort;

	// Entity systems
	private AnimationSystem animSystem;
	private AbstractClientEntityCreator entityCreator;
	private ClientEntityLauncherSystem launchSystem;

	protected AbstractGameClient(String _gameServerIP, int _gamePort, String _lobbyIP, int _lobbyPort, AbstractClientEntityCreator _entityCreator) {
		super();

		gameServerIP = _gameServerIP;
		gamePort = _gamePort;
		lobbyIP = _lobbyIP;
		lobbyPort = _lobbyPort;
		this.entityCreator =_entityCreator;
		physicsController = new SimplePhysicsController<PhysicalEntity>(this);
		animSystem = new AnimationSystem(this);
		launchSystem = new ClientEntityLauncherSystem(this);

	}


	@Override
	public void simpleInitApp() {
		// Clear existing mappings
		getInputManager().clearMappings();
		getInputManager().clearRawInputListeners();

		assetManager.registerLocator("assets/", FileLocator.class); // default
		assetManager.registerLocator("assets/", ClasspathLocator.class);

		guiFont_small = getAssetManager().loadFont("Interface/Fonts/Console.fnt");

		cam.setFrustumPerspective(45f, (float) cam.getWidth() / cam.getHeight(), 0.01f, Globals.CAM_DIST);

		getInputManager().addMapping(QUIT, new KeyTrigger(KeyInput.KEY_ESCAPE));
		getInputManager().addListener(this, QUIT);            

		getInputManager().addMapping(TEST, new KeyTrigger(KeyInput.KEY_T));
		getInputManager().addListener(this, TEST);            

		setUpLight();

		hud = this.createHUD(getCamera());
		input = new MouseAndKeyboardCamera(getCamera(), getInputManager());

		if (Globals.RECORD_VID) {
			Globals.p("Recording video");
			VideoRecorderAppState video_recorder = new VideoRecorderAppState();
			stateManager.attach(video_recorder);
		}

		// Don't connect to network until JME is up and running!
		try {
			lobbyClient = new KryonetLobbyClient(lobbyIP, lobbyPort, lobbyPort, this, !Globals.LIVE_SERVER);
			this.clientStatus = STATUS_CONNECTED_TO_LOBBY;
			lobbyClient.sendMessageToServer(new RequestListOfGameServersMessage());
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}

		try {
			networkClient = new KryonetGameClient(gameServerIP, gamePort, gamePort, this, !Globals.LIVE_SERVER); // todo - connect to lobby first!
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}

		loopTimer.start();

		// Turn off stats
		setDisplayFps(false);
		setDisplayStatView(false);

	}


	public long getServerTime() {
		return System.currentTimeMillis() + clientToServerDiffTime;
	}

	private HUD createHUD(Camera c) {
		BitmapFont guiFont_small = getAssetManager().loadFont("Interface/Fonts/Console.fnt");
		HUD hud = new HUD(this, guiFont_small, c);
		getGuiNode().attachChild(hud);
		return hud;
	}


	/*
	 * Default light; override if required.
	 */
	protected void setUpLight() {
		AmbientLight al = new AmbientLight();
		al.setColor(ColorRGBA.White);
		getGameNode().addLight(al);

		DirectionalLight sun = new DirectionalLight();
		sun.setColor(ColorRGBA.Yellow);
		sun.setDirection(new Vector3f(.5f, -1f, .5f).normalizeLocal());
		getGameNode().addLight(sun);
	}


	@Override
	public void simpleUpdate(float tpf_secs) {
		if (tpf_secs > 1) { this.getRootNode();
		tpf_secs = 1;
		}

		try {
			serverTime = System.currentTimeMillis() + this.clientToServerDiffTime;
			renderTime = serverTime - Globals.CLIENT_RENDER_DELAY; // Render from history

			if (networkClient != null && networkClient.isConnected()) {

				// Process messages in JME thread
				synchronized (unprocessedMessages) {
					// Check we don't already know about it
					//while (!this.unprocessedMessages.isEmpty()) {
					Iterator<MyAbstractMessage> mit = this.unprocessedMessages.iterator();
					while (mit.hasNext()) {
						MyAbstractMessage message = mit.next();// this.unprocessedMessages.remove(0);
						if (message.scheduled) {
							if (message.timestamp > renderTime) {
								continue;
							}
						}
						mit.remove();

						if (message instanceof NewEntityMessage) {
							NewEntityMessage newEntityMessage = (NewEntityMessage) message;
							if (!this.entities.containsKey(newEntityMessage.entityID)) {
								createEntity(newEntityMessage, newEntityMessage.timestamp);
							} else {
								// We already know about it
							}

						} else if (message instanceof EntityUpdateMessage) {
							if (clientStatus >= STATUS_JOINED_GAME) {
								EntityUpdateMessage mainmsg = (EntityUpdateMessage)message;
								for(EntityUpdateMessage.UpdateData eum : mainmsg.data) {
									IEntity e = this.entities.get(eum.entityID);
									if (e != null) {
										//Settings.p("Received EntityUpdateMessage for " + e);
										//EntityPositionData epd = new EntityPositionData(eum.pos, eum.dir, mainmsg.timestamp);
										PhysicalEntity pe = (PhysicalEntity)e;
										if (eum.force) {
											// Set it now!
											pe.setWorldTranslation(eum.pos);
											pe.setWorldRotation(eum.dir);
											pe.clearPositiondata();
											if (pe == this.currentAvatar) {
												currentAvatar.clientAvatarPositionData.clear(); // Clear our local data as well
												currentAvatar.storeAvatarPosition(serverTime);
											}
										}
										pe.addPositionData(eum.pos, eum.dir, mainmsg.timestamp); // Store the position for use later
										if (pe instanceof IClientSideAnimated && eum.animationCode != null) {
											IClientSideAnimated ia = (IClientSideAnimated)pe;
											ia.getAnimList().addData(new HistoricalAnimationData(mainmsg.timestamp, eum.animationCode));
											/*if (eum.animationCode != null && eum.animationCode.equals(AbstractAvatar.ANIM_DIED)) {
												int dfgdfg = 456456;
											}*/
										}
									} else {
										// Globals.p("Unknown entity ID for update: " + eum.entityID);
										// Ask the server for entity details since we don't know about it.
										// No, since we might not have joined the game yet! (server uses broadcast()
										// networkClient.sendMessageToServer(new UnknownEntityMessage(eum.entityID));
									}
								}
							}
						} else if (message instanceof RemoveEntityMessage) {
							RemoveEntityMessage rem = (RemoveEntityMessage)message;
							this.removeEntity(rem.entityID);

						} else if (message instanceof GeneralCommandMessage) {
							GeneralCommandMessage msg = (GeneralCommandMessage)message;
							if (msg.command == GeneralCommandMessage.Command.AllEntitiesSent) { // We now have enough data to start
								clientStatus = STATUS_STARTED;
								this.getRootNode().attachChild(this.gameNode);
							}

						} else if (message instanceof AbilityUpdateMessage) {
							AbilityUpdateMessage aum = (AbilityUpdateMessage) message;
							IAbility a = (IAbility)entities.get(aum.entityID);
							if (a != null) {
								if (aum.timestamp > a.getLastUpdateTime()) {
									a.decode(aum);
									a.setLastUpdateTime(aum.timestamp);
								}
							}

						} else if (message instanceof EntityKilledMessage) {
							EntityKilledMessage asm = (EntityKilledMessage) message;
							PhysicalEntity killed = (PhysicalEntity)this.entities.get(asm.killedEntityID);
							PhysicalEntity killer = (PhysicalEntity)this.entities.get(asm.killerEntityID);
							if (killed == this.currentAvatar) {
								Globals.p("You have been killed by " + killer);
								AbstractAvatar avatar = (AbstractAvatar)killed;
								avatar.setAlive(false);
							} else if (killer == this.currentAvatar) {
								Globals.p("You have killed " + killed);
							}
							
						} else if (message instanceof EntityLaunchedMessage) {
							EntityLaunchedMessage elm = (EntityLaunchedMessage)message;
							this.launchSystem.scheduleLaunch(elm); //this.entities

						} else if (message instanceof AvatarStartedMessage) {
							if (Globals.DEBUG_PLAYER_RESTART) {
								Globals.p("Rcvd AvatarStartedMessage");
							}
							AvatarStartedMessage asm = (AvatarStartedMessage)message;
							if (this.currentAvatar != null && asm.entityID == this.currentAvatar.getID()) {
								AbstractAvatar avatar = (AbstractAvatar)this.entities.get(asm.entityID);
								avatar.setAlive(true); // todo -point camera fwds again
							}
						} else if (message instanceof ListOfGameServersMessage) {
							ListOfGameServersMessage logs = (ListOfGameServersMessage)message;
							// todo - do something with message

						} else if (message instanceof AvatarStatusMessage) {
							AvatarStatusMessage asm = (AvatarStatusMessage)message;
							if (this.currentAvatar != null && asm.entityID == this.currentAvatar.getID()) {
								this.hud.setHealthText((int)asm.health);
							}

						} else {
							throw new RuntimeException("Unknown message type: " + message);
						}
					}
				}

				if (clientStatus >= STATUS_CONNECTED_TO_GAME && sendPingInterval.hitInterval()) {
					networkClient.sendMessageToServer(new PingMessage(false, 0));
				}

				if (clientStatus == STATUS_STARTED) {

					this.sendInputsIfTime();

					if (Globals.SHOW_LATEST_AVATAR_POS_DATA_TIMESTAMP) {
						try {
							long timeDiff = this.currentAvatar.serverPositionData.getMostRecent().serverTimestamp - renderTime;
							this.hud.setDebugText("Latest Data is " + timeDiff + " newer than we need");
						} catch (Exception ex) {
							// do nothing, no data yet
						}
					}


					StringBuffer strListEnts = new StringBuffer(); // Log entities

					// Add entities
					Iterator<IEntity> it = this.entitiesToAdd.iterator();
					while (it.hasNext()) {
						IEntity e = it.next();
						//long timeToAdd = this.entitiesScheduledToBeAdded.get(e);
						//if (timeToAdd < renderTime) { // Only remove them when its time
						//it.remove();
						this.actuallyAddEntity(e);
						//}
					}
					it = null;
					this.entitiesToAdd.clear();

					// Remove entities
					//for(Integer i : this.toRemove.keySet()) {
					Iterator<Integer> it2 = this.entitiesToRemove.iterator();
					while (it2.hasNext()) {
						int i = it2.next();
						//long timeToRemove = this.toRemove.get(i);
						//if (timeToRemove < renderTime) { // Only remove them when its time
						//this.toRemove.remove(i);
						//it2.remove();
						this.actuallyRemoveEntity(i);
						//}
					}
					//it2 = null;
					this.entitiesToRemove.clear();

					this.launchSystem.process(renderTime);

					// Loop through each entity and process them				
					for (IEntity e : this.entities.values()) {
						if (e instanceof IPlayerControlled) {
							IPlayerControlled p = (IPlayerControlled)e;
							p.resetPlayerInput();
						}
						if (e instanceof PhysicalEntity) {
							PhysicalEntity pe = (PhysicalEntity)e;  // pe.getWorldTranslation();  
							if (pe.moves) { // Only bother with things that can move
								pe.calcPosition(this, renderTime, tpf_secs); // Must be before we process physics as this calcs additionalForce
							}
							strListEnts.append(pe.name + ": " + pe.getWorldTranslation() + "\n");
						}

						if (e instanceof IProcessByClient) {
							IProcessByClient pbc = (IProcessByClient)e;
							pbc.processByClient(this, tpf_secs); // Mainly to process client-side movement of the avatar
						}

						if (e instanceof IClientSideAnimated) {
							IClientSideAnimated pbc = (IClientSideAnimated)e;
							this.animSystem.process(pbc, tpf_secs);
						}
					}

					// Now do client-only entities
					for (IEntity e : this.clientOnlyEntities.values()) {
						if (e instanceof IProcessByClient) {
							IProcessByClient pbc = (IProcessByClient)e;
							pbc.processByClient(this, tpf_secs);
						}
					}

					this.hud.log_ta.setText(strListEnts.toString());
				}
			}

			if (showGameTimeInterval.hitInterval()) {
				if (this.gameData != null) {
					this.hud.setGameStatus(SimpleGameData.getStatusDesc(gameData.getGameStatus()));
					this.hud.setGameTime(this.gameData.getTime(serverTime));

				}
			}

			input.reset();
			
			loopTimer.waitForFinish(); // Keep clients and server running at same speed
			loopTimer.start();

		} catch (Exception ex) {
			ex.printStackTrace();
			this.quit("Error: " + ex);
		}
	}


	private void sendInputsIfTime() { // todo - rename
		if (this.currentAvatar != null) {
			// Send inputs
			if (networkClient.isConnected()) {
				//if (sendInputsInterval.hitInterval()) {  Don't need this since it's once a loop anyway
				this.networkClient.sendMessageToServer(new PlayerInputMessage(this.input));
				//}
			}
		}
	}


	/*
	 * Mainly for when a client requests the server to create an entity, e.g. a grenade (for lobbing).
	 */
	protected final void createEntity(NewEntityMessage msg, long timeToCreate) {
		IEntity e = this.entityCreator.createEntity(this, msg);
		if (e != null) {
			if (Globals.DEBUG_ENTITY_ADD_REMOVE) {
				Globals.p("Created " + e);
			}
			if (e instanceof AbstractAvatar || e instanceof IAbility || e instanceof AbstractEnemyAvatar) {
				this.actuallyAddEntity(e); // Need to add it immediately so there's an avatar to add the grenade launcher to, or a grenade launcher to add a bullet to
			} else {
				this.addEntity(e); // Schedule it for addition at the right time
			}
		}

	}


	@Override
	public void messageReceived(MyAbstractMessage message) { // todo - catch exception and stop main program
		if (Globals.DEBUG_MSGS) {
			Globals.p("Rcvd " + message.getClass().getSimpleName());
		}

		synchronized (unprocessedMessages) {
			if (message instanceof PingMessage) {
				PingMessage pingMessage = (PingMessage) message;
				if (!pingMessage.s2c) {
					long p2 = System.currentTimeMillis() - pingMessage.originalSentTime;
					this.pingRTT = this.pingCalc.add(p2);
					clientToServerDiffTime = pingMessage.responseSentTime - pingMessage.originalSentTime - (pingRTT/2); // If running on the same server, this should be 0! (or close enough)
					this.hud.setPing(pingRTT);
					//Settings.p("pingRTT = " + pingRTT);
					//Settings.p("clientToServerDiffTime = " + clientToServerDiffTime);

				} else {
					pingMessage.responseSentTime = System.currentTimeMillis();
					networkClient.sendMessageToServer(message); // Send it straight back
				}

			} else if (message instanceof GameSuccessfullyJoinedMessage) {
				GameSuccessfullyJoinedMessage npcm = (GameSuccessfullyJoinedMessage)message;
				if (this.playerID <= 0) {
					this.playerID = npcm.playerID;
					this.side = npcm.side;
					//this.hud.setDebugText("PlayerID=" + this.playerID);
					//Settings.p("We are player " + playerID);
					clientStatus = STATUS_JOINED_GAME;
				} else {
					throw new RuntimeException("Already rcvd NewPlayerAckMessage");
				}

			} else if (message instanceof WelcomeClientMessage) {
				WelcomeClientMessage rem = (WelcomeClientMessage)message;
				if (clientStatus < STATUS_RCVD_WELCOME) {
					clientStatus = STATUS_RCVD_WELCOME; // Need to wait until we receive something from the server before we can send to them?
					networkClient.sendMessageToServer(new NewPlayerRequestMessage("Mark Gray", 1));
					clientStatus = STATUS_SENT_JOIN_REQUEST;
				} else {
					throw new RuntimeException("Received second welcome message");
				}

			} else if (message instanceof SimpleGameDataMessage) {
				SimpleGameDataMessage gsm = (SimpleGameDataMessage)message;
				this.gameData = gsm.gameData;

			} else {
				unprocessedMessages.add(message);

			}

			if (Globals.DEBUG_MSGS) {
				if (clientStatus < STATUS_RCVD_WELCOME) {
					Globals.p("Still not received Welcome message");
				}
			}
		}
	}


	@Override
	public void addEntity(IEntity e) {
		if (e.getID() <= 0) {
			throw new RuntimeException("No entity id!");
		}
		this.entitiesToAdd.add(e);
	}


	private void actuallyAddEntity(IEntity e) {
		synchronized (entities) {
			if (e.getID() <= 0) {
				throw new RuntimeException("No entity id!");
			}
			this.entities.put(e.getID(), e);

			if (e instanceof PhysicalEntity) {
				if (e instanceof ILaunchable == false) { // Don't draw bullets yet! 
					PhysicalEntity pe = (PhysicalEntity)e;
					this.getGameNode().attachChild(pe.getMainNode());
				}
			}

		}
	}


	@Override
	public void removeEntity(int id) {
		this.entitiesToRemove.add(id);
	}


	private void actuallyRemoveEntity(int id) {
		synchronized (entities) {
			IEntity e = this.entities.get(id);
			if (e != null) {
				if (Globals.DEBUG_ENTITY_ADD_REMOVE) {
					Globals.p("Removing entity " + id + ":" + e.getName());
				}
				if (e instanceof PhysicalEntity) {
					PhysicalEntity pe =(PhysicalEntity)e;
					if (pe.simpleRigidBody != null) {
						this.physicsController.removeSimpleRigidBody(pe.simpleRigidBody);
					}
					pe.getMainNode().removeFromParent();
				}
				this.entities.remove(id);
			} else {
				Globals.pe("Entity id " + id + " not found for removal");
			}
		}
	}


	@Override
	public void onAction(String name, boolean value, float tpf) {
		if (name.equalsIgnoreCase(QUIT)) {
			if (value) {
				quit("User chose to");
			}
		} else if (name.equalsIgnoreCase(TEST)) {
			if (value) {
				//this.avatar.setWorldTranslation(new Vector3f(10, 10, 10));

				// Toggle client sync
				Globals.SYNC_AVATAR_POS = !Globals.SYNC_AVATAR_POS;
				Globals.p("Client sync is " + Globals.SYNC_AVATAR_POS);
			}
		}
	}


	private void quit(String reason) {
		Globals.p("quitting: " + reason);
		if (this.networkClient.isConnected()) {
			if (playerID >= 0) {
				this.networkClient.sendMessageToServer(new PlayerLeftMessage(this.playerID));
				/*try {
				executor.awaitTermination(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
			}
			this.networkClient.close();
		}
		this.stop();

	}


	@Override
	public boolean isServer() {
		return false;
	}


	@Override
	public void connected() {
		Globals.p("Connected!");

	}


	@Override
	public void disconnected() {
		Globals.p("Disconnected!");
		quit("");
	}


	@Override
	public SimplePhysicsController<PhysicalEntity> getPhysicsController() {
		return physicsController;
	}


	@Override
	public void collisionOccurred(SimpleRigidBody<PhysicalEntity> a, SimpleRigidBody<PhysicalEntity> b, Vector3f point) {
		/*if (a.userObject instanceof Floor == false && b.userObject instanceof Floor == false) {
			Globals.p("Collision between " + a.userObject + " and " + b.userObject);
		}*/
		PhysicalEntity pea = a.userObject;
		PhysicalEntity peb = b.userObject;

		if (pea instanceof IClientControlled) {
			IClientControlled cc = (IClientControlled)pea;
			//if (cc.isItOurEntity()) {
			if (pea instanceof IRemoveOnContact) {
				IRemoveOnContact roc = (IRemoveOnContact)pea;
				roc.remove();
			}
			//}
		}
		if (peb instanceof IClientControlled) {
			IClientControlled cc = (IClientControlled)peb;
			//if (cc.isItOurEntity()) {
			if (peb instanceof IRemoveOnContact) {
				IRemoveOnContact roc = (IRemoveOnContact)peb;
				roc.remove();
			}
			//}
		}

	}


	//@Override
	public void addClientOnlyEntity(IEntity e) {
		this.clientOnlyEntities.put(e.getID(), e); // todo - create toAdd
	}


	public void removeClientOnlyEntity(IEntity e) {
		this.clientOnlyEntities.remove(e.getID());
	}


	@Override
	public int getNextEntityID() {
		return nextEntityID.getAndAdd(1);
	}


	@Override
	public Node getGameNode() {
		return gameNode;
	}

}
