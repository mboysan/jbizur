package ee.ut.jbizur.role;

import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.address.MulticastAddress;

import java.util.Set;

public class BizurSettings extends RoleSettings {
    private int numBuckets;

    @Override
    protected void defaults() {
        super.defaults();
        setNumBuckets(CoreConf.get().consensus.bizur.bucketCount);
    }

    public int getNumBuckets() {
        return numBuckets;
    }

    protected BizurSettings setNumBuckets(int numBuckets) {
        this.numBuckets = numBuckets;
        return this;
    }

    @Override
    protected RoleSettings setRoleId(String roleId) {
        return super.setRoleId(roleId);
    }

    @Override
    protected RoleSettings setAddress(Address address) {
        return super.setAddress(address);
    }

    @Override
    protected RoleSettings setMulticastAddress(MulticastAddress multicastAddress) {
        return super.setMulticastAddress(multicastAddress);
    }

    @Override
    protected RoleSettings setMemberAddresses(Set<Address> memberAddresses) {
        return super.setMemberAddresses(memberAddresses);
    }

    @Override
    protected synchronized boolean registerAddress(Address toRegister){
        return super.registerAddress(toRegister);
    }

    @Override
    protected synchronized boolean unregisterAddress(Address toUnregister) {
        return super.unregisterAddress(toUnregister);
    }
}
