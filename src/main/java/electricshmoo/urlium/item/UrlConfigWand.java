package electricshmoo.urlium.item;

import electricshmoo.urlium.UrlComMod;
import electricshmoo.urlium.util.IBlockEntityDataSaver;
import electricshmoo.urlium.util.IEntityDataSaver;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UrlConfigWand extends Item implements PolymerItem {

    private final PolymerModelData polymerModel;


    public UrlConfigWand(Settings settings, String modelId) {
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
        if (!context.getWorld().isClient()) {
            BlockPos clicked = context.getBlockPos();
            World world = context.getWorld();
            BlockState clickedBlock = world.getBlockState(clicked);
            PlayerEntity player = context.getPlayer();

            if (clickedBlock.hasBlockEntity() &&
                (
                    clickedBlock.getBlock().getTranslationKey().equals("block.minecraft.chest") ||
                    clickedBlock.getBlock().getTranslationKey().equals("block.minecraft.trapped_chest") ||
                    clickedBlock.getBlock().getTranslationKey().equals("block.minecraft.lectern") ||
                    clickedBlock.getBlock().getTranslationKey().equals("block.minecraft.barrel") ||
                    clickedBlock.getBlock().getTranslationKey().equals("block.minecraft.hopper") ||
                    clickedBlock.getBlock().getTranslationKey().equals("block.minecraft.dispenser") ||
                    clickedBlock.getBlock().getTranslationKey().equals("block.minecraft.dropper") ||
                    clickedBlock.getBlock().getTranslationKey().equals("block.minecraft.comparator") ||
                    clickedBlock.getBlock().getTranslationKey().equals("block.minecraft.sculk_sensor") ||
                    clickedBlock.getBlock().getTranslationKey().equals("block.minecraft.daylight_detector") ||
                    //clickedBlock.getBlock().getTranslationKey().indexOf("shulker_box") > 0 ||
                    clickedBlock.getBlock().getTranslationKey().indexOf("_sign") > 0
                )
            ) {
                IBlockEntityDataSaver clickedBlockEntity = ((IBlockEntityDataSaver) world.getBlockEntity(clicked));
                var pdata = clickedBlockEntity.getPersistentData();
                String userName = player.getEntityName();
                String blockType = clickedBlock.getBlock().getTranslationKey();
                String shortName = blockType.substring(blockType.lastIndexOf(".")+1 );


                StringBuilder blockStateStringBuilder = new StringBuilder(BlockArgumentParser.stringifyBlockState(clickedBlock));
                String blockStateString = blockStateStringBuilder.toString();

                if (pdata.contains("report")) {
                    player.sendMessage(Text.literal("Change reporting on this "+pdata.getString("blocktype")+" disabled."), false);
                    pdata.remove("report");
                    pdata.remove("blocktype");
                    sendBlockConfigStatus( clicked,  shortName,  blockStateString, userName,context.getHand().toString(),  false);
                } else {
                    int[] coords = new int[]{clicked.getX(), clicked.getY(), clicked.getZ()};
                    pdata.putIntArray("report", coords);
                    pdata.putString("blocktype",shortName);
                    player.sendMessage(Text.literal("Change reporting on this "+shortName+" enabled."), false);
                    sendBlockConfigStatus( clicked, shortName,  blockStateString, userName, context.getHand().toString(),true);
                }
            }  else {
                player.sendMessage(Text.literal("Target block is NOT a Sign, Chest, Barrel, Comparator, Lectern, Light or Sculk Sensor.  It is a: "+clickedBlock.getBlock().getTranslationKey()+".  Listed block entities only, sorry."), false);
            }
        }
        return ActionResult.SUCCESS;
    }
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!user.getWorld().isClient() && !entity.isMobOrPlayer()){
            String userName = user.getEntityName();
            String displayName = entity.getDisplayName().getString();
            String typeName = entity.getType().getName().getString();

            IEntityDataSaver clickedEntity = ((IEntityDataSaver) entity);

            var pdata = clickedEntity.getPersistentData();
            boolean reporting = false;

            if (pdata != null) {
                if (pdata.contains("report")) {
                    reporting = false;
                    pdata.remove("report");
                    pdata.remove("user");
                    pdata.remove("entityid");
                    pdata.remove("entitytype");
                    pdata.remove("entitydisplayname");
                } else {
                    reporting = true;
                    pdata.putString("report", "entity");
                    pdata.putString("user", userName);
                    pdata.putString("entityid", entity.getUuidAsString());
                    pdata.putString("entitytype", entity.getType().getName().getString());
                    pdata.putString("entitydisplayname", entity.getDisplayName().getString());
                }
            }
            sendEntityConfigStatus( entity.getBlockPos(), typeName, displayName, entity, user.getDisplayName().getString(), hand.toString(), reporting);
        }
        return ActionResult.SUCCESS;
    }
    private void sendBlockConfigStatus(BlockPos clicked, String shortName, String blockState, String userName, String hand, Boolean reporting){
        try {
            Map<Object, Object> data = new HashMap<>();
            long unixTime = System.currentTimeMillis();
            data.put("x", clicked.getX());
            data.put("y", clicked.getY());
            data.put("z", clicked.getZ());
            data.put("device", "configwand");
            data.put("reporting", reporting);
            data.put("target", "block");
            data.put("hand", hand);
            data.put("block", shortName);
            data.put("blockState", blockState);
            data.put("user", userName);
            data.put("ts", unixTime);
            UrlComMod.sendPOST(data);

        } catch (IOException ignored) { }
    }
    private void sendEntityConfigStatus(BlockPos clicked, String typeName, String displayName, Entity entity, String userName, String hand,  Boolean reporting){

        try {
            Map<Object, Object> data = new HashMap<>();
            long unixTime = System.currentTimeMillis();
            data.put("x", clicked.getX());
            data.put("y", clicked.getY());
            data.put("z", clicked.getZ());
            data.put("device", "configwand");
            data.put("reporting", reporting);
            data.put("target", "entity");
            data.put("hand", hand);
            data.put("entityid", entity.getUuidAsString());
            data.put("entitydisplayname", displayName);
            data.put("entitytype", typeName);
            data.put("user", userName);
            data.put("ts", unixTime);
            UrlComMod.sendPOST(data);

        } catch (IOException ignored) { }
    }
}