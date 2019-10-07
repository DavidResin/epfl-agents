package datatypes;

import java.util.ArrayList;
import java.util.List;

import logist.topology.Topology.City;

public class MyState {
	private City citySrc, cityDst;
	private static List<MyState> states = new ArrayList<MyState>();
	
	public static void setStates(List<City> cities) {
		for (City citySrc : cities) {
			for (City cityDst : cities) {
				if (cityDst == citySrc)
					states.add(new MyState(citySrc, null));
				else
					states.add(new MyState(citySrc, cityDst));
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
		return cityDst != null;
	}
	
	public double getDistance() {
		return citySrc.distanceTo(cityDst);
	}
}
