package network.address;

import org.pmw.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.concurrent.TimeUnit;

/**
 * Defines a TCP address
 */
public class TCPAddress extends Address {

    /**
     * Ip address
     */
    private InetAddress ip;
    /**
     * Port number
     */
    private int portNumber;

    /**
     * @param ipStr      ip address as string
     * @param portNumber port number
     * @throws UnknownHostException if the <tt>ipStr</tt> could not be parsed to {@link InetAddress}.
     */
    public TCPAddress(String ipStr, int portNumber) throws UnknownHostException {
        this(InetAddress.getByName(ipStr), portNumber);
    }

    /**
     * @param ip         ip address
     * @param portNumber port number. If set to 0, then {@link network.messenger.MessageReceiver} will find an
     *                   available port automatically. Otherwise, the port specified will be used.
     */
    public TCPAddress(InetAddress ip, int portNumber) {
        this.ip = ip;
        this.portNumber = portNumber;
    }

    public TCPAddress() {
    }

    /**
     * @param ip sets {@link #ip}
     * @return this
     */
    public TCPAddress setIp(InetAddress ip) {
        this.ip = ip;
        return this;
    }

    /**
     * @param portNumber sets {@link #portNumber}
     * @return this
     */
    public TCPAddress setPortNumber(int portNumber) {
        this.portNumber = portNumber;
        return this;
    }

    /**
     * @return gets {@link #ip}
     */
    public InetAddress getIp() {
        return ip;
    }

    /**
     * @return gets {@link #portNumber}
     */
    public int getPortNumber() {
        return portNumber;
    }

    @Override
    public String resolveAddressId() {
        return getIp() + "_" + getPortNumber();
    }

    /**
     * Used for dynamic ip address resolution.
     * @return resolved ip address of the process.
     */
    public static InetAddress resolveIpAddress(){
        for (int i = 0; i < 5; i++) {
            try(final DatagramSocket socket = new DatagramSocket()){
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                String ipStr = socket.getLocalAddress().getHostAddress();
                if(ipStr != null){
                    return InetAddress.getByName(ipStr);
                }
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (SocketException | UnknownHostException | InterruptedException e) {
                Logger.error(e);
            }
        }
        /* if above solution does not work, use ifconfig.co */
        try {
            URL url = new URL("http://ifconfig.co");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
                for (String line; (line = reader.readLine()) != null;) {
                    if(line.contains("class=\"ip\"")){
                        String ipStr = line.substring(line.indexOf("ip") + 4, line.indexOf("</"));
                        return InetAddress.getByName(ipStr);
                    }
                }
            } catch (IOException e) {
                Logger.error(e);
            }
        } catch (MalformedURLException e) {
            Logger.error(e);
        }
        try {
            Logger.warn("Could not resolve ip address, using localhost: 127.0.0.1");
            return InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            Logger.debug(e);
        }
        return null;
    }

    @Override
    public boolean isSame(Address other) {
        TCPAddress address = (TCPAddress) other;
        return address.getIp().toString().equals(this.getIp().toString())
                && address.getPortNumber() == this.getPortNumber();
    }

    @Override
    public String toString() {
        return "TCPAddress{" +
                "ip=" + ip +
                ", portNumber=" + portNumber +
                '}';
    }
}
