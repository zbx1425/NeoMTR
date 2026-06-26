package mtr.servlet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lx862.tprobe3.servlet.JsonDataSerializer;
import mtr.data.DataCache;
import mtr.data.RailwayData;
import mtr.data.Route;
import mtr.data.Station;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class InfoServletHandler extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		final AsyncContext asyncContext = request.startAsync();

		Webserver.minecraftCallback.runOnMainThread(() -> {
			final JsonArray dataArray = new JsonArray();
			final JsonArray playersArray = new JsonArray();

			Webserver.minecraftCallback.getLevelPlayers().forEach(player -> {
				final RailwayData railwayData = RailwayData.getInstance(player.level());
				final DataCache dataCache = Webserver.minecraftCallback.getDataCache(railwayData);

				final JsonObject dataObject = new JsonObject();
				dataObject.addProperty("player", player.getName().getString());
				dataObject.add("_playerPos", JsonDataSerializer.serialize(player.blockPosition()));

				final String routeName;
				final String routeNumber;
				final String destination;
				final String circular;
				final int color;
				final Route route = railwayData == null ? null : railwayData.railwayDataCoolDownModule.getRidingRoute(player);
				if (route == null) {
					routeName = "";
					routeNumber = "";
					destination = "";
					circular = "";
					color = 0;
				} else {
					routeName = route.name;
					routeNumber = route.isLightRailRoute ? route.lightRailRouteNumber : "";
					final Station station = dataCache.platformIdToStation.get(route.getLastPlatformId());
					destination = station == null ? "" : station.name;
					circular = route.circularState == Route.CircularState.NONE ? "" : route.circularState == Route.CircularState.CLOCKWISE ? "cw" : "ccw";
					color = route.color;
				}
				dataObject.addProperty("name", routeName);
				dataObject.addProperty("number", routeNumber);
				dataObject.addProperty("destination", destination);
				dataObject.addProperty("circular", circular);
				dataObject.addProperty("color", color);

				playersArray.add(dataObject);
			});
			dataArray.add(playersArray);

			IServletHandler.sendResponse(response, asyncContext, dataArray.toString());
		});
	}
}
