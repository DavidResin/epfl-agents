package agents;

/* import table */
import logist.simulation.Vehicle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import datatypes.Action;
import datatypes.State;
import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class DeliberativeAgent implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			// Initialize Q and C
			Queue<State> Q = new LinkedList<State>();
			ArrayList<State> C = new ArrayList<State>();
			ArrayList<State> goalStates = new ArrayList<State>();
			
			// Initialize initial state
			State initialState = new State(topology.cities());
			for(Task task : tasks){
				initialState.getCityMap().get(task.pickupCity).put(task.deliveryCity, task);
			}
			initialState.setCurrentCity(vehicle.getCurrentCity());
			Q.add(initialState);
			
			while(!Q.isEmpty()){
				State currentState = Q.poll();
				// Is the current state a goal state?
				if(currentState.getCarriedWeight() == 0 && !currentState.tasksLeft()){
					goalStates.add(currentState);
				}
				// Is a deliver action possible?
				if(currentState.getCarriedTasks().get(currentState.getCurrentCity()) != null){
					State newState = new State(currentState);
					newState.getCarriedTasks().put(currentState.getCurrentCity(), null);
					newState.getPlan().appendDelivery(currentState.getCarriedTasks().get(currentState.getCurrentCity()));
				}
			}
			
			plan = naivePlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		return plan;
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}
