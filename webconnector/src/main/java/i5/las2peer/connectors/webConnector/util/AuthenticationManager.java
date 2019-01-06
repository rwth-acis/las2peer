package i5.las2peer.connectors.webConnector.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;

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

import i5.las2peer.api.security.*;
import i5.las2peer.connectors.webConnector.WebConnector;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.p2p.EthereumNode;
import i5.las2peer.security.*;
import i5.las2peer.tools.CryptoTools;
import net.minidev.json.JSONObject;

public class AuthenticationManager {

	private final L2pLogger logger = L2pLogger.getInstance(AuthenticationManager.class.getName());

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
		String token = extractToken(authorizationHeader, accessTokenQueryParam, accessTokenHeader);

		String endpoint;
		try {
			String oidcProviderURI = (oidcProviderHeader != null) ? oidcProviderHeader : connector.defaultOIDCProvider;
			JSONObject oidcProviderInfo = getOidcProviderInfo(oidcProviderURI);
			endpoint = (String) ((JSONObject) oidcProviderInfo.get("config")).get("userinfo_endpoint");
		} catch (IllegalArgumentException e) {
			throw new InternalServerErrorException(e);
		}

		JSONObject userInfo = retrieveOidcUserInfo(endpoint, token).toJSONObject();

		if (!userInfo.containsKey("sub") || !userInfo.containsKey("email") || !userInfo.containsKey("preferred_username")) {
			throw new ForbiddenException("Could not get all necessary OIDC fields. Please check your scopes.");
		}

		// TODO choose other scheme for generating agent password
		String password = (String) userInfo.get("sub");

		try {
			String oidcAgentId = connector.getL2pNode().getUserManager().getAgentIdByOIDCSub((String) userInfo.get("sub"));
			return getExistingOidcAgent(oidcAgentId, password);
		} catch (AgentAccessDeniedException e) {
			connector.logError("Authentication failed!", e);
			throw new NotAuthorizedException(e.toString(), e);
		} catch (AgentNotFoundException e) {
			return createNewOidcAgent(password, userInfo);
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
		return AnonymousAgentImpl.getInstance();
	}

	private String extractToken(String authorizationHeader, String accessTokenQueryParam, String accessTokenHeader) {
		if (accessTokenHeader != null) {
			// get OIDC parameters from headers
			return accessTokenHeader;
		} else if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer ")) {
			// get BEARER token from Authentication field
			return authorizationHeader.substring("BEARER ".length());
		} else { // get OIDC parameters from GET values
			return accessTokenQueryParam;
		}
	}

	private JSONObject getOidcProviderInfo(String oidcProviderURI) {
		if (connector.oidcProviders.contains(oidcProviderURI)
				&& connector.oidcProviderInfos.get(oidcProviderURI) != null) {
			return connector.oidcProviderInfos.get(oidcProviderURI);
		} else {
			throw new IllegalArgumentException("The given OIDC provider (" + oidcProviderURI + ") is not whitelisted!"
					+ "Please make sure the complete OIDC provider URI is added to the config.");
		}
	}

	// send request to OpenID Connect user info endpoint to retrieve
	// complete user information in exchange for access token.
	private UserInfo retrieveOidcUserInfo(String endpoint, String authToken) {
		HTTPResponse hrs;
		try {
			URI userinfoEndpointUri = new URI(endpoint);
			HTTPRequest hrq = new HTTPRequest(Method.GET, userinfoEndpointUri.toURL());
			hrq.setAuthorization("Bearer " + authToken);

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

		if (userInfoResponse instanceof UserInfoErrorResponse) {
			// failed request for OpenID Connect user info
			UserInfoErrorResponse uier = (UserInfoErrorResponse) userInfoResponse;
			ErrorObject err = uier.getErrorObject();
			String cause = "Session expired?";
			if (err != null) {
				cause = err.getDescription();
			}
			throw new NotAuthorizedException("Open ID Connect UserInfo request failed! Cause: " + cause);
		} else {
			return ((UserInfoSuccessResponse) userInfoResponse).getUserInfo();
		}
	}

	private PassphraseAgentImpl getExistingOidcAgent(String oidcAgentId, String password) throws AgentException {
		try {
			connector.getLockOidc().lock(oidcAgentId);
			PassphraseAgentImpl pa = (PassphraseAgentImpl) connector.getL2pNode().getAgent(oidcAgentId);
			pa.unlock(password);

			// TODO provide OIDC user data for agent
			// if (pa instanceof UserAgent) {
			// 		((UserAgent) pa).setUserData(userInfo.toJSONString());
			// }

			return pa;
		} finally {
			connector.getLockOidc().unlock(oidcAgentId);
		}
	}

	private UserAgentImpl createNewOidcAgent(String password, JSONObject userInfo) {
		try {
			UserAgentImpl oidcAgent;
			if (connector.getL2pNode() instanceof EthereumNode) {
				String loginName = (String) userInfo.get("preferred_username");
				oidcAgent = EthereumAgent.createEthereumAgent(loginName, password);
			} else {
				// TODO: should we just always create an EthereumAgent?
				// should Node have a createAgent (instance? static?) method?
				// whatever, let's be conservative for now
				oidcAgent = UserAgentImpl.createUserAgent(password);
			}
			String oidcAgentId = oidcAgent.getIdentifier();
			try {
				connector.getLockOidc().lock(oidcAgentId);
				oidcAgent.unlock(password);
				oidcAgent.setEmail((String) userInfo.get("email"));
				oidcAgent.setLoginName((String) userInfo.get("preferred_username"));
				// TODO provide OIDC user data for agent
				// oidcAgent.setUserData(userInfo.toJSONString());
				connector.getL2pNode().storeAgent(oidcAgent);
				try {
					connector.getL2pNode().getUserManager().registerOIDCSub(oidcAgent, (String) userInfo.get("sub"));
				} catch (AgentAlreadyExistsException e) {
					logger.log(Level.FINE, "Could not register OIDC sub", e);
				}
				return oidcAgent;
			} finally {
				connector.getLockOidc().unlock(oidcAgentId);
			}
		} catch (Exception e) {
			throw new InternalServerErrorException("OIDC agent creation failed", e);
		}
	}
}
