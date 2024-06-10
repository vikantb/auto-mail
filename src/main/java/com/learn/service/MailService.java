package com.learn.service;

import static java.util.stream.Collectors.groupingBy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.BatchModifyMessagesRequest;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.learn.auth.CurrentUser;
import com.learn.dto.RequestDto;

import lombok.extern.log4j.Log4j2;

/* class to demonstrate use of Gmail list labels API */
@Service
@Log4j2
public class MailService {

	private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MailService.class);

	private Gmail service;
	
	@Autowired
	private UserService userService;
	
	private static volatile boolean stopFlag = false;

	private static final int THREAD_POOL_SIZE = 10;

	/**
	 * Application name.
	 */
	private static final String APPLICATION_NAME = "Auto Mail";
	/**
	 * Global instance of the JSON factory.
	 */
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	/**
	 * Directory to store authorization tokens for this application.
	 */
	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES = Arrays.asList(GmailScopes.MAIL_GOOGLE_COM, GmailScopes.GMAIL_MODIFY);
	private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

	/**
	 * Creates an authorized Credential object.
	 *
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		InputStream in = MailService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		if (in == null) {
			throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
		}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
				.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
				.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8082).build();
		Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
		// returns an authorized Credential object.
		return credential;
	}
	
	private Gmail getGmailService() {
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(userService.getAccessToken(), null))
                .createScoped(SCOPES);
        service =  new Gmail.Builder(new NetHttpTransport(), JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
        return service;
    }

	private Gmail getGmailService2() {
		if (service != null) {
			return service;
		}
		// Build a new authorized API client service.
		NetHttpTransport HTTP_TRANSPORT;
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
					.setApplicationName(APPLICATION_NAME).build();
		} catch (Exception e) {
			log.error("Error: {}", e.getMessage());
		}
		return service;
	}

	public void deleteMessageFromUser(RequestDto requestDto) throws IOException {
		List<Message> messageList = getMessages(requestDto);
		Map<String, List<Message>> map = convertMessagesToMap(messageList);

		for (Map.Entry<String, List<Message>> entry : map.entrySet()) {
			String senderId = entry.getKey();
			try {
				List<Message> messages = entry.getValue();
				if (!CollectionUtils.isEmpty(messages)) {
					deleteMessage(messages.stream().map(Message::getId).collect(Collectors.toList()));
					log.info("Messages count:{} deleted for id:{}", messages.size(), senderId);
				}
			} catch (Exception e) {
				log.error("Exception occured deleting messages for id:{}, error:{}", senderId, e.getMessage());
			}
		}
	}

	private void deleteMessage(List<String> messageIds) throws IOException {
		ExecutorService executorService = Executors.newFixedThreadPool(5);
		Gmail service = getGmailService();

		List<CompletableFuture<Void>> futures = messageIds.stream().map(id -> CompletableFuture.runAsync(() -> {
			try {
				service.users().messages().trash("me", id).execute();
			} catch (IOException e) {
				log.error("Error occured while deleting message:{}", id, e.getMessage());
			}

		}, executorService)).collect(Collectors.toList());

		// Combine all CompletableFutures and wait for them to complete
		CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		allOf.join();
		// Shutdown the executor service
		executorService.shutdown();
	}

	public void modifyMessage(RequestDto requestDto) throws IOException {
		List<Message> messageList = getMessages(requestDto);
		Map<String, List<Message>> map = convertMessagesToMap(messageList);

		for (Map.Entry<String, List<Message>> entry : map.entrySet()) {
			String senderId = entry.getKey();
			try {
				List<Message> messages = entry.getValue();
				if (!CollectionUtils.isEmpty(messages)) {
					if (requestDto.getSelfFolder()) {
						requestDto.getAddLabels().add(getCustomLabelFromName(senderId));
					}
					modifyMessage(messages.stream().map(Message::getId).collect(Collectors.toList()), requestDto);
					log.info("Messages count:{} moved for id:{}", messages.size(), senderId);
				}
			} catch (Exception e) {
				log.error("Exception occured moving messages for id:{}, error:{}", senderId, e.getMessage());
			}

		}
	}

	public void updateLabelName(Integer size) throws IOException {
		Gmail service = getGmailService();
		ListLabelsResponse listResponse = service.users().labels().list("me").execute();
		List<Label> labels = listResponse.getLabels();
		labels = labels.stream().filter(d -> d.getType().equals("user")).collect(Collectors.toList());

		ExecutorService executorService = Executors.newFixedThreadPool(3);

		AtomicInteger counter = new AtomicInteger(0);

		List<CompletableFuture<Void>> futures = labels.stream()
				.filter(label -> !label.getName().equalsIgnoreCase(getCustomLabelFromName(label.getName())))
				.map(label -> CompletableFuture.runAsync(() -> {
					String customLabelName = getCustomLabelFromName(label.getName());
					label.setName(customLabelName);
					if (!stopFlag) {
						try {
							service.users().labels().update("me", label.getId(), label).execute();
						} catch (IOException e) {
							log.error("error occured while updating label name:{}. Error:{}", label.getName(),
									e.getMessage());
							counter.incrementAndGet();
							if (counter.get() > size) {
								stopFlag = true;
							}
						}
					}

				}, executorService)).collect(Collectors.toList());

		// Combine all CompletableFutures and wait for them to complete
		CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
		allOf.join();

		stopFlag = true;
		// Shutdown the executor service
		executorService.shutdown();
	}

	public List<Label> getUserLabels(String mailId) throws IOException {
		Gmail service = getGmailService();
		ListLabelsResponse listResponse = service.users().labels().list(mailId).execute();
		List<Label> labels = listResponse.getLabels();
		return labels;
	}

	private String getCustomLabelFromName(String name) {
		try {
			String label = name.substring(name.lastIndexOf("@") + 1);
			String[] _parts = label.split("\\.");

			int size = _parts.length;

			return String.format("%s.%s", _parts[size - 2], _parts[size - 1]);
		} catch (Exception e) {
			log.error("Error while generating new Custome label name:{}", e.getMessage());
			return name;
		}
	}

	private Map<String, List<Message>> convertMessagesToMap(List<Message> messageList) throws IOException {
		return messageList.stream().collect(groupingBy(message -> getSenderEmailIdFromMessage(message)));
	}

	private String getSenderEmailIdFromMessage(Message message) {
		try {
			List<MessagePartHeader> headers = message.getPayload().getHeaders();
			for (MessagePartHeader header : headers) {
				if (header.getName().equalsIgnoreCase("From")) {
					String from = header.getValue();

					if (from.indexOf('<') >= 0) {
						return from.substring(from.indexOf('<') + 1, from.indexOf('>'));
					} else {
						return from.substring(1, from.length() - 1);
					}
				}
			}
		} catch (Exception e) {
			log.error("Error while fetching sender mail-id");
		}
		return "unknown";
	}

	private void modifyMessage(List<String> messageIds, RequestDto requestDto) throws IOException {
		resolveRequestLabels(requestDto);
		List<String> removalIds = requestDto.getRemoveLabels();
		List<String> labelIds = requestDto.getAddLabels();

		BatchModifyMessagesRequest mods = new BatchModifyMessagesRequest().setIds(messageIds).setAddLabelIds(labelIds)
				.setRemoveLabelIds(removalIds);
		Gmail service = getGmailService();
		service.users().messages().batchModify("me", mods).execute();
	}

	public void resolveRequestLabels(RequestDto requestDto) throws IOException {

		Gmail service = getGmailService();
		ListLabelsResponse listResponse = service.users().labels().list("me").execute();
		List<Label> labels = listResponse.getLabels();

		List<String> removeLabels = requestDto.getRemoveLabels();
		List<String> _removeLabels = new ArrayList<>();
		for (Label label : labels) {
			for (String r_label : removeLabels) {
				if (requestDto.getRemoveAllLabels()
						|| (label.getName().equalsIgnoreCase(r_label) || label.getId().equalsIgnoreCase(r_label))) {
					_removeLabels.add(label.getId());
					break;
				}
			}
		}
		requestDto.setRemoveLabels(_removeLabels);

		List<String> addLabels = requestDto.getAddLabels();
		List<String> _addLabels = new ArrayList<>();
		for (String a_label : addLabels) {
			boolean isAdded = false;
			for (Label label : labels) {
				if (label.getName().equalsIgnoreCase(a_label) || label.getId().equalsIgnoreCase(a_label)) {
					_addLabels.add(label.getId());
					isAdded = true;
					break;
				}
			}
			if (!isAdded) {
				_addLabels.add(createLabel(service, a_label));
			}
		}
		requestDto.setAddLabels(_addLabels);
	}

	public String createLabel(Gmail service, String name) throws IOException {
		Label newLabel = new Label().setName(name).setLabelListVisibility("labelShow").setMessageListVisibility("show");
		try {
//			newLabel.setColor(new LabelColor().setBackgroundColor(randomColor()).setTextColor(randomColor()));
		} catch (Exception e) {
			log.error("Error occured while set-up the label color. Error:{}", e.getMessage());
		}

		Label createdLabel = service.users().labels().create("me", newLabel).execute();
		return createdLabel.getId();
	}

	public List<Message> getMessageByFilter(RequestDto requestDto) {
		List<Message> messageList = getMessages(requestDto);
		log.info("Messages list size:{}", messageList.size());

		for (Message message : messageList) {
			setReadableMessageBody(message);
		}
		return messageList;
	}

	private List<Message> getMessages(RequestDto requestDto) {
		List<Message> response = new ArrayList<>();
		// Search for messages from the specified sender in the inbox

		String query = generateQuery(requestDto);

		Gmail service = getGmailService();
		ListMessagesResponse gmailResponse = null;
		String pageToken = null;

		Long requestMaxResult = requestDto.getMaxResult();
		if (requestMaxResult == null) {
			requestMaxResult = 1000L;
		}

		Long pageMaxResult = 500L;
		if (requestMaxResult < pageMaxResult) {
			pageMaxResult = requestMaxResult;
		}

		do {
			try {
				gmailResponse = service.users().messages().list("me").setMaxResults(pageMaxResult)
						.execute();
				List<Message> messages = gmailResponse.getMessages();
				if (CollectionUtils.isEmpty(messages)) {
					return response;
				}

				ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

				List<CompletableFuture<Message>> futures = messages.stream()
						.map(message -> CompletableFuture.supplyAsync(() -> {
							Message messageInfo = null;
							try {
								return service.users().messages().get("me", message.getId()).execute();
							} catch (Exception e) {
								log.error("Error while parsing id:{}, messageInfo:{}", message.getId(), messageInfo);
								return null;
							}
						}, executorService)).collect(Collectors.toList());

				// Wait for all tasks to complete and collect the results
				CompletableFuture<List<Message>> allMessagesFuture = CompletableFuture
						.allOf(futures.toArray(new CompletableFuture[0]))
						.thenApply(v -> futures.stream().map(CompletableFuture::join) // Get the result of each
																						// CompletableFuture
								.collect(Collectors.toList()));

				// Get the result (a list of messages) from the CompletableFuture
				List<Message> newMessages = allMessagesFuture.join();
				response.addAll(newMessages);
				pageToken = gmailResponse.getNextPageToken();
			} catch (IOException e) {
				System.out.println("Error: " + e.getMessage());
			}
		} while (pageToken != null && (requestMaxResult < response.size()));
		return response;
	}

	private String generateQuery(RequestDto requestDto) {
		String query = "";
		if (StringUtils.isNotEmpty(requestDto.getQuery())) {
			query = String.format("%s", requestDto.getQuery());
		}

		String from = String.join(" OR from:", requestDto.getEmailIds());

		if (StringUtils.isNotEmpty(from)) {
			query = String.format("%s (from:%s)", query, from);
		}

		return query;
	}

	private void setReadableMessageBody(Message message) {
		if (message == null || message.getPayload() == null || message.getPayload().getBody() == null) {
			return; // Handle case where message or its body is null
		}

		try {
			MessagePart part = message.getPayload();
			part.setParts(null);

			MessagePartBody body = part.getBody();
			String data = body.getData();
			byte[] bodyBytes = Base64.decodeBase64(data);
			data = new String(bodyBytes, StandardCharsets.UTF_8);
			body.setData(data);
		} catch (Exception e) {
			log.error("Error at setReadableMessageBody :{}", e.getMessage());
		}

	}

	public Set<String> getEmailAddressesFromInbox(RequestDto request) throws IOException {
		Set<String> emailAddresses = new HashSet<>();
		List<Message> messages = getMessages(request);
		for (Message message : messages) {
			String emailId = getSenderEmailIdFromMessage(message);
			if(!StringUtils.isEmpty(emailId) && emailId != "unknown") {
				emailAddresses.add(emailId.toLowerCase());
			}
		}
		return emailAddresses;
	}

	@SuppressWarnings("unused")
	private String randomColor() {
		List<String> colors = Arrays.asList("#000000", "#434343", "#666666", "#999999", "#cccccc", "#efefef", "#f3f3f3",
				"#ffffff", "#fb4c2f", "#ffad47", "#fad165", "#16a766", "#43d692", "#4a86e8", "#a479e2", "#f691b3",
				"#f6c5be", "#ffe6c7", "#fef1d1", "#b9e4d0", "#c6f3de", "#c9daf8", "#e4d7f5", "#fcdee8", "#efa093",
				"#ffd6a2", "#fce8b3", "#89d3b2", "#a0eac9", "#a4c2f4", "#d0bcf1", "#fbc8d9", "#e66550", "#ffbc6b",
				"#fcda83", "#44b984", "#68dfa9", "#6d9eeb", "#b694e8", "#f7a7c0", "#cc3a21", "#eaa041", "#f2c960",
				"#149e60", "#3dc789", "#3c78d8", "#8e63ce", "#e07798", "#ac2b16", "#cf8933", "#d5ae49", "#0b804b",
				"#2a9c68", "#285bac", "#653e9b", "#b65775", "#822111", "#a46a21", "#aa8831", "#076239", "#1a764d",
				"#1c4587", "#41236d", "#83334c", "#464646", "#e7e7e7", "#0d3472", "#b6cff5", "#0d3b44", "#98d7e4",
				"#3d188e", "#e3d7ff", "#711a36", "#fbd3e0", "#8a1c0a", "#f2b2a8", "#7a2e0b", "#ffc8af", "#7a4706",
				"#ffdeb5", "#594c05", "#fbe983", "#684e07", "#fdedc1", "#0b4f30", "#b3efd3", "#04502e", "#a2dcc1",
				"#c2c2c2", "#4986e7", "#2da2bb", "#b99aff", "#994a64", "#f691b2", "#ff7537", "#ffad46", "#662e37",
				"#ebdbde", "#cca6ac", "#094228", "#42d692", "#16a765");

		Random random = new Random();
		int index = random.nextInt(colors.size());
		return colors.get(index);
	}

}
