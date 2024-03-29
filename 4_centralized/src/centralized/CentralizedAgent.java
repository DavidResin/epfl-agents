package centralized;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedAgent implements CentralizedBehavior {
	
	private static final double PROBA_RANDOM = .3;
	private static final int N_ITERATIONS = 2000;
	private static final int N_STAGES = 8;

    private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    private double proba_random;
    private int n_iterations;
    private int n_stages;
    
    @Override
    public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        } catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        // the probability of localChoice to go for a random assignment is proba_random
        proba_random = PROBA_RANDOM;
        // the amound of iterations for the CSPPlan is n_iterations
        n_iterations = N_ITERATIONS;
        // the exponent of the amount of initial solution for CSPMultiplePlan (if it is 10, then the algorithm will start with 2^10 initial solutions and divide by 2 each time)
        n_stages = N_STAGES;
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
    }

    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
        long time_start = System.currentTimeMillis();
        
        //List<Plan> plans = CSPPlan(vehicles, tasks);
        List<Plan> plans = CSPMultiplePlan(vehicles, tasks, n_stages, n_iterations);
        
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The plan was generated in " + duration + " milliseconds.");
        
        return plans;
    }
    
    private List<Plan> CSPPlan(List<Vehicle> vehicles, TaskSet tasks) {
    	System.out.println(vehicles);
    	
    	for (Task task : tasks)
    		System.out.println(task);
    	
    	// Get the initial solution
    	Assignment oldA, A = selectInitialSolution(vehicles, tasks);
    	List<Assignment> N;
    	
    	for (int i = 0; i < n_iterations; i++) {   
    		oldA = A;
    		N = A.chooseNeighbors();
    		A = this.localChoice(N);
    		
    		if (A == null)
    			A = oldA;
    		
    		System.out.print("Iteration " + i + " : " + N.size() + " neighbors / ");
    		for (int j = 0; j < vehicles.size(); j++)
    			System.out.print(A.getOrders().get(j).size() + " ");
    		System.out.print(" / " + A.getCost() + " / " + (oldA.getCost() - A.getCost()));
    		System.out.println();
    	}
    	
    	return A.getPlans();
    }
    
    private List<Plan> CSPMultiplePlan(List<Vehicle> vehicles, TaskSet tasks, int stages, int iterations) {
    	System.out.println(vehicles);
    	
    	for (Task task : tasks)
    		System.out.println(task);
    	
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
    	
    	System.out.println("Plan chosen: cost = " + As.get(0).getCost());
    	return As.get(0).getPlans();
    }
    
    private Assignment iterate(Assignment A, int i) {
    	List<Assignment> N = A.chooseNeighbors();
    	Assignment newA = localChoice(N);
    	
    	if (newA == null)
    		newA = A;
    	
    	System.out.print("Iteration " + i + " : " + N.size() + " neighbors / ");
		for (List<Integer> o : newA.getOrders())
			System.out.print(o.size() + " ");
		System.out.print(" / " + newA.getCost() + " / " + (newA.getCost() - A.getCost()));
		System.out.println();
		
		return newA;
    }
    
    private Assignment selectInitialSolution(List<Vehicle> vehicles, TaskSet tasks) {
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
    
    private Assignment localChoice(List<Assignment> N) {
    	double cost, bestCost = 0;
		List<Assignment> candidates = new ArrayList<Assignment>();
    	Random random = new Random();
    	
    	if (random.nextFloat() <= this.proba_random)
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
