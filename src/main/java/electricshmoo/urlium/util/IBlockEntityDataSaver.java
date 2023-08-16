package electricshmoo.urlium.util;

import net.minecraft.nbt.NbtCompound;

public interface IBlockEntityDataSaver {
    NbtCompound getPersistentData();
}
