package datatypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logist.task.Task;
import logist.topology.Topology.City;

public class State{
	private City currentCity;
	private Map<City, ArrayList<Task>> cityMap;
	private Map<City, ArrayList<Task>> carriedTasks;

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
}
