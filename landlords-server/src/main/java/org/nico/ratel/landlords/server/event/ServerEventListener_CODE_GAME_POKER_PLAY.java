package org.nico.ratel.landlords.server.event;

import org.nico.noson.Noson;
import org.nico.ratel.landlords.channel.ChannelUtils;
import org.nico.ratel.landlords.entity.*;
import org.nico.ratel.landlords.enums.*;
import org.nico.ratel.landlords.helper.MapHelper;
import org.nico.ratel.landlords.helper.PokerHelper;
import org.nico.ratel.landlords.server.ServerContains;
import org.nico.ratel.landlords.server.robot.RobotEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerEventListener_CODE_GAME_POKER_PLAY implements ServerEventListener {

    @Override
    public void call(ClientSide clientSide, String data) {
        Room room = ServerContains.getRoom(clientSide.getRoomId());
        if (room != null) {
            if (room.getCurrentSellClient() == clientSide.getId()) {
                Character[] options = Noson.convert(data, Character[].class);
                int[] indexes = PokerHelper.getIndexes(options, clientSide.getPokers());
                if (PokerHelper.checkPokerIndex(indexes, clientSide.getPokers())) {
                    boolean sellFlag = true;

                    List<Poker> currentPokers = PokerHelper.getPoker(indexes, clientSide.getPokers());
                    PokerSell currentPokerShell = PokerHelper.checkPokerType(currentPokers);
                    if (currentPokerShell.getSellType() != SellType.ILLEGAL) {
                        if (room.getLastSellClient() != clientSide.getId() && room.getLastPokerShell() != null) {
                            PokerSell lastPokerShell = room.getLastPokerShell();

                            if ((lastPokerShell.getSellType() != currentPokerShell.getSellType() || lastPokerShell.getSellPokers().size() != currentPokerShell.getSellPokers().size()) && currentPokerShell.getSellType() != SellType.BOMB && currentPokerShell.getSellType() != SellType.KING_BOMB) {
                                String result = MapHelper.newInstance()
                                        .put("playType", currentPokerShell.getSellType())
                                        .put("playCount", currentPokerShell.getSellPokers().size())
                                        .put("preType", lastPokerShell.getSellType())
                                        .put("preCount", lastPokerShell.getSellPokers().size())
                                        .json();
                                sellFlag = false;
                                ChannelUtils.pushToClient(clientSide.getChannel(), ClientEventCode.CODE_GAME_POKER_PLAY_MISMATCH, result);
                            } else if (lastPokerShell.getScore() >= currentPokerShell.getScore()) {
                                String result = MapHelper.newInstance()
                                        .put("playScore", currentPokerShell.getScore())
                                        .put("preScore", lastPokerShell.getScore())
                                        .json();
                                sellFlag = false;
                                ChannelUtils.pushToClient(clientSide.getChannel(), ClientEventCode.CODE_GAME_POKER_PLAY_LESS, result);
                            }
                        }
                    } else {
                        sellFlag = false;
                        ChannelUtils.pushToClient(clientSide.getChannel(), ClientEventCode.CODE_GAME_POKER_PLAY_INVALID, null);
                    }

                    if (sellFlag) {

                        if(currentPokerShell.getSellType() == SellType.BOMB || currentPokerShell.getSellType() == SellType.KING_BOMB){
                            room.setMagnification(room.getMagnification() * 2);
                        }

                        ClientSide next = clientSide.getNext();

                        room.setLastSellClient(clientSide.getId());
                        room.setLastPokerShell(currentPokerShell);
                        room.setCurrentSellClient(next.getId());

                        clientSide.getPokers().removeAll(currentPokers);
                        MapHelper mapHelper = MapHelper.newInstance()
                                .put("clientId", clientSide.getId())
                                .put("clientNickname", clientSide.getNickname())
                                .put("clientType", clientSide.getType())
                                .put("pokers", currentPokers)
                                .put("lastSellClientId", clientSide.getId())
                                .put("lastSellPokers", currentPokers);

                        if (!clientSide.getPokers().isEmpty()) {
                            mapHelper.put("sellClinetNickname", next.getNickname());
                        }

                        String result = mapHelper.json();

                        for (ClientSide client : room.getClientSideList()) {
                            if (client.getRole() == ClientRole.PLAYER) {
                                ChannelUtils.pushToClient(client.getChannel(), ClientEventCode.CODE_SHOW_POKERS, result);
                            }
                        }

                        notifyWatcherPlayPoker(room, result);

                        if (clientSide.getPokers().isEmpty()) {
                            List<Map<String, Object>> playScoreList = new ArrayList<>();

                            calculateScore(clientSide, room, playScoreList);
                            result = MapHelper.newInstance()
                                    .put("winnerNickname", clientSide.getNickname())
                                    .put("winnerType", clientSide.getType())
                                    .put("roomType", room.getType() == RoomType.PVE ? "PVE" : "PVP")
                                    .put("magnification", room.getMagnification())
                                    .put("playerList", playScoreList)
                                    .json();

                            for (ClientSide client : room.getClientSideList()) {
                                if (client.getRole() == ClientRole.PLAYER) {
                                    ChannelUtils.pushToClient(client.getChannel(), ClientEventCode.CODE_GAME_OVER, result);
                                }
                            }

                            notifyWatcherGameOver(room, result);

                            ServerEventListener.get(ServerEventCode.CODE_CLIENT_EXIT).call(clientSide, data);
                        } else {
                            if (next.getRole() == ClientRole.PLAYER) {
                                ServerEventListener.get(ServerEventCode.CODE_GAME_POKER_PLAY_REDIRECT).call(next, result);
                            } else {
                                RobotEventListener.get(ClientEventCode.CODE_GAME_POKER_PLAY).call(next, data);
                            }
                        }
                    }
                } else {
                    ChannelUtils.pushToClient(clientSide.getChannel(), ClientEventCode.CODE_GAME_POKER_PLAY_INVALID, null);
                }
            } else {
                ChannelUtils.pushToClient(clientSide.getChannel(), ClientEventCode.CODE_GAME_POKER_PLAY_ORDER_ERROR, null);
            }
        } else {
//			ChannelUtils.pushToClient(clientSide.getChannel(), ClientEventCode.CODE_ROOM_PLAY_FAIL_BY_INEXIST, null);
        }
    }

    /**
     * 通知观战者出牌信息
     *
     * @param room   房间
     * @param result 出牌信息
     */
    private void notifyWatcherPlayPoker(Room room, String result) {
        for (ClientSide watcher : room.getWatcherList()) {
            ChannelUtils.pushToClient(watcher.getChannel(), ClientEventCode.CODE_SHOW_POKERS, result);
        }
    }

    /**
     * 通知观战者游戏结束
     *
     * @param room   房间
     * @param result 出牌信息
     */
    private void notifyWatcherGameOver(Room room, String result) {
        for (ClientSide watcher : room.getWatcherList()) {
            ChannelUtils.pushToClient(watcher.getChannel(), ClientEventCode.CODE_GAME_OVER, result);
        }
    }

    private void calculateScore(ClientSide clientSide, Room room, List<Map<String, Object>> playScoreList) {

        if (room.getType() == RoomType.PVE) {//pve
            for (ClientSide client : room.getClientSideList()) {
                if (client.getRole() == ClientRole.PLAYER) {
                    Score score = client.getScore();
                    int scoreChange = 0;
                    if (clientSide != client) { //落败方
                        if (clientSide.getType() == ClientType.LANDLORD) {
                            scoreChange = -ServerContains.SCORE_EACH * 2 * room.getMagnification();
                        } else {
                            scoreChange = -ServerContains.SCORE_EACH * room.getMagnification();
                        }
                    } else {// 获胜方
                        if (clientSide.getType() == ClientType.LANDLORD) {
                            scoreChange = ServerContains.SCORE_EACH * 2 * room.getMagnification();
                        } else {
                            scoreChange = ServerContains.SCORE_EACH * room.getMagnification();
                        }
                    }
                    score.setPveScore(score.getPveScore() + scoreChange);
                    ServerContains.SCORE_MAP.put(client.getNickname(), score);
                    client.setScore(score);
                    Map<String, Object> map = new HashMap<>();
                    map.put("nickName", client.getNickname());

                    map.put("currentPveScore", score.getPveScore());
                    map.put("type", client.getType() == ClientType.LANDLORD ? "Landlord" : "Peasant");
                    map.put("scoreChange", scoreChange);
                    playScoreList.add(map);
                }
            }
        } else { //pvp
            for (ClientSide client : room.getClientSideList()) {
                Score score = client.getScore();
                int scoreChange = 0;
                if (clientSide != client) { //落败方
                    if (clientSide.getType() == ClientType.LANDLORD) {
                        scoreChange = -ServerContains.SCORE_EACH * 2 * room.getMagnification();
                    } else {
                        scoreChange = -ServerContains.SCORE_EACH * room.getMagnification();
                    }
                } else {// 获胜方
                    if (clientSide.getType() == ClientType.LANDLORD) {
                        scoreChange = ServerContains.SCORE_EACH * 2 * room.getMagnification();
                    } else {
                        scoreChange = ServerContains.SCORE_EACH * room.getMagnification();
                    }
                }
                score.setPveScore(score.getPvpScore() + scoreChange);
                ServerContains.SCORE_MAP.put(client.getNickname(), score);
                client.setScore(score);
                Map<String, Object> map = new HashMap<>();
                map.put("nickName", client.getNickname());
                map.put("roomType", "pvp");
                map.put("currentPvpScore", score.getPvpScore());
                map.put("type", client.getType() == ClientType.LANDLORD ? "Landlord" : "Peasant");
                map.put("scoreChange", scoreChange);
                playScoreList.add(map);
            }
        }


    }
}
