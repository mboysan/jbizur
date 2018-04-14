package protocol.commands;

public class SequenceNumber {

    private long seqId;
    private String roleId;

    public SequenceNumber() {
    }

    public SequenceNumber(long seqId, String roleId) {
        this.seqId = seqId;
        this.roleId = roleId;
    }

    public long getSeqId() {
        return seqId;
    }

    public SequenceNumber setSeqId(long seqId) {
        this.seqId = seqId;
        return this;
    }

    public String getRoleId() {
        return roleId;
    }

    public SequenceNumber setRoleId(String roleId) {
        this.roleId = roleId;
        return this;
    }

    @Override
    public String toString() {
        return "SequenceNumber{" +
                "seqId=" + seqId +
                ", roleId='" + roleId + '\'' +
                '}';
    }
}
