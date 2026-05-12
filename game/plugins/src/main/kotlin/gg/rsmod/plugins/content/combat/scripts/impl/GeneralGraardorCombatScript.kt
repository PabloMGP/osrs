package gg.rsmod.plugins.content.combat.scripts.impl

import gg.rsmod.game.model.Graphic
import gg.rsmod.game.model.World
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
import gg.rsmod.plugins.api.ProjectileType
import gg.rsmod.plugins.api.Skills
import gg.rsmod.plugins.api.cfg.Gfx
import gg.rsmod.plugins.api.ext.*
import gg.rsmod.plugins.content.combat.*
import gg.rsmod.plugins.content.combat.formula.MeleeCombatFormula
import gg.rsmod.plugins.content.combat.strategy.MagicCombatStrategy

object GeneralGraardorCombatScript : CombatScript() {

    override val ids = intArrayOf(6260)

    override suspend fun handleSpecialCombat(it: QueueTask) {
        val npc = it.npc
        val world = npc.world
        var target = npc.getCombatTarget() ?: return

        var attackCounter = 0
        var phase = 1

        target.msg("<col=ff0000>General Graardor roars: BANDOS DEMANDS BLOOD!</col>")

        while (npc.canEngageCombat(target) && npc.isAttackDelayReady()) {
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
                    2 -> target.msg("<col=ff9900>Graardor enters Phase 2: ranged shockwaves unlocked.</col>")
                    3 -> target.msg("<col=ff0000>Graardor enters Phase 3: stuns, poison, and stat drains unlocked.</col>")
                    4 -> target.msg("<col=990000>GRAARDOR ENTERS BERSERK MODE. Pray correctly or die.</col>")
                }
            }

            val distance = npc.getFrontFacingTile(target).getDistance(target.tile)

            /*
             * Important:
             * If ranged/mage is allowed from too far away, Graardor will camp one tile
             * and never need to chase the player.
             *
             * This forces him to walk closer first, then use melee/ranged/mage once nearby.
             */
            if (distance > 4) {
                npc.moveToAttackRange(it, target, distance = 3, projectile = false)
            } else if (distance <= 1 && npc.moveToAttackRange(it, target, distance = 1, projectile = false)) {
                attackCounter++

                when {
                    phase >= 4 && attackCounter % 4 == 0 -> berserkCombo(npc, target, world)
                    phase >= 4 && attackCounter % 5 == 0 -> prayerPunish(npc, target, world)
                    phase >= 3 && attackCounter % 6 == 0 -> crushingRoar(npc, target, world)
                    phase >= 3 && attackCounter % 5 == 0 -> bindingSlam(npc, target)
                    phase >= 2 && attackCounter % 4 == 0 -> rangedShockwave(npc, target, world, phase)
                    else -> meleeSmash(npc, target, phase)
                }

                npc.postAttackLogic(target)
            } else if (npc.moveToAttackRange(it, target, distance = 4, projectile = true)) {
                attackCounter++

                when {
                    phase >= 4 && attackCounter % 4 == 0 -> rangedShockwave(npc, target, world, phase)
                    phase >= 3 && attackCounter % 5 == 0 -> crushingRoar(npc, target, world)
                    else -> rangedBoulder(npc, target, world)
                }

                npc.postAttackLogic(target)
            }

            it.wait(
                when (phase) {
                    4 -> 3
                    3 -> 4
                    else -> 5
                }
            )

