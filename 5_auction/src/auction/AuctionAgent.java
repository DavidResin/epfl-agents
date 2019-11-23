package auction;

//the list of imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import auction.Planning;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 * 
 */
@SuppressWarnings("unused")
public class AuctionAgent implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	
	// Storage of auction data
	private List<Task> auction_tasks;
	private List<Integer> auction_winners;
	private List<Long[]> auction_bids;
	
	private List<Plan> plans;

	@Override
	public void setup(Topology topology, TaskDistribution distribution,
			Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();

		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
		
		auction_tasks = new ArrayList<Task>();
		auction_winners = new ArrayList<Integer>();
		auction_bids = new ArrayList<Long[]>();
		
		plans = new ArrayList<Plan>();
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		for(int i = 0; i < bids.length; i ++){
			System.out.print(i + ": " + bids[i] + " ");
		}
		System.out.println();
		if (winner == agent.id()) {
			currentCity = previous.deliveryCity;
		}
		
		auction_tasks.add(previous);
		auction_winners.add(winner);
		auction_bids.add(bids);
	}
	
	@Override
	public Long askPrice(Task task) {

		if (vehicle.capacity() < task.weight)
			return null;

		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask
				+ currentCity.distanceUnitsTo(task.pickupCity);
		double marginalCost = Measures.unitsToKM(distanceSum
				* vehicle.costPerKm());

		double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		double bid = ratio * marginalCost;
		System.out.println("Task: " + task + ", bid: " + bid);
		return (long) Math.round(bid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);

		Plan planVehicle1 = naivePlan(vehicle, tasks);

		List<Plan> plans = new ArrayList<Plan>();
		plans.add(planVehicle1);
		while (plans.size() < vehicles.size())
			plans.add(Plan.EMPTY);

		return plans;
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
	
	private double speculateFutureCost(Task auctionedTask){
		double totalFutureCost = 0.0;
		
		// Iterate over all the possible next tasks
		for(City cityFrom : topology.cities()){
			for(City cityTo : topology.cities()){
				if(cityTo == cityFrom)
					continue;
				double cost = 0.0;
				// Create the list of tasks in this speculated plan
				List<Task> tasks = new ArrayList<Task>();
				if(auctionedTask != null)
					tasks.add(auctionedTask);
				Task speculatedTask = new Task(0, cityFrom, cityTo, 0, distribution.weight(cityFrom, cityTo)); 
				tasks.add(speculatedTask);
				
				// Compute a good plan for this task set
				List<Plan> plans = Planning.CSPMultiplePlan(agent.vehicles(), tasks, 4, 1000);
				
				// Compute the cost of this plan
				for(int i = 0; i < agent.vehicles().size(); i++){
					cost += plans.get(i).totalDistance() * agent.vehicles().get(i).costPerKm();
				}
				System.out.println(auctionedTask + " / " + speculatedTask + " cost: " + cost);
				
				// Add this cost to the total cost, weighted by the probability that this task will be the next one
				totalFutureCost += cost * distribution.probability(cityFrom, cityTo) / topology.cities().size();
			}
		}
		
		return totalFutureCost;
	}
		
	// Returns the list of the current gains of each adversary (all rewards - all bids), ignoring the cost of fuel which is an uncertain value
	private List<Double> getGains() {		
		List<Double> gains = new ArrayList<Double>(Collections.nCopies(auction_bids.get(0).length, 0d));
		
		for (int i = 0; i < auction_tasks.size(); i++) {
			int winner = auction_winners.get(i);
			
			double total = gains.get(winner);
			total += auction_tasks.get(i).reward;
			total -= auction_bids.get(i)[winner];
			
			gains.set(winner, total);
		}
		
		return gains;
	}
	
	// This value determines our willingness to lose money on the bid
	private double getRiskToLoseMoney() {
		return 0.0;
	}
	
	// This value determines our willingness to lose the bid
	private double getRiskToLoseBid() {
		return 0.0;
	}
	
	// The expected bid of the other agents
	private double getExpectedBid() {
		return 0.0;
	}
	
	// The expected cost for us if we take the next task
	private double getExpectedCost() {
		return 0.0;
	}
	
	private double getBid() {
		return getExpectedCost();
	}
}

