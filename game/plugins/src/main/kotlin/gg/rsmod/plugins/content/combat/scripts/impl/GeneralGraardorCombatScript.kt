package gg.rsmod.plugins.content.combat.scripts.impl

import gg.rsmod.game.model.combat.CombatClass
import gg.rsmod.game.model.combat.CombatScript
import gg.rsmod.game.model.combat.StyleType
import gg.rsmod.game.model.combat.WeaponStyle
import gg.rsmod.game.model.entity.Npc
import gg.rsmod.game.model.entity.Pawn
import gg.rsmod.game.model.entity.Player
import gg.rsmod.game.model.queue.QueueTask
import gg.rsmod.plugins.api.ChatMessageType
import gg.rsmod.plugins.api.HitType
import gg.rsmod.plugins.api.Skills
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

        target.msg("<col=ff0000>General Graardor custom script loaded.</col>")

        while (npc.canEngageCombat(target)) {
            npc.facePawn(target)

            val hpPercent =
                npc.getCurrentLifepoints().toDouble() /
                    npc.getMaximumLifepoints().toDouble()

            val newPhase = when {
                hpPercent <= 0.20 -> 4
                hpPercent <= 0.45 -> 3
                hpPercent <= 0.75 -> 2
                else -> 1
            }

            if (newPhase != phase) {
                phase = newPhase

                when (phase) {
                    2 -> {
                        target.msg("<col=ff9900>Graardor roars: BANDOS WILL CRUSH YOU!</col>")
                        target.msg("<col=ff9900>Phase 2: Shockwaves become faster.</col>")
                    }

                    3 -> {
                        target.msg("<col=ff0000>Graardor becomes enraged!</col>")
                        target.msg("<col=ff0000>Phase 3: He can freeze, poison, and drain your stats.</col>")
                    }

                    4 -> {
                        target.msg("<col=990000>GRAARDOR ENTERS BERSERK MODE!</col>")
                        target.msg("<col=990000>Phase 4: Faster attacks and brutal double hits.</col>")
                    }
                }
            }

            if (
                npc.moveToAttackRange(it, target, distance = 1, projectile = false) &&
                npc.isAttackDelayReady()
            ) {
                attackCounter++

                when {
                    phase >= 4 && attackCounter % 2 == 0 -> berserkCombo(npc, target)

                    phase >= 3 && attackCounter % 4 == 0 -> crushingRoar(npc, target)

                    phase >= 3 && attackCounter % 3 == 0 -> bindingSlam(npc, target)

                    phase >= 2 && attackCounter % 3 == 0 -> shockwave(npc, target, phase)

                    else -> meleeSmash(npc, target, phase)
                }

                npc.postAttackLogic(target)
            }

            it.wait(
                when (phase) {
                    4 -> 1
                    3 -> 2
                    else -> 3
                }
            )

            target = npc.getCombatTarget() ?: break
        }

        npc.resetFacePawn()
        npc.removeCombatTarget()
    }

    private fun meleeSmash(npc: Npc, target: Pawn, phase: Int) {
        npc.prepareAttack(CombatClass.MELEE, StyleType.CRUSH, WeaponStyle.ACCURATE)
        npc.animate(npc.combatDef.attackAnimation)

        if (phase >= 2) {
            target.msg("<col=ff6600>Graardor swings with crushing force.</col>")
        }

        npc.dealHit(
            target = target,
            formula = MeleeCombatFormula,
            delay = 1,
            type = HitType.MELEE,
        )
    }

    private fun shockwave(npc: Npc, target: Pawn, phase: Int) {
        npc.prepareAttack(CombatClass.RANGED, StyleType.CRUSH, WeaponStyle.ACCURATE)
        npc.animate(npc.combatDef.attackAnimation)

        target.msg("<col=ff0000>Graardor slams the ground: SHOCKWAVE!</col>")

        npc.dealHit(
            target = target,
            formula = RangedCombatFormula,
            delay = 1,
            type = HitType.RANGE,
        )

        if (phase >= 3) {
            target.msg("<col=ff3300>The shockwave echoes again!</col>")

            npc.dealHit(
                target = target,
                formula = RangedCombatFormula,
                delay = 2,
                type = HitType.RANGE,
            )
        }
    }

    private fun bindingSlam(npc: Npc, target: Pawn) {
        npc.prepareAttack(CombatClass.MELEE, StyleType.CRUSH, WeaponStyle.AGGRESSIVE)
        npc.animate(npc.combatDef.attackAnimation)

        target.msg("<col=00ccff>Graardor smashes the floor and pins you in place!</col>")

        npc.dealHit(
            target = target,
            formula = MeleeCombatFormula,
            delay = 1,
            type = HitType.MELEE,
        ) {
            target.freeze(cycles = 4) {
                target.msg("<col=00ccff>You have been stunned by Graardor's slam.</col>")
            }
        }
    }

    private fun crushingRoar(npc: Npc, target: Pawn) {
        npc.prepareAttack(CombatClass.MAGIC, StyleType.MAGIC, WeaponStyle.ACCURATE)
        npc.animate(npc.combatDef.attackAnimation)

        target.msg("<col=cc00ff>Graardor releases a crushing roar!</col>")

        if (target is Player) {
            target.skills.decrementCurrentLevel(Skills.ATTACK, 5, capped = false)
            target.skills.decrementCurrentLevel(Skills.STRENGTH, 5, capped = false)
            target.skills.decrementCurrentLevel(Skills.DEFENCE, 5, capped = false)
            target.skills.decrementCurrentLevel(Skills.PRAYER, 3, capped = false)
            target.message("Your combat stats and prayer are drained.", ChatMessageType.GAME_MESSAGE)
        }

        npc.dealHit(
            target = target,
            formula = RangedCombatFormula,
            delay = 1,
            type = HitType.REGULAR_HIT,
        )

        target.poison(6)
    }

    private fun berserkCombo(npc: Npc, target: Pawn) {
        npc.prepareAttack(CombatClass.MELEE, StyleType.CRUSH, WeaponStyle.AGGRESSIVE)
        npc.animate(npc.combatDef.attackAnimation)

        target.msg("<col=990000>Graardor unleashes a berserk combo!</col>")

        npc.dealHit(
            target = target,
            formula = MeleeCombatFormula,
            delay = 1,
            type = HitType.MELEE,
        )

        npc.dealHit(
            target = target,
            formula = MeleeCombatFormula,
            delay = 2,
            type = HitType.MELEE,
        )

        npc.dealHit(
            target = target,
            formula = RangedCombatFormula,
            delay = 3,
            type = HitType.RANGE,
        )
    }

    private fun Pawn.msg(message: String) {
        if (this is Player) {
            this.message(message, ChatMessageType.GAME_MESSAGE)
        }
    }
}
