package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.messenger.udp.Multicaster;
import ee.ut.jbizur.role.Role;

import java.lang.reflect.InvocationTargetException;

public class NetworkManager {

    protected Role role;

    private AbstractClient client;
    private AbstractServer server;
    private Multicaster multicaster;

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

    protected Multicaster createMulticaster() {
        if (role.getSettings().isMultiCastEnabled()) {
            return new Multicaster(role.getSettings().getMulticastAddress(), role);
        }
        return null;
    }

    protected AbstractClient createClient() {
        try {
            Class<? extends AbstractClient> clientClass = (Class<? extends AbstractClient>) Class.forName(Conf.get().network.client);
            return clientClass.getConstructor(Role.class).newInstance(role);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("client could not be created");
    }

    protected AbstractServer createServer() {
        try {
            Class<? extends AbstractServer> serverClass = (Class<? extends AbstractServer>) Class.forName(Conf.get().network.server);
            return serverClass.getConstructor(Role.class).newInstance(role);
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
}
