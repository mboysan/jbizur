package ee.ut.jbizur.common.config;

public class JbizurConfig {
  public final java.util.List<JbizurConfig.Clients$Elm> clients;
  public final JbizurConfig.Consensus consensus;
  public final JbizurConfig.Logging logging;
  public final java.util.List<JbizurConfig.Members$Elm> members;
  public final JbizurConfig.Network network;
  public final JbizurConfig.Node node;
  public static class Clients$Elm {
    public final java.lang.String id;
    public final boolean instance;
    public final java.lang.String tcpAddress;
    
    public Clients$Elm(com.typesafe.config.Config c, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
      this.id = c.hasPathOrNull("id") ? c.getString("id") : "client";
      this.instance = c.hasPathOrNull("instance") && c.getBoolean("instance");
      this.tcpAddress = c.hasPathOrNull("tcpAddress") ? c.getString("tcpAddress") : "127.0.0.1:0";
    }
  }
  
  public static class Consensus {
    public final Consensus.Bizur bizur;
    public static class Bizur {
      public final int bucketCount;
      public final int bucketElectRetryCount;
      public final long maxElectionWaitSec;
      
      public Bizur(com.typesafe.config.Config c, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
        this.bucketCount = c.hasPathOrNull("bucketCount") ? c.getInt("bucketCount") : 5;
        this.bucketElectRetryCount = c.hasPathOrNull("bucketElectRetryCount") ? c.getInt("bucketElectRetryCount") : 5;
        this.maxElectionWaitSec = c.hasPathOrNull("maxElectionWaitSec") ? c.getLong("maxElectionWaitSec") : 5;
      }
    }
    
    public Consensus(com.typesafe.config.Config c, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
      this.bizur = c.hasPathOrNull("bizur") ? new Consensus.Bizur(c.getConfig("bizur"), parentPath + "bizur.", $tsCfgValidator) : new Consensus.Bizur(com.typesafe.config.ConfigFactory.parseString("bizur{}"), parentPath + "bizur.", $tsCfgValidator);
    }
  }
  
  public static class Logging {
    public final java.lang.String level;
    public final java.lang.String pattern;
    public final boolean writeToConsole;
    public final java.lang.String writeToFile;
    
    public Logging(com.typesafe.config.Config c, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
      this.level = c.hasPathOrNull("level") ? c.getString("level") : "INFO";
      this.pattern = c.hasPathOrNull("pattern") ? c.getString("pattern") : "[{level}] {date:HH:mm:ss:SSS} {class}.{method}(): {message}";
      this.writeToConsole = !c.hasPathOrNull("writeToConsole") || c.getBoolean("writeToConsole");
      this.writeToFile = c.hasPathOrNull("writeToFile") ? c.getString("writeToFile") : null;
    }
  }
  
  public static class Members$Elm {
    public final java.lang.String id;
    public final boolean instance;
    public final java.lang.String tcpAddress;
    
    public Members$Elm(com.typesafe.config.Config c, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
      this.id = c.hasPathOrNull("id") ? c.getString("id") : "member";
      this.instance = c.hasPathOrNull("instance") && c.getBoolean("instance");
      this.tcpAddress = c.hasPathOrNull("tcpAddress") ? c.getString("tcpAddress") : "127.0.0.1:0";
    }
  }
  
  public static class Network {
    public final boolean bufferedIO;
    public final java.lang.String client;
    public final Network.Multicast multicast;
    public final long responseTimeoutSec;
    public final int sendFailRetryCount;
    public final java.lang.String sendRecvAs;
    public final java.lang.String serializer;
    public final java.lang.String server;
    public final long shutdownWaitSec;
    public final Network.Tcp tcp;
    public static class Multicast {
      public final java.lang.String address;
      public final boolean enabled;
      public final int intervalms;
      
