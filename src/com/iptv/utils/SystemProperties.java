package com.iptv.utils;

import java.lang.reflect.Method;

public class SystemProperties {
	private static Class<?> mClassType = null;
	private static Method mGetMethod = null;

	private static void init() {
		try {
			if (mClassType == null) {
				mClassType = Class.forName("android.os.SystemProperties");
				mGetMethod = mClassType.getDeclaredMethod("get", String.class);
			}
		} catch (Exception e) {
		}
	}

	public static String get(String key, String defaultValue) {
		init();
		String value = null;
		try {
			value = (String) mGetMethod.invoke(mClassType, key);
		} catch (Exception e) {
		}
		return value == null ? defaultValue : value;
	}
}
