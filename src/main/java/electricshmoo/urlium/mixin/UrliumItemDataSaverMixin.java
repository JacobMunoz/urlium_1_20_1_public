package electricshmoo.urlium.mixin;

import electricshmoo.urlium.UrlComMod;
import electricshmoo.urlium.util.IItemDataSaver;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Mixin(ItemStack.class)
public abstract class UrliumItemDataSaverMixin implements IItemDataSaver {
    private NbtCompound persistData;
    @Override
    public NbtCompound getPersistentData() {
        if (this.persistData == null) {
            this.persistData = new NbtCompound();
        }
        return persistData;
    }

    @Inject(method= "getNbt", at=@At("HEAD"))
    protected  void injectReadMethod(CallbackInfoReturnable<NbtCompound> cir){
//        UrlComMod.LOGGER.info("HIT getNbt " );

//        if (.getReturnValue()!=null) {
//            UrlComMod.LOGGER.info("FOUND DATA: getOrCreateNbt " +cir.getReturnValue().toString());
//            if (cir.getReturnValue().contains("urlium", 10))
//                this.persistData = cir.getReturnValue().getCompound("urlium");
//        }
    }

    @Inject(method="writeNbt", at=@At("HEAD"))
    protected  void injectWriteMethod(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir){
        if (this.persistData != null) {
            nbt.put("urlium",persistData);
            if (persistData.contains("report")) {

                long unixTime = System.currentTimeMillis();
                Map<Object, Object> data = new HashMap<>();
                data.put("device", "book");
                data.put("ts", unixTime);

                try {
                    UrlComMod.sendPOST(data);
                    UrlComMod.LOGGER.info("Reported on book at: " + unixTime );
                } catch (IOException ignore) {
                    UrlComMod.LOGGER.info("Failed to send post report on book at: "+ unixTime );
                }
            }
        }
    }
}
