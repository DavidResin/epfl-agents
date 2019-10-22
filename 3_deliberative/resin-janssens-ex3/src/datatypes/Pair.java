package datatypes;

import logist.plan.Plan;

public class Pair {
	private State key;
	private Plan value;
	
	public Pair(State key, Plan value){
		this.key = key;
		this.value = value;
	}
	
	public State getKey(){
		return key;
	}
	
	public Plan getValue(){
		return value;
	}
}
