package com.iptv.parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to parse a .m3u file.
 * 
 * @author Ke
 */
public class M3UParser {
	private static final String PREFIX_EXTM3U = "#EXTM3U";
	private static final String PREFIX_EXTINF = "#EXTINF:";
	private static final String PREFIX_COMMENT = "#";
	private static final String EMPTY_STRING = "";
	private static final String ATTR_NAME = "name";
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_DLNA_EXTRAS = "dlna_extras";
	private static final String ATTR_PLUGIN = "plugin";
	private static final String ATTR_CHANNEL_NAME = "channel_name";
	private static final String ATTR_DURATION = "duration";
	private static final String ATTR_LOGO = "logo";
	private static final String ATTR_GROUP_TITLE = "group-title";
	private static final String ATTR_TVG_PREFIX = "tvg-";
	private static final String ATTR_TVG_SUFFIX = "-tvg";
	private static final String INVALID_STREAM_URL = "http://0.0.0.0:1234";

	private static M3UParser mInstance = null;
	private M3UHandler mHandler = null;
	private M3UItem mTempItem = null;

	private M3UParser() {
	}

	public static final M3UParser getInstance() {
		if (mInstance == null) {
			mInstance = new M3UParser();
		}
		return mInstance;
	}

	/**
	 * Setup a default handler to handle the m3u file parse result.
	 * 
	 * @param handler
	 *            a M3UHandler instance.
	 */
	public void setHandler(M3UHandler handler) {
		mHandler = handler;
	}

	/**
	 * Use the default handler to parse a m3u file.
	 * 
	 * @param filename
	 *            a file to be parsed.
	 */
	public void parse(String filename) {
		parse(filename, mHandler);
	}

