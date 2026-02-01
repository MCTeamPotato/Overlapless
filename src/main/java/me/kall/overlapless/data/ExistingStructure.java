package me.kall.overlapless.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record ExistingStructure(int minY, int maxY, String existing) {
    @Override
    public @NotNull String toString() {
        return this.minY + ";" + this.maxY + ";" + this.existing;
    }

    @Contract("_ -> new")
    public static @NotNull ExistingStructure fromString(@NotNull String string) {
        String[] parts = string.split(";");
        return new ExistingStructure(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), parts[2]);
    }
}
