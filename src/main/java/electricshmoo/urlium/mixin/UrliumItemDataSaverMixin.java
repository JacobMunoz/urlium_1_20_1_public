package electricshmoo.urlium.mixin;

import electricshmoo.urlium.util.IItemDataSaver;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;

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

    /*
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
                data.put("device", "item");
                data.put("ts", unixTime);

                try {
                    UrlComMod.sendPOST(data);
                    UrlComMod.LOGGER.info("Reported on item at: " + unixTime );
                } catch (IOException ignore) {
                    UrlComMod.LOGGER.info("Failed to send post report on item at: "+ unixTime );
                }
            }
        }
    }

     */
}
