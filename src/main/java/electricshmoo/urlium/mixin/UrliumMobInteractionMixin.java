package electricshmoo.urlium.mixin;

import electricshmoo.urlium.UrlComMod;
import electricshmoo.urlium.util.IEntityDataSaver;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static electricshmoo.urlium.UrlComMod.*;
import static java.lang.Math.floor;

@Mixin(MobEntity.class)
public abstract class UrliumMobInteractionMixin implements  IEntityDataSaver{

    private NbtCompound persistData;
    @Override
    public NbtCompound getPersistentData() {
        if (this.persistData == null) {
            this.persistData = new NbtCompound();
        }
        return persistData;
    }

    @Inject(method="readCustomDataFromNbt", at=@At("HEAD"))
    protected  void injectReadMethod(NbtCompound nbt, CallbackInfo info){
        if (nbt.contains("urlium",10))
        this.persistData = nbt.getCompound("urlium");
    }

    @Inject(method="writeCustomDataToNbt", at=@At("HEAD"))
    protected  void injectWriteMethod(NbtCompound nbt, CallbackInfo ci){
        if (this.persistData != null) {
            nbt.put("urlium",persistData);
            if (persistData.contains("report")) { // consider if writing is necessary
            }
        }
    }

    @Inject(method="interact",at=@At("HEAD"))
    protected void mobOverride(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir){
        if (!player.getWorld().isClient() && this.persistData != null) {
            if (persistData.contains("report")) {
                String userName = player.getEntityName();
                BlockPos position = player.getBlockPos();
                long unixTime = System.currentTimeMillis();
                Map<Object, Object> data = new HashMap<>();
                data.put("x", position.getX());
                data.put("y", position.getY());
                data.put("z", position.getZ());
                data.put("user", userName);
                data.put("device", "entity");
                data.put("method", "interact");
                data.put("entityid", persistData.getString("entityid"));
                data.put("entitydisplayname", persistData.getString("entitydisplayname"));
                data.put("entitytype",persistData.getString("entitytype"));
                data.put("ts", floor(unixTime/100));
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
        }
    }

    @Inject(method="interactWithItem",at=@At("HEAD"))
    protected void mobWandOverride(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir){
    if (!player.getWorld().isClient() ){
        ItemStack itemStack = player.getStackInHand(hand);
        NbtCompound pdata = getPersistentData();
        MobEntity mob = (MobEntity)(Object)this;
        String itemName = itemStack.getItem().toString();
        if (itemName.equals("item/urlconfigwand_item")) {

            boolean reporting = false;
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
                    pdata.putString("user", player.getEntityName());
                    pdata.putString("entityid", mob.getUuidAsString());
                    pdata.putString("entitytype", mob.getType().getName().getString());
                    pdata.putString("entitydisplayname", mob.getDisplayName().getString());
                }
                BlockPos position = player.getBlockPos();
                long unixTime = System.currentTimeMillis();
                Map<Object, Object> data = new HashMap<>();
                data.put("x", position.getX());
                data.put("y", position.getY());
                data.put("z", position.getZ());
                data.put("user", player.getEntityName());
                data.put("device", "configwand");
                data.put("target", "entity");
                data.put("reporting", reporting);
                data.put("entityid", mob.getUuidAsString());
                data.put("entitydisplayname", mob.getDisplayName().getString());
                data.put("entitytype",mob.getType().getName().getString());
                data.put("ts", unixTime);
                try {
                    sendPOST(data);
                } catch (IOException ignore) {
                }
            } else if (itemName.equals("item/urlpostwand_item")) {
                BlockPos position = mob.getBlockPos();
                long unixTime = System.currentTimeMillis();
                Map<Object, Object> data = new HashMap<>();
                data.put("x", position.getX());
                data.put("y", position.getY());
                data.put("z", position.getZ());
                data.put("user", player.getEntityName());
                data.put("device", "wand");
                data.put("target", "entity");
                data.put("hand", hand.toString());
                data.put("entityid", mob.getUuidAsString());
                data.put("entitydisplayname", mob.getDisplayName().getString());
                data.put("entitytype",mob.getType().getName().getString());
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
        }
    }
}
