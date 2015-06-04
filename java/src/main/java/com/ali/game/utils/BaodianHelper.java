package com.ali.game.utils;
/*
  Helper class to sign or verify HTTP requests to/from Baodian server.

  Copyright (c) 2015, alibaba
  http://open.aliplay.com
  All rights reserved.
  
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:
      * Redistributions of source code must retain the above copyright
        notice, this list of conditions and the following disclaimer.
      * Redistributions in binary form must reproduce the above copyright
        notice, this list of conditions and the following disclaimer in the
        documentation and/or other materials provided with the distribution.
      * Neither the name of the <organization> nor the
        names of its contributors may be used to endorse or promote products
        derived from this software without specific prior written permission.
  
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

// 20150507b

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public class BaodianHelper {

  public static final String ENDPOINT_APPLY_CONSUME = "http://gateway.6uu.com/coin/applyConsume.action";
  public static final String ENDPOINT_CHECK_AUTH_CODE = "http://gateway.6uu.com/coin/checkUserAuthCode.action";

  private static final String SIGN_KEY = "sign";

  private String secret; // baodian secret

  public BaodianHelper(String secret) {
    this.secret = secret;
  }

  /**
   * Builds the query string. Parameter values are URL encoded in this method.
   */
  public String buildQueryString(Map<String,String> params) throws UnsupportedEncodingException, NoSuchAlgorithmException {
    if (params.containsKey(SIGN_KEY)) {
      throw new IllegalArgumentException("Trying to build from params already having a sign");
    }
    StringBuilder sb = new StringBuilder(SIGN_KEY);
    sb.append("=");
    sb.append(sign(params));
    for (Map.Entry<String, String> p : params.entrySet()) {
      String key = p.getKey();
      String value = p.getValue();
      if (value == null || value.isEmpty()) {
        throw new IllegalArgumentException("Missing value for parameter " + key);
      }
      value = java.net.URLEncoder.encode(value,"GBK"); // XXX: GBK is assumed
      sb.append("&").append(key).append("=").append(value);
    }
    return sb.toString();
  }

  /**
   * Warning: You should take the GBK bytes of the flat string to calculate sign.
   */
  public String buildFlatString(Map<String,String> params) {
    return flatten(params);
  }

  /**
   * Verify the sign in the parameters.
   */
  public boolean verify(Map<String, String> params) throws UnsupportedEncodingException, NoSuchAlgorithmException {
    String provided = params.get(SIGN_KEY);
    if(provided == null || provided.isEmpty()) {
      throw new IllegalArgumentException("Trying to verify params having no proper sign");
    }
    return sign(params).equals(provided);
  }

  private String sign(Map<String, String> params) throws UnsupportedEncodingException, NoSuchAlgorithmException {
    byte[] bytes = flatten(params).getBytes("GBK"); // XXX GBK is assumed
    return md5(bytes);
  }

  private String flatten(Map<String, String> params) {
    StringBuilder s = new StringBuilder();
    // entries in dictionary order
    for (Map.Entry<String, String> p: new TreeMap<String,String>(params).entrySet()) {
      if (!p.getKey().equals(SIGN_KEY)) {
        s.append(p.getKey()).append(p.getValue());
      }
    }
    s.append(secret); // secret appended
    return s.toString();
  }
  
  // md5 takes bytes rather than a string (of characters)
  private static String md5(byte[] bytes) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    md.update(bytes);
    byte[] bs = md.digest();
    StringBuffer sb = new StringBuffer();
    for (int i=0;i<bs.length;i++) {
      int v = bs[i] & 0xff;
      if( v < 16 ) {
        sb.append(0);
      }
      sb.append(Integer.toHexString(v));
    }
     return sb.toString();
  }

  public static void main(String[] args) throws UnsupportedEncodingException, NoSuchAlgorithmException {
    if (args.length % 2 == 0) {
      System.out.println("Usage:\n\tjava BaodianHelper secret key1 value1 key2 value2 ...");
      System.exit(1);
    }
    String secret = args[0];  // Baodian secret
    Map<String,String> params = new TreeMap<String,String>();
    for (int i = 1; i < args.length; i+=2) {
      params.put(args[i],args[i+1]);
    }

    BaodianHelper helper = new BaodianHelper(secret);
    String queryString = helper.buildQueryString(params);
    String gbkString = helper.buildFlatString(params);
    System.out.println("Convert the string to GBK, then calculate md5 checksum as sign: ");
    System.out.println("    " + gbkString);
    System.out.println("The final HTTP query sting is: ");
    System.out.println("    " + queryString);
    System.out.println();
    System.out.println("Tips: ");
    System.out.println("    - The ts parameter is in milliseconds.");
    System.out.println("    - Make sure ts is up to date, otherwise, you get SIGNATURE_EXPIRED.");
    System.out.println("    - All parameter names are in lower case.");
    System.out.println("    - Missing required parameters gets ILLEGAL_ARGUMENT.");
    System.out.println("    - " + BaodianHelper.ENDPOINT_CHECK_AUTH_CODE);
    System.out.println("    - " + BaodianHelper.ENDPOINT_APPLY_CONSUME);
  }
}