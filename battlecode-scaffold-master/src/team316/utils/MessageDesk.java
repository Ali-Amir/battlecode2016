package team316.utils;

import java.util.ArrayList;
import java.util.List;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class MessageDesk {
	
	final SingleMessage[] todayMessages;
	final SingleMessage[] tomorrowMessages;
	final static int MESSAGES_LIMIT = 40;
	int todayMessagesCount = 0;
	int tomorrowMessagesCount = 0;
	public MessageDesk(){
		todayMessages = new SingleMessage[MESSAGES_LIMIT];
		tomorrowMessages = new SingleMessage[MESSAGES_LIMIT];
	}
	
	public void addMessagesForToday(SingleMessage message){
		if(todayMessagesCount == MESSAGES_LIMIT){
			return;
		}
		todayMessages[todayMessagesCount] = message;
		todayMessagesCount++;
	}
	
	public void addMessagesForTomorrow(SingleMessage message){
		if(tomorrowMessagesCount == MESSAGES_LIMIT){
			return;
		}
		todayMessages[tomorrowMessagesCount] = message;
		tomorrowMessagesCount++;
	}
	
	public void sendTodayMessages(RobotController rc) throws GameActionException{
		SingleMessage message1, message2;
		for(int i = 0;i < todayMessagesCount; i+=2){
			message1 = todayMessages[i];
			if(i == MESSAGES_LIMIT - 1){
				message2 = SingleMessage.getSingleEmptyMessage();
			}else{
				message2 = todayMessages[i+1];
			}
			rc.broadcastMessageSignal(message1.value, message2.value, Math.max(message1.radius, message2.radius));
		}
		todayMessagesCount = 0;
	}
	
	public void initOnNewTurn(){
		int j = 0;
		for(; todayMessagesCount < MESSAGES_LIMIT && j < tomorrowMessagesCount; todayMessagesCount ++, j++){
			todayMessages[todayMessagesCount] = tomorrowMessages[j];
		}
		tomorrowMessagesCount -= j;
	}
}
