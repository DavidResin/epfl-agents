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

public class StateAStar implements Comparable<StateAStar> {
	private City currentCity;
	private Map<City, ArrayList<Task>> cityMap;
	private double g;
	private List<Step> steps;
	private TaskSet newTasks;
	private TaskSet currTasks;
	
	public StateAStar() {
		
	}
	
	public City getCurrentCity() {
		return currentCity;
	}
	
	public void setCurrentCity(City city){
		currentCity = city;
	}
	
	public int getCarriedWeight(){
		int sum = 0;
		
		for (Task t : currTasks)
			sum += t.weight;
		
		return sum;
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
		
		StateAStar state2 = (StateAStar) obj;
		if(state2.currentCity.equals(this.currentCity)
				&& state2.getCityMap().equals(this.cityMap)
				&& state2.getCurrTasks().equals(this.currTasks))
			return true;
		
		return false;
	}
	
	private Map<City, ArrayList<Task>> getCityMap() {
		return this.cityMap;
	}

	@Override
	public String toString(){
		return "Current city: " + this.currentCity + "\n CityMap: " + this.cityMap + "\n Carried tasks: " + this.currTasks + "\n";
	}

	public boolean isBetterThan(List<StateAStar> states) {
		for (StateAStar other : states)
			if (this.currentCity.equals(other.currentCity)
					&& this.currTasks.equals(other.getCurrTasks())
					&& this.newTasks.equals(other.getNewTasks()))
					return true;	
		
		return false;
	}

	public List<StateAStar> getNextStates(StateAStar state) {
		List<StepAStar> steps = getNextSteps(state);
		List<StateAStar> states = new ArrayList<StateAStar>();
		
		for (StepAStar s : steps) {
			TaskSet newCurrTasks = this.currTasks.clone();
			TaskSet newNewTasks = this.newTasks.clone();
			StateAStar nextState = new StateAStar();
			
			if (s.type == StepAStar.Type.PICK) {
				nextState.setCity(s.task.pickupCity);
				newCurrTasks.add(s.task);
				newNewTasks.remove(s.task);
			}
			
			if (s.type == StepAStar.Type.DROP) {
				nextState.setCity(s.task.deliveryCity);
				newCurrTasks.remove(s.task);
			}
			
			nextState.setCurrTasks(newCurrTasks);
			nextState.setNewTasks(newNewTasks);
		}
		
		return states;
	}

	private void setCity(City city) {
		this.currentCity = city;
	}

	public List<StepAStar> getNextSteps(StateAStar state) {
		List<StepAStar> steps = new ArrayList<StepAStar>();
		
		for (Task t : newTasks)
			steps.add(new StepAStar(StepAStar.Type.PICK, t));

		for (Task t : currTasks)
			steps.add(new StepAStar(StepAStar.Type.DROP, t));
		
		return steps;
	}
	
	public boolean isFinal() {
		return newTasks.isEmpty() && currTasks.isEmpty();
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
	public int compareTo(StateAStar other) {
		return Double.compare(this.getF(), other.getF());
	}

	private double getF() {
		double h = 0;
				
		for (Task t : this.currTasks)
			h += this.currentCity.distanceTo(t.deliveryCity);
		
		for (Task t : this.newTasks)
			h += this.currentCity.distanceTo(t.pickupCity) + t.pathLength();
		
		return this.g + h;
	}

	public void setNewTasks(TaskSet tasks) {
		this.newTasks = tasks;
	}

	public void setCurrTasks(TaskSet tasks) {
		this.currTasks = tasks;
	}
	
	public TaskSet getNewTasks() {
		return this.newTasks;
	}
	
	public TaskSet getCurrTasks() {
		return this.currTasks;
	}
}

class StepAStar {
	public enum Type {
		PICK, DROP
	};
	public Type type;
	public Task task;
	
	public StepAStar(Type type, Task task) {
		this.type = type;
		this.task = task;
	}
}
