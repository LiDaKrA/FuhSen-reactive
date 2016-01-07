/*
 * Copyright (C) 2014 EIS Uni-Bonn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package controllers.de.fuhsen.wrappers;

import java.util.Map;

//Json
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;

import com.typesafe.config.ConfigFactory;

import external.services.OAuthService;
import external.services.TwitterOAuthService;

import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.libs.oauth.OAuth.RequestToken;
import play.mvc.Controller;
import play.mvc.Result;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class Twitter extends Controller
{   
    // timeout
    private Long t = 100000L;
    
    public Result index() {
        
        return ok("Ok");
        
    }
    
	private static final OAuthService service = new TwitterOAuthService(
			ConfigFactory.load().getString("consumer.key"),
			ConfigFactory.load().getString("consumer.secret")
			);
    
    public Result searchTwitter()
    {
    	String callbackUrl = controllers.de.fuhsen.wrappers.routes.Twitter.searchTwitterCallback().absoluteURL(request());
    	
    	System.out.println("callbackUrl: "+callbackUrl);
    	
    	Tuple<String, RequestToken> t = service.retrieveRequestToken(callbackUrl);
    	flash("request_token", t._2.token);
    	flash("request_secret", t._2.secret);
    	
    	Map<String, String[]> params = Controller.request().queryString();
    	String search = params.get("query")[0];

    	flash("search", search);
    	return redirect(t._1);
    }
    
    public Result searchTwitterCallback()
    {
        System.out.println("Executing searchTwitterCallback...");
        
    	RequestToken token = new RequestToken(flash("request_token"), flash("request_secret"));
    	String authVerifier = request().getQueryString("oauth_verifier");  	
    	String search = flash("search");
  
    	Promise<JsonNode> search_results_json = service.getSearchResults(token, authVerifier, search);    	
    	String json_results = search_results_json.get(t).toString();
    	String json_final = json_results.toString().substring(1, json_results.toString().length()-1);
    	
    	System.out.println(Json.prettyPrint(Json.parse(json_final)));
    	
    	return ok(""); 
    }
    
    
}