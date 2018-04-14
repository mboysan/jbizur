package network.address;

/**
 * Defines an MPI protocol host address
 */
public class MPIAddress extends Address {

    /**
     * Rank of the process
     */
    private int rank;

    /**
     * MPI groupId to work with. Used for message tags.
     */
    private int groupId;

    /**
     * @param rank rank of the process
     */
    public MPIAddress(int rank, int groupId) {
        this.rank = rank;
        this.groupId = groupId;
    }

    public MPIAddress(){

    }

    /**
     * @param rank sets {@link #rank}
     * @return this
     */
    public MPIAddress setRank(int rank) {
        this.rank = rank;
        return this;
    }

    /**
     * @return gets {@link #rank}
     */
    public int getRank() {
        return rank;
    }

    public int getGroupId() {
        return groupId;
    }

    public MPIAddress setGroupId(int groupId) {
        this.groupId = groupId;
        return this;
    }

    @Override
    public String resolveAddressId() {
        return "p" + rank + "g" + groupId;
    }

    @Override
    public boolean isSame(Address other) {
        MPIAddress addr = (MPIAddress) other;
        return addr.getRank() == this.getRank() && addr.getGroupId() == this.groupId;
    }

    @Override
    public String toString() {
        return "MPIAddress{" +
                "rank=" + rank +
                ", groupId=" + groupId +
                '}';
    }
}
