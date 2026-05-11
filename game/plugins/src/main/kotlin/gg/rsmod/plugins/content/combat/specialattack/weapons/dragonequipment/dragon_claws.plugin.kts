package gg.rsmod.plugins.content.combat.specialattack.weapons.dragonequipment

import gg.rsmod.plugins.content.combat.dealHit
import gg.rsmod.plugins.content.combat.formula.MeleeCombatFormula
import gg.rsmod.plugins.content.combat.specialattack.SpecialAttacks

val SPECIAL_REQUIREMENT = 50

SpecialAttacks.register(SPECIAL_REQUIREMENT, Items.DRAGON_CLAWS) {
    player.animate(Anims.DRAGON_CLAWS_SPECIAL)

    val accuracy = MeleeCombatFormula.getAccuracy(player, target, specialAttackMultiplier = 1.25)
    val landHit = accuracy >= world.randomDouble()

    for (i in 0 until 4) {
        val multiplier = when (i) {
            0 -> 1.10
            1 -> 0.55
            else -> 0.25
        }

        val maxHit = MeleeCombatFormula.getMaxHit(
            player,
            target,
            specialAttackMultiplier = multiplier,
        )

        player.dealHit(
            target = target,
            maxHit = maxHit,
            landHit = landHit,
            delay = if (target.entityType.isNpc) i + 1 else 1,
            hitType = HitType.MELEE,
        )
    }
}
