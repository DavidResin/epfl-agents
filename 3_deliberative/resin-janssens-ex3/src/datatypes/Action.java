package datatypes;

import logist.topology.Topology.City;

public class Action {
	public enum ActionType {
		MOVE,
		DELIVER,
		PICKUP
	}
	
	private ActionType type;
	private City destinationCity;
	
	public Action(ActionType type){
		this.type = type;
	}
	
	public Action(ActionType type, City city){
		this.type = type;
		this.destinationCity = city;
	}
	
	public ActionType getType(){
		return type;
	}
	
	public City getDestinationCity(){
		return destinationCity;
	}
}
