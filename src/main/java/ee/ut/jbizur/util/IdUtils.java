package ee.ut.jbizur.util;

import ee.ut.jbizur.network.address.Address;

import java.util.*;

public final class IdUtils {

    public static Queue<Address> getAddressQueue(Set<Address> addressSet) {
        List<Address> addressList = orderAddresses(addressSet);
        return new ArrayDeque<>(addressList);
    }

    public static List<Address> orderAddresses(Set<Address> addressSet) {
        List<Address> addressList = new ArrayList<>(addressSet);
        addressList.sort((a1, a2) -> {
            int id1 = a1.resolveAddressId().hashCode();
            int id2 = a2.resolveAddressId().hashCode();
            return Integer.compare(id1, id2);
        });
        return addressList;
    }
}
