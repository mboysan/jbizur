package ee.ut.jbizur.network.messenger;

import ee.ut.jbizur.annotations.ForTestingOnly;
import ee.ut.jbizur.config.GeneralConfig;
import ee.ut.jbizur.config.NodeConfig;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.messenger.udp.Multicaster;
import ee.ut.jbizur.protocol.commands.ping.Connect_NC;
import ee.ut.jbizur.role.Role;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MessageProcessor {

    protected Role role;

    private AbstractClient client;
    private AbstractServer server;
    private Multicaster multicaster;

    private ScheduledExecutorService multicastExecutor;

    @ForTestingOnly
    public MessageProcessor() {
        this(null);
    }

    public MessageProcessor(Role role) {
        this.role = role;
    }

    @ForTestingOnly
    public void registerRole(Role role) {
        this.role = role;
    }

    public void start() {
        if (role == null) {
            throw new IllegalArgumentException("role instance is null");
        }

        this.client = createClient();
        this.server = createServer();
        this.multicaster = createMulticaster();

        Address modifiedAddress = server.initAndGetAddress(role.getSettings().getAddress());
        role.setAddress(modifiedAddress);
        server.startRecv(modifiedAddress);

        initMulticast();
    }

    public void shutdown() {
        if (multicastExecutor != null) {
            multicastExecutor.shutdown();
        }
        if (multicaster != null) {
            multicaster.shutdown();
        }
        server.shutdown();
    }

    protected Multicaster createMulticaster() {
        return new Multicaster(role.getSettings().getMulticastAddress(), role);
    }

    protected AbstractClient createClient() {
        Class<? extends AbstractClient> clientClass = GeneralConfig.getClientClass();
        try {
            return clientClass.getConstructor(Role.class).newInstance(role);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("client could not be created");
    }

    protected AbstractServer createServer() {
        Class<? extends AbstractServer> serverClass = GeneralConfig.getServerClass();
        try {
            return serverClass.getConstructor(Role.class).newInstance(role);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("server could not be initiated");
    }

    protected void initMulticast() {
        if (role.isAddressesAlreadyRegistered() || multicaster == null) {
            return;
        }
        multicastExecutor = Executors.newScheduledThreadPool(1);
        multicaster.startRecv();
        multicastExecutor.scheduleAtFixedRate(() -> {
            if (!role.checkNodesDiscovered()) {
                multicaster.multicast(
                        new Connect_NC()
                                .setSenderId(role.getSettings().getRoleId())
                                .setSenderAddress(role.getSettings().getAddress())
                                .setNodeType("member")
                );
            } else {
                multicastExecutor.shutdown();
            }
        }, 0, NodeConfig.getMulticastIntervalMs(), TimeUnit.MILLISECONDS);
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
