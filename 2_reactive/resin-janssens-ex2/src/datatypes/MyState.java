package datatypes;

import java.util.ArrayList;
import java.util.List;

import logist.topology.Topology.City;

public class MyState {
	private City citySrc, cityDst;
	private static List<MyState> states = new ArrayList<MyState>();
	
	public static void setStates(List<City> cities) {
		if(states.size() == 0){
			for (City citySrc : cities) {
				for (City cityDst : cities) {
					states.add(new MyState(citySrc, cityDst));
				}
			}
		}
	}
	
	public static List<MyState> getAllStates(){
		return states;
	}
	
	public MyState(City citySrc, City cityDst) {
		this.citySrc = citySrc;
		this.cityDst = cityDst;
	}

	public City getCitySrc() {
		return citySrc;
	}

	public City getCityDst() {
		return cityDst;
	}
	
	public Boolean hasTask() {
		return citySrc != cityDst;
	}
	
	public double getDistance() {
		return citySrc.distanceTo(cityDst);
	}
	
	@Override
	public String toString(){
		return citySrc.name + "-" + cityDst.name;
	}
	
	public static MyState find(City citySrc, City cityDst){
		for(MyState state : states){
			if(state.getCitySrc() == citySrc && state.getCityDst() == cityDst){
				return state;
			}
		}
		return null;
	}
}
