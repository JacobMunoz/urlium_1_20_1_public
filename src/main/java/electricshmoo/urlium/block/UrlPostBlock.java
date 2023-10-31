package electricshmoo.urlium.block;


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
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
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
//            int pow = state.get(POWER);
            int realpow = world.getReceivedStrongRedstonePower(pos);

                world.setBlockState(pos, state.with(POWER, realpow), 2);
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
                    data.put("p", realpow);

                    sendPOST(data);

                } catch (IOException ignore) { }
        }
    }

    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit){
        if (!world.isClient ) {
            int realpow = state.get(POWER);
            String userName = player.getEntityName();
            try {
                long unixTime = System.currentTimeMillis();
                Map<Object, Object> data = new HashMap<>();
                data.put("device", "block");
                data.put("method", "use");
                data.put("user", userName);
                if (player.isUsingItem()){

                    ItemStack itemStack = player.getStackInHand(hand);
                    data.put("itemtype", itemStack.getItem().toString());
                    data.put("itemname", itemStack.getName().getString());
                }
                data.put("ts", unixTime);
                data.put("x", pos.getX());
                data.put("y", pos.getY());
                data.put("z", pos.getZ());
                data.put("p", realpow);
                sendPOST(data);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            /*
            Item sound for future setting?  - or leave it up to server response?

            Identifier soundEventId = new Identifier("minecraft", "ui.button.click");
            SoundEvent vanillaSoundEvent = Registries.SOUND_EVENT.get(soundEventId);
            SoundEvent soundEvent = new PolymerSoundEvent( soundEventId, 3f, true, vanillaSoundEvent );
            player.playSound(soundEvent, 1,1 );
            */
        }
        return ActionResult.success(world.isClient);
    }



    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
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

}
