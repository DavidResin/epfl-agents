package centralized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

public class Assignment {
	private final List<Task> tasks;
	private final List<Vehicle> vehicles;
	
	private List<List<Integer>> orders; // Map from a vehicle id to a list of task ids (pickup and deliveries)
	
	public Assignment(TaskSet tasks, List<Vehicle> vehicles){
		this.tasks = new ArrayList<Task>();
    	for(Task task : tasks){
    		this.tasks.add(task);
    	}
		this.vehicles = vehicles;
		this.orders = new ArrayList<List<Integer>>(vehicles.size());
		for(int i = 0; i < vehicles.size(); i++){
			this.orders.add(new ArrayList<Integer>());
		}
	}

	public Assignment(List<Task> tasks, List<Vehicle> vehicles, List<List<Integer>> orders) {
		this.tasks = tasks;
		this.vehicles = vehicles;
		this.orders = orders;
	}
	
	public Assignment deepCopy() {
		List<List<Integer>> newOrders = new ArrayList<List<Integer>>();
		
		for (List<Integer> sublist : this.orders)
		    newOrders.add(new ArrayList<Integer>(sublist));
		
		return new Assignment(this.tasks, this.vehicles, newOrders);
	}
	
	private double cost() {
		// TODO
		return 0;
	}
	
	private void swapTasks(int v_id, int t1_id, int t2_id) {
		Collections.swap(this.orders.get(v_id), t1_id, t2_id);
	}
    
    public Assignment changingTaskOrder(int v_id, int t1_id, int t2_id) {
    	Assignment newA = this.deepCopy();
    	newA.swapTasks(v_id, t1_id, t2_id);    	
    	return newA;
    }
	
	public void addTask(int v_id, int t_id) {
		List<Integer> moving = Arrays.asList(new Integer[]{t_id, t_id});
		this.orders.get(v_id).addAll(0, moving);
	}
	
	public void remTask(int v_id, int t_id) {
		List<Integer> moving = Arrays.asList(new Integer[]{t_id, t_id});
		this.orders.get(v_id).removeAll(moving);
	}

	// Move the first task of the src vehicle to the dst vehicle
	public Assignment changingVehicle(int v_src_id, int v_dst_id) {
		int t_id = this.orders.get(v_src_id).get(0);

		Assignment newA = this.deepCopy();
		newA.remTask(v_src_id, t_id);
		newA.addTask(v_dst_id, t_id);
		
		return newA;
	}
    
    public List<Assignment> chooseNeighbors() {
    	// TODO
    	return null;
    }
	
	// Shuffles tasks within a vehicle
	public void shuffleTasks(int v_id) {
		// TODO
	}
	
	// Shuffles tasks between vehicles
	public void shuffleVehicles() {
		// TODO
	}
	
	// Shuffles everything
	public void shuffle() {
		shuffleVehicles();
		
		for (int i = 0; i < orders.size(); i++)
			shuffleTasks(i);
	}
	
	public boolean testConstraints() {
		// tasks dont appear in 2 vehicles
		// all tasks are taken and dropped by the same vehicle but not by NULL
		// all tasks are picked up before they are delivered
		// load can't be exceeded
		
		return false;
	}
	
	public List<Plan> getPlans() {
		List<Plan> plans = new ArrayList<Plan>();
		for(Vehicle vehicle : vehicles){
			Plan plan = new Plan(vehicle.getCurrentCity());
			for(Integer i : orders.get(vehicles.indexOf(vehicle))){
				
			}
		}
		
		return plans;
	}

	public List<List<Integer>> getOrders() {
		return orders;
	}

	public void setOrders(List<List<Integer>> orders) {
		this.orders = orders;
	}

	public List<Task> getTasks() {
		return tasks;
	}

	public List<Vehicle> getVehicles() {
		return vehicles;
	}
}
