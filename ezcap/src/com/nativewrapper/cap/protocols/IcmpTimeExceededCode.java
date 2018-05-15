package com.nativewrapper.cap.protocols;

import java.util.Map;
import java.util.HashMap;

public enum IcmpTimeExceededCode {

	// http://www.infraexpert.com/info/5.0adsl.htm
	// ICMP Type 11： Time Exceeded
	TTL       (0, "Time to Live exceeded in Transit"),
	FRAGTIME  (1, "Fragment Reassembly Time Exceeded");


	private int mCode;
	private String mDesc;
 
	private IcmpTimeExceededCode (int code, String desc) {
		mCode = code;
		mDesc = desc;
	}

	public int getCode () {
		return mCode;
	}
 

	private static final Map <Integer, String> mDescMap = new HashMap <Integer, String>();
	static {
		for (IcmpTimeExceededCode entry : values()) {
			mDescMap.put (entry.mCode, entry.mDesc);
		}
	}
	public static Map get () {
		return mDescMap;
	}

	public static String getDesc (int code) {
		if (mDescMap.containsKey(code)) {
			return mDescMap.get (code);
		} else {
			return "unknown.";
		}
	}
}
