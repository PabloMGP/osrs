package gg.rsmod.plugins.content.combat.specialattack.weapons.godswords

import gg.rsmod.plugins.content.combat.dealHit
import gg.rsmod.plugins.content.combat.formula.MeleeCombatFormula
import gg.rsmod.plugins.content.combat.specialattack.SpecialAttacks

val SPECIAL_REQUIREMENT = 50

SpecialAttacks.register(SPECIAL_REQUIREMENT, Items.ARMADYL_GODSWORD) {

    // AGS special animation
    player.animate(7074)

    // AGS special graphic
    player.graphic(1222)

    val maxHit = MeleeCombatFormula.getMaxHit(
        player,
        target,
        specialAttackMultiplier = 1.85,
    )

    val accuracy = MeleeCombatFormula.getAccuracy(
        player,
        target,
        specialAttackMultiplier = 2.00,
    )

    val landHit = accuracy >= world.randomDouble()

    player.dealHit(
        target = target,
        maxHit = maxHit,
        landHit = landHit,
        delay = if (target.entityType.isNpc) 1 else 1,
        hitType = HitType.MELEE,
    )
}
