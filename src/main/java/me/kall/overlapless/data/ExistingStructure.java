package me.kall.overlapless.data;

import java.util.Objects;

public final class ExistingStructure {
    private final int minY;
    private final int maxY;
    private final String existing;

    private final int hash;

    public ExistingStructure(int minY, int maxY, String existing) {
        this.minY = minY;
        this.maxY = maxY;
        this.existing = existing;
        this.hash = Objects.hash(this.minY, this.maxY, this.existing);
    }

    public int minY() {
        return this.minY;
    }

    public int maxY() {
        return this.maxY;
    }

    public String existing() {
        return this.existing;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof ExistingStructure existingStructure) {
            return existingStructure.minY == this.minY && existingStructure.maxY == this.maxY && Objects.equals(existingStructure.existing, this.existing);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.hash;
    }
}
