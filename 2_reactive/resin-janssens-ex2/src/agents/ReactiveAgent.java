package agents;

import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;
import datatypes.MyAction;
import datatypes.MyState;

public class ReactiveAgent implements ReactiveBehavior {

	private Random random;
	private int numActions;
	private Agent myAgent;
	private TaskDistribution td;
	private Topology topology;
	private double numSkippedActions = 0.0;
	private Map<MyState, Double> V;
	private Map<MyState, MyAction> Best;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		this.random = new Random();
		this.numActions = 0;
		this.myAgent = agent;
		this.td = td;
		this.topology = topology;
		
		MyState.setStates(topology.cities());
		System.out.println("=====" + agent.name() + "=====");
		printV();
		reinforcementLearningAlgorithm(discount);
		printV();
	}
	
	public void reinforcementLearningAlgorithm(double discount) {		
		double threshold = 0.0001;
		double max = threshold + 1;
		
		Map<MyState, Double> nextV = new HashMap<MyState, Double>();
		Map<MyState, Double> currV;
		Best = new HashMap<MyState, MyAction>();
		
		for (MyState state : MyState.getAllStates())
			nextV.put(state, 0.0);
		
		while (max > threshold) {
			currV = nextV;
			nextV = new HashMap<MyState, Double>();
			
			for (MyState state : MyState.getAllStates())
				nextV.put(state, 0.0);
			
			for (MyState currState : MyState.getAllStates()) {
				Map<MyAction, Double> Q = new HashMap<MyAction, Double>();
				
				for (MyAction action : MyAction.values()) {
					double value = getReward(currState, action);
					
					for (MyState nextState : MyState.getAllStates()) {
						double transProb = getTransProb(currState, action, nextState);
						double nextValue = nextV.get(nextState);
						value += discount * transProb * nextValue;
					}
					
					Q.put(action, value);
					nextV.put(currState, value);
					Best.put(currState, action);
				}
			}
			
			max = getMaxVChange(currV, nextV);
		}
		
		V = nextV;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		City city = vehicle.getCurrentCity();
		MyState state = MyState.find(city, availableTask == null ? vehicle.getCurrentCity() : availableTask.deliveryCity);
		
		if (Best.get(state) == MyAction.SKIP) {
			System.out.println(state + ": SKIP");
			action = new Move(city.randomNeighbor(random));
			numSkippedActions += 1;
		}

		else {
			System.out.println(state + ": TAKE");
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1){
			System.out.println("The total profit after " + numActions + " actions is " + myAgent.getTotalProfit() + " (average profit: " + (myAgent.getTotalProfit() / (double)numActions) + ")");
			System.out.println("Skipped rate: " + numSkippedActions/numActions + "total number of skipped actions: " + numSkippedActions);
		}
		
		numActions++;
		return action;
	}
	
	private void printV() {
		if(V == null){
			System.out.println("NULL");
			return;
		}
		for (MyState state : MyState.getAllStates())
			System.out.println(state + ": " + Best.get(state) + ", " + V.get(state));
	}
	
	// Reward table
	private double getReward(MyState state, MyAction action) {
		Vehicle vehicle = myAgent.vehicles().iterator().next();
		double reward = -vehicle.costPerKm();
		
		if (state.hasTask())
			reward *= state.getDistance();
		else
			reward *= getAvgDistance(state.getCitySrc()); 
			
		if (state.hasTask() && action == MyAction.TAKE)
			reward += td.reward(state.getCitySrc(), state.getCityDst());
		
		return reward;
	}

	// Average distance to neighbor cities, only used by the reward table
	private double getAvgDistance(City citySrc) {
		double sum = 0;
		
		for (City cityDst : citySrc.neighbors())
			sum += citySrc.distanceTo(cityDst);
		
		return sum / citySrc.neighbors().size();
	}
	
	// Transition table
	private double getTransProb(MyState currState, MyAction action, MyState nextState) {
		City step1 = currState.getCityDst();
		City step2 = nextState.getCityDst();
		
		if (step1 != nextState.getCitySrc())
			return 0d;
		
		if (action == MyAction.TAKE) {
			if (nextState.hasTask())
				return td.probability(step1, step2);
			else 
				return getEmptyProb(step1);
		}
		else {
			if (step1.neighbors().contains(step2))
				return 1d / step1.neighbors().size();
			else
				return 0d;
		}
	}
	
	// Probability of no task available, only used by the transition table
	private double getEmptyProb(City citySrc) {
		double sum = 0;
		
		for (City cityDst : topology.cities())
			sum += td.probability(citySrc, cityDst);
		
		return 1 - sum;
	}
	
	private double getMaxVChange(Map<MyState, Double> currV, Map<MyState, Double> nextV) {
		double max = 0d;
		
		for (MyState state : currV.keySet()) {
			double temp = Math.abs(currV.get(state) - nextV.get(state));
			
			if (temp > max)
				max = temp;
		}
		
		return max;
	}
}
