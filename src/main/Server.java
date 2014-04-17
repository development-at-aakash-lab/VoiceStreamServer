package main;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

class Server {

	static AudioInputStream ais;
	static AudioFormat audioFormat;
	static int port = 50005;
	static int sampleRate = 44100;
	static SourceDataLine sourceDataLine;
	static boolean streaming = true;
	static int i = 0;
	static Queue<InetAddress> q = new LinkedList<>();
	private static InetAddress currentSpeakerAddress;
	static Set<InetAddress> h = new HashSet<>();
	public static final String PERMISSION_TEXT = "You may start talking";

	public static void main(String args[]) throws Exception {

		DatagramSocket serverSocket = new DatagramSocket(port);

		byte[] receiveData = new byte[8192];

		audioFormat = new AudioFormat(sampleRate, 16, 1, true, false);

		sourceDataLine = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, audioFormat));
		sourceDataLine.open(audioFormat);
		sourceDataLine.start();
		
		/*FloatControl volumeControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
		System.out.println("Working");
		volumeControl.setValue(1.00f);*/

		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		ByteArrayInputStream baiss = new ByteArrayInputStream(receivePacket.getData());
		while (streaming == true) {
			serverSocket.receive(receivePacket);
			String requestText = new String(receivePacket.getData());
			InetAddress requestAddress = receivePacket.getAddress();
			if (i == 0) {
				currentSpeakerAddress = requestAddress;
				notifyToTalk(currentSpeakerAddress);
			}
			if (requestText.contains("Raise Hand")) {
				if (currentSpeakerAddress.equals(requestAddress)) {
					System.out.println(requestAddress.getHostAddress() + " is online");
				} else {
					storeID(requestAddress);
				}
			} else if (requestText.contains("Withdraw")) {
				if (currentSpeakerAddress.equals(requestAddress)) {
					if (h.isEmpty()) {
						break;
					}
					currentSpeakerAddress = getNext();
				} else {
					if (h.remove(requestAddress)) {
						q.remove(requestAddress);
					}
				}
			} else if (currentSpeakerAddress.equals(requestAddress)) {
				ais = new AudioInputStream(baiss, audioFormat, receivePacket.getLength());
				toSpeaker(receivePacket.getData());
				System.out.println(i++ + " " + receivePacket.getLength());
			}
		}
		sourceDataLine.drain();
		sourceDataLine.close();
	}

	public static void toSpeaker(byte soundbytes[]) {
		try {
			sourceDataLine.write(soundbytes, 0, soundbytes.length);
		} catch (Exception e) {
			System.out.println("Not working in speakers...");
		}
	}

	public static InetAddress getNext() {
		InetAddress next = q.remove();
		h.remove(next);
		notifyToTalk(next);
		System.out.println("Next user is " + next.getHostAddress());
		return next;
	}

	public static void storeID(InetAddress address) {
		System.out.println("Raise Hand by " + address.getHostAddress());
		if (h.add(address)) {
			q.add(address);
		}
	}

	private static void notifyToTalk(final InetAddress nextSpeaker) {
		final byte[] request = (PERMISSION_TEXT).getBytes();
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					new DatagramSocket().send(new DatagramPacket(request, request.length, nextSpeaker, port));
					System.out.println("Permission given to " + nextSpeaker.getHostAddress());
				} catch (SocketException | UnknownHostException e) {
				} catch (IOException e) {
				}
			}
		}).start();
	}
}
