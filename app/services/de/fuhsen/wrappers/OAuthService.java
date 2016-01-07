package external.services;

import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.libs.oauth.OAuth.RequestToken;

//Json
//Json
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.Json;

public interface OAuthService
{
	public Tuple<String, RequestToken> retrieveRequestToken(String callbaclUrl);
	public Promise<JsonNode> getSearchResults(RequestToken token, String authVerifier, String query_string);
}
