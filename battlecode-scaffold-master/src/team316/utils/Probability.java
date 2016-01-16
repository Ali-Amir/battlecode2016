package team316.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import team316.RobotPlayer;

public class Probability <T>{
	private static final double EPS = 1e-7;
	/**
	 * Returns a random unit to build according to buildDistribution.  
	 */
	public T getRandomSample(Map<T,Double> distribution){
		List<T> options = new ArrayList<>();
		double sum = 0;
		for(T option: distribution.keySet()){
			options.add(option);
			sum += distribution.getOrDefault(option, 0.0);
		}
		double acc = 0;
		double randomDouble = (RobotPlayer.rnd.nextDouble() * sum);
		for(T option: distribution.keySet()){
			acc += distribution.get(option);
			if(randomDouble < acc){
				return option;
			}
		}
		System.out.println(distribution);
		System.out.println(options);
		return options.get(options.size()-1);
	}
	public static boolean accepts(double acceptProbability){
		if(acceptProbability < EPS)
			return false;
		if((int)(acceptProbability + EPS) > 1)
			return true;
		return RobotPlayer.rnd.nextDouble() < acceptProbability;
	}
}
