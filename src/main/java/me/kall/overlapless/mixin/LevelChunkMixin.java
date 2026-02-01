package me.kall.overlapless.mixin;

import me.kall.overlapless.ext.Forgettable;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin implements Forgettable {
    @Unique private boolean overlapless$isForgettable = true;

    @Override
    public boolean overlapless$isForgettable() {
        return this.overlapless$isForgettable;
    }

    @Override
    public void overlapless$setForgettable(boolean isForgettable) {
        this.overlapless$isForgettable = isForgettable;
    }
}
