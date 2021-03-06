package com.nativewrapper.cap;

import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.nativewrapper.cap.CapJni;
import com.nativewrapper.cap.IPacketListener;
import com.nativewrapper.cap.Interface;
import com.nativewrapper.cap.Packet;
import com.nativewrapper.cap.protocols.*;
import com.nativewrapper.types.*;


public class CapMain {
	
	static private CapJni mCap = new CapJni ();

	static private IPacketListener pktListener = new IPacketListener () {

		@Override
		public void onReceivedPacket (Packet packet) {
			System.out.println ("----------------------------------------------------");
			System.out.println (packet);

			EtherHeader eh = new EtherHeader();
			RawReader.toStruct (eh, packet.getRaw(), 0);
			System.out.println (eh);

			if (eh.getType() == EtherType.ARP.getType()) {
				EtherArp arp = new EtherArp();
				RawReader.toStruct (arp, packet.getRaw(), eh.structSize());
				System.out.println (arp);

			} else if (eh.getType() == EtherType.IP.getType()) {
				// IPv4
				IpHeader iph = new IpHeader ();
				RawReader.toStruct (iph, packet.getRaw(), eh.structSize());

				int optLen = iph.getHeaderLen() - iph.structSize();
				if (optLen < 0) {
					System.out.println ("invalid packet.");
					return;
				}

				System.out.println (iph);

				if (iph.getProto() == IpProtoNumber.ICMP.getProtoNum()) {
					// ICMP
					Icmp icmp = new Icmp ();
					RawReader.toStruct (icmp, packet.getRaw(), eh.structSize() + iph.getHeaderLen());
					System.out.println (icmp);

				} else if (iph.getProto() == IpProtoNumber.TCP.getProtoNum()) {
					// TCP
					TcpHeader tcph = new TcpHeader();
					RawReader.toStruct (tcph, packet.getRaw(), eh.structSize() + iph.getHeaderLen());
					System.out.println (tcph);

					int payloadLen = iph.getTotalLen() - iph.getHeaderLen() - tcph.getDoff();
					System.out.println ("payloadLen=[" + payloadLen + "]");
					if (payloadLen < 0) {
						System.out.println ("invalid packet.");
						return;
					} else if (payloadLen > 0) {
						int s = eh.structSize() + iph.getHeaderLen() + tcph.getDoff();
						byte [] b = Arrays.copyOfRange (packet.getRaw(), s, packet.length());
						System.out.println ("::payload dump::");
						RawReader.hexDump (b);
					} else {
						System.out.println ("header only.");
					}

				} else if (iph.getProto() == IpProtoNumber.UDP.getProtoNum()) {
					// UDP
					UdpHeader udph = new UdpHeader();
					RawReader.toStruct (udph, packet.getRaw(), eh.structSize() + iph.getHeaderLen());
					System.out.println (udph);

					int payloadLen = udph.getTotalLen() - udph.structSize();
					System.out.println ("payloadLen=[" + payloadLen + "]");
					if (payloadLen < 0) {
						System.out.println ("invalid packet.");
						return;
					} else if (payloadLen > 0) {
						int s = eh.structSize() + iph.getHeaderLen() + udph.structSize();
						byte [] b = Arrays.copyOfRange (packet.getRaw(), s, packet.length());
						System.out.println ("::payload dump::");
						RawReader.hexDump (b);
					} else {
						System.out.println ("header only.");
					}
				}
			}

//			System.out.println ("::hexDump::");
//			RawReader.hexDump (packet.getRaw());
		}
	};


	public static void main (String[] args) {

		BufferedReader stdReader = new BufferedReader (new InputStreamReader(System.in));

		if (!mCap.init()) {
			System.exit(1);
		}

		System.out.print ("\n" + mCap.getVersion() + "\n");

		List<Interface> ifList = mCap.getInterfaceList();
		if (ifList.size() == 0) {
			System.exit(1);
		} else {
			System.out.print ("\nnetwork interfaces\n");
			for (int i = 0; i < ifList.size(); ++ i) {
				System.out.println (" - [" + ifList.get(i).getName() + "] " + ifList.get(i).getAddress());
			}
		}

		System.out.print ("Enter interface name: ");
		String nic = "";
		try {
			while (true) { 
				nic = stdReader.readLine();
				if (nic.equals("")) {
					continue;
				} else {
					System.out.println ("[" + nic + "]");
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		mCap.setInterface (nic);
//		mCap.setFilter ("tcp");

		int id = mCap.registerPacketListener (pktListener);
		System.out.println ("listener id:" + id);

		System.out.println ("### start ###");
		if (!mCap.start()) {
			System.exit(1);
		}

		String lineBuf = "";
		try {
			while (true) {
				System.out.print ("ezcap > ");

				lineBuf = stdReader.readLine();
				String line = lineBuf.trim();
				if (line.equals("")) {

				} else if (line.equals("quit")) {
					System.out.println ("### quit ###");
					break;

				} else if (line.equals("ver")) {
					System.out.println ("### version ###");
					System.out.println (mCap.getVersion());

				} else if (line.equals("start")) {
					System.out.println ("### start ###");
					if (!mCap.start()) {
						System.exit(1);
					}

				} else if (line.equals("stop")) {
					System.out.println ("### stop ###");
					mCap.stop();

				} else if (line.equals("getf")) {
					System.out.println ("### show now filter ###");
					System.out.println ("[" + mCap.getFilter() + "]");

				} else if (line.equals("clearf")) {
					System.out.println ("### clear filter ###");
					mCap.clearFilter();

				} else  {
					String filter = "";
					String regex = "^setf +";
					Pattern p = Pattern.compile(regex);
					Matcher m = p.matcher (line);
					if (m.find()) {
						filter = line.substring (m.end());
						if ((filter == null) || (filter.isEmpty())) {
							System.out.println ("invalid command... [" + line + "]");
						} else {
							System.out.println ("### set filter ### --> [" + filter + "]");
							mCap.setFilter (filter);
						}

					} else {
						System.out.println ("invalid command... [" + line + "]");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		mCap.stop();
		mCap.unregisterPacketListener (id);
		mCap.fin();

	}
}
