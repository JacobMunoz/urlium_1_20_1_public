package electricshmoo.urlium.mixin;

import electricshmoo.urlium.UrlComMod;
import electricshmoo.urlium.util.IBlockEntityDataSaver;
import net.minecraft.block.BlockState;
import net.minecraft.block.SculkSensorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SculkSensorBlockEntity;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static electricshmoo.urlium.UrlComMod.filterUpdate;
import static electricshmoo.urlium.UrlComMod.generateMapHash;

@Mixin(SculkSensorBlock.class)
public abstract class UrliumSculkSensorUpdateMixin implements IBlockEntityDataSaver {

    @Inject(method="scheduledTick",at=@At("TAIL"))
    protected void detectScheduledTickChange(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci){
        IBlockEntityDataSaver clickedBlockEntity = ((IBlockEntityDataSaver) world.getBlockEntity(pos));
        var pdata = clickedBlockEntity.getPersistentData();
        if (pdata.contains("report")) {
            reportSculkEvent(world, pos, state);
        }
    }

    @Inject(method="setCooldown",at=@At("TAIL"))
    private static void detectCooldownChange(World world, BlockPos pos, BlockState state, CallbackInfo ci){
        IBlockEntityDataSaver clickedBlockEntity = ((IBlockEntityDataSaver) world.getBlockEntity(pos));
        var pdata = clickedBlockEntity.getPersistentData();
        if (pdata.contains("report")) {
            reportSculkEvent( world.getServer().getWorld(world.getRegistryKey()), pos, state);
        }
    }
    @Inject(method="setActive",at=@At("TAIL"))
    private void detectActiveChange(Entity sourceEntity, World world, BlockPos pos, BlockState state, int power, int frequency, CallbackInfo ci){
        IBlockEntityDataSaver clickedBlockEntity = ((IBlockEntityDataSaver) world.getBlockEntity(pos));
        var pdata = clickedBlockEntity.getPersistentData();
        if (pdata.contains("report")) {
            reportSculkEvent( world.getServer().getWorld(world.getRegistryKey()), pos, state);
        }
    }


    static private void reportSculkEvent(ServerWorld world, BlockPos pos, BlockState state){
        BlockEntity blockEntity = world.getBlockEntity(pos);
        SculkSensorBlockEntity sensorBlockEntity = (SculkSensorBlockEntity)blockEntity;
        assert sensorBlockEntity != null;

        StringBuilder blockStateStringBuilder = new StringBuilder(BlockArgumentParser.stringifyBlockState(state));
        String blockStateString = blockStateStringBuilder.toString();

        long unixTime = System.currentTimeMillis();
        Map<Object, Object> data = new HashMap<>();
        data.put("device", "sculk_sensor");
        data.put("freq", sensorBlockEntity.getLastVibrationFrequency());
        data.put("phase", SculkSensorBlock.getPhase(state).toString());
        data.put("p", state.getWeakRedstonePower(world,pos, Direction.UP));
        data.put("blockState", blockStateString);
        data.put("x", pos.getX());
        data.put("y", pos.getY());
        data.put("z", pos.getZ());

        String hash = generateMapHash(data);
        data.put("hash", hash);
        if ( filterUpdate(data) ) {
            data.put("ts", unixTime);
            try {
                UrlComMod.sendPOST(data);
            } catch (IOException ignore) {
            }
        }
    }
}
