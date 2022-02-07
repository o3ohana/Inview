package com.ssafy.groupcall;

import java.io.IOException;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.MediaType;
import org.kurento.client.PausedEvent;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.RecordingEvent;
import org.kurento.client.StoppedEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class CallHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(CallHandler.class);

	private static final Gson gson = new GsonBuilder().create();

	private static final String RECORDER_FILE_PATH = "file:///tmp/HelloWorldRecorded.webm";

	@Autowired
	private RoomManager roomManager;

	@Autowired
	private UserRegistry registry;

	@Autowired
	private KurentoClient kurento;

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		final JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

		final UserSession user = registry.getBySession(session);

		if (user != null) {
			log.debug("Incoming message from user '{}': {}", user.getUserId(), jsonMessage);
		} else {
			log.debug("Incoming message from new user: {}", jsonMessage);
		}

		switch (jsonMessage.get("id").getAsString()) {
		case "joinRoom":
			joinRoom(jsonMessage, session);
			break;
		case "receiveVideoFrom":
			final int senderId = jsonMessage.get("sender").getAsInt();
			final UserSession sender = registry.getByUserId(senderId);
			final String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
			user.receiveVideoFrom(sender, sdpOffer);
			break;
		case "leaveRoom":
			leaveRoom(user);
			break;
		case "onIceCandidate":
			JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();

			if (user != null) {
				IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
						candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
				if(jsonMessage.get("userId")!=null) user.addCandidate(cand, jsonMessage.get("userId").getAsInt());
				else user.addCandidate(cand, user.getUserId()); //녹화 기능에 필요
			}
			break;
		case "start":
			start(session, jsonMessage);
			break;
		case "stop":
			if (user != null)
				user.stop();
			break;
		case "play":
			play(user, session, jsonMessage);
			break;
		default:
			break;
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		UserSession user = registry.removeBySession(session);
		roomManager.getRoom(user.getMeetingId()).leave(user);
	}

	private void joinRoom(JsonObject params, WebSocketSession session) throws IOException {
		final int meetingId = params.get("meetingId").getAsInt();
		final int userId = params.get("userId").getAsInt();
		log.info("PARTICIPANT {}: trying to join room {}", userId, meetingId);

		Room room = roomManager.getRoom(meetingId);
		final UserSession user = room.join(userId, session);
		registry.register(user);
	}

	private void leaveRoom(UserSession user) throws IOException {
		final Room room = roomManager.getRoom(user.getMeetingId());
		room.leave(user);
		if (room.getParticipants().isEmpty()) {
			roomManager.removeRoom(room);
		}
	}

	private void start(final WebSocketSession session, JsonObject jsonMessage) {
		try {
				
			
			// 1. Media logic (webRtcEndpoint in loopback)
			UserSession user = registry.getBySession(session);
			MediaPipeline pipeline = user.getPipeline();
			WebRtcEndpoint webRtcEndpoint = user.getOutgoingWebRtcPeer();
			webRtcEndpoint.connect(webRtcEndpoint);

			MediaProfileSpecType profile = getMediaProfileFromMessage(jsonMessage);

			RecorderEndpoint recorder = new RecorderEndpoint.Builder(pipeline, RECORDER_FILE_PATH)
					.withMediaProfile(profile).build();

			recorder.addRecordingListener(new EventListener<RecordingEvent>() {
				@Override
				public void onEvent(RecordingEvent event) {
					JsonObject response = new JsonObject();
					response.addProperty("id", "recording");
					try {
						synchronized (session) {
							session.sendMessage(new TextMessage(response.toString()));
						}
					} catch (IOException e) {
						log.error(e.getMessage());
					}
				}
			});

			recorder.addStoppedListener(new EventListener<StoppedEvent>() {
				@Override
				public void onEvent(StoppedEvent event) {
					JsonObject response = new JsonObject();
					response.addProperty("id", "stopped");
					try {
						synchronized (session) {
							session.sendMessage(new TextMessage(response.toString()));
						}
					} catch (IOException e) {
						log.error(e.getMessage());
					}
				}
			});

			recorder.addPausedListener(new EventListener<PausedEvent>() {
				@Override
				public void onEvent(PausedEvent event) {
					JsonObject response = new JsonObject();
					response.addProperty("id", "paused");
					try {
						synchronized (session) {
							session.sendMessage(new TextMessage(response.toString()));
						}
					} catch (IOException e) {
						log.error(e.getMessage());
					}
				}
			});

			connectAccordingToProfile(webRtcEndpoint, recorder, profile);

			// 2. Store user session
			user.setRecorderEndpoint(recorder);

			webRtcEndpoint.gatherCandidates();

			recorder.record();
		} catch (Throwable t) {
			log.error("Start error", t);
			sendError(session, t.getMessage());
		}
	}

	private MediaProfileSpecType getMediaProfileFromMessage(JsonObject jsonMessage) {

		MediaProfileSpecType profile = MediaProfileSpecType.WEBM;
		return profile;
	}

	private void connectAccordingToProfile(WebRtcEndpoint webRtcEndpoint, RecorderEndpoint recorder,
			MediaProfileSpecType profile) {
		switch (profile) {
		case WEBM:
			webRtcEndpoint.connect(recorder, MediaType.AUDIO);
			webRtcEndpoint.connect(recorder, MediaType.VIDEO);
			break;
		case WEBM_AUDIO_ONLY:
			webRtcEndpoint.connect(recorder, MediaType.AUDIO);
			break;
		case WEBM_VIDEO_ONLY:
			webRtcEndpoint.connect(recorder, MediaType.VIDEO);
			break;
		default:
			throw new UnsupportedOperationException("Unsupported profile for this tutorial: " + profile);
		}
	}

	private void play(UserSession user, final WebSocketSession session, JsonObject jsonMessage) {
		try {

			// 1. Media logic
			final MediaPipeline pipeline = kurento.createMediaPipeline();
			WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
			PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline, RECORDER_FILE_PATH).build();
			player.connect(webRtcEndpoint);

			// Player listeners
			player.addErrorListener(new EventListener<ErrorEvent>() {
				@Override
				public void onEvent(ErrorEvent event) {
					log.info("ErrorEvent for session '{}': {}", session.getId(), event.getDescription());
					sendPlayEnd(session, pipeline);
				}
			});
			player.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
				@Override
				public void onEvent(EndOfStreamEvent event) {
					log.info("EndOfStreamEvent for session '{}'", session.getId());
					sendPlayEnd(session, pipeline);
				}
			});

			// 2. Store user session
			user.setMediaPipeline(pipeline);
			user.setOutgoingMedia(webRtcEndpoint);

			// 3. SDP negotiation
			String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
			String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

			JsonObject response = new JsonObject();
			response.addProperty("id", "playResponse");
			response.addProperty("sdpAnswer", sdpAnswer);

			// 4. Gather ICE candidates
			webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

				@Override
				public void onEvent(IceCandidateFoundEvent event) {
					JsonObject response = new JsonObject();
					response.addProperty("id", "iceCandidate");
					response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
					try {
						synchronized (session) {
							session.sendMessage(new TextMessage(response.toString()));
						}
					} catch (IOException e) {
						log.error(e.getMessage());
					}
				}
			});

			// 5. Play recorded stream
			player.play();

			synchronized (session) {
				session.sendMessage(new TextMessage(response.toString()));
			}

			webRtcEndpoint.gatherCandidates();
		} catch (Throwable t) {
			log.error("Play error", t);
			sendError(session, t.getMessage());
		}
	}

	public void sendPlayEnd(WebSocketSession session, MediaPipeline pipeline) {
		try {
			JsonObject response = new JsonObject();
			response.addProperty("id", "playEnd");
			session.sendMessage(new TextMessage(response.toString()));
		} catch (IOException e) {
			log.error("Error sending playEndOfStream message", e);
		}
		// Release pipeline
		pipeline.release();
	}

	private void sendError(WebSocketSession session, String message) {
		try {
			JsonObject response = new JsonObject();
			response.addProperty("id", "error");
			response.addProperty("message", message);
			session.sendMessage(new TextMessage(response.toString()));
		} catch (IOException e) {
			log.error("Exception sending message", e);
		}
	}
}
