package datatypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class State implements Comparable<State> {
	private City currentCity;
	private Map<City, ArrayList<Task>> cityMap;
	private Map<City, ArrayList<Task>> carriedTasks;
	private double g;
	private List<Step> steps;
	private TaskSet currentTasks;

	public State(List<City> cities){
		cityMap = new HashMap<City, ArrayList<Task>>();
		carriedTasks = new HashMap<City, ArrayList<Task>>();
		for(City city : cities){
			carriedTasks.put(city, new ArrayList<Task>());
			cityMap.put(city, new ArrayList<Task>());
		}
	}
	
	public State(State state){
		currentCity = state.currentCity;
		cityMap = new HashMap<City, ArrayList<Task>>();
		carriedTasks = new HashMap<City, ArrayList<Task>>();
		for(City city : state.getCityMap().keySet()){
			carriedTasks.put(city, new ArrayList<Task>());
			cityMap.put(city, new ArrayList<Task>());
			for(Task task : state.getCarriedTasks().get(city)){
				carriedTasks.get(city).add(task);
			}
			for(Task task : state.getCityMap().get(city)){
				cityMap.get(city).add(task);
			}
		}
	}
	
	public State() {
		
	}
	
	public Map<City, ArrayList<Task>> getCityMap(){
		return cityMap;
	}
	
	public Map<City, ArrayList<Task>> getCarriedTasks(){
		return carriedTasks;
	}
	
	public City getCurrentCity() {
		return currentCity;
	}
	
	public void setCurrentCity(City city){
		currentCity = city;
	}
	
	public int getCarriedWeight(){
		int carriedWeight = 0;
		for(City city : carriedTasks.keySet()){
			for(Task task : carriedTasks.get(city)){
				carriedWeight += task.weight;
			}
		}
		return carriedWeight;
	}
	
	public boolean tasksLeft(){
		for(City city : cityMap.keySet()){
			if(!cityMap.get(city).isEmpty())
				return true;
		}
		return false;
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj == this)
			return true;
		
		if(obj == null || obj.getClass() != this.getClass())
			return false;
		
		State state2 = (State) obj;
		if(state2.currentCity.equals(this.currentCity)
				&& state2.getCityMap().equals(this.cityMap)
				&& state2.getCarriedTasks().equals(this.carriedTasks))
			return true;
		
		return false;
	}
	
	@Override
	public String toString(){
		return "Current city: " + this.currentCity + "\n CityMap: " + this.cityMap + "\n Carried tasks: " + this.carriedTasks + "\n";
	}

	public boolean isBetterThan(List<State> states) {
		for (State other : states)
			if (this.currentCity.equals(other.currentCity)
					&& this.carriedTasks.equals(other.getCarriedTasks())
					&& this.remainingTasks.equals(other.getRemainingTasks()))
					return true;	
		
		return false;
	}
	

	public Collection<? extends State> nextStates() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isFinal() {
		// TODO Auto-generated method stub
		return false;
	}

	public Plan getPlan(Vehicle vehicle) {
		City currCity = vehicle.getCurrentCity();
		City nextCity = null;
		Plan plan = new Plan(vehicle.getCurrentCity());
		
		for (Step step : this.steps) {
			if (step.type == Step.Type.PICK)
				nextCity = step.task.pickupCity;
			else
				nextCity = step.task.deliveryCity;
			
			for (City c : currCity.pathTo(nextCity))
				currCity = nextCity;
			
			if (step.type == Step.Type.PICK)
				plan.appendPickup(step.task);
			else
				plan.appendDelivery(step.task);
		}
		
		return plan;
	}

	@Override
	public int compareTo(State other) {
		return Double.compare(this.getF(), other.getF());
	}

	private double getF() {
		return g + getH();
	}
	
	private double getH() {
		return 0;
	}

	public void setCurrentTasks(TaskSet tasks) {
		this.currentTasks = tasks;
	}

	public void setCarriedTasks(TaskSet tasks) {
		this.carriedTasks = tasks;
	}
}

class Step {
	public enum Type {
		PICK, DROP
	};
	public Type type;
	public Task task;
	
	public Step(Type type, Task task) {
		this.type = type;
		this.task = task;
	}
}
