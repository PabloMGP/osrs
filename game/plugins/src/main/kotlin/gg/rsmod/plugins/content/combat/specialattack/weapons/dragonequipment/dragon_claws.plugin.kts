package gg.rsmod.plugins.content.combat.specialattack.weapons.dragonequipment

import gg.rsmod.plugins.content.combat.dealHit
import gg.rsmod.plugins.content.combat.formula.MeleeCombatFormula
import gg.rsmod.plugins.content.combat.specialattack.SpecialAttacks

val SPECIAL_REQUIREMENT = 50

SpecialAttacks.register(SPECIAL_REQUIREMENT, Items.DRAGON_CLAWS) {

    // Dragon claws special animation
    player.animate(10961)

    val accuracy = MeleeCombatFormula.getAccuracy(
        player,
        target,
        specialAttackMultiplier = 1.25
    )

    val landHit = accuracy >= world.randomDouble()

    val multipliers = listOf(
        1.10,
        0.80,
        0.60,
        0.45,
        0.35,
        0.30,
        0.25,
        0.20
    )

    for ((i, multiplier) in multipliers.withIndex()) {

        val maxHit = MeleeCombatFormula.getMaxHit(
            player,
            target,
            specialAttackMultiplier = multiplier,
        )

        player.dealHit(
            target = target,
            maxHit = maxHit,
            landHit = landHit,

            // First 4 hits immediately, last 4 shortly after
            delay = when {
                i < 4 -> 1
                else -> 2
            },

            hitType = HitType.MELEE,
        )
    }
}
