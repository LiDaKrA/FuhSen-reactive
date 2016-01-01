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
 
package de.fuhsen.engine;

//Java 
import java.util.ArrayList;

//Logging
import play.Logger;

//Jena imports
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

//WS imports
import javax.inject.Inject;
import play.mvc.*;
import play.libs.ws.*;
import play.libs.F.Function;
import play.libs.F.Promise;

//Json imports
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;

public class QueryExecutor {
    
    @Inject WSClient ws;
    
    
    public void search(ArrayList<String> queryStrings) {
        
        Model model = ModelFactory.createDefaultModel();
       
	    //WSRequest request = ws.url("http://linkeddatawrapper-dataextraction.rhcloud.com/ldw/googleplus/search.html?query=Lionel Messi&numResults=50");
		    
	    //Promise<JsonNode> jsonPromise = request.get().map(response -> {
        //    return response.asJson();
        //});
		    	
	    Logger.info("Search executed succesfully");
        
    }
    
}