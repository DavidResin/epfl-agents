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
	private double numSkippedActions = 0.0;
	private Map<MyState, Double> V;
	private Map<MyState, MyAction> Best;
	private double dF;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		this.random = new Random();
		this.numActions = 0;
		this.myAgent = agent;
		this.td = td;
		this.dF = discount;
		
		MyState.setStates(topology.cities());
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
						double nextValue = currV.get(nextState);
						value += discount * transProb * nextValue;
					}

					Q.put(action, value);
					
					if(nextV.get(currState) == 0.0 || value > nextV.get(currState)){
						nextV.put(currState, value);
						Best.put(currState, action);
					}
					
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
			System.out.println("The total profit for factor " + dF + " after " + numActions + " actions is " + myAgent.getTotalProfit() + " (average profit: " + (myAgent.getTotalProfit() / (double)numActions) + ")");
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
		
		if (action == MyAction.SKIP)
			return reward * getAvgDistance(state.getCitySrc());
		else {
			if (state.hasTask()) {
				reward *= state.getDistance();
				return reward + td.reward(state.getCitySrc(), state.getCityDst()); 
			}
			else
				return Double.NEGATIVE_INFINITY;
		}
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
		City cityA1 = currState.getCitySrc();
		City cityA2 = currState.getCityDst();
		City cityB1 = nextState.getCitySrc();
		City cityB2 = nextState.getCityDst();
		
		if (action == MyAction.SKIP) {
			// Probability that the agent is in city B1, which is NOT a neighbour of city A1, after SKIPping from city A1
			if (!cityA1.hasNeighbor(cityB1))
				return 0d;
			// Probability that the agent is in city B1, which is a neighbour of city A1, while there is a task present to city B2, after SKIPping from city A1
			else if (nextState.hasTask())
				return (1.0 / cityA1.neighbors().size()) * td.probability(cityB1, cityB2);
			// Probability that the agent is in city B1, which is a neighbour of city A1, while there is no task present, after SKIPping from city A1
			else 
				return (1.0 / cityA1.neighbors().size()) * td.probability(cityB1, null);
		}
		else {
			// Probability that the agent is anywhere after TAKEing a task in city A1, while there was no task there OR
			// Probability that the agent is in city B1, which is NOT the destination of the task in city A1, after TAKEing the task in city A1
			if (!currState.hasTask() || cityB1 != cityA2)
				return 0d;
			// Probability that the agent is in city B1, the destination of the task in city A1, while there is a task present to city B2, after TAKEing the task in city A1
			else if (nextState.hasTask())
				return td.probability(cityB1, cityB2);
			// Probability that the agent is in city B1, the destination of the task in city A1, while there is no task present, after TAKEing the task in city A1
			else
				return td.probability(cityB1, null);
		}
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
