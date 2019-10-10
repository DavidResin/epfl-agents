package datatypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logist.topology.Topology.City;

public class State {
	private City currentCity;
	private Map<City, Map<City, Integer>> cityMap;
	
	public State(List<City> cities){
		cityMap = new HashMap<City, Map<City, Integer>>();
		for(City city : cities){
			cityMap.put(city, new HashMap<City, Integer>());
			for(City city2 : cities){
				cityMap.get(city).put(city2, 0);
			}
		}
	}
	
	public Map<City, Map<City, Integer>> getCityMap(){
		return cityMap;
	}
	
	public City getCurrentCity(){
		return currentCity;
	}
	
	public void setCurrentCity(City city){
		currentCity = city;
	}
	
}
