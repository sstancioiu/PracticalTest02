package ro.pub.cs.systems.eim.practicaltest02.network;

import android.util.Log;
import android.util.Xml;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.protocol.HTTP;
import ro.pub.cs.systems.eim.practicaltest02.general.Constants;
import ro.pub.cs.systems.eim.practicaltest02.general.Utilities;
import ro.pub.cs.systems.eim.practicaltest02.model.WeatherForecastInformation;

public class CommunicationThread extends Thread {

    private ServerThread serverThread;
    private Socket socket;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

//    public class StackOverflowXmlParser {
//        // We don't use namespaces
//        private final String ns = null;
//
//        public List parse(InputStream in) throws XmlPullParserException, IOException {
//            try {
//                XmlPullParser parser = Xml.newPullParser();
//                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
//                parser.setInput(in, null);
//                parser.nextTag();
//                return readFeed(parser);
//            } finally {
//                in.close();
//            }
//        }
//
//        private List readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
//            List entries = new ArrayList();
//
//            parser.require(XmlPullParser.START_TAG, ns, "feed");
//            while (parser.next() != XmlPullParser.END_TAG) {
//                if (parser.getEventType() != XmlPullParser.START_TAG) {
//                    continue;
//                }
//                String name = parser.getName();
//                // Starts by looking for the entry tag
//                if (name.equals("WordDefinition")) {
//                    entries.add(readEntry(parser));
//                } else {
//                    skip(parser);
//                }
//            }
//            return entries;
//        }
//    }

    @Override
    public void run() {
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            if (bufferedReader == null || printWriter == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Buffered Reader / Print Writer are null!");
                return;
            }
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (word_to_search!");
            String word_to_search = bufferedReader.readLine();
            if (word_to_search == null || word_to_search.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (word_to_search");
                return;
            }
            HashMap<String, String> data = serverThread.getData();
            String resultWord = null;
            if (data.containsKey(word_to_search)) {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the cache...");
                resultWord = data.get(word_to_search);
            } else {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(Constants.WEB_SERVICE_ADDRESS);
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair(Constants.QUERY_ATTRIBUTE, word_to_search));
                UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
                httpPost.setEntity(urlEncodedFormEntity);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String pageSourceCode = httpClient.execute(httpPost, responseHandler);
                if (pageSourceCode == null) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                    return;
                }
                Log.i(Constants.TAG, pageSourceCode);
                int c = pageSourceCode.indexOf("<WordDefinition>");
                int f = pageSourceCode.indexOf("</WordDefinition>");
                resultWord = pageSourceCode.substring(c, f);
                Log.i(Constants.TAG, resultWord);
//                resultWord = pageSourceCode;



            }
            if (resultWord == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD]  Information is null!");
                return;
            }
            printWriter.println(resultWord);
            printWriter.flush();
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        }
         finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioException) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                    if (Constants.DEBUG) {
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }

}
