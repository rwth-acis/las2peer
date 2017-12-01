package i5.las2peer.connectors.webConnector.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.UserInfoErrorResponse;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.UserInfoSuccessResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;

import i5.las2peer.api.security.AgentAccessDeniedException;
import i5.las2peer.api.security.AgentException;
import i5.las2peer.api.security.AgentNotFoundException;
import i5.las2peer.api.security.AnonymousAgent;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.security.AgentImpl;
import i5.las2peer.security.AnonymousAgentImpl;
import i5.las2peer.security.PassphraseAgentImpl;
import i5.las2peer.security.UserAgentImpl;
import i5.las2peer.tools.CryptoTools;
import net.minidev.json.JSONObject;

public class AuthenticationManager {

	public static final String ACCESS_TOKEN_KEY = "access_token";
	public static final String OIDC_PROVIDER_KEY = "oidc_provider";

	private final WebConnector connector;

	public AuthenticationManager(WebConnector connector) {
		this.connector = connector;
	}

	public AgentImpl authenticateAgent(MultivaluedMap<String, String> requestHeaders, String accessTokenQueryParam) {
		final String authorizationHeader = requestHeaders.getFirst(HttpHeaders.AUTHORIZATION);
		final String accessTokenHeader = requestHeaders.getFirst(ACCESS_TOKEN_KEY);
		if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("basic ")) {
			// basic authentication
			return authenticateBasic(authorizationHeader);
		} else if (connector.oidcProviderInfos != null && accessTokenQueryParam != null || accessTokenHeader != null
				|| (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer "))) {
			// openid connect
			final String oidcProviderHeader = requestHeaders.getFirst(OIDC_PROVIDER_KEY);
			return authenticateOIDC(authorizationHeader, accessTokenQueryParam, accessTokenHeader, oidcProviderHeader);
		} else {
			// anonymous login
			return authenticateAnonymous();
		}
	}

	private AgentImpl authenticateBasic(String authorizationHeader) {
		// looks like: Authentication Basic <Byte64(name:pass)>
		String userPass = authorizationHeader.substring("BASIC ".length());
		userPass = new String(Base64.getDecoder().decode(userPass), StandardCharsets.UTF_8);
		int separatorPos = userPass.indexOf(':');

		// get username and password
		String username = userPass.substring(0, separatorPos);
		String password = userPass.substring(separatorPos + 1);

		return authenticateNamePassword(username, password);
	}

	private PassphraseAgentImpl authenticateOIDC(String authorizationHeader, String accessTokenQueryParam,
			String accessTokenHeader, String oidcProviderHeader) {
		// extract token
		String token = "";
		String oidcProviderURI = connector.defaultOIDCProvider;
		if (accessTokenHeader != null) {
			// get OIDC parameters from headers
			token = accessTokenHeader;
			if (oidcProviderHeader != null) {
				oidcProviderURI = oidcProviderHeader;
			}
		} else if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer ")) {
			// get BEARER token from Authentication field
			token = authorizationHeader.substring("BEARER ".length());
			if (oidcProviderHeader != null) {
				oidcProviderURI = oidcProviderHeader;
			}
		} else { // get OIDC parameters from GET values
			token = accessTokenQueryParam;
			if (oidcProviderHeader != null) {
				oidcProviderURI = oidcProviderHeader;
			}
		}

		// validate given OIDC provider and get provider info
		JSONObject oidcProviderInfo = null;
		if (!connector.oidcProviders.contains(oidcProviderURI)
				|| connector.oidcProviderInfos.get(oidcProviderURI) == null) {
			throw new InternalServerErrorException("The given OIDC provider (" + oidcProviderURI
					+ ") is not whitelisted! Please make sure the complete OIDC provider URI is added to the config.");
		} else {
			oidcProviderInfo = connector.oidcProviderInfos.get(oidcProviderURI);
		}

		// send request to OpenID Connect user info endpoint to retrieve
		// complete user information
		// in exchange for access token.
		HTTPRequest hrq;
		HTTPResponse hrs;

		try {
			URI userinfoEndpointUri = new URI(
					(String) ((JSONObject) oidcProviderInfo.get("config")).get("userinfo_endpoint"));
			hrq = new HTTPRequest(Method.GET, userinfoEndpointUri.toURL());
			hrq.setAuthorization("Bearer " + token);

			// TODO process all error cases that can happen (in particular invalid tokens)
			hrs = hrq.send();
		} catch (IOException | URISyntaxException e) {
			throw new InternalServerErrorException("Fetching OIDC user info failed", e);
		}

