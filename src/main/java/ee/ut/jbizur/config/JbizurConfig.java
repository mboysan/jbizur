package ee.ut.jbizur.config;

public class JbizurConfig {
  public final java.util.List<JbizurConfig.Clients$Elm> clients;
  public final JbizurConfig.Consensus consensus;
  public final JbizurConfig.Logging logging;
  public final java.util.List<JbizurConfig.Members$Elm> members;
  public final JbizurConfig.Network network;
  public final JbizurConfig.Node node;
  public final JbizurConfig.Tests tests;
  public static class Clients$Elm {
    public final java.lang.String id;
    public final boolean instance;
    public final java.lang.String tcpAddress;
    
    public Clients$Elm(com.typesafe.config.Config c) {
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
      
      public Bizur(com.typesafe.config.Config c) {
        this.bucketCount = c.hasPathOrNull("bucketCount") ? c.getInt("bucketCount") : 5;
        this.bucketElectRetryCount = c.hasPathOrNull("bucketElectRetryCount") ? c.getInt("bucketElectRetryCount") : 5;
        this.maxElectionWaitSec = c.hasPathOrNull("maxElectionWaitSec") ? c.getLong("maxElectionWaitSec") : 5;
      }
    }
    
    public Consensus(com.typesafe.config.Config c) {
      this.bizur = c.hasPathOrNull("bizur") ? new Consensus.Bizur(c.getConfig("bizur")) : new Consensus.Bizur(com.typesafe.config.ConfigFactory.parseString("bizur{}"));
    }
  }
  
  public static class Logging {
    public final java.lang.String file;
    public final java.lang.String level;
    public final java.lang.String pattern;
    
    public Logging(com.typesafe.config.Config c) {
      this.file = c.hasPathOrNull("file") ? c.getString("file") : null;
      this.level = c.hasPathOrNull("level") ? c.getString("level") : "INFO";
      this.pattern = c.hasPathOrNull("pattern") ? c.getString("pattern") : "[{level}] {date:HH:mm:ss:SSS} {class}.{method}(): {message}";
    }
  }
  
  public static class Members$Elm {
    public final java.lang.String id;
    public final boolean instance;
    public final java.lang.String tcpAddress;
    
    public Members$Elm(com.typesafe.config.Config c) {
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
      
      public Multicast(com.typesafe.config.Config c) {
        this.address = c.hasPathOrNull("address") ? c.getString("address") : "230.0.0.1:54321";
        this.enabled = !c.hasPathOrNull("enabled") || c.getBoolean("enabled");
        this.intervalms = c.hasPathOrNull("intervalms") ? c.getInt("intervalms") : 1000;
      }
    }
    
    public static class Tcp {
      public final java.lang.String defaultAddress;
      public final boolean keepalive;
      
      public Tcp(com.typesafe.config.Config c) {
        this.defaultAddress = c.hasPathOrNull("defaultAddress") ? c.getString("defaultAddress") : "127.0.0.1:0";
        this.keepalive = !c.hasPathOrNull("keepalive") || c.getBoolean("keepalive");
      }
    }
    
    public Network(com.typesafe.config.Config c) {
      this.bufferedIO = c.hasPathOrNull("bufferedIO") && c.getBoolean("bufferedIO");
      this.client = c.hasPathOrNull("client") ? c.getString("client") : "ee.ut.jbizur.network.messenger.tcp.custom.BlockingClientImpl";
      this.multicast = c.hasPathOrNull("multicast") ? new Network.Multicast(c.getConfig("multicast")) : new Network.Multicast(com.typesafe.config.ConfigFactory.parseString("multicast{}"));
      this.responseTimeoutSec = c.hasPathOrNull("responseTimeoutSec") ? c.getLong("responseTimeoutSec") : 10;
      this.sendFailRetryCount = c.hasPathOrNull("sendFailRetryCount") ? c.getInt("sendFailRetryCount") : 0;
      this.sendRecvAs = c.hasPathOrNull("sendRecvAs") ? c.getString("sendRecvAs") : "OBJECT";
      this.serializer = c.hasPathOrNull("serializer") ? c.getString("serializer") : "ee.ut.jbizur.protocol.ByteSerializer";
      this.server = c.hasPathOrNull("server") ? c.getString("server") : "ee.ut.jbizur.network.messenger.tcp.custom.BlockingServerImpl";
      this.shutdownWaitSec = c.hasPathOrNull("shutdownWaitSec") ? c.getLong("shutdownWaitSec") : 300;
      this.tcp = c.hasPathOrNull("tcp") ? new Network.Tcp(c.getConfig("tcp")) : new Network.Tcp(com.typesafe.config.ConfigFactory.parseString("tcp{}"));
    }
  }
  
  public static class Node {
    public final Node.Client client;
    public final Node.Member member;
    public static class Client {
      public final int expectedCount;
      public final java.lang.String idFormat;
      
      public Client(com.typesafe.config.Config c) {
        this.expectedCount = c.hasPathOrNull("expectedCount") ? c.getInt("expectedCount") : 0;
        this.idFormat = c.hasPathOrNull("idFormat") ? c.getString("idFormat") : "member%d";
      }
    }
    
    public static class Member {
      public final int expectedCount;
      public final java.lang.String idFormat;
      
