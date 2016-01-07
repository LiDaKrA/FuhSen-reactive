package services.de.fuhsen.common;

import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.libs.F.Function;
import play.libs.Json;
import play.libs.ws.*;
import play.mvc.Result;
import play.mvc.Results;

public class Functions
{
	public static Function<WSResponse, JsonNode> responseToJson = new Function<WSResponse, JsonNode>()
	{
		public JsonNode apply(WSResponse s)
		{
			return s.asJson();
		}
	};
	
	public static Function<JsonNode, Result> jsonToResult = new Function<JsonNode, Result>()
	{
		public Result apply(JsonNode s)
		{
			return Results.ok(s);
		}
	};
	
	public static Function<Throwable, JsonNode> searchError = new Function<Throwable, JsonNode>()
	{
		@Override
		public JsonNode apply(Throwable t)
		{
			Logger.error("Failed to execute search", t);
			return Json.parse("{\"error\": \"failed to execute query search\"}");
		}
	};
}
