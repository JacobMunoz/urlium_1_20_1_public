package electricshmoo.urlium.mixin;

import electricshmoo.urlium.UrlComMod;
import electricshmoo.urlium.util.IEntityDataSaver;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
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

import static electricshmoo.urlium.UrlComMod.filterUpdate;
import static electricshmoo.urlium.UrlComMod.generateMapHash;
import static java.lang.Math.floor;

@Mixin(Entity.class)
public abstract class UrliumEntityResponderMixin implements IEntityDataSaver {

    private NbtCompound persistData;
    @Override
    public NbtCompound getPersistentData() {
        if (this.persistData == null) {
            this.persistData = new NbtCompound();
        }
        return persistData;
    }

    @Inject(method="readNbt", at=@At("HEAD"))
    protected  void injectReadMethod(NbtCompound nbt, CallbackInfo info){
        if (nbt.contains("urlium",10))
            this.persistData = nbt.getCompound("urlium");
    }

    @Inject(method="writeNbt", at=@At("HEAD"))
    protected  void injectWriteMethod(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir){
        if (this.persistData != null) {
            nbt.put("urlium",persistData);
        }
    }
    @Inject(method="interact",at=@At("HEAD"))
    protected void interact(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        //Entity entity = (Entity)(Object)this;
        if (this.persistData != null ) {
            if (persistData.contains("report")  ) {
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
    /*
        @Inject(method="remove",at=@At("HEAD"))
        protected void mobDeath(Entity.RemovalReason reason, CallbackInfo ci) {
            if (this.persistData != null ) {
                if (persistData.contains("report")  ) {
                    reportEntityEvent("death","","");
                }
            }
        }

        @Inject(method="setOnFire",at=@At("HEAD"))
        protected void mobBurning(CallbackInfo ci) {
            if (this.persistData != null ) {
                if (persistData.contains("report")  ) {
                    reportEntityEvent("burning","","");
                }
            }
        }
        @Inject(method="setOnFireFor",at=@At("HEAD"))
        protected void mobBurningFor(int seconds, CallbackInfo ci) {
            if (this.persistData != null ) {
                if (persistData.contains("report")  ) {
                    reportEntityEvent("burning", "for " + seconds +" seconds","");
                }
            }
        }
    @Inject(method="extinguish",at=@At("HEAD"))
    protected void mobExtinguished(CallbackInfo ci) {
        if (this.persistData != null ) {
            if (persistData.contains("report")  ) {
                reportEntityEvent("extinguished","","");
            }
        }
    }
    @Inject(method="damage",at=@At("HEAD"))
    protected void mobDamaged(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.persistData != null ) {
            if (persistData.contains("report")  ) {
                reportEntityEvent("damaged", Float.toString(amount) + " from " + source.getSource().getEntityName(), source.getAttacker().getEntityName());
            }
        }
    }
    @Inject(method="startRiding",at=@At("HEAD"))
    protected void mobRideStart(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (this.persistData != null ) {
            if (persistData.contains("report")  ) {
                reportEntityEvent("riding", "start", "");
            }
        }
    }
    @Inject(method="stopRiding",at=@At("HEAD"))
    protected void mobRideStop(CallbackInfo ci) {
        if (this.persistData != null ) {
            if (persistData.contains("report")  ) {
                reportEntityEvent("riding", "stop", "");
            }
        }
    }

    private void reportEntityEvent(String eventType, String details, String source){
        Entity entity = (Entity)(Object)this;
        BlockPos position = entity.getBlockPos();
        long unixTime = System.currentTimeMillis();
        Map<Object, Object> data = new HashMap<>();
        data.put("x", position.getX());
        data.put("y", position.getY());
        data.put("z", position.getZ());
        data.put("device", "entity");
        data.put("method", "report");
        data.put("event", eventType);
        if (!details.equals("")) data.put("details", details);
        if (!source.equals("")) data.put("source", source);
        data.put("entityid", persistData.getString("entityid"));
        data.put("entitydisplayname", persistData.getString("entitydisplayname"));
        data.put("entitytype",persistData.getString("entitytype"));
        data.put("ts", unixTime);
        try {
            UrlComMod.sendPOST(data);
        } catch (IOException ignore) {
        }
    }
      */
}
