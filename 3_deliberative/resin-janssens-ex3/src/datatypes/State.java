package datatypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logist.plan.Plan;
import logist.task.Task;
import logist.topology.Topology.City;

public class State {
	private City currentCity;
	private Map<City, ArrayList<Task>> cityMap;
	private Map<City, ArrayList<Task>> carriedTasks;
	private Plan plan;
	
	public State(List<City> cities){
		cityMap = new HashMap<City, ArrayList<Task>>();
		carriedTasks = new HashMap<City, ArrayList<Task>>();
		for(City city : cities){
			carriedTasks.put(city, new ArrayList<Task>());
			cityMap.put(city, new ArrayList<Task>());
		}
		plan = Plan.EMPTY;
	}
	
	public State(State state){
		currentCity = state.currentCity;
		cityMap = new HashMap<City, ArrayList<Task>>();
		for(City city : state.getCityMap().keySet()){
			carriedTasks.put(city, state.getCarriedTasks().get(city));
			cityMap.put(city, state.getCityMap().get(city));
		}
		plan = state.plan;
	}
	
	public Map<City, ArrayList<Task>> getCityMap(){
		return cityMap;
	}
	
	public Map<City, ArrayList<Task>> getCarriedTasks(){
		return carriedTasks;
	}
	
	public City getCurrentCity(){
		return currentCity;
	}
	
	public Plan getPlan(){
		return plan;
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
	
}
