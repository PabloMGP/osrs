package gg.rsmod.plugins.content.npcs.definitions.other

import gg.rsmod.game.fs.def.NpcDef
import gg.rsmod.game.model.combat.StyleType

val GRAARDOR = 6260

val graardorDef = world.definitions.get(NpcDef::class.java, GRAARDOR)

println("Graardor size BEFORE = ${graardorDef.size}")

graardorDef.size = 4
graardorDef.width = 180
graardorDef.length = 180

println("Graardor size AFTER = ${graardorDef.size}")

set_combat_def(GRAARDOR) {
    configs {
        attackSpeed = 6
        attackStyle = StyleType.CRUSH
        xpMultiplier = 1.15
        respawnDelay = 90
    }

    stats {
        hitpoints = 2550
        attack = 260
        strength = 285
        defence = 250
        magic = 230
        ranged = 230
    }

    bonuses {
        attackStab = 0
        attackSlash = 0
        attackCrush = 105
        attackMagic = 85
        attackRanged = 85

        defenceStab = 100
        defenceSlash = 100
        defenceCrush = 100
        defenceMagic = 90
        defenceRanged = 90

        attackBonus = 95
        strengthBonus = 95
        rangedStrengthBonus = 75
        magicDamageBonus = 45
    }

    anims {
        attack = 7060
        death = 7062
        block = 7061
    }

    aggro {
        radius = 18
    }
}
