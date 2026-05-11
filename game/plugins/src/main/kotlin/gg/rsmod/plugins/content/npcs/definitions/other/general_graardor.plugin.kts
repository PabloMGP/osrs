package gg.rsmod.plugins.content.npcs.definitions.bosses

val ids = intArrayOf(6260)

ids.forEach {
    set_combat_def(it) {
        configs {
            attackSpeed = 6
            respawnDelay = 90
        }

        stats {
            hitpoints = 5550
            attack = 280
            strength = 350
            defence = 250
        }

        bonuses {
            attackStab = 0
            attackSlash = 0
            attackCrush = 120

            defenceStab = 90
            defenceSlash = 90
            defenceCrush = 90
            defenceMagic = 80
            defenceRanged = 80

            attackBonus = 100
            strengthBonus = 120
        }

        anims {
            attack = 7060
            death = 7062
            block = 7061
        }

        aggro {
            radius = 10
        }
    }
}
