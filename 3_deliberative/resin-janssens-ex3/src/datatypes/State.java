package datatypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logist.plan.Plan;
import logist.task.Task;
import logist.topology.Topology.City;

public class State {
	private City currentCity;
	private Map<City, Map<City, Task>> cityMap;
	private Map<City, Task> carriedTasks;
	private Plan plan;
	
	public State(List<City> cities){
		cityMap = new HashMap<City, Map<City, Task>>();
		carriedTasks = new HashMap<City, Task>();
		for(City city : cities){
			carriedTasks.put(city, null);
			cityMap.put(city, new HashMap<City, Task>());
			for(City city2 : cities){
				cityMap.get(city).put(city2, null);
			}
		}
		plan = Plan.EMPTY;
	}
	
	public State(State state){
		currentCity = state.currentCity;
		cityMap = new HashMap<City, Map<City, Task>>();
		for(City city : state.getCityMap().keySet()){
			carriedTasks.put(city, state.getCarriedTasks().get(city));
			cityMap.put(city, new HashMap<City, Task>());
			for(City city2 : state.getCityMap().keySet()){
				cityMap.get(city).put(city2, state.getCityMap().get(city).get(city2));
			}
		}
		plan = state.plan;
	}
	
	public Map<City, Map<City, Task>> getCityMap(){
		return cityMap;
	}
	
	public Map<City, Task> getCarriedTasks(){
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
			carriedWeight += carriedTasks.get(city).weight;
		}
		return carriedWeight;
	}
	
	public boolean tasksLeft(){
		for(City city : cityMap.keySet()){
			for(City city2 : cityMap.get(city).keySet()){
				if(cityMap.get(city).get(city2) != null){
					return true;
				}
			}
		}
		return false;
	}
	
}
