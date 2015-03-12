package client;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class which provides a forwarding table to fill out
 * Parametrized with the type of AbstractRoute object to contain
 * @author Jaco
 * @version 09-03-2015
 */
public class ForwardingTable<T extends AbstractRoute> {
	private Set<T> table = new HashSet<T>();
	private ReentrantLock lock = new ReentrantLock();

	/**
	 * Set a rule in the forwarding table
	 * 
	 * @param route
	 *            AbstractRoute object
	 */
	public void setRoute(T route) {
		lock.lock();
		this.removeRoute(route);
		this.table.add(route);
		lock.unlock();
	}

	/**
	 * Gets all routes in the forwarding table
	 * 
	 * @return collection of AbstractRoute objects
	 */
	public Set<T> getRoutes() {
		Set<T> returnSet = new HashSet<T>();

		lock.lock();
		Iterator<T> it = this.table.iterator();
		while (it.hasNext()) {
			returnSet.add(it.next());
		}
		lock.unlock();
		return returnSet;
	}

	/**
	 * Remove a rule from the forwarding table
	 * 
	 * @param AbstractRoute object
	 */
	public void removeRoute(T route) {
		lock.lock();
		T routeToRemove = null;
		Iterator<T> it = this.table.iterator();
		while (it.hasNext()) {
			T existingRoute = it.next();
			if (existingRoute.destination == route.destination) {
				routeToRemove = existingRoute;
				break;
			}
		}
		if (routeToRemove != null) {
			this.table.remove(routeToRemove);
		}
		lock.unlock();
	}

	/**
	 * Remove a rule from the forwarding table
	 * 
	 * @param destination
	 */
	public void removeRoute(int destination) {
		lock.lock();
		T routeToRemove = null;
		Iterator<T> it = this.table.iterator();
		while (it.hasNext()) {
			T existingRoute = it.next();
			if (existingRoute.destination == destination) {
				routeToRemove = existingRoute;
				break;
			}
		}
		if (routeToRemove != null) {
			this.table.remove(routeToRemove);
		}
		lock.unlock();
	}

	/**
	 * Get a rule from the forwarding table for a destination
	 * 
	 * @param destination
	 *            Destination address
	 * @return The AbstractRoute ojbect, or null if no route exists
	 */
	public T getRoute(int destination) {
		T resultRoute = null;
		lock.lock();
		Iterator<T> it = this.table.iterator();
		while (it.hasNext()) {
			T existingRoute = it.next();
			if (existingRoute.destination == destination) {
				resultRoute = existingRoute;
			}
		}
		lock.unlock();
		return resultRoute;
	}

	/**
	 * Clears the forwarding table of all forwarding rules
	 */
	public void clear() {
		lock.lock();
		table.clear();
		lock.unlock();
	}

	/**
	 * Called by the framework to obtain the forwarding table.
	 * 
	 * @return byte array
	 */
	public byte[] serialize() {
		lock.lock();
		byte[] serialized = new byte[this.table.size() * 8];

		int i = 0;
		Iterator<T> it = this.table.iterator();
		while (it.hasNext()) {
			T route = it.next();
			serialized[i + 0] = (byte) (route.destination >> 24);
			serialized[i + 1] = (byte) (route.destination >> 16);
			serialized[i + 2] = (byte) (route.destination >> 8);
			serialized[i + 3] = (byte) (route.destination >> 0);
			serialized[i + 4] = (byte) (route.nextHop >> 24);
			serialized[i + 5] = (byte) (route.nextHop >> 16);
			serialized[i + 6] = (byte) (route.nextHop >> 8);
			serialized[i + 7] = (byte) (route.nextHop >> 0);

			i += 8;
		}
		lock.unlock();
		return serialized;
	}
}