	/**
	 * Use a specific handler to parse a m3u file.
	 * 
	 * @param filename
	 *            a file to be parsed.
	 * @param handler
	 *            a specific handler which will not change the default handler.
	 */
	public void parse(String filename, M3UHandler handler) {
		if (handler == null) { // No need do anything, if no handler.
			return;
		}
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(filename)));
			String tmp = null;
			while ((tmp = shrink(br.readLine())) != null) {
				if (tmp.startsWith(PREFIX_EXTM3U)) {
					handler.onReadEXTM3U(parseHead(shrink(tmp.replaceFirst(
							PREFIX_EXTM3U, EMPTY_STRING))));
				} else if (tmp.startsWith(PREFIX_EXTINF)) {
					// The old item must be committed when we meet a new item.
					flush(handler);
					mTempItem = parseItem(shrink(tmp.replaceFirst(
							PREFIX_EXTINF, EMPTY_STRING)));
				} else if (tmp.startsWith(PREFIX_COMMENT)) {
					// Do nothing.
				} else if (tmp.equals(EMPTY_STRING)) {
					// Do nothing.
				} else { // The single line is treated as the stream URL.
					updateURL(tmp);
				}
			}
			flush(handler);
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String shrink(String str) {
		return str == null ? null : str.trim();
	}

	private void flush(M3UHandler handler) {
		if (mTempItem != null) {
			// The invalid item must be skipped.
			if (mTempItem.getStreamURL() != null) {
				handler.onReadEXTINF(mTempItem);
			}
			mTempItem = null;
		}
	}

	private void updateURL(String url) {
		if (mTempItem != null && !INVALID_STREAM_URL.equals(url)) {
			mTempItem.setStreamURL(url.trim().split("\\s*")[0]);
		}
	}

	private void putAttr(Map<String, String> map, String key, String value) {
		map.put(key, value);
	}

	private String getAttr(Map<String, String> map, String key) {
		String value = map.get(key);
		if (value == null) {
			value = map.get(ATTR_TVG_PREFIX + key);
			if (value == null) {
				value = map.get(key + ATTR_TVG_SUFFIX);
			}
		}
		return value;
	}

	private M3UHead parseHead(String words) {
		Map<String, String> attr = parseAttributes(words);
		M3UHead header = new M3UHead();
		header.setName(getAttr(attr, ATTR_NAME));
		header.setType(getAttr(attr, ATTR_TYPE));
		header.setDLNAExtras(getAttr(attr, ATTR_DLNA_EXTRAS));
		header.setPlugin(getAttr(attr, ATTR_PLUGIN));
		return header;
	}

	private M3UItem parseItem(String words) {
		Map<String, String> attr = parseAttributes(words);
		M3UItem item = new M3UItem();
		item.setChannelName(getAttr(attr, ATTR_CHANNEL_NAME));
		item.setDuration(convert2int(getAttr(attr, ATTR_DURATION)));
		item.setLogoURL(getAttr(attr, ATTR_LOGO));
		item.setGroupTitle(getAttr(attr, ATTR_GROUP_TITLE));
		item.setType(getAttr(attr, ATTR_TYPE));
		item.setDLNAExtras(getAttr(attr, ATTR_DLNA_EXTRAS));
		item.setPlugin(getAttr(attr, ATTR_PLUGIN));
		return item;
	}

	private Map<String, String> parseAttributes(String words) {
		Map<String, String> attr = new HashMap<String, String>();
		if (words == null || words.equals(EMPTY_STRING)) {
			return attr;
		}
		Status status = Status.READY;
		String tmp = words;
		StringBuffer connector = new StringBuffer();
		int i = 0;
		char c = tmp.charAt(i);
		if (c == '-' || Character.isDigit(c)) {
			connector.append(c);
			while (++i < tmp.length()) {
				c = tmp.charAt(i);
				if (Character.isDigit(c)) {
					connector.append(c);
				} else {
					break;
				}
			}
			putAttr(attr, ATTR_DURATION, connector.toString());
			tmp = shrink(tmp.replaceFirst(connector.toString(), EMPTY_STRING));
			reset(connector);
			i = 0;
		}
		String key = EMPTY_STRING;
		boolean startWithQuota = false;
		while (i < tmp.length()) {
			c = tmp.charAt(i++);
			switch (status) {
			case READY:
				if (Character.isWhitespace(c)) {
					// Do nothing
				} else if (c == ',') {
					putAttr(attr, ATTR_CHANNEL_NAME, tmp.substring(i));
					i = tmp.length();
				} else {
					connector.append(c);
					status = Status.READING_KEY;
				}
				break;
			case READING_KEY:
				if (c == '=') {
					key = shrink(key + connector.toString());
					reset(connector);
					status = Status.KEY_READY;
				} else {
					connector.append(c);
				}
				break;
			case KEY_READY:
				if (!Character.isWhitespace(c)) {
					if (c == '"') {
						startWithQuota = true;
					} else {
						connector.append(c);
					}
					status = Status.READING_VALUE;
				}
				break;
			case READING_VALUE:
				if (startWithQuota) {
					connector.append(c);
					int end = tmp.indexOf("\"", i);
					end = end == -1 ? tmp.length() : end;
					connector.append(tmp.substring(i, end));
					startWithQuota = false;
					putAttr(attr, key, connector.toString());
					i = end + 1;
					reset(connector);
					key = EMPTY_STRING;
					status = Status.READY;
					break;
				}
				if (Character.isWhitespace(c)) {
					if (connector.length() > 0) {
						putAttr(attr, key, connector.toString());
						reset(connector);
					}
					key = EMPTY_STRING;
					status = Status.READY;
				} else {
					connector.append(c);
				}
				break;
			default:
				break;
			}
		}
		if (!key.equals(EMPTY_STRING) && connector.length() > 0) {
			putAttr(attr, key, connector.toString());
			reset(connector);
		}
		return attr;
	}

	private int convert2int(String value) {
		int ret = -1;
		try {
			ret = Integer.parseInt(value);
		} catch (Exception e) {
			ret = -1;
		}
		return ret;
	}

	private void reset(StringBuffer buffer) {
		buffer.delete(0, buffer.length());
	}

	private static enum Status {
		READY, READING_KEY, KEY_READY, READING_VALUE,
	}
}
