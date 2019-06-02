package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.MulticastAddress;
import ee.ut.jbizur.role.RoleSettings;

import java.util.Set;

public class BizurSettings extends RoleSettings {
    private int numBuckets;

    @Override
    protected void defaults() {
        super.defaults();
        setNumBuckets(Conf.get().consensus.bizur.bucketCount);
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
    protected void registerAddress(Address toRegister){
        super.registerAddress(toRegister);
    }

    @Override
    protected void unregisterAddress(Address toUnregister) {
        super.unregisterAddress(toUnregister);
    }
}
