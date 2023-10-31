package electricshmoo.urlium.mixin;

import electricshmoo.urlium.UrlComMod;
import electricshmoo.urlium.util.IBlockEntityDataSaver;
import net.minecraft.block.BlockState;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ComparatorBlockEntity;
import net.minecraft.util.math.BlockPos;
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

@Mixin(ComparatorBlock.class)
public abstract class UrliumComparatorPowerUpdateMixin  implements IBlockEntityDataSaver {

    @Inject(method="update",at=@At("TAIL"))
    protected void detectPowerChange(World world, BlockPos pos, BlockState state, CallbackInfo ci){
        IBlockEntityDataSaver clickedBlockEntity = ((IBlockEntityDataSaver) world.getBlockEntity(pos));
        var pdata = clickedBlockEntity.getPersistentData();
        if (pdata.contains("report")) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            ComparatorBlockEntity comparatorBlockEntity = (ComparatorBlockEntity)blockEntity;
            assert comparatorBlockEntity != null;
            long unixTime = System.currentTimeMillis();
            Map<Object, Object> data = new HashMap<>();
            data.put("device", "comparator");
            data.put("x", pos.getX());
            data.put("y", pos.getY());
            data.put("z", pos.getZ());
            data.put("p", comparatorBlockEntity.getOutputSignal());
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
}