            target = npc.getCombatTarget() ?: break
        }

        npc.resetFacePawn()
        npc.removeCombatTarget()
    }

    private fun meleeSmash(
        npc: Npc,
        target: Pawn,
        phase: Int,
    ) {
        npc.prepareAttack(CombatClass.MELEE, StyleType.CRUSH, WeaponStyle.AGGRESSIVE)
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

    private fun rangedBoulder(
        npc: Npc,
        target: Pawn,
        world: World,
    ) {
        npc.prepareAttack(CombatClass.RANGED, StyleType.CRUSH, WeaponStyle.ACCURATE)
        npc.animate(npc.combatDef.attackAnimation, priority = true)

        target.msg("<col=ff9900>Graardor hurls a corrupted Bandos projectile!</col>")

        val projectile = npc.createProjectile(
            target = target,
            gfx = Gfx.RED_DRAGONFIRE_PROJ,
            type = ProjectileType.ARROW,
        )

        val hitDelay = MagicCombatStrategy.getHitDelay(
            npc.getFrontFacingTile(target),
            target.getCentreTile(),
        )

        world.spawn(projectile)

        npc.dealHit(
            target = target,
            formula = MeleeCombatFormula,
            delay = hitDelay,
            type = HitType.RANGE,
        )
    }

    private fun rangedShockwave(
        npc: Npc,
        target: Pawn,
        world: World,
        phase: Int,
    ) {
        npc.prepareAttack(CombatClass.RANGED, StyleType.CRUSH, WeaponStyle.ACCURATE)
        npc.animate(npc.combatDef.attackAnimation, priority = true)

        target.msg("<col=ff0000>Graardor slams the ground: SHOCKWAVE!</col>")

        val projectile = npc.createProjectile(
            target = target,
            gfx = Gfx.RED_DRAGONFIRE_PROJ,
            type = ProjectileType.ARROW,
        )

        val hitDelay = MagicCombatStrategy.getHitDelay(
            npc.getFrontFacingTile(target),
            target.getCentreTile(),
        )

        world.spawn(projectile)
        target.graphic(Graphic(Gfx.RESET, 110, projectile.lifespan))

        npc.dealHit(
            target = target,
            formula = MeleeCombatFormula,
            delay = hitDelay,
            type = HitType.RANGE,
        )

        if (phase >= 3) {
            target.msg("<col=ff3300>A second shockwave follows!</col>")

            npc.dealHit(
                target = target,
                formula = MeleeCombatFormula,
                delay = hitDelay + 1,
                type = HitType.RANGE,
            )
        }

        if (phase >= 4) {
            target.msg("<col=990000>The entire room shakes violently!</col>")

            npc.dealHit(
                target = target,
                formula = MeleeCombatFormula,
                delay = hitDelay + 2,
                type = HitType.RANGE,
            )
        }
    }

    private fun bindingSlam(
        npc: Npc,
        target: Pawn,
    ) {
        npc.prepareAttack(CombatClass.MELEE, StyleType.CRUSH, WeaponStyle.AGGRESSIVE)
        npc.animate(npc.combatDef.attackAnimation, priority = true)

        target.msg("<col=00ccff>Graardor pins you with a binding slam!</col>")

        npc.dealHit(
            target = target,
            formula = MeleeCombatFormula,
            delay = 1,
            type = HitType.MELEE,
        ) {
            target.freeze(cycles = 5) {
                target.msg("<col=00ccff>You are stunned by Graardor's slam.</col>")
            }
        }
    }

    private fun crushingRoar(
        npc: Npc,
        target: Pawn,
        world: World,
    ) {
        npc.prepareAttack(CombatClass.MAGIC, StyleType.MAGIC, WeaponStyle.ACCURATE)
        npc.animate(npc.combatDef.attackAnimation, priority = true)

        target.msg("<col=cc00ff>Graardor releases a crushing roar!</col>")

        val projectile = npc.createProjectile(
            target = target,
            gfx = Gfx.BLUE_DRAGONFIRE_PROJ,
            type = ProjectileType.MAGIC,
        )

        val hitDelay = MagicCombatStrategy.getHitDelay(
            npc.getFrontFacingTile(target),
            target.getCentreTile(),
        )

        world.spawn(projectile)
        target.graphic(Graphic(Gfx.RESET, 110, projectile.lifespan))

        if (target is Player) {
            target.skills.decrementCurrentLevel(Skills.ATTACK, 4, capped = false)
            target.skills.decrementCurrentLevel(Skills.STRENGTH, 4, capped = false)
            target.skills.decrementCurrentLevel(Skills.DEFENCE, 4, capped = false)
            target.skills.decrementCurrentLevel(Skills.PRAYER, 3, capped = false)
            target.message("Your combat stats and prayer are drained.", ChatMessageType.GAME_MESSAGE)
        }

        npc.dealHit(
            target = target,
            formula = MeleeCombatFormula,
            delay = hitDelay,
            type = HitType.MAGIC,
        )

        target.poison(4)
    }

    private fun prayerPunish(
        npc: Npc,
        target: Pawn,
        world: World,
    ) {
        npc.prepareAttack(CombatClass.MAGIC, StyleType.MAGIC, WeaponStyle.AGGRESSIVE)
        npc.animate(npc.combatDef.attackAnimation, priority = true)

        target.msg("<col=ff00ff>Graardor studies your protection prayer...</col>")

        val projectile = npc.createProjectile(
            target = target,
            gfx = Gfx.WHITE_DRAGONFIRE_PROJ,
            type = ProjectileType.MAGIC,
        )

        val hitDelay = MagicCombatStrategy.getHitDelay(
            npc.getFrontFacingTile(target),
            target.getCentreTile(),
        )

        world.spawn(projectile)
        target.graphic(Graphic(Gfx.RESET, 110, projectile.lifespan))

        if (target is Player) {
            target.message(
                "<col=ff0000>Graardor punishes your prayer and drains your Prayer points!</col>",
                ChatMessageType.GAME_MESSAGE,
            )

            target.skills.decrementCurrentLevel(Skills.PRAYER, 5, capped = false)
        }

        npc.dealHit(
            target = target,
            formula = MeleeCombatFormula,
            delay = hitDelay,
            type = HitType.MAGIC,
        )

        npc.dealHit(
            target = target,
            formula = MeleeCombatFormula,
            delay = hitDelay + 1,
            type = HitType.RANGE,
        )
    }

    private fun berserkCombo(
        npc: Npc,
        target: Pawn,
        world: World,
    ) {
        npc.prepareAttack(CombatClass.MELEE, StyleType.CRUSH, WeaponStyle.AGGRESSIVE)
        npc.animate(npc.combatDef.attackAnimation, priority = true)

        target.msg("<col=990000>Graardor unleashes a berserk combo!</col>")

        npc.dealHit(
            target = target,
            formula = MeleeCombatFormula,
            delay = 1,
            type = HitType.MELEE,
        )

        val projectile = npc.createProjectile(
            target = target,
            gfx = Gfx.RED_DRAGONFIRE_PROJ,
            type = ProjectileType.ARROW,
        )

        val hitDelay = MagicCombatStrategy.getHitDelay(
            npc.getFrontFacingTile(target),
            target.getCentreTile(),
        )

        world.spawn(projectile)

        npc.dealHit(
            target = target,
            formula = MeleeCombatFormula,
            delay = hitDelay,
            type = HitType.RANGE,
        )

        target.poison(3)
    }

    private fun Pawn.msg(message: String) {
        if (this is Player) {
            this.message(message, ChatMessageType.GAME_MESSAGE)
        }
    }
}
