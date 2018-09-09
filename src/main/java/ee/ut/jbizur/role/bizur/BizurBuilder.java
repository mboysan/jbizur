package ee.ut.jbizur.role.bizur;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.address.MulticastAddress;

import java.util.Set;

public class BizurBuilder {

    private BizurSettings config;

    protected BizurBuilder(){
        config = new BizurSettings();
    }

    public static BizurBuilder builder() {
        return new BizurBuilder();
    }

    public BizurBuilder withMemberId(String memberId) {
        config.setRoleId(memberId);
        return this;
    }

    public BizurBuilder withAddress(Address address) {
        config.setAddress(address);
        return this;
    }

    public BizurBuilder withMulticastAddress(MulticastAddress multicastAddress) {
        config.setMulticastAddress(multicastAddress);
        return this;
    }

    public BizurBuilder withNumBuckets(int bucketCount) {
        config.setNumBuckets(bucketCount);
        return this;
    }

    public BizurBuilder withMemberAddresses(Set<Address> addresses) {
        config.setMemberAddresses(addresses);
        return this;
    }

    protected BizurSettings getConfig() {
        return this.config;
    }

    public BizurNode build() throws InterruptedException {
        BizurNode bizurNode = new BizurNode(config);
        config.registerRoleRef(bizurNode);
        return bizurNode;
    }

    public BizurClient buildClient() throws InterruptedException {
        BizurClient bizurClient = new BizurClient(config);
        config.registerRoleRef(bizurClient);
        return bizurClient;
    }

}
