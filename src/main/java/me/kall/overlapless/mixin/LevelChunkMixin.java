package me.kall.overlapless.mixin;

import me.kall.overlapless.ext.StructureHolder;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin implements StructureHolder {
    @Unique private boolean overlapless$isFullFilled;

    @Override
    public boolean overlapless$isFullFilled() {
        return this.overlapless$isFullFilled;
    }

    @Override
    public void overlapless$setFullFilled(boolean isFullFilled) {
        this.overlapless$isFullFilled = isFullFilled;
    }
}
