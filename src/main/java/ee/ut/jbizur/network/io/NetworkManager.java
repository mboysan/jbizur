package ee.ut.jbizur.network.io;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.config.LogConf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.handlers.MsgListeners;
import ee.ut.jbizur.network.io.udp.Multicaster;
import ee.ut.jbizur.protocol.commands.ICommand;
import ee.ut.jbizur.protocol.commands.ic.InternalCommand;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.role.Role;
import org.pmw.tinylog.Logger;

import java.lang.reflect.InvocationTargetException;

public class NetworkManager {

    protected Role role;

    AbstractClient client;
    AbstractServer server;
    Multicaster multicaster;
    private final MsgListeners msgListeners = new MsgListeners();

    public NetworkManager(Role role) {
        this.role = role;
    }

    public NetworkManager start() {
        if (role == null) {
            throw new IllegalArgumentException("role instance is null");
        }

        this.client = createClient();
        this.server = createServer();
        this.multicaster = createMulticaster();

        Address modifiedAddress = server.initAndGetAddress(role.getSettings().getAddress());
        role.setAddress(modifiedAddress);
        server.startRecv(modifiedAddress);

        if (multicaster != null) {
            multicaster.initMulticast();
        }
        return this;
    }

    public void shutdown() {
        if (multicaster != null) {
            multicaster.shutdown();
        }
        server.shutdown();
        client.shutdown();
    }

    private Multicaster createMulticaster() {
        if (role.getSettings().isMultiCastEnabled()) {
            return new Multicaster(this, role.getSettings());
        }
        return null;
    }

    protected AbstractClient createClient() {
        try {
            Class<? extends AbstractClient> clientClass = (Class<? extends AbstractClient>) Class.forName(Conf.get().network.client);
            return clientClass.getConstructor(NetworkManager.class).newInstance(this);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("client could not be created");
    }

    protected AbstractServer createServer() {
        try {
            Class<? extends AbstractServer> serverClass = (Class<? extends AbstractServer>) Class.forName(Conf.get().network.server);
            return serverClass.getConstructor(NetworkManager.class).newInstance(this);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("server could not be initiated");
    }

    public AbstractClient getClient() {
        return client;
    }

    public AbstractServer getServer() {
        return server;
    }

    public Multicaster getMulticaster() {
        return multicaster;
    }

    public Role getRole() {
        return role;
    }

    public void sendMessage(NetworkCommand message) {
        if (LogConf.isDebugEnabled()) {
            Logger.debug("OUT " + role.logMsg(message.toString()));
        }
        if (role.getSettings().getAddress().equals(message.getReceiverAddress())) {
            handleCmd(message);
        } else {
            getClient().send(message);
        }
    }

    public void handleCmd(ICommand cmd) {
        if (cmd instanceof InternalCommand) {
            if (LogConf.isDebugEnabled()) {
                Logger.debug("IC_IN " + role.logMsg(cmd.toString()));
            }
            role.handleInternalCommand((InternalCommand) cmd);
        } else if (cmd instanceof NetworkCommand) {
            if (LogConf.isDebugEnabled()) {
                Logger.debug("NC_IN " + role.logMsg(cmd.toString()));
            }
            if (!msgListeners.tryHandle((NetworkCommand) cmd)) {
                role.handleNetworkCommand((NetworkCommand) cmd);
            }
        } else {
            throw new IllegalArgumentException("unrecognized cmd type: " + cmd.getClass());
        }
    }

    public MsgListeners getMsgListeners() {
        return msgListeners;
    }

    public String getId() {
        return role.getSettings().getRoleId();
    }

    @Override
    public String toString() {
        return "NetworkManager{" +
                "role=" + role +
                '}';
    }
}
