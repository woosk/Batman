package com.gretec.batman;

public class SimpleUtils {

	private static Object mSyncObject=new Object();

	/**
	 * String -> Hex
	 * 
	 * @param s
	 * @return
	 */
	public static String stringToHex(String s) {
		String str = "";
		for (int i = 0; i < s.length(); i++) {
			int ch = (int) s.charAt(i);
			String s4 = Integer.toHexString(ch);
			if (s4.length() == 1) {
				s4 = '0' + s4;
			}
			str = str + s4 + " ";
		}
		return str;
	}

	/**
	 * Hex -> String
	 * 
	 * @param s
	 * @return
	 */

	/**
	 * Hex -> Byte
	 * 
	 * @param s
	 * @return
	 * @throws Exception
	 */

	/**
	 * Byte -> Hex
	 * 
	 * @param bytes
	 * @return
	 */
	public static String byteToHex(byte[] bytes, int count) {
		StringBuffer sb = new StringBuffer();
		synchronized (mSyncObject) {
			for (int i = 0; i < count; i++) {
				String hex = Integer.toHexString(bytes[i] & 0xFF);
				if (hex.length() == 1) {
					hex = '0' + hex;
				}
//				Log.d("MonitorActivity",i+":"+hex);
				sb.append(hex).append(" ");
			}
		}
		return sb.toString();
	}
}
