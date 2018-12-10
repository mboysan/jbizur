package ee.ut.jbizur.protocol.commands;

/**
 * Enum to define the tag of the message to _send/recv.
 */
public enum MessageTag {
    /**
     * Any tag to _send/recv.
     */
    ANY_TAG(0);

    /**
     * Tag of the message to _send/recv.
     */
    private final int tag;

    /**
     * @param tag sets {@link #tag}
     */
    MessageTag(int tag) {
        this.tag = tag;
    }

    /**
     * @return the value of the tag.
     */
    public int getTagValue() {
        return tag;
    }
}
