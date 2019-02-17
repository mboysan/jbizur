package ee.ut.jbizur.role;

import ee.ut.jbizur.config.NodeConfig;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.protocol.internal.NodeAddressRegistered_IC;
import ee.ut.jbizur.protocol.internal.NodeAddressUnregistered_IC;
import org.pmw.tinylog.Logger;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RoleSettings {

    private Role roleRef;
    private Map<String, Address> memberAddressMap = new HashMap<>();
    private Set<Address> memberAddresses = new HashSet<>();

    private String roleId;
    private Address address;
    private MulticastAddress multicastAddress;
    private int anticipatedMemberCount;

    public RoleSettings() {
        defaults();
    }

    protected void defaults() {
        setRoleId(NodeConfig.getMemberId(0));
        try {
            setAddress(new TCPAddress(NodeConfig.compileTCPAddress()));
        } catch (UnknownHostException e) {
            Logger.error(e);
        }
        try {
            setMulticastAddress(new MulticastAddress(NodeConfig.compileMulticastAddress()));
        } catch (UnknownHostException e) {
            Logger.error(e);
        }
        setAnticipatedMemberCount(NodeConfig.getAnticipatedMemberCount());
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
        Address prv = memberAddressMap.putIfAbsent(toRegister.resolveAddressId(), toRegister);
        if (prv == null) {
            memberAddresses.add(toRegister);
            if (roleRef != null) {
                roleRef.handleInternalCommand(new NodeAddressRegistered_IC());
            }
            Logger.info(String.format("Address [%s] registered on role [%s]", toRegister, roleRef));
        }
    }

    protected synchronized void unregisterAddress(Address toUnregister) {
        Address prv = memberAddressMap.remove(toUnregister.resolveAddressId());
        if (prv != null) {
            memberAddresses.remove(toUnregister);
            if (roleRef != null) {
                roleRef.handleInternalCommand(new NodeAddressUnregistered_IC());
            }
            Logger.info(String.format("Address [%s] unregistered from role [%s]", toUnregister, roleRef));
        }
    }

    /**
     * @return the size of the {@link #memberAddressMap}.
     */
    public int getProcessCount(){
        return memberAddressMap.size();
    }

    public static int calcQuorumSize(int processCount) {
        return processCount/2 + 1;
    }

    public int getQuorumSize() {
        return getProcessCount()/2 + 1;
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