      public Multicast(com.typesafe.config.Config c, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
        this.address = c.hasPathOrNull("address") ? c.getString("address") : "230.0.0.1:54321";
        this.enabled = !c.hasPathOrNull("enabled") || c.getBoolean("enabled");
        this.intervalms = c.hasPathOrNull("intervalms") ? c.getInt("intervalms") : 1000;
      }
    }
    
    public static class Tcp {
      public final java.lang.String defaultAddress;
      public final boolean keepalive;
      
      public Tcp(com.typesafe.config.Config c, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
        this.defaultAddress = c.hasPathOrNull("defaultAddress") ? c.getString("defaultAddress") : "127.0.0.1:0";
        this.keepalive = !c.hasPathOrNull("keepalive") || c.getBoolean("keepalive");
      }
    }
    
    public Network(com.typesafe.config.Config c, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
      this.bufferedIO = c.hasPathOrNull("bufferedIO") && c.getBoolean("bufferedIO");
      this.client = c.hasPathOrNull("client") ? c.getString("client") : "ee.ut.jbizur.network.io.tcp.custom.BlockingClientImpl";
      this.multicast = c.hasPathOrNull("multicast") ? new Network.Multicast(c.getConfig("multicast"), parentPath + "multicast.", $tsCfgValidator) : new Network.Multicast(com.typesafe.config.ConfigFactory.parseString("multicast{}"), parentPath + "multicast.", $tsCfgValidator);
      this.responseTimeoutSec = c.hasPathOrNull("responseTimeoutSec") ? c.getLong("responseTimeoutSec") : 10;
      this.sendFailRetryCount = c.hasPathOrNull("sendFailRetryCount") ? c.getInt("sendFailRetryCount") : 0;
      this.sendRecvAs = c.hasPathOrNull("sendRecvAs") ? c.getString("sendRecvAs") : "OBJECT";
      this.serializer = c.hasPathOrNull("serializer") ? c.getString("serializer") : "ee.ut.jbizur.common.protocol.ByteSerializer";
      this.server = c.hasPathOrNull("server") ? c.getString("server") : "ee.ut.jbizur.network.io.tcp.custom.BlockingServerImpl";
      this.shutdownWaitSec = c.hasPathOrNull("shutdownWaitSec") ? c.getLong("shutdownWaitSec") : 20;
      this.tcp = c.hasPathOrNull("tcp") ? new Network.Tcp(c.getConfig("tcp"), parentPath + "tcp.", $tsCfgValidator) : new Network.Tcp(com.typesafe.config.ConfigFactory.parseString("tcp{}"), parentPath + "tcp.", $tsCfgValidator);
    }
  }
  
  public static class Node {
    public final Node.Client client;
    public final Node.Member member;
    public static class Client {
      public final int expectedCount;
      public final java.lang.String idFormat;
      
      public Client(com.typesafe.config.Config c, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
        this.expectedCount = c.hasPathOrNull("expectedCount") ? c.getInt("expectedCount") : 0;
        this.idFormat = c.hasPathOrNull("idFormat") ? c.getString("idFormat") : "member%d";
      }
    }
    
    public static class Member {
      public final int expectedCount;
      public final java.lang.String idFormat;
      
      public Member(com.typesafe.config.Config c, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
        this.expectedCount = c.hasPathOrNull("expectedCount") ? c.getInt("expectedCount") : 0;
        this.idFormat = c.hasPathOrNull("idFormat") ? c.getString("idFormat") : "member%d";
      }
    }
    
    public Node(com.typesafe.config.Config c, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
      this.client = c.hasPathOrNull("client") ? new Node.Client(c.getConfig("client"), parentPath + "client.", $tsCfgValidator) : new Node.Client(com.typesafe.config.ConfigFactory.parseString("client{}"), parentPath + "client.", $tsCfgValidator);
      this.member = c.hasPathOrNull("member") ? new Node.Member(c.getConfig("member"), parentPath + "member.", $tsCfgValidator) : new Node.Member(com.typesafe.config.ConfigFactory.parseString("member{}"), parentPath + "member.", $tsCfgValidator);
    }
  }
  
