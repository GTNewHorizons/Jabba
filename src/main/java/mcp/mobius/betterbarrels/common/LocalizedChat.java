package mcp.mobius.betterbarrels.common;

public enum LocalizedChat {

    BONK,
    DOWNGRADE,
    COREUPGRADE_EXISTS,
    UPGRADE_EXISTS,
    UPGRADE_REMOVE,
    UPGRADE_INSUFFICIENT,
    UPGRADE_REQUIRED,
    FACADE_REDSTONE,
    FACADE_HOPPER,
    STACK_REMOVE,
    BSPACE_PREVENT,
    BSPACE_REMOVE,
    BSPACE_NOREACT,
    BSPACE_CONTENT,
    BSAPCE_STRUCTURE,
    BSPACE_FORK_RESONATING,
    BSPACE_FORK_LOST,
    BSPACE_RESONATING,
    HAMMER_NORMAL,
    HAMMER_BSPACE,
    HAMMER_REDSTONE,
    HAMMER_HOPPER,
    HAMMER_STORAGE,
    HAMMER_STRUCTURAL,
    HAMMER_VOID,
    HAMMER_CREATIVE,
    DOLLY_TOO_COMPLEX;

    public final String localizationKey;

    private LocalizedChat() {
        this.localizationKey = "text.jabba." + this.name().toLowerCase();
    }
}
