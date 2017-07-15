/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps.java.loref;

import java.io.File;
import java.io.IOException;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferNegotiator;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 *
 * @author lore_f
 */
public class IMUtilities {

	private String username;
	private String password;
	private String server;

	public interface IMListener {

		void onMessageReceived(String sender, String messageBody);

		void onConnected();

		void onConnectionInterrupted(Exception e);

		void onSendMessageException(String recipient, String message, Exception e);

		void onSendFileException(String recipient, String fileDef, Exception e);

		void onFTPUploadFileException(File file, Exception e);
		
		void onConnectionClosed();

	}

	private IMListener imListener;

	public void setIMListener(IMListener listener) {
		imListener = listener;
	}

	AbstractXMPPConnection connection = null;
	ChatManager chatManager = null;

	public IMUtilities(String usern, String passw, String servr) {

		username = usern;
		password = passw;
		server = servr;

	}

	public boolean connect() {

		try {

			XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
			configBuilder.setUsernameAndPassword(username, password);
			configBuilder.setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible);
			configBuilder.setXmppDomain(server);
			configBuilder.setSendPresence(true);
			configBuilder.setKeystoreType(null);

			connection = new XMPPTCPConnection(configBuilder.build());
			connection.addConnectionListener(new ConnectionListener() {

				@Override
				public void reconnectionSuccessful() {
					// TODO Auto-generated method stub

				}

				@Override
				public void reconnectionFailed(Exception e) {
					// TODO Auto-generated method stub

				}

				@Override
				public void reconnectingIn(int seconds) {
					// TODO Auto-generated method stub

				}

				@Override
				public void connectionClosedOnError(Exception e) {

					if (imListener != null)
						imListener.onConnectionInterrupted(e);

				}

				@Override
				public void connectionClosed() {
					// TODO Auto-generated method stub
					if (imListener != null)
						imListener.onConnectionClosed();
				}

				@Override
				public void connected(XMPPConnection connection) {

					login();

				}

				@Override
				public void authenticated(XMPPConnection connection, boolean resumed) {

					createChatManager();

				}
			});

			connection.connect();
			return true;

		} catch (Exception e) {

			return false;
		}

	}

	private void login() {

		try {

			connection.login();
			
		} catch (SmackException | IOException | XMPPException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private void createChatManager() {
		
		chatManager = ChatManager.getInstanceFor(connection);
		
		chatManager.addIncomingListener(new IncomingChatMessageListener() {

			@Override
			public void newIncomingMessage(EntityBareJid recipient, Message message, Chat chat) {
				if (imListener != null)
					imListener.onMessageReceived(recipient.asEntityBareJidString(), message.getBody());

			}

		});

		if (imListener != null)
			imListener.onConnected();
		
	}

	public void disconnect() {

		connection.disconnect();
		
	}

	public boolean sendMessage(String recipient, String message) {

		if (connection.isConnected()) {

			try {

				EntityBareJid jid = JidCreate.entityBareFrom(recipient);
				Chat chat = chatManager.chatWith(jid);
				chat.send(message);

				return true;

			} catch (XmppStringprepException | NotConnectedException | InterruptedException e) {

				if (imListener != null)
					imListener.onSendMessageException(recipient, message, e);
				
				return false;
			}

		} else {

			return false;

		}

	}

	public boolean sendFile(String recipient, String fileDef) {

		File file = new File(fileDef);

		if (connection.isConnected() && file.exists() && file.isFile()) {

			FileTransferNegotiator.IBB_ONLY = false;
			FileTransferManager fileTransferManager = FileTransferManager.getInstanceFor(connection);

			try {
				
			OutgoingFileTransfer outgoingFileTransfer = fileTransferManager
					.createOutgoingFileTransfer(JidCreate.entityFullFrom(recipient + "/authorized-controller"));
			outgoingFileTransfer.sendFile(file, fileDef);

			return true;
			
			} catch (XmppStringprepException | SmackException e) {
				
				if (imListener != null)
					imListener.onSendFileException(recipient, fileDef, e);
				
				return false;
				
			}
						
		}
		
		return false;
	}

}
