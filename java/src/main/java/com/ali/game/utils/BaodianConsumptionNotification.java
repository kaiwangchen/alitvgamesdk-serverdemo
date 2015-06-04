package com.ali.game.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.http.HttpServletRequest;

import com.ali.game.utils.BaodianHelper;
import com.alibaba.fastjson.JSON;

public class BaodianConsumptionNotification {

	private static final String ENC = "GBK";

	private static Map<String, String> parseKV(String encodedQueryString, String enc) throws UnsupportedEncodingException {
		Map<String, String> result = new HashMap<String, String>();
		String decoded = java.net.URLDecoder.decode(encodedQueryString, enc);
		// convention from http://en.wikipedia.org/wiki/Query_string
		String[] parameters = decoded.split("&");
		// however, don't take bare parameters and duplications
		for (String p : parameters) {
			if (p.isEmpty()) {
				continue;
			}
			int pos = p.indexOf('=');
			if (pos == -1 || pos == 0 || pos+1 == p.length()) {
				throw new IllegalArgumentException("Found non kv pair " + p + " in " + encodedQueryString);
			}
			String[] kv = p.split("=");
			if (result.get(kv[0]) != null) {
				throw new IllegalArgumentException("Found duplicated parameter " + kv[0] + " in " + encodedQueryString);
			}
			result.put(kv[0], kv[1]);
		}
		return result;
	}

	private String appOrderId;
	private long baodianOrderId;
	private long totalAmount;
	private long creditAmount;
	private long msTimestamp;
	private boolean isSuccess;
	private String errorCode;

	private String encodedQueryString;
	private boolean verified = false;
	private Map<String, String> paramsToVerify;

	private static  Map<String,Boolean> optionMap = new HashMap<String, Boolean>();
	static {
                //             param,           required
		optionMap.put("app_order_id", 	true);
		optionMap.put("coin_order_id", 	true);
		optionMap.put("consume_amount", true);
		optionMap.put("credit_amount", 	false);
		optionMap.put("ts", 		true);
		optionMap.put("is_success", 	true);
		optionMap.put("error_code", 	false);
		optionMap.put("sign", 		true);
	}

	public String getAppOrderId() { return appOrderId; }
	public long getBaodianOrderId() { return baodianOrderId; }
	public long getTotalAmount() { return totalAmount; }
	public long getCreditAmount() { return creditAmount; }
	public long getMilliseconds() { return msTimestamp; }
	public boolean isSuccess() { return isSuccess; }
	public String errorCode() { return errorCode; }

	private BaodianConsumptionNotification () {
	}

	public static BaodianConsumptionNotification buildFrom(HttpServletRequest request) {
		String queryString = request.getQueryString();
		if (queryString == null || queryString.isEmpty()) {
			throw new IllegalArgumentException("No query string found in request");
		}

		// This is a workaround to meet the GBK assumption. We cannot use request.getParameter()
		// because it is already decoded by servlet container with its default character settings.
		Map<String, String> kvMap;
		try {
			kvMap = parseKV(queryString, ENC);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		}

		// ensure parameter presence
		Map<String, String> params = new HashMap<String,String>();
		for (String paramName : optionMap.keySet()) {
			String value = kvMap.get(paramName);
			boolean paramIsRequired = optionMap.get(paramName);
			if (paramIsRequired) {
				// required parameter must be present, and non-empty
				if (value == null || value.isEmpty()) {
					throw new IllegalArgumentException("Missing required parameter " + paramName);
				}
				params.put(paramName, value);
			}
			else {
				// non-empty optional parameter affects signature
				if (value != null && !value.isEmpty()) {
					params.put(paramName, value);
				}
			}
		}
		if (!kvMap.get("is_success").equals("T") && kvMap.get("error_code") == null) {
			throw new IllegalArgumentException("Missing error_code in a failure notification");
		}
		
		BaodianConsumptionNotification notification = new BaodianConsumptionNotification();
		notification.appOrderId = params.get("app_order_id");
		notification.baodianOrderId = Long.parseLong(params.get("coin_order_id"));
		notification.totalAmount = Long.parseLong(params.get("consume_amount"));
		notification.creditAmount = Long.parseLong(params.get("credit_amount"));
		notification.msTimestamp = Long.parseLong(params.get("ts"));
		notification.isSuccess = (params.get("is_success").equals("T")) ? true : false;
		notification.errorCode = notification.isSuccess ? "" : params.get("error_code");
		notification.encodedQueryString = queryString;
		notification.paramsToVerify = params;
		return notification; // not verified yet
	}


	public boolean verify(String baodianSecret) {
		// signature validation
		BaodianHelper helper = new BaodianHelper(baodianSecret);
		try {
			return verified = helper.verify(paramsToVerify);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public String accept() {
		Map<String, Comparable> resultMap = new TreeMap<String, Comparable>();
		resultMap.put("is_success", "T");
		resultMap.put("app_order_id", appOrderId);
		resultMap.put("coin_order_id", baodianOrderId);
		return toJSONString(resultMap);
	}
	public String reject(String msg) {
		Map<String, Comparable> resultMap = new TreeMap<String, Comparable>();
		resultMap.put("is_success", "F");
		resultMap.put("error_code", "FAIL");
		resultMap.put("msg", msg);
		resultMap.put("app_order_id", appOrderId);
		resultMap.put("coin_order_id", baodianOrderId);
		return toJSONString(resultMap);
	}
	public static String reject(HttpServletRequest request, String msg) {
		Map<String, Comparable> resultMap = new TreeMap<String, Comparable>();
		resultMap.put("is_success", "F");
		resultMap.put("error_code", "FAIL");
		resultMap.put("msg", msg);
		// copy from request instead
		try {
			resultMap.put("app_order_id", request.getParameter("app_order_id"));
		} catch (Exception e1) {
			resultMap.put("app_order_id", "0");
		}
		try {
			resultMap.put("coin_order_id", Long.parseLong(request.getParameter("coin_order_id")));
		} catch (Exception e2) {
			resultMap.put("coin_order_id", 0);
		}
		return toJSONString(resultMap);
	}
	private static String toJSONString(Map<String, Comparable> kvMap) {
		return JSON.toJSONString(kvMap);
	}
}