  public JbizurConfig(com.typesafe.config.Config c) {
    final $TsCfgValidator $tsCfgValidator = new $TsCfgValidator();
    final java.lang.String parentPath = "";
    this.clients = c.hasPathOrNull("clients") ? $_LJbizurConfig_Clients$Elm(c.getList("clients"), parentPath, $tsCfgValidator) : null;
    this.consensus = c.hasPathOrNull("consensus") ? new JbizurConfig.Consensus(c.getConfig("consensus"), parentPath + "consensus.", $tsCfgValidator) : new JbizurConfig.Consensus(com.typesafe.config.ConfigFactory.parseString("consensus{}"), parentPath + "consensus.", $tsCfgValidator);
    this.logging = c.hasPathOrNull("logging") ? new JbizurConfig.Logging(c.getConfig("logging"), parentPath + "logging.", $tsCfgValidator) : new JbizurConfig.Logging(com.typesafe.config.ConfigFactory.parseString("logging{}"), parentPath + "logging.", $tsCfgValidator);
    this.members = c.hasPathOrNull("members") ? $_LJbizurConfig_Members$Elm(c.getList("members"), parentPath, $tsCfgValidator) : null;
    this.network = c.hasPathOrNull("network") ? new JbizurConfig.Network(c.getConfig("network"), parentPath + "network.", $tsCfgValidator) : new JbizurConfig.Network(com.typesafe.config.ConfigFactory.parseString("network{}"), parentPath + "network.", $tsCfgValidator);
    this.node = c.hasPathOrNull("node") ? new JbizurConfig.Node(c.getConfig("node"), parentPath + "node.", $tsCfgValidator) : new JbizurConfig.Node(com.typesafe.config.ConfigFactory.parseString("node{}"), parentPath + "node.", $tsCfgValidator);
    $tsCfgValidator.validate();
  }
  private static java.util.List<JbizurConfig.Clients$Elm> $_LJbizurConfig_Clients$Elm(com.typesafe.config.ConfigList cl, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
    java.util.ArrayList<JbizurConfig.Clients$Elm> al = new java.util.ArrayList<>();
    for (com.typesafe.config.ConfigValue cv: cl) {
      al.add(new JbizurConfig.Clients$Elm(((com.typesafe.config.ConfigObject)cv).toConfig(), parentPath, $tsCfgValidator));
    }
    return java.util.Collections.unmodifiableList(al);
  }
  private static java.util.List<JbizurConfig.Members$Elm> $_LJbizurConfig_Members$Elm(com.typesafe.config.ConfigList cl, java.lang.String parentPath, $TsCfgValidator $tsCfgValidator) {
    java.util.ArrayList<JbizurConfig.Members$Elm> al = new java.util.ArrayList<>();
    for (com.typesafe.config.ConfigValue cv: cl) {
      al.add(new JbizurConfig.Members$Elm(((com.typesafe.config.ConfigObject)cv).toConfig(), parentPath, $tsCfgValidator));
    }
    return java.util.Collections.unmodifiableList(al);
  }
  private static final class $TsCfgValidator  {
    private final java.util.List<java.lang.String> badPaths = new java.util.ArrayList<>();
    
    void addBadPath(java.lang.String path, com.typesafe.config.ConfigException e) {
      badPaths.add("'" + path + "': " + e.getClass().getName() + "(" + e.getMessage() + ")");
    }
    
    void validate() {
      if (!badPaths.isEmpty()) {
        java.lang.StringBuilder sb = new java.lang.StringBuilder("Invalid configuration:");
        for (java.lang.String path : badPaths) {
          sb.append("\n    ").append(path);
        }
        throw new com.typesafe.config.ConfigException(sb.toString()) {};
      }
    }
  }
}