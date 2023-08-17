package electricshmoo.urlium.item;

import electricshmoo.urlium.util.IBlockEntityDataSaver;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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
                    clickedBlock.getBlock().getTranslationKey().equals("block.minecraft.lectern") ||
                    clickedBlock.getBlock().getTranslationKey().indexOf("_sign")>0
                )
            ) {
                IBlockEntityDataSaver clickedBlockEntity = ((IBlockEntityDataSaver) world.getBlockEntity(clicked));
                var pdata = clickedBlockEntity.getPersistentData();
                if (pdata.contains("report")) {
                    player.sendMessage(Text.literal("Change reporting on this "+pdata.getString("blocktype")+" disabled."), false);
                    pdata.remove("report");
                    pdata.remove("blocktype");
                } else {
                    int[] coords = new int[]{clicked.getX(), clicked.getY(), clicked.getZ()};
                    pdata.putIntArray("report", coords);
                    pdata.putString("blocktype","chest");
                    String blockType = clickedBlock.getBlock().getTranslationKey();
                    String shortName = blockType.substring(blockType.lastIndexOf(".")+1 );
                    pdata.putString("blocktype",shortName);
                    player.sendMessage(Text.literal("Change reporting on this "+shortName+" enabled."), false);
                }
            }  else {
                player.sendMessage(Text.literal("Target block is NOT a Sign, Chest or Lectern, it is a: "+clickedBlock.getBlock().getTranslationKey()+".  Signs, normal chests, and lecterns only, sorry."), false);
            }
        }
        return ActionResult.SUCCESS;
    }
}