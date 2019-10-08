package agents;

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

public class ShortSightedAgent implements ReactiveBehavior {

	private int numActions;
	private Agent myAgent;
	private int cityIndex;
	private Topology topology;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.numActions = 0;
		this.myAgent = agent;
		this.cityIndex = 0;
		this.topology = topology;
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		// This dummy agent simply goes to the first neighbor in the list if no task is given.
		if (availableTask == null) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.neighbors().get(0));
		} else {
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		cityIndex = (cityIndex + 1) % topology.cities().size();
		
		return action;
	}
}