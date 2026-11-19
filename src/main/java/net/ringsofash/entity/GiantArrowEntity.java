package net.ringsofash.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;

/** Fleche geante de l'archer : meme comportement qu'une fleche (ramassable, pointe), mais un grand modele 3D et une plus grande boite. */
public class GiantArrowEntity extends Arrow {
    public GiantArrowEntity(EntityType<? extends GiantArrowEntity> type, Level level) { super(type, level); }
}
