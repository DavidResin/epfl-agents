package agents;

/* import table */
import logist.simulation.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import datatypes.Action;
import datatypes.State;
import datatypes.StateAStar;
import datatypes.Pair;
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
			plan = aStar(vehicle, tasks);
			break;
		case BFS:
			plan = breadthFirstSearch(vehicle, tasks);
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
	
	private StateAStar initialState(Vehicle vehicle, TaskSet tasks) {
		StateAStar state = new StateAStar();
		state.setNewTasks(tasks.clone());
		state.setCurrTasks(vehicle.getCurrentTasks().clone());
		state.setCurrentCity(vehicle.getCurrentCity());
		return state;
	}
	
	private Plan aStar(Vehicle vehicle, TaskSet tasks) {
		// what heuristic to choose?
		// f(n) (current cost) = g(n) (cost so far) + List<A> (projected cost)
		List<StateAStar> Q = new ArrayList<StateAStar>();
		List<StateAStar> C = new ArrayList<StateAStar>();
		Q.add(initialState(vehicle, tasks));
		StateAStar n = null;
		int steps = 0;
		
		do {
			n = Q.remove(0);
			steps++;
			
			if (n.isBetterThan(C)) {
				C.add(n);
				Q.addAll(n.getNextStates());
				Collections.sort(Q);
			}
		}
		while (!n.isFinal() && !Q.isEmpty());
		
		return n.getPlan(vehicle);
	}
	
	private Plan breadthFirstSearch(Vehicle vehicle, TaskSet tasks){
		Plan plan;

		// Initialize data structures
		Queue<Pair> Q = new LinkedList<Pair>();
		ArrayList<State> C = new ArrayList<State>();
		HashMap<State, Plan> bestPlans = new HashMap<State, Plan>();
		ArrayList<State> goalStates = new ArrayList<State>();
		
		// Initialize initial state
		State initialState = new State(topology.cities());
		for(Task task : tasks){
			initialState.getCityMap().get(task.pickupCity).add(task);
		}
		initialState.setCurrentCity(vehicle.getCurrentCity());
		// Check if the vehicle is already holding tasks (in case of a cancelled plan)
		if(!vehicle.getCurrentTasks().isEmpty()){
			for(Task task : vehicle.getCurrentTasks()){
				initialState.getCarriedTasks().get(task.deliveryCity).add(task);
			}
		}
		bestPlans.put(initialState, Plan.EMPTY);
		Q.add(new Pair(initialState, Plan.EMPTY));
		
		while(!Q.isEmpty()){
			Pair nextEntry = Q.poll();
			State currentState = nextEntry.getKey();
			Plan currentPlan = nextEntry.getValue();
			
			// Did we already visit the current state? (Cycle detection)
			boolean alreadyVisited = false;
			for(State state : C){
				if(state.equals(currentState)){
					if(bestPlans.get(state).totalDistance() >= currentPlan.totalDistance()){
						bestPlans.put(currentState, currentPlan);
					}
					alreadyVisited = true;
					break;
				}
			}
			if(alreadyVisited)
				continue;
			else
				bestPlans.put(currentState, currentPlan);
			
			C.add(currentState);
			
			// Is the current state a goal state?
			if(currentState.getCarriedWeight() == 0 && !currentState.tasksLeft()){
				goalStates.add(currentState);
				continue;
			}
			
			// Calculate the successor states and add them to the queue
			Q.addAll(getSuccessorStates(currentState, bestPlans.get(currentState), vehicle));
		}
		
		// Find the goal state with the cheapest plan
		plan = bestPlans.get(goalStates.get(0));
		for(State goalState : goalStates){
			if(bestPlans.get(goalState).totalDistance() < plan.totalDistance()){
				plan = bestPlans.get(goalState);
			}
		}
		System.out.println("Best plan cost: " + plan.totalDistance());
		System.out.println(plan);
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
	
	public ArrayList<Pair> getSuccessorStates(State currentState, Plan currentPlan, Vehicle vehicle){
		ArrayList<Pair> successors = new ArrayList<Pair>();
		
		City currentCity = currentState.getCurrentCity();
		int capacity = vehicle.capacity();
		
		// Is a deliver action possible?
		if(!currentState.getCarriedTasks().get(currentCity).isEmpty()){
			State newState = new State(currentState);
			Plan newPlan = copyPlan(currentPlan, vehicle.getCurrentCity());
			newState.getCarriedTasks().get(currentCity).clear();
			for(Task task : currentState.getCarriedTasks().get(currentCity)){
				newPlan.appendDelivery(task);
			}
			successors.add(new Pair(newState, newPlan));
		}
		
		// Is a pickup action possible?
		if(!currentState.getCityMap().get(currentCity).isEmpty()){
			boolean pickedUp = false;
			int totalWeight = currentState.getCarriedWeight();
			State newState = new State(currentState);
			Plan newPlan = copyPlan(currentPlan, vehicle.getCurrentCity());
			for(Task task : currentState.getCityMap().get(currentCity)){
				if(totalWeight + task.weight <= capacity){
					newPlan.appendPickup(task);
					newState.getCarriedTasks().get(task.deliveryCity).add(task);
					newState.getCityMap().get(currentCity).remove(task);
					totalWeight += task.weight;
					pickedUp = true;
				}
			}
			
			if(pickedUp)
				successors.add(new Pair(newState, newPlan));
		}
		
		// Add all the possible move actions
		for(City neighbor : currentCity.neighbors()){
			State newState = new State(currentState);
			Plan newPlan = copyPlan(currentPlan, vehicle.getCurrentCity());
			newState.setCurrentCity(neighbor);
			newPlan.appendMove(neighbor);
			successors.add(new Pair(newState, newPlan));
		}
		return successors;
	}
	
	public Plan copyPlan(Plan oldPlan, City initialCity){
		Plan newPlan = new Plan(initialCity);
		for(logist.plan.Action action : oldPlan){
			newPlan.append(action);
		}
		return newPlan;
	}
}
