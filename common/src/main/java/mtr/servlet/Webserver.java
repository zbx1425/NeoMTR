package mtr.servlet;

import com.lx862.tprobe3.servlet.FrontendServlet;
import mtr.MTR;
import mtr.data.DataCache;
import mtr.data.RailwayData;
import mtr.data.Route;
import com.lx862.tprobe3.servlet.DepotServletHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public abstract class Webserver {
	private static Server webServer;
	private static ServerConnector serverConnector;
	public static MinecraftCallback minecraftCallback;

	public static void setMinecraftCallback(MinecraftCallback callback) {
		minecraftCallback = callback;
	}

	public static void init() {
		webServer = new Server(new QueuedThreadPool(100, 10, 120));
		serverConnector = new ServerConnector(webServer);
		webServer.setConnectors(new Connector[]{serverConnector});
		final ServletContextHandler context = new ServletContextHandler();
		webServer.setHandler(context);
		final URL url = MTR.class.getResource("/assets/mtr/website/");
		if (url != null) {
			try {
				context.setBaseResource(Resource.newResource(url.toURI()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		final ServletHolder servletHolder = new ServletHolder("default", DefaultServlet.class);
		servletHolder.setInitParameter("dirAllowed", "true");
		servletHolder.setInitParameter("cacheControl", "max-age=0,public");
		context.addServlet(servletHolder, "/");
		context.addServlet(DataServletHandler.class, "/data");
		context.addServlet(InfoServletHandler.class, "/info");
		context.addServlet(ArrivalsServletHandler.class, "/arrivals");
		context.addServlet(DelaysServletHandler.class, "/delays");
		context.addServlet(RouteFinderServletHandler.class, "/route");

		// TProbe
		context.addServlet(FrontendServlet.class, "/mtrtv/*");
		context.addServlet(DepotServletHandler.class, "/tprobe/depots/*");
	}

	public static void start(Path path) {
		if(minecraftCallback == null) throw new IllegalStateException("Minecraft callback not configured!");

		int port = 8888;
		try {
			port = Mth.clamp(Integer.parseInt(String.join("", Files.readAllLines(path)).replaceAll("\\D", "")), 1025, 65535);
		} catch (Exception ignored) {
			try {
				Files.write(path, Collections.singleton(String.valueOf(port)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		serverConnector.setPort(port);
		try {
			webServer.start();
		} catch (Exception e) {
			MTR.LOGGER.error("Error starting webserver!", e);
		}
	}

	public static void stop() {
		try {
			webServer.stop();
		} catch (Exception e) {
			MTR.LOGGER.error("Error stopping webserver!", e);
		}
	}

	public interface MinecraftCallback {
		void runOnMainThread(Runnable runnable);
		@Nullable MinecraftServer getServer();
		List<Level> getLevels();
		List<Player> getLevelPlayers();
		Set<Route> getRoutes(RailwayData railwayData);
		DataCache getDataCache(RailwayData railwayData);
	}
}
