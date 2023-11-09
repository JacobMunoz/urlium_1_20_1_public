package electricshmoo.urlium;

import electricshmoo.urlium.block.UrlPostBlock;
import electricshmoo.urlium.command.*;
import electricshmoo.urlium.item.UrlConfigWand;
import electricshmoo.urlium.item.UrlPostBlockItem;
import electricshmoo.urlium.item.UrlPostWand;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UrlComMod implements ModInitializer {

	public static final String MOD_ID = "urlium";
    public static final Logger LOGGER = LoggerFactory.getLogger("urlium");
	public static Block URL_POST_BLOCK = null;
	private static String postUrl;
	private static String userAgent;
	private static String messageCommand1Name;
	private static String messageCommand2Name;
	private static String messageCommand3Name;
	public static int messageCommand1Level;
	public static int messageCommand2Level;
	public static int messageCommand3Level;
	private static String messageAuthentication;
	public static int usetblocklevel;
	public static int ugetblocklevel;
	public static Map<String, String> previousMessages = new HashMap<>();
	public static final String URL_POST_BLOCK_ID_STRING = "block/urlpost_block";
	public static final Identifier URL_POST_BLOCK_ID = new Identifier(MOD_ID, URL_POST_BLOCK_ID_STRING);
	public static final String URL_POST_WAND_ID_STRING = "item/urlpostwand_item";
	public static final Identifier URL_POST_WAND_ID = new Identifier(MOD_ID, URL_POST_WAND_ID_STRING);
	public static final String URL_CONFIG_WAND_ID_STRING = "item/urlconfigwand_item";
	public static final Identifier URL_CONFIG_WAND_ID = new Identifier(MOD_ID, URL_CONFIG_WAND_ID_STRING);
	public static Item ConfigWand;

	public void onInitialize() {
		LOGGER.info("URLium 1.3.0 initializing...");

		PolymerResourcePackUtils.markAsRequired();
		PolymerResourcePackUtils.addModAssets(MOD_ID);

		postUrl = getConfigData("target", "");
		LOGGER.info("Setting: target URL: "+postUrl+".");

		userAgent = getConfigData("agent","urlium_1.3.0");
		LOGGER.info("Setting: http user agent: "+userAgent+".");

		messageCommand1Name = getConfigData("messageCommand","webcom");
		messageCommand1Level = Integer.parseInt(getConfigData("messageCommandLevel","0"));
		messageCommand2Name = getConfigData("messageCommand2","webcom2");
		messageCommand2Level = Integer.parseInt(getConfigData("messageCommand2Level","-1"));
		messageCommand3Name = getConfigData("messageCommand3","webcom3");
		messageCommand3Level = Integer.parseInt(getConfigData("messageCommand3Level","-1"));
		messageAuthentication = getConfigData("securityToken","");
		ugetblocklevel = Integer.parseInt(getConfigData("ugetblockLevel","2"));
		usetblocklevel = Integer.parseInt(getConfigData("usetblockLevel","2"));

		if ( !messageAuthentication.equals("") ){
			LOGGER.info("Setting: securityToken loaded.");
		}
		registerPostBlock();
		registerPostWand();
		registerConfigWand();
		if (ugetblocklevel >= 0 ) {
			registerUGetBlockCommand(); // allow config to use -1 to disable
			LOGGER.info("Setting: ugetblock enabled for user level: "+ugetblocklevel+".");
		}
		if (usetblocklevel >= 0 ) {
			registerUSetBlockCommand(); // allow config to use -1 to disable
			LOGGER.info("Setting: usetblock enabled for user level: "+usetblocklevel+".");
		}

		if (messageCommand1Level>=0) {
			LOGGER.info("Setting: messageCommand ("+messageCommand1Name+") enabled for user level: "+messageCommand1Level+".");
			registerServerMessageCommand1();
		}
		if (messageCommand2Level>=0) {
			LOGGER.info("Setting: messageCommand2 ("+messageCommand2Name+") enabled for user level: "+messageCommand2Level+".");
			registerServerMessageCommand2();
		}
		if (messageCommand3Level>=0) {
			LOGGER.info("Setting: messageCommand3 ("+messageCommand3Name+") enabled for user level: "+messageCommand3Level+".");
			registerServerMessageCommand3();
		}

		LOGGER.info("URLium initialized.");
	}
	public static String getMessageCommand1Name(){
		return messageCommand1Name;
	}
	public static String getMessageCommand2Name(){
		return messageCommand2Name;
	}
	public static String getMessageCommand3Name(){
		return messageCommand3Name;
	}
	private static String getConfigData(String param, String deefault) {
		File f = new File("./config/urlium.properties");
		if(f.exists() && !f.isDirectory()) {
			try {
				List<String> cfgLines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
				for (String line : cfgLines) {
					if ( line.length() > 0
					&& (line.charAt(0) != '#')
					&& (line.indexOf("=") > 0)) {
						String para = line.substring(0,line.indexOf("="));
						String val = line.substring(line.indexOf("=")+1);
						if (para.equals(param)) {
//							LOGGER.info("URLium Param " + para + " = " + val);
							return val;
						}
					}
				}
			} catch (IOException e) {
				LOGGER.info("Failed to parse URLium config file");
			}
		} else {
			LOGGER.info("No config file (urlium.properties) found.  Attempting to write default config file.");
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(f.toPath().toString()));
				writer.write("#set target to the http(s) url to send data to\n");
				writer.write("target=");
				writer.write("#set agent to the User-Agent header to be transmitted (browser name)\n");
				writer.write("agent=urlium_1.3.0");
				writer.write("#optional authentication value to send to webserver as shared secret\n");
				writer.write("securityToken=secret_token");
				writer.write("#command name to transmit user messages to server\n");
				writer.write("messageCommand=webcom");
				writer.write("#command user permission level.  use -1 to disable completely\n");
				writer.write("messageCommandLevel=0");
				writer.write("#command name to transmit user messages to server\n");
				writer.write("messageCommand2=webcom2");
				writer.write("#command user permission level.  use -1 to disable completely\n");
				writer.write("messageCommand2Level=-1");
				writer.write("#command name to transmit user messages to server\n");
				writer.write("messageCommand3=webcom3");
				writer.write("#command user permission level.  use -1 to disable completely\n");
				writer.write("messageCommand3Level=-1");
				writer.write("#/usetblock command user permission level.  use -1 to disable completely\n");
				writer.write("usetblockLevel=2");
				writer.write("#/ugetblock command user permission level.  use -1 to disable completely\n");
				writer.write("ugetblockLevel=2");
				writer.close();
				LOGGER.info("Wrote default config file (urlium.properties)  Please set 'target'.");
			} catch (IOException e) {
				LOGGER.info("Failed to write default config file (urlium.properties)  Please check config folder.");
			}
		}
		return deefault;
	}

	public static void registerPostBlock() {
		URL_POST_BLOCK = new UrlPostBlock(URL_POST_BLOCK_ID_STRING);
		var blockreg = Registry.register(Registries.BLOCK, URL_POST_BLOCK_ID, URL_POST_BLOCK);
		Registry.register(Registries.ITEM, URL_POST_BLOCK_ID, new UrlPostBlockItem(new Item.Settings(), blockreg, URL_POST_BLOCK_ID_STRING));
	}
	public static void registerPostWand() {
		var item = new UrlPostWand(new Item.Settings(), URL_POST_WAND_ID_STRING);
		Registry.register(Registries.ITEM, URL_POST_WAND_ID, item);
	}
	public static void registerConfigWand() {
		ConfigWand = new UrlConfigWand(new Item.Settings(), URL_CONFIG_WAND_ID_STRING);
		Registry.register(Registries.ITEM, URL_CONFIG_WAND_ID, ConfigWand);
	}
	public static void registerUGetBlockCommand(){
		CommandRegistrationCallback.EVENT.register(UGetBlockCommand::register);
	}
	public static void registerUSetBlockCommand(){
		CommandRegistrationCallback.EVENT.register(USetBlockCommand::register);
	}
	public static void registerServerMessageCommand1(){
		CommandRegistrationCallback.EVENT.register(ServerCommand1::register);
	}
	public static void registerServerMessageCommand2(){
		CommandRegistrationCallback.EVENT.register(ServerCommand2::register);
	}
	public static void registerServerMessageCommand3(){
		CommandRegistrationCallback.EVENT.register(ServerCommand3::register);
	}



	public static void sendPOST(Map<Object, Object> data ) throws IOException {
		if (postUrl.equals("")) {
			LOGGER.info("URLium target config is blank.  No POST transmitted.");
			return;
		}
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(postUrl))
				.POST(buildFormDataFromMap(data))
				.timeout(Duration.ofSeconds(5))
				.header("User-Agent", userAgent)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("Authentication", "Bearer " + messageAuthentication)
				.build();

		HttpClient client = HttpClient.newBuilder().build();
		client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(HttpResponse::body)
				.thenAccept(UrlComMod::getResponse);

	}
	private static HttpRequest.BodyPublisher buildFormDataFromMap(Map<Object, Object> data) {
		var builder = new StringBuilder();
		for (Map.Entry<Object, Object> entry : data.entrySet()) {
			if (builder.length() > 0) {
				builder.append("&");
			}
			builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
			builder.append("=");
			builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
		}
		return HttpRequest.BodyPublishers.ofString(builder.toString());
	}
	public static void getResponse(String data) {
		UrlComMod.LOGGER.info("POST Response Data: " + data);
	}
	public static String generateMapHash(Map<Object, Object> dataMap) {
		int hashCode = dataMap.hashCode();
		return Integer.toHexString(hashCode);
	}
	public static boolean filterUpdate(Map<Object, Object> message) {
		// Extract the X, Y, Z coordinates and data from the message map.
		String hash = (String)message.get("hash");
		String coordinateKey = (int)message.get("x") + "_" + (int)message.get("y") + "_" + (int)message.get("z");
		if (previousMessages.containsKey(coordinateKey)) {
			Object previousData = previousMessages.get(coordinateKey);
			if (!hash.equals(previousData)) {
				previousMessages.put(coordinateKey, hash);
				return true;
			} else {
				return false;
			}
		} else {
			previousMessages.put(coordinateKey, hash);
			return true;
		}
	}
}