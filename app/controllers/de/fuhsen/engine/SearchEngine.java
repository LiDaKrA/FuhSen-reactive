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
//import de.fuhsen.engine.*;

//Json
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;

//To injst WSClient
import javax.inject.Inject;

//Jena imports
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.query.*;

import play.mvc.*;
import play.libs.ws.*;
import play.libs.F.Function;
import play.libs.F.Promise;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

public class SearchEngine extends Controller {

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
		parsePersonsToJson(model);
        
        //ToDo  move all the code above inside QueryExecutor
        //QueryExecutor qe = new QueryExecutor();
        //qe.search(null);

        JsonNode json = Json.newObject()
                    .put("key3", "value3")
                    .put("key4", "value4");
        
        return ok(json);
    }
    
    private void parsePersonsToJson(Model model) {
		
		String query = ("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ "	PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
				+ "	PREFIX type: <http://dbpedia.org/class/yago/> "
				+ "	PREFIX prop: <http://dbpedia.org/property/> "
				+ "	PREFIX foaf: <http://xmlns.com/foaf/0.1/> "
				+ "	PREFIX fuhsen: <http://unibonn.eis.de/fuhsen/common_entities/> "
				+ "SELECT ?person ?name ?depiction ?familyName ?givenName ?gender ?birthday ?occupation ?currentAddress ?currentWork WHERE { "
				+ "?person fuhsen:hadPrimarySource 'GOOGLE+' . "
				+ "?person foaf:name ?name . "
				+ "?person foaf:Image ?image . "
				+ "?image foaf:depiction ?depiction . "
				+ "OPTIONAL { ?name foaf:family_name ?familyName } . "
				+ "OPTIONAL { ?name foaf:givenname ?givenName } . "
				+ "OPTIONAL { ?person foaf:gender ?gender } . "
				+ "OPTIONAL { ?person foaf:birthday ?birthday } . "
				+ "OPTIONAL { ?person fuhsen:occupation ?occupation } . "
				+ "OPTIONAL { ?person fuhsen:placesLived ?placesLived . "
				+ "			  ?placesLived fuhsen:placesLivedprimary 'true' . "
				+ "			  ?placesLived fuhsen:livedAt ?currentAddress . } . "
				+ "OPTIONAL { ?person fuhsen:organization ?organization . "
				+ "			  ?organization fuhsen:organizationprimary 'true' . "
				+ "			  ?organization fuhsen:organizationtype 'work' . "
				+ "			  ?organization fuhsen:organizationname ?currentWork . } . "
				+ "} limit 500");
		
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ResultSet results = qexec.execSelect();
		
		int aSize = 0;
		
		while(results.hasNext()) {
			
			QuerySolution row = results.next();
			
			//TODO remove this condition is temporal due to problems in JSON translation
			if (row.get("name").isLiteral())
			{
				//tmpResult["id"] = row.get("person").toString()
				Logger.info(row.getLiteral("name").getString());
				//tmpResult["title"] = row.getLiteral("name").getString()					
	            //String[] excerpts = prepareExcerptForPerson(row)
				//tmpResult["excerpt"] = excerpts[0]
				//tmpResult["excerpt1"] = excerpts[1]
	            //tmpResult["image"] = row.getLiteral("depiction").toString()
				//tmpResult["dataSource"] = "GOOGLE+"
	           
				//docs.add(tmpResult)
				aSize = aSize + 1;
			}
		}
		
		//return [resultList, aSize]		
	}
    

}
