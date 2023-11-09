package electricshmoo.urlium.item;

import electricshmoo.urlium.UrlComMod;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static electricshmoo.urlium.UrlComMod.filterUpdate;
import static electricshmoo.urlium.UrlComMod.generateMapHash;
import static java.lang.Math.floor;

public class UrlPostWand extends Item implements PolymerItem {

    private final PolymerModelData polymerModel;



    public UrlPostWand(Settings settings, String modelId) {
        super(settings);
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
            PlayerEntity player = context.getPlayer();
            String userName = player.getEntityName();
            Integer pow = world.getReceivedRedstonePower(clicked);
            Hand hand = context.getHand();
            String methodHand = hand.toString();
            StringBuilder blockStateStringBuilder = new StringBuilder(BlockArgumentParser.stringifyBlockState(clickedBlock));
            String blockStateString = blockStateStringBuilder.toString();
            String blockType = clickedBlock.getBlock().getTranslationKey();
            String shortName = blockType.substring(blockType.lastIndexOf(".")+1 );

            try {
                Map<Object, Object> data = new HashMap<>();
                long unixTime = System.currentTimeMillis();

                data.put("x", clicked.getX());
                data.put("y", clicked.getY());
                data.put("z", clicked.getZ());
                data.put("p", pow);
                data.put("device", "wand");
                data.put("target", "block");
                data.put("hand", methodHand);
                data.put("block", block.getName().getString());
                data.put("blocktype", shortName);
                data.put("blockState", blockStateString);
                data.put("user", userName);
                data.put("ts", unixTime);

                UrlComMod.sendPOST(data);

            } catch (IOException ignored) { }

        }
        return ActionResult.SUCCESS;
    }
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {

        if (!user.getWorld().isClient()){
            World world =  user.getWorld();
            String userName = user.getEntityName();
            String targetDisplayName = entity.getDisplayName().getString();
            String targetName = entity.getType().getName().getString();

            Map<Object, Object> data = new HashMap<>();
            long unixTime = System.currentTimeMillis();

            data.put("x", entity.getBlockPos().getX());
            data.put("y", entity.getBlockPos().getY());
            data.put("z", entity.getBlockPos().getZ());
            data.put("user", userName);
            data.put("device", "wand");
            data.put("target", "entity");
            data.put("hand", hand.toString());
            data.put("entityid", entity.getUuidAsString());
            data.put("entitydisplayname", targetDisplayName);
            data.put("entitytype", targetName);
            data.put("ts", floor(unixTime/100000));
            String hash = generateMapHash(data);
            data.put("hash", hash);
            if ( filterUpdate(data) ) {
                try {
                    data.put("ts", unixTime);
                    UrlComMod.sendPOST(data);
                } catch (IOException ignore) {
                }
            }


        }
        return ActionResult.SUCCESS;
    }

}