		// process response from OpenID Connect user info endpoint
		UserInfoResponse userInfoResponse;
		try {
			userInfoResponse = UserInfoResponse.parse(hrs);
		} catch (ParseException e) {
			throw new InternalServerErrorException("Couldn't parse UserInfo response", e);
		}

		// failed request for OpenID Connect user info
		if (userInfoResponse instanceof UserInfoErrorResponse) {
			UserInfoErrorResponse uier = (UserInfoErrorResponse) userInfoResponse;
			ErrorObject err = uier.getErrorObject();
			String cause = "Session expired?";
			if (err != null) {
				cause = err.getDescription();
			}
			throw new NotAuthorizedException("Open ID Connect UserInfo request failed! Cause: " + cause);
		}

		// successful request
		UserInfo userInfo = ((UserInfoSuccessResponse) userInfoResponse).getUserInfo();

		JSONObject ujson = userInfo.toJSONObject();

		if (!ujson.containsKey("sub") || !ujson.containsKey("email") || !ujson.containsKey("preferred_username")) {
			throw new ForbiddenException("Could not get provider information. Please check your scopes.");
		}

		String sub = (String) ujson.get("sub");

		// TODO choose other scheme for generating agent password
		String password = sub;

		try {
			String oidcAgentId = connector.getL2pNode().getUserManager().getAgentIdByOIDCSub(sub);
			try {
				connector.getLockOidc().lock(oidcAgentId);
				PassphraseAgentImpl pa = (PassphraseAgentImpl) connector.getL2pNode().getAgent(oidcAgentId);
				pa.unlock(password);
				if (pa instanceof UserAgentImpl) {
					UserAgentImpl ua = (UserAgentImpl) pa;
					// TODO provide OIDC user data for agent
					// ua.setUserData(ujson.toJSONString());
					return ua;
				} else {
					return pa;
				}
			} finally {
				connector.getLockOidc().unlock(oidcAgentId);
			}
		} catch (AgentAccessDeniedException e) {
			connector.logError("Authentication failed!", e);
			throw new NotAuthorizedException(e.toString(), e);
		} catch (AgentNotFoundException e) {
			try {
				UserAgentImpl oidcAgent = UserAgentImpl.createUserAgent(password);
				String oidcAgentId = oidcAgent.getIdentifier();
				try {
					connector.getLockOidc().lock(oidcAgentId);
					oidcAgent.unlock(password);
					oidcAgent.setEmail((String) ujson.get("email"));
					oidcAgent.setLoginName((String) ujson.get("preferred_username"));
					// TODO provide OIDC user data for agent
					// oidcAgent.setUserData(ujson.toJSONString());
					connector.getL2pNode().storeAgent(oidcAgent);
					connector.getL2pNode().getUserManager().registerOIDCSub(oidcAgent, sub);
					return oidcAgent;
				} finally {
					connector.getLockOidc().unlock(oidcAgentId);
				}
			} catch (Exception e1) {
				throw new InternalServerErrorException("OIDC agent creation failed", e1);
			}
		} catch (AgentException e) {
			connector.logError("Could not retrieve and unlock agent from network", e);
			throw new NotAuthorizedException(e.toString(), e);
		}
	}

	private AgentImpl authenticateNamePassword(String username, String password) {
		if (username.equalsIgnoreCase(AnonymousAgent.LOGIN_NAME)) {
			return authenticateAnonymous();
		}
		try {
			String userId;
			// check if username is an agent id
			if (CryptoTools.isAgentID(username)) {
				userId = username;
			} else {
				userId = connector.getL2pNode().getAgentIdForLogin(username);
			}

			PassphraseAgentImpl userAgent = (PassphraseAgentImpl) connector.getL2pNode().getAgent(userId);
			userAgent.unlock(password);

			return userAgent;
		} catch (AgentNotFoundException e) {
			connector.logError("user " + username + " not found", e);
			throw new NotAuthorizedException("user " + username + " not found");
		} catch (AgentAccessDeniedException e) {
			connector.logError("passphrase invalid for user " + username, e);
			throw new NotAuthorizedException("passphrase invalid for user " + username);
		} catch (Exception e) {
			connector.logError("something went horribly wrong. Check your request for correctness.", e);
			throw new NotAuthorizedException("something went horribly wrong. Check your request for correctness.");
		}
	}

	private AgentImpl authenticateAnonymous() {
		AnonymousAgentImpl anonymousAgent = AnonymousAgentImpl.getInstance();
		return anonymousAgent;
	}

}
