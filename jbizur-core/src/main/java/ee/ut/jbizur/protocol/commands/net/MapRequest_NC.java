package ee.ut.jbizur.protocol.commands.net;

public class MapRequest_NC extends NetworkCommand {
    {setRequest(true);}

    private String mapName;

    public String getMapName() {
        return mapName;
    }

    public MapRequest_NC setMapName(String mapName) {
        this.mapName = mapName;
        return this;
    }

    @Override
    public String toString() {
        return "MapRequest_NC{" +
                "mapName='" + mapName + '\'' +
                "} " + super.toString();
    }
}
