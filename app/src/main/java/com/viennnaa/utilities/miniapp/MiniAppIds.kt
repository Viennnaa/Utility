package com.viennnaa.utilities.miniapp

/**
 * Mini app identifiers, in one place because they are used twice: as the
 * navigation route and as the namespace for a mini app's saved settings.
 *
 * These are persisted data. Changing one orphans that mini app's saved settings
 * and breaks any shortcut pointing at it, so treat them as fixed once shipped.
 */
object MiniAppIds {
    const val RANDOM_NUMBER = "random-number"
    const val CHOICE_MAKER = "choice-maker"
    const val LIST_PICKER = "list-picker"
    const val DICE_ROLLER = "dice-roller"
    const val TEAM_SPLITTER = "team-splitter"
    const val TIP_SPLITTER = "tip-splitter"
}
