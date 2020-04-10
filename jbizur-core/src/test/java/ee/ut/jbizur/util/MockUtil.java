package ee.ut.jbizur.util;

import ee.ut.jbizur.protocol.address.Address;

public class MockUtil {

    public static Address mockAddress(String id) {
        return new Address() {
            @Override
            public String resolveAddressId() {
                return id;
            }

            @Override
            public String toString() {
                return "MockAddress{id=" + id + "}";
            }
        };
    }

}
