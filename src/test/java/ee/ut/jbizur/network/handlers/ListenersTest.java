package ee.ut.jbizur.network.handlers;

import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.protocol.commands.nc.common.Ack_NC;
import ee.ut.jbizur.protocol.commands.nc.common.Nack_NC;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import utils.MultiThreadExecutor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class ListenersTest {

    private Listeners listeners;

    @Before
    public void setUp() {
        listeners = new Listeners();
    }

    @Test
    public void testHandleAndRemove() {
        listeners.add(0, (cmd) -> false);   // non-removable
        listeners.add(1, (cmd) -> true);    // removable
        listeners.add(2, (cmd) -> true);    // removable

        Assert.assertEquals(3, listeners.getListeners().size());

        for (int i = 0; i < 10; i++) {
            listeners.handle(new NetworkCommand().setCorrelationId(0));
            Assert.assertEquals(3, listeners.getListeners().size());    // listener must not be removed
        }

        listeners.handle(new NetworkCommand().setCorrelationId(1));
        Assert.assertEquals(2, listeners.getListeners().size());    // listener must be removed

        listeners.handle(new NetworkCommand().setCorrelationId(2));
        Assert.assertEquals(1, listeners.getListeners().size());    // listener must be removed

        try {
            listeners.handle(new NetworkCommand().setCorrelationId(2));
        } catch (Exception e) {
            fail("consecutive calls on the removed listener must not raise exceptions: " + e);
        }
    }

    @Test
    public void testBaseListener() {
        AtomicInteger handleCount = new AtomicInteger(0);
        Consumer<NetworkCommand> consumer = (c) -> {
            handleCount.incrementAndGet();
        };
        listeners.add(0, new BaseListener(consumer));

        listeners.handle(new NetworkCommand().setCorrelationId(0));
        listeners.handle(new NetworkCommand().setCorrelationId(0));
        listeners.handle(new NetworkCommand().setCorrelationId(0));

        Assert.assertEquals(3, handleCount.get());
        Assert.assertEquals(1, listeners.getListeners().size());    // base listener still exists
        Assert.assertTrue(listeners.getListeners().get(0) instanceof BaseListener);
    }

    @Test
    public void testCallbackListener() {
        listeners.add(0, new BaseListener((c) -> {}));  // always add the base listener

        AtomicInteger handleCount = new AtomicInteger(0);
        Consumer<NetworkCommand> consumer = (c) -> {
            handleCount.incrementAndGet();
        };
        CallbackListener cl = new CallbackListener(consumer);
        listeners.add(1, cl);

        listeners.handle(new NetworkCommand().setCorrelationId(1)); // listener removed here
        listeners.handle(new NetworkCommand().setCorrelationId(1));
        listeners.handle(new NetworkCommand().setCorrelationId(1));

        Assert.assertEquals(1, handleCount.get());
        Assert.assertEquals(1, listeners.getListeners().size());    // base listener still exists
        Assert.assertTrue(listeners.getListeners().get(0) instanceof BaseListener);
    }

    @Test
    public void testQuorumListenerOnAcks() throws ExecutionException, InterruptedException {
        listeners.add(0, new BaseListener((c) -> {}));  // always add the base listener

        int totalSize = 3;
        int quorumSize = 2;

        AtomicInteger counter = new AtomicInteger(0);
        QuorumListener qc = addQuorumListener(totalSize, quorumSize, counter);
        MultiThreadExecutor mte = new MultiThreadExecutor();
        for (int i = 0; i < totalSize; i++) {
            Supplier<NetworkCommand> s = () -> new Ack_NC().setCorrelationId(1);
            mte.execute(() -> listeners.handle(s.get()));
        }
        Assert.assertTrue(qc.await());
        Assert.assertTrue(qc.isMajorityAcked());
        Assert.assertTrue("counter=" + counter.get(), counter.get() >= quorumSize);

        /* we wait for commands to be handled by listeners.
           Otherwise we cannot know if listeners removed the QuorumListener or not. */
        mte.endExecution();

        Assert.assertEquals(1, listeners.getListeners().size());    // base listener still exists
        Assert.assertTrue(listeners.getListeners().get(0) instanceof BaseListener);
    }

    @Test
    public void testQuorumListenerOnNAcks() throws ExecutionException, InterruptedException {
        listeners.add(0, new BaseListener((c) -> {}));  // always add the base listener

        int totalSize = 3;
        int quorumSize = 2;
        AtomicInteger counter = new AtomicInteger(0);
        QuorumListener qc = addQuorumListener(totalSize, quorumSize, counter);
        MultiThreadExecutor mte = new MultiThreadExecutor();
        for (int i = 0; i < totalSize; i++) {
            Supplier<NetworkCommand> s = () -> new Nack_NC().setCorrelationId(1);
            mte.execute(() -> listeners.handle(s.get()));
        }
        Assert.assertTrue(qc.await());
        Assert.assertFalse(qc.isMajorityAcked());
        Assert.assertTrue("counter=" + counter.get(), counter.get() >= quorumSize);

        /* we wait for commands to be handled by listeners.
           Otherwise we cannot know if listeners removed the QuorumListener or not. */
        mte.endExecution();

        Assert.assertEquals(1, listeners.getListeners().size());    // base listener still exists
        Assert.assertTrue(listeners.getListeners().get(0) instanceof BaseListener);
    }

    @Test
    public void testQuorumListenerOnAckAndNacksTogether() throws ExecutionException, InterruptedException {
        listeners.add(0, new BaseListener((c) -> {}));  // always add the base listener

        int totalSize = 3;
        int quorumSize = 2;
        AtomicInteger counter = new AtomicInteger(0);
        QuorumListener qc = addQuorumListener(totalSize, quorumSize, counter);
        MultiThreadExecutor mte = new MultiThreadExecutor();
        mte.execute(() -> listeners.handle(new Ack_NC().setCorrelationId(1)));
        mte.execute(() -> listeners.handle(new Nack_NC().setCorrelationId(1)));
        mte.execute(() -> listeners.handle(new Ack_NC().setCorrelationId(1)));
        Assert.assertTrue(qc.await());
        Assert.assertTrue(qc.isMajorityAcked());
        Assert.assertTrue("counter=" + counter.get(), counter.get() >= quorumSize);

        /* we wait for commands to be handled by listeners.
           Otherwise we cannot know if listeners removed the QuorumListener or not. */
        mte.endExecution();

        Assert.assertEquals(1, listeners.getListeners().size());    // base listener still exists
        Assert.assertTrue(listeners.getListeners().get(0) instanceof BaseListener);
    }

    private QuorumListener addQuorumListener(int totalSize, int quorumSize, AtomicInteger counter) {
        QuorumListener qc = new QuorumListener(totalSize, quorumSize, cmd -> {
            counter.incrementAndGet();
            return cmd instanceof Ack_NC;
        });
        listeners.add(1, qc);
        return qc;
    }
}