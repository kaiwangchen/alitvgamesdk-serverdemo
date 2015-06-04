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

(function() {
  var crypto = require('crypto');
  var iconv = require('iconv-lite');

  var BaodianHelper;

  BaodianHelper = (function() {
    // constructor
    function BaodianHelper (baodianSecret) {
      this._secret = baodianSecret;
    }

    // Builds the query string. Parameter values are URL encoded in this method.
    BaodianHelper.prototype.build_query_string = function (params) {
      if (params.hasOwnProperty("sign")) {
        throw new Error("Trying to build from params already having a sign");
      }
      var qstring = {"v": "sign=" + this._sign(params)};
      Object.keys(params).sort().forEach(function (key) {
        var value = params[key];
        if (value === undefined || value.length === 0) {
          throw new Error("Missing value for parameter " + key)
        }
        qstring.v += "&" + key + "=" + _java_urlencode(value,'gbk');
      }, qstring);
      return qstring.v;
    }
   //
   // Warning: You should take the GBK bytes of the flat string to calculate sign.
    BaodianHelper.prototype.build_flat_string = function (params) {
      return this._flatten(params);
    }
     
    // Verify the sign in the parameters.
    BaodianHelper.prototype.verify = function (params) {
      var provided = params["sign"];
      if (provided)
        return this._sign(params) === provided;
      else
        throw new Error("Trying to verify params having no proper sign");
    }
    
    // private
    BaodianHelper.prototype._sign = function (params) {
      var flat = this._flatten(params);
      // js use unicode, see https://mathiasbynens.be/notes/javascript-encoding
      var bytes = iconv.encode(flat.toString("binary"), 'gbk');
      var md5 = crypto.createHash('md5');
      md5.update(bytes);
      return md5.digest("hex");
    }
    BaodianHelper.prototype._flatten = function (params) {
      var flat = {"v": ""};
      // dictionary order
      Object.keys(params).sort().forEach(function (key) {
        if (key !== "sign")
          this.v += key + params[key];
      },flat);
      return flat.v + this._secret; // secret appended
    }

    // http://docs.oracle.com/javase/7/docs/api/java/net/URLEncoder.html
    // https://cnodejs.org/topic/50fb0178df9e9fcc58c565c9
    var _java_urlencode = function (s, enc) {
      var a = s.split('');
      var encodeStr = '';
      for(var i=0; i<a.length; i++) {
        var c = a[i];
        if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
          || c === '.' || c === '-' || c === '*' || c === '_') {
          encodeStr += c;
        }
        else if (c === ' ') {
          encodeStr += '+';
        }
        else {
          var buf = iconv.encode(c, enc);
          for(var j = 0; j < buf.length; j++) {
            encodeStr += '%' + buf[j].toString(16).toUpperCase();
          }
        }
      }
      return encodeStr;
    }

    return BaodianHelper;
  })();

  // exports class
  module.exports = BaodianHelper;

  // main function
  if (require.main === module) {
    var argv = process.argv;
    if (argv < 5 || argv.length % 2 === 0) {
      console.log("Usage:\n\tnode BaodianHelper secret key1 value1 key2 value2 ...");
      process.exit(1);
    }
    var secret = argv[2];  // Baodian secret
    var params = {};
    for (var i=3; i<argv.length; i+=2) {
      params[argv[i]] = argv[i+1];
    }

    // var BaodianHelper = require("./BaodianHelper.js")
    var helper = new BaodianHelper(secret);
    var queryString = helper.build_query_string(params);
    var gbkString = helper.build_flat_string(params);
    console.log("Convert the string to GBK, then calculate md5 checksum as sign: ");
    console.log("    " + gbkString);
    console.log("The final HTTP query sting is: ");
    console.log("    " + queryString);
    console.log()
    console.log("Tips: ");
    console.log("    - The ts parameter is in milliseconds.");
    console.log("    - Make sure ts is up to date, otherwise, you get SIGNATURE_EXPIRED.");
    console.log("    - All parameter names are in lower case.");
    console.log("    - Missing required parameters gets ILLEGAL_ARGUMENT.");
    console.log("    - http://gateway.6uu.com/coin/checkUserAuthCode.action");
    console.log("    - http://gateway.6uu.com/coin/applyConsume.action");
  }
}).call(this);
