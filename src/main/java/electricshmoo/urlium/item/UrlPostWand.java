package electricshmoo.urlium.item;

import electricshmoo.urlium.block.UrlPostBlock;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class UrlPostWand extends Item implements PolymerItem {

    private final PolymerModelData polymerModel;
    private static String postUrl;
    private static String userAgent;


    public UrlPostWand(Settings settings, String modelId, String url, String agent) {
        super(settings);
        postUrl = url;
        userAgent = agent;
        this.polymerModel = PolymerResourcePackUtils.requestModel(Items.BARRIER, new Identifier("urlium", modelId));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return this.polymerModel.item();
    }

    @Override
    public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return this.polymerModel.value();
    }
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getWorld().isClient()){
            BlockPos clicked = context.getBlockPos();
            World world =  context.getWorld();
            BlockState clickedBlock = world.getBlockState(clicked);
            Block block = clickedBlock.getBlock();
            String blockName = String.valueOf(block.getName());
            PlayerEntity player = context.getPlayer();
            String userName = player.getEntityName();
            Integer pow = world.getReceivedRedstonePower(clicked);
            Hand hand = context.getHand();
            String method = hand.toString();

            try {
                sendPOST(clicked, pow, blockName,clickedBlock.toString(), method, userName);
                spawnParticles(world,clicked);
            } catch (IOException ignored) { }

        }

        return ActionResult.SUCCESS;
    }
    private static void spawnParticles(World world, BlockPos pos) {
        double d = 0.5625;
        Random random = world.random;
        Direction[] var5 = Direction.values();
        int var6 = var5.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            Direction direction = var5[var7];
            BlockPos blockPos = pos.offset(direction);
            if (!world.getBlockState(blockPos).isOpaqueFullCube(world, blockPos)) {
                Direction.Axis axis = direction.getAxis();
                double e = axis == Direction.Axis.X ? 0.5 + d * (double)direction.getOffsetX() : (double)random.nextFloat();
                double f = axis == Direction.Axis.Y ? 0.5 + d * (double)direction.getOffsetY() : (double)random.nextFloat();
                double g = axis == Direction.Axis.Z ? 0.5 + d * (double)direction.getOffsetZ() : (double)random.nextFloat();
                world.addParticle( DustParticleEffect.DEFAULT, (double)pos.getX() + e, (double)pos.getY() + f, (double)pos.getZ() + g, 0.0, 0.0, 0.0);

            }
        }
    }
    private static void sendPOST(BlockPos pos, Integer pow, String block, String blockState, String method, String sender) throws IOException {
        long unixTime = System.currentTimeMillis();

        Map<Object, Object> data = new HashMap<>();
        data.put("device", "wand");
        data.put("hand", method);
        data.put("ts", unixTime);
        data.put("x", pos.getX());
        data.put("y", pos.getY());
        data.put("z", pos.getZ());
        data.put("p", pow);
        data.put("block", block);
        data.put("blockState", blockState);
        data.put("user", sender);

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
                .thenAccept(UrlPostBlock::getResponse);

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
        //UrlComMod.LOGGER.info((builder.toString()));
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }
}
