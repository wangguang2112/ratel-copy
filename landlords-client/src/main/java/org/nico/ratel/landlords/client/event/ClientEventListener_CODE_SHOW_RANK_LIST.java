package org.nico.ratel.landlords.client.event;

import io.netty.channel.Channel;
import org.nico.noson.Noson;
import org.nico.noson.entity.NoType;
import org.nico.ratel.landlords.enums.ClientEventCode;
import org.nico.ratel.landlords.print.FormatPrinter;
import org.nico.ratel.landlords.print.SimplePrinter;
import org.nico.ratel.landlords.print.SimpleWriter;

import java.util.List;
import java.util.Map;

import static org.nico.ratel.landlords.client.event.ClientEventListener_CODE_CLIENT_NICKNAME_SET.NICKNAME_MAX_LENGTH;

public class ClientEventListener_CODE_SHOW_RANK_LIST extends ClientEventListener{

	@Override
	public void call(Channel channel, String data) {
		
		List<Map<String, Object>> rankList = Noson.convert(data, new NoType<List<Map<String, Object>>>() {});

		SimplePrinter.printNotice(" \nRANKING LIST:(PVE + PVP)\n");

		if(rankList.isEmpty()){
			SimplePrinter.printNotice("There has no Players!\n");
		} else {
			System.out.printf("#%15s|%15s|%15s#\n" ,"nickName","pvp points","pve points");
			for(Map<String, Object> map:rankList){
				System.out.printf("#%15s|%15s|%15s#\n" ,map.get("nickName"),map.get("pvpScore"),map.get("pveScore"));
			}
		}


		SimplePrinter.printNotice("Enter 'enter' key to return to options list\n");
		String line = SimpleWriter.write("rank");
		get(ClientEventCode.CODE_SHOW_OPTIONS).call(channel, data);

	}



}
