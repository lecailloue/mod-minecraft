package net.ringsofash.item;

import java.util.List;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Les emplacements d'une arme, pour les dessiner dans l'infobulle : une case par
 * emplacement, l'icone de la rune a l'interieur quand il est pris. Le dessin lui-meme
 * est cote client (voir RuneSlotsTooltip).
 */
public record RuneSlots(List<Rune> runes, int slots) implements TooltipComponent {
}
