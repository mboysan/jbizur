package protocol.commands.bizur;

import datastore.bizur.Bucket;
import protocol.commands.NetworkCommand;

public class AckRead_NC extends NetworkCommand {
    private Bucket bucket;

    public Bucket getBucket() {
        return bucket;
    }

    public AckRead_NC setBucket(Bucket bucket) {
        this.bucket = bucket;
        return this;
    }

    @Override
    public String toString() {
        return "AckRead_NC{" +
                "bucket=" + bucket +
                "} " + super.toString();
    }
}