      public Member(com.typesafe.config.Config c) {
        this.expectedCount = c.hasPathOrNull("expectedCount") ? c.getInt("expectedCount") : 0;
        this.idFormat = c.hasPathOrNull("idFormat") ? c.getString("idFormat") : "member%d";
      }
    }
    
    public Node(com.typesafe.config.Config c) {
      this.client = c.hasPathOrNull("client") ? new Node.Client(c.getConfig("client")) : new Node.Client(com.typesafe.config.ConfigFactory.parseString("client{}"));
      this.member = c.hasPathOrNull("member") ? new Node.Member(c.getConfig("member")) : new Node.Member(com.typesafe.config.ConfigFactory.parseString("member{}"));
    }
  }
  
  public static class Tests {
    public final Tests.Functional functional;
    public final Tests.Integration integration;
    public static class Functional {
      public final java.lang.Integer iterateKeysTest;
      public final java.lang.Integer keyValueDeleteMultiThreadTest;
      public final java.lang.Integer keyValueDeleteTest;
      public final java.lang.Integer keyValueSetGetMultiThreadTest;
      public final java.lang.Integer keyValueSetGetTest;
      
      public Functional(com.typesafe.config.Config c) {
        this.iterateKeysTest = c.hasPathOrNull("iterateKeysTest") ? c.getInt("iterateKeysTest") : null;
        this.keyValueDeleteMultiThreadTest = c.hasPathOrNull("keyValueDeleteMultiThreadTest") ? c.getInt("keyValueDeleteMultiThreadTest") : null;
        this.keyValueDeleteTest = c.hasPathOrNull("keyValueDeleteTest") ? c.getInt("keyValueDeleteTest") : null;
        this.keyValueSetGetMultiThreadTest = c.hasPathOrNull("keyValueSetGetMultiThreadTest") ? c.getInt("keyValueSetGetMultiThreadTest") : null;
        this.keyValueSetGetTest = c.hasPathOrNull("keyValueSetGetTest") ? c.getInt("keyValueSetGetTest") : null;
      }
    }
    
    public static class Integration {
      public final java.lang.Integer keyValueSetGetMultiThreadTest;
      public final java.lang.Integer simpleIterateKeysTest;
      
      public Integration(com.typesafe.config.Config c) {
        this.keyValueSetGetMultiThreadTest = c.hasPathOrNull("keyValueSetGetMultiThreadTest") ? c.getInt("keyValueSetGetMultiThreadTest") : null;
        this.simpleIterateKeysTest = c.hasPathOrNull("simpleIterateKeysTest") ? c.getInt("simpleIterateKeysTest") : null;
      }
    }
    
    public Tests(com.typesafe.config.Config c) {
      this.functional = c.hasPathOrNull("functional") ? new Tests.Functional(c.getConfig("functional")) : new Tests.Functional(com.typesafe.config.ConfigFactory.parseString("functional{}"));
      this.integration = c.hasPathOrNull("integration") ? new Tests.Integration(c.getConfig("integration")) : new Tests.Integration(com.typesafe.config.ConfigFactory.parseString("integration{}"));
    }
  }
  
  public JbizurConfig(com.typesafe.config.Config c) {
    this.clients = $_LJbizurConfig_Clients$Elm(c.getList("clients"));
    this.consensus = c.hasPathOrNull("consensus") ? new JbizurConfig.Consensus(c.getConfig("consensus")) : new JbizurConfig.Consensus(com.typesafe.config.ConfigFactory.parseString("consensus{}"));
    this.logging = c.hasPathOrNull("logging") ? new JbizurConfig.Logging(c.getConfig("logging")) : new JbizurConfig.Logging(com.typesafe.config.ConfigFactory.parseString("logging{}"));
    this.members = $_LJbizurConfig_Members$Elm(c.getList("members"));
    this.network = c.hasPathOrNull("network") ? new JbizurConfig.Network(c.getConfig("network")) : new JbizurConfig.Network(com.typesafe.config.ConfigFactory.parseString("network{}"));
    this.node = c.hasPathOrNull("node") ? new JbizurConfig.Node(c.getConfig("node")) : new JbizurConfig.Node(com.typesafe.config.ConfigFactory.parseString("node{}"));
    this.tests = c.hasPathOrNull("tests") ? new JbizurConfig.Tests(c.getConfig("tests")) : new JbizurConfig.Tests(com.typesafe.config.ConfigFactory.parseString("tests{}"));
  }
  private static java.util.List<JbizurConfig.Clients$Elm> $_LJbizurConfig_Clients$Elm(com.typesafe.config.ConfigList cl) {
    java.util.ArrayList<JbizurConfig.Clients$Elm> al = new java.util.ArrayList<>();
    for (com.typesafe.config.ConfigValue cv: cl) {
      al.add(new JbizurConfig.Clients$Elm(((com.typesafe.config.ConfigObject)cv).toConfig()));
    }
    return java.util.Collections.unmodifiableList(al);
  }
  private static java.util.List<JbizurConfig.Members$Elm> $_LJbizurConfig_Members$Elm(com.typesafe.config.ConfigList cl) {
    java.util.ArrayList<JbizurConfig.Members$Elm> al = new java.util.ArrayList<>();
    for (com.typesafe.config.ConfigValue cv: cl) {
      al.add(new JbizurConfig.Members$Elm(((com.typesafe.config.ConfigObject)cv).toConfig()));
    }
    return java.util.Collections.unmodifiableList(al);
  }
}