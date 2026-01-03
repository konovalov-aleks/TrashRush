package dev.av.konovalov.trashrush;

public enum TrashType {
    PLASTIC(1, 5, -15,
            150, 0, 5,
            R.drawable.trash_bin_plastic),
    PAPER(1, 6, -2,
            900, 0.1f, 100,
            R.drawable.trash_bin_paper),

    GLASS(3, 10, -8,
            300, 0, 0.5f,
            R.drawable.trash_bin_glass),

    METAL(5, 15, -5,
            200, 0, 4,
            R.drawable.trash_bin_metal),

    BATTERY(6, 10, -100,
            50, 0, 500,
            R.drawable.trash_bin_battery);

    public final int unlockLevel;
    public final int reward;
    public final int penalty;
    public final float co2Saved;
    public final float treesSaved;
    public final float waterSaved;
    public final int trashBinResource;

    TrashType(int unlockLevel, int reward, int penalty,
              float co2Saved, float treesSaved, float waterSaved,
              int trashBinResource) {
        this.unlockLevel = unlockLevel;
        this.reward = reward;
        this.penalty = penalty;
        this.co2Saved = co2Saved;
        this.treesSaved = treesSaved;
        this.waterSaved = waterSaved;
        this.trashBinResource = trashBinResource;
    }

    public int getColor() {
        switch (this) {
            case PLASTIC:
                return 0xFF2196F3; // Blue
            case PAPER:
                return 0xFFFFC107;   // Yellow
            case GLASS:
                return 0xFF4CAF50;   // Green
            case METAL:
                return 0xFF9E9E9E;   // Gray
            case BATTERY:
                return 0xFFF44336; // Red
            default:
                return 0xFF000000;
        }
    }
}