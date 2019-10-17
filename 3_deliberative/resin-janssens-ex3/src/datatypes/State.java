package datatypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logist.topology.Topology.City;

public class State {
	private City currentCity;
	private Map<City, Map<City, Boolean>> cityMap;
	private Map<City, Boolean> carriedTasks;
	
	public State(List<City> cities){
		cityMap = new HashMap<City, Map<City, Boolean>>();
		for(City city : cities){
			carriedTasks.put(city, false);
			cityMap.put(city, new HashMap<City, Boolean>());
			for(City city2 : cities){
				cityMap.get(city).put(city2, false);
			}
		}
	}
	
	public Map<City, Map<City, Boolean>> getCityMap(){
		return cityMap;
	}
	
	public Map<City, Boolean> getCarriedTasks(){
		return carriedTasks;
	}
	
	public City getCurrentCity(){
		return currentCity;
	}
	
	public void setCurrentCity(City city){
		currentCity = city;
	}
	
}
