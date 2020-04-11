package ee.ut.jbizur.role;

import ee.ut.jbizur.config.GenCoreConf;
import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.address.MulticastAddress;
import ee.ut.jbizur.protocol.address.TCPAddress;
import ee.ut.jbizur.protocol.commands.intl.InternalCommand;
import ee.ut.jbizur.protocol.commands.intl.NodeAddressRegistered_IC;
import ee.ut.jbizur.protocol.commands.intl.NodeAddressUnregistered_IC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Consumer;

public class RoleSettings {

    private static final Logger logger = LoggerFactory.getLogger(RoleSettings.class);

    private Set<Address> memberAddresses = Collections.synchronizedSortedSet(new TreeSet<>());

    private String roleId;
    private Address address;
    private MulticastAddress multicastAddress;
    private boolean isMultiCastEnabled;
    private int anticipatedMemberCount;
    private Consumer<InternalCommand> internalCommandConsumer;

    public RoleSettings() {
        defaults();
    }

    protected void defaults() {
        List<GenCoreConf.Members$Elm> members = CoreConf.get().members;
        Optional<GenCoreConf.Members$Elm> m = members != null
                ? CoreConf.get().members.stream().filter(e -> e.instance).findFirst()
                : Optional.empty();

        setRoleId(m.isPresent() ? m.get().id : "node-" + UUID.randomUUID().toString());
        try {
            TCPAddress tcp = m.isPresent()
                    ? TCPAddress.resolveTCPAddress(m.get().tcpAddress)
                    : TCPAddress.resolveTCPAddress(CoreConf.get().network.tcp.defaultAddress);
            setAddress(tcp);
        } catch (UnknownHostException e) {
            logger.error(e.getMessage(), e);
        }
        try {
            boolean isMulticastEnabled = CoreConf.get().network.multicast.enabled;
            setMultiCastEnabled(isMulticastEnabled);
            if (isMulticastEnabled) {
                setMulticastAddress(
                        MulticastAddress.resolveMulticastAddress(CoreConf.get().network.multicast.address)
                );
            }
        } catch (UnknownHostException e) {
            logger.error(e.getMessage(), e);
        }
        setAnticipatedMemberCount(members != null
                        ? Math.max(CoreConf.get().members.size(), CoreConf.get().node.member.expectedCount)
                        : CoreConf.get().node.member.expectedCount
        );
    }

    public String getRoleId() {
        return roleId;
    }

    protected RoleSettings setRoleId(String roleId) {
        this.roleId = roleId;
        return this;
    }

    public Address getAddress() {
        return address;
    }

    protected RoleSettings setAddress(Address address) {
        this.address = address;
        return this;
    }

    public MulticastAddress getMulticastAddress() {
        return multicastAddress;
    }

    protected RoleSettings setMulticastAddress(MulticastAddress multicastAddress) {
        this.multicastAddress = multicastAddress;
        return this;
    }

    public boolean isMultiCastEnabled() {
        return isMultiCastEnabled;
    }

    public RoleSettings setMultiCastEnabled(boolean multiCastEnabled) {
        isMultiCastEnabled = multiCastEnabled;
        return this;
    }

    protected RoleSettings setMemberAddresses(Set<Address> memberAddresses) {
        for (Address address : memberAddresses) {
            registerAddress(address);
        }
        return this;
    }

    /**
     * Adds address to the set of address. If address already exist returns without doing anything.
     * @param toRegister address to register
     */
    protected synchronized boolean registerAddress(Address toRegister){
        if (memberAddresses.add(toRegister)) {
            if (internalCommandConsumer != null) {
                internalCommandConsumer.accept(new NodeAddressRegistered_IC());
            }
            logger.info("Address {} registered on role {}", toRegister, getRoleId());
            return true;
        }
        return false;
    }

    protected synchronized boolean unregisterAddress(Address toUnregister) {
        if (memberAddresses.remove(toUnregister)) {
            if (internalCommandConsumer != null) {
                internalCommandConsumer.accept(new NodeAddressUnregistered_IC());
            }
            logger.info("Address {} unregistered on role {}", toUnregister, getRoleId());
            return true;
        }
        return false;
    }

    public synchronized RoleSettings setInternalCommandConsumer(Consumer<InternalCommand> internalCommandConsumer) {
        this.internalCommandConsumer = internalCommandConsumer;
        return this;
    }

    /**
     * @return the size of the {@link #memberAddresses}.
     */
    public int getProcessCount(){
        return memberAddresses.size();
    }

    public static int calcQuorumSize(int processCount) {
        return processCount/2 + 1;
    }

    public Set<Address> getMemberAddresses() {
        return memberAddresses;
    }


    public int getAnticipatedMemberCount() {
        return Math.max(anticipatedMemberCount, getMemberAddresses().size());
    }

    protected RoleSettings setAnticipatedMemberCount(int anticipatedMemberCount) {
        this.anticipatedMemberCount = anticipatedMemberCount;
        return this;
    }
}
