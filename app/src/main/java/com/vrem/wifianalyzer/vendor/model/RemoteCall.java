/*
 * Copyright (C) 2015 - 2016 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.vrem.wifianalyzer.vendor.model;

import android.os.AsyncTask;

import com.vrem.wifianalyzer.Logger;
import com.vrem.wifianalyzer.MainContext;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

class RemoteCall extends AsyncTask<String, Void, String> {
    static final String MAC_VENDOR_LOOKUP = "http://api.macvendors.com/%s";
    private static final int MAX_RESPONSE_LEN = 100;

    @Override
    protected String doInBackground(String... params) {
        if (params == null || params.length < 1 || StringUtils.isBlank(params[0])) {
            return StringUtils.EMPTY;
        }
        String macAddress = params[0];
        String request = String.format(MAC_VENDOR_LOOKUP, macAddress);
        BufferedReader bufferedReader = null;
        try {
            URLConnection urlConnection = getURLConnection(request);
            bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }
            if (response.length() > MAX_RESPONSE_LEN) {
                return StringUtils.EMPTY;
            }
            return new RemoteResult(macAddress, response.toString()).toJson();
        } catch (Exception e) {
            return StringUtils.EMPTY;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    URLConnection getURLConnection(String request) throws IOException {
        return new URL(request).openConnection();
    }

    @Override
    protected void onPostExecute(String result) {
        if (StringUtils.isNotBlank(result)) {
            Logger logger = MainContext.INSTANCE.getLogger();
            try {
                RemoteResult remoteResult = new RemoteResult(result);
                String macAddress = remoteResult.getMacAddress();
                String vendorName = remoteResult.getVendorName();
                if (StringUtils.isNotBlank(macAddress)) {
                    logger.info(this, macAddress + " " + vendorName);
                    Database database = MainContext.INSTANCE.getDatabase();
                    database.insert(macAddress, vendorName);
                }
            } catch (JSONException e) {
                logger.error(this, result, e);
            }
        }
    }
}
