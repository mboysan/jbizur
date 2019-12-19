package ee.ut.jbizur.integration;

import ee.ut.jbizur.common.ResourceCloser;
import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.TCPAddress;
import ee.ut.jbizur.network.io.NetworkManager;
import ee.ut.jbizur.protocol.commands.ic.InternalCommand;
import ee.ut.jbizur.protocol.commands.nc.NetworkCommand;
import ee.ut.jbizur.util.NetUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.MultiThreadExecutor;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Supplier;

@RunWith(Parameterized.class)
public class NetworkManagerIT implements ResourceCloser {

    @Parameterized.Parameters(name = "conf={0}")
    public static Object[][] conf() {
        return new Object[][]{
                {"NMIT.buffered-custom.conf"},
                {"NMIT.buffered-rapidoid.conf"},
                {"NMIT.custom.conf"},
                {"NMIT.rapidoid.conf"},
        };
    }

    @Parameterized.Parameter
    public String confName;

    private static final Logger logger = LoggerFactory.getLogger(NetworkManagerIT.class);

    private NMWrapper nmW1;
    private NMWrapper nmW2;
    private NMWrapper nmW3;

    @Before
    public void setUp() throws Exception {
        // set the configuration
        Conf.setConfigFromResources(confName);

        nmW1 = new NMWrapper("nm1").start();
        nmW2 = new NMWrapper("nm2").start();
        nmW3 = new NMWrapper("nm3").start();
    }

    @After
    public void tearDown() throws Exception {
        closeResources(nmW1.nm, nmW2.nm, nmW3.nm);

        Assert.assertEquals(0, nmW1.ncq.size());
        Assert.assertEquals(0, nmW2.ncq.size());
        Assert.assertEquals(0, nmW3.ncq.size());
    }

    @Test
    public void testSendSimpleP2P() throws InterruptedException {
        NetworkCommand ncSend = new NetworkCommand()
                .setMsgId(1)
                .setReceiverAddress(nmW2.addr);
        nmW1.nm.send(ncSend);

        NetworkCommand ncRecv = nmW2.ncq.take();
        Assert.assertEquals(ncSend.getMsgId(), ncRecv.getMsgId());
    }

    @Test
    public void testMultiSend() throws InterruptedException {
        TCPAddress recvAddr = nmW3.addr;

        int totalSend = 500;
        for (int i = 0; i < totalSend; i++) {
            Supplier<NetworkCommand> ncSupplier = () -> new NetworkCommand().setReceiverAddress(recvAddr);
            nmW1.nm.send(ncSupplier.get());
            nmW2.nm.send(ncSupplier.get());
        }

        for (int i = 0; i < totalSend * 2; i++) {
            nmW3.ncq.take();
        }
    }

    @Test
    public void testMultiSendConcurrent() throws InterruptedException, ExecutionException {
        TCPAddress recvAddr = nmW3.addr;

        MultiThreadExecutor mte = new MultiThreadExecutor();
        int totalSend = 500;
        for (int i = 0; i < totalSend; i++) {
            Supplier<NetworkCommand> ncSupplier = () -> new NetworkCommand().setReceiverAddress(recvAddr);
            mte.execute(() -> nmW1.nm.send(ncSupplier.get()));
            mte.execute(() -> nmW2.nm.send(ncSupplier.get()));
        }
        mte.endExecution();

        for (int i = 0; i < totalSend * 2; i++) {
            nmW3.ncq.take();
        }
    }

    private static class NMWrapper {
        final NetworkManager nm;

        final TCPAddress addr;
        final LinkedBlockingDeque<NetworkCommand> ncq = new LinkedBlockingDeque<>();
        final LinkedBlockingDeque<InternalCommand> icq = new LinkedBlockingDeque<>();

        NMWrapper(String name) throws IOException {
            addr = new TCPAddress("localhost", NetUtil.findOpenPort());
            this.nm = new NetworkManager(name, addr, (ic) -> icq.offer(ic), (nc) -> ncq.offer(nc));
        }

        private NMWrapper start() throws UnknownHostException {
            nm.start();
            return this;
        }
    }

}