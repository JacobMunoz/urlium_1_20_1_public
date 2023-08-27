package electricshmoo.urlium;

import electricshmoo.urlium.block.UrlPostBlock;
import electricshmoo.urlium.command.QueryCommand;
import electricshmoo.urlium.command.ServerCommand;
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
import java.util.List;
import java.util.Map;

public class UrlComMod implements ModInitializer {

	public static final String MOD_ID = "urlium";
    public static final Logger LOGGER = LoggerFactory.getLogger("urlium");
	public static Block URL_POST_BLOCK = null;
	private static String postUrl;
	private static String userAgent;
	private static String messageCommandName;

	public static final String URL_POST_BLOCK_ID_STRING = "block/urlpost_block";
	public static final Identifier URL_POST_BLOCK_ID = new Identifier(MOD_ID, URL_POST_BLOCK_ID_STRING);
	public static final String URL_POST_WAND_ID_STRING = "item/urlpostwand_item";
	public static final Identifier URL_POST_WAND_ID = new Identifier(MOD_ID, URL_POST_WAND_ID_STRING);
	public static final String URL_CONFIG_WAND_ID_STRING = "item/urlconfigwand_item";
	public static final Identifier URL_CONFIG_WAND_ID = new Identifier(MOD_ID, URL_CONFIG_WAND_ID_STRING);

	public void onInitialize() {
		LOGGER.info("URLium initializing...");

		PolymerResourcePackUtils.markAsRequired();
		PolymerResourcePackUtils.addModAssets(MOD_ID);

		postUrl = getConfigData("target", "");
		userAgent = getConfigData("agent","urlium_1.0.3");
		messageCommandName = getConfigData("messageCommand","webcom");

		registerPostBlock();
		registerPostWand();
		registerConfigWand();
		registerQueryCommand();
		registerServerMessageCommand();

		LOGGER.info("URLium initialized.");
	}
	public static String getMessageComandName(){
		return messageCommandName;
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
							LOGGER.info("URLium Param " + para + " = " + val);
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
				writer.write("agent=urlium_1.0.3");
				writer.write("#command name to transmit user messages to server\n");
				writer.write("messageCommand=webcom");
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
		var item = new UrlConfigWand(new Item.Settings(), URL_CONFIG_WAND_ID_STRING);
		Registry.register(Registries.ITEM, URL_CONFIG_WAND_ID, item);
	}
	public static void registerQueryCommand(){
		CommandRegistrationCallback.EVENT.register(QueryCommand::register);
	}
	public static void registerServerMessageCommand(){
		CommandRegistrationCallback.EVENT.register(ServerCommand::register);
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

}