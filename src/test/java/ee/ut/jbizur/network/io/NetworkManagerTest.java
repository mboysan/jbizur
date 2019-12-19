package ee.ut.jbizur.network.io;

import ee.ut.jbizur.config.Conf;
import ee.ut.jbizur.network.address.Address;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import utils.MockUtils;

public class NetworkManagerTest {
    static {
        Conf.setConfigFromResources("jbizur_func_test.conf");
    }

    private Address serverAddr1;
    private Address serverAddr2;

    private NetworkManager nm;

    @Before
    public void setUp() throws Exception {
        serverAddr1 = MockUtils.mockAddress("m1");
        serverAddr2 = MockUtils.mockAddress("m2");

        nm = Mockito.spy(new NetworkManager("nm1", serverAddr1, (ic) -> {}, (nc) -> {}));
        nm.start();
        System.out.println();

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void test() {

    }
}