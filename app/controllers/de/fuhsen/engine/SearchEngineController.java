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
package controllers.de.fuhsen.engine;

import play.*;
import play.mvc.*;
import views.html.*;

//FuhSen Engine
import de.fuhsen.engine.*;

//Json
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;

//Test
import javax.inject.Inject;

//Jena imports
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import play.mvc.*;
import play.libs.ws.*;
import play.libs.F.Function;
import play.libs.F.Promise;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

public class SearchEngineController extends Controller {

    @Inject WSClient ws;
    // timeout
    private Long t = 100000L;

    //@BodyParser.Of(Json.class)
    public Result index() {
        
        //linkeddatawrapper-dataextraction.rhcloud.com/ldw/googleplus/search.html?query=Lionel Messi&numResults=50
        
        WSRequest request = ws.url("http://linkeddatawrapper-dataextraction.rhcloud.com/ldw/googleplus/search.html");
        WSRequest complexRequest = request.setQueryParameter("query", "Lionel Messi")
                                            .setQueryParameter("numResults", "50");
        
        //Promise<JsonNode> jsonPromise = complexRequest.get().map(response -> {
        //    return response.asJson();
        //});
        
        Promise<WSResponse> responsePromise = complexRequest.get();
        
        Model model = ModelFactory.createDefaultModel();
        
        WSResponse result = responsePromise.get(t);
        String modelText = result.getBody();
        Model resultsGoogle = ModelFactory.createDefaultModel();
        
        try {
		    
		    resultsGoogle.read(new ByteArrayInputStream(modelText.getBytes("UTF-8")), null, "JSON-LD");
		
        }catch(UnsupportedEncodingException ex){
            Logger.error("Encoding exception...");
        }
		
		model.add(resultsGoogle);
        
        //ToDo  move all the code above inside QueryExecutor
        //QueryExecutor qe = new QueryExecutor();
        //qe.search(null);

        JsonNode json = Json.newObject()
                    .put("key3", "value3")
                    .put("key4", "value4");
        
        return ok(json);
    }

}
