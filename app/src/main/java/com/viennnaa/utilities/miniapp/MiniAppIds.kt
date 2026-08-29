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
    const val PASSWORD_GEN = "password-generator"
    const val RPS = "rock-paper-scissors"
    const val UNIT_CONVERTER = "unit-converter"
    const val PERCENTAGE = "percentage"
    const val DATE_CALC = "date-calculator"
    const val TEXT_TOOLS = "text-tools"
    const val FLASHLIGHT = "flashlight"
    const val LEVEL = "bubble-level"
    const val COMPASS = "compass"
    const val RULER = "ruler"
    const val STOPWATCH = "stopwatch"
    const val QR_GEN = "qr-generator"
    const val QR_SCAN = "qr-scanner"
    const val DISCOUNT = "discount"
    const val TIME_ZONES = "time-zones"
    const val WIFI_QR = "wifi-qr"
}
