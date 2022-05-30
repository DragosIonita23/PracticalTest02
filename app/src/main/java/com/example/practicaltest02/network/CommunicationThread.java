package com.example.practicaltest02.network;

import android.util.Log;

import com.example.practicaltest02.general.Constants;
import com.example.practicaltest02.general.Utilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
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
import cz.msebera.android.httpclient.util.EntityUtils;

public class CommunicationThread extends Thread {
    private ServerThread serverThread;
    private Socket socket;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

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
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (city / information type!");
            String name = bufferedReader.readLine();
            if (name == null || name.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (city / information type!");
                return;
            }
            HashMap<String, String> data = serverThread.getData();
            String wordDefinition = null;
            if (data.containsKey(name)) {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the cache...");
                wordDefinition = data.get(name);
            } else {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
                HttpClient httpClient = new DefaultHttpClient();
                String pageSourceCode = "";
                if(false) {
                    HttpPost httpPost = new HttpPost(Constants.WEB_SERVICE_ADDRESS);
                    List<NameValuePair> params = new ArrayList<>();
                    params.add(new BasicNameValuePair("q", name));
                    params.add(new BasicNameValuePair("mode", Constants.WEB_SERVICE_MODE));
                    params.add(new BasicNameValuePair("APPID", Constants.WEB_SERVICE_API_KEY));
                    UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
                    httpPost.setEntity(urlEncodedFormEntity);
                    ResponseHandler<String> responseHandler = new BasicResponseHandler();

                    pageSourceCode = httpClient.execute(httpPost, responseHandler);
                } else {
                    HttpGet httpGet = new HttpGet(Constants.WEB_SERVICE_ADDRESS + name);
                    HttpResponse httpGetResponse = httpClient.execute(httpGet);
                    HttpEntity httpGetEntity = httpGetResponse.getEntity();
                    if (httpGetEntity != null) {
                        pageSourceCode = EntityUtils.toString(httpGetEntity);

                    }
                }

                if (pageSourceCode == null) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                    return;
                } else
                    Log.i(Constants.TAG, "[COMMUNICATION THREAD] PAGESOURCECOD = " + pageSourceCode );

                // Updated for openweather API
                if (false) {
                    Document document = Jsoup.parse(pageSourceCode);
                    Element element = document.child(0);
//                    Elements elements = element.getElementsByTag(Constants.SCRIPT_TAG);
//                    for (Element script : elements) {
//                        String scriptData = script.data();
//                        if (scriptData.contains(Constants.SEARCH_KEY)) {
//                            int position = scriptData.indexOf(Constants.SEARCH_KEY) + Constants.SEARCH_KEY.length();
//                            scriptData = scriptData.substring(position);
//                            JSONObject content = new JSONObject(scriptData);
//                            JSONObject currentObservation = content.getJSONObject(Constants.CURRENT_OBSERVATION);
//                            String temperature = currentObservation.getString(Constants.TEMPERATURE);
//                            String windSpeed = currentObservation.getString(Constants.WIND_SPEED);
//                            String condition = currentObservation.getString(Constants.CONDITION);
//                            String pressure = currentObservation.getString(Constants.PRESSURE);
//                            String humidity = currentObservation.getString(Constants.HUMIDITY);
//                            weatherForecastInformation = new WeatherForecastInformation(
//                                    temperature, windSpeed, condition, pressure, humidity
//                            );
//                            serverThread.setData(city, weatherForecastInformation);
//                            break;
//                        }
//                    }
                } else {
                    JSONArray content = new JSONArray(pageSourceCode);

                    JSONObject contentObj = content.getJSONObject(0);
                    JSONArray meaningsArray = contentObj.getJSONArray(Constants.MEANINGS);
                    JSONObject meaning;
                    JSONObject definitionObject;
                    JSONArray definitions;

                    meaning = meaningsArray.getJSONObject(0);

                    definitions = meaning.getJSONArray(Constants.DEFINITIONS);

                    definitionObject = definitions.getJSONObject(0);

                    wordDefinition = definitionObject.getString(Constants.DEFINITION);

                    serverThread.setData(name, wordDefinition);
                }
            }
            if (wordDefinition == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Weather Forecast Information is null!");
                return;
            }
            printWriter.println(wordDefinition);
            printWriter.flush();
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        } catch (JSONException jsonException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + jsonException.getMessage());
            if (Constants.DEBUG) {
                jsonException.printStackTrace();
            }
        } finally {
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
