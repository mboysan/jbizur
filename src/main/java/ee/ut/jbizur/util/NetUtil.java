package ee.ut.jbizur.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

public class NetUtil {

    private static final Logger logger = LoggerFactory.getLogger(NetUtil.class);

    public static int findOpenPort() throws IOException {
        try (ServerSocket ss = createServerSocket(0)) {
            return ss.getLocalPort();
        }
    }

    private static ServerSocket createServerSocket(int port) {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            logger.warn("Could not create ServerSocket on port={}, retrying with port=0... [{}]", port, e.getMessage());
            serverSocket = createServerSocket(0);
        }
        return serverSocket;
    }

}
