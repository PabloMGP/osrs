package gg.rsmod.plugins.content.combat.scripts.impl

import gg.rsmod.game.model.combat.CombatClass
import gg.rsmod.game.model.combat.CombatScript
import gg.rsmod.game.model.combat.StyleType
import gg.rsmod.game.model.combat.WeaponStyle
import gg.rsmod.game.model.entity.Npc
import gg.rsmod.game.model.queue.QueueTask
import gg.rsmod.plugins.api.HitType
import gg.rsmod.plugins.content.combat.*
import gg.rsmod.plugins.content.combat.formula.MeleeCombatFormula
import gg.rsmod.plugins.content.combat.formula.RangedCombatFormula

object GeneralGraardorCombatScript : CombatScript() {

    override val ids = intArrayOf(6260)

    override suspend fun handleSpecialCombat(it: QueueTask) {
        val npc = it.player.getCombatTarget() as? Npc ?: return
        var target = npc.getCombatTarget() ?: return
        var attackCounter = 0

        while (npc.canEngageCombat(target)) {
            npc.facePawn(target)

            if (npc.moveToAttackRange(it, target, distance = 1, projectile = false) && npc.isAttackDelayReady()) {
                attackCounter++

                if (attackCounter % 4 == 0) {
                    npc.prepareAttack(CombatClass.RANGED, StyleType.CRUSH, WeaponStyle.ACCURATE)
                    npc.animate(npc.combatDef.attackAnimation)

                    npc.dealHit(
                        target = target,
                        formula = RangedCombatFormula,
                        delay = 1,
                        type = HitType.RANGE,
                    )

                    npc.postAttackLogic(target)
                } else {
                    npc.prepareAttack(CombatClass.MELEE, StyleType.CRUSH, WeaponStyle.ACCURATE)
                    npc.animate(npc.combatDef.attackAnimation)

                    npc.dealHit(
                        target = target,
                        formula = MeleeCombatFormula,
                        delay = 1,
                        type = HitType.MELEE,
                    )

                    npc.postAttackLogic(target)
                }
            }

            it.wait(1)
            target = npc.getCombatTarget() ?: break
        }

        npc.resetFacePawn()
        npc.removeCombatTarget()
    }
}
