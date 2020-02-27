package i5.las2peer.connectors.webConnector.util;

import java.io.BufferedReader;

import java.io.InputStreamReader;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;

import java.net.URL;

import i5.las2peer.api.execution.ServiceNotFoundException;
import i5.las2peer.logging.L2pLogger;

public class L2P_HTTPUtil {
    private static final L2pLogger logger = L2pLogger.getInstance(L2P_HTTPUtil.class);

    public static String getHTTP(String requestURL, String requestMethod)
            throws MalformedURLException, ServiceNotFoundException {

        logger.info("[HTTP] " + requestMethod + "@ " + requestURL);

        // https://stackoverflow.com/a/35013372
        // https://stackoverflow.com/q/33491373
        try {
            URL url = new URL(requestURL);
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod(requestMethod);
            http.setDoOutput(true);
            http.setInstanceFollowRedirects(true);

            boolean redirect = false;

            // normally, 3xx is redirect
            int status = http.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER)
                redirect = true;
            }

            if ( redirect )
            {
                // get redirect url from "location" header field
                String newUrl = http.getHeaderField("Location");

                // get the cookie if need, for login
                String cookies = http.getHeaderField("Set-Cookie");

                // open the new connnection again
                http = (HttpURLConnection) new URL(newUrl).openConnection();
                http.setRequestProperty("Cookie", cookies);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer responseBuilder = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                responseBuilder.append(inputLine);
            }
            in.close();

            return responseBuilder.toString();

        } catch (IOException e) {
            throw new ServiceNotFoundException("HTTP request failed", e);
        }
    }
}