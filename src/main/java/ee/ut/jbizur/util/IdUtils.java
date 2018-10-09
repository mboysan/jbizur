package ee.ut.jbizur.util;

import ee.ut.jbizur.network.address.Address;
import org.pmw.tinylog.Logger;

import java.util.*;

public final class IdUtils {
    private static final long seed = System.currentTimeMillis();
    private static final Random RANDOM = new Random(seed);
    static {
        Logger.info("seed: " + seed);
    }

    public static int generateId() {
        return RANDOM.nextInt(Integer.MAX_VALUE);
    }

    public static int hashKey(String s, int size) {
        int R = 31;
        int hash = 0;
        for (int i = 0; i < s.length(); i++)
            hash = (R * hash + s.charAt(i)) % size;
        return hash;
    }

    public static Address nextAddressInUnorderedSet(Set<Address> addressSet, int bucketIndex) {
        List<Address> orderedAddressList = orderAddresses(addressSet);
        return orderedAddressList.get(bucketIndex % addressSet.size());
    }

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
