package ee.ut.jbizur.common.util;

import ee.ut.jbizur.protocol.address.Address;

import java.io.Serializable;
import java.util.*;

public final class IdUtil {

    public static int generateId() {
        return RngUtil.nextInt(Integer.MAX_VALUE);
    }

    /**
     * Taken from <a href="https://algs4.cs.princeton.edu/34hash/">34hash site</a>.
     * @param s key to hash.
     * @return index of the bucket.
     */
    public static int hashKey(String s, int size) {
        int R = 31;
        int hash = 0;
        for (int i = 0; i < s.length(); i++)
            hash = (R * hash + s.charAt(i)) % size;
        return hash;
    }

    public static int hashKey(Serializable s, int size) {
        return s.hashCode() % size;
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
