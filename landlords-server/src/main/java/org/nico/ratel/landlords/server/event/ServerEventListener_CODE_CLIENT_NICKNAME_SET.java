package org.nico.ratel.landlords.server.event;

import org.nico.ratel.landlords.channel.ChannelUtils;
import org.nico.ratel.landlords.entity.ClientSide;
import org.nico.ratel.landlords.entity.Score;
import org.nico.ratel.landlords.enums.ClientEventCode;
import org.nico.ratel.landlords.helper.MapHelper;
import org.nico.ratel.landlords.server.ServerContains;

public class ServerEventListener_CODE_CLIENT_NICKNAME_SET implements ServerEventListener{

	public static final int NICKNAME_MAX_LENGTH = 10;
	
	@Override
	public void call(ClientSide client, String nickname) {
		
		if (nickname.trim().length() > NICKNAME_MAX_LENGTH) {
			String result = MapHelper.newInstance().put("invalidLength", nickname.trim().length()).json();
			ChannelUtils.pushToClient(client.getChannel(), ClientEventCode.CODE_CLIENT_NICKNAME_SET, result);
		}else{
			ClientSide clientSide = ServerContains.CLIENT_SIDE_MAP.get(client.getId());
			clientSide.setNickname(nickname);
			Score score = ServerContains.SCORE_MAP.get(nickname);
			if(score == null){
				score = new Score();
			}
			client.setScore(score);
			String result = MapHelper.newInstance()
					.put("pvpScore", score.getPvpScore())
					.put("pveScore", score.getPveScore())
					.json();
			ChannelUtils.pushToClient(client.getChannel(), ClientEventCode.CODE_SHOW_OPTIONS, result);
		}
	}

}
