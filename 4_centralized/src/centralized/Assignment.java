package centralized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;

public class Assignment {
	List<Task> tasks;
	List<Vehicle> vehicles;
	
	List<List<Integer>> orders; // Map from a vehicle id to a list of task ids (pickup and deliveries)
	List<Integer> attributions; // Map from a task id to a vehicle id
	
	// The domain here is trivial : it's just any number between 0 and 2 x n_tasks + n_vehicles - 1
	
	public Assignment() {
		
	}
	
	public Assignment deepCopy() {
		return null;
	}
	
	private double cost() {
		// TODO
		return 0;
	}
    
    private Assignment changingTaskOrder() {
    	// TODO
    	return null;
    }
	
	// Move the first task of the src vehicle to the dst vehicle
	private Assignment changingVehicle(int v_src_id, int v_dst_id) {
		Assignment newA = this.deepCopy();
		int t_id = newA.orders.get(v_src_id).get(0);
		List<Integer> moving = Arrays.asList(new Integer[]{t_id, t_id});
		
		newA.orders.get(v_src_id).removeAll(moving);
		newA.orders.get(v_dst_id).addAll(moving);
		
		return newA;
	}
    
    private List<Assignment> chooseNeighbors() {
    	// TODO
    	return null;
    }
	
	// Shuffles tasks within a vehicle
	private void shuffleTasks(int v_id) {
		// TODO
	}
	
	// Shuffles tasks between vehicles
	private void shuffleVehicles() {
		// TODO
	}
	
	// Shuffles everything
	private void shuffle() {
		shuffleVehicles();
		
		for (int i = 0; i < orders.size(); i++)
			shuffleTasks(i);
	}
	
	private boolean testConstraints() {
		// tasks dont appear in 2 vehicles
		// all tasks are taken and dropped by the same vehicle but not by NULL
		// all tasks are picked up before they are delivered
		// load can't be exceeded
		
		return false;
	}
	
	public List<Plan> getPlans() {
		//TODO
		return null;
	}
}
