package com.followupnadlan.accessibility

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Small hand-built icon set matching the Material Symbols used in the design file,
 * so the accessibility UI needs no extra icon dependency. Each path is authored on a
 * 24x24 viewport. Tint is applied at draw time via Icon(tint = ...).
 */
private fun icon(name: String, build: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(build).build()

private fun ImageVector.Builder.solid(
    color: Color = Color.Black,
    build: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
) {
    path(fill = SolidColor(color), pathBuilder = build)
}

object AccessibilityIcons {

    val Forum: ImageVector = icon("forum") {
        solid {
            moveTo(3f, 4f); horizontalLineTo(17f); verticalLineTo(14f); horizontalLineTo(7f)
            lineTo(3f, 18f); close()
        }
        solid {
            moveTo(19f, 8f); horizontalLineTo(21f); verticalLineTo(22f); lineTo(17f, 18f)
            horizontalLineTo(8f); verticalLineTo(16f); horizontalLineTo(19f); close()
        }
    }

    val Chat: ImageVector = icon("chat") {
        solid {
            moveTo(3f, 3f); horizontalLineTo(21f); verticalLineTo(17f); horizontalLineTo(8f)
            lineTo(3f, 22f); close()
        }
    }

    val Sms: ImageVector = icon("sms") {
        solid {
            moveTo(3f, 3f); horizontalLineTo(21f); verticalLineTo(17f); horizontalLineTo(8f)
            lineTo(3f, 22f); close()
        }
    }

    // Paper-plane "send" — matches the send-icon in quick-send-pure.html's [שלח] button.
    val Send: ImageVector = icon("send") {
        solid {
            moveTo(2f, 21f); lineTo(23f, 12f); lineTo(2f, 3f)
            verticalLineTo(10f); lineTo(17f, 12f); lineTo(2f, 14f); close()
        }
    }

    val Shield: ImageVector = icon("shield") {
        solid {
            moveTo(12f, 2f); lineTo(20f, 5f); verticalLineTo(11f)
            curveTo(20f, 16f, 16.5f, 20f, 12f, 22f)
            curveTo(7.5f, 20f, 4f, 16f, 4f, 11f); verticalLineTo(5f); close()
        }
    }

    val CheckCircle: ImageVector = icon("check_circle") {
        solid {
            moveTo(12f, 2f)
            curveTo(17.5f, 2f, 22f, 6.5f, 22f, 12f)
            curveTo(22f, 17.5f, 17.5f, 22f, 12f, 22f)
            curveTo(6.5f, 22f, 2f, 17.5f, 2f, 12f)
            curveTo(2f, 6.5f, 6.5f, 2f, 12f, 2f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(10.6f, 16.2f); lineTo(6.4f, 12f); lineTo(7.8f, 10.6f); lineTo(10.6f, 13.4f)
            lineTo(16.2f, 7.8f); lineTo(17.6f, 9.2f); close()
        }
    }

    val PhoneMissed: ImageVector = icon("phone_missed") {
        solid {
            moveTo(2f, 14f)
            curveTo(2f, 11f, 7f, 9f, 12f, 9f)
            curveTo(17f, 9f, 22f, 11f, 22f, 14f)
            curveTo(22f, 15.5f, 21f, 16f, 19.5f, 15.5f)
            lineTo(16.5f, 14.5f)
            curveTo(15.5f, 14.2f, 15f, 13.7f, 15f, 12.8f)
            verticalLineTo(11.8f)
            curveTo(13.8f, 11.4f, 10.2f, 11.4f, 9f, 11.8f)
            verticalLineTo(12.8f)
            curveTo(9f, 13.7f, 8.5f, 14.2f, 7.5f, 14.5f)
            lineTo(4.5f, 15.5f)
            curveTo(3f, 16f, 2f, 15.5f, 2f, 14f); close()
        }
        solid {
            moveTo(14f, 2f); lineTo(18f, 6f); lineTo(22f, 2f); lineTo(23.4f, 3.4f)
            lineTo(19.4f, 7.4f); lineTo(23.4f, 11.4f); lineTo(22f, 12.8f); lineTo(18f, 8.8f)
            lineTo(14f, 12.8f); lineTo(12.6f, 11.4f); lineTo(16.6f, 7.4f); lineTo(12.6f, 3.4f); close()
        }
    }

    val PhoneInTalk: ImageVector = icon("phone_in_talk") {
        solid {
            moveTo(6.5f, 3f)
            curveTo(7.2f, 3f, 7.7f, 3.4f, 7.9f, 4.1f)
            lineTo(8.8f, 7f)
            curveTo(9f, 7.6f, 8.8f, 8.2f, 8.4f, 8.6f)
            lineTo(6.6f, 10.4f)
            curveTo(7.8f, 12.9f, 9.1f, 14.2f, 11.6f, 15.4f)
            lineTo(13.4f, 13.6f)
            curveTo(13.8f, 13.2f, 14.4f, 13f, 15f, 13.2f)
            lineTo(17.9f, 14.1f)
            curveTo(18.6f, 14.3f, 19f, 14.8f, 19f, 15.5f)
            verticalLineTo(19f)
            curveTo(19f, 20f, 18f, 21f, 17f, 20.9f)
            curveTo(9f, 20.2f, 3.8f, 15f, 3.1f, 7f)
            curveTo(3f, 6f, 4f, 5f, 5f, 5f); close()
        }
        solid {
            moveTo(15f, 3f); curveTo(18.5f, 3f, 21f, 5.5f, 21f, 9f); horizontalLineTo(19f)
            curveTo(19f, 6.6f, 17.4f, 5f, 15f, 5f); close()
        }
        solid {
            moveTo(15f, 7f); curveTo(16.4f, 7f, 17f, 7.6f, 17f, 9f); horizontalLineTo(15f); close()
        }
    }

    val GppGood: ImageVector = icon("gpp_good") {
        solid {
            moveTo(12f, 2f); lineTo(20f, 5f); verticalLineTo(11f)
            curveTo(20f, 16f, 16.5f, 20f, 12f, 22f)
            curveTo(7.5f, 20f, 4f, 16f, 4f, 11f); verticalLineTo(5f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(10.8f, 15.4f); lineTo(7.6f, 12.2f); lineTo(9f, 10.8f); lineTo(10.8f, 12.6f)
            lineTo(14.6f, 8.8f); lineTo(16f, 10.2f); close()
        }
    }

    val Lock: ImageVector = icon("lock") {
        solid {
            moveTo(6f, 10f); horizontalLineTo(18f); verticalLineTo(21f); horizontalLineTo(6f); close()
        }
        solid {
            moveTo(8f, 10f); verticalLineTo(7f)
            curveTo(8f, 4.8f, 9.8f, 3f, 12f, 3f)
            curveTo(14.2f, 3f, 16f, 4.8f, 16f, 7f); verticalLineTo(10f); horizontalLineTo(14f)
            verticalLineTo(7f); curveTo(14f, 5.9f, 13.1f, 5f, 12f, 5f)
            curveTo(10.9f, 5f, 10f, 5.9f, 10f, 7f); verticalLineTo(10f); close()
        }
    }

    val Edit: ImageVector = icon("edit") {
        solid {
            moveTo(3f, 17.2f); lineTo(14.1f, 6.1f); lineTo(17.9f, 9.9f); lineTo(6.8f, 21f)
            horizontalLineTo(3f); close()
        }
        solid {
            moveTo(15.5f, 4.7f); lineTo(17.6f, 2.6f)
            curveTo(18f, 2.2f, 18.6f, 2.2f, 19f, 2.6f)
            lineTo(21.4f, 5f); curveTo(21.8f, 5.4f, 21.8f, 6f, 21.4f, 6.4f)
            lineTo(19.3f, 8.5f); close()
        }
    }

    val Delete: ImageVector = icon("delete") {
        solid {
            moveTo(6f, 7f); horizontalLineTo(18f); verticalLineTo(20f)
            curveTo(18f, 21f, 17f, 22f, 16f, 22f); horizontalLineTo(8f)
            curveTo(7f, 22f, 6f, 21f, 6f, 20f); close()
        }
        solid {
            moveTo(9f, 3f); horizontalLineTo(15f); verticalLineTo(5f); horizontalLineTo(20f)
            verticalLineTo(7f); horizontalLineTo(4f); verticalLineTo(5f); horizontalLineTo(9f); close()
        }
    }

    val PersonAdd: ImageVector = icon("person_add") {
        solid {
            moveTo(9f, 4f); curveTo(11.2f, 4f, 13f, 5.8f, 13f, 8f)
            curveTo(13f, 10.2f, 11.2f, 12f, 9f, 12f)
            curveTo(6.8f, 12f, 5f, 10.2f, 5f, 8f)
            curveTo(5f, 5.8f, 6.8f, 4f, 9f, 4f); close()
        }
        solid {
            moveTo(1f, 20f); curveTo(1f, 16f, 5f, 14f, 9f, 14f)
            curveTo(13f, 14f, 17f, 16f, 17f, 20f); verticalLineTo(21f); horizontalLineTo(1f); close()
        }
        solid {
            moveTo(18f, 8f); horizontalLineTo(20f); verticalLineTo(11f); horizontalLineTo(23f)
            verticalLineTo(13f); horizontalLineTo(20f); verticalLineTo(16f); horizontalLineTo(18f)
            verticalLineTo(13f); horizontalLineTo(15f); verticalLineTo(11f); horizontalLineTo(18f); close()
        }
    }

    val Add: ImageVector = icon("add") {
        solid {
            moveTo(11f, 4f); horizontalLineTo(13f); verticalLineTo(11f); horizontalLineTo(20f)
            verticalLineTo(13f); horizontalLineTo(13f); verticalLineTo(20f); horizontalLineTo(11f)
            verticalLineTo(13f); horizontalLineTo(4f); verticalLineTo(11f); horizontalLineTo(11f); close()
        }
    }

    val Link: ImageVector = icon("link") {
        solid {
            moveTo(7f, 11f); horizontalLineTo(17f); verticalLineTo(13f); horizontalLineTo(7f); close()
        }
        solid {
            moveTo(8f, 7f); horizontalLineTo(11f); verticalLineTo(9f); horizontalLineTo(8f)
            curveTo(6.3f, 9f, 5f, 10.3f, 5f, 12f); curveTo(5f, 13.7f, 6.3f, 15f, 8f, 15f)
            horizontalLineTo(11f); verticalLineTo(17f); horizontalLineTo(8f)
            curveTo(5.2f, 17f, 3f, 14.8f, 3f, 12f); curveTo(3f, 9.2f, 5.2f, 7f, 8f, 7f); close()
        }
        solid {
            moveTo(16f, 7f); horizontalLineTo(13f); verticalLineTo(9f); horizontalLineTo(16f)
            curveTo(17.7f, 9f, 19f, 10.3f, 19f, 12f); curveTo(19f, 13.7f, 17.7f, 15f, 16f, 15f)
            horizontalLineTo(13f); verticalLineTo(17f); horizontalLineTo(16f)
            curveTo(18.8f, 17f, 21f, 14.8f, 21f, 12f); curveTo(21f, 9.2f, 18.8f, 7f, 16f, 7f); close()
        }
    }

    val Dialpad: ImageVector = icon("dialpad") {
        solid {
            moveTo(5f, 3f); horizontalLineTo(8f); verticalLineTo(6f); horizontalLineTo(5f); close()
            moveTo(10.5f, 3f); horizontalLineTo(13.5f); verticalLineTo(6f); horizontalLineTo(10.5f); close()
            moveTo(16f, 3f); horizontalLineTo(19f); verticalLineTo(6f); horizontalLineTo(16f); close()
            moveTo(5f, 8.5f); horizontalLineTo(8f); verticalLineTo(11.5f); horizontalLineTo(5f); close()
            moveTo(10.5f, 8.5f); horizontalLineTo(13.5f); verticalLineTo(11.5f); horizontalLineTo(10.5f); close()
            moveTo(16f, 8.5f); horizontalLineTo(19f); verticalLineTo(11.5f); horizontalLineTo(16f); close()
            moveTo(5f, 14f); horizontalLineTo(8f); verticalLineTo(17f); horizontalLineTo(5f); close()
            moveTo(10.5f, 14f); horizontalLineTo(13.5f); verticalLineTo(17f); horizontalLineTo(10.5f); close()
            moveTo(16f, 14f); horizontalLineTo(19f); verticalLineTo(17f); horizontalLineTo(16f); close()
            moveTo(10.5f, 19.5f); horizontalLineTo(13.5f); verticalLineTo(22.5f); horizontalLineTo(10.5f); close()
        }
    }

    val Close: ImageVector = icon("close") {
        solid {
            moveTo(6.4f, 5f); lineTo(12f, 10.6f); lineTo(17.6f, 5f); lineTo(19f, 6.4f)
            lineTo(13.4f, 12f); lineTo(19f, 17.6f); lineTo(17.6f, 19f); lineTo(12f, 13.4f)
            lineTo(6.4f, 19f); lineTo(5f, 17.6f); lineTo(10.6f, 12f); lineTo(5f, 6.4f); close()
        }
    }

    val Settings: ImageVector = icon("settings") {
        solid {
            moveTo(12f, 8f); curveTo(14.2f, 8f, 16f, 9.8f, 16f, 12f)
            curveTo(16f, 14.2f, 14.2f, 16f, 12f, 16f)
            curveTo(9.8f, 16f, 8f, 14.2f, 8f, 12f)
            curveTo(8f, 9.8f, 9.8f, 8f, 12f, 8f); close()
            moveTo(10.5f, 2f); horizontalLineTo(13.5f); lineTo(14f, 4.5f)
            lineTo(16.5f, 5.5f); lineTo(18.7f, 4.2f); lineTo(20.8f, 6.3f); lineTo(19.5f, 8.5f)
            lineTo(20.5f, 11f); lineTo(23f, 11.5f); verticalLineTo(14.5f); lineTo(20.5f, 15f)
            lineTo(19.5f, 17.5f); lineTo(20.8f, 19.7f); lineTo(18.7f, 21.8f); lineTo(16.5f, 20.5f)
            lineTo(14f, 21.5f); lineTo(13.5f, 24f); horizontalLineTo(10.5f); lineTo(10f, 21.5f)
            lineTo(7.5f, 20.5f); lineTo(5.3f, 21.8f); lineTo(3.2f, 19.7f); lineTo(4.5f, 17.5f)
            lineTo(3.5f, 15f); lineTo(1f, 14.5f); verticalLineTo(11.5f); lineTo(3.5f, 11f)
            lineTo(4.5f, 8.5f); lineTo(3.2f, 6.3f); lineTo(5.3f, 4.2f); lineTo(7.5f, 5.5f)
            lineTo(10f, 4.5f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 10f); curveTo(13.1f, 10f, 14f, 10.9f, 14f, 12f)
            curveTo(14f, 13.1f, 13.1f, 14f, 12f, 14f)
            curveTo(10.9f, 14f, 10f, 13.1f, 10f, 12f)
            curveTo(10f, 10.9f, 10.9f, 10f, 12f, 10f); close()
        }
    }

    val Home: ImageVector = icon("home") {
        solid {
            moveTo(12f, 3f); lineTo(21f, 11f); horizontalLineTo(18f); verticalLineTo(21f)
            horizontalLineTo(14f); verticalLineTo(15f); horizontalLineTo(10f); verticalLineTo(21f)
            horizontalLineTo(6f); verticalLineTo(11f); horizontalLineTo(3f); close()
        }
    }

    val Today: ImageVector = icon("today") {
        solid {
            moveTo(4f, 5f); horizontalLineTo(20f); verticalLineTo(21f); horizontalLineTo(4f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 9f); horizontalLineTo(18f); verticalLineTo(19f); horizontalLineTo(6f); close()
        }
        solid {
            moveTo(7f, 3f); horizontalLineTo(9f); verticalLineTo(6f); horizontalLineTo(7f); close()
            moveTo(15f, 3f); horizontalLineTo(17f); verticalLineTo(6f); horizontalLineTo(15f); close()
        }
        solid {
            moveTo(8f, 11f); horizontalLineTo(12f); verticalLineTo(14f); horizontalLineTo(8f); close()
        }
    }

    val Group: ImageVector = icon("group") {
        solid {
            moveTo(8f, 5f); curveTo(9.7f, 5f, 11f, 6.3f, 11f, 8f)
            curveTo(11f, 9.7f, 9.7f, 11f, 8f, 11f)
            curveTo(6.3f, 11f, 5f, 9.7f, 5f, 8f)
            curveTo(5f, 6.3f, 6.3f, 5f, 8f, 5f); close()
            moveTo(2f, 19f); curveTo(2f, 15.7f, 5f, 13.5f, 8f, 13.5f)
            curveTo(11f, 13.5f, 14f, 15.7f, 14f, 19f); horizontalLineTo(2f); close()
        }
        solid {
            moveTo(16f, 6f); curveTo(17.7f, 6f, 19f, 7.3f, 19f, 9f)
            curveTo(19f, 10.7f, 17.7f, 12f, 16f, 12f); horizontalLineTo(15.5f)
            curveTo(16f, 11f, 16f, 9f, 15.5f, 8f); curveTo(15.5f, 7f, 16f, 6f, 16f, 6f); close()
            moveTo(16.5f, 13.6f); curveTo(19f, 14f, 22f, 15.7f, 22f, 19f)
            horizontalLineTo(16f); curveTo(16f, 17f, 16f, 15f, 16.5f, 13.6f); close()
        }
    }

    val Storefront: ImageVector = icon("storefront") {
        solid {
            moveTo(4f, 4f); horizontalLineTo(20f); verticalLineTo(7f)
            curveTo(20f, 8.5f, 18.5f, 10f, 17f, 10f)
            curveTo(15.5f, 10f, 14f, 8.5f, 14f, 7f)
            curveTo(14f, 8.5f, 12.5f, 10f, 11f, 10f)
            curveTo(9.5f, 10f, 8f, 8.5f, 8f, 7f)
            curveTo(8f, 8.5f, 6.5f, 10f, 5f, 10f)
            curveTo(3.5f, 10f, 2f, 8.5f, 2f, 7f); close()
        }
        solid {
            moveTo(4f, 11f); horizontalLineTo(20f); verticalLineTo(21f); horizontalLineTo(4f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(6f, 13f); horizontalLineTo(11f); verticalLineTo(17f); horizontalLineTo(6f); close()
        }
    }

    val Verified: ImageVector = Shield

    val Person: ImageVector = icon("person") {
        solid {
            moveTo(12f, 4f); curveTo(14.2f, 4f, 16f, 5.8f, 16f, 8f)
            curveTo(16f, 10.2f, 14.2f, 12f, 12f, 12f)
            curveTo(9.8f, 12f, 8f, 10.2f, 8f, 8f)
            curveTo(8f, 5.8f, 9.8f, 4f, 12f, 4f); close()
            moveTo(4f, 20f); curveTo(4f, 16f, 8f, 14f, 12f, 14f)
            curveTo(16f, 14f, 20f, 16f, 20f, 20f); verticalLineTo(21f); horizontalLineTo(4f); close()
        }
    }

    val Call: ImageVector = icon("call") {
        solid {
            moveTo(6.5f, 3f)
            curveTo(7.2f, 3f, 7.7f, 3.4f, 7.9f, 4.1f)
            lineTo(8.8f, 7f)
            curveTo(9f, 7.6f, 8.8f, 8.2f, 8.4f, 8.6f)
            lineTo(6.6f, 10.4f)
            curveTo(7.8f, 12.9f, 9.1f, 14.2f, 11.6f, 15.4f)
            lineTo(13.4f, 13.6f)
            curveTo(13.8f, 13.2f, 14.4f, 13f, 15f, 13.2f)
            lineTo(17.9f, 14.1f)
            curveTo(18.6f, 14.3f, 19f, 14.8f, 19f, 15.5f)
            verticalLineTo(19f)
            curveTo(19f, 20f, 18f, 21f, 17f, 20.9f)
            curveTo(9f, 20.2f, 3.8f, 15f, 3.1f, 7f)
            curveTo(3f, 6f, 4f, 5f, 5f, 5f); close()
        }
    }

    val Schedule: ImageVector = icon("schedule") {
        solid {
            moveTo(12f, 2f); curveTo(17.5f, 2f, 22f, 6.5f, 22f, 12f)
            curveTo(22f, 17.5f, 17.5f, 22f, 12f, 22f)
            curveTo(6.5f, 22f, 2f, 17.5f, 2f, 12f)
            curveTo(2f, 6.5f, 6.5f, 2f, 12f, 2f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(11f, 6f); horizontalLineTo(13f); verticalLineTo(12.4f); lineTo(16.5f, 14.4f)
            lineTo(15.5f, 16.1f); lineTo(11f, 13.5f); close()
        }
    }

    val Block: ImageVector = icon("block") {
        solid {
            moveTo(12f, 2f); curveTo(17.5f, 2f, 22f, 6.5f, 22f, 12f)
            curveTo(22f, 17.5f, 17.5f, 22f, 12f, 22f)
            curveTo(6.5f, 22f, 2f, 17.5f, 2f, 12f)
            curveTo(2f, 6.5f, 6.5f, 2f, 12f, 2f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(7f, 5.6f); lineTo(18.4f, 17f); lineTo(17f, 18.4f); lineTo(5.6f, 7f); close()
        }
    }

    val RadioChecked: ImageVector = icon("radio_button_checked") {
        solid {
            moveTo(12f, 2f); curveTo(17.5f, 2f, 22f, 6.5f, 22f, 12f)
            curveTo(22f, 17.5f, 17.5f, 22f, 12f, 22f)
            curveTo(6.5f, 22f, 2f, 17.5f, 2f, 12f)
            curveTo(2f, 6.5f, 6.5f, 2f, 12f, 2f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 5f); curveTo(15.9f, 5f, 19f, 8.1f, 19f, 12f)
            curveTo(19f, 15.9f, 15.9f, 19f, 12f, 19f)
            curveTo(8.1f, 19f, 5f, 15.9f, 5f, 12f)
            curveTo(5f, 8.1f, 8.1f, 5f, 12f, 5f); close()
        }
        solid {
            moveTo(12f, 7.5f); curveTo(14.5f, 7.5f, 16.5f, 9.5f, 16.5f, 12f)
            curveTo(16.5f, 14.5f, 14.5f, 16.5f, 12f, 16.5f)
            curveTo(9.5f, 16.5f, 7.5f, 14.5f, 7.5f, 12f)
            curveTo(7.5f, 9.5f, 9.5f, 7.5f, 12f, 7.5f); close()
        }
    }

    val RadioUnchecked: ImageVector = icon("radio_button_unchecked") {
        solid {
            moveTo(12f, 2f); curveTo(17.5f, 2f, 22f, 6.5f, 22f, 12f)
            curveTo(22f, 17.5f, 17.5f, 22f, 12f, 22f)
            curveTo(6.5f, 22f, 2f, 17.5f, 2f, 12f)
            curveTo(2f, 6.5f, 6.5f, 2f, 12f, 2f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 4.5f); curveTo(16.1f, 4.5f, 19.5f, 7.9f, 19.5f, 12f)
            curveTo(19.5f, 16.1f, 16.1f, 19.5f, 12f, 19.5f)
            curveTo(7.9f, 19.5f, 4.5f, 16.1f, 4.5f, 12f)
            curveTo(4.5f, 7.9f, 7.9f, 4.5f, 12f, 4.5f); close()
        }
    }

    val ChevronStart: ImageVector = icon("chevron") {
        solid {
            moveTo(15f, 5f); lineTo(16.4f, 6.4f); lineTo(10.8f, 12f); lineTo(16.4f, 17.6f)
            lineTo(15f, 19f); lineTo(8f, 12f); close()
        }
    }

    val ChevronForward: ImageVector = icon("chevron_forward") {
        solid {
            moveTo(9f, 5f); lineTo(7.6f, 6.4f); lineTo(13.2f, 12f); lineTo(7.6f, 17.6f)
            lineTo(9f, 19f); lineTo(16f, 12f); close()
        }
    }

    val Search: ImageVector = icon("search") {
        solid {
            moveTo(11f, 3f); curveTo(15.4f, 3f, 19f, 6.6f, 19f, 11f)
            curveTo(19f, 12.8f, 18.4f, 14.5f, 17.4f, 15.9f)
            lineTo(21.7f, 20.2f); lineTo(20.2f, 21.7f); lineTo(15.9f, 17.4f)
            curveTo(14.5f, 18.4f, 12.8f, 19f, 11f, 19f)
            curveTo(6.6f, 19f, 3f, 15.4f, 3f, 11f); curveTo(3f, 6.6f, 6.6f, 3f, 11f, 3f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(11f, 5.5f); curveTo(14f, 5.5f, 16.5f, 8f, 16.5f, 11f)
            curveTo(16.5f, 14f, 14f, 16.5f, 11f, 16.5f)
            curveTo(8f, 16.5f, 5.5f, 14f, 5.5f, 11f); curveTo(5.5f, 8f, 8f, 5.5f, 11f, 5.5f); close()
        }
    }

    val WhatsApp: ImageVector = icon("whatsapp") {
        solid {
            moveTo(12f, 2f)
            curveTo(6.5f, 2f, 2f, 6.5f, 2f, 12f)
            curveTo(2f, 13.8f, 2.5f, 15.5f, 3.3f, 17f)
            lineTo(2f, 22f); lineTo(7.2f, 20.7f)
            curveTo(8.6f, 21.5f, 10.3f, 22f, 12f, 22f)
            curveTo(17.5f, 22f, 22f, 17.5f, 22f, 12f)
            curveTo(22f, 6.5f, 17.5f, 2f, 12f, 2f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(8.4f, 7.2f)
            curveTo(8.6f, 7.2f, 8.8f, 7.2f, 9f, 7.2f)
            curveTo(9.2f, 7.2f, 9.4f, 7.2f, 9.6f, 7.7f)
            curveTo(9.8f, 8.2f, 10.2f, 9.3f, 10.3f, 9.4f)
            curveTo(10.4f, 9.6f, 10.4f, 9.7f, 10.3f, 9.9f)
            curveTo(10.2f, 10.1f, 10.1f, 10.2f, 10f, 10.4f)
            curveTo(9.9f, 10.5f, 9.7f, 10.7f, 9.9f, 11f)
            curveTo(10.1f, 11.3f, 10.7f, 12.3f, 11.6f, 13.1f)
            curveTo(12.7f, 14.1f, 13.6f, 14.4f, 13.9f, 14.5f)
            curveTo(14.2f, 14.7f, 14.4f, 14.6f, 14.6f, 14.4f)
            curveTo(14.8f, 14.2f, 15.2f, 13.7f, 15.4f, 13.4f)
            curveTo(15.6f, 13.2f, 15.8f, 13.2f, 16f, 13.3f)
            curveTo(16.2f, 13.4f, 17.3f, 13.9f, 17.5f, 14f)
            curveTo(17.7f, 14.1f, 17.9f, 14.2f, 17.9f, 14.4f)
            curveTo(18f, 14.6f, 18f, 15.5f, 17.6f, 16f)
            curveTo(17.2f, 16.5f, 16.4f, 17f, 15.9f, 17f)
            curveTo(15.5f, 17.1f, 15f, 17.1f, 12.9f, 16.3f)
            curveTo(10.4f, 15.3f, 8.8f, 12.8f, 8.6f, 12.5f)
            curveTo(8.5f, 12.3f, 7.5f, 11f, 7.5f, 9.6f)
            curveTo(7.5f, 8.2f, 8.2f, 7.5f, 8.4f, 7.2f); close()
        }
    }

    val Battery: ImageVector = icon("battery") {
        solid {
            moveTo(10.3f, 1.9f); horizontalLineTo(13.7f)
            curveTo(14.3f, 1.9f, 14.7f, 2.4f, 14.7f, 3f)
            verticalLineTo(4.5f); horizontalLineTo(9.3f); verticalLineTo(3f)
            curveTo(9.3f, 2.4f, 9.7f, 1.9f, 10.3f, 1.9f); close()
        }
        solid {
            moveTo(4f, 6f); horizontalLineTo(20f); verticalLineTo(22f); horizontalLineTo(4f); close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(11f, 9f); horizontalLineTo(13f); verticalLineTo(13f); horizontalLineTo(11f); close()
            moveTo(11f, 15.5f); horizontalLineTo(13f); verticalLineTo(17.5f); horizontalLineTo(11f); close()
        }
    }

    val ExpandMore: ImageVector = icon("expand_more") {
        solid {
            moveTo(12f, 15.4f); lineTo(5.6f, 9f); lineTo(7f, 7.6f); lineTo(12f, 12.6f)
            lineTo(17f, 7.6f); lineTo(18.4f, 9f); close()
        }
    }
}
