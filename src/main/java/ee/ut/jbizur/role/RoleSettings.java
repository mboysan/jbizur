package ee.ut.jbizur.role;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.config.JbizurConfig;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.protocol.commands.ic.NodeAddressRegistered_IC;
import ee.ut.jbizur.protocol.commands.ic.NodeAddressUnregistered_IC;
import org.pmw.tinylog.Logger;

import java.net.UnknownHostException;
import java.util.*;

public class RoleSettings {

    private Role roleRef;
    private Set<Address> memberAddresses = Collections.synchronizedSortedSet(new TreeSet<>());

    private String roleId;
    private Address address;
    private MulticastAddress multicastAddress;
    private boolean isMultiCastEnabled;
    private int anticipatedMemberCount;

    public RoleSettings() {
        defaults();
    }

    protected void defaults() {
        Optional<JbizurConfig.Members$Elm> m = Conf.get().members.stream().filter(e -> e.instance).findFirst();
        setRoleId(m.isPresent() ? m.get().id : "node-" + UUID.randomUUID().toString());
        try {
            TCPAddress tcp = m.isPresent()
                    ? TCPAddress.resolveTCPAddress(m.get().tcpAddress)
                    : TCPAddress.resolveTCPAddress(Conf.get().network.tcp.defaultAddress);
            setAddress(tcp);
        } catch (UnknownHostException e) {
            Logger.error(e);
        }
        try {
            boolean isMulticastEnabled = Conf.get().network.multicast.enabled;
            setMultiCastEnabled(isMulticastEnabled);
            if (isMulticastEnabled) {
                setMulticastAddress(
                        MulticastAddress.resolveMulticastAddress(Conf.get().network.multicast.address)
                );
            }
        } catch (UnknownHostException e) {
            Logger.error(e);
        }
        int expectedMemberCount = Conf.get().members.size();
        if (expectedMemberCount == 0) {
            expectedMemberCount = Conf.get().node.member.expectedCount;
        }
        setAnticipatedMemberCount(expectedMemberCount);
    }

    public synchronized void registerRoleRef(Role roleRef) {
        this.roleRef = roleRef;
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
    protected synchronized void registerAddress(Address toRegister){
        if (memberAddresses.add(toRegister)) {
            if (roleRef != null) {
                roleRef.handleInternalCommand(new NodeAddressRegistered_IC());
            }
            Logger.info(String.format("Address [%s] registered on role [%s]", toRegister, roleRef));
        }
    }

    protected synchronized void unregisterAddress(Address toUnregister) {
        if (memberAddresses.remove(toUnregister)) {
            if (roleRef != null) {
                roleRef.handleInternalCommand(new NodeAddressUnregistered_IC());
            }
            Logger.info(String.format("Address [%s] unregistered from role [%s]", toUnregister, roleRef));
        }
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
        return anticipatedMemberCount;
    }

    protected RoleSettings setAnticipatedMemberCount(int anticipatedMemberCount) {
        this.anticipatedMemberCount = anticipatedMemberCount;
        return this;
    }
}
