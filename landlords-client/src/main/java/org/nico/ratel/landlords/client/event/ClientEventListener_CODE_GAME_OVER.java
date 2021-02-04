package org.nico.ratel.landlords.client.event;

import java.util.List;
import java.util.Map;

import org.nico.ratel.landlords.helper.MapHelper;
import org.nico.ratel.landlords.print.SimplePrinter;

import io.netty.channel.Channel;

public class ClientEventListener_CODE_GAME_OVER extends ClientEventListener{

	@Override
	public void call(Channel channel, String data) {
		Map<String, Object> map = MapHelper.parser(data);
		SimplePrinter.printNotice("\nPlayer " + map.get("winnerNickname") + "[" + map.get("winnerType") + "]" + " won the game\n");
		SimplePrinter.printNotice("current magnification: " + map.get("magnification") + "\n");

		System.out.printf("#%15s|%15s|%15s|%15s#\n" ,"nickName","role","current-scores","score");
		List<Map<String,Object>> playerList = (List<Map<String, Object>>) map.get("playerList");
		for(Map<String,Object> value : playerList){
			/*
			*  map.put("nickName", client.getNickname());
                    map.put("roomType", "pve");
                    map.put("currentPveScore", client.getScore().getPveSocre());
                    map.put("type",client.getType() == ClientType.LANDLORD ? "Landlord" : "Peasant");
                    map.put("scoreChange", scoreChange);*/
			if(map.get("roomType").equals("PVE")){
				System.out.printf("#%15s|%15s|%15s|%15s#\n" ,value.get("nickName"),value.get("type"),value.get("currentPveScore"),value.get("scoreChange"));
			} else {
				System.out.printf("#%15s|%15s|%15s|%15s#\n" ,value.get("nickName"),value.get("type"),value.get("currentPvpScore"),value.get("scoreChange"));
			}

		}

		SimplePrinter.printNotice("\nGame over, friendship first, competition second\n");
	}

}
