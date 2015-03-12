package client;

/**
 * Basic implementation of AbstractRoute.
 * @author Jaco
 * @version 09-03-2015
 */
public class BasicRoute extends AbstractRoute {
    public int dest;
    public int cost;
    public int neigh;

    public BasicRoute(int dest, int cost, int neigh){
        this.cost = cost;
        this.dest = dest;
        this.neigh = neigh;
        this.nextHop = neigh;
    }

    public void setCost(int cost){
        this.cost = cost;
    }

    public void setNeigh(int neigh){
        this.neigh = neigh;
        this.nextHop = neigh;
    }

    public int getDest(){
        return dest;
    }

    public int getCost(){
        return cost;
    }

    public int getNeigh(){
        return neigh;
    }

    @Override
    public String toString() {
        return String.format("{Dest: %s, Cost: %s, Neigh: %s}", dest, cost, neigh);
    }
}
