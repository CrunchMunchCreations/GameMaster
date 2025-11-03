package xyz.crunchmunch.mods.gamemaster.commands

import com.mojang.brigadier.arguments.StringArgumentType
import de.phyrone.brig.wrapper.DSLCommandNode
import de.phyrone.brig.wrapper.executesNoResult
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import xyz.crunchmunch.mods.gamemaster.players.InventoryManager

fun DSLCommandNode<CommandSourceStack>.inventoryCommands() {
    literal("inventory") {
        literal("load") {
            argument("player", EntityArgument.player()) {
                argument("storage", StringArgumentType.string()) {
                    suggest {
                        SharedSuggestionProvider.suggest(InventoryManager.getStorageKeys(EntityArgument.getPlayer(it, "player").uuid), this)
                    }

                    executesNoResult {
                        loadInventory(this, EntityArgument.getPlayer(it, "player"), StringArgumentType.getString(it, "storage"))
                    }
                }

                executesNoResult {
                    loadInventory(this, EntityArgument.getPlayer(it, "player"))
                }
            }
        }

        literal("save") {
            argument("player", EntityArgument.player()) {
                argument("storage", StringArgumentType.string()) {
                    executesNoResult {
                        saveInventory(this, EntityArgument.getPlayer(it, "player"), StringArgumentType.getString(it, "storage"))
                    }
                }

                executesNoResult {
                    saveInventory(this, EntityArgument.getPlayer(it, "player"))
                }
            }
        }
    }
}

private fun saveInventory(source: CommandSourceStack, player: ServerPlayer, storage: String = InventoryManager.DEFAULT_STORAGE_DIR) {
    InventoryManager.saveAndClearPlayerInventory(player, storage)
    source.sendSystemMessage(Component.literal("Saved ").append(player.displayName ?: player.name).append("'s inventory to $storage."))
}

private fun loadInventory(source: CommandSourceStack, player: ServerPlayer, storage: String = InventoryManager.DEFAULT_STORAGE_DIR) {
    InventoryManager.loadPreviousPlayerInventory(player, storage)
    source.sendSystemMessage(Component.literal("Restored ").append(player.displayName ?: player.name).append("'s inventory from $storage."))
}