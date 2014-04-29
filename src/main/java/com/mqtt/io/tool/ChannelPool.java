package com.mqtt.io.tool;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelPool {

	private final static ConcurrentHashMap<String, Channel> cientIdChannelMap = new ConcurrentHashMap<String, Channel>(
			1000000, 0.9f, 256);
	private final static ConcurrentHashMap<Channel, String> channelClientIdMap = new ConcurrentHashMap<Channel, String>();

	
	private final static ConcurrentHashMap<String, Set<Channel>> topicChannelMap = new ConcurrentHashMap<String, Set<Channel>>(
			1000000, 0.9f, 256);

	private final static ConcurrentHashMap<Channel, Set<String>> channelTopicMap = new ConcurrentHashMap<Channel, Set<String>>();

	private final static ChannelFutureListener clientRemover = new ChannelFutureListener() {
		public void operationComplete(ChannelFuture future) throws Exception {
			remove(future.channel());
		}
	};

	public static void put(Channel channel, String clientId) {
		if (channel == null) {
			return;
		}
		if (clientId == null) {
			return;
		}
		channel.closeFuture().addListener(clientRemover);
		channelClientIdMap.put(channel, clientId);
		Channel oldChannel = cientIdChannelMap.put(clientId, channel);
		if (oldChannel != null) {
			remove(oldChannel);
			oldChannel.close();
		}
	}

	public static void remove(Channel chn) {
		removeTopic(chn);
		String clientId = channelClientIdMap.remove(chn);
		if (clientId != null) {
			cientIdChannelMap.remove(clientId, chn);
		}
		chn.closeFuture().removeListener(clientRemover);
	}

	public static void putTopic(Channel chn, String topic) {
		if (chn == null) {
			return;
		}
		if (topic == null) {
			return;
		}
		
		Set<String> topicSet = channelTopicMap.get(chn);
		if(topicSet == null){
			topicSet = new HashSet<String>(1);
		}
		topicSet.add(topic);
		
		channelTopicMap.put(chn, topicSet);
		
		Set<Channel> channelSet = topicChannelMap.get(topic);
		if(channelSet == null){
			channelSet = new HashSet<Channel>(1);
		}
		channelSet.add(chn);
	}

	public static void removeTopic(Channel chn, String topic) {
//		String topic = channelTopicMap.remove(chn);
//		if (topic != null) {
//			topicChannelMap.remove(topic, chn);
//		}
	}

	public static String getClientId(Channel chn) {
		return channelClientIdMap.get(chn);
	}

//	public static String getTopic(Channel chn) {
//		return channelTopicMap.get(chn);
//	}

	public static Set<Channel> getChannelByTopics(String topic) {
		if (topic == null) {
			return null;
		}
		return topicChannelMap.get(topic);
	}
}