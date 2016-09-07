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

import controllers.de.fuhsen.wrappers.security.TokenManager;
import play.mvc.*;
import views.html.*;

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
}


