package com.lx862.tprobe3.servlet;

import com.google.gson.JsonObject;

public class TProbeHelper {
    public static JsonObject getTProbeResponse(int responseCode, String textResponse, JsonObject data) {
        JsonObject respObject = new JsonObject();
        respObject.addProperty("code", responseCode);
        respObject.addProperty("currentTime", System.currentTimeMillis());
        respObject.addProperty("text", textResponse);
        respObject.addProperty("version", 1);
        if(data != null) respObject.add("data", data);
        return respObject;
    }
}
