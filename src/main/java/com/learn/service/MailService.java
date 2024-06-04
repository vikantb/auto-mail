package com.learn.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.ModifyMessageRequest;

import lombok.extern.slf4j.Slf4j;

/* class to demonstrate use of Gmail list labels API */
@Service
@Slf4j
public class MailService {

	private Gmail service;

	@Value("${server.port}")
	private static Integer PORT;

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
	private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);
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
		// Build a new authorized API client service.
		NetHttpTransport HTTP_TRANSPORT;
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
					.setApplicationName(APPLICATION_NAME).build();
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
		return service;
	}

	public void deleteMessageFromUser(String sender) throws IOException {
		List<Message> messages = getMessagesFromInbox(sender);
		getOrCreateLabelId("BIN");
		for (Message message : messages) {
//			moveMessageToLabel(message.getId(), getOrCreateLabelId("BIN"));
			trashMessage(message.getId());
		}

		System.out.println("All emails from sender " + sender + " moved to removed to bin.");
	}

	private List<Message> getMessagesFromInbox(String sender) {
		// Search for messages from the specified sender in the inbox
		String query = "from:" + sender + " in:inbox";

		Gmail service = getGmailService();
		ListMessagesResponse response;
		try {
			response = service.users().messages().list("me").setQ(query).execute();
			return response.getMessages();
		} catch (IOException e) {
			System.out.println("Error: " + e.getMessage());
		}
		return null;
	}

	private void printMessage(String sender) throws IOException {
		// Get the full message
		List<Message> messages = getMessagesFromInbox(sender);
		// Print the email content
		for (Message message : messages) {
			if (message.getPayload() != null) {
				printMessageParts(message.getPayload().getParts());
			}
		}

	}

	private void printMessageParts(List<MessagePart> parts) {
		if (parts != null) {
			for (MessagePart part : parts) {
				if (part.getParts() != null) {
					printMessageParts(part.getParts());
				} else {
					String mimeType = part.getMimeType();
					if (mimeType.equals("text/plain") || mimeType.equals("text/html")) {
						MessagePartBody body = part.getBody();
						byte[] decodedBytes = java.util.Base64.getDecoder().decode(body.getData());
						String text = new String(decodedBytes);
						System.out.println(text.length() + " Body: " + text);
//                        return text;
					}
				}
			}
		}
	}

	private void moveMessageToLabel(String messageId, String targetLabelId) throws IOException {
		ModifyMessageRequest mods = new ModifyMessageRequest().setAddLabelIds(Collections.singletonList(targetLabelId))
				.setRemoveLabelIds(Collections.singletonList("INBOX"));

		service.users().messages().modify("me", messageId, mods).execute();
	}

	private void trashMessage(String messageId) throws IOException {
		service.users().messages().trash("me", messageId).execute();
	}

	public String getOrCreateLabelId(String labelName) throws IOException {
		ListLabelsResponse listResponse = service.users().labels().list("me").execute();
		List<Label> labels = listResponse.getLabels();

		for (Label label : labels) {
			System.out.print(" " + label.getName() + "_id:" + label.getId());
			if (label.getName().equalsIgnoreCase(labelName)) {
				return label.getId();
			}
		}
		return null;

		// If label does not exist, create it
//		Label newLabel = new Label().setName(labelName).setLabelListVisibility("labelShow")
//				.setMessageListVisibility("show"); // Optional: Set label color
//
//		Label createdLabel = service.users().labels().create("me", newLabel).execute();
//		return createdLabel.getId();
	}

	public Set<String> getEmailAddressesFromInbox() throws IOException {
		Gmail service = getGmailService();
		Set<String> emailAddresses = new HashSet<>();
		String user = "me";

		// Initialize the token for pagination
		String pageToken = null;
		do {
			// Get list of messages from inbox
			ListMessagesResponse response = service.users().messages().list(user).setQ("in:inbox")
					.setPageToken(pageToken).execute();
			List<Message> messages = response.getMessages();

			if (messages != null) {
				messages = messages.stream().filter(d -> d != null).collect(Collectors.toList());
				System.out.println("Total messages: " + messages.size());

				for (Message message : messages) {
				 CompletableFuture.supplyAsync((d)-> getEmailAddress(message));
				}
			}
			System.out.println("emailAddresses size: " + emailAddresses.size());
			// Update the token for the next page of results
			pageToken = response.getNextPageToken();
		} while (pageToken != null);

		return emailAddresses;
	}
	
	private String getEmailAddress(Message message) {
		try {
			Message fullMessage = service.users().messages().get("me", message.getId()).execute();
			List<MessagePartHeader> headers = fullMessage.getPayload().getHeaders();
			for (MessagePartHeader header : headers) {
				try {
					if (header.getName().equalsIgnoreCase("From")) {
						String from = header.getValue();
						// Extract email address from the "From" header
						String email = from.substring(from.indexOf('<') + 1, from.indexOf('>'));
						return email;
					}
				} catch (Exception e) {
//					System.out.println("Error: " + e.getMessage());
				}
			}
		} catch (Exception e) {
//			System.out.println("Error: " + e.getMessage());
		}
		return null;
	}
}
