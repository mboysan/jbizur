package ee.ut.jbizur.role;

import ee.ut.jbizur.config.BizurConf;
import ee.ut.jbizur.config.CoreConf;
import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.address.MulticastAddress;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class BizurBuilder {

    private final BizurSettings settings;

    protected BizurBuilder(){
        settings = new BizurSettings();
    }

    public static BizurBuilder builder() {
        return new BizurBuilder();
    }

    public BizurBuilder withMemberId(String memberId) {
        settings.setRoleId(memberId);
        return this;
    }

    public BizurBuilder withAddress(Address address) {
        settings.setAddress(address);
        return this;
    }

    public BizurBuilder withMulticastEnabled(boolean isEnabled) {
        settings.setMultiCastEnabled(isEnabled);
        return this;
    }

    public BizurBuilder withMulticastAddress(MulticastAddress multicastAddress) {
        settings.setMulticastAddress(multicastAddress);
        return this;
    }

    public BizurBuilder withNumBuckets(int bucketCount) {
        settings.setNumBuckets(bucketCount);
        return this;
    }

    public BizurBuilder withMemberAddresses(Set<Address> addresses) {
        settings.setMemberAddresses(addresses);
        return this;
    }

    public BizurBuilder withConfiguration(File workingDirFile) {
        BizurConf.set(workingDirFile);
        settings.defaults();
        return this;
    }

    public BizurNode build() throws IOException {
        return new BizurNode(settings);
    }

    public BizurClient buildClient() throws IOException {
        return new BizurClient(settings);
    }

}
