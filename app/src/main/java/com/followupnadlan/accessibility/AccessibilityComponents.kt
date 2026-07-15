package com.followupnadlan.accessibility

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A large rounded "pill" action button, the primary CTA style across the design. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = AccessibilityColors.Primary,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    val resolvedBackground = if (enabled) background else background.copy(alpha = 0.45f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(resolvedBackground)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(21.dp))
            }
            Text(text, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

/** A pill button with an outline and white fill — the secondary "לא עכשיו" style. */
@Composable
fun OutlinePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = AccessibilityColors.PrimaryTintBorder,
    contentColor: Color = AccessibilityColors.TextBody,
    leadingIcon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(AccessibilityColors.Surface)
            .border(1.5.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            }
            Text(text, color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

/** White rounded card with a soft shadow, used for all content groupings. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 18,
    background: Color = AccessibilityColors.Surface,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp),
        color = background,
        border = border,
        shadowElevation = 2.dp
    ) {
        content()
    }
}

/** Rounded square icon badge (the tinted icon tiles at the top of Home/Setup). */
@Composable
fun IconBadge(
    icon: ImageVector,
    background: Color,
    tint: Color,
    boxSize: Int = 64,
    cornerRadius: Int = 20,
    iconSize: Int = 34,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(boxSize.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize.dp))
    }
}

/** A status chip row: left icon + label, right check. Used by Home's three chips. */
@Composable
fun StatusChip(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    trailingIcon: ImageVector = AccessibilityIcons.CheckCircle,
    trailingTint: Color = AccessibilityColors.GreenBright,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AccessibilityColors.SubtleSurface)
            .border(1.dp, AccessibilityColors.CardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AccessibilityColors.TextStrong)
        }
        Icon(trailingIcon, contentDescription = null, tint = trailingTint, modifier = Modifier.size(21.dp))
    }
}

/** A radio-style selectable row (label on one side, checked/unchecked dot on the other). */
@Composable
fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingTint: Color = AccessibilityColors.TextMuted
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = leadingTint, modifier = Modifier.size(21.dp))
            }
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) AccessibilityColors.TextStrong else AccessibilityColors.TextBody
            )
        }
        Icon(
            if (selected) AccessibilityIcons.RadioChecked else AccessibilityIcons.RadioUnchecked,
            contentDescription = null,
            tint = if (selected) AccessibilityColors.Primary else AccessibilityColors.UnselectedIcon,
            modifier = Modifier.size(22.dp)
        )
    }
}

/** Screen heading with a trailing icon, e.g. "מה קרה היום" + calendar. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = AccessibilityColors.Heading
        )
        if (trailingIcon != null) {
            Icon(trailingIcon, contentDescription = null, tint = AccessibilityColors.IconDark, modifier = Modifier.size(24.dp))
        }
    }
}

/** A round circular icon avatar, used in exclusion rows. */
@Composable
fun CircleAvatar(
    background: Color,
    modifier: Modifier = Modifier,
    boxSize: Int = 38,
    initial: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = AccessibilityColors.Primary
) {
    Box(
        modifier = modifier
            .size(boxSize.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        when {
            initial != null -> Text(initial, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccessibilityColors.Primary)
            icon != null -> Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
    }
}

/** Centered caption line under a primary CTA, e.g. "מוכן לשיחות שלא נענו". */
@Composable
fun CaptionWithIcon(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(
            text,
            modifier = Modifier.padding(start = 6.dp),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}
