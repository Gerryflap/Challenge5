package protocol;

import client.*;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GerbenRoutingProtocol implements IRoutingProtocol {
	private LinkLayer linkLayer;
	private ConcurrentHashMap<Integer, BasicRoute> forwardingTable = new ConcurrentHashMap<Integer, BasicRoute>();
    private long nextPoll = 0;
    private static final int SKIP_TIME = 5000;

	@Override
	public void init(LinkLayer linkLayer) {
		this.linkLayer = linkLayer;


        this.forwardingTable.put(this.linkLayer.getOwnAddress(), new BasicRoute(this.linkLayer.getOwnAddress(), 0, this.linkLayer.getOwnAddress()));
		// First, send a broadcast packet (to address 0), with no data
        Packet discoveryBroadcastPacket = new Packet(this.linkLayer.getOwnAddress(), 0, this.getForwardingDataTable());
        this.linkLayer.transmit(discoveryBroadcastPacket);
        nextPoll = System.currentTimeMillis() + 2 * SKIP_TIME;
    }

	@Override
	public void run() {
		try {
			while (true) {
                if (System.currentTimeMillis() > nextPoll){
                    this.forwardingTable.clear();
                    this.forwardingTable.put(this.linkLayer.getOwnAddress(), new BasicRoute(this.linkLayer.getOwnAddress(), 0, this.linkLayer.getOwnAddress()));
                    Packet discoveryBroadcastPacket = new Packet(this.linkLayer.getOwnAddress(), 0, this.getForwardingDataTable());
                    this.linkLayer.transmit(discoveryBroadcastPacket);
                    nextPoll = System.currentTimeMillis() + SKIP_TIME;
                }
				// Try to receive a packet
				Packet packet = this.linkLayer.receive();
				if (packet != null) {
                    boolean forward = handleData(packet);
                    if (forward){
                        Packet discoveryBroadcastPacket = new Packet(this.linkLayer.getOwnAddress(), 0, this.getForwardingDataTable());
                        this.linkLayer.transmit(discoveryBroadcastPacket);
                    }
                    System.out.println(this.forwardingTable);
				}
				
				Thread.sleep(10);
			}
		} catch (InterruptedException e) {
			// We were interrupted, stop execution of the protocol
		}
	}

    public boolean handleData(Packet packet){
        DataTable dataTable = packet.getData();
        Integer[] data;
        BasicRoute temp;
        boolean newValues = false;
        for(int i = 0; i < dataTable.getNRows(); i++){
            data = dataTable.getRow(i);
            data[1] += this.linkLayer.getLinkCost(packet.getSourceAddress());
            data[2] = packet.getSourceAddress();
            if(this.forwardingTable.containsKey(data[0])){
                if(this.forwardingTable.get(data[0]).getCost() > data[1]) {
                    temp = new BasicRoute(data[0], data[1], data[2]);
                    this.forwardingTable.put(data[0], temp);
                    newValues = true;
                }
            } else {
                temp = new BasicRoute(data[0], data[1], data[2]);
                this.forwardingTable.put(data[0], temp);
                newValues = true;
            }
        }
        return newValues;
    }

    public DataTable getForwardingDataTable(){
        Set<Integer> forwardingKeySet = this.forwardingTable.keySet();
        DataTable dataTable = new DataTable(3);
        BasicRoute basicRoute;
        for(int link: forwardingKeySet){
            basicRoute = this.forwardingTable.get(link);
            dataTable.addRow(new Integer[]{basicRoute.getDest(), basicRoute.getCost(), basicRoute.getNeigh()});
        }
        return dataTable;

    }

	@Override
	public ConcurrentHashMap<Integer, BasicRoute> getForwardingTable() {
		return this.forwardingTable;
	}
}
