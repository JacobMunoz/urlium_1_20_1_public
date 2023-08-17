package electricshmoo.urlium.block;


import electricshmoo.urlium.UrlComMod;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static electricshmoo.urlium.UrlComMod.sendPOST;

public class UrlPostBlock extends Block implements PolymerTexturedBlock {
    private final BlockState polymerBlockState;

    public UrlPostBlock(String modelId) {
        super(FabricBlockSettings.copy(Blocks.DIAMOND_BLOCK));
        this.polymerBlockState = PolymerBlockResourceUtils.requestBlock(
                BlockModelType.FULL_BLOCK,
                PolymerBlockModel.of(new Identifier("urlium", modelId)));

    }
    @Override
    public Block getPolymerBlock(BlockState state) {
        return this.polymerBlockState.getBlock();
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return this.polymerBlockState;
    }
    public static final IntProperty POWER = IntProperty.of("power",0,15);

    @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(POWER, ctx.getWorld().getReceivedRedstonePower(ctx.getBlockPos()));
    }


    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (!world.isClient) {
            int pow = state.get(POWER);
            int realpow = world.getReceivedRedstonePower(sourcePos);
            if (pow != world.getReceivedRedstonePower(sourcePos)) {
                if (realpow > 0) {
                    world.scheduleBlockTick(pos, this, 4);
                    try {
                        long unixTime = System.currentTimeMillis();
                        Map<Object, Object> data = new HashMap<>();
                        data.put("device", "block");
                        data.put("method", "sense");
                        data.put("ts", unixTime);
                        data.put("x", pos.getX());
                        data.put("y", pos.getY());
                        data.put("z", pos.getZ());
                        data.put("p", pow);

                        sendPOST(data);

                    } catch (IOException ignore) { }
                } else {
                    world.setBlockState(pos, state.with(POWER, 0), 2);
                    world.scheduleBlockTick(pos, this, 4);
                    try {
                        long unixTime = System.currentTimeMillis();
                        Map<Object, Object> data = new HashMap<>();
                        data.put("device", "block");
                        data.put("method", "sense");
                        data.put("ts", unixTime);
                        data.put("x", pos.getX());
                        data.put("y", pos.getY());
                        data.put("z", pos.getZ());
                        data.put("p", 0);

                        sendPOST(data);

                    } catch (IOException ignore) { }
                }
            }
        }
    }

    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit){
        if (!world.isClient && hand == Hand.OFF_HAND ) {
            int realpow = state.get(POWER);
            String userName = player.getEntityName();
            try {
                long unixTime = System.currentTimeMillis();
                Map<Object, Object> data = new HashMap<>();
                data.put("device", "block");
                data.put("method", "use");
                data.put("user", userName);
                data.put("ts", unixTime);
                data.put("x", pos.getX());
                data.put("y", pos.getY());
                data.put("z", pos.getZ());
                data.put("p", realpow);
                sendPOST(data);
                spawnParticles(world,pos);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return ActionResult.success(world.isClient);
//        return super.onUse(state,world,pos,player,hand,hit);
    }



    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        UrlComMod.LOGGER.info("Hit scheduledTick at "+ pos.getX() + ","+pos.getY()+","+pos.getZ());
        UrlComMod.LOGGER.info("Receiving power? " + (world.isReceivingRedstonePower(pos) ?"Yes":"No"));
        if (state.get(POWER) != 0 ) {
            if (world.isReceivingRedstonePower(pos)) {
                int pow = world.getReceivedRedstonePower(pos);
                world.setBlockState(pos, state.with(POWER,pow), 2);
            } else {
                world.setBlockState(pos, state.with(POWER,0), 2);
            }
        } else {
            if (world.isReceivingRedstonePower(pos)) {
                int pow = world.getReceivedRedstonePower(pos);
                world.setBlockState(pos, state.with(POWER,pow), 2);
            } else {
                world.setBlockState(pos, state.with(POWER,0), 2);
            }
        }
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POWER);
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
                world.addParticle(DustParticleEffect.DEFAULT, (double)pos.getX() + e, (double)pos.getY() + f, (double)pos.getZ() + g, 0.0, 0.0, 0.0);

            }
        }
    }
}
