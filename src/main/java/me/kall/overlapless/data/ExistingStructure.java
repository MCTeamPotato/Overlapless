package me.kall.overlapless.data;

import java.util.Objects;

public record ExistingStructure(int minY, int maxY, String existing) {
    @Override
    public boolean equals(Object object) {
        if (object instanceof ExistingStructure existingStructure) {
            return existingStructure.minY == this.minY && existingStructure.maxY == this.maxY && Objects.equals(existingStructure.existing, this.existing);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.minY, this.maxY, this.existing);
    }
}
