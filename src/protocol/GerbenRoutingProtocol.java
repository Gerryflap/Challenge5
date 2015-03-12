package protocol;

import client.*;
import com.sun.javafx.scene.control.skin.IntegerFieldSkin;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GerbenRoutingProtocol implements IRoutingProtocol {
	private LinkLayer linkLayer;
	private ConcurrentHashMap<Integer, BasicRoute> forwardingTable = new ConcurrentHashMap<Integer, BasicRoute>();
    private int lastReset = 0;
    private Integer[] commitedChanges = new Integer[6];
    private Integer[] neighbours = new Integer[6];

	@Override
	public void init(LinkLayer linkLayer) {
		this.linkLayer = linkLayer;

        updateNeighbours();
        this.forwardingTable.put(this.linkLayer.getOwnAddress(), new BasicRoute(this.linkLayer.getOwnAddress(), 0, this.linkLayer.getOwnAddress()));
		// First, send a broadcast packet (to address 0), with no data
        Packet discoveryBroadcastPacket = new Packet(this.linkLayer.getOwnAddress(), 0, this.getForwardingDataTable());
        this.linkLayer.transmit(discoveryBroadcastPacket);
    }

	@Override
	public void run() {
		try {
            int count = 0;
			while (true) {
                count ++;
                if(count == 1 && checkChanges()){
                    incrementChange();
                    System.out.printf("Change detected, seq number: %s \n", this.lastReset);
                    count = 0;
                    commitChanges(getChanges());
                    updateNeighbours();
                }

                if (count == 1){
                    count = 0;
                }

				// Try to receive a packet
				Packet packet = this.linkLayer.receive();
				if (packet != null) {
                    if (packet.getData().getNColumns() == 1 && (getChangeNum(packet.getData()) > this.lastReset || hasNewChanges((getChangesFromChangePacket(packet))))) {
                        printArray(commitedChanges);
                        printArray(getChangesFromChangePacket(packet));
                        this.lastReset = getChangeNum(packet.getData());
                        System.out.printf("Change packet recieved, seq number: %s \n", this.lastReset);
                        count = 0;
                        commitChanges(getNewChanges(getChangesFromChangePacket(packet)));
                        updateNeighbours();
                    } else if (packet.getData().getNColumns() != 1) {
                        boolean forward = handleData(packet);
                        if (forward) {
                            Packet discoveryBroadcastPacket = new Packet(this.linkLayer.getOwnAddress(), 0, this.getForwardingDataTable());
                            this.linkLayer.transmit(discoveryBroadcastPacket);
                        }
                    }
                }
				
				Thread.sleep(50);
			}
		} catch (InterruptedException e) {
			// We were interrupted, stop execution of the protocol
		}
	}

    public boolean hasNewChanges(Integer[] packetChanges){
        boolean exists;
        for(Integer integer: packetChanges){
            if(integer != null){
                exists = false;
                for(Integer integer1: commitedChanges){
                    if(integer.equals(integer1)){
                        exists = true;
                    }
                }
                if(!exists){
                    return true;
                }
            }
        }
        return false;
    }

    public Integer[] getNewChanges(Integer[] changes){
        boolean exists;
        Integer[] changed = new Integer[6];
        for(Integer integer: changes){
            if(integer != null){
                exists = false;
                for(Integer integer1: commitedChanges){
                    if(integer.equals(integer1)){
                        exists = true;
                    }
                }
                if(!exists){
                    changed[integer-1] = integer;
                }
            }
        }
        return changed;
    }

    public void updateNeighbours(){
        System.out.print(linkLayer.getOwnAddress() + " Old: ");
        printArray(neighbours);
        for (int i = 0; i < neighbours.length; i++) {
            neighbours[i] = linkLayer.getLinkCost(i+1);
        }
        System.out.print(linkLayer.getOwnAddress() + " New: ");
        printArray(neighbours);

    }

    public void printArray(Integer[] array){
        String out = "[";
        for(Integer integer: array){
            if (out.length() == 1){
                out += integer;
            } else {
                out += ", "+integer;
            }
        }
        out += "]";
        System.out.println(out);
    }

    public boolean checkChanges(){
        for (int i = 1; i < 7; i++){
            if(neighbours[i-1] != linkLayer.getLinkCost(i)){
                return true;
            }
        }
        return false;
    }

    public void setMaxCost(Integer[] changes){
        for(Integer integer: changes) {
            if (integer != null) {
                for (BasicRoute basicRoute : this.forwardingTable.values()) {
                    if (basicRoute.getNeigh() == integer || basicRoute.getDest() == integer) {
                        if (basicRoute.getDest() != linkLayer.getOwnAddress()) {
                            this.forwardingTable.remove(basicRoute.getDest());
                        }
                    }
                }
            }
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

    public Integer[] getChanges(){
        Integer[] changes = new Integer[6];
        for (int i = 1; i < 7; i++){
            if(neighbours[i-1] != linkLayer.getLinkCost(i)){
                System.out.println("Change detected, link: "+ i);
                changes[i-1] = i;
            }
        }
        printArray(changes);
        if (changes.length != 0){
            changes[linkLayer.getOwnAddress()-1] = linkLayer.getOwnAddress();
        }
        return changes;

    }

    public void commitChanges(Integer[] changes){
        boolean inArray = false;
        for(Integer change: changes){
            if (change != null){
                commitedChanges[change-1] = change;
                if (change == linkLayer.getOwnAddress()){
                    inArray = true;
                }
            }
        }
        Packet discoveryBroadcastPacket = new Packet(this.linkLayer.getOwnAddress(), 0, getChangeTable(changes));
        this.linkLayer.transmit(discoveryBroadcastPacket);
        setMaxCost(changes);
        if(inArray) {
            this.forwardingTable.put(this.linkLayer.getOwnAddress(), new BasicRoute(this.linkLayer.getOwnAddress(), 0, this.linkLayer.getOwnAddress()));
            discoveryBroadcastPacket = new Packet(this.linkLayer.getOwnAddress(), 0, this.getForwardingDataTable());
            this.linkLayer.transmit(discoveryBroadcastPacket);
        }
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

    public DataTable getChangeTable(Integer[] changes){
        DataTable dataTable = new DataTable(1);
        dataTable.addRow(new Integer[]{this.lastReset});
        for(Integer change: changes){
            if (change != null) {
                dataTable.addRow(new Integer[]{change});
            } else {
                dataTable.addRow(new Integer[]{0});
            }
        }
        return dataTable;
    }

    public Integer[] getChangesFromChangePacket(Packet packet){
        Integer[] changes = new Integer[6];
        for (int i = 1; i < packet.getData().getNRows(); i++){
            Integer change = packet.getData().getRow(i)[0];
            change = change == 0?null:change;
            changes[i-1] = change;
        }
        return changes;
    }

    public int getChangeNum(DataTable dataTable){
        return dataTable.getRow(0)[0];
    }

    public void incrementChange(){
        commitedChanges = new Integer[6];
        this.lastReset ++;
    }
	@Override
	public ConcurrentHashMap<Integer, BasicRoute> getForwardingTable() {
		return this.forwardingTable;
	}
}
