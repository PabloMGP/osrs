package gg.rsmod.plugins.content.combat.scripts.impl

import gg.rsmod.game.model.combat.CombatClass
import gg.rsmod.game.model.combat.CombatScript
import gg.rsmod.game.model.combat.StyleType
import gg.rsmod.game.model.combat.WeaponStyle
import gg.rsmod.game.model.queue.QueueTask
import gg.rsmod.plugins.api.HitType
import gg.rsmod.plugins.api.ext.*
import gg.rsmod.plugins.content.combat.*
import gg.rsmod.plugins.content.combat.formula.MeleeCombatFormula
import gg.rsmod.plugins.content.combat.formula.RangedCombatFormula
import gg.rsmod.game.model.entity.Player

package gg.rsmod.plugins.content.combat.scripts.impl

import gg.rsmod.game.model.combat.CombatClass
import gg.rsmod.game.model.combat.CombatScript
import gg.rsmod.game.model.combat.StyleType
import gg.rsmod.game.model.combat.WeaponStyle
import gg.rsmod.game.model.entity.Player
import gg.rsmod.game.model.queue.QueueTask
import gg.rsmod.plugins.api.HitType
import gg.rsmod.plugins.api.ext.*
import gg.rsmod.plugins.content.combat.*
import gg.rsmod.plugins.content.combat.formula.MeleeCombatFormula
import gg.rsmod.plugins.content.combat.formula.RangedCombatFormula

object GeneralGraardorCombatScript : CombatScript() {

    override val ids = intArrayOf(6260)

    override suspend fun handleSpecialCombat(it: QueueTask) {
        val npc = it.npc
        var target = npc.getCombatTarget() ?: return

        var attackCounter = 0
        var phase = 1

        while (npc.canEngageCombat(target)) {
            npc.facePawn(target)

            val hpPercent = npc.getCurrentLifepoints().toDouble() / npc.getMaximumLifepoints().toDouble()

            val newPhase = when {
                hpPercent <= 0.20 -> 4
                hpPercent <= 0.40 -> 3
                hpPercent <= 0.70 -> 2
                else -> 1
            }

            if (newPhase != phase) {
                phase = newPhase

                if (target is Player) {
                    when (phase) {
                        2 -> target.message("<col=ff6600>General Graardor roars: You dare challenge Bandos?</col>")
                        3 -> target.message("<col=ff0000>General Graardor becomes enraged! His shockwaves grow stronger.</col>")
                        4 -> target.message("<col=990000>General Graardor enters a berserk rage!</col>")
                    }
                }
            }

            if (npc.moveToAttackRange(it, target, distance = 1, projectile = false) && npc.isAttackDelayReady()) {
                attackCounter++

                val shockwaveFrequency = when (phase) {
                    1 -> 4
                    2 -> 3
                    else -> 2
                }

                val useShockwave = attackCounter % shockwaveFrequency == 0

                if (useShockwave) {
                    npc.prepareAttack(CombatClass.RANGED, StyleType.CRUSH, WeaponStyle.ACCURATE)
                    npc.animate(npc.combatDef.attackAnimation)

                    if (target is Player) {
                        target.message("<col=ff0000>General Graardor slams the ground with a massive shockwave!</col>")
                    }

                    npc.dealHit(
                        target = target,
                        formula = RangedCombatFormula,
                        delay = 1,
                        type = HitType.RANGE,
                    )

                    if (phase >= 3) {
                        if (target is Player) {
                            target.message("<col=ff3300>The shockwave echoes again!</col>")
                        }

                        npc.dealHit(
                            target = target,
                            formula = RangedCombatFormula,
                            delay = 2,
                            type = HitType.RANGE,
                        )
                    }

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

                    if (phase == 4) {
                        if (target is Player) {
                            target.message("<col=990000>Graardor follows up with a berserk strike!</col>")
                        }

                        npc.dealHit(
                            target = target,
                            formula = MeleeCombatFormula,
                            delay = 2,
                            type = HitType.MELEE,
                        )
                    }

                    npc.postAttackLogic(target)
                }
            }

            it.wait(if (phase == 4) 1 else 2)
            target = npc.getCombatTarget() ?: break
        }

        npc.resetFacePawn()
        npc.removeCombatTarget()
    }
}
