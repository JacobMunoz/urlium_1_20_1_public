package electricshmoo.urlium.item;

import electricshmoo.urlium.UrlComMod;
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
import java.util.HashMap;
import java.util.Map;

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

            try {
                Map<Object, Object> data = new HashMap<>();
                long unixTime = System.currentTimeMillis();

                data.put("ts", unixTime);
                data.put("x", clicked.getX());
                data.put("y", clicked.getY());
                data.put("z", clicked.getZ());
                data.put("p", pow);
                data.put("device", "wand");
                data.put("hand", methodHand);
                data.put("block", block);
                data.put("blockState", clickedBlock);
                data.put("user", userName);

                UrlComMod.sendPOST(data);

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
}
