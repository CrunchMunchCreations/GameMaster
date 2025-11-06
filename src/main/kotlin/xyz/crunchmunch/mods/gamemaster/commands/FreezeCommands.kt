package xyz.crunchmunch.mods.gamemaster.commands

import de.phyrone.brig.wrapper.DSLCommandNode
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import xyz.crunchmunch.mods.gamemaster.players.FreezeManager
import xyz.crunchmunch.mods.gamemaster.utils.sendSuccess

fun DSLCommandNode<CommandSourceStack>.freezeCommands() {
    literal("freeze") {
        argument("players", EntityArgument.players()) {
            executes {
                val players = EntityArgument.getPlayers(it, "players")

                for (player in players) {
                    FreezeManager.freezePlayer(player)
                }

                sendSuccess(Component.literal("Froze ${players.size} players."))

                players.size
            }
        }
    }

    literal("unfreeze") {
        argument("players", EntityArgument.players()) {
            executes {
                val players = EntityArgument.getPlayers(it, "players")

                for (player in players) {
                    FreezeManager.unfreezePlayer(player)
                }

                sendSuccess(Component.literal("Unfroze ${players.size} players."))

                players.size
            }
        }
    }
}