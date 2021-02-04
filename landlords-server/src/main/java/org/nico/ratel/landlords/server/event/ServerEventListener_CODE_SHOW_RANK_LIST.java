package org.nico.ratel.landlords.server.event;

import org.nico.noson.Noson;
import org.nico.ratel.landlords.channel.ChannelUtils;
import org.nico.ratel.landlords.entity.ClientSide;
import org.nico.ratel.landlords.entity.Score;
import org.nico.ratel.landlords.enums.ClientEventCode;
import org.nico.ratel.landlords.helper.MapHelper;
import org.nico.ratel.landlords.server.ServerContains;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerEventListener_CODE_SHOW_RANK_LIST implements ServerEventListener {


    @Override
    public void call(ClientSide client, String data) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String keySet : ServerContains.SCORE_MAP.keySet()) {
            Score score = ServerContains.SCORE_MAP.get(keySet);
            HashMap<String, Object> map = new HashMap<>();
            map.put("nickName", keySet);
            map.put("pvpScore", score.getPvpScore());
            map.put("pveScore", score.getPveScore());
            list.add(map);
        }
        list.sort((o1, o2) -> (int) o1.get("pvpScore") + (int) o1.get("pveScore") - (int) o2.get("pvpScore") - (int) o2.get("pveScore"));

        ChannelUtils.pushToClient(client.getChannel(), ClientEventCode.CODE_SHOW_RANK_LIST, Noson.reversal(list));

    }

}
