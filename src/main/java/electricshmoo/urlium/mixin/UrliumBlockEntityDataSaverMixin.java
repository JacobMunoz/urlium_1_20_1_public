package electricshmoo.urlium.mixin;

import com.google.gson.Gson;
import electricshmoo.urlium.UrlComMod;
import electricshmoo.urlium.util.IBlockEntityDataSaver;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static electricshmoo.urlium.UrlComMod.filterUpdate;
import static electricshmoo.urlium.UrlComMod.sendPOST;
import static net.minecraft.block.ChestBlock.getInventory;
import static net.minecraft.block.DaylightDetectorBlock.INVERTED;

@Mixin(BlockEntity.class)
public abstract class UrliumBlockEntityDataSaverMixin implements IBlockEntityDataSaver {
    @Shadow @Nullable protected World world;
    private NbtCompound persistData;
    @Override
    public NbtCompound getPersistentData() {
        if (this.persistData == null) {
            this.persistData = new NbtCompound();
        }
        return persistData;
    }

    private String getInventoryString(Inventory inv){
        Gson gson = new Gson();
        StringBuilder inventoryString = new StringBuilder();
        boolean inventorySuffix = false;
        inventoryString.append("{");
        for (int slot = 0; slot < inv.size(); slot++) {
            ItemStack stack = inv.getStack(slot);
            if (!stack.isEmpty()) {
                if (inventorySuffix) inventoryString.append(",");
                inventoryString.append("\""+Integer.toString( slot )+"\":{");
                inventoryString.append("\"count\":" + Integer.toString(stack.getCount()) + ",");
                inventoryString.append("\"item\":" + gson.toJson(stack.getItem().toString()) + ",");
                inventoryString.append("\"name\":" + gson.toJson(stack.getName().getString()));
                inventoryString.append("}");
                inventorySuffix = true;
            }
        }
        inventoryString.append("}");

        return inventoryString.toString();
    }

    @Inject(method="readNbt", at=@At("HEAD"))
    protected  void injectReadMethod(NbtCompound nbt, CallbackInfo info){
        if (nbt.contains("urlium",10))
            this.persistData = nbt.getCompound("urlium");
    }

