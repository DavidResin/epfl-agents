package agents;

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
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	private TaskDistribution td;
	private Topology topology;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		this.td = td;
		this.topology = topology;
	}
	
	public void reinforcementLearningAlgorithm(TaskDistribution td, double discount){
		HashMap<MyState, MyAction> Best = new HashMap<MyState, MyAction>();
		HashMap<MyState, Double> V = new HashMap<MyState, Double>();
		
		// Boolean to indicate whether the last iteration of the RLA has changed V
		boolean improvement = true;
		
		while(improvement){
			improvement = false;
			for(MyState state : MyState.getAllStates()){
				HashMap<MyAction, Double> Q = new HashMap<MyAction, Double>();
				
				for(MyAction action : MyAction.values()){
					double value = 0.0; // change to reward
					for(MyState stateAfterTransition : MyState.getAllStates()){
						value += 0;
					}
					Q.put(action, value);
					
					if(V.get(state) == null || value > V.get(state)){
						V.put(state, value);
						Best.put(state, action);
						
						// Make the improvement flag true since V has been updated
						improvement = true;
					}
				}
			}
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
	
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
	
	private double getAvgDistance(City citySrc) {
		double sum = 0;
		
		for (City cityDst : citySrc.neighbors())
			sum += citySrc.distanceTo(cityDst);
		
		return sum / citySrc.neighbors().size();
	}
	
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
	
	private double getEmptyProb(City citySrc) {
		double sum = 0;
		
		for (City cityDst : topology.cities())
			sum += td.probability(citySrc, cityDst);
		
		return 1 - sum;
	}
}
