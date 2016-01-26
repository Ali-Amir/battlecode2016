package team316.utils;

public class SingleMessage {
	public int value;
	public int radius;
	public Integer frequency = null;

	public SingleMessage(int message, int radius){
		this.value = message;
		this.radius = radius;
		this.frequency = null;
	}

	public SingleMessage(int message, int radius, Integer frequency){
		//TODO
		this.value = message;
		this.radius = radius;
		this.frequency = frequency;
	}
	
	public static SingleMessage getSingleEmptyMessage(){
		return new SingleMessage(EncodedMessage.makeEmptyMessage(), 0);
	}
}