    @Inject(method="writeNbt", at=@At("HEAD"))
    protected  void injectWriteMethod(NbtCompound nbt, CallbackInfo info){
        if (this.persistData != null) {
            nbt.put("urlium",persistData);
            if (persistData.contains("report")) {

                int[] coords = persistData.getIntArray("report");
                BlockPos blockPos = new BlockPos(coords[0],coords[1],coords[2] );
                BlockState blockState = world.getBlockState(blockPos);

                StringBuilder blockStateStringBuilder = new StringBuilder(BlockArgumentParser.stringifyBlockState(blockState));
                String blockStateString = blockStateStringBuilder.toString();

                long unixTime = System.currentTimeMillis();
                Map<Object, Object> data = new HashMap<>();
                String blockType = persistData.getString("blocktype");
                StringBuilder hashSource = new StringBuilder();

                if (blockType.equals("chest")) {
                    ChestBlock block = (ChestBlock) blockState.getBlock();
                    Inventory inv = getInventory(block, blockState, world, blockPos, true);
                    data.put("device", "chest");
                    data.put("inventory", getInventoryString(inv));
                    data.put("blockState", blockStateString);

                } else if (blockType.equals("trapped_chest")) {
                    TrappedChestBlock block = (TrappedChestBlock) blockState.getBlock();
                    Inventory inv  = getInventory(block, blockState, world, blockPos, true);
                    data.put("p", block.getWeakRedstonePower(blockState,world,blockPos, Direction.UP));
                    data.put("device", "trapped_chest");
                    data.put("inventory", getInventoryString(inv));
                    data.put("blockState", blockStateString);

                } else if (blockType.equals("barrel")) {
                    BarrelBlockEntity barrelEntity = (BarrelBlockEntity) world.getBlockEntity(blockPos);

                    Gson gson = new Gson();
                    StringBuilder inventoryString = new StringBuilder();
                    inventoryString.append("{");
                    boolean inventorySuffix = false;
                    for (int slot = 0; slot < barrelEntity.size(); slot++) {
                        ItemStack stack = barrelEntity.getStack(slot);
                        if (!stack.isEmpty()) {
                            if (inventorySuffix) inventoryString.append(",");
                            inventoryString.append("\""+Integer.toString( slot )+"\":{");
                            inventoryString.append("\"count\":"+Integer.toString( stack.getCount() )+",");
                            inventoryString.append("\"item\":"+gson.toJson(stack.getItem().toString())+",");
                            inventoryString.append("\"name\":"+gson.toJson(stack.getName().getString()));
                            inventoryString.append("}");
                            inventorySuffix = true;
                        }
                    }
                    inventoryString.append("}");
                    data.put("device", "barrel");
                    data.put("inventory", inventoryString.toString());
                    data.put("blockState", blockStateString);

                } else if (blockType.equals("hopper")) {
                    HopperBlockEntity hopperEntity = (HopperBlockEntity) world.getBlockEntity(blockPos);
                    Gson gson = new Gson();
                    StringBuilder inventoryString = new StringBuilder();
                    inventoryString.append("{");
                    boolean inventorySuffix = false;
                    for (int slot = 0; slot < hopperEntity.size(); slot++) {
                        ItemStack stack = hopperEntity.getStack(slot);
                        if (!stack.isEmpty()) {
                            if (inventorySuffix) inventoryString.append(",");
                            inventoryString.append("\""+Integer.toString( slot )+"\":{");
                            inventoryString.append("\"count\":"+Integer.toString( stack.getCount() )+",");
                            inventoryString.append("\"item\":"+gson.toJson(stack.getItem().toString())+",");
                            inventoryString.append("\"name\":"+gson.toJson(stack.getName().getString()));
                            inventoryString.append("}");
                            inventorySuffix = true;
                        }
                    }
                    inventoryString.append("}");
                    data.put("device", "hopper");
                    data.put("inventory", inventoryString.toString());
                    data.put("blockState", blockStateString);

                } else if (blockType.equals("dispenser")) {
                    DispenserBlockEntity dispenserEntity = (DispenserBlockEntity) world.getBlockEntity(blockPos);
                    Gson gson = new Gson();
                    StringBuilder inventoryString = new StringBuilder();
                    inventoryString.append("{");
                    boolean inventorySuffix = false;
                    for (int slot = 0; slot < dispenserEntity.size(); slot++) {
                        ItemStack stack = dispenserEntity.getStack(slot);
                        if (!stack.isEmpty()) {
                            if (inventorySuffix) inventoryString.append(",");
                            inventoryString.append("\""+Integer.toString( slot )+"\":{");
                            inventoryString.append("\"count\":"+Integer.toString( stack.getCount() )+",");
                            inventoryString.append("\"item\":"+gson.toJson(stack.getItem().toString())+",");
                            inventoryString.append("\"name\":"+gson.toJson(stack.getName().getString()));
                            inventoryString.append("}");
                            inventorySuffix = true;
                        }
                    }
                    inventoryString.append("}");
                    data.put("device", "dispenser");
                    data.put("inventory", inventoryString.toString());
                    data.put("blockState", blockStateString);

                } else if (blockType.equals("dropper")) {
                    DropperBlockEntity dropperEntity = (DropperBlockEntity) world.getBlockEntity(blockPos);
                    Gson gson = new Gson();
                    StringBuilder inventoryString = new StringBuilder();
                    inventoryString.append("{");
                    boolean inventorySuffix = false;
                    for (int slot = 0; slot < dropperEntity.size(); slot++) {
                        ItemStack stack = dropperEntity.getStack(slot);
                        if (!stack.isEmpty()) {
                            if (inventorySuffix) inventoryString.append(",");
                            inventoryString.append("\""+Integer.toString( slot )+"\":{");
                            inventoryString.append("\"count\":"+Integer.toString( stack.getCount() )+",");
                            inventoryString.append("\"item\":"+gson.toJson(stack.getItem().toString())+",");
                            inventoryString.append("\"name\":"+gson.toJson(stack.getName().getString()));
                            inventoryString.append("}");
                            inventorySuffix = true;
                        }
                    }
                    inventoryString.append("}");
                    data.put("device", "dropper");
                    data.put("inventory", inventoryString.toString());
                    data.put("blockState", blockStateString);

                } else if (blockType.equals("lectern")) {
                    LecternBlock block = (LecternBlock) blockState.getBlock();
                    LecternBlockEntity blockEntity = (LecternBlockEntity) world.getBlockEntity(blockPos);
                    data.put("device", "lectern");
                    StringBuilder bookContents = new StringBuilder();

                    if (blockEntity.hasBook()) {
                        ItemStack book = blockEntity.getBook();
                        NbtCompound bookNbt = book.getNbt();
                        bookContents.append(bookNbt.toString());
                        data.put("hasBook", 1);
                        data.put("bookContents", bookNbt.get("pages").toString());

                        if (book.getItem() instanceof WrittenBookItem) { // Check if it's a written book
                            if (bookNbt != null && bookNbt.contains("author", NbtElement.STRING_TYPE) && bookNbt.contains("title", NbtElement.STRING_TYPE)) {
                                data.put("bookAuthor", bookNbt.getString("author"));
                                data.put("bookTitle", bookNbt.getString("title"));
                                data.put("isWritten", 1);
                                StringBuilder bookTextJson = new StringBuilder();
                                bookTextJson.append("[");
                                NbtList pagesNbt = bookNbt.getList("pages", NbtElement.STRING_TYPE);
                                for (int i = 0; i < pagesNbt.size(); i++) {
                                    String pageJson = pagesNbt.getString(i); // Page content in JSON string format
                                    bookTextJson.append(pageJson);
                                    if (i < pagesNbt.size() - 1) {
                                        bookTextJson.append(","); // Separate JSON strings with commas
                                    }
                                }
                                bookTextJson.append("]");
                                data.put("bookContents", bookTextJson.toString());
                            }
                        } else {
                            data.put("isWritten", 0);
                        }
                    } else {
                        data.put("bookContents", "");
                        data.put("hasBook", 0);
                    }
                    data.put("p", block.getWeakRedstonePower(blockState,world,blockPos, Direction.UP));
                    data.put("blockState", blockStateString);

                } else if (blockType.equals("sculk_sensor")) {
                    SculkSensorBlockEntity blockEntity = (SculkSensorBlockEntity) world.getBlockEntity(blockPos);
                    data.put("device", "sculk_sensor");
                    data.put("freq", blockEntity.getLastVibrationFrequency());
                    data.put("phase", SculkSensorBlock.getPhase(blockState).toString());
                    data.put("p", blockState.getWeakRedstonePower(world, blockPos, Direction.UP));
                    data.put("blockState", blockStateString);
                } else if (blockType.equals("daylight_detector")) {
                    DaylightDetectorBlockEntity blockEntity = (DaylightDetectorBlockEntity) world.getBlockEntity(blockPos);
                    data.put("device", "daylight_detector");
                    data.put("inverted",  blockState.get(INVERTED));
                    data.put("p", blockState.getWeakRedstonePower(world, blockPos, Direction.UP));
                    data.put("blockState", blockStateString);
                }
                /*else if (blockType.indexOf("shulker_box")>=0) {
                    ShulkerBoxBlockEntity shulkerEntity = (ShulkerBoxBlockEntity) world.getBlockEntity(blockPos);
                    Gson gson = new Gson();
                    StringBuilder inventoryString = new StringBuilder();

                    inventoryString.append("{");
                    for (int slot = 0; slot < shulkerEntity.size(); slot++) {
                        ItemStack stack = shulkerEntity.getStack(slot);
                        if (!stack.isEmpty()) {
                            if (slot > 0) inventoryString.append(",");
                            inventoryString.append("\""+Integer.toString( slot )+"\":{");
                            inventoryString.append("\"count\":"+Integer.toString( stack.getCount() )+",");
                            inventoryString.append("\"item\":"+gson.toJson(stack.getItem().toString())+",");
                            inventoryString.append("\"name\":"+gson.toJson(stack.getName().getString()));
                            inventoryString.append("}");
                        }
                    }
                    inventoryString.append("}");
                    data.put("device", blockType);
                    data.put("inventory", inventoryString.toString());
                }*/
                else if (blockType.indexOf("_sign")>0) {
                    Block blockToCheck = world.getBlockState(blockPos).getBlock();
                    if (blockToCheck instanceof AbstractSignBlock) {
                        BlockEntity blockEntity = world.getBlockEntity(blockPos);

                        if (blockEntity instanceof SignBlockEntity) {
                            SignBlockEntity signBlockEntity = (SignBlockEntity) blockEntity;

                            Text[] frontTextMessages = signBlockEntity.getFrontText().getMessages(false);
                            String frontText = "[";
                            for (int i = 0; i < frontTextMessages.length; i++) {
                                frontText  +=  Text.Serializer.toJson(frontTextMessages[i]);; // Extract the text content as a plain string
                                if (i+1 < frontTextMessages.length) frontText += ",";
                                else frontText += "]";
                            }
                            data.put("frontText", frontText);
//                            data.put("frontColor", signBlockEntity.getFrontText().getColor().asString());  //just fetch via RCON... not here, right?
                            Text[] backTextMessages = signBlockEntity.getBackText().getMessages(false);
                            String backText = "[";
                            for (int i = 0; i < backTextMessages.length; i++) {
                                backText  +=  Text.Serializer.toJson(backTextMessages[i]);; // Extract the text content as a plain string
                                if (i+1 < backTextMessages.length) backText += ",";
                                else backText += "]";
                            }
                            data.put("backText", backText);
                        }
                    }
                    data.put("device", persistData.getString("blocktype"));
                    data.put("blockState", blockStateString);

                } else {
                    return;
                }
                data.put("x", coords[0]);
                data.put("y", coords[1]);
                data.put("z", coords[2]);
                String hash = UrlComMod.generateMapHash(data);
                data.put("hash", hash);
                if ( filterUpdate(data) ) {
                    data.put("ts", unixTime);
                    try {
                        sendPOST(data);
                    } catch (IOException ignore) {
                    }
                }
            }
        }
    }
}
