package auction;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.agent.Agent;
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
public class AuctionAgent implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	
	private long timeout_setup;
    private long timeout_plan;
    private long timeout_bid;
	
	// Storage of auction data
	private List<Task> auction_tasks;
	private List<Integer> auction_winners;
	private List<Long[]> auction_bids;
	private List<Double[]> bid_factors;
	
	// List of tasks we won
	private List<Task> won_tasks;
	// Cost of the optimal plan for delivering all tasks we have currently won
	private double current_cost;
	
	// All model parameters
	private Double profitFactor = 1.5;
	private Double speculationFactor = 0.0;
	private Double competitionFactor = 0.2;
	private Double aggressivenessFactor = 1.0;
	
	private List<Plan> plans;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

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
		bid_factors = new ArrayList<Double[]>();
		
		won_tasks = new ArrayList<Task>();
		current_cost = 0.0;
		
		plans = new ArrayList<Plan>();
		
		// this code is used to get the timeouts
        LogistSettings ls = null;
        
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
		// the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
        
        System.out.println(timeout_plan);
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		for (int i = 0; i < bids.length; i ++)
			System.out.print(i + ": " + bids[i] + " ");

		System.out.println();

		if (winner == agent.id()) {
			currentCity = previous.deliveryCity;
			
			// Add the task we won to the list of won tasks and compute the new plan cost
			won_tasks.add(previous);
			
			// Compute a good plan for this list of tasks
			List<Plan> plans = Planning.CSPMultiplePlan(agent.vehicles(), won_tasks, 4, 1000);
			
			// Compute the cost of this plan
			current_cost = 0.0;
			
			for (int i = 0; i < agent.vehicles().size(); i++)
				current_cost += plans.get(i).totalDistance() * agent.vehicles().get(i).costPerKm();			
		}
		
		auction_tasks.add(previous);
		auction_winners.add(winner);
		auction_bids.add(bids);
		
		// Predict the bid factor for all the agents
		List<Double> distances = getAccumulatedDistances(previous);
		Double[] latest_bid_factors = new Double[bids.length];
		
		for (int i = 0; i < bids.length; i++) {
			if (distances.get(i) == 0) {
				// The agent had not won any tasks yet
				double distance = previous.pickupCity.distanceTo(previous.deliveryCity);
				latest_bid_factors[i] = bids[i] / distance;
			} else
				latest_bid_factors[i] = bids[i] / distances.get(i);
		}
		
		bid_factors.add(latest_bid_factors);
	}
	
	@Override
	public Long askPrice(Task task) {

		/*if (vehicle.capacity() < task.weight)
			return null;

		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask
				+ currentCity.distanceUnitsTo(task.pickupCity);
		double marginalCost = Measures.unitsToKM(distanceSum
				* vehicle.costPerKm());

		double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		double bid = ratio * marginalCost;*/
		
		double bid = getBid(task);
		System.out.println("Task: " + task + ", bid: " + bid);
		
		return (long) Math.round(bid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
		List<Task> taskList = new ArrayList<Task>();
		
		for (Task task : tasks)
			taskList.add(task);
		
		List<Plan> plans;
		if (taskList.size() == 0) {
			plans = new ArrayList<Plan>();
			
			for (int i = 0; i < vehicles.size(); i++)
				plans.add(Plan.EMPTY);
		} else
			plans = Planning.CSPMultiplePlan(vehicles, taskList, 4, 1000);
		
		double cost = 0.0;
		
		if (taskList.size() != 0)
			for (int i = 0; i < agent.vehicles().size(); i++)
				cost += plans.get(i).totalDistance() * agent.vehicles().get(i).costPerKm();

		double profit = tasks.rewardSum() - cost;
		System.out.println("Agent " + agent.id() + " | Total profit : " + profit);

		return plans;
		
	}
	
	private double speculateFutureCost(Task auctionedTask){
		long time_start = System.currentTimeMillis();
		double totalFutureCost = 0.0;
		
		// Iterate over all the possible next tasks
		for (City cityFrom : topology.cities()) {
			for (City cityTo : topology.cities()) {
				if (cityTo == cityFrom || distribution.probability(cityFrom, cityTo) < 0.1)
					continue;
				
				double cost = 0.0;
				
				// Create the list of tasks in this speculated plan
				List<Task> tasks = new ArrayList<Task>();
				tasks.addAll(won_tasks);
				tasks.add(auctionedTask);
				Task speculatedTask = new Task(won_tasks.size() + 1, cityFrom, cityTo, 0, distribution.weight(cityFrom, cityTo)); 
				tasks.add(speculatedTask);
				
				// Compute a good plan for this task set
				List<Plan> plans = Planning.CSPMultiplePlan(agent.vehicles(), tasks, 2, 500);
				
				// Compute the cost of this plan
				for (int i = 0; i < agent.vehicles().size(); i++)
					cost += plans.get(i).totalDistance() * agent.vehicles().get(i).costPerKm();
				
				// System.out.println(auctionedTask + " / " + speculatedTask + " cost: " + cost);
				// Add this cost to the total cost, weighted by the probability that this task will be the next one
				totalFutureCost += cost * distribution.probability(cityFrom, cityTo) / topology.cities().size();
				
				// Check if we are not going overtime
				long time_now = System.currentTimeMillis();
				
				if (time_now - time_start > timeout_bid * 0.8)
					return 0;
			}
		}
		
		return totalFutureCost;
	}
	
	// Returns the list of the current gains of each adversary (all rewards - all bids), ignoring the cost of fuel which is an uncertain value
	private List<Double> getGains() {		
		List<Double> gains = new ArrayList<Double>(Collections.nCopies(auction_bids.get(0).length, 0d));
		
		for (int i = 0; i < auction_tasks.size(); i++) {
			int winner = auction_winners.get(i);
			double total = gains.get(winner) + auction_bids.get(i)[winner];
			gains.set(winner, total);
		}
		
		return gains;
	}
	
	private List<Double> getAccumulatedDistances() {
		return getAccumulatedDistances(null);
	}
	
	private List<Double> getAccumulatedDistances(Task extra_task) {
		List<Double> accDis = new ArrayList<Double>();
		
		if (firstTime())
			return null;
		
		for (int i = 0; i < auction_bids.get(0).length; i++) {
			List<Task> tasks_of_i = new ArrayList<Task>();
			double total = 0d;
			
			for (int j = 0; j < auction_winners.size(); j++)
				if (auction_winners.get(j) == i)
					tasks_of_i.add(auction_tasks.get(j));
			
			if (extra_task != null && !tasks_of_i.contains(extra_task))
				tasks_of_i.add(extra_task);
			
			for (int j = 0; j < tasks_of_i.size(); j++)
				for (int k = 0; k < tasks_of_i.size(); k++)
					if (k != j)
						total += tasks_of_i.get(j).deliveryCity.distanceTo(tasks_of_i.get(k).deliveryCity)
							+ tasks_of_i.get(j).deliveryCity.distanceTo(tasks_of_i.get(k).pickupCity)
							+ tasks_of_i.get(j).pickupCity.distanceTo(tasks_of_i.get(k).deliveryCity)
							+ tasks_of_i.get(j).pickupCity.distanceTo(tasks_of_i.get(k).pickupCity);
					
			accDis.add(Math.sqrt(total));
		}
		
		return accDis;
	}
	
	private List<Double> getEstimatedTotals() {
		List<Double> accDis = getAccumulatedDistances();
		List<Double> gains = getGains();
		List<Double> totals = new ArrayList<Double>();
		double otherCost;
		
		for (int i = 0; i < gains.size(); i++) {
			if (accDis.get(agent.id()) == 0)
				otherCost = 0;
			else
				otherCost = current_cost * accDis.get(i) / accDis.get(agent.id());
			
			totals.add(gains.get(i) + otherCost);
		}
		
		return totals;
	}
	
	private List<Integer> getRankings() {
		if (auction_winners.size() == 0)
			return null;
		
		List<Integer> ranks = new ArrayList<Integer>();
		List<Double> totals = getEstimatedTotals();
		Collections.sort(totals);
		
		for (int i = 0; i < totals.size(); i++)
			ranks.add(totals.indexOf(getEstimatedTotals().get(i)));
		
		return ranks;
	}
	
	// Returns a value from .75 (first) to 1.25 (last)
	private double getRankingFactor() {
		List<Integer> ranks = getRankings();		
		
		if (ranks == null || ranks.size() < 2)
			return 1;
		
		return ranks.get(agent.id()) * 1.0 / (ranks.size() - 1) / 2 + .75d;
	}
	
	private Boolean firstTime() {
		return auction_winners.size() == 0;
	}
	
	// The expected bid of the other agents
	private List<Double> getExpectedBid(Task auctionedTask) {
		ArrayList<Double> predictedBids = new ArrayList<Double>();
		List<Double> estimated_distances = getAccumulatedDistances(auctionedTask);
		
		if (bid_factors.size() == 0)
			return null;
		
		for (int i = 0; i < bid_factors.get(0).length; i++) {
			// Compute the average bid factor for this agent
			double bid_factor = 0;
			
			for(int j = 0; j < bid_factors.size(); j++)
				bid_factor += bid_factors.get(j)[i];
			
			bid_factor /= bid_factors.size();
			
			// Compute the predicted bid of the agent
			if (estimated_distances.get(i) == 0)
				predictedBids.add(auctionedTask.pickupCity.distanceTo(auctionedTask.deliveryCity) * bid_factor); // If there is no distance estimation, take the distance of the auctioned task
			else
				predictedBids.add(estimated_distances.get(i) * bid_factor);
		}
		
		return predictedBids;
	}
	
	// The expected extra cost for us if we take the next task
	private double getExpectedCost(Task auctionedTask) {
		double cost = 0.0;
		
		// Create a list of all the tasks we would have to deliver if we were to take this task
		List<Task> tasks = new ArrayList<Task>();
		tasks.addAll(won_tasks);
		tasks.add(auctionedTask);
		
		// Compute a good plan for this list of tasks
		List<Plan> plans = Planning.CSPMultiplePlan(agent.vehicles(), tasks, 4, 1000);
		
		// Compute the cost of this plan
		for (int i = 0; i < agent.vehicles().size(); i++)
			cost += plans.get(i).totalDistance() * agent.vehicles().get(i).costPerKm();
		
		return cost;
	}
	
	private double getBid(Task auctionedTask) {
		long time_start = System.currentTimeMillis();
		System.out.println("=====");
		System.out.println("Agent " + agent.id() + " | Auctioned task : " + auctionedTask);
		
		// Compute the current rankings
		List<Integer> rankings = getRankings();
		System.out.println("Current estimated ranking: " + rankings);
		
		if (!firstTime())
			System.out.println("Estimated totals: " + getEstimatedTotals());
		
		// Compute the predicted bids of the other agents
		List<Double> predicted_bids = getExpectedBid(auctionedTask);
		
		// Get the lowest out of all these bids (except for our own)
		double expectedBid = 0.0;
		
		if (predicted_bids != null) {
			if (agent.id() == 0)
				expectedBid = predicted_bids.get(1);
			else
				expectedBid = predicted_bids.get(0);
			
			for (int i = 1; i < predicted_bids.size(); i++) {
				if (i == agent.id())
					continue;
				
				Double bid = predicted_bids.get(i);
				
				if (bid < expectedBid)
					expectedBid = bid;
			}				
		}
		
		System.out.println("Expected bids: " + predicted_bids + ", lowest: " + expectedBid);
		
		// Compute the difference with the cost of delivering all tasks we have already won
		System.out.println("Current cost: " + current_cost);
		double cost = getExpectedCost(auctionedTask);
		double expectedCost = cost - current_cost;
		
		if (expectedCost < 0)
			expectedCost = 0.0;
		
		System.out.println("Expected cost with auctioned task: " + cost + " | difference: " + expectedCost);
		
		// Compute the speculated value for the possible next plan
		double speculatedFutureCost = speculateFutureCost(auctionedTask);
		double expectedFutureCost = speculatedFutureCost - cost;
		System.out.println("Speculated cost with new task: " + speculatedFutureCost + " | difference: " + expectedFutureCost);
		
		// Compute the time it took to generate the bid
		long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
        System.out.println("The bid was generated in " + duration + " milliseconds.");
		
        // double bid = (expectedCost * profitFactor * (1 - speculationFactor - competitionFactor) + expectedFutureCost * speculationFactor + expectedBid * competitionFactor) * (getRankingFactor() * aggressivenessFactor);
        double bid = expectedCost * profitFactor * (1 - speculationFactor - competitionFactor) + expectedFutureCost * speculationFactor + expectedBid * competitionFactor;
        return bid;
	}
}

