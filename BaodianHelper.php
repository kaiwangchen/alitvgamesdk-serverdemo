<?php
// 
// Helper class to sign or verify HTTP requests to/from Baodian server.
// Copyright (c) 2015, alibaba
// http://open.aliplay.com
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above copyright
//       notice, this list of conditions and the following disclaimer in the
//       documentation and/or other materials provided with the distribution.
//     * Neither the name of the <organization> nor the
//       names of its contributors may be used to endorse or promote products
//       derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// 

// 20150511a

define("ENDPOINT_APPLY_CONSUME", "http://gateway.6uu.com/coin/applyConsume.action");
define("ENDPOINT_CHECK_AUTH_CODE", "http://gateway.6uu.com/coin/checkUserAuthCode.action");

define("SIGN_KEY", "sign");

class BaodianHelper {


  var $_secret; // baodian secret
  var $_enc;    // character set of params.
               // Since PHP string is raw bytes, and I don't like guess.

  /**
   * baodianSecret - Baodian secret
   * enc - character set of params string
   */
  function __construct($baodianSecret, $enc = "GBK") {
    $this->_secret = $baodianSecret;
    $this->_enc = strtoupper($enc);
  }

  /**
   * Builds the query string. Parameter values are URL encoded in this method.
   */
  function build_query_string($params) {  
    if (array_key_exists(SIGN_KEY, $params)) {
      throw new Exception("Trying to build from params already having a sign");
    }
    $s = SIGN_KEY . "=" . $this->_sign($params);
    $enc = $this->_enc;
    foreach ($params as $key => $val) {
      if ($val) {
        if (strcmp($enc, "GBK") != 0) {
          $val = iconv($enc, "GBK", $val);
        }
        $s .= "&" . $key . "=" . urlencode($val);
      }
      else {
        throw new Exception("Missing value for parameter " + key);
      }
    }
    return $s;
  }

  /**
   * Warning: You should take the GBK bytes of the flat string to calculate sign.
   */
  function build_flat_string($params) {
    return $this->_flatten($params);
  }

  /**
   * Verify the sign in the parameters.
   */
  function verify($params) {
    $provided = $params[SIGN_KEY];
    if ($provided) {
      $trusted = $this->_sign($params);
      return strcmp($provided, $trusted) == 0;
    }
    else {
      throw new Exception("Trying to verify params having no proper sign");
    }
  }

  private function _sign($params) {
    $bytes = iconv($this->_enc, "GBK", $this->_flatten($params)); // XXX GBK is assumed
    return md5($bytes); // PHP md5
  }

  private function _flatten($params) {
    $keys = array_keys($params);
    // entries in dictionary order
    sort($keys, SORT_STRING);

    $s = "";
    foreach ($keys as $key) {
      $val = $params[$key];
      if (strcmp($key, SIGN_KEY) != 0) {
        $s .= $key . $val; 
      }
    }
    $s .= $this->_secret;  // secret appended
    return $s;
  }
}

//// main function

//  $c = count($argv); // $argv[0] is BaodianHelper.php
//  if ($c % 2 != 0 || $c < 4) {
//    echo "Usage:\n\tjava BaodianHelper secret key1 value1 key2 value2 ...";
//    exit(1);
//  }
//  $secret = $argv[1];  // Baodian secret
//  $params = array();
//  for ($i = 2; $i < $c; $i+=2) {
//    $params[$argv[$i]] = $argv[$i+1];
//  }
//
//  $helper = new BaodianHelper($secret, "UTF-8");
//  $queryString = $helper->build_query_string($params);
//  $gbkString = $helper->build_flat_string($params);
//  echo "Convert the string to GBK, then calculate md5 checksum as sign: \n";
//  echo "    " . $gbkString . "\n";
//  echo "The final HTTP query sting is: \n";
//  echo "    " . $queryString . "\n";
//  echo "\n";
//  echo "Tips: \n";
//  echo "    - The ts parameter is in milliseconds.\n";
//  echo "    - Make sure ts is up to date, otherwise, you get SIGNATURE_EXPIRED.\n";
//  echo "    - All parameter names are in lower case.\nb";
//  echo "    - Missing required parameters gets ILLEGAL_ARGUMENT.\n";
//  echo "    - " . ENDPOINT_CHECK_AUTH_CODE . "\n";
//  echo "    - " . ENDPOINT_APPLY_CONSUME . "\n";
