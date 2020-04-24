package ee.ut.jbizur.util;

import ee.ut.jbizur.protocol.address.Address;
import ee.ut.jbizur.protocol.address.TCPAddress;

import java.util.Objects;

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

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Address that = (Address) o;
                return Objects.equals(id, that.resolveAddressId());
            }

            @Override
            public int hashCode() {
                return Objects.hash(id);
            }
        };
    }

}
