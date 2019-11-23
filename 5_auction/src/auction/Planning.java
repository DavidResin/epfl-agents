package auction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import auction.Assignment;
import logist.agent.Agent;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

public class Planning {
	
	private static final double PROBA_RANDOM = .3;
	//private static final int N_ITERATIONS = 2000;
	//private static final int N_STAGES = 8;

    private static double proba_random;
	
	public static List<Plan> CSPMultiplePlan(List<Vehicle> vehicles, List<Task> tasks, int stages, int iterations) {    	
    	List<Assignment> As = new ArrayList<Assignment>();
    	
    	for (int i = 0; i < Math.pow(2, stages); i++)
    		As.add(selectInitialSolution(vehicles, tasks));
    	
    	for (int s = 0; s < stages; s++) {
    		List<Assignment> newAs = new ArrayList<Assignment>();
    		
    		for (Assignment A : As) {
    			Assignment newA = A;
    		
    			for (int i = 0; i < iterations / Math.pow(2, stages - s - 1); i++)
    				newA = iterate(newA, i);
    				
    			newAs.add(newA);
    		}
    		
    		Collections.sort(newAs);
    		As = newAs.subList(0, (int) (Math.pow(2, stages - s - 1)));
    	}
    	return As.get(0).getPlans();
    }
    
    private static Assignment iterate(Assignment A, int i) {
    	List<Assignment> N = A.chooseNeighbors();
    	Assignment newA = localChoice(N);
    	
    	if (newA == null)
    		newA = A;
    	
    	/*if(i % 100 == 0){
    		System.out.print("Iteration " + i + " : " + N.size() + " neighbors / ");
    		for (List<Integer> o : newA.getOrders())
    			System.out.print(o.size() + " ");
    		System.out.print(" / " + newA.getCost() + " / " + (newA.getCost() - A.getCost()));
    		System.out.println();
    	}*/
		
		return newA;
    }
    
    private static Assignment selectInitialSolution(List<Vehicle> vehicles, List<Task> tasks) {
    	// Create an empty assignment
    	Assignment A = new Assignment(tasks, vehicles);
    	
    	Random random = new Random();
    	
    	// Assign all tasks to a random vehicle
    	for (int i = 0; i < A.getTasks().size(); i++){
    		int random_vehicle = random.nextInt(vehicles.size());
    		A.addTask(random_vehicle, i);
    	}
    	
    	// Shuffle all vehicles
    	for (int i = 0; i < vehicles.size(); i++) {
    		do {
    			Collections.shuffle(A.getOrders().get(i));
    		} while (!A.isValid());
    	}
    	
    	return A;
    }
    
    private static Assignment localChoice(List<Assignment> N) {
    	double cost, bestCost = 0;
		List<Assignment> candidates = new ArrayList<Assignment>();
    	Random random = new Random();
    	
    	if (random.nextFloat() <= proba_random)
    		return null;
    	
    	for (Assignment A : N) {
    		cost = A.getCost();
    		
    		if (candidates.size() == 0 || cost < bestCost) {
    			candidates = new ArrayList<Assignment>();
    			bestCost = cost;
    		}
    		
    		if (cost == bestCost)
    			candidates.add(A);
    	}
    	
    	return candidates.get(random.nextInt(candidates.size()));
    }
}
