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
package controllers;

import controllers.de.fuhsen.wrappers.RestApiWrapperTrait;
import controllers.de.fuhsen.wrappers.security.RestApiOAuthTrait;
import controllers.de.fuhsen.wrappers.security.TokenManager;
import play.mvc.*;
import views.html.*;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Application extends Controller {

    private String keyword;

    public Result index() {
        return ok(index.render());
    }

    public Result TokenLifeLength(String wrapperId) {
        String life_length = "{ \"life_length\" : \""+TokenManager.getTokenLifeLength(wrapperId)+"\" }";
        return ok(life_length);
    }

    public Result results() {
        return ok(results.render());
    }

    public Result details() {
        return ok(details.render());
    }

    public Result getKeyword(){
        String json_res = "{ \"keyword\" : \""+this.keyword+"\" }";
        return ok(json_res);
    }

    public String javaRequest(RestApiOAuthTrait wrapper, String apiUrl) {
        try
        {
            OAuthConsumer consumer = new DefaultOAuthConsumer(wrapper.oAuthConsumerKey().key(),wrapper.oAuthConsumerKey().secret());
            consumer.setTokenWithSecret(wrapper.oAuthRequestToken().token(),wrapper.oAuthRequestToken().secret());

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            consumer.sign(conn);

            if (conn.getResponseCode() != 200) {
                System.err.println("NOT OK "+conn.getResponseCode()+" "+conn.getResponseMessage());
                return "NOT OK-"+conn.getResponseCode()+"-"+conn.getResponseMessage();
            }
            else {
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        (conn.getInputStream())));

                String finalOutput = "";
                String output;
                System.out.println("Output from Server .... \n");
                while ((output = br.readLine()) != null) {
                    finalOutput += output;
                }

                conn.disconnect();
                return finalOutput;
            }

        }catch(Exception e)
        {
            System.err.println("NOT OK - Exception "+e.getMessage());
            return "NOT OK - Exception "+e.getMessage();
        }

    }
}


