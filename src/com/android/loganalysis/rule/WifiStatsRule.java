/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.loganalysis.rule;

import com.android.loganalysis.item.BugreportItem;
import com.android.loganalysis.item.DumpsysWifiStatsItem;

import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Rules definition for Process usage
 */
public class WifiStatsRule extends AbstractPowerRule {

    private static final String WIFI_STATS = "WIFI_STATS";
    private static final int WIFI_DISCONNECT_THRESHOLD = 1; // wifi disconnect should never happen
    // Wifi scans are scheduled by GSA every 285 seconds, anything more frequent is an issue
    private static final long WIFI_SCAN_INTERVAL_THRESHOLD_MS = 285000;

    private StringBuffer mAnalysisBuffer;
    private BugreportItem mBugreportItem = null;

    public WifiStatsRule (BugreportItem bugreportItem) {
        super(bugreportItem);
        mBugreportItem = bugreportItem;
    }

    @Override
    public void applyRule() {
        mAnalysisBuffer = new StringBuffer();
        if (mBugreportItem.getDumpsys() == null || getTimeOnBattery() <= 0) {
            return;
        }
        DumpsysWifiStatsItem dumpsysWifiStatsItem = mBugreportItem.getDumpsys().getWifiStats();
        if (dumpsysWifiStatsItem == null) {
            return;
        }
        if (dumpsysWifiStatsItem.getNumWifiScans() > 0) {
            final long observedWifiScanIntervalMs = getTimeOnBattery() /
                    dumpsysWifiStatsItem.getNumWifiScans();

            if (observedWifiScanIntervalMs < WIFI_SCAN_INTERVAL_THRESHOLD_MS) {
                mAnalysisBuffer.append(String.format("Wifi scans happened every %d seconds.",
                        TimeUnit.MILLISECONDS.toSeconds(observedWifiScanIntervalMs)));
            }
            if (dumpsysWifiStatsItem.getNumWifiDisconnects() >= WIFI_DISCONNECT_THRESHOLD) {
                mAnalysisBuffer.append(String.format("Wifi got disconnected %d times",
                        dumpsysWifiStatsItem.getNumWifiDisconnects()));
            }
        }
    }

    @Override
    public JSONObject getAnalysis() {
        JSONObject wifiStatsAnalysis = new JSONObject();
        try {
            wifiStatsAnalysis.put(WIFI_STATS, mAnalysisBuffer.toString());
        } catch (JSONException e) {
          // do nothing
        }
        return wifiStatsAnalysis;
    }
}
