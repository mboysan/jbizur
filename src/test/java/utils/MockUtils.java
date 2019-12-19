package utils;

import ee.ut.jbizur.network.address.Address;
import ee.ut.jbizur.network.io.AbstractServer;
import org.mockito.Mockito;

public class MockUtils {

    public static Address mockAddress(String id) {
        return new Address() {
            @Override
            public String resolveAddressId() {
                return id;
            }
        };
    }

    public static AbstractServer server(String name, Address address) {
        return Mockito.spy(new AbstractServer(name, address) {});
    }

}
